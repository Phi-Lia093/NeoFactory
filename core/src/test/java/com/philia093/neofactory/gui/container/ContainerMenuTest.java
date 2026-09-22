package com.philia093.neofactory.gui.container;

import com.badlogic.gdx.Input;
import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.material.Materials;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the clicks of {@link ContainerMenu}.
 * <p>
 * The class holds the whole logic of a container and never touches a window, so every
 * gesture the player can make is checked here: taking and dropping a stack, taking
 * half of it, swapping two items, moving one with shift and the rules of a result
 * slot. The screen on top of it only has to translate the mouse into a slot.
 */
class ContainerMenuTest {

    private static final int CHEST_SLOTS = 3;

    private static final int LEFT = Input.Buttons.LEFT;
    private static final int RIGHT = Input.Buttons.RIGHT;

    private Inventory chest;
    private PlayerInventory player;
    private ContainerLayout layout;
    private ContainerMenu menu;
    private int changes;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @BeforeEach
    void setUp() {
        chest = new Inventory(CHEST_SLOTS);
        player = new PlayerInventory();
        layout = new ContainerLayout();
        layout.addGrid(8, 8, CHEST_SLOTS, 1, chest, 0, Slot.Rule.NORMAL);
        layout.addGrid(8, 62, 9, 4, player, 0, Slot.Rule.NORMAL);
        menu = new ContainerMenu(layout, player);
        menu.setChangeListener(container -> changes++);
        menu.open();
        changes = 0;
    }

    @Test
    void thePanelGrowsAroundItsSlots() {
        // Three slots above nine columns of four rows, each eight pixels from the frame
        // of the panel, see ContainerLayout.PADDING. The nine columns are 176 pixels
        // wide, exactly the width of the panel picture.
        assertEquals(176, layout.panelWidth());
        assertEquals(62 + 3 * ContainerLayout.SLOT_PITCH + ContainerLayout.SLOT_SIZE
                + ContainerLayout.PADDING, layout.panelHeight());
    }

    @Test
    void theBevelBetweenTwoSlotsBelongsToNeitherOfThem() {
        Slot first = slot(0);
        Slot second = slot(1);

        assertNotNull(layout.slotAt(first.x() + 1, first.y() + 1), "inside the first slot");
        assertEquals(second, layout.slotAt(second.x() + 1, second.y() + 1), "inside the second");
        assertNull(layout.slotAt(first.x() + ContainerLayout.SLOT_SIZE,
                first.y() + 1), "the pixel behind the first slot");
    }

    @Test
    void aLeftClickTakesAWholeStack() {
        chest.set(0, ItemStack.of(Items.STONE, 3));

        click(slot(0), LEFT, false);

        assertEquals(0, slot(0).stack().count(), "the slot is empty");
        assertEquals(3, menu.cursorStack().count(), "the mouse carries the stack");
        assertTrue(menu.cursorStack().sameItem(ItemStack.of(Items.STONE, 1)));
        assertEquals(1, changes, "the container told its listener");
    }

    @Test
    void aLeftClickDropsAWholeStack() {
        chest.set(0, ItemStack.of(Items.STONE, 3));
        click(slot(0), LEFT, false);

        click(slot(1), LEFT, false);

        assertEquals(3, slot(1).stack().count(), "the stack is in the second slot");
        assertTrue(menu.cursorStack().isEmpty(), "the mouse is empty again");
    }

    @Test
    void aLeftClickTopsUpAStackOfTheSameItem() {
        chest.set(0, ItemStack.of(Items.STONE, 10));
        chest.set(1, ItemStack.of(Items.STONE, 5));

        click(slot(1), LEFT, false);
        click(slot(0), LEFT, false);

        assertEquals(15, slot(0).stack().count());
        assertTrue(menu.cursorStack().isEmpty());
    }

