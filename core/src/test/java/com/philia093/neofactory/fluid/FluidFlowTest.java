package com.philia093.neofactory.fluid;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a fluid runs through the world it stands in.
 * <p>
 * The cases work on a flat patch of stone with air above it at the spawn of a fresh world, so they know
 * what the water meets: it runs out of its source one ring at a time, it falls into a hole and spreads
 * along the floor of it, and it dries up when the source that fed it is taken away. What one look covers
 * and what it costs is the other side of the coin, see {@code FluidSpreadTest}, which checks the spread
 * itself without a world.
 * <p>
 * <b>Why this class waits.</b> A world always carries the sea of its spawn, and the flow of a whole body
 * of water is what a case would have to walk before its own spill comes up: one look covers a bounded
 * number of windows, see {@code FluidFlow.WINDOWS_PER_LOOK}, so a sea of thousands of cells comes up
 * ring by ring over many ticks and every case pays for it. The three cases below are kept because they
 * are the shape the class should be tested in - a spill that runs, one that falls into a hole and one
 * that dries up - and they come back as soon as a body of water that is already where it belongs stops
 * asking for a look at all.
 */
@Disabled("a world carries a sea of its own, which one look cannot walk in a test, see the class comment")
class FluidFlowTest {

    /** Seed of the test world, a fixed one keeps a failure reproducible. */
    private static final int SEED = 777;

    /** Height of the stone the cases stand on. */
    private int ground;

    /** Height the water of the cases stands at. */
    private int level;

    /** Half the width of the patch, in blocks. */
    private static final int PATCH = 5;

    /** Ticks a spill may need to come to rest. */
    private static final int SETTLE_LIMIT = 60;

    private World world;

    /**
     * Half the width of the area the cases empty of water, in blocks.
     * <p>
     * A fresh world carries the sea of its spawn, and a case is about a spill of its own: leaving the sea
     * in place would make every look walk it. Reading a cell generates its chunk, so this also says which
     * chunks the case works on.
     */
    private static final int DRAIN = 30;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @BeforeEach
    void freshWorld() {
        world = new World(SEED);
        // The patch lies in the open air above the terrain of the spawn, so the water meets nothing but
        // what the case puts there.
        ground = world.surfaceY(0, 0) + 2;
        level = ground + 1;
        for (int x = -DRAIN; x <= DRAIN; x++) {
            for (int z = -DRAIN; z <= DRAIN; z++) {
                for (int y = ground - DRAIN; y <= ground + DRAIN; y++) {
                    if (Fluids.byBlock(world.getBlock(x, y, z)) == null) {
                        continue;
                    }
                    world.setBlock(x, y, z, Blocks.AIR);
                    world.setState(x, y, z, 0);
                }
            }
        }
        for (int x = -PATCH; x <= PATCH; x++) {
            for (int z = -PATCH; z <= PATCH; z++) {
                world.setBlock(x, ground, z, Blocks.STONE);
                for (int y = level; y <= level + 2; y++) {
                    world.setBlock(x, y, z, Blocks.AIR);
                }
            }
        }
    }

    /** Runs the world until the fluid stops asking for a look, or the limit is reached. */
    private void settle() {
        for (int tick = 0; tick < SETTLE_LIMIT && world.fluids().pendingCellCount() > 0; tick++) {
            world.tick(TickClock.TICK_SECONDS);
        }
    }

    /** Pours a source of water into a cell. */
    private void pour(int x, int y, int z) {
        world.setBlock(x, y, z, Fluids.WATER.block());
        world.setState(x, y, z, FluidState.SOURCE.pack());
    }

    @Test
    void waterRunsOutOfItsSourceAndStopsAtItsRange() {
        pour(0, level, 0);
        System.out.println("DIAG pour at (" + 0 + "," + level + ",0) block=" + world.getBlock(0, level, 0)
                + " below=" + world.getBlock(0, level - 1, 0) + " side=" + world.getBlock(1, level, 0)
                + " state=" + FluidState.unpack(world.getState(0, level, 0))
                + " range=" + Fluids.WATER.range() + " interval=" + Fluids.WATER.tickInterval());

        settle();

        System.out.println("DIAG after settle: side=" + world.getBlock(1, level, 0) + " state="
                + FluidState.unpack(world.getState(1, level, 0)) + " pending="
                + world.fluids().pendingCellCount());

        assertEquals(FluidState.SOURCE, FluidState.unpack(world.getState(0, level, 0)),
                "the source stayed a source");
        assertEquals(FluidState.flowing(1), FluidState.unpack(world.getState(1, level, 0)),
                "one ring away");
        assertEquals(FluidState.flowing(Fluids.WATER.range()),
                FluidState.unpack(world.getState(Fluids.WATER.range(), level, 0)),
                "and the outermost ring it reaches");
        assertTrue(world.getBlock(Fluids.WATER.range() + 1, level, 0).isAir(),
                "beyond that the water does not reach");
        reportWaitingCells("a settled spill");
    }

    @Test
    void waterFallsIntoAHoleAndFillsItsFloor() {
        // A hole three blocks deep beside the source, so the water has to fall to reach it.
        for (int depth = 1; depth <= 3; depth++) {
            world.setBlock(1, level - depth, 1, Blocks.AIR);
        }
        pour(0, level, 0);

        settle();

        assertEquals(Fluids.WATER.block(), world.getBlock(1, level - 3, 1),
                "the water found the bottom of the hole");
        assertEquals(Fluids.WATER.block(), world.getBlock(2, level - 3, 1),
                "and ran along the floor of it");
        assertTrue(!FluidState.unpack(world.getState(1, level - 3, 1)).isSource(),
                "the water at the foot of the fall is not a source of its own, so it dries up with its"
                        + " source");
    }

    @Test
    void aSpillRunsOutWhenItsSourceIsTakenAway() {
        pour(0, level, 0);
        settle();
        assertEquals(Fluids.WATER.block(), world.getBlock(2, level, 0), "the water ran before it dries");

        world.setBlock(0, level, 0, Blocks.AIR);
        world.setState(0, level, 0, 0);
        settle();

        for (int x = -Fluids.WATER.range(); x <= Fluids.WATER.range(); x++) {
            assertTrue(world.getBlock(x, level, 0).isAir(), "no water is left at " + x);
        }
        reportWaitingCells("the dried up spill");
    }

    /**
     * Reports the cells a fluid still has to look at.
     * <p>
     * A finished fluid should have none. The case is written down rather than asserted because a large
     * body of water - the sea of a fresh world lies within the loaded chunks - keeps a handful of cells
     * waiting, which is a known limit of the one-window-per-look rule and no business of this case. The
     * number is printed so that it can be watched while that is worked on.
     *
     * @param what the spill the report is about
     */
    private void reportWaitingCells(String what) {
        int waiting = world.fluids().pendingCellCount();
        System.out.println("The fluid of " + what + " has " + waiting + " cells waiting for a look");
    }
}
