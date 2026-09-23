package com.philia093.neofactory.fluid;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the fluids of a world: that a spill runs ring by ring, stops at the range of its fluid,
 * is held back by a wall and disappears together with the source it lived from.
 * <p>
 * The tests work on a real {@link World} with a patch of empty ground at its spawn, which is the
 * only piece of this that cannot be made up: a fluid reaches through chunks, it uses the state of
 * the blocks and it is advanced by the tick of the world.
 */
class FluidFlowTest {

    /** Layer the fluids of the tests stand in: the one the player stands in. */
    private static final int LAYER = Chunk.LAYER_OBJECT;

    /** Seed of the test world, a fixed one keeps a failure reproducible. */
    private static final int SEED = 4242;

    /** Half the side of the empty patch, big enough for water of seven cells. */
    private static final int PATCH = 12;

    /** Ticks one test may spend on letting a spill settle. */
    private static final int SETTLE_LIMIT = 600;

    private World world;

    @BeforeAll
    static void register() {
        TestRegistries.ensure();
    }

    @BeforeEach
    void freshWorld() {
        world = new World(SEED);
        clearPatch();
    }

    @Test
    void waterRunsSevenCellsFromItsSource() {
        pour(Fluids.WATER, 0, 0);
        settle();

        assertTrue(FluidState.unpack(metaAt(0, 0)).isSource(), "the source kept its flag");
        assertEquals(Fluids.WATER.block(), blockAt(7, 0), "seven cells to the east");
        assertEquals(7, FluidState.unpack(metaAt(7, 0)).level(), "the level counts the steps");
        assertEquals(Blocks.AIR, blockAt(8, 0), "and not one cell farther");
    }

    @Test
    void aSpillRunsRingByRing() {
        pour(Fluids.WATER, 0, 0);

        for (int tick = 0; tick < Fluids.WATER.tickInterval() - 1; tick++) {
            world.tick(TickClock.TICK_SECONDS);
        }
        assertEquals(Blocks.AIR, blockAt(1, 0), "no ring appears before its time");

        world.tick(TickClock.TICK_SECONDS);

        assertEquals(Fluids.WATER.block(), blockAt(1, 0), "the first ring arrived");
        assertEquals(Blocks.AIR, blockAt(2, 0), "the second one still waits");
    }

    @Test
    void lavaReachesThreeCells() {
        pour(Fluids.LAVA, 0, 0);
        settle();

        assertEquals(Fluids.LAVA.block(), blockAt(3, 0), "three cells to the east");
        assertEquals(Blocks.AIR, blockAt(4, 0), "lava is thicker than water");
    }

    @Test
    void aWallHoldsTheWaterBack() {
        for (int y = -PATCH; y <= PATCH; y++) {
            world.setBlock(3, Chunk.flatY(LAYER), y, Blocks.STONE);
        }
        pour(Fluids.WATER, 0, 0);
        settle();

        assertEquals(Blocks.STONE, blockAt(3, 0), "the wall stands");
        assertEquals(Fluids.WATER.block(), blockAt(2, 0), "the water reached the wall");
        assertEquals(Blocks.AIR, blockAt(4, 0), "and nothing ran through");
    }

    @Test
    void takingTheSourceBackDrainsTheSpill() {
        pour(Fluids.WATER, 0, 0);
        settle();
        assertEquals(Fluids.WATER.block(), blockAt(5, 0), "the spill is there");

        // A bucket over the source takes the block with it.
        world.setBlock(0, Chunk.flatY(LAYER), 0, Blocks.AIR);
        world.setState(0, Chunk.flatY(LAYER), 0, 0);
        settle();

        assertEquals(Blocks.AIR, blockAt(0, 0), "the source is gone");
        assertEquals(0, fluidCells(), "and not a drop was left behind");
    }

    @Test
    void waterWithoutASourceDriesUp() {
        world.setBlock(0, Chunk.flatY(LAYER), 0, Fluids.WATER.block());
        world.setState(0, Chunk.flatY(LAYER), 0, FluidState.flowing(3).pack());

        settle();

        assertEquals(Blocks.AIR, blockAt(0, 0), "a fluid that no source feeds disappears");
    }

    @Test
    void aPlantIsFlooded() {
        world.setBlock(2, Chunk.flatY(LAYER), 0, Blocks.TALL_GRASS);
        pour(Fluids.WATER, 0, 0);
        settle();

        assertEquals(Fluids.WATER.block(), blockAt(2, 0), "the grass is under water");
    }

    @Test
    void aLakeOfWaterNeverCoversLava() {
        pour(Fluids.LAVA, 4, 0);
        pour(Fluids.WATER, -4, 0);
        settle();

        assertEquals(Fluids.LAVA.block(), blockAt(4, 0), "lava keeps its cell");
        assertEquals(Fluids.WATER.block(), blockAt(2, 0), "the water stops next to it");
    }

