package com.philia093.neofactory.gui.recipe;

import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.recipe.Recipe;
import com.philia093.neofactory.recipe.RecipeIndex;
import com.philia093.neofactory.recipe.RecipeType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * One group of the items a screen of recipes browses.
 * <p>
 * The groups are what a player of a factory thinks in: the blocks, the machines, the pipes and the rest of
 * the creative inventory, and then one group per kind of recipe - the smelting, the grinding and so on -
 * which lists the items those recipes make. Choosing a group shows its items, and choosing an item shows
 * the recipes that make it or take it.
 *
 * @param name name written in the head of the panel
 * @param icon item drawn as the tab of the group
 * @param items items of the group, in the order they are listed
 */
public record RecipeCategory(String name, Item icon, List<Item> items) {

    /** Checks the fields, so a group always has a name and a list. */
    public RecipeCategory {
        Objects.requireNonNull(name, "name");
        items = List.copyOf(items);
    }

    /** Amount of items this group holds. */
    public int size() {
        return items.size();
    }

    /**
     * Builds the groups of the screen: the categories of the creative inventory first, one group per kind
     * of recipe behind them.
     * <p>
     * A group of recipes lists the items those recipes make and not the recipes themselves, so every group
     * of the screen is a list of items and a click in any of them asks the same question, see
     * {@link RecipeBrowserGui}.
     *
     * @param index index of the recipes of the game
     * @return the groups, in the order the arrows of the head walk through them
     */
    public static List<RecipeCategory> of(RecipeIndex index) {
        List<RecipeCategory> groups = new ArrayList<>();
        for (com.philia093.neofactory.gui.creative.CreativeTab tab
                : com.philia093.neofactory.gui.creative.CreativeRegistry.tabs()) {
            switch (tab.kind()) {
                case ITEMS -> groups.add(new RecipeCategory(tab.title(), tab.icon(), itemsOf(tab)));
                default -> {
                    // The search and the inventory of the player are no groups: what a player looks for is
                    // found by typing, and what they own lies in their own screen.
                }
            }
        }
        for (RecipeType type : RecipeType.all()) {
            List<Item> products = new ArrayList<>();
            LinkedHashSet<Item> seen = new LinkedHashSet<>();
            for (Recipe recipe : index.ofType(type)) {
                if (seen.add(recipe.result().item())) {
                    products.add(recipe.result().item());
                }
            }
            groups.add(new RecipeCategory(type.name(), products.isEmpty() ? null : products.get(0),
                    products));
        }
        return List.copyOf(groups);
    }

    /** Every item of a tab of the creative inventory. */
    private static List<Item> itemsOf(com.philia093.neofactory.gui.creative.CreativeTab tab) {
        List<Item> found = new ArrayList<>();
        for (Item item : com.philia093.neofactory.item.ItemRegistry.all()) {
            if (tab.matches(item)) {
                found.add(item);
            }
        }
        return found;
    }
}
