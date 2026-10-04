package com.philia093.neofactory.machine;

import com.badlogic.gdx.Input;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.ContainerMenu;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.TickClock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks how a machine of the line is fed by hand and what its cell of energy takes.
 * <p>
 * <b>A machine of the electrical age is fed twice.</b> Power arrives over a line of cables, which is what the
 * industry is built on, and a player who has no line yet - or one that stands still - puts redstone dust into
 * the cell of energy at the foot of the panel instead: the piece is burned in the buffer of the machine, and
 * the machine works on what it is worth exactly as it works on what a line hands over. The two are the same
 * ring seen from both sides, so nothing of the recipe loop is repeated here, see {@link Reagents} and
 * {@link Reagents#energyOf(ItemStack)}.
 * <p>
 * What this case is asked is the rest of it: that one piece is burned a frame, that nothing is burned which
 * would not fit whole into the buffer, and that the cell of a machine of the line takes the reagent and
 * nothing else - a machine that swallowed whatever a player dropped on it would be a machine that is filled
 * with junk by accident. The cell itself is asked of the screen, so the case also pins down that a furnace
 * and a generator keep the cell as a picture of their own state, see {@link MachineMenu#hasEnergySlot()}.
 */
class MachineRedstoneFeedTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aMachineOfTheLineBurnsOnePieceOfReagentAFrame() {
        ElectricMaceratorMachine macerator = new ElectricMaceratorMachine(Voltage.LOW);
        macerator.buffer().setAmount(0);
        macerator.inventory().set(macerator.reagentSlot(), ItemStack.of(Items.REDSTONE_DUST, 4));

        macerator.tick(TickClock.TICK_SECONDS);
        assertEquals(Reagents.DUST_ENERGY, macerator.buffer().amount(), "the first piece is in the buffer");
        assertEquals(3, macerator.inventory().get(macerator.reagentSlot()).count(),
                "and the pile in the cell is one piece shorter");

        macerator.tick(TickClock.TICK_SECONDS);
        assertEquals(2 * Reagents.DUST_ENERGY, macerator.buffer().amount());
        assertEquals(2, macerator.inventory().get(macerator.reagentSlot()).count());
    }

    @Test
    void nothingIsBurnedThatTheBufferCannotHold() {
        ElectricMaceratorMachine macerator = new ElectricMaceratorMachine(Voltage.LOW);
        MachineEnergyStorage buffer = macerator.buffer();
        int capacity = buffer.capacity();
        int left = Reagents.DUST_ENERGY / 2;

        // A buffer with less than a whole piece of room left keeps the piece for later instead of losing it.
        buffer.setAmount(capacity - left);
        macerator.inventory().set(macerator.reagentSlot(), ItemStack.of(Items.REDSTONE_DUST, 1));

        macerator.tick(TickClock.TICK_SECONDS);
        assertEquals(capacity - left, buffer.amount(), "the buffer was not touched");
        assertEquals(1, macerator.inventory().get(macerator.reagentSlot()).count(), "and the piece waits");

        buffer.setAmount(capacity - Reagents.DUST_ENERGY);
        macerator.tick(TickClock.TICK_SECONDS);
        assertEquals(capacity, buffer.amount(), "a piece that fits whole is burned");
        assertTrue(macerator.inventory().get(macerator.reagentSlot()).isEmpty(),
                "and the last piece of a pile leaves the cell empty");
    }

    @Test
    void aShelfThatHoldsNoReagentBurnsNothing() {
        ElectricMaceratorMachine macerator = new ElectricMaceratorMachine(Voltage.LOW);
        macerator.buffer().setAmount(0);
        macerator.inventory().set(macerator.reagentSlot(), ItemStack.of(Items.COAL, 1));

        macerator.tick(TickClock.TICK_SECONDS);
        assertEquals(0, macerator.buffer().amount(), "coal is no reagent of a machine of the line");
        assertEquals(Items.COAL, macerator.inventory().get(macerator.reagentSlot()).item(),
                "so it stays where a player put it");

        assertEquals(0, Reagents.energyOf(ItemStack.EMPTY), "and a cell that holds nothing is worth nothing");
        assertFalse(Reagents.isReagent(ItemStack.of(Items.COAL, 1)));
        assertTrue(Reagents.isReagent(ItemStack.of(Items.REDSTONE_DUST, 1)));
        assertEquals(Reagents.DUST_ENERGY, Reagents.energyOf(ItemStack.of(Items.REDSTONE_DUST, 64)),
                "a pile is worth what one piece of it is worth, because one piece is burned at a time");
    }

    @Test
    void onlyAMachineOfTheLineHasAShelf() {
        ElectricMaceratorMachine macerator = new ElectricMaceratorMachine(Voltage.LOW);
        assertTrue(macerator.reagentSlot() >= 0, "a machine of the line has the cell it is fed through");
        assertEquals(MachineInventory.Role.ENERGY, macerator.inventory().role(macerator.reagentSlot()));
        assertEquals(3, macerator.inventory().size(), "an input, an output and the cell of energy");

        assertEquals(-1, new SmeltingMachine().inventory().slotOf(MachineInventory.Role.ENERGY),
                "a furnace that burns coal is fed by its flame");
        assertEquals(-1, new SteamTurbineMachine(TurbineTier.LV).inventory()
                .slotOf(MachineInventory.Role.ENERGY), "and a generator fills its own buffer");

        MachineMenu furnace = new MachineMenu(new SmeltingMachine(), new PlayerInventory());
        MachineMenu generator = new MachineMenu(new SteamTurbineMachine(TurbineTier.LV),
                new PlayerInventory());
        MachineMenu cell = new MachineMenu(macerator, new PlayerInventory());

        assertFalse(furnace.hasEnergySlot(), "so the cell of a furnace stays a picture of its own state");
        assertEquals(SlotKind.GENERIC, furnace.energySlotKind(), "of the plain slot of its panel");
        assertFalse(generator.hasEnergySlot(), "and a generator is read and not filled");
        assertEquals(SlotKind.BATTERY, generator.energySlotKind(),
                "on the cell of the electricity, the one a battery is put into in the original game");
        assertTrue(cell.hasEnergySlot(), "while a machine of the line is fed through its own cell");
        assertEquals(SlotKind.BATTERY, cell.energySlotKind(), "which wears the picture of a battery");
    }

    @Test
    void theCellOfAMachineTakesTheReagentAndNothingElse() {
        ElectricMaceratorMachine macerator = new ElectricMaceratorMachine(Voltage.LOW);
        PlayerInventory player = new PlayerInventory();
        MachineMenu menu = new MachineMenu(macerator, player);
        ContainerLayout layout = menu.container().layout();
        ContainerMenu container = menu.container();
        Slot cell = slotOf(layout, macerator.inventory(), macerator.reagentSlot());

        // A player picks a pile of stone up and clicks it onto the cell of energy of the machine.
        player.set(0, ItemStack.of(Items.STONE, 4));
        click(container, slotOf(layout, player, 0));
        assertEquals(Items.STONE, container.cursorStack().item(), "the pile is on the mouse");

        click(container, cell);
        assertEquals(Items.STONE, container.cursorStack().item(), "a cell of energy refuses a block");
        assertTrue(macerator.inventory().get(macerator.reagentSlot()).isEmpty(),
                "and nothing of it was swallowed");

        // The reagent of the machine is what goes in, and the pile goes in whole.
        player.set(0, ItemStack.of(Items.REDSTONE_DUST, 4));
        click(container, slotOf(layout, player, 0));
        assertEquals(Items.REDSTONE_DUST, container.cursorStack().item(), "the dust is on the mouse");
        click(container, cell);
        assertEquals(4, macerator.inventory().get(macerator.reagentSlot()).count(),
                "and the cell takes it");
        assertTrue(container.cursorStack().isEmpty(), "so the whole pile went into the cell");
    }

    @Test
    void theBoxOfTheCellNamesTheBufferAndNotTheReagent() {
        ElectricMaceratorMachine macerator = new ElectricMaceratorMachine(Voltage.LOW);
        PlayerInventory player = new PlayerInventory();
        MachineMenu menu = new MachineMenu(macerator, player);
        Slot cell = slotOf(menu.container().layout(), macerator.inventory(), macerator.reagentSlot());
        macerator.buffer().setAmount(Reagents.DUST_ENERGY);

        assertFalse(menu.container().namesItsStack(cell),
                "the box of the cell of energy belongs to the machine and not to the item in it");
        assertEquals(List.of(MachineMenu.ENERGY,
                        Reagents.DUST_ENERGY + " / " + macerator.bufferCapacity() + " " + MachineMenu.ENERGY_UNIT),
                menu.energyTooltip(false), "and what it names is what the buffer of the machine holds");

        assertTrue(menu.container().namesItsStack(slotOf(menu.container().layout(), player, 0)),
                "every other slot of the screen names the item that lies in it");
    }

    /** The slot of a layout that shows one cell of an inventory. */
    private static Slot slotOf(ContainerLayout layout, Inventory inventory, int index) {
        for (Slot slot : layout.slots()) {
            if (slot.inventory() == inventory && slot.index() == index) {
                return slot;
            }
        }
        throw new IllegalArgumentException("The layout shows no cell " + index + " of " + inventory);
    }

    /** One click on a cell of the panel, the way a player moves a stack with the left button. */
    private static void click(ContainerMenu container, Slot slot) {
        container.touchDown(slot.x(), slot.y(), Input.Buttons.LEFT, false);
        container.touchUp(slot.x(), slot.y(), Input.Buttons.LEFT, false);
    }
}
