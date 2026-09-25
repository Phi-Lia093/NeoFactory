package com.philia093.neofactory.item;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.fluid.SimpleFluidStorage;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Checks the rule that trades a cell with a tank.
 * <p>
 * The rule is plain item arithmetic, so it is checked here without a machine and without a window: a
 * full cell is poured into a tank that takes the whole amount, an empty one is filled from a tank that
 * holds a whole cell worth of fluid, and nothing of a cell is ever lost. What the rule hands back when
 * nothing happened is the very stack that was given, which is what lets a machine put it back without
 * writing into the slot.
 */
class CellExchangeTest {

    /** A fluid of the industry, one the game has no cell of. */
    private static final Fluid OIL = new Fluid("oil", new Color(0.1f, 0.1f, 0.1f, 1.0f));

    /** A tank of a size, empty to begin with. */
    private static SimpleFluidStorage tank(int capacity) {
        return new SimpleFluidStorage(capacity);
    }

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aFullCellIsPouredIntoAnEmptyTank() {
        SimpleFluidStorage water = tank(2000);

        ItemStack afterwards = CellExchange.exchange(ItemStack.of(Items.WATER_CELL, 1),
                List.of(water), List.of());

        assertEquals(Fluids.WATER, water.fluid(), "the tank took the water");
        assertEquals(FluidContainer.CAPACITY, water.amount());
        assertEquals(Items.FLUID_CELL, afterwards.item(), "and the cell is empty now");
        assertEquals(1, afterwards.count());
    }

    @Test
    void aFullCellStaysFullWhenNoTankTakesItAll() {
        // Room for one unit less than a cell holds, so the tank may not swallow a part of it.
        SimpleFluidStorage nearlyFull = tank(FluidContainer.CAPACITY + 1);
        nearlyFull.fill(Fluids.WATER, FluidContainer.CAPACITY, false);

        ItemStack given = ItemStack.of(Items.WATER_CELL, 1);
        ItemStack afterwards = CellExchange.exchange(given, List.of(nearlyFull), List.of());

        assertSame(given, afterwards, "nothing happened, so the slot keeps its stack");
        assertEquals(FluidContainer.CAPACITY, nearlyFull.amount(), "and the tank was not touched");
    }

    @Test
    void aFullCellSkipsATankOfAnotherFluid() {
        SimpleFluidStorage lava = tank(2000);
        lava.fill(Fluids.LAVA, 100, false);

        ItemStack afterwards = CellExchange.exchange(ItemStack.of(Items.WATER_CELL, 1),
                List.of(lava), List.of());

        assertEquals(Fluids.LAVA, lava.fluid(), "a tank never mixes two fluids");
        assertEquals(100, lava.amount());
        assertEquals(Items.WATER_CELL, afterwards.item(), "so the cell keeps its water");
    }

    @Test
    void aFullCellMovesOnToTheTankThatTakesIt() {
        SimpleFluidStorage full = tank(FluidContainer.CAPACITY);
        full.fill(Fluids.WATER, FluidContainer.CAPACITY, false);
        SimpleFluidStorage room = tank(2000);

        ItemStack afterwards = CellExchange.exchange(ItemStack.of(Items.WATER_CELL, 1),
                List.of(full, room), List.of());

        assertEquals(Fluids.WATER, room.fluid());
        assertEquals(FluidContainer.CAPACITY, room.amount(), "the second tank took it");
        assertEquals(Items.FLUID_CELL, afterwards.item());
    }

    @Test
    void anEmptyCellIsFilledFromATankThatHoldsAWholeCell() {
        SimpleFluidStorage made = tank(2000);
        made.fill(Fluids.LAVA, FluidContainer.CAPACITY, false);

        ItemStack afterwards = CellExchange.exchange(ItemStack.of(Items.FLUID_CELL, 1),
                List.of(), List.of(made));

        assertEquals(Items.LAVA_CELL, afterwards.item(), "the cell carries the lava now");
        assertEquals(0, made.amount(), "and the tank is empty");
    }

    @Test
    void anEmptyCellStaysEmptyWhileTheTankHoldsLessThanACell() {
        SimpleFluidStorage made = tank(2000);
        made.fill(Fluids.LAVA, FluidContainer.CAPACITY - 1, false);

        ItemStack given = ItemStack.of(Items.FLUID_CELL, 1);
        ItemStack afterwards = CellExchange.exchange(given, List.of(), List.of(made));

        assertSame(given, afterwards, "a half cell is not a thing of this game");
        assertEquals(FluidContainer.CAPACITY - 1, made.amount());
    }

    @Test
    void aFluidTheGameHasNoCellOfKeepsTheTank() {
        SimpleFluidStorage made = tank(2000);
        made.set(OIL, FluidContainer.CAPACITY);

        ItemStack afterwards = CellExchange.exchange(ItemStack.of(Items.FLUID_CELL, 1),
                List.of(), List.of(made));

        assertEquals(Items.FLUID_CELL, afterwards.item(), "no cell of oil exists yet");
        assertEquals(FluidContainer.CAPACITY, made.amount());
    }

    @Test
    void anItemThatCarriesNoFluidIsLeftAlone() {
        ItemStack stone = ItemStack.of(Items.STONE, 1);
        SimpleFluidStorage water = tank(2000);
        water.fill(Fluids.WATER, FluidContainer.CAPACITY, false);

        assertSame(stone, CellExchange.exchange(stone, List.of(tank(2000)), List.of(water)),
                "a stone is not a cell");
    }

    @Test
    void aStackOfCellsIsLeftAlone() {
        SimpleFluidStorage made = tank(4000);
        made.fill(Fluids.WATER, 3000, false);

        ItemStack several = ItemStack.of(Items.FLUID_CELL, 4);
        assertSame(several, CellExchange.exchange(several, List.of(), List.of(made)),
                "a full cell would have nowhere to stay beside its stack");
        assertEquals(3000, made.amount(), "and the tank keeps its water");
    }

    @Test
    void anEmptySlotIsLeftAlone() {
        assertSame(ItemStack.EMPTY,
                CellExchange.exchange(ItemStack.EMPTY, List.of(tank(2000)), List.of(tank(2000))));
    }
}
