package com.philia093.neofactory.world.save;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.nbt.NbtByteArray;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtList;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.Section;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a stored chunk holds.
 * <p>
 * A world of cubes stores the column of a chunk as the sections it is built from, so the questions
 * here are about heights: does a cell keep the height it was stored at, does a section that holds
 * nothing stay out of the file, and does the state of a cell travel with its block. The version of
 * the format is checked as well: a file of another version has to be refused instead of half read.
 */
class ChunkCodecTest {

    /** Column the tests write to, inside the first chunk. */
    private static final int CELL_X = 3;

    /** Second column coordinate of the cell the tests write to. */
    private static final int CELL_Z = 4;

    /** Height of the ground section, where the flat engine kept its floor. */
    private static final int FLOOR_Y = 64;

    /** Height of a section high above the ground, which a flat world never reached. */
    private static final int HIGH_Y = 200;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aBlockKeepsTheHeightItWasStoredAt() {
        Chunk written = new Chunk(0, 0);
        written.setRawId(CELL_X, HIGH_Y, CELL_Z, Blocks.STONE.id());

        Chunk read = roundTrip(written);

        assertEquals(Blocks.STONE, read.getBlock(CELL_X, HIGH_Y, CELL_Z));
        assertEquals(HIGH_Y, read.highestBlockY(CELL_X, CELL_Z),
                "the height map of the column lost the block");
    }

    @Test
    void aBlockIdBeyondAByteSurvivesWriting() {
        Chunk chunk = new Chunk(0, 0);
        // An id no block uses: the palette keeps the number as it is, so the stored data
        // shows whether it was kept whole or cut down to its lowest byte.
        chunk.setRawId(CELL_X, FLOOR_Y, CELL_Z, 300);

        assertTrue(storedPalette(chunk).contains(300), "the block id was cut off while writing");
    }

    @Test
    void aSectionThatHoldsNothingIsNotWritten() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setRawId(CELL_X, FLOOR_Y, CELL_Z, Blocks.STONE.id());

        NbtList sections = ChunkCodec.write(chunk).getList(ChunkCodec.TAG_SECTIONS);

        assertEquals(1, sections.size(), "a section of nothing but air reached the file");
        assertEquals(FLOOR_Y / Section.SIZE,
                sections.getCompound(0).getInt(ChunkCodec.TAG_SECTION_Y, -1));
    }

    @Test
    void aBlockAndItsStateComeBack() {
        Chunk written = new Chunk(0, 0);
        written.setRawId(CELL_X, FLOOR_Y, CELL_Z, Blocks.STONE.id());
        written.setState(CELL_X, FLOOR_Y, CELL_Z, 129);

        Chunk read = roundTrip(written);

        assertEquals(Blocks.STONE, read.getBlock(CELL_X, FLOOR_Y, CELL_Z));
        assertEquals(129, read.state(CELL_X, FLOOR_Y, CELL_Z));
    }

    @Test
    void aStateOfASectionHighAboveTheGroundIsKept() {
        Chunk written = new Chunk(0, 0);
        written.setRawId(CELL_X, HIGH_Y, CELL_Z, Blocks.STONE.id());
        written.setState(CELL_X, HIGH_Y, CELL_Z, 5);

        Chunk read = roundTrip(written);

        assertEquals(5, read.state(CELL_X, HIGH_Y, CELL_Z));
    }

    @Test
    void twoSectionsOfOneColumnKeepTheirHeights() {
        Chunk written = new Chunk(0, 0);
        written.setRawId(CELL_X, FLOOR_Y, CELL_Z, Blocks.DIRT.id());
        written.setRawId(CELL_X, HIGH_Y, CELL_Z, Blocks.STONE.id());

        Chunk read = roundTrip(written);

        assertEquals(Blocks.DIRT, read.getBlock(CELL_X, FLOOR_Y, CELL_Z));
        assertEquals(Blocks.STONE, read.getBlock(CELL_X, HIGH_Y, CELL_Z));
        assertEquals(Blocks.AIR, read.getBlock(CELL_X, FLOOR_Y + 1, CELL_Z),
                "a block was read back into the wrong layer of the column");
    }

    @Test
    void aCellLosesItsStateWhenItsBlockChanges() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setRawId(CELL_X, FLOOR_Y, CELL_Z, Blocks.STONE.id());
        chunk.setState(CELL_X, FLOOR_Y, CELL_Z, 3);

        chunk.setRawId(CELL_X, FLOOR_Y, CELL_Z, Blocks.DIRT.id());

        assertEquals(0, chunk.state(CELL_X, FLOOR_Y, CELL_Z),
                "the state of the block that was there stayed behind");
    }

    @Test
    void anIdTheGameDoesNotKnowBecomesAir() {
        Chunk written = new Chunk(0, 0);
        // A number no block of the game uses: reading it back would put a block where nothing is.
        written.setRawId(CELL_X, FLOOR_Y, CELL_Z, 5000);

        Chunk read = roundTrip(written);

        assertEquals(Blocks.AIR, read.getBlock(CELL_X, FLOOR_Y, CELL_Z));
    }

    @Test
    void aChunkOfAnotherFormatIsRefused() {
        NbtCompound data = ChunkCodec.write(new Chunk(0, 0));
        data.putInt(SaveTags.DATA_VERSION, SaveFormat.DATA_VERSION + 1);

        assertThrows(SaveException.class, () -> ChunkCodec.read(new Chunk(0, 0), data));
    }

    @Test
    void aChunkWithoutSectionsIsRefused() {
        NbtCompound data = ChunkCodec.write(new Chunk(0, 0));
        data.remove(ChunkCodec.TAG_SECTIONS);

        assertThrows(SaveException.class, () -> ChunkCodec.read(new Chunk(0, 0), data));
    }

    @Test
    void aSectionWithADamagedArrayIsRefused() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setRawId(CELL_X, FLOOR_Y, CELL_Z, Blocks.STONE.id());
        NbtCompound data = ChunkCodec.write(chunk);
        // A data array that cannot hold one index per cell of its section.
        NbtCompound blocks = data.getList(ChunkCodec.TAG_SECTIONS).getCompound(0)
                .getCompound(ChunkCodec.TAG_BLOCKS);
        blocks.put(new NbtByteArray(PackedValues.TAG_DATA, new byte[] {1, 2, 3}));

        assertThrows(SaveException.class, () -> ChunkCodec.read(new Chunk(0, 0), data));
    }

    /** Writes a chunk and reads it back into a fresh one of the same coordinates. */
    private static Chunk roundTrip(Chunk chunk) {
        Chunk read = new Chunk(chunk.chunkX(), chunk.chunkZ());
        ChunkCodec.read(read, ChunkCodec.write(chunk));
        return read;
    }

    /** Every value the palette of the first stored section holds. */
    private static List<Integer> storedPalette(Chunk chunk) {
        NbtCompound blocks = ChunkCodec.write(chunk).getList(ChunkCodec.TAG_SECTIONS)
                .getCompound(0).getCompound(ChunkCodec.TAG_BLOCKS);
        int[] palette = blocks.getIntArray(PackedValues.TAG_PALETTE).toArray();
        List<Integer> values = new ArrayList<>();
        for (int value : palette) {
            values.add(value);
        }
        return values;
    }
}
