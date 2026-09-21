package com.philia093.neofactory.world.save;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtIntArray;
import com.philia093.neofactory.world.Chunk;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks what a stored chunk holds.
 * <p>
 * Two things about the format are easy to get wrong and are pinned down here: a
 * block id is a full 32 bit number, so an id beyond the range of a byte has to
 * survive writing instead of being cut off, and the state of a cell travels with
 * its block, so a cell that was stored with a state comes back with it. The version
 * of the format is checked as well: a file of another version has to be refused
 * instead of half read.
 */
class ChunkCodecTest {

    /** Cell the tests write to, inside the first chunk. */
    private static final int CELL_X = 3;

    /** Second coordinate of the cell the tests write to. */
    private static final int CELL_Y = 4;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aBlockIdBeyondAByteSurvivesWriting() {
        Chunk chunk = new Chunk(0, 0);
        // An id no block uses: writing takes the number as it is, so the stored data
        // shows whether it was kept whole or cut down to its lowest byte.
        chunk.setRawId(CELL_X, CELL_Y, Chunk.LAYER_OBJECT, 300);

        NbtIntArray blocks = ChunkCodec.write(chunk).getIntArray(ChunkCodec.TAG_BLOCKS);

        assertEquals(300, blocks.get(storedIndex(CELL_X, CELL_Y, Chunk.LAYER_OBJECT)),
                "the block id was cut off while writing");
    }

    @Test
    void aBlockAndItsStateComeBack() {
        Chunk written = new Chunk(0, 0);
        written.setRawId(CELL_X, CELL_Y, Chunk.LAYER_OBJECT, Blocks.STONE.id());
        written.setMeta(CELL_X, CELL_Y, Chunk.LAYER_OBJECT, 129);

        Chunk read = new Chunk(0, 0);
        ChunkCodec.read(read, ChunkCodec.write(written));

        assertEquals(Blocks.STONE, read.getBlock(CELL_X, CELL_Y, Chunk.LAYER_OBJECT));
        assertEquals(129, read.meta(CELL_X, CELL_Y, Chunk.LAYER_OBJECT));
    }

    @Test
    void aCellLosesItsStateWhenItsBlockChanges() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setRawId(CELL_X, CELL_Y, Chunk.LAYER_OBJECT, Blocks.STONE.id());
        chunk.setMeta(CELL_X, CELL_Y, Chunk.LAYER_OBJECT, 3);

        chunk.setRawId(CELL_X, CELL_Y, Chunk.LAYER_OBJECT, Blocks.DIRT.id());

        assertEquals(0, chunk.meta(CELL_X, CELL_Y, Chunk.LAYER_OBJECT),
                "the state of the block that was there stayed behind");
    }

    @Test
    void aChunkOfAnotherFormatIsRefused() {
        NbtCompound data = ChunkCodec.write(new Chunk(0, 0));
        data.putInt(SaveTags.DATA_VERSION, SaveFormat.DATA_VERSION + 1);

        assertThrows(SaveException.class, () -> ChunkCodec.read(new Chunk(0, 0), data));
    }

    @Test
    void aChunkWithoutStateIsRefused() {
        NbtCompound data = ChunkCodec.write(new Chunk(0, 0));
        data.remove(ChunkCodec.TAG_META);

        assertThrows(SaveException.class, () -> ChunkCodec.read(new Chunk(0, 0), data));
    }

    /**
     * Index a cell has inside the block array of a written chunk.
     *
     * @param localX local X coordinate
     * @param localY local Y coordinate
     * @param layer layer index
     * @return the index inside the stored array
     */
    private static int storedIndex(int localX, int localY, int layer) {
        return layer * Chunk.CELL_COUNT + localY * Constants.CHUNK_SIZE + localX;
    }
}