    @Test
    void aLeftClickSwapsTwoDifferentItems() {
        chest.set(0, ItemStack.of(Items.STONE, 2));
        chest.set(1, ItemStack.of(Items.DIRT, 7));

        click(slot(1), LEFT, false);
        click(slot(0), LEFT, false);

        assertTrue(slot(0).stack().sameItem(ItemStack.of(Items.DIRT, 1)), "dirt took the place");
        assertEquals(7, slot(0).stack().count());
        assertTrue(menu.cursorStack().sameItem(ItemStack.of(Items.STONE, 1)),
                "the stone the slot held is on the mouse now");
        assertEquals(2, menu.cursorStack().count());
    }

    @Test
    void aRightClickTakesHalfOfTheStack() {
        chest.set(0, ItemStack.of(Items.STONE, 7));

        click(slot(0), RIGHT, false);

        assertEquals(4, menu.cursorStack().count(), "half of seven, rounded up");
        assertEquals(3, slot(0).stack().count(), "the rest stays in the slot");
    }

    @Test
    void aRightClickDropsASingleItem() {
        chest.set(0, ItemStack.of(Items.STONE, 5));

        click(slot(0), RIGHT, false);
        click(slot(1), RIGHT, false);

        assertEquals(1, slot(1).stack().count(), "one item was dropped");
        assertEquals(2, menu.cursorStack().count(), "the rest stays on the mouse");
    }

    @Test
    void anOutputSlotOnlyHandsItemsOut() {
        ContainerLayout outputLayout = new ContainerLayout();
        outputLayout.add(8, 8, chest, 0, Slot.Rule.OUTPUT);
        outputLayout.addGrid(8, 62, 9, 4, player, 0, Slot.Rule.NORMAL);
        ContainerMenu result = new ContainerMenu(outputLayout, player);
        result.open();
        Slot resultSlot = outputLayout.slots().get(0);
        Slot playerSlot = outputLayout.slots().get(1);
        chest.set(0, ItemStack.of(Materials.IRON.ingot(), 2));
        player.set(0, ItemStack.of(Items.STONE, 5));

        // What the container made is taken, a carried stack is not dropped into it.
        click(result, playerSlot, LEFT, false);
        click(result, resultSlot, LEFT, false);
        assertTrue(result.cursorStack().sameItem(ItemStack.of(Items.STONE, 1)),
                "the stone stays on the mouse");
        assertEquals(5, result.cursorStack().count());
        assertEquals(2, chest.get(0).count(), "the result is untouched");

        // With an empty mouse the result is taken as usual.
        Slot empty = outputLayout.slots().get(2);
        click(result, empty, LEFT, false);
        assertTrue(result.cursorStack().isEmpty(), "the stone went into an empty slot");
        click(result, resultSlot, LEFT, false);
        assertTrue(result.cursorStack().sameItem(ItemStack.of(Materials.IRON.ingot(), 1)),
                "the result was taken");
        assertEquals(2, result.cursorStack().count());
    }

    @Test
    void aCreativeGridDestroysWhatIsPutIntoIt() {
        ContainerMenu grid = creativeGrid();
        Slot gridSlot = grid.layout().slots().get(0);
        Slot playerSlot = grid.layout().slots().get(45);
        player.set(0, ItemStack.of(Items.STONE, 12));

        // Shift moves the stack to the other side; the other side owns every item and
        // fills its slots again, so the stack disappears instead of staying.
        click(grid, playerSlot, LEFT, true);

        assertTrue(player.get(0).isEmpty(), "the stack is gone instead of staying");
        assertTrue(gridSlot.stack().isEmpty(), "and the grid slot stays empty");
    }

    @Test
    void aCreativeGridSwallowsTheStackOnTheMouse() {
        ContainerMenu grid = creativeGrid();
        Slot gridSlot = grid.layout().slots().get(0);
        Slot playerSlot = grid.layout().slots().get(45);
        player.set(0, ItemStack.of(Items.STONE, 12));

        click(grid, playerSlot, LEFT, false);
        assertEquals(12, grid.cursorStack().count(), "the stack is on the mouse");

        click(grid, gridSlot, LEFT, false);

        assertTrue(grid.cursorStack().isEmpty(), "the carried stack is gone");
        assertTrue(gridSlot.stack().isEmpty(), "and nothing was stored");
    }

