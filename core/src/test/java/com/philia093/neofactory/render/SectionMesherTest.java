package com.philia093.neofactory.render;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.Section;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what the mesher writes for a section.
 * <p>
 * The point of meshing a world of cubes is that a block which is never seen costs nothing, so the
 * questions here are the ones a wrong mesh answers badly: how many corners a block shows, which of them
 * are left out because a neighbour hides them, how dark a corner next to a neighbour is, and which
 * picture a face asks for. Every corner is counted instead of looked at, so the arithmetic of the mesh
 * is checked without a window.
 */
class SectionMesherTest {

    /** Layer the pictures of the test world live in, one per name. */
    private static final Map<String, Integer> LAYERS = Map.of(
            "stone", 0, "grass_top", 1, "grass_side", 2, "dirt", 3, "log_oak", 4, "log_oak_top", 5);

    /** Section the tests mesh. */
    private final Section section = new Section(0);

    /** Cells outside the section, so a face at the border can be hidden as well. */
    private final Map<String, Block> outside = new HashMap<>();

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aLoneBlockShowsItsSixFaces() {
        put(4, 4, 4, Blocks.STONE);

        List<MeshData> meshes = mesh();

        assertEquals(1, meshes.size());
        assertEquals(6 * 4, corners(meshes), "a block in the open shows four corners per face");
        assertEquals(6 * 2, triangles(meshes), "and two triangles per face");
        assertTrue(holdsLayer(meshes, LAYERS.get("stone")), "every face asks for the one picture");
    }

    @Test
    void twoBlocksHideTheFacesBetweenThem() {
        put(4, 4, 4, Blocks.STONE);
        put(5, 4, 4, Blocks.STONE);

        assertEquals(10 * 4, corners(mesh()),
                "two blocks that touch share a face, which neither of them draws");
    }

    @Test
    void aNeighbourOutsideTheSectionHidesTheFaceAtTheBorder() {
        put(0, 4, 4, Blocks.STONE);
        outside.put("-1,4,4", Blocks.STONE);

        assertEquals(5 * 4, corners(mesh()), "the block on the other side of the border hides a face");
    }

