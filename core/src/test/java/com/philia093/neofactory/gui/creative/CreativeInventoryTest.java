package com.philia093.neofactory.gui.creative;

import com.badlogic.gdx.Input;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the logic of the creative inventory: the groups, the grid, the scroll and
 * the endless supply behind a slot.
 * <p>
 * The screen is asked for the very items the game knows, see
 * {@link com.philia093.neofactory.support.TestRegistries}, so a new item of a category
 * shows up here without a change of this test - except in the one check that every
 * item belongs to exactly one group, which is what makes a forgotten category fail the
 * build instead of hiding an item from the player.
 */
class CreativeInventoryTest {

    /** Tab that lists every item, used to test the scroll without a long category. */
    private static final List<CreativeTab> ALL = List.of(
            CreativeTab.items("all", "All", null, item -> item != Items.AIR));

    /** A long tab and a short one, to test what another tab does to a scroll. */
    private static final List<CreativeTab> TWO = List.of(
            CreativeTab.items("all", "All", null, item -> item != Items.AIR),
            CreativeTab.items("blocks", "Blocks", null, Item::isBlockItem));

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void everyItemIsInExactlyOneGroup() {
        Set<Item> seen = new HashSet<>();
        List<Item> collected = new ArrayList<>();
        for (CreativeTab tab : CreativeRegistry.tabs()) {
            if (!tab.isItems()) {
                continue;
            }
            for (Item item : ItemRegistry.all()) {
                if (tab.matches(item)) {
                    assertTrue(seen.add(item), item.name() + " is listed by two groups");
                    collected.add(item);
                }
            }
        }
        assertEquals(ItemRegistry.count() - 1, collected.size(),
                "every item except air is in a group");
        assertFalse(collected.contains(Items.AIR), "air is not an item");
    }

    @Test
    void everyGroupHoldsTheItemsTheGameIsWrittenIn() {
        assertTrue(tab("blocks").matches(Items.STONE), "a block is a block");
        assertTrue(tab("machines").matches(Items.FURNACE), "a furnace is a machine");
        assertFalse(tab("blocks").matches(Items.FURNACE),
                "and the blocks hand their machines over instead of listing them twice");
        assertTrue(tab("materials").matches(Items.STICK), "a stick is a material");
        assertTrue(tab("food").matches(Items.APPLE), "an apple is food");
        assertTrue(tab("tools").matches(Items.IRON_PICKAXE), "a pickaxe is a tool");
        assertTrue(tab("armour").matches(Items.IRON_HELMET), "a helmet is armour");
        assertFalse(tab("food").matches(Items.IRON_HELMET), "armour is not food");
        assertFalse(tab("tools").matches(Items.APPLE), "food is not a tool");
    }

    @Test
    void theGridShowsTheFirstGroupWhenTheScreenOpens() {
        CreativeInventory creative = new CreativeInventory();

        assertEquals("blocks", creative.selectedTab().name(), "the first tab is chosen");
        assertEquals(Items.STONE, creative.stackAt(0).item(), "the first block");
        assertEquals(Items.STONE.maxStackSize(), creative.stackAt(0).count(),
                "a creative stack is full");
        // The furnace is the block that was added most recently, but it holds a machine
        // and is therefore listed by the tab of the machines, see CreativeRegistry.
        assertTrue(creative.stackAt(blockCount() - 1).isEmpty(),
                "the free room behind the group of the blocks");
    }

    @Test
    void theTabOfTheMachinesListsTheFurnace() {
        CreativeInventory creative = new CreativeInventory();
        creative.select(indexOfTab("machines"));

        assertEquals(Items.FURNACE, creative.stackAt(0).item(), "the only machine of the game");
        assertTrue(creative.stackAt(1).isEmpty(), "and nothing behind it so far");
        assertEquals(1, creative.matches().size(), "the list holds the furnace alone");
    }

