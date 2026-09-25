package com.philia093.neofactory.item;

import com.badlogic.gdx.graphics.Color;
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
 * Checks what a cell carries and what it refuses.
 * <p>
 * The game has one container and no bucket, so a cell has to hold every fluid of the game and only one
 * of them at a time: an empty cell takes the first fluid it is offered - water, lava or the oil of the
 * industry - and a full one takes nothing more, whatever it already carries. The rules are checked on
 * the value and on the items the game builds from it.
 */
class FluidContainerTest {

    /** A fluid of the industry, one that has no item of its own yet. */
    private static final Fluid OIL = new Fluid("oil", new Color(0.1f, 0.1f, 0.1f, 1.0f));

    @BeforeAll
    static void register() {
        TestRegistries.ensure();
    }

    @Test
    void anEmptyCellTakesAnyFluid() {
        FluidContainer cell = FluidContainer.cell();

        assertTrue(cell.isEmpty());
        assertNull(cell.content());
        assertEquals(FluidContainer.CAPACITY, cell.capacity());
        assertTrue(cell.accepts(Fluids.WATER));
        assertTrue(cell.accepts(Fluids.LAVA));
        assertTrue(cell.accepts(OIL), "the cell is the container of the industry");
        assertFalse(cell.accepts(null), "nothing is no fluid");
    }

    @Test
    void aFullCellTakesNothingMore() {
        FluidContainer full = FluidContainer.cell(Fluids.WATER);

        assertFalse(full.isEmpty());
        assertFalse(full.accepts(Fluids.WATER), "there is no room in a full cell");
        assertFalse(full.accepts(Fluids.LAVA), "and the second fluid never enters it");
        assertTrue(full.holds(Fluids.WATER));
        assertFalse(full.holds(Fluids.LAVA));
    }

    @Test
    void aBrokenContainerIsRefused() {
        assertThrows(NullPointerException.class, () -> FluidContainer.cell(null));
    }

    @Test
    void theItemsOfTheGameCarryTheirContainer() {
        assertTrue(Items.FLUID_CELL.isFluidContainer());
        assertTrue(Items.FLUID_CELL.container().isEmpty());
        assertTrue(Items.WATER_CELL.container().holds(Fluids.WATER));
        assertTrue(Items.LAVA_CELL.container().holds(Fluids.LAVA));

        assertFalse(Items.STONE.isFluidContainer(), "a block carries no fluid");
        assertNull(Items.STONE.container());
    }

    @Test
    void anEmptyContainerStacksAndAFullOneDoesNot() {
        assertEquals(Items.STONE.maxStackSize(), Items.FLUID_CELL.maxStackSize(),
                "an empty cell stacks like any other material");
        assertTrue(Items.FLUID_CELL.isStackable());

        assertEquals(1, Items.WATER_CELL.maxStackSize(), "a cell with a fluid in it does not");
        assertEquals(1, Items.LAVA_CELL.maxStackSize());
        assertFalse(Items.WATER_CELL.isStackable());
    }

    @Test
    void onlyAFilledCellPaintsItsWindow() {
        assertTrue(Items.WATER_CELL.container().paintsItsWindow(),
                "a cell shows its fluid in the window");
        assertTrue(Items.LAVA_CELL.container().paintsItsWindow());
        assertFalse(Items.FLUID_CELL.container().paintsItsWindow(), "an empty one shows nothing");

        assertTrue(FluidContainer.cell(Fluids.WATER).paintsItsWindow());
        assertFalse(FluidContainer.cell().paintsItsWindow());
    }
}
