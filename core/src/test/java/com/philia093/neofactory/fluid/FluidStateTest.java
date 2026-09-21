package com.philia093.neofactory.fluid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the state a cell of a fluid carries.
 * <p>
 * The state is packed into the same 32 bit number a chunk keeps for every block, so the walk
 * there and back has to be exact: a level that is read as a source, or the other way round,
 * would turn a lake into a stone that never runs or into a spring that never stops.
 */
class FluidStateTest {

    @Test
    void aStateTravelsThroughTheBlockState() {
        for (int level = 0; level <= 12; level++) {
            FluidState flowing = FluidState.flowing(level + 1);
            FluidState read = FluidState.unpack(flowing.pack());

            assertEquals(flowing.level(), read.level(), "level " + level);
            assertFalse(read.isSource());
        }
    }

    @Test
    void aSourceIsMarkedAsOne() {
        FluidState read = FluidState.unpack(FluidState.SOURCE.pack());

        assertTrue(read.isSource());
        assertEquals(0, read.level());
        assertEquals(0, FluidState.SOURCE.level());
    }

    @Test
    void aLevelAndTheSourceFlagDoNotShareBits() {
        // The level uses the lower byte, the flag the ninth bit, so the deepest level of a
        // fluid never looks like a source.
        assertEquals(0xFF, FluidState.flowing(0xFF).pack());
        assertEquals(0x100, FluidState.SOURCE.pack());
        assertFalse(FluidState.unpack(0xFF).isSource());
        assertTrue(FluidState.unpack(0x100).isSource());
    }

    @Test
    void anUntouchedCellReadsAsAnEmptyState() {
        FluidState untouched = FluidState.unpack(0);

        assertEquals(0, untouched.level());
        assertFalse(untouched.isSource());
    }

    @Test
    void aBrokenStateIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new FluidState(-1, false));
        assertThrows(IllegalArgumentException.class, () -> new FluidState(256, false));
        assertThrows(IllegalArgumentException.class, () -> new FluidState(1, true),
                "a source is never away from itself");
        assertThrows(IllegalArgumentException.class, () -> FluidState.flowing(0),
                "a flowing cell is at least one step from its source");
    }

    @Test
    void aStateHasAReadableName() {
        assertEquals("FluidState(source)", FluidState.SOURCE.toString());
        assertEquals("FluidState(level 4)", FluidState.flowing(4).toString());
    }
}