    @Test
    void withoutThatSwitchTheCarriedStackIsKept() {
        ContainerLayout outputLayout = new ContainerLayout();
        outputLayout.add(8, 8, chest, 0, Slot.Rule.OUTPUT);
        outputLayout.addGrid(8, 62, 9, 4, player, 0, Slot.Rule.NORMAL);
        ContainerMenu result = new ContainerMenu(outputLayout, player);
        result.open();
        Slot resultSlot = outputLayout.slots().get(0);
        Slot playerSlot = outputLayout.slots().get(1);
        player.set(0, ItemStack.of(Items.STONE, 5));

        click(result, playerSlot, LEFT, false);
        click(result, resultSlot, LEFT, false);

        assertEquals(5, result.cursorStack().count(),
                "an ordinary result slot never eats what it cannot take");
    }

    @Test
    void shiftMovesAStackToTheOtherSide() {
        player.set(0, ItemStack.of(Items.STONE, 12));

        click(slot(CHEST_SLOTS), LEFT, true);

        assertEquals(12, chest.get(0).count(), "the stack moved into the container");
        assertTrue(player.get(0).isEmpty(), "the player slot is empty");
    }

    @Test
    void shiftKeepsWhatDoesNotFit() {
        chest.set(0, ItemStack.of(Items.STONE, 63));
        chest.set(1, ItemStack.of(Items.DIRT, 64));
        chest.set(2, ItemStack.of(Items.DIRT, 64));
        player.set(0, ItemStack.of(Items.STONE, 5));

        click(slot(CHEST_SLOTS), LEFT, true);

        assertEquals(64, chest.get(0).count(), "the stack in the container was topped up");
        assertEquals(4, player.get(0).count(), "what did not fit stays in the player slot");
    }

    @Test
    void aClickBesideTheSlotsPutsTheStackBack() {
        chest.set(0, ItemStack.of(Items.STONE, 3));
        click(slot(0), LEFT, false);

        // The padding inside the frame holds no slot.
        menu.touchDown(1, 1, LEFT, false);

        assertTrue(menu.cursorStack().isEmpty(), "the mouse is empty again");
        assertEquals(3, player.countOf(Items.STONE), "the stack went back to the player");
    }

    @Test
    void closingPutsTheCarriedStackBack() {
        chest.set(0, ItemStack.of(Items.STONE, 3));
        click(slot(0), LEFT, false);

        menu.close();

        assertFalse(menu.isOpen());
        assertTrue(menu.cursorStack().isEmpty());
        assertEquals(3, player.countOf(Items.STONE));
    }

    @Test
    void closingKeepsTheContainerOpenWhileTheInventoryIsFull() {
        fill(player, Items.DIRT, 64);
        chest.set(0, ItemStack.of(Items.STONE, 3));
        click(slot(0), LEFT, false);

        menu.close();

        assertTrue(menu.isOpen(), "a stack on the mouse keeps the container open");
        assertEquals(3, menu.cursorStack().count());
    }

    @Test
    void toggleOpensAndCloses() {
        menu.close();
        assertFalse(menu.isOpen());

        menu.toggle();
        assertTrue(menu.isOpen());

        menu.toggle();
        assertFalse(menu.isOpen());
    }

    @Test
    void aDragSharesAStackOverTheSlotsItTouches() {
        chest.set(0, ItemStack.of(Items.STONE, 4));
        click(slot(0), LEFT, false);
        assertEquals(4, menu.cursorStack().count(), "the mouse carries the stack");

        // Press on the first slot and drag across the next two of them.
        menu.touchDown(centreX(slot(0)), centreY(slot(0)), LEFT, false);
        menu.touchDragged(centreX(slot(1)), centreY(slot(1)));
        menu.touchDragged(centreX(slot(2)), centreY(slot(2)));
        menu.touchUp(centreX(slot(2)), centreY(slot(2)), LEFT, false);

        // The slot the press started on is part of the drag, so the stack is shared out over
        // all three slots the mouse touched: the remainder lands in the first one.
        assertEquals(2, chest.get(0).count(), "the slot of the press took the remainder");
        assertEquals(1, chest.get(1).count(), "the first slot the mouse reached");
        assertEquals(1, chest.get(2).count(), "the second one");
        assertTrue(menu.cursorStack().isEmpty(), "the whole stack was handed out");
    }

