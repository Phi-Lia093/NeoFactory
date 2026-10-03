package com.philia093.neofactory.render;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.Section;
import com.philia093.neofactory.world.interaction.FaceAppearance;
import com.philia093.neofactory.world.interaction.FacePicture;
import com.philia093.neofactory.world.interaction.FaceRole;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a block entity decides what its sides are drawn with.
 * <p>
 * A machine of the industry is drawn from its model and from what its block entity says about the sides a
 * player gave a job to, see {@link FaceAppearance}. What is checked here is what the mesher writes for such
 * a cell: the picture of the model where the entity owns nothing, the casing of the block and the overlay of
 * the role where it does, and the ask itself - one per cell, and none for a cell whose block carries no
 * block entity.
 */
class MachineAppearanceTest {

    /** Layers of the pictures of the test world, one per name. */
    private static final Map<String, Integer> LAYERS = Map.ofEntries(
            Map.entry("bronze_casing/bronze_casing_side", 21),
            Map.entry("bronze_casing/bronze_casing_top", 20),
            Map.entry("bronze_casing/bronze_casing_bottom", 22),
            Map.entry("grinder/grinder_front", 23),
            Map.entry("grinder/grinder_top", 24),
            Map.entry("machine_casing/lv", 30),
            Map.entry("machine_overlay/pipe", 31),
            Map.entry("machine_overlay/energy_out", 32));

    /** Layer of the casing the machine of the test is built of. */
    private static final int CASING = 30;

    /** Layer of the overlay of a side that carries a pipe. */
    private static final int PIPE = 31;

    /** Layer of the overlay of a side the power leaves through. */
    private static final int ENERGY_OUT = 32;

    /** Section the tests mesh. */
    private final Section section = new Section(0);

    /** Cells outside the section, so a face at the border can be hidden as well. */
    private final Map<String, Block> outside = new HashMap<>();

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aModelStandsWhereNoBlockEntityOwnsASide() {
        put(4, 4, 4, Blocks.GRINDER);

        List<MeshData> meshes = build(0, SectionMesher.NO_APPEARANCES);

        // Six faces of four corners and the two overlays the model of the grinder writes: its gear over the
        // top and its mouth over the front, see models/block/grinder.json.
        assertEquals(8 * 4, corners(meshes), "every face of the machine and its two overlays are drawn");
        assertEquals(4, cornersOfLayer(meshes, LAYERS.get("grinder/grinder_front")),
                "the mouth of the machine keeps the picture of the model");
        assertEquals(4, cornersOfLayer(meshes, LAYERS.get("grinder/grinder_top")),
                "and so does the gear over its top");
        assertEquals(0, cornersOfLayer(meshes, CASING),
                "no casing of a tier is drawn while no block entity owns a side");
        assertEquals(0, cornersOfLayer(meshes, PIPE), "and no overlay of a role either");
    }

    @Test
    void anOwnedSideWearsTheCasingAndTheOverlayOfItsRole() {
        put(4, 4, 4, Blocks.GRINDER);
        FaceAppearance owner = face -> face == BlockFace.EAST
                ? FacePicture.of("machine_casing/lv", FaceRole.FLUID_OUT)
                : null;

        List<MeshData> meshes = build(0, owningTheGrinder(owner));

        assertEquals(8 * 4 + 4, corners(meshes),
                "the plain side of the model is replaced by the casing of the tier and the overlay of the"
                        + " pipe, one face more than the model wrote there");
        assertEquals(4, cornersOfLayer(meshes, CASING), "the casing of the tier stands on that side alone");
        assertEquals(4, cornersOfLayer(meshes, PIPE), "with the overlay of the pipe over it");
        assertEquals(3 * 4, cornersOfLayer(meshes, LAYERS.get("bronze_casing/bronze_casing_side")),
                "and the three other flanks keep the casing of the model");
        assertTrue(planeOf(meshes, CASING) < planeOf(meshes, PIPE),
                "the overlay lies a hair outside the face it is drawn over");
    }