    @Test
    void onlyTheSurfaceOfASolidCubeIsDrawn() {
        for (int x = 1; x <= 3; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = 1; z <= 3; z++) {
                    put(x, y, z, Blocks.STONE);
                }
            }
        }

        assertEquals(6 * 9 * 4, corners(mesh()),
                "a cube of twenty seven blocks shows the six sides of its surface, nine faces each,"
                        + " so the block inside costs nothing");
    }

    @Test
    void aFaceAsksForThePictureOfItsOwnSide() {
        put(4, 4, 4, Blocks.GRASS);

        List<MeshData> meshes = mesh();

        assertTrue(holdsLayer(meshes, LAYERS.get("grass_top")), "the top shows its own picture");
        assertTrue(holdsLayer(meshes, LAYERS.get("grass_side")), "the sides show the shaded picture");
        assertTrue(holdsLayer(meshes, LAYERS.get("dirt")), "and the bottom shows dirt");
    }

    @Test
    void theTopOfABlockIsBrighterThanItsBottom() {
        put(4, 4, 4, Blocks.GRASS);

        List<MeshData> meshes = mesh();

        assertEquals(153, brightest(meshes, LAYERS.get("grass_top")), 2,
                "the face that catches the whole sky keeps the red of the tint of its block, 0.6");
        assertEquals(77, brightest(meshes, LAYERS.get("dirt")), 2,
                "and the one that looks down keeps half of that, 0.3");
    }

    @Test
    void aCornerBetweenTwoBlocksIsDarkerThanOneInTheOpen() {
        // Two blocks that share only an edge: the corners along that edge are shadowed, the others are
        // not, which is what makes the edge read as a corner.
        put(4, 4, 4, Blocks.STONE);
        put(5, 5, 4, Blocks.STONE);

        List<MeshData> meshes = mesh();

        float lowest = 2.0f;
        float highest = -1.0f;
        for (MeshData mesh : meshes) {
            float[] vertices = mesh.vertexFloats();
            for (int corner = 0; corner < mesh.vertexCount(); corner++) {
                float red = vertices[corner * MeshData.FLOATS_PER_VERTEX + MeshData.RED];
                lowest = Math.min(lowest, red);
                highest = Math.max(highest, red);
            }
        }
        assertTrue(lowest < highest, "every corner of the two blocks was drawn with the same light");
    }

    @Test
    void aCornerTakesTheTextureCoordinateOfItsPicture() {
        put(4, 4, 4, Blocks.STONE);

        MeshData mesh = mesh().get(0);

        float lowest = Float.MAX_VALUE;
        float highest = -Float.MAX_VALUE;
        for (int corner = 0; corner < mesh.vertexCount(); corner++) {
            float v = mesh.vertexFloats()[corner * MeshData.FLOATS_PER_VERTEX + 4];
            lowest = Math.min(lowest, v);
            highest = Math.max(highest, v);
        }
        assertEquals(0.0f, lowest, 1.0e-6f, "no corner takes the first row of the picture");
        assertEquals(1.0f, highest, 1.0e-6f, "no corner takes the last row of the picture");
    }

    @Test
    void aPictureTheArrayDoesNotHoldIsSkipped() {
        put(4, 4, 4, Blocks.STONE);

        List<MeshData> meshes = SectionMesher.build(section, 0, 0, 0, this::blockAt, picture -> -1);

        assertTrue(meshes.isEmpty(), "a face whose picture is unknown is not drawn");
    }

    /** Puts a block into the section. */
    private void put(int x, int y, int z, Block block) {
        section.setRawId(x, y, z, block.id());
    }

    /** Meshes the section with the layers of the test world. */
    private List<MeshData> mesh() {
        return SectionMesher.build(section, 0, 0, 0, this::blockAt, LAYERS::get);
    }

    /** The blocks of the section and of the shell around it. */
    private Block blockAt(int x, int y, int z) {
        if (Section.contains(x) && Section.contains(y) && Section.contains(z)) {
            return BlockRegistry.byId(section.rawId(x, y, z));
        }
        Block block = outside.get(x + "," + y + "," + z);
        return block == null ? Blocks.AIR : block;
    }

    /** Amount of corners all meshes together hold. */
    private static int corners(List<MeshData> meshes) {
        int total = 0;
        for (MeshData mesh : meshes) {
            total += mesh.vertexCount();
        }
        return total;
    }

    /** Amount of triangles all meshes together hold. */
    private static int triangles(List<MeshData> meshes) {
        int total = 0;
        for (MeshData mesh : meshes) {
            total += mesh.indexCount() / 3;
        }
        return total;
    }

    /** {@code true} when a corner of the mesh asks for that layer of the array. */
    private static boolean holdsLayer(List<MeshData> meshes, int layer) {
        for (MeshData mesh : meshes) {
            float[] vertices = mesh.vertexFloats();
            for (int corner = 0; corner < mesh.vertexCount(); corner++) {
                if ((int) vertices[corner * MeshData.FLOATS_PER_VERTEX + 5] == layer) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Brightest red any corner of that layer is drawn with, {@code -1} when it is not drawn. */
    private static int brightest(List<MeshData> meshes, int layer) {
        int brightest = -1;
        for (MeshData mesh : meshes) {
            float[] vertices = mesh.vertexFloats();
            for (int corner = 0; corner < mesh.vertexCount(); corner++) {
                if ((int) vertices[corner * MeshData.FLOATS_PER_VERTEX + 5] == layer) {
                    int red = (int) (vertices[corner * MeshData.FLOATS_PER_VERTEX + MeshData.RED]
                            * 255.0f + 0.5f);
                    brightest = Math.max(brightest, red);
                }
            }
        }
        return brightest;
    }
}
