package com.philia093.neofactory.item;

import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.fluid.SimpleFluidStorage;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks how a cell and a tank of a machine meet.
 * <p>
 * The whole rule of feeding a machine by hand lives here: a tank takes a whole cell or nothing, and a cell
 * is filled with a whole thousand or stays empty. Nothing of the game may lose a drop of fluid, so every
 * case that does not add up is checked as well - a tank of another fluid, a tank with too little room and a
 * hand that carries no single cell.
 */
class CellTransferTest {

    /** Amount of fluid the tank of this test holds, four cells. */
    private static final int TANK = 4000;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    /** An empty tank of the size this test works with. */
    private static SimpleFluidStorage tank() {
        return new SimpleFluidStorage(TANK);
    }

    @Test
    void aFullCellIsPouredIntoAnEmptyTank() {
        SimpleFluidStorage tank = tank();

        CellTransfer.Result result = CellTransfer.pour(ItemStack.of(Items.WATER_CELL, 1), tank);

        assertTrue(result.moved());
        assertEquals(Fluids.WATER, tank.fluid());
        assertEquals(FluidContainer.CAPACITY, tank.amount());
        assertEquals(Items.FLUID_CELL, result.carried().item(), "the hand carries the empty cell now");
        assertEquals(1, result.carried().count());
    }

    @Test
    void aCellTheTankAlreadyHoldsIsPouredOnTop() {
        SimpleFluidStorage tank = tank();
        tank.fill(Fluids.WATER, 500, false);

        CellTransfer.Result result = CellTransfer.pour(ItemStack.of(Items.WATER_CELL, 1), tank);

        assertTrue(result.moved());
        assertEquals(500 + FluidContainer.CAPACITY, tank.amount());
    }

    @Test
    void aTankOfAnotherFluidRefusesTheCell() {
        SimpleFluidStorage tank = tank();
        tank.fill(Fluids.LAVA, 500, false);
        ItemStack carried = ItemStack.of(Items.WATER_CELL, 1);

        CellTransfer.Result result = CellTransfer.pour(carried, tank);

        assertFalse(result.moved());
        assertSame(carried, result.carried(), "the hand keeps the very stack it held");
        assertEquals(500, tank.amount(), "and the tank keeps what was in it");
        assertEquals(Fluids.LAVA, tank.fluid());
    }

    @Test
    void aTankWithoutRoomForAWholeCellRefusesIt() {
        SimpleFluidStorage tank = tank();
        tank.fill(Fluids.WATER, TANK - FluidContainer.CAPACITY + 1, false);
        ItemStack carried = ItemStack.of(Items.WATER_CELL, 1);

        CellTransfer.Result result = CellTransfer.pour(carried, tank);

        assertFalse(result.moved(), "nine hundred and ninety nine is not a whole cell");
        assertEquals(TANK - FluidContainer.CAPACITY + 1, tank.amount());
        assertSame(carried, result.carried());
    }

    @Test
    void anEmptyCellIsFilledFromATankThatHoldsAWholeCell() {
        SimpleFluidStorage tank = tank();
        tank.fill(Fluids.STEAM, 1500, false);

        CellTransfer.Result result = CellTransfer.fill(ItemStack.of(Items.FLUID_CELL, 1), tank);

        assertTrue(result.moved());
        assertEquals(Items.STEAM_CELL, result.carried().item());
        assertEquals(1500 - FluidContainer.CAPACITY, tank.amount(), "exactly one cell was taken");
    }

    @Test
    void aTankWithLessThanAWholeCellFillsNothing() {
        SimpleFluidStorage tank = tank();
        tank.fill(Fluids.STEAM, FluidContainer.CAPACITY - 1, false);
        ItemStack carried = ItemStack.of(Items.FLUID_CELL, 1);

        CellTransfer.Result result = CellTransfer.fill(carried, tank);

        assertFalse(result.moved());
        assertEquals(FluidContainer.CAPACITY - 1, tank.amount());
        assertSame(carried, result.carried());
    }

    @Test
    void aFullCellIsNotFilledFromATank() {
        SimpleFluidStorage tank = tank();
        tank.fill(Fluids.STEAM, 2000, false);
        ItemStack carried = ItemStack.of(Items.WATER_CELL, 1);

        CellTransfer.Result result = CellTransfer.fill(carried, tank);

        assertFalse(result.moved(), "a full cell has no room for a second fluid");
        assertEquals(2000, tank.amount());
        assertSame(carried, result.carried());
    }

    @Test
    void nothingHappensWithoutACell() {
        SimpleFluidStorage tank = tank();
        tank.fill(Fluids.WATER, 1000, false);

        assertFalse(CellTransfer.pour(ItemStack.of(Items.STONE, 1), tank).moved());
        assertFalse(CellTransfer.pour(ItemStack.EMPTY, tank).moved());
        assertFalse(CellTransfer.pour(null, tank).moved());
        assertFalse(CellTransfer.fill(ItemStack.of(Items.STONE, 1), tank).moved());
        assertFalse(CellTransfer.fill(ItemStack.EMPTY, tank).moved());
        assertFalse(CellTransfer.fill(null, tank).moved());
        assertEquals(1000, tank.amount(), "and the tank was never touched");
    }

    @Test
    void aStackOfCellsIsLeftAlone() {
        SimpleFluidStorage tank = tank();
        tank.fill(Fluids.STEAM, 2000, false);
        ItemStack carried = ItemStack.of(Items.FLUID_CELL, 3);

        CellTransfer.Result result = CellTransfer.fill(carried, tank);

        assertFalse(result.moved(), "a stack of empty cells has nowhere to put the full one");
        assertEquals(2000, tank.amount());
        assertSame(carried, result.carried());
    }
}