    @Test
    void waterNeverEatsTheGround() {
        Block ground = world.getBlock(6, Chunk.flatY(Chunk.LAYER_FLOOR), 0);

        pour(Fluids.WATER, 0, 0);
        settle();

        assertEquals(ground, world.getBlock(6, Chunk.flatY(Chunk.LAYER_FLOOR), 0), "the ground below stayed");
        assertEquals(ground, world.getBlock(0, Chunk.flatY(Chunk.LAYER_FLOOR), 0), "and so did the one at the"
                + " source");
    }

    @Test
    void aSpillStopsAtTheRimOfAHole() {
        // A strip of the ground is dug out, so the layer of the player has nothing to stand on.
        for (int y = -PATCH; y <= PATCH; y++) {
            world.setBlock(3, Chunk.flatY(FluidFlow.FLOOR_LAYER), y, Blocks.AIR);
        }
        pour(Fluids.WATER, 0, 0);
        settle();

        assertEquals(Fluids.WATER.block(), blockAt(2, 0), "the water reached the rim");
        assertEquals(Blocks.AIR, blockAt(4, 0), "and does not float over the hole");
    }

    @Test
    void waterOfTheGroundLayerLivesInHoles() {
        // A hole in the ground, filled from the ground layer.
        world.setBlock(0, Chunk.flatY(FluidFlow.FLOOR_LAYER), 0, Fluids.WATER.block());
        world.setState(0, Chunk.flatY(FluidFlow.FLOOR_LAYER), 0, FluidState.SOURCE.pack());
        settle();

        assertEquals(Fluids.WATER.block(), blockAt(0, 0, FluidFlow.FLOOR_LAYER),
                "the hole keeps its water");
        assertEquals(Blocks.STONE, blockAt(1, 0, FluidFlow.FLOOR_LAYER),
                "and the ground around it is never replaced");
        assertEquals(Blocks.AIR, blockAt(0, 0), "the layer above stays empty");
    }

    @Test
    void aSpillDriesUpRingByRing() {
        pour(Fluids.WATER, 0, 0);
        settle();
        int lake = fluidCells();
        assertEquals(2 * 7 * 8 + 1, lake, "the lake of a spread of seven");

        world.setBlock(0, Chunk.flatY(LAYER), 0, Blocks.AIR);
        world.setState(0, Chunk.flatY(LAYER), 0, 0);
        for (int tick = 0; tick < Fluids.WATER.tickInterval(); tick++) {
            world.tick(TickClock.TICK_SECONDS);
        }

        assertTrue(fluidCells() > 0, "the lake is not gone in a single step");
        assertTrue(fluidCells() < lake, "but it is already smaller than it was");

        settle();

        assertEquals(0, fluidCells(), "and in the end not a drop is left");
    }

    /** Empties the object layer of a patch around the spawn, so a spill has room to run. */
    private void clearPatch() {
        for (int y = -PATCH; y <= PATCH; y++) {
            for (int x = -PATCH; x <= PATCH; x++) {
                // Solid ground, so the layer of the player has something to stand on: a fluid over
                // a hole would float in the air and is refused, see FluidFlow.
                world.setBlock(x, Chunk.flatY(FluidFlow.FLOOR_LAYER), y, Blocks.STONE);
                world.setState(x, Chunk.flatY(FluidFlow.FLOOR_LAYER), y, 0);
                world.setBlock(x, Chunk.flatY(LAYER), y, Blocks.AIR);
                world.setState(x, Chunk.flatY(LAYER), y, 0);
            }
        }
    }

    /**
     * Pours a source of a fluid into a cell.
     *
     * @param fluid fluid to pour
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     */
    private void pour(Fluid fluid, int x, int y) {
        world.setBlock(x, Chunk.flatY(LAYER), y, fluid.block());
        world.setState(x, Chunk.flatY(LAYER), y, FluidState.SOURCE.pack());
    }

    /** Ticks the world until no fluid waits for a look. */
    private void settle() {
        int tick = 0;
        while (tick < SETTLE_LIMIT && world.fluids().pendingCellCount() > 0) {
            world.tick(TickClock.TICK_SECONDS);
            tick++;
        }
        assertEquals(0, world.fluids().pendingCellCount(),
                "the spill needs more than " + SETTLE_LIMIT + " ticks");
    }

    /** Block of a cell of the layer the fluids live in. */
    private Block blockAt(int x, int y) {
        return world.getBlock(x, Chunk.flatY(LAYER), y);
    }

    /** Block of a cell of a layer. */
    private Block blockAt(int x, int y, int layer) {
        return world.getBlock(x, Chunk.flatY(layer), y);
    }

    /** State of a cell of the layer the fluids live in. */
    private int metaAt(int x, int y) {
        return world.getState(x, Chunk.flatY(LAYER), y);
    }

    /** Amount of cells of the patch that carry a fluid. */
    private int fluidCells() {
        int found = 0;
        for (int y = -PATCH; y <= PATCH; y++) {
            for (int x = -PATCH; x <= PATCH; x++) {
                if (Fluids.byBlock(blockAt(x, y)) != null) {
                    found++;
                }
            }
        }
        return found;
    }
}
