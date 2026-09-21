package com.philia093.neofactory.gui.creative;

import com.badlogic.gdx.Input;
import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * What the creative inventory shows: the chosen tab, the scroll and the grid of items.
 * <p>
 * The class is the whole logic of the screen and knows nothing about drawing, so it is
 * tested without a window, see {@link CreativeTab} for the tabs and
 * {@link com.philia093.neofactory.gui.CreativeInventoryGui} for the other half.
 * <p>
 * The grid is a real {@link Inventory} of {@value #PAGE_SIZE} slots, so an existing
 * container can show it, see
 * {@link com.philia093.neofactory.gui.container.ContainerLayout}. Its slots are result
 * slots: they only hand items out, and {@link #sourceAt(int)} is the endless supply
 * behind them, which is why a stack that was taken from the grid is there again right
 * away. A player therefore never runs out of anything, exactly like in the original
 * game.
 * <p>
 * The visible part of the list is {@value #ROWS} rows of {@value #COLUMNS} items. Every
 * change - another tab, a scroll or another query - picks the items again and refills
 * the grid, so the grid and the list can never disagree. The search box lists everything
 * while it is empty, so a player who opens that tab sees the whole game.
 */
public final class CreativeInventory {

    /** Amount of items in one row of the grid. */
    public static final int COLUMNS = 9;

    /** Amount of rows that fit into the grid at once. */
    public static final int ROWS = 5;

    /** Amount of slots of the grid. */
    public static final int PAGE_SIZE = COLUMNS * ROWS;

    private final List<CreativeTab> tabs;
    private final Inventory grid = new Inventory(PAGE_SIZE);

    /** Index of the chosen tab inside {@link #tabs()}. */
    private int selected;

    /** First row of the list that is shown, the scroll of the grid. */
    private int firstRow;

    /** Text of the search box. */
    private String query = "";

    /** Items of the chosen tab that pass the query, the source of the grid. */
    private List<Item> matches = List.of();

    /** Creates the screen with the tabs of the game, see {@link CreativeRegistry}. */
    public CreativeInventory() {
        this(CreativeRegistry.tabs());
    }

    /**
     * Creates the screen with the given tabs.
     *
     * @param tabs tabs in the order they are drawn, at least one
     */
    public CreativeInventory(List<CreativeTab> tabs) {
        Objects.requireNonNull(tabs, "tabs");
        if (tabs.isEmpty()) {
            throw new IllegalArgumentException("A creative inventory needs at least one tab");
        }
        this.tabs = List.copyOf(tabs);
        refresh();
    }

    /** The tabs, in the order they are drawn. */
    public List<CreativeTab> tabs() {
        return tabs;
    }

    /** The chosen tab. */
    public CreativeTab selectedTab() {
        return tabs.get(selected);
    }

    /** Index of the chosen tab inside {@link #tabs()}. */
    public int selectedIndex() {
        return selected;
    }

    /**
     * Chooses a tab and shows it from its first row.
     *
     * @param index index of the tab, ignored when it does not exist
     */
    public void select(int index) {
        if (index < 0 || index >= tabs.size() || index == selected) {
            return;
        }
        selected = index;
        firstRow = 0;
        refresh();
    }

    /** Text of the search box. */
    public String query() {
        return query;
    }

    /**
     * Writes the text of the search box and shows its result from the first row.
     *
     * @param query text the player typed, {@code null} counts as empty
     */
    public void setQuery(String query) {
        String wanted = query == null ? "" : query;
        if (wanted.equals(this.query)) {
            return;
        }
        this.query = wanted;
        selected = searchTab();
        firstRow = 0;
        refresh();
    }

    /** The items the chosen tab shows, before the scroll cuts them to the page. */
    public List<Item> matches() {
        return matches;
    }

    /** First row of the list that is shown. */
    public int firstRow() {
        return firstRow;
    }

    /** Amount of rows the list of the chosen tab has, at least one. */
    public int rowCount() {
        return Math.max(1, (matches.size() + COLUMNS - 1) / COLUMNS);
    }

    /** Last row the grid may start at, {@code 0} when the list fits into the grid. */
    public int maxRow() {
        return Math.max(0, rowCount() - ROWS);
    }

    /**
     * Scrolls the list by whole rows.
     *
     * @param rows amount of rows to move down, negative scrolls up
     */
    public void scroll(int rows) {
        setFirstRow(firstRow + rows);
    }

    /**
     * Scrolls the list by notches of the wheel.
     * <p>
     * The screen asks once per frame, also on the frames without a notch, so a zero has
     * to move nothing - a caller that maps every value to a step would walk the list down
     * on its own and never let the player scroll back up.
     *
     * @param notches notches of the wheel, positive when it turns up
     */
    public void scrollBy(float notches) {
        if (notches == 0.0f) {
            return;
        }
        scroll(notches > 0.0f ? -1 : 1);
    }

    /**
     * Moves the list to a row.
     *
     * @param row requested first row, clamped to the list
     */
    public void setFirstRow(int row) {
        int wanted = Math.max(0, Math.min(row, maxRow()));
        if (wanted == firstRow) {
            return;
        }
        firstRow = wanted;
        refill();
    }

    /** The grid the container shows, {@value #PAGE_SIZE} result slots. */
    public Inventory grid() {
        return grid;
    }

    /**
     * Stack the grid shows in one of its slots.
     *
     * @param index slot of the grid, {@code 0} to {@value #PAGE_SIZE} minus one
     * @return the stack, {@link ItemStack#EMPTY} when the slot lies outside the grid
     */
    public ItemStack stackAt(int index) {
        if (index < 0 || index >= PAGE_SIZE) {
            return ItemStack.EMPTY;
        }
        return grid.get(index);
    }

    /**
     * The endless stack behind a slot of the grid.
     * <p>
     * Taking a stack out of the grid empties the slot, so the container asks for the
     * next one; this is where it comes from. A slot beyond the items of the tab stays
     * empty, which is what the free room of the last row shows.
     *
     * @param index slot of the grid, {@code 0} to {@value #PAGE_SIZE} minus one
     * @return a full stack of the item, {@link ItemStack#EMPTY} for a free slot
     */
    public ItemStack sourceAt(int index) {
        if (index < 0 || index >= PAGE_SIZE) {
            return ItemStack.EMPTY;
        }
        int position = firstRow * COLUMNS + index;
        if (position < 0 || position >= matches.size()) {
            return ItemStack.EMPTY;
        }
        Item item = matches.get(position);
        return ItemStack.of(item, item.maxStackSize());
    }

    /**
     * {@code true} when the search box owns the key and nothing else may read it.
     * <p>
     * While the player types, a letter belongs into the box and must not reach the hotkeys
     * of the game: without this, a typed {@code E} closed the screen, because the game reads
     * that key as the one that opens the inventory. Everything that writes a character is
     * taken here, and the keys that steer the screen rather than its text are left to the
     * game, so a player can always leave the screen again.
     *
     * @param keyCode key that was pressed, see {@link Input.Keys}
     * @return {@code true} when the search box owns the key
     */
    public static boolean isSearchKey(int keyCode) {
        if (keyCode == Input.Keys.ESCAPE) {
            return false;
        }
        return keyCode < Input.Keys.F1 || keyCode > Input.Keys.F12;
    }

    /** {@code true} when the search tab is chosen. */
    public boolean isSearching() {
        return selectedTab().kind() == CreativeTab.Kind.SEARCH;
    }

    /** {@code true} when the inventory of the player is shown instead of a grid. */
    public boolean showsPlayerInventory() {
        return selectedTab().kind() == CreativeTab.Kind.INVENTORY;
    }

    /** Picks the items of the chosen tab again and refills the grid from the top. */
    private void refresh() {
        matches = List.copyOf(itemsOf(selectedTab()));
        refill();
    }

    /** Refills the grid from the items that are already picked out. */
    private void refill() {
        for (int index = 0; index < PAGE_SIZE; index++) {
            grid.set(index, sourceAt(index));
        }
    }

    /** The items a tab holds, before the scroll cuts them to the page. */
    private List<Item> itemsOf(CreativeTab tab) {
        List<Item> found = new ArrayList<>();
        if (tab.kind() == CreativeTab.Kind.INVENTORY) {
            // The inventory of the player is shown instead of a list of items.
            return found;
        }
        if (tab.kind() == CreativeTab.Kind.SEARCH) {
            String needle = query.trim().toLowerCase(Locale.ROOT);
            for (Item item : ItemRegistry.all()) {
                if (item == Items.AIR) {
                    continue;
                }
                // An empty box shows everything, which is what a player sees first when
                // the search tab is opened.
                if (needle.isEmpty() || item.name().toLowerCase(Locale.ROOT).contains(needle)
                        || item.displayName().toLowerCase(Locale.ROOT).contains(needle)) {
                    found.add(item);
                }
            }
            return found;
        }
        for (Item item : ItemRegistry.all()) {
            if (tab.matches(item)) {
                found.add(item);
            }
        }
        return found;
    }

    /** Index of the search tab, {@code 0} when the game has none. */
    private int searchTab() {
        for (int index = 0; index < tabs.size(); index++) {
            if (tabs.get(index).kind() == CreativeTab.Kind.SEARCH) {
                return index;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return "CreativeInventory(" + selectedTab().name() + ", row " + firstRow + " of "
                + maxRow() + ", " + matches.size() + " items)";
    }
}
