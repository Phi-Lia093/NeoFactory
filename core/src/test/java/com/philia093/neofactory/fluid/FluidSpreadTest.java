package com.philia093.neofactory.fluid;

import com.badlogic.gdx.utils.LongMap;
import com.badlogic.gdx.utils.LongSet;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks where a fluid runs, without a world and without a window.
 * <p>
 * The walk is arithmetic on positions: it is handed the cells a fluid grows from and a question - may it
 * stand here? - and answers with a state per cell. Everything the world side is built on is checked here:
 * a fluid spreads sideways one step at a time and never climbs, and it falls for free, which is what lets
 * water run down a cliff and spread along the floor below it.
 */
class FluidSpreadTest {

    /** Every cell of an empty world, so a case only has to say what is in the way. */
    private static final FluidSpread.Cells OPEN = (x, y, z) -> true;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aSpillGrowsOneRingPerStep() {
        LongSet sources = new LongSet();
        sources.add(FluidSpread.key(0, 0, 0));

        LongMap<FluidState> settled = FluidSpread.settle(sources, Fluids.WATER, OPEN);

        assertEquals(FluidState.SOURCE, settled.get(FluidSpread.key(0, 0, 0)),
                "a source is where the water starts");
        assertEquals(FluidState.flowing(1), settled.get(FluidSpread.key(1, 0, 0)), "one step away");
        assertEquals(FluidState.flowing(2), settled.get(FluidSpread.key(2, 0, 0)), "two steps away");
        assertNull(settled.get(FluidSpread.key(Fluids.WATER.range() + 1, 0, 0)),
                "and it stops at the range of the fluid");
    }

    @Test
    void waterFallsForFreeAndSpreadsAtTheBottom() {
        // A floor at y = 0 only: everything above it is open, so the water falls from the source at y = 3
        // down to the floor and runs along it.
        FluidSpread.Cells airAboveTheFloor = (x, y, z) -> y > 0;
        LongSet sources = new LongSet();
        sources.add(FluidSpread.key(0, 3, 0));

        LongMap<FluidState> settled = FluidSpread.settle(sources, Fluids.WATER, airAboveTheFloor);

        assertNull(settled.get(FluidSpread.key(0, 0, 0)), "the floor itself holds no water");
        assertEquals(FluidState.fallen(0), settled.get(FluidSpread.key(0, 1, 0)),
                "the foot of the fall is as strong as the source above it");
        assertEquals(FluidState.flowing(1), settled.get(FluidSpread.key(1, 1, 0)),
                "and from there the water runs along the floor");
        assertEquals(FluidState.flowing(2), settled.get(FluidSpread.key(2, 1, 0)), "one ring further");
    }

    @Test
    void aFluidThatCanFallDoesNotSpreadSideways() {
        // A single hole under the source: the water goes through it instead of soaking the wall beside it.
        FluidSpread.Cells holeBelow = (x, y, z) -> y != 0 || x == 0;
        LongSet sources = new LongSet();
        sources.add(FluidSpread.key(0, 1, 0));

        LongMap<FluidState> settled = FluidSpread.settle(sources, Fluids.WATER, holeBelow);

        assertEquals(FluidState.fallen(0), settled.get(FluidSpread.key(0, 0, 0)), "it fell into the hole");
        assertNull(settled.get(FluidSpread.key(1, 1, 0)),
                "and did not run along the ground it could have fallen from");
        assertTrue(settled.containsKey(FluidSpread.key(1, 0, 0)),
                "it runs along the floor of the hole instead");
    }

    @Test
    void aFluidNeverClimbs() {
        LongSet sources = new LongSet();
        sources.add(FluidSpread.key(0, 0, 0));

        LongMap<FluidState> settled = FluidSpread.settle(sources, Fluids.WATER, OPEN);

        assertNull(settled.get(FluidSpread.key(0, 1, 0)), "nothing above the source");
        assertNull(settled.get(FluidSpread.key(0, -1, 0)),
                "and nothing below it either: falling needs an open cell under a cell of the fluid");
    }

    @Test
    void twoSourcesShareTheWaterBetweenThem() {
        LongSet sources = new LongSet();
        sources.add(FluidSpread.key(0, 0, 0));
        sources.add(FluidSpread.key(2, 0, 0));

        LongMap<FluidState> settled = FluidSpread.settle(sources, Fluids.WATER, OPEN);

        assertEquals(FluidState.SOURCE, settled.get(FluidSpread.key(0, 0, 0)));
        assertEquals(FluidState.SOURCE, settled.get(FluidSpread.key(2, 0, 0)));
        assertEquals(FluidState.flowing(1), settled.get(FluidSpread.key(1, 0, 0)),
                "the cell between them is one step from either source");
    }

    @Test
    void aSourceThatCannotStandIsDropped() {
        LongSet sources = new LongSet();
        sources.add(FluidSpread.key(0, 0, 0));

        LongMap<FluidState> settled = FluidSpread.settle(sources, Fluids.WATER, (x, y, z) -> false);

        assertTrue(settled.size == 0, "a source buried in a wall feeds nothing");
    }

    @Test
    void aPackedCellReadsBackAsTheCellItWas() {
        long cell = FluidSpread.key(-13, 70, 5);

        assertEquals(-13, FluidSpread.xOf(cell));
        assertEquals(70, FluidSpread.yOf(cell));
        assertEquals(5, FluidSpread.zOf(cell));
        assertEquals(cell, FluidSpread.key(FluidSpread.xOf(cell), FluidSpread.yOf(cell),
                FluidSpread.zOf(cell)), "and packing what was read gives the same number");
        assertEquals(FluidSpread.key(-13, 69, 5), FluidSpread.below(cell), "the cell under it");
        assertEquals(FluidSpread.key(-13, 71, 5), FluidSpread.above(cell), "and the one over it");
    }
}
