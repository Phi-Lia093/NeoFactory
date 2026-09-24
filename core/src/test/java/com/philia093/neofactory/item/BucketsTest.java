package com.philia093.neofactory.item;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the table that maps a fluid to the item that carries it.
 * <p>
 * The table is spelled out by hand, so it is checked from both sides: every fluid that stands in
 * the world has a bucket and a cell, a fluid of the industry has a cell but never a bucket, and
 * the cell of a fluid draws the one grey scale picture of the game in the colour of its fluid.
 */
class BucketsTest {

    /** A fluid of the industry, one the game has no block and no bucket for. */
    private static final Fluid OIL = oilOfTheIndustry();

    /**
     * Builds the fluid of the industry.
     * <p>
     * The tables of the game are filled first: a fluid is built with the block it stands in the
     * world with, and the blocks of the game only exist once they are registered.
     *
     * @return the fluid
     */
    private static Fluid oilOfTheIndustry() {
        TestRegistries.ensure();
        return new Fluid("oil", new Color(0.1f, 0.1f, 0.1f, 1.0f), false);
    }

    @BeforeAll
    static void register() {
        TestRegistries.ensure();
    }

    @Test
    void everyFluidThatPoursIntoTheWorldHasABucket() {
        assertEquals(Items.WATER_BUCKET, Buckets.filled(FluidContainer.Kind.BUCKET, Fluids.WATER));
        assertEquals(Items.LAVA_BUCKET, Buckets.filled(FluidContainer.Kind.BUCKET, Fluids.LAVA));
        assertEquals(Items.BUCKET, Buckets.empty(FluidContainer.Kind.BUCKET));
    }

    @Test
    void aBucketHasNoFormForAFluidOfTheIndustry() {
        assertNull(Buckets.filled(FluidContainer.Kind.BUCKET, OIL),
                "the game has no bucket of oil, see FluidContainer");
    }

    @Test
    void everyFluidOfTheGameHasACell() {
        assertEquals(Items.FLUID_CELL, Buckets.empty(FluidContainer.Kind.CELL));

        for (Fluid fluid : Fluids.all()) {
            Item cell = Buckets.filled(FluidContainer.Kind.CELL, fluid);

            assertNotNull(cell, "the cell of " + fluid.name());
            assertTrue(cell.container().holds(fluid), "and it carries that fluid");
            assertEquals(Color.WHITE, cell.tint(),
                    "the colour is painted into the window of the cell, not over the whole item");
            assertEquals(Items.FLUID_CELL.texture(), cell.texture(),
                    "every cell draws the one picture of the game");
        }
    }
}