    @Test
    void anOwnedSideFollowsTheStateOfItsCell() {
        put(4, 4, 4, Blocks.GRINDER);
        int east = Blocks.GRINDER.states().stateOf(Map.of("facing", "east", "lit", "false"));
        // The block entity owns its east side, and the model of a machine that faces east carries the mouth
        // of the machine onto that very side, see SectionMesher#turned.
        FaceAppearance owner = face -> face == BlockFace.EAST
                ? FacePicture.of("machine_casing/lv", FaceRole.ENERGY_OUT)
                : null;

        List<MeshData> meshes = build(east, owningTheGrinder(owner));

        assertEquals(4, cornersOfLayer(meshes, CASING),
                "the casing of the tier is drawn on the side the entity named");
        assertEquals(4, cornersOfLayer(meshes, ENERGY_OUT), "with the overlay of the power over it");
        assertEquals(0, cornersOfLayer(meshes, LAYERS.get("grinder/grinder_front")),
                "the mouth of the machine was turned onto that side and is gone with it");
        assertEquals(4, cornersOfLayer(meshes, LAYERS.get("grinder/grinder_top")),
                "while the top of the machine keeps its gear");
    }

    @Test
    void onlyACellThatCarriesABlockEntityIsAsked() {
        put(4, 4, 4, Blocks.STONE);
        put(5, 4, 4, Blocks.CHEST);
        AtomicInteger asks = new AtomicInteger();

        build(0, (x, y, z) -> {
            asks.incrementAndGet();
            return null;
        });

        assertEquals(1, asks.get(), "the chest carries a block entity and the stone does not");
    }

    /** Puts a block into the section. */
    private void put(int x, int y, int z, Block block) {
        section.setRawId(x, y, z, block.id());
    }

    /** Meshes the section with one state and the block entities that own their sides. */
    private List<MeshData> build(int state, SectionMesher.Appearances appearances) {
        return SectionMesher.build(section, 0, 0, 0, this::blockAt, (x, y, z) -> state,
                SectionMesher.NO_LIGHT, this::layer, appearances);
    }

    /** The block entities of the section, answering for the machine of the test alone. */
    private SectionMesher.Appearances owningTheGrinder(FaceAppearance owner) {
        return (x, y, z) -> x == 4 && y == 4 && z == 4 ? owner : null;
    }

    /** Layer of a picture of the test world, negative when the table does not hold it. */
    private int layer(String picture) {
        return LAYERS.getOrDefault(picture, -1);
    }

    /** The blocks of the section and of the shell around it. */
    private Block blockAt(int x, int y, int z) {
        if (Section.contains(x) && Section.contains(y) && Section.contains(z)) {
            return BlockRegistry.byId(section.rawId(x, y, z));
        }
        Block block = outside.get(x + "," + y + "," + z);
        return block == null ? Blocks.AIR : block;
    }

    /** The coordinate the corners of one layer share, which is the plane the side lies on. */
    private static float planeOf(List<MeshData> meshes, int layer) {
        float plane = Float.NaN;
        int corners = 0;
        for (MeshData mesh : meshes) {
            float[] vertices = mesh.vertexFloats();
            for (int corner = 0; corner < mesh.vertexCount(); corner++) {
                int base = corner * MeshData.FLOATS_PER_VERTEX;
                if ((int) vertices[base + 5] != layer) {
                    continue;
                }
                if (corners == 0) {
                    plane = vertices[base];
                }
                assertEquals(plane, vertices[base], 1.0e-4f, "a side lies on one plane");
                corners++;
            }
        }
        assertNotEquals(0, corners, "layer " + layer + " is drawn at all");
        return plane;
    }

    /** Amount of corners all meshes together hold. */
    private static int corners(List<MeshData> meshes) {
        int total = 0;
        for (MeshData mesh : meshes) {
            total += mesh.vertexCount();
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
}
