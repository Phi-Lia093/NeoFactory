package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.save.ChunkCodec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what makes a chunk a chunk the player changed.
 * <p>
 * Everything about dropping a chunk hangs on this flag: a chunk that is marked
 * although the generator produced it is written on every save and never leaves
 * memory, and a chunk that is not marked although the player built in it loses the
 * change as soon as it is dropped. Both mistakes are silent, so they are pinned
 * down here.
 */
class ChunkModificationTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void generatedTerrainIsNotAPlayerChange() {
        World world = new World(4242, 0, 0);
        for (int x = -20; x <= 20; x++) {
            world.getBlock(x, 64, 0);
            world.getBlock(x, 65, 0);
        }
        world.ensureChunksAround(0, 0, 3);

        assertEquals(0, world.modifiedChunkCount());
        for (Chunk chunk : world.chunks()) {
            assertFalse(chunk.isModified());
        }
    }

    @Test
    void decorationsAreNotAPlayerChange() {
        World world = new World(99, 0, 0);
        // The decoration path writes into the object layer of a chunk that may not
        // be generated yet, but it is still the seed talking.
        world.setObjectBlock(3, 3, Blocks.LEAVES_OAK);
        world.placeObjectIfAir(4, 4, Blocks.SAPLING_OAK);

        assertEquals(0, world.modifiedChunkCount());
    }

    @Test
    void aBlockWrittenByThePlayerMarksItsChunk() {
        World world = new World(7, 0, 0);
        world.setBlock(1, 65, 2, Blocks.STONE);

        Chunk chunk = world.chunkIfLoaded(0, 0);
        assertNotNull(chunk);
        assertTrue(chunk.isModified());
        assertEquals(1, world.modifiedChunkCount());
        assertEquals(Blocks.STONE, world.getBlock(1, 65, 2));
    }

    @Test
    void aStateWrittenByThePlayerMarksItsChunk() {
        // A state is a change like a block is: the sides a pipe is joined on live in the state of its cell,
        // so a chunk that carries a turned pipe has to reach the save game. A state that is written without
        // the mark is lost as soon as the chunk leaves memory, which is silent and therefore pinned here.
        World world = new World(11, 0, 0);
        world.setBlock(3, 65, 1, Blocks.FURNACE);
        Chunk chunk = world.chunkIfLoaded(0, 0);
        assertNotNull(chunk);
        // The chunk of the block is marked by the block itself; what is asked here is the state alone.
        chunk.clearModified();

        world.setState(3, 65, 1, 5);

        assertTrue(chunk.isModified(), "a written state has to reach the save game");
        assertEquals(5, world.getState(3, 65, 1));
        assertEquals(1, world.modifiedChunkCount());

        // The same state written again is not a change: a system that rewrites the state of a cell every
        // tick must not keep the chunk on the list of what has to be saved.
        chunk.clearModified();
        world.setState(3, 65, 1, 5);

        assertEquals(0, world.modifiedChunkCount(), "writing the same state again costs nothing");
    }

    @Test
    void readingAStoredChunkClearsTheFlag() {
        Chunk written = new Chunk(0, 0);
        written.setRawId(5, 6, 1, Blocks.STONE.id());
        written.markModified();
        NbtCompound data = ChunkCodec.write(written);

        Chunk read = new Chunk(0, 0);
        ChunkCodec.read(read, data);

        assertEquals(Blocks.STONE, read.getBlock(5, 6, 1));
        // The file is the authority now, so nothing is pending for a save.
        assertFalse(read.isModified());
    }
}
