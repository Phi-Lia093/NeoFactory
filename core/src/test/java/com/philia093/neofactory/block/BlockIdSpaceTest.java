package com.philia093.neofactory.block;

import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the id space of the blocks.
 * <p>
 * A factory adds blocks by the dozen - ores, machines, pipes, cables - so two
 * things have to stay true while the game grows: the ids remain a run of numbers
 * from zero, which is what {@link Blocks#NEXT_FREE_ID} and the array lookup of
 * {@link BlockRegistry#byId(int)} are built on, and the storage is wide enough for
 * far more blocks than the 256 a single byte could hold.
 */
class BlockIdSpaceTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theIdsAreARunWithoutAGap() {
        assertEquals(Blocks.NEXT_FREE_ID, BlockRegistry.count(),
                "a block was registered without bumping NEXT_FREE_ID");
    }

    @Test
    void theStorageHoldsMoreThanAByteOfIds() {
        assertTrue(BlockRegistry.MAX_BLOCKS > 0xFF,
                "the id space is still limited to a single byte");

        Block last = BlockRegistry.byId(Blocks.NEXT_FREE_ID - 1);
        assertEquals(Blocks.NEXT_FREE_ID - 1, last.id(), "the last block lost its id");
    }
}
