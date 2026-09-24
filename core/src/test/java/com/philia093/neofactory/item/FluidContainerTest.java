package com.philia093.neofactory.item;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a container carries and what it refuses.
 * <p>
 * The difference between a bucket and a cell is the whole point of the two: a bucket is the tool of
 * the fluids the game carries in the open - water and lava - and it must never hold a fluid of the
 * industry, while a cell takes every fluid the game knows. The rules are checked on the value and on
 * the items the game builds from it.
 */
class FluidContainerTest {

    /** A fluid of the industry, one that has no bucket and no name a bucket knows. */
    private static final Fluid OIL = new Fluid("oil", new Color(0.1f, 0.1f, 0.1f, 1.0f), false);

    @BeforeAll
    static void register() {
        TestRegistries.ensure();
    }

    @Test
    void anEmptyBucketTakesWaterAndLava() {
        FluidContainer bucket = FluidContainer.bucket();

        assertEquals(FluidContainer.Kind.BUCKET, bucket.kind());
        assertTrue(bucket.isEmpty());
        assertNull(bucket.content());
        assertEquals(FluidContainer.CAPACITY, bucket.capacity());
        assertTrue(bucket.accepts(Fluids.WATER));
        assertTrue(bucket.accepts(Fluids.LAVA));
        assertFalse(bucket.accepts(null), "nothing is no fluid");
    }

    @Test
    void aBucketRefusesEveryOtherFluid() {
        assertFalse(FluidContainer.bucket().accepts(OIL), "a bucket never carries oil");
        assertThrows(IllegalArgumentException.class, () -> FluidContainer.bucket(OIL),
                "and no item of the game is a bucket of it");
    }

    @Test
    void aCellTakesAnyFluid() {
        FluidContainer cell = FluidContainer.cell();

        assertEquals(FluidContainer.Kind.CELL, cell.kind());
        assertTrue(cell.accepts(OIL), "the cell is the container of the industry");
        assertTrue(cell.accepts(Fluids.WATER));
        assertTrue(cell.accepts(Fluids.LAVA));
        assertFalse(cell.accepts(null));
    }

    @Test
    void aFullContainerTakesNothingMore() {
        FluidContainer full = FluidContainer.bucket(Fluids.WATER);

        assertFalse(full.isEmpty());
        assertFalse(full.accepts(Fluids.WATER), "there is no room in a full bucket");
        assertFalse(full.accepts(Fluids.LAVA), "and the second fluid never enters it");
        assertTrue(full.holds(Fluids.WATER));
        assertFalse(full.holds(Fluids.LAVA));
    }

    @Test
    void theItemsOfTheGameCarryTheirContainer() {
        assertTrue(Items.BUCKET.isFluidContainer());
        assertTrue(Items.BUCKET.container().isEmpty());
        assertTrue(Items.WATER_BUCKET.container().holds(Fluids.WATER));
        assertTrue(Items.LAVA_BUCKET.container().holds(Fluids.LAVA));
        assertTrue(Items.FLUID_CELL.container().isEmpty());
        assertTrue(Items.WATER_CELL.container().holds(Fluids.WATER));
        assertTrue(Items.LAVA_CELL.container().holds(Fluids.LAVA));

        assertFalse(Items.STONE.isFluidContainer(), "a block carries no fluid");
        assertNull(Items.STONE.container());
    }

    @Test
    void anEmptyContainerStacksAndAFullOneDoesNot() {
        assertEquals(Items.STONE.maxStackSize(), Items.BUCKET.maxStackSize(),
                "an empty bucket stacks like any other material");
        assertEquals(Items.STONE.maxStackSize(), Items.FLUID_CELL.maxStackSize());
        assertTrue(Items.BUCKET.isStackable());
        assertTrue(Items.FLUID_CELL.isStackable());

        assertEquals(1, Items.WATER_BUCKET.maxStackSize(), "a bucket with a fluid in it does not");
        assertEquals(1, Items.LAVA_BUCKET.maxStackSize());
        assertEquals(1, Items.WATER_CELL.maxStackSize());
        assertEquals(1, Items.LAVA_CELL.maxStackSize());
        assertFalse(Items.WATER_BUCKET.isStackable());
    }

    @Test
    void onlyAFilledCellPaintsItsWindow() {
        assertTrue(Items.WATER_CELL.container().paintsItsWindow(),
                "a cell shows its fluid in the window");
        assertTrue(Items.LAVA_CELL.container().paintsItsWindow());
        assertFalse(Items.FLUID_CELL.container().paintsItsWindow(), "an empty one shows nothing");
        assertFalse(Items.WATER_BUCKET.container().paintsItsWindow(),
                "a bucket is drawn by the art pack and is painted over by nothing");
        assertFalse(Items.LAVA_BUCKET.container().paintsItsWindow());

        assertTrue(FluidContainer.cell(Fluids.WATER).paintsItsWindow());
        assertFalse(FluidContainer.cell().paintsItsWindow());
        assertFalse(FluidContainer.bucket(Fluids.WATER).paintsItsWindow());
        assertFalse(FluidContainer.bucket().paintsItsWindow());
    }
}