    @Test
    void aDragSharesAStackOverFourSlotsIncludingTheOneItStartedOn() {
        // A field of two by two, the shape a crafting grid has.
        Inventory field4 = new Inventory(4);
        ContainerLayout work = new ContainerLayout();
        work.addGrid(8, 8, 2, 2, field4, 0, Slot.Rule.NORMAL);
        work.addGrid(8, 62, 9, 4, player, 0, Slot.Rule.NORMAL);
        ContainerMenu field = new ContainerMenu(work, player);
        field.open();
        field4.set(0, ItemStack.of(Items.STONE, 8));
        List<Slot> slots = work.slots();

        // Take the stack and drag it across the whole field.
        click(field, slots.get(0), LEFT, false);
        field.touchDown(centreX(slots.get(0)), centreY(slots.get(0)), LEFT, false);
        field.touchDragged(centreX(slots.get(1)), centreY(slots.get(1)));
        field.touchDragged(centreX(slots.get(2)), centreY(slots.get(2)));
        field.touchDragged(centreX(slots.get(3)), centreY(slots.get(3)));
        field.touchUp(centreX(slots.get(3)), centreY(slots.get(3)), LEFT, false);

        assertEquals(2, field4.get(0).count(), "the slot the drag started on is filled as well");
        assertEquals(2, field4.get(1).count());
        assertEquals(2, field4.get(2).count());
        assertEquals(2, field4.get(3).count());
        assertTrue(field.cursorStack().isEmpty(), "nothing is left on the mouse");
    }

    @Test
    void aDragSharesTheStackOutWhileItMoves() {
        chest.set(0, ItemStack.of(Items.STONE, 6));
        click(slot(0), LEFT, false);

        menu.touchDown(centreX(slot(0)), centreY(slot(0)), LEFT, false);
        menu.touchDragged(centreX(slot(1)), centreY(slot(1)));
        assertEquals(3, chest.get(0).count(), "both slots took half of the stack at once");
        assertEquals(3, chest.get(1).count());
        assertTrue(menu.cursorStack().isEmpty(), "nothing is left on the mouse");

        menu.touchDragged(centreX(slot(2)), centreY(slot(2)));
        assertEquals(2, chest.get(0).count(), "reaching a third slot shared the stack out again");
        assertEquals(2, chest.get(1).count());
        assertEquals(2, chest.get(2).count());

        menu.touchUp(centreX(slot(2)), centreY(slot(2)), LEFT, false);
        assertEquals(2, chest.get(0).count(), "letting go changes nothing");
        assertEquals(2, chest.get(1).count());
        assertEquals(2, chest.get(2).count());
    }

    @Test
    void aShiftClickOnAResultCraftsAsLongAsTheIngredientsLast() {
        Inventory output = new Inventory(1);
        ContainerLayout outputLayout = new ContainerLayout();
        outputLayout.add(8, 8, output, 0, Slot.Rule.OUTPUT);
        outputLayout.addGrid(8, 62, 9, 4, player, 0, Slot.Rule.NORMAL);
        ContainerMenu container = new ContainerMenu(outputLayout, player);
        container.open();
        Slot resultSlot = outputLayout.slots().get(0);

        // Every craft eats one log and makes four planks, the recipe of the game. The filler
        // is asked for the next result right after one was taken, so it gives up an
        // ingredient first and then says what the field holds now, exactly like
        // InventoryGui does it.
        Inventory field = new Inventory(1);
        field.set(0, ItemStack.of(Items.LOG_OAK, 3));
        container.setResultFiller(slot -> {
            if (!field.get(0).isEmpty()) {
                field.get(0).setCount(field.get(0).count() - 1);
                if (field.get(0).isEmpty()) {
                    field.set(0, ItemStack.EMPTY);
                }
            }
            return field.get(0).isEmpty() ? ItemStack.EMPTY : ItemStack.of(Items.PLANKS_OAK, 4);
        });
        resultSlot.set(ItemStack.of(Items.PLANKS_OAK, 4));

        click(container, resultSlot, LEFT, true);

        assertEquals(12, player.countOf(Items.PLANKS_OAK), "all three logs were crafted");
        assertTrue(field.get(0).isEmpty(), "the field gave up everything it held");
        assertTrue(resultSlot.stack().isEmpty(), "the last result was taken as well");
    }

