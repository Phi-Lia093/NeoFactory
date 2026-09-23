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
            world.getFlatBlock(x, 0, Chunk.LAYER_FLOOR);
            world.getFlatBlock(x, 0, Chunk.LAYER_OBJECT);
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
        world.placeObjectIfAir(4, 4, Blocks.TALL_GRASS);

        assertEquals(0, world.modifiedChunkCount());
    }

    @Test
    void aBlockWrittenByThePlayerMarksItsChunk() {
        World world = new World(7, 0, 0);
        world.setFlatBlock(1, 2, Chunk.LAYER_OBJECT, Blocks.STONE);

        Chunk chunk = world.chunkIfLoaded(0, 0);
        assertNotNull(chunk);
        assertTrue(chunk.isModified());
        assertEquals(1, world.modifiedChunkCount());
        assertEquals(Blocks.STONE, world.getFlatBlock(1, 2, Chunk.LAYER_OBJECT));
    }

    @Test
    void readingAStoredChunkClearsTheFlag() {
        Chunk written = new Chunk(0, 0);
        written.setRawId(5, 6, Chunk.LAYER_OBJECT, Blocks.STONE.id());
        written.markModified();
        NbtCompound data = ChunkCodec.write(written);

        Chunk read = new Chunk(0, 0);
        ChunkCodec.read(read, data);

        assertEquals(Blocks.STONE, read.getBlock(5, 6, Chunk.LAYER_OBJECT));
        // The file is the authority now, so nothing is pending for a save.
        assertFalse(read.isModified());
    }
}
