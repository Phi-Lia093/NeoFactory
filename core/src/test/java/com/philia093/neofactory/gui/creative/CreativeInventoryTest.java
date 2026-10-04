package com.philia093.neofactory.gui.creative;

import com.badlogic.gdx.Input;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.item.Batteries;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.machine.BatteryBoxes;
import com.philia093.neofactory.machine.Diodes;
import com.philia093.neofactory.machine.MachineFamilies;
import com.philia093.neofactory.machine.Transformers;
import com.philia093.neofactory.pipe.PipeMaterials;
import com.philia093.neofactory.pipe.PipeSize;
import com.philia093.neofactory.pipe.Pipes;
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
        assertTrue(tab("tools").matches(Items.DIAMOND_SWORD), "and a sword is one as well");
        assertFalse(tab("food").matches(Items.IRON_PICKAXE), "a tool is not food");
        assertFalse(tab("tools").matches(Items.APPLE), "food is not a tool");
    }

    @Test
    void theGridShowsTheFirstGroupWhenTheScreenOpens() {
        CreativeInventory creative = new CreativeInventory();

        assertEquals("blocks", creative.selectedTab().name(), "the first tab is chosen");
        assertEquals(Items.STONE, creative.stackAt(0).item(), "the first block");
        assertEquals(1, creative.stackAt(0).count(),
                "the shelf shows one piece, see CreativeInventory#shownAt");
        // The furnace and the boiler are the two blocks that were added most recently, but they hold a
        // machine and are therefore listed by the tab of the machines, see CreativeRegistry.
        assertTrue(creative.stackAt(blockCount() - 1).isEmpty(),
                "the free room behind the group of the blocks");
    }

    @Test
    void theTabOfTheMachinesListsTheMachinesOfTheGame() {
        CreativeInventory creative = new CreativeInventory();
        creative.select(indexOfTab("machines"));

        assertEquals(Items.FURNACE, creative.stackAt(0).item(), "the furnace comes first");
        assertEquals(Items.BRONZE_BOILER, creative.stackAt(1).item(),
                "then the boiler of the industry");
        assertEquals(Items.STEAM_FURNACE, creative.stackAt(2).item(),
                "then the machines that run on its steam");
        assertEquals(Items.ALLOY_FURNACE, creative.stackAt(3).item(), "the alloy furnace");
        assertEquals(Items.GRINDER, creative.stackAt(4).item(), "the grinder");
        assertEquals(Items.COMPRESSOR, creative.stackAt(5).item(), "the compressor");
        assertEquals(Items.EXTRACTOR, creative.stackAt(6).item(), "the extractor");
        assertEquals(Items.FORGE_HAMMER, creative.stackAt(7).item(), "and the forge hammer");
        assertEquals(Items.STEEL_BOILER, creative.stackAt(8).item(),
                "then the machines of the age of steel, one per machine of bronze");
        assertEquals(Items.STEEL_STEAM_FURNACE, creative.stackAt(9).item(), "the steam furnace of steel");
        assertEquals(Items.STEEL_ALLOY_FURNACE, creative.stackAt(10).item(), "the alloy furnace of steel");
        assertEquals(Items.STEEL_GRINDER, creative.stackAt(11).item(), "the grinder of steel");
        assertEquals(Items.STEEL_COMPRESSOR, creative.stackAt(12).item(), "the compressor of steel");
        assertEquals(Items.STEEL_EXTRACTOR, creative.stackAt(13).item(), "the extractor of steel");
        assertEquals(Items.STEEL_FORGE_HAMMER, creative.stackAt(14).item(), "and the hammer of steel");
        assertEquals(Items.STEAM_TURBINE_LV, creative.stackAt(15).item(),
                "then the turbines, the first machines of the game that make power");
        assertEquals(Items.STEAM_TURBINE_MV, creative.stackAt(16).item(), "the turbine of the middle voltage");
        assertEquals(Items.STEAM_TURBINE_HV, creative.stackAt(17).item(), "and the one of the high voltage");

        // The eighteen machines of the line of the power stand behind them, one per family and tier of the
        // table of the families: the furnace of the low voltage comes first and the alloy smelter of the high
        // voltage last, see MachineFamilies.
        int slot = 18;
        for (MachineFamilies.Family family : MachineFamilies.all()) {
            for (Voltage tier : MachineFamilies.TIERS) {
                assertEquals(ItemRegistry.byName(family.nameOf(tier)), creative.stackAt(slot++).item(),
                        family.titleOf(tier) + " is listed with the machines");
            }
        }

        // And the twelve boxes of cells behind them, four sizes of each of the three tiers of the line: the
        // machines of the batteries are machines like every other one, see BatteryBoxes. The grid of the screen
        // shows the front of that list, so the whole of it is read from the list itself.
        List<Item> listed = creative.matches();
        int at = 36;
        for (Voltage tier : BatteryBoxes.tiers()) {
            for (int cells : BatteryBoxes.CELLS) {
                String name = BatteryBoxes.nameOf(tier, cells);
                assertEquals(ItemRegistry.byName(name), listed.get(at++),
                        BatteryBoxes.titleOf(tier, cells) + " is listed with the machines");
            }
        }
        // And the fifteen diodes behind them, five widths of each of the three tiers: a diode is a block a
        // line of cables runs through and is listed with the machines for the same reason, see Diodes.
        for (Voltage tier : Diodes.tiers()) {
            for (int width : Diodes.WIDTHS) {
                String name = Diodes.nameOf(tier, width);
                assertEquals(ItemRegistry.byName(name), listed.get(at++),
                        Diodes.titleOf(tier, width) + " is listed with the machines");
            }
        }
        // And the six transformers behind those, three sizes of each of the two ages one joins: they are the
        // machines that step a line from one age into another and are listed here for the same reason, see
        // Transformers.
        for (Voltage tier : Transformers.TIERS) {
            for (int size : Transformers.SIZES) {
                String name = Transformers.nameOf(tier, size);
                assertEquals(ItemRegistry.byName(name), listed.get(at++),
                        Transformers.titleOf(tier, size) + " is listed with the machines");
            }
        }
        assertEquals(36 + BatteryBoxes.COUNT + Diodes.COUNT + Transformers.COUNT, listed.size(),
                "the list holds the machines of the game");
        assertEquals(listed.size(), at, "and every one of them was walked");
        assertEquals(ItemRegistry.byName(MachineFamilies.all().get(0).nameOf(Voltage.LOW)),
                listed.get(18), "the machines of the line come first");
        assertFalse(creative.stackAt(36).isEmpty(), "and the front of the boxes is shown in the grid");
        assertEquals(ItemRegistry.byName(BatteryBoxes.nameOf(Voltage.LOW, 1)), creative.stackAt(36).item());
    }

    @Test
    void theBoxesOfCellsAreListedWithTheMachinesAndNotWithTheCells() {
        CreativeInventory creative = new CreativeInventory();
        String name = BatteryBoxes.nameOf(Voltage.LOW, 4);
        Item box = ItemRegistry.byName(name);

        assertTrue(tab("machines").matches(box), "a box is a machine a player builds");
        assertFalse(tab("materials").matches(box), "and no cell a player carries around");
    }

    @Test
    void theTabOfThePipesListsEveryPipeAndNoMachine() {
        CreativeInventory creative = new CreativeInventory();
        creative.select(indexOfTab("pipes"));

        assertEquals(Pipes.COUNT, creative.matches().size(), "a tab for all the pipes of the game");
        assertEquals(Pipes.of(PipeMaterials.WOOD, PipeSize.SMALL).item(), creative.stackAt(0).item(),
                "wood comes first, the smallest size it is made in at its front");
        assertFalse(tab("pipes").matches(Items.FURNACE), "a machine is no pipe");
        assertFalse(tab("pipes").matches(Items.STONE), "and neither is a stone");
        assertFalse(tab("machines").matches(Pipes.of(PipeMaterials.BRONZE, PipeSize.MEDIUM).item()),
                "and a pipe is no machine");
    }

    @Test
    void theChestIsListedWithTheBlocksAndNotWithTheMachines() {
        CreativeInventory creative = new CreativeInventory();
        creative.select(indexOfTab("blocks"));

        assertTrue(tab("blocks").matches(Items.CHEST), "a chest is a block a player builds");
        assertFalse(tab("machines").matches(Items.CHEST),
                "and no machine, because a container works on nothing");
        assertTrue(creative.matches().contains(Items.CHEST), "the chest is listed with the blocks");
    }

    @Test
    void theCellsOfTheIndustryAreListedWithTheMaterials() {
        CreativeInventory creative = new CreativeInventory();
        creative.select(indexOfTab("materials"));

        Item battery = ItemRegistry.byName("battery_acid_lv");
        assertTrue(tab("materials").matches(battery), "a cell of energy is a thing a player carries");
        assertFalse(tab("machines").matches(battery), "and no machine, because it works on nothing");
        assertFalse(tab("tools").matches(ItemRegistry.byName("battery_lithium_hv")),
                "a cell is held and not swung at a block");
        assertFalse(tab("blocks").matches(ItemRegistry.byName("battery_lithium_hv")));

        for (Batteries.Cell cell : Batteries.all()) {
            Item item = ItemRegistry.byName(Batteries.itemNameOf(cell));
            assertTrue(creative.matches().contains(item),
                    Batteries.itemNameOf(cell) + " is listed with the materials");
        }
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
        assertEquals(Items.LOG_OAK, creative.stackAt(0).item(), "the tenth item starts the row");

        // A scroll far past the end lands on the last row, whichever row that is: the list of the game is
        // as long as the items it holds, so the amount to scroll by is read off the list itself.
        creative.scroll(creative.maxRow() + 1);
        assertEquals(creative.maxRow(), creative.firstRow(), "the scroll stops at the end");
        creative.scroll(-(creative.maxRow() + 1));
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
        assertEquals(4, creative.matches().size(), "the four pickaxes of the ladder");
        assertTrue(creative.matches().contains(Items.WOOD_PICKAXE));
        assertTrue(creative.matches().contains(Items.STONE_PICKAXE));
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
        assertEquals(1, creative.stackAt(0).count(), "and one piece is what a slot shows");
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
