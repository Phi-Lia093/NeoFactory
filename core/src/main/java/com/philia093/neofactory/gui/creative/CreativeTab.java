package com.philia093.neofactory.gui.creative;

import com.philia093.neofactory.item.Item;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * One tab of the creative inventory.
 * <p>
 * A tab says what the list beside it holds: the items of a category, the result of
 * the search box or the inventory of the player, see {@link Kind}. It carries no
 * state of its own - which tab is chosen and how far its list is scrolled belongs to
 * {@link CreativeInventory} - so the same definition serves every world.
 * <p>
 * The icon is the item drawn on the tab, exactly the way the item looks in a slot,
 * see {@link com.philia093.neofactory.render.BlockTextureCache#itemIcon(Item)}. A tab
 * without an icon stays a plain label and is recognised by its title alone, which is
 * what the search tab and the inventory tab do.
 * <p>
 * The name is stable and meant for tests and log lines, the title is what the player
 * reads in the tooltip of the tab.
 */
public final class CreativeTab {

    /** What a tab lists. */
    public enum Kind {

        /** Items of a category, {@link CreativeTab#matches(Item)} decides which. */
        ITEMS,

        /** The result of the search box, which the query of the inventory decides. */
        SEARCH,

        /**
         * The inventory of the player.
         * <p>
         * The tab shows no list of its own: the very screen of the player inventory is
         * shown instead, so a player keeps the crafting field while being in creative
         * mode and handles their own items the way they always do.
         */
        INVENTORY
    }

    private final String name;
    private final String title;
    private final Kind kind;
    private final Item icon;
    private final Predicate<Item> filter;

    private CreativeTab(String name, String title, Kind kind, Item icon,
            Predicate<Item> filter) {
        this.name = Objects.requireNonNull(name, "name");
        this.title = Objects.requireNonNull(title, "title");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.icon = icon;
        this.filter = filter;
    }

    /**
     * Creates a tab that holds the items of a category.
     *
     * @param name stable name of the tab, used by tests and log lines
     * @param title name the player reads
     * @param icon item drawn on the tab, {@code null} for a tab without one
     * @param filter decides which items belong to the category
     * @return the tab
     */
    public static CreativeTab items(String name, String title, Item icon,
            Predicate<Item> filter) {
        return new CreativeTab(name, title, Kind.ITEMS, icon,
                Objects.requireNonNull(filter, "filter"));
    }

    /**
     * Creates the tab that shows the result of the search box.
     *
     * @param name stable name of the tab
     * @param title name the player reads
     * @param icon item drawn on the tab, {@code null} for a tab without one
     * @return the tab
     */
    public static CreativeTab search(String name, String title, Item icon) {
        return new CreativeTab(name, title, Kind.SEARCH, icon, item -> false);
    }

    /**
     * Creates the tab that shows the inventory of the player.
     *
     * @param name stable name of the tab
     * @param title name the player reads
     * @param icon item drawn on the tab, {@code null} for a tab without one
     * @return the tab
     */
    public static CreativeTab inventory(String name, String title, Item icon) {
        return new CreativeTab(name, title, Kind.INVENTORY, icon, item -> false);
    }

    /** Stable name of the tab. */
    public String name() {
        return name;
    }

    /** Name of the tab the player reads in its tooltip. */
    public String title() {
        return title;
    }

    /** What this tab lists. */
    public Kind kind() {
        return kind;
    }

    /** Item drawn on the tab, {@code null} when the tab carries no icon. */
    public Item icon() {
        return icon;
    }

    /** {@code true} when the tab draws an icon on itself. */
    public boolean hasIcon() {
        return icon != null;
    }

    /** {@code true} when this tab lists the items of a category. */
    public boolean isItems() {
        return kind == Kind.ITEMS;
    }

    /**
     * {@code true} when an item belongs to this tab.
     * <p>
     * Only a category answers with {@code true}: the search tab and the inventory tab
     * have no list of items of their own.
     *
     * @param item item to test
     * @return {@code true} when the category of this tab holds the item
     */
    public boolean matches(Item item) {
        return kind == Kind.ITEMS && item != null && filter.test(item);
    }

    @Override
    public String toString() {
        return "CreativeTab(" + name + ", " + kind + ")";
    }
}
