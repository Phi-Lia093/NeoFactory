package com.philia093.neofactory.machine;

import com.badlogic.gdx.Input;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.item.FluidContainer;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks how a player hands a fluid to a machine and takes one away, without a slot in between.
 * <p>
 * A tank of a machine is not a slot: a click on it trades the cell the mouse carries with the tank itself,
 * which is the one place where a cell and a tank meet, see
 * {@link com.philia093.neofactory.item.CellTransfer}. The clicks go through the very container the screen
 * uses, so what is checked here is what the hand of a player does.
 */
class MachineTankClickTest {

    private SteamBoilerMachine boiler;
    private PlayerInventory player;
    private MachineMenu menu;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @BeforeEach
    void setUp() {
        boiler = new SteamBoilerMachine();
        player = new PlayerInventory();
        menu = new MachineMenu(boiler, player);
        menu.container().open();
    }

    @Test
    void aFullCellOnTheMouseIsPouredIntoTheTankOfWater() {
        pickUp(Items.WATER_CELL);

        clickTank(0);

        assertEquals(Fluids.WATER, boiler.water().fluid(), "the cell of water was poured in");
        assertEquals(FluidContainer.CAPACITY, boiler.water().amount());
        assertEquals(Items.FLUID_CELL, menu.container().cursorStack().item(),
                "and the mouse carries an empty cell now");
    }

    @Test
    void anEmptyCellOnTheMouseIsFilledFromTheTankOfSteam() {
        boiler.steam().fill(Fluids.STEAM, FluidContainer.CAPACITY, false);
        pickUp(Items.FLUID_CELL);

        clickTank(1);

        assertEquals(Items.STEAM_CELL, menu.container().cursorStack().item());
        assertEquals(0, boiler.steam().amount(), "exactly one cell was taken");
    }

    @Test
    void aTankWithTooLittleRoomKeepsTheCellAndTheFluid() {
        boiler.water().fill(Fluids.WATER, SteamBoilerMachine.WATER_CAPACITY - 500, false);
        pickUp(Items.WATER_CELL);

        clickTank(0);

        assertEquals(Items.WATER_CELL, menu.container().cursorStack().item(),
                "five hundred is not a whole cell, so the cell stays closed");
        assertEquals(SteamBoilerMachine.WATER_CAPACITY - 500, boiler.water().amount());
    }

    @Test
    void aTankOfAnotherFluidKeepsTheCell() {
        // A tank that holds something else refuses a cell of water: mixing two fluids in one tank is not a
        // thing this game does.
        boiler.water().fill(Fluids.LAVA, 500, false);
        pickUp(Items.WATER_CELL);

        clickTank(0);

        assertEquals(Items.WATER_CELL, menu.container().cursorStack().item());
        assertEquals(500, boiler.water().amount(), "and the tank kept what it held");
        assertEquals(Fluids.LAVA, boiler.water().fluid());
    }

    @Test
    void theTankOfWaterDoesNotHandOutWhatItHolds() {
        // The tank a machine takes fluid from pours a full cell in, the tank it makes fluid in fills an
        // empty one, and neither does the job of the other.
        boiler.water().fill(Fluids.WATER, 2000, false);
        pickUp(Items.FLUID_CELL);

        clickTank(0);

        assertEquals(Items.FLUID_CELL, menu.container().cursorStack().item());
        assertEquals(2000, boiler.water().amount());
    }

    @Test
    void aClickOnATankSwallowsWhateverTheMouseCarries() {
        pickUp(Items.STONE);

        clickTank(0);

        assertEquals(Items.STONE, menu.container().cursorStack().item(), "the stone stays on the mouse");
        assertEquals(0, player.countOf(Items.STONE), "instead of being put back into the inventory");
        assertEquals(0, boiler.water().amount());
    }

    @Test
    void theTooltipOfATankNamesTheFluidAndTheRoomThatIsLeft() {
        assertEquals(List.of(MachineMenu.EMPTY_TANK,
                        "0 / " + SteamBoilerMachine.WATER_CAPACITY + " " + MachineMenu.FLUID_UNIT),
                menu.tankTooltip(menu.fluidSlots().get(0)));

        boiler.water().fill(Fluids.WATER, 3200, false);
        boiler.steam().fill(Fluids.STEAM, 160, false);

        assertEquals(List.of("Water",
                        "3200 / " + SteamBoilerMachine.WATER_CAPACITY + " " + MachineMenu.FLUID_UNIT),
                menu.tankTooltip(menu.fluidSlots().get(0)));
        assertEquals(List.of("Steam",
                        "160 / " + SteamBoilerMachine.STEAM_CAPACITY + " " + MachineMenu.FLUID_UNIT),
                menu.tankTooltip(menu.fluidSlots().get(1)));
    }

    @Test
    void theTanksAreFoundAtTheirCellsOfThePanelAndNowhereElse() {
        MachineMenu.FluidSlot water = menu.fluidSlots().get(0);

        assertSame(water, menu.fluidSlotAt(water.x() + 1, water.y() + 1));
        assertSame(water, menu.fluidSlotAt(water.x() + ContainerLayout.SLOT_SIZE - 1,
                water.y() + ContainerLayout.SLOT_SIZE - 1));
        assertNull(menu.fluidSlotAt(water.x() - 1, water.y()), "beside a tank is not on it");
        assertNull(menu.fluidSlotAt(water.x(), water.y() + ContainerLayout.SLOT_SIZE));
    }

    @Test
    void aMachineWithoutATankHasNothingToClick() {
        MachineMenu furnace = new MachineMenu(new SmeltingMachine(), new PlayerInventory());
        furnace.container().open();
        furnace.container().touchDown(furnace.container().layout().panelWidth() - 1,
                furnace.container().layout().panelHeight() - 1, Input.Buttons.LEFT, false);

        assertTrue(furnace.fluidSlots().isEmpty(), "a furnace has slots and no tank");
    }

    /** Puts a single piece of the player inventory on the mouse by clicking the slot that holds it. */
    private void pickUp(Item item) {
        player.set(0, ItemStack.of(item, 1));
        Slot slot = slotHolding(item);
        click(slot.x() + ContainerLayout.SLOT_SIZE / 2, slot.y() + ContainerLayout.SLOT_SIZE / 2);
    }

    /** The slot of the panel that holds a stack of an item. */
    private Slot slotHolding(Item item) {
        for (Slot slot : menu.container().layout().slots()) {
            if (slot.stack().item() == item) {
                return slot;
            }
        }
        throw new AssertionError("No slot of the panel holds " + item.name());
    }

    /** Clicks the middle of a tank of the panel. */
    private void clickTank(int nth) {
        MachineMenu.FluidSlot tank = menu.fluidSlots().get(nth);
        click(tank.x() + ContainerLayout.SLOT_SIZE / 2, tank.y() + ContainerLayout.SLOT_SIZE / 2);
    }

    /** Clicks a point of the panel with the left button: a press and a release on the same place. */
    private void click(int x, int y) {
        menu.container().touchDown(x, y, Input.Buttons.LEFT, false);
        menu.container().touchUp(x, y, Input.Buttons.LEFT, false);
    }
}
