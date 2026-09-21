package com.philia093.neofactory.fluid;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the table of the fluids and what a fluid promises about itself.
 * <p>
 * A fluid is data, so everything it claims can be read back: the block it stands in the world
 * with, the colour it is painted in and the sheet its animation is cut from. The tests need no
 * window, which is the point of keeping the fluid apart from the picture it uses.
 */
class FluidTest {

    @BeforeAll
    static void register() {
        TestRegistries.ensure();
    }

    @Test
    void theTableKnowsWaterAndLava() {
        assertEquals(Fluids.WATER, Fluids.byName("water"));
        assertEquals(Fluids.LAVA, Fluids.byName("lava"));
        assertNull(Fluids.byName("oil"), "an unknown name finds nothing");
        assertNull(Fluids.byName(null), "and so does no name at all");
        assertEquals(2, Fluids.all().size(), "the game starts with the two basic fluids");
    }

    @Test
    void everyFluidOwnsItsBlock() {
        for (Fluid fluid : Fluids.all()) {
            assertTrue(fluid.block().isLiquid(), fluid + " stands in the world as a liquid");
            assertFalse(fluid.block().isSolid(), "a fluid is walked through");
            assertTrue(fluid.block().isTransparent(), "the ground below is seen through a fluid");
            assertTrue(fluid.block().hardness() < 0.0f, "no tool gets through a fluid");
            assertFalse(fluid.block().isGround(), "a fluid is no ground to build on");
            assertEquals(fluid.name(), fluid.block().name(), "fluid and block share their name");
            assertEquals(fluid, Fluids.byBlock(fluid.block()), "the block finds its fluid again");
            assertTrue(Fluids.isFluidBlock(fluid.block()));
        }
        assertFalse(Fluids.isFluidBlock(Blocks.STONE), "stone belongs to no fluid");
        assertNull(Fluids.byBlock(null), "and no block at all belongs to none either");
    }

    @Test
    void theBlockOfAFluidWearsItsColourAndItsFrames() {
        for (Fluid fluid : Fluids.all()) {
            assertEquals(fluid.color(), fluid.block().tint(),
                    "the block is painted in the colour of the fluid");
            assertEquals(fluid.stillTexture(), fluid.block().texture(),
                    "the block draws the sheet of the fluid");
            assertTrue(fluid.block().isAnimated(), "the sheet of a fluid is played");
            assertEquals(fluid.frames(), fluid.block().animation().frames());
            assertEquals(fluid.frameTicks(), fluid.block().animation().frameTicks());
        }
    }

    @Test
    void waterRunsFartherThanLava() {
        assertEquals(7, Fluids.WATER.range(), "water reaches seven cells");
        assertEquals(3, Fluids.LAVA.range(), "lava reaches three, it is thicker");
        assertTrue(Fluids.WATER.spreads());
        assertTrue(Fluids.WATER.tickInterval() < Fluids.LAVA.tickInterval(),
                "water runs, lava crawls");
    }

    @Test
    void onlyWaterAndLavaGoIntoABucket() {
        assertTrue(Fluids.WATER.bucketable(), "water is carried in a bucket");
        assertTrue(Fluids.LAVA.bucketable(), "and lava as well");
    }

    @Test
    void aBrokenFluidIsRefused() {
        Color color = new Color(1.0f, 1.0f, 1.0f, 1.0f);

        assertThrows(NullPointerException.class, () -> new Fluid(null, color, "sheet", 1, 1, 1, 1,
                true, Blocks.STONE));
        assertThrows(NullPointerException.class, () -> new Fluid("oil", null, "sheet", 1, 1, 1, 1,
                true, Blocks.STONE));
        assertThrows(NullPointerException.class, () -> new Fluid("oil", color, null, 1, 1, 1, 1,
                true, Blocks.STONE));
        assertThrows(NullPointerException.class, () -> new Fluid("oil", color, "sheet", 1, 1, 1, 1,
                true, null));
        assertThrows(IllegalArgumentException.class, () -> new Fluid(" ", color, "sheet", 1, 1, 1, 1,
                true, Blocks.STONE));
        assertThrows(IllegalArgumentException.class, () -> new Fluid("oil", color, " ", 1, 1, 1, 1,
                true, Blocks.STONE));
        assertThrows(IllegalArgumentException.class, () -> new Fluid("oil", color, "sheet", 0, 1, 1,
                1, true, Blocks.STONE));
        assertThrows(IllegalArgumentException.class, () -> new Fluid("oil", color, "sheet", 1, 0, 1,
                1, true, Blocks.STONE));
        assertThrows(IllegalArgumentException.class, () -> new Fluid("oil", color, "sheet", 1, 1, 0,
                1, true, Blocks.STONE));
        assertThrows(IllegalArgumentException.class, () -> new Fluid("oil", color, "sheet", 1, 1, 1,
                0, true, Blocks.STONE));
    }

    @Test
    void theColourOfAFluidIsACopy() {
        Color given = new Color(0.1f, 0.2f, 0.3f, 1.0f);
        Fluid fluid = new Fluid("oil", given, "sheet", 1, 1, 1, 1, true, Blocks.STONE);

        given.set(0.9f, 0.9f, 0.9f, 1.0f);

        assertEquals(0.1f, fluid.color().r, 1.0e-6f, "the fluid keeps the colour it was given");
        assertEquals(1.0f, fluid.color().a, 1.0e-6f);
    }
}