    @Test
    void aShiftClickOnAResultLeavesTheIngredientsAloneWhenTheInventoryIsFull() {
        Inventory output = new Inventory(1);
        ContainerLayout outputLayout = new ContainerLayout();
        outputLayout.add(8, 8, output, 0, Slot.Rule.OUTPUT);
        outputLayout.addGrid(8, 62, 9, 4, player, 0, Slot.Rule.NORMAL);
        ContainerMenu container = new ContainerMenu(outputLayout, player);
        container.open();
        Slot resultSlot = outputLayout.slots().get(0);
        fill(player, Items.STONE, 64);

        AtomicInteger crafts = new AtomicInteger();
        Inventory field = new Inventory(1);
        field.set(0, ItemStack.of(Items.LOG_OAK, 3));
        container.setResultFiller(slot -> {
            crafts.incrementAndGet();
            field.get(0).setCount(field.get(0).count() - 1);
            return ItemStack.of(Items.PLANKS_OAK, 4);
        });
        resultSlot.set(ItemStack.of(Items.PLANKS_OAK, 4));

        click(container, resultSlot, LEFT, true);

        assertEquals(0, player.countOf(Items.PLANKS_OAK), "nothing was handed to the player");
        assertEquals(4, resultSlot.stack().count(), "the result waits in its slot");
        assertEquals(0, crafts.get(), "and no ingredient was eaten for nothing");
        assertEquals(3, field.get(0).count(), "the field still holds its three logs");
    }

    @Test
    void thePanelRectTellsInsideFromOutside() {
        assertTrue(layout.contains(0, 0), "the upper left corner belongs to the panel");
        assertTrue(layout.contains(layout.panelWidth() - 1, layout.panelHeight() - 1),
                "so does the lower right one");
        assertFalse(layout.contains(-1, 10), "a point left of the panel is outside");
        assertFalse(layout.contains(10, -1), "so is a point above it");
        assertFalse(layout.contains(layout.panelWidth(), 10), "and one right of it");
        assertFalse(layout.contains(10, layout.panelHeight()), "and one below it");
    }

    @Test
    void aClickBesideThePanelThrowsTheStackIntoTheWorld() {
        List<ItemStack> dropped = new ArrayList<>();
        menu.setDropper(dropped::add);
        chest.set(0, ItemStack.of(Items.STONE, 3));
        click(slot(0), LEFT, false);
        assertEquals(3, menu.cursorStack().count(), "the mouse carries the stack");

        assertTrue(menu.dropCursor(), "the stack was thrown away");

        assertTrue(menu.cursorStack().isEmpty(), "the mouse is empty afterwards");
        assertEquals(1, dropped.size(), "the stack went to the world");
        assertEquals(3, dropped.get(0).count());
        assertEquals(0, player.countOf(Items.STONE), "and not to the player");
    }

    @Test
    void aStackThrownAwayGoesToThePlayerWhenThereIsNoWorld() {
        chest.set(0, ItemStack.of(Items.STONE, 3));
        click(slot(0), LEFT, false);

        menu.dropCursor();

        assertTrue(menu.cursorStack().isEmpty(), "the mouse is empty again");
        assertEquals(3, player.countOf(Items.STONE), "without a world the player keeps the items");
    }

