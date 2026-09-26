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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
    private static final Map<String, Integer> LAYERS = Map.ofEntries(
            Map.entry("stone", 0), Map.entry("grass_top", 1), Map.entry("grass_side", 2),
            Map.entry("dirt", 3), Map.entry("log_oak", 4), Map.entry("log_oak_top", 5),
            Map.entry("sapling_oak", 6),
            Map.entry("stone_slab_top", 7), Map.entry("stone_slab_side", 8),
            Map.entry("furnace/furnace_side", 9), Map.entry("furnace/furnace_top", 10),
            Map.entry("furnace/furnace_front_off", 11));

    /**
     * Layers of a grinder that runs, the machine whose top is a strip of frames.
     * <p>
     * The numbers are far behind the ones of the pictures above, so a corner of a machine is never
     * counted as a corner of the terrain these tests mesh otherwise.
     */
    private static final Map<String, Integer> GRINDER_LAYERS = Map.ofEntries(
            Map.entry("bronze_casing/bronze_casing_top", 20),
            Map.entry("bronze_casing/bronze_casing_side", 21),
            Map.entry("bronze_casing/bronze_casing_bottom", 22),
            Map.entry("grinder/grinder_top_active", 23),
            Map.entry("grinder/grinder_front_active", 24));

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
        assertEquals(128, brightest(meshes, LAYERS.get("dirt")), 2,
                "and the one that looks down keeps half of the white its picture is drawn in, 0.5,"
                        + " because dirt carries no colour of the grass block");
    }

    @Test
    void theSideOfGrassCarriesTheTintOfItsBlock() {
        put(4, 4, 4, Blocks.GRASS);

        List<MeshData> meshes = mesh();

        // The four sides and the top are painted in the colour of the biome, because their sheets hold
        // brightness only; the dirt of the bottom keeps the colour it is drawn in, see
        // models/block/grass.json.
        assertEquals((4 + 1 + 1) * 4, corners(meshes),
                "every side of the grass is drawn once, and the top and the bottom once");
        assertEquals(122, brightest(meshes, LAYERS.get("grass_side")), 2,
                "the side is painted in the red of the tint of the grass, 0.6, over the light of the"
                        + " brightest side of a block, 0.8");
    }

    @Test
    void thePictureOfASideIsUprightOnEveryFace() {
        put(4, 4, 4, Blocks.GRASS);

        List<MeshData> meshes = mesh();

        // The north face of the block lies at Z = 4. Seen from outside the north face, the +X axis
        // runs to the left, so the corner at X = 0 takes the right edge of the picture; the corner
        // at the top of the face takes its first row.
        assertCorner(meshes, LAYERS.get("grass_side"), 4.0f, 5.0f, 4.0f, 1.0f, 0.0f,
                "the upper left corner of the north face");
        assertCorner(meshes, LAYERS.get("grass_side"), 5.0f, 4.0f, 4.0f, 0.0f, 1.0f,
                "the lower right corner of the north face");
        // The south face lies at Z = 5 and reads the other way round, because a viewer in front of it
        // sees the +X axis to the right.
        assertCorner(meshes, LAYERS.get("grass_side"), 4.0f, 5.0f, 5.0f, 0.0f, 0.0f,
                "the upper left corner of the south face");
    }

    @Test
    void theFrontOfAFurnaceFollowsTheStateOfItsCell() {
        put(4, 4, 4, Blocks.FURNACE);

        List<MeshData> meshes = mesh();

        assertCorner(meshes, LAYERS.get("furnace/furnace_front_off"), 4.0f, 4.0f, 4.0f, 1.0f, 1.0f,
                "the mouth of a furnace that was never set");
        assertEquals(4, cornersOfLayer(meshes, LAYERS.get("furnace/furnace_front_off")),
                "and it is drawn on one face only, the one the model writes its front on");
    }

    @Test
    void theGearOnTheTopOfARunningMachineCarriesItsFrames() {
        put(4, 4, 4, Blocks.GRINDER);
        int running = Blocks.GRINDER.states().stateOf(Map.of("facing", "north", "lit", "true"));

        List<MeshData> meshes = SectionMesher.build(section, 0, 0, 0, this::blockAt,
                (x, y, z) -> running, new SectionMesher.Pictures() {

                    @Override
                    public int layer(String picture) {
                        return GRINDER_LAYERS.getOrDefault(picture, -1);
                    }

                    @Override
                    public int frames(String picture) {
                        // The gear of the art pack is a strip of four frames; the mouth of the machine
                        // and the casing around it are single pictures, see BlockPictures#frameCountOf.
                        return "grinder/grinder_top_active".equals(picture) ? 4 : 1;
                    }
                });

        assertEquals(4, framesOf(meshes, GRINDER_LAYERS.get("grinder/grinder_top_active")),
                "the gear turns through the four frames of the run its layer begins");
        assertEquals(4, cornersOfLayer(meshes, GRINDER_LAYERS.get("grinder/grinder_top_active")),
                "and that run is drawn on the top of the machine and nowhere else");
        assertEquals(1, framesOf(meshes, GRINDER_LAYERS.get("grinder/grinder_front_active")),
                "the mouth of the machine stands still while the gear behind it turns");
        assertEquals(1, framesOf(meshes, GRINDER_LAYERS.get("bronze_casing/bronze_casing_side")),
                "and so does the casing the machine is built of");
    }

    @Test
    void aStateTurnsTheModelOfABlock() {
        put(4, 4, 4, Blocks.FURNACE);
        int east = Blocks.FURNACE.states().stateOf(Map.of("facing", "east"));

        List<MeshData> meshes = SectionMesher.build(section, 0, 0, 0, this::blockAt,
                (x, y, z) -> east, name -> LAYERS.getOrDefault(name, -1));

        // The model faces north, so a turn of ninety degrees about the vertical axis of the block
        // carries the mouth to the east face: the plane X = 5 is where the front is drawn now.
        assertCorner(meshes, LAYERS.get("furnace/furnace_front_off"), 5.0f, 5.0f, 4.0f, 1.0f, 0.0f,
                "the mouth of a furnace that faces east");
        assertTrue(holdsLayer(meshes, LAYERS.get("furnace/furnace_side")), "the other faces are its sides");
    }

    @Test
    void aPlantIsDrawnWithoutCullingOrShadow() {
        put(4, 4, 4, Blocks.SAPLING_OAK);

        List<MeshData> meshes = mesh();

        // Two crossed planes, each showing its two sides: four faces of four corners, and none of
        // them is hidden by the air around it, because a plane is not the border of a block.
        assertEquals(4 * 4, corners(meshes), "both planes of the plant are drawn from both sides");
        assertTrue(holdsLayer(meshes, LAYERS.get("sapling_oak")), "with the picture of the plant");
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

    @Test
    void theGroundUnderAHalfBlockKeepsItsTopFace() {
        // A slab is thinner than a cube, so its cell does not hide the face behind it: the piece of ground
        // the slab does not cover is still seen, see SectionMesher#hides.
        put(4, 4, 4, Blocks.STONE);
        put(4, 5, 4, Blocks.STONE_SLAB);

        List<MeshData> meshes = mesh();

        assertEquals(6 * 4, cornersOfLayer(meshes, LAYERS.get("stone")),
                "the ground keeps all six of its faces, the slab above hides none of them");
    }

    /** Puts a block into the section. */
    private void put(int x, int y, int z, Block block) {
        section.setRawId(x, y, z, block.id());
    }

    /** Meshes the section with the layers of the test world. */
    private List<MeshData> mesh() {
        // A picture the table does not hold is not in the array of the test world, which is what
        // BlockPictures#layer reports with a negative layer as well.
        return SectionMesher.build(section, 0, 0, 0, this::blockAt,
                name -> LAYERS.getOrDefault(name, -1));
    }

    /** The blocks of the section and of the shell around it. */
    private Block blockAt(int x, int y, int z) {
        if (Section.contains(x) && Section.contains(y) && Section.contains(z)) {
            return BlockRegistry.byId(section.rawId(x, y, z));
        }
        Block block = outside.get(x + "," + y + "," + z);
        return block == null ? Blocks.AIR : block;
    }

    /**
     * Checks the spot inside its picture the corner of a face at a position takes.
     *
     * @param meshes meshes of a build
     * @param layer layer the picture of the face lives in
     * @param x world X coordinate of the corner
     * @param y world Y coordinate of the corner
     * @param z world Z coordinate of the corner
     * @param u column the corner has to ask for
     * @param v row the corner has to ask for
     * @param what the corner, used in the message
     */
    private static void assertCorner(List<MeshData> meshes, int layer, float x, float y, float z,
            float u, float v, String what) {
        for (MeshData mesh : meshes) {
            float[] vertices = mesh.vertexFloats();
            for (int corner = 0; corner < mesh.vertexCount(); corner++) {
                int base = corner * MeshData.FLOATS_PER_VERTEX;
                if ((int) vertices[base + 5] != layer
                        || Math.abs(vertices[base] - x) > 1.0e-4f
                        || Math.abs(vertices[base + 1] - y) > 1.0e-4f
                        || Math.abs(vertices[base + 2] - z) > 1.0e-4f) {
                    continue;
                }
                assertEquals(u, vertices[base + 3], 1.0e-5f, what + " takes the wrong column");
                assertEquals(v, vertices[base + 4], 1.0e-5f, what + " takes the wrong row");
                return;
            }
        }
        fail("no corner at (" + x + ", " + y + ", " + z + ") draws layer " + layer + ": " + what);
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

    /** Amount of corners that ask for one layer. */
    private static int cornersOfLayer(List<MeshData> meshes, int layer) {
        int count = 0;
        for (MeshData mesh : meshes) {
            float[] vertices = mesh.vertexFloats();
            for (int corner = 0; corner < mesh.vertexCount(); corner++) {
                if ((int) vertices[corner * MeshData.FLOATS_PER_VERTEX + 5] == layer) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Frames the corners of one layer carry, {@code 0} when that layer is not drawn at all. */
    private static int framesOf(List<MeshData> meshes, int layer) {
        int frames = 0;
        for (MeshData mesh : meshes) {
            float[] vertices = mesh.vertexFloats();
            for (int corner = 0; corner < mesh.vertexCount(); corner++) {
                if ((int) vertices[corner * MeshData.FLOATS_PER_VERTEX + 5] == layer) {
                    frames = Math.max(frames,
                            (int) vertices[corner * MeshData.FLOATS_PER_VERTEX + MeshData.FRAMES]);
                }
            }
        }
        return frames;
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
