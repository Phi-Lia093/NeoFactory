package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what marking a cell as out of date does.
 * <p>
 * A block entity changes what a cell looks like without changing the cell itself: a machine that was turned
 * shows another picture on its sides and a side that was given another job wears another overlay, see
 * {@code FaceAppearance}. The block and the state of that cell stay what they were, so the world is told
 * about the change instead of being written to, and it has to draw the cell again and to store it again.
 */
class WorldMarkDirtyTest {

    private static final int SEED = 4711;
    private static final int X = 3;
    private static final int Y = 200;
    private static final int Z = 5;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void markingACellMeshesItsSectionAgain() {
        World world = new World(SEED, 0, 0);
        world.setBlock(X, Y, Z, Blocks.BRONZE_BOILER);
        Chunk chunk = world.chunkIfLoaded(Chunk.chunkOf(X), Chunk.chunkOf(Z));
        assertNotNull(chunk, "writing a block brings the chunk of the cell into being");
        Section section = chunk.section(Y / Section.SIZE);
        assertNotNull(section, "the block lies in a section of that chunk");

        // A section that is drawn is not out of date, which is the state the machine usually stands in.
        section.clearDirty();
        chunk.clearModified();

        world.markDirty(X, Y, Z);

        assertTrue(section.isDirty(), "the section is meshed again");
        assertTrue(chunk.isModified(), "and the change reaches the save game");
    }

    @Test
    void markingACellOfAChunkThatIsNotLoadedDoesNothing() {
        World world = new World(SEED, 0, 0);
        // The chunks around the spawn are prepared by the constructor, so the cell of this test lies in a
        // chunk of the far end of the world, which nobody has looked at yet.
        int farX = X + 4096;
        int farZ = Z + 4096;
        assertNull(world.chunkIfLoaded(Chunk.chunkOf(farX), Chunk.chunkOf(farZ)),
                "the chunk of the cell is not in memory");

        world.markDirty(farX, Y, farZ);

        assertEquals(0, world.modifiedChunkCount(), "no chunk was loaded and none was marked");
    }
}
