package com.philia093.neofactory.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Finds an item by what a player typed into a box.
 * <p>
 * Three names of an item are searched, because a player of a factory knows an item by any of them: the
 * name it is written down as, the name it reads as on screen, and the chemical formula of its material -
 * somebody who types {@code Fe} looks for every shape of iron, see
 * {@link com.philia093.neofactory.material.Material#formula()}.
 * <p>
 * <b>An empty box finds everything</b>, which is what a player sees first when a list is opened, and a
 * search never ignores the case: {@code Stone} and {@code stone} find the same item.
 */
public final class ItemSearch {

    private ItemSearch() {
        // Utility class: never instantiated.
    }

    /**
     * {@code true} when a query finds an item.
     *
     * @param item item to look at, {@code null} and air are never found
     * @param query text a player typed, {@code null} and empty find every item
     * @return {@code true} when the item belongs to the result
     */
    public static boolean matches(Item item, String query) {
        if (item == null || item == Items.AIR) {
            return false;
        }
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return true;
        }
        return item.name().toLowerCase(Locale.ROOT).contains(needle)
                || item.displayName().toLowerCase(Locale.ROOT).contains(needle)
                || item.chemicalFormula().toLowerCase(Locale.ROOT).contains(needle);
    }

    /**
     * Every item of the game a query finds.
     *
     * @param query text a player typed, {@code null} and empty find every item
     * @return the items, in the order the game registered them
     */
    public static List<Item> all(String query) {
        List<Item> found = new ArrayList<>();
        for (Item item : ItemRegistry.all()) {
            if (matches(item, query)) {
                found.add(item);
            }
        }
        return found;
    }
}
