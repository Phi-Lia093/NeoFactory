package com.philia093.neofactory.render;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.block.model.ModelRegistry;
import com.philia093.neofactory.block.state.BlockStateRegistry;
import com.philia093.neofactory.pipe.PipeSize;
import com.philia093.neofactory.pipe.Pipes;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.Section;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the mesh a pipe is drawn from where it stands for an item: in the hand, on the ground and in a
 * slot.
 * <p>
 * {@code PipeModelTest} reads the boxes of the model files and {@code PipeIconMeshTest} goes one step
 * further: it meshes the very block in the very state a slot asks for - the straight length of a pipe,
 * see {@code Block#itemState} - with the mesher the world and the icon of a block are built from. That is
 * the path a player sees, and it is the path that has to hold the thin tube of a pipe: a mesh that came out
 * empty or as a whole cube is a pipe that is drawn like a block, which is what a slot must never show.
 */
class PipeIconMeshTest {

    /** A cell of a section, the one block of the test stands in. */
    private static final int CELL = 0;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    /** Meshes one pipe in one of its states, the way a cell of the world and an item are meshed. */
    private static List<MeshData> meshOf(Pipes.Pipe pipe, int state) {
        Section section = new Section(CELL);
        Block block = pipe.block();
        section.setRawId(0, 0, 0, block.id());
        section.setState(0, 0, 0, state);
        // The very call an icon, a drop and the hand are meshed with, see ItemCubeMeshes#build(Block): it
        // reads the state of the cell out of the section, which is what this test pins down.
        return SectionMesher.build(section, 0, 0, 0,
                (x, y, z) -> x == 0 && y == 0 && z == 0 ? block : Blocks.AIR,
                name -> name == null ? -1 : 0);
    }

    /** Meshes one pipe the way an item of it is meshed, see {@code ItemCubeMeshes#build(Block)}. */
    private static List<MeshData> meshOf(Pipes.Pipe pipe) {
        return meshOf(pipe, pipe.block().itemState());
    }

    @Test
    void everyStateOfAPipeIsMeshedWithTheModelOfItsOwnMask() {
        for (Pipes.Pipe pipe : Pipes.all()) {
            BlockStateRegistry.Shown straight = BlockStateRegistry.shown(pipe.block(), Pipes.STRAIGHT_MASK);
            assertSame(ModelRegistry.byName(
                    Pipes.modelName(pipe.material().texture(), pipe.size(), Pipes.STRAIGHT_MASK)),
                    straight.model(), pipe + " does not show the model of a straight run");
            assertEquals(90 * Pipes.turnsToDraw(Pipes.STRAIGHT_MASK), straight.rotateY(),
                    pipe + " shows its model without the turn of its state");

            float[] alone = extentOf(meshOf(pipe, 0), 2);
            float[] run = extentOf(meshOf(pipe, Pipes.STRAIGHT_MASK), 2);
            float[] all = extentOf(meshOf(pipe, Pipes.ALL_MASK), 0);
            assertEquals(pipe.size().thickness() / (float) PipeSize.UNITS, alone[1] - alone[0], 0.001f,
                    pipe + " is not meshed as the stub of its size when nothing is joined");
            assertEquals(1.0f, run[1] - run[0], 0.001f,
                    pipe + " is not meshed as a straight run when two sides are joined");
            assertEquals(1.0f, all[1] - all[0], 0.001f,
                    pipe + " is not meshed across its whole cell when every side is joined");
        }
    }

    /** How far the mesh of a pipe reaches along one axis, as a pair of least and largest coordinate. */
    private static float[] extentOf(List<MeshData> meshes, int axis) {
        float least = Float.MAX_VALUE;
        float largest = -Float.MAX_VALUE;
        for (MeshData mesh : meshes) {
            float[] floats = mesh.vertexFloats();
            int used = mesh.vertexCount() * MeshData.FLOATS_PER_VERTEX;
            for (int at = 0; at + MeshData.FLOATS_PER_VERTEX <= used; at += MeshData.FLOATS_PER_VERTEX) {
                least = Math.min(least, floats[at + axis]);
                largest = Math.max(largest, floats[at + axis]);
            }
        }
        return new float[] { least, largest };
    }

    @Test
    void theMeshOfAPipeItemIsTheTubeOfItsSizeAndNotABlock() {
        for (Pipes.Pipe pipe : Pipes.all()) {
            List<MeshData> meshes = meshOf(pipe);
            int vertices = 0;
            for (MeshData mesh : meshes) {
                vertices += mesh.vertexCount();
            }
            assertTrue(vertices > 0, pipe + " is meshed into nothing at all, so a slot stays empty");

            // The item of a pipe is the straight run of its mask, which joins north and south: the tube
            // therefore runs the length of the cell along Z and is only as wide as the size says across X
            // and Y. A pipe that was meshed as the whole block it stands in would fill every axis.
            float width = pipe.size().thickness() / (float) PipeSize.UNITS;
            for (int axis = 0; axis <= 1; axis++) {
                float[] across = extentOf(meshes, axis);
                assertEquals(width, across[1] - across[0], 0.001f,
                        pipe + " is not as wide as its size");
                assertEquals(0.5f, (across[0] + across[1]) * 0.5f, 0.001f,
                        pipe + " is not centred in its cell");
                assertFalse(across[0] < 0.0f, pipe + " reaches outside its cell");
                assertTrue(across[1] <= 1.0f, pipe + " reaches outside its cell");
            }
            float[] along = extentOf(meshes, 2);
            assertEquals(0.0f, along[0], 0.001f, pipe + " does not reach one wall of its cell");
            assertEquals(1.0f, along[1], 0.001f, pipe + " does not reach the other wall of its cell");
        }
    }

    @Test
    void theMeshOfAPipeItemIsAStraightLengthWithItsArms() {
        Pipes.Pipe pipe = Pipes.of(com.philia093.neofactory.pipe.PipeMaterials.COPPER, PipeSize.SMALL);
        List<MeshData> meshes = meshOf(pipe);

        // A straight run joins north and south, and the arms of the tube reach both of those walls, see
        // Pipes#STRAIGHT_MASK.
        float[] along = extentOf(meshes, 2);
        assertEquals(0.0f, along[0], 0.001f, "the tube does not reach the north wall");
        assertEquals(1.0f, along[1], 0.001f, "the tube does not reach the south wall");
    }
}
