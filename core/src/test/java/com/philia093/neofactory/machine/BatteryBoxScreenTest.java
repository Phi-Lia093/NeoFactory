package com.philia093.neofactory.machine;

import com.badlogic.gdx.Input;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.ContainerMenu;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.item.Batteries;
import com.philia093.neofactory.item.BatteryChemistry;
import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.TickClock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what the screen of a box of cells does with what a player clicks it with.
 * <p>
 * The panel of a box is a grid of plain slots, so every slot of it is one of the cells of the machine and what
 * may lie in one of them is the business of the machine: <b>a box takes the cells of its own tier and nothing
 * else</b>, whatever is inside them, see {@link BatteryBoxMachine#acceptsItem}. That rule is asked by the
 * container of the screen for every way a stack may arrive - a click of a player, the key of the inventory -
 * and it is checked here through the clicks themselves, the way a player makes them.
 */
class BatteryBoxScreenTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theScreenOfABoxTakesTheCellsOfItsTierAndNothingElse() {
        BatteryBoxMachine box = new BatteryBoxMachine(Voltage.MEDIUM, 4);
        PlayerInventory player = new PlayerInventory();
        MachineMenu menu = new MachineMenu(box, player);
        ContainerLayout layout = menu.container().layout();
        ContainerMenu container = menu.container();
        Slot cell = slotOf(layout, box.inventory(), 0);

        // A player picks a cell of the low voltage up and clicks it onto the box of the middle one.
        player.set(0, cell(BatteryChemistry.LITHIUM, Voltage.LOW));
        click(container, slotOf(layout, player, 0));
        assertEquals(Batteries.itemNameOf(Batteries.of(BatteryChemistry.LITHIUM, Voltage.LOW)),
                container.cursorStack().item().name(), "the cell is on the mouse");

        click(container, cell);
        assertEquals(1, container.cursorStack().count(),
                "a box of the middle voltage refuses a cell of the low one, which its line would destroy");
        assertTrue(box.inventory().get(0).isEmpty(), "and nothing of it was swallowed");

        // A block is no cell at all, and the box refuses it the same way.
        click(container, slotOf(layout, player, 0));
        player.set(0, ItemStack.of(Items.STONE, 1));
        click(container, slotOf(layout, player, 0));
        click(container, cell);
        assertEquals(Items.STONE, container.cursorStack().item(), "a box refuses a block");

        // The cell of the own tier is what a box is built for, and it goes in.
        click(container, slotOf(layout, player, 0));
        player.set(0, cell(BatteryChemistry.LITHIUM, Voltage.MEDIUM));
        click(container, slotOf(layout, player, 0));
        click(container, cell);

        assertTrue(container.cursorStack().isEmpty(), "the cell of the middle voltage went in");
        assertEquals(1, box.bank().cells(), "and the box reads it as one of its own cells");
        assertEquals(1, box.inventory().get(0).count());
    }

    @Test
    void theKeyOfTheInventoryMovesACellIntoTheBox() {
        BatteryBoxMachine box = new BatteryBoxMachine(Voltage.LOW, 4);
        PlayerInventory player = new PlayerInventory();
        MachineMenu menu = new MachineMenu(box, player);
        ContainerLayout layout = menu.container().layout();
        ContainerMenu container = menu.container();

        player.set(0, cell(BatteryChemistry.LITHIUM, Voltage.LOW));
        shiftClick(container, slotOf(layout, player, 0));

        assertEquals(1, box.bank().cells(), "the cell of the tier went into the box");
        assertEquals(1, box.inventory().get(0).count());
        assertTrue(player.get(0).isEmpty(), "and left the inventory");

        player.set(1, cell(BatteryChemistry.LITHIUM, Voltage.HIGH));
        shiftClick(container, slotOf(layout, player, 1));

        assertEquals(1, box.bank().cells(), "a cell of another tier is no cell of this box");
        assertEquals(1, player.get(1).count(), "so it stays where the player had it");
    }

    @Test
    void aCellThatIsSpentStaysInItsBox() {
        // A box keeps a cell that has nothing left in it: a cell is not a tool and is never thrown away, see
        // Battery#isDrained. What stands in the box is a cell a player fills again - a spent cell is a mouth of
        // the box like any other and asks a line to fill it, while it holds nothing to give.
        BatteryBoxMachine box = new BatteryBoxMachine(Voltage.LOW, 1);
        ItemStack spent = cell(BatteryChemistry.LITHIUM, Voltage.LOW);
        spent.setDamage(spent.item().maxDamage());
        box.inventory().set(0, spent);

        box.tick(TickClock.TICK_SECONDS);

        assertSame(spent, box.inventory().get(0), "the cell is where the player put it");
        assertEquals(0, box.bank().amount());
        assertEquals(0, box.bank().extract(Voltage.LOW.euPerTick() * 10, false),
                "and there is nothing in it to take out, however many mouths the box has");
        assertEquals(1, box.bank().cells(), "while the cell is still one of the cells of the box");
    }

    /** A stack of one cell of a chemistry and a tier, fresh and full. */
    private static ItemStack cell(BatteryChemistry chemistry, Voltage tier) {
        return ItemStack.of(ItemRegistry.byName(Batteries.itemNameOf(Batteries.of(chemistry, tier))), 1);
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
        click(container, slot, false);
    }

    /** One click with the key of the inventory held, which moves the stack to the other side. */
    private static void shiftClick(ContainerMenu container, Slot slot) {
        click(container, slot, true);
    }

    /** One click on a cell of the panel, with or without the key of the inventory held. */
    private static void click(ContainerMenu container, Slot slot, boolean shift) {
        container.touchDown(slot.x(), slot.y(), Input.Buttons.LEFT, shift);
        container.touchUp(slot.x(), slot.y(), Input.Buttons.LEFT, shift);
    }
}