    @Test
    void aDragThatEndsBesideThePanelDropsTheRest() {
        List<ItemStack> dropped = new ArrayList<>();
        menu.setDropper(dropped::add);
        chest.set(0, ItemStack.of(Items.STONE, 6));
        chest.set(2, ItemStack.of(Items.STONE, 64));
        click(slot(0), LEFT, false);

        // Dragging across the whole row: the last slot is full, so two stones stay on the
        // mouse instead of being shared out.
        menu.touchDown(centreX(slot(0)), centreY(slot(0)), LEFT, false);
        menu.touchDragged(centreX(slot(1)), centreY(slot(1)));
        menu.touchDragged(centreX(slot(2)), centreY(slot(2)));
        assertEquals(2, menu.cursorStack().count(), "two stones are left on the mouse");

        menu.dropDragRemainder();

        assertEquals(2, chest.get(0).count(), "the slots reached keep their share");
        assertEquals(2, chest.get(1).count());
        assertEquals(64, chest.get(2).count(), "the full slot took nothing");
        assertEquals(1, dropped.size(), "the rest went to the world");
        assertEquals(2, dropped.get(0).count());
        assertTrue(menu.cursorStack().isEmpty(), "the mouse is empty again");
    }

    @Test
    void aWorkFieldIsDroppedWhenTheContainerCloses() {
        Inventory field4 = new Inventory(4);
        ContainerLayout work = new ContainerLayout();
        work.addGrid(8, 8, 2, 2, field4, 0, Slot.Rule.WORK);
        work.addGrid(8, 62, 9, 4, player, 0, Slot.Rule.NORMAL);
        ContainerMenu field = new ContainerMenu(work, player);
        List<ItemStack> dropped = new ArrayList<>();
        field.setDropper(dropped::add);
        field.open();
        field4.set(0, ItemStack.of(Items.STONE, 3));
        field4.set(1, ItemStack.of(Items.DIRT, 2));

        field.close();

        assertFalse(field.isOpen());
        assertEquals(2, dropped.size(), "both stacks of the field were handed to the world");
        assertEquals(3, dropped.get(0).count());
        assertEquals(2, dropped.get(1).count());
        assertTrue(field4.get(0).isEmpty(), "the field is empty afterwards");
        assertEquals(0, player.countOf(Items.STONE), "nothing went to the player instead");
    }

    @Test
    void withoutAWorldAWorkFieldGoesBackToThePlayer() {
        Inventory field4 = new Inventory(4);
        ContainerLayout work = new ContainerLayout();
        work.addGrid(8, 8, 2, 2, field4, 0, Slot.Rule.WORK);
        work.addGrid(8, 62, 9, 4, player, 0, Slot.Rule.NORMAL);
        ContainerMenu field = new ContainerMenu(work, player);
        field.open();
        field4.set(0, ItemStack.of(Items.STONE, 3));

        field.close();

        assertTrue(field4.get(0).isEmpty());
        assertEquals(3, player.countOf(Items.STONE), "the item is not lost");
    }

    @Test
    void aDragLeavesWhatDoesNotFitOnTheMouse() {
        // The mouse carries stone while every slot the drag touches holds a full stack
        // of dirt, so nothing can be handed out.
        player.set(0, ItemStack.of(Items.STONE, 5));
        chest.set(0, ItemStack.of(Items.DIRT, 64));
        chest.set(1, ItemStack.of(Items.DIRT, 64));
        chest.set(2, ItemStack.of(Items.DIRT, 64));
        click(slot(CHEST_SLOTS), LEFT, false);
        assertEquals(5, menu.cursorStack().count(), "the stone is on the mouse");

        menu.touchDown(centreX(slot(0)), centreY(slot(0)), LEFT, false);
        menu.touchDragged(centreX(slot(1)), centreY(slot(1)));
        menu.touchDragged(centreX(slot(2)), centreY(slot(2)));
        menu.touchUp(centreX(slot(2)), centreY(slot(2)), LEFT, false);

        assertEquals(5, menu.cursorStack().count(), "the stone stays on the mouse");
        assertEquals(64, chest.get(0).count(), "the full stacks were not touched");
    }

    @Test
    void aButtonThatNeverMovesStillMovesTheWholeStack() {
        chest.set(0, ItemStack.of(Items.STONE, 3));

        // A press and a release on the same slot is a click, not a drag.
        menu.touchDown(centreX(slot(0)), centreY(slot(0)), LEFT, false);
        menu.touchUp(centreX(slot(0)), centreY(slot(0)), LEFT, false);

        assertEquals(3, menu.cursorStack().count(), "the whole stack was taken");
        assertTrue(slot(0).stack().isEmpty());
    }

