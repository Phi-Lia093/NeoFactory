package com.philia093.neofactory.fluid;

import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.BlockPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks where a fluid stands once it has run.
 * <p>
 * The spread is arithmetic on positions, so a made up field is enough to prove what matters: a
 * level counts the steps to the nearest source, two sources share what lies between them, a wall
 * is walked around and never through, and a fluid that may stand nowhere is not fed at all.
 */
class FluidSpreadTest {

    /** A field without a single obstacle. */
    private static final FluidSpread.Cells OPEN = (x, y) -> true;

    @BeforeAll
    static void register() {
        TestRegistries.ensure();
    }

    @Test
    void waterRunsSevenCellsFromItsSource() {
        Map<BlockPos, Integer> levels = FluidSpread.settle(List.of(BlockPos.of(0, 0)),
                Fluids.WATER, OPEN);

        assertEquals(0, levels.get(BlockPos.of(0, 0)), "the source itself");
        assertEquals(7, levels.get(BlockPos.of(7, 0)), "seven cells to the east");
        assertEquals(7, levels.get(BlockPos.of(0, -7)), "seven cells to the south");
        assertEquals(4, levels.get(BlockPos.of(2, 2)), "the level counts the steps");
        assertNull(levels.get(BlockPos.of(8, 0)), "the ring after the last one is not reached");
        assertEquals(2 * 7 * 8 + 1, levels.size(), "the diamond of a spread of seven");
    }

    @Test
    void lavaReachesThreeCells() {
        Map<BlockPos, Integer> levels = FluidSpread.settle(List.of(BlockPos.of(0, 0)),
                Fluids.LAVA, OPEN);

        assertEquals(3, levels.get(BlockPos.of(3, 0)));
        assertNull(levels.get(BlockPos.of(4, 0)), "lava is thicker than water");
        assertEquals(2 * 3 * 4 + 1, levels.size());
    }

    @Test
    void theNearestSourceWins() {
        Map<BlockPos, Integer> levels = FluidSpread.settle(
                List.of(BlockPos.of(0, 0), BlockPos.of(10, 0)), Fluids.WATER, OPEN);

        assertEquals(1, levels.get(BlockPos.of(1, 0)));
        assertEquals(1, levels.get(BlockPos.of(9, 0)));
        assertEquals(5, levels.get(BlockPos.of(5, 0)), "the middle is fed by both");
        assertEquals(0, levels.get(BlockPos.of(10, 0)), "and the second source keeps its level");
    }

    @Test
    void aWallStopsTheWaterAndIsWalkedAround() {
        // A wall at x = 2 that reaches upwards from the source, so the water has to take the
        // way around its southern end.
        FluidSpread.Cells cells = (x, y) -> x != 2 || y < 0;
        Map<BlockPos, Integer> levels = FluidSpread.settle(List.of(BlockPos.of(0, 0)),
                Fluids.WATER, cells);

        assertNull(levels.get(BlockPos.of(2, 1)), "the wall is not flooded");
        assertNull(levels.get(BlockPos.of(2, 3)), "and neither is the cell behind it");
        assertTrue(levels.containsKey(BlockPos.of(2, -2)), "the water finds the way around");
        assertTrue(levels.get(BlockPos.of(2, -2)) > 3, "and that way is longer");
    }

    @Test
    void aSourceThatMayStandNowhereFeedsNothing() {
        FluidSpread.Cells buried = (x, y) -> x != 0 || y != 0;

        Map<BlockPos, Integer> levels = FluidSpread.settle(List.of(BlockPos.of(0, 0)),
                Fluids.WATER, buried);

        assertTrue(levels.isEmpty(), "a source inside a wall is dropped");
    }

    @Test
    void aCellThatTwoSourcesBothNameIsListedOnce() {
        Map<BlockPos, Integer> levels = FluidSpread.settle(
                List.of(BlockPos.of(0, 0), BlockPos.of(0, 0), BlockPos.of(1, 0)), Fluids.WATER, OPEN);

        assertEquals(0, levels.get(BlockPos.of(0, 0)), "the source keeps its level");
        assertEquals(0, levels.get(BlockPos.of(1, 0)), "and so does the second one");
    }

    @Test
    void theNeighboursOfACellAreItsFourSides() {
        List<BlockPos> neighbours = FluidSpread.neighbours(BlockPos.of(2, 3));

        assertEquals(4, neighbours.size(), "the world is seen from above, so there are four");
        assertTrue(neighbours.contains(BlockPos.of(2, 4)), "to the north");
        assertTrue(neighbours.contains(BlockPos.of(2, 2)), "to the south");
        assertTrue(neighbours.contains(BlockPos.of(3, 3)), "to the east");
        assertTrue(neighbours.contains(BlockPos.of(1, 3)), "to the west");
    }
}