    @Test
    void aStackTakenFromTheGridIsThereAgainRightAway() {
        CreativeInventory creative = new CreativeInventory();

        ItemStack taken = creative.grid().remove(0);
        assertEquals(Items.STONE, taken.item(), "the click took the first block");
        assertTrue(creative.stackAt(0).isEmpty(), "the slot is empty after the click");

        ItemStack next = creative.sourceAt(0);
        assertEquals(Items.STONE, next.item(), "the supply behind the slot");
        assertEquals(Items.STONE.maxStackSize(), next.count(), "and it is a full stack");
        assertNotSame(taken, next, "every take hands out a stack of its own");
    }


    @Test
    void theWheelScrollsByWholeRows() {
        CreativeInventory creative = new CreativeInventory(ALL);
        int rows = (ItemRegistry.count() - 1 + CreativeInventory.COLUMNS - 1)
                / CreativeInventory.COLUMNS;

        assertEquals(rows, creative.rowCount(), "the list is as long as the items need");
        assertEquals(rows - CreativeInventory.ROWS, creative.maxRow(), "the room left over");
        assertEquals(Items.STONE, creative.stackAt(0).item(), "the first row is shown");

        creative.scroll(1);
        assertEquals(1, creative.firstRow(), "one row down");
        assertEquals(Items.SNOW, creative.stackAt(0).item(), "the tenth item starts the row");

        creative.scroll(99);
        assertEquals(creative.maxRow(), creative.firstRow(), "the scroll stops at the end");
        creative.scroll(-99);
        assertEquals(0, creative.firstRow(), "and at the beginning");
    }

    @Test
    void theSearchLooksAtBothNamesOfAnItem() {
        CreativeInventory creative = new CreativeInventory();
        creative.select(indexOf(CreativeTab.Kind.SEARCH));

        assertEquals(ItemRegistry.count() - 1, creative.matches().size(),
                "an empty box shows every item of the game");

        creative.setQuery("pickaxe");
        assertTrue(creative.isSearching(), "typing stays on the search tab");
        assertEquals(2, creative.matches().size(), "the iron and the diamond pickaxe");
        assertTrue(creative.matches().contains(Items.IRON_PICKAXE));
        assertTrue(creative.matches().contains(Items.DIAMOND_PICKAXE));

        creative.setQuery("Stone");
        assertTrue(creative.matches().contains(Items.STONE), "the search ignores the case");
    }

    @Test
    void theSearchFindsTheNameThePlayerReads() {
        CreativeInventory creative = new CreativeInventory();

        creative.setQuery("Iron Ore");

        assertEquals(1, creative.matches().size(), "only the ore carries that name");
        assertSame(Items.IRON_ORE, creative.matches().get(0));
    }

    @Test
    void theGridFollowsTheSearch() {
        CreativeInventory creative = new CreativeInventory();

        creative.setQuery("diamond");

        assertEquals(Items.DIAMOND, creative.matches().get(0), "the gem comes first");
        assertEquals(Items.DIAMOND, creative.stackAt(0).item(), "and stands in the grid");
        assertEquals(Items.DIAMOND.maxStackSize(), creative.stackAt(0).count());
    }

    @Test
    void anotherTabAndAnotherSearchStartAtTheTopAgain() {
        CreativeInventory creative = new CreativeInventory(TWO);

        creative.scroll(2);
        assertEquals(2, creative.firstRow(), "the long list was scrolled");

        creative.select(1);
        assertEquals(0, creative.firstRow(), "another tab shows its first row");

        creative.select(0);
        creative.scroll(2);
        creative.setQuery("stone");
        assertEquals(0, creative.firstRow(), "another search shows its first row");
    }

    @Test
    void theInventoryTabHandsThePanelToTheInventoryOfThePlayer() {
        CreativeInventory creative = new CreativeInventory();

        creative.select(indexOf(CreativeTab.Kind.INVENTORY));

        assertTrue(creative.showsPlayerInventory(), "the player inventory is shown");
        assertTrue(creative.matches().isEmpty(), "the player holds the items, not the tab");
        assertTrue(creative.stackAt(0).isEmpty(), "so the grid holds nothing");
    }

