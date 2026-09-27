package com.philia093.neofactory.recipe;

import com.philia093.neofactory.item.Item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the game can make and what it can make it of, looked up by item.
 * <p>
 * The table is built once from {@link RecipeRegistry}, after the recipe files were read, and it answers
 * the two questions a player asks while they look at an item: <b>how is this made</b> -
 * {@link #resultsOf(Item)} - and <b>what is this used for</b> - {@link #usesOf(Item)}. Both answers are
 * the recipes themselves, so a screen of recipes only has to lay them out.
 * <p>
 * <b>The table is an index and not a copy.</b> The recipes it hands out are the very objects the game
 * runs, which is what keeps a clicked recipe and a machine in step: a screen that shows a recipe and a
 * machine that performs it can never mean two different things.
 */
public final class RecipeIndex {

    /** Recipes that make an item, one list per product. */
    private final Map<Item, List<Recipe>> byProduct = new LinkedHashMap<>();

    /** Recipes that take an item, one list per material. */
    private final Map<Item, List<Recipe>> byIngredient = new LinkedHashMap<>();

    /** Creates an index of every recipe the game holds right now. */
    public RecipeIndex() {
        rebuild();
    }

    /**
     * Builds the index from the recipe table again.
     * <p>
     * Called when the index is created and when a test registers recipes of its own: the loader fills
     * the table once while the game starts, so nothing of a running game ever rebuilds this.
     */
    public void rebuild() {
        byProduct.clear();
        byIngredient.clear();
        for (RecipeType type : RecipeType.all()) {
            for (Recipe recipe : RecipeRegistry.recipes(type)) {
                byProduct.computeIfAbsent(recipe.result().item(), product -> new ArrayList<>())
                        .add(recipe);
                for (Ingredient ingredient : recipe.ingredients()) {
                    for (Item item : ingredient.items()) {
                        List<Recipe> listed = byIngredient.computeIfAbsent(item,
                                material -> new ArrayList<>());
                        if (!listed.contains(recipe)) {
                            listed.add(recipe);
                        }
                    }
                }
            }
        }
    }

    /**
     * The recipes that make an item.
     *
     * @param item item a player looks at
     * @return the recipes, an empty list when nothing makes it
     */
    public List<Recipe> resultsOf(Item item) {
        return listOf(byProduct, item);
    }

    /**
     * The recipes that take an item.
     * <p>
     * A recipe that takes one item in three cells - a pickaxe of three ingots - is listed once and not
     * three times: what a player asks is which recipes use an ingot, not how many of its cells do.
     *
     * @param item item a player looks at
     * @return the recipes, an empty list when nothing uses it
     */
    public List<Recipe> usesOf(Item item) {
        return listOf(byIngredient, item);
    }

    /**
     * Every recipe of one kind, the list a category of the screen of recipes shows.
     *
     * @param type kind of the recipe
     * @return the recipes, an empty list when the game holds none of that kind
     */
    public List<Recipe> ofType(RecipeType type) {
        return RecipeRegistry.recipes(type);
    }

    /**
     * The recipes whose product a player typed into a box.
     * <p>
     * The search runs over the product of every recipe, so a player who types {@code pickaxe} finds the
     * four recipes of the ladder instead of the items themselves, see
     * {@link com.philia093.neofactory.item.ItemSearch}.
     *
     * @param query text a player typed, {@code null} and empty find every recipe
     * @return the recipes, in the order their products were registered
     */
    public List<Recipe> find(String query) {
        List<Recipe> found = new ArrayList<>();
        for (List<Recipe> recipes : byProduct.values()) {
            for (Recipe recipe : recipes) {
                if (com.philia093.neofactory.item.ItemSearch.matches(recipe.result().item(), query)) {
                    found.add(recipe);
                }
            }
        }
        return found;
    }

    /** Amount of items the index knows a recipe for, as a product or as a material. */
    public int itemCount() {
        return byProduct.size();
    }

    /** The list of a map, empty when the key is unknown. */
    private static List<Recipe> listOf(Map<Item, List<Recipe>> lists, Item key) {
        List<Recipe> found = lists.get(key);
        return found == null ? List.of() : List.copyOf(found);
    }

    @Override
    public String toString() {
        return "RecipeIndex(" + byProduct.size() + " products, " + byIngredient.size()
                + " materials)";
    }
}