    /** X coordinate of the middle of a slot. */
    private static int centreX(Slot slot) {
        return slot.x() + ContainerLayout.SLOT_SIZE / 2;
    }

    /** Y coordinate of the middle of a slot. */
    private static int centreY(Slot slot) {
        return slot.y() + ContainerLayout.SLOT_SIZE / 2;
    }

    /** Slot of the panel: the first {@link #CHEST_SLOTS} belong to the container. */
    private Slot slot(int index) {
        return layout.slots().get(index);
    }

    /**
     * A container shaped like the creative grid: nine by five result slots with the hotbar
     * below them, and the switch that destroys what is put into them.
     */
    private ContainerMenu creativeGrid() {
        ContainerLayout gridLayout = new ContainerLayout();
        gridLayout.addGrid(8, 8, 9, 5, new Inventory(45), 0, Slot.Rule.OUTPUT);
        gridLayout.addGrid(8, 122, 9, 1, player, 0, Slot.Rule.NORMAL);
        ContainerMenu grid = new ContainerMenu(gridLayout, player);
        grid.setVoidsOverflow(true);
        grid.open();
        return grid;
    }

    @Test
    void aCreativeShelfHandsOutOnePieceAndAWholeStack() {
        Inventory shelf = new Inventory(1);
        shelf.set(0, ItemStack.of(Items.STONE, 1));
        ContainerLayout shelfLayout = new ContainerLayout();
        shelfLayout.add(8, 8, shelf, 0, Slot.Rule.OUTPUT);
        shelfLayout.addGrid(8, 62, 9, 4, player, 0, Slot.Rule.NORMAL);
        ContainerMenu grid = new ContainerMenu(shelfLayout, player);
        // What the grid of a creative inventory is, see ContainerMenu#setCreativeSupply.
        grid.setResultFiller(slot -> ItemStack.of(Items.STONE, 1));
        grid.setCreativeSupply(true);
        grid.open();
        Slot shelfSlot = shelfLayout.slots().get(0);

        click(grid, shelfSlot, LEFT, false);

        assertEquals(1, grid.cursorStack().count(), "one piece per left click");
        assertEquals(1, shelf.get(0).count(), "and the shelf fills itself again");

        grid.carryBack();
        click(grid, shelfSlot, RIGHT, false);

        assertEquals(Items.STONE.maxStackSize(), grid.cursorStack().count(),
                "a whole stack per right click");
        assertEquals(1, shelf.get(0).count(), "the shelf keeps its piece either way");
    }

    @Test
    void aMachineStillHandsOutItsWholeResult() {
        // The behaviour above belongs to the shelf of the game and not to a machine: the output
        // of a recipe keeps handing out everything it holds.
        Inventory machine = new Inventory(1);
        machine.set(0, ItemStack.of(Materials.IRON.ingot(), 2));
        ContainerLayout machineLayout = new ContainerLayout();
        machineLayout.add(8, 8, machine, 0, Slot.Rule.OUTPUT);
        machineLayout.addGrid(8, 62, 9, 4, player, 0, Slot.Rule.NORMAL);
        ContainerMenu output = new ContainerMenu(machineLayout, player);
        output.open();

        click(output, machineLayout.slots().get(0), LEFT, false);

        assertEquals(2, output.cursorStack().count(), "the whole result of the machine");
    }

    /** Clicks the middle of a slot: a press and a release on the same place. */
    private void click(Slot slot, int button, boolean shift) {
        click(menu, slot, button, shift);
    }

    /** Clicks the middle of a slot of any container. */
    private static void click(ContainerMenu container, Slot slot, int button, boolean shift) {
        int x = slot.x() + ContainerLayout.SLOT_SIZE / 2;
        int y = slot.y() + ContainerLayout.SLOT_SIZE / 2;
        container.touchDown(x, y, button, shift);
        container.touchUp(x, y, button, shift);
    }

    /** Fills every slot of an inventory with the same stack. */
    private static void fill(Inventory inventory, Item item, int count) {
        for (int index = 0; index < inventory.size(); index++) {
            inventory.set(index, ItemStack.of(item, count));
        }
    }
}