    @Test
    void aKeyOfTheSearchBoxIsKeptFromTheHotkeys() {
        // A letter the player types belongs into the box: with the old order a typed E
        // closed the screen, because the game reads that key as the inventory hotkey.
        assertTrue(CreativeInventory.isSearchKey(Input.Keys.E), "E belongs into the box");
        assertTrue(CreativeInventory.isSearchKey(Input.Keys.T), "and T as well");
        assertTrue(CreativeInventory.isSearchKey(Input.Keys.NUM_1),
                "a digit switches no hotbar slot while the player types");
        assertTrue(CreativeInventory.isSearchKey(Input.Keys.BACKSPACE), "backspace deletes");

        assertFalse(CreativeInventory.isSearchKey(Input.Keys.ESCAPE),
                "escape always leaves the screen");
        assertFalse(CreativeInventory.isSearchKey(Input.Keys.F11),
                "and a function key still reaches the game");
    }

    @Test
    void aFrameWithoutAWheelNotchLeavesTheListWhereItIs() {
        CreativeInventory creative = new CreativeInventory();
        creative.select(indexOf(CreativeTab.Kind.SEARCH));
        assertTrue(creative.maxRow() > 0, "the whole game does not fit into one page");

        // The screen asks once per frame, also on the frames where the wheel stood still.
        // A zero that moved the list would walk it down on its own and hold it at the end.
        creative.scrollBy(0.0f);

        assertEquals(0, creative.firstRow(), "the list stays at its first row");
        assertEquals(creative.matches().get(0), creative.stackAt(0).item(),
                "and the grid still shows the first item of the list");
    }

    @Test
    void theWheelWalksTheListUpAndDown() {
        CreativeInventory creative = new CreativeInventory();
        creative.select(indexOf(CreativeTab.Kind.SEARCH));

        creative.scrollBy(-1.0f);
        assertEquals(1, creative.firstRow(), "a notch down walks one row down");
        assertEquals(creative.matches().get(CreativeInventory.COLUMNS),
                creative.stackAt(0).item(), "and the grid shows the second row");

        creative.scrollBy(1.0f);
        assertEquals(0, creative.firstRow(), "a notch up walks back to the first row");

        creative.scrollBy(1.0f);
        assertEquals(0, creative.firstRow(), "the first row is the upper end of the list");

        creative.setFirstRow(creative.maxRow() + 5);
        assertEquals(creative.maxRow(), creative.firstRow(), "the last row is the lower end");
    }

    @Test
    void aSlotBesideTheGridStaysEmpty() {
        CreativeInventory creative = new CreativeInventory();

        assertTrue(creative.stackAt(-1).isEmpty(), "before the first slot");
        assertTrue(creative.stackAt(CreativeInventory.PAGE_SIZE).isEmpty(), "behind the last");
        assertTrue(creative.sourceAt(-1).isEmpty(), "the supply has no slot there either");
        assertTrue(creative.sourceAt(CreativeInventory.PAGE_SIZE).isEmpty());
    }

    @Test
    void aTabThatDoesNotExistChangesNothing() {
        CreativeInventory creative = new CreativeInventory();

        creative.select(-1);
        creative.select(99);

        assertEquals(0, creative.selectedIndex(), "the first tab stays chosen");
    }

    /** The tab of the game with the given name, so a test can ask what it holds. */
    private static CreativeTab tab(String name) {
        for (CreativeTab tab : CreativeRegistry.tabs()) {
            if (tab.name().equals(name)) {
                return tab;
            }
        }
        throw new AssertionError("the game has no tab called " + name);
    }

    /** Index of the tab of the game with the given name. */
    private static int indexOfTab(String name) {
        for (int index = 0; index < CreativeRegistry.tabs().size(); index++) {
            if (CreativeRegistry.tabs().get(index).name().equals(name)) {
                return index;
            }
        }
        throw new AssertionError("the game has no tab called " + name);
    }

    /** Index of the first tab of the given kind. */
    private static int indexOf(CreativeTab.Kind kind) {
        for (int index = 0; index < CreativeRegistry.tabs().size(); index++) {
            if (CreativeRegistry.tabs().get(index).kind() == kind) {
                return index;
            }
        }
        throw new AssertionError("the game has no tab of kind " + kind);
    }

    /** Amount of blocks the game owns, the length of the first group. */
    private static int blockCount() {
        return (int) ItemRegistry.all().stream().filter(Item::isBlockItem).count();
    }
}
