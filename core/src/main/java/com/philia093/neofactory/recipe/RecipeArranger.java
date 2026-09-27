package com.philia093.neofactory.recipe;

import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lays a recipe into a grid, out of what a player carries.
 * <p>
 * This is what a click on a recipe of the screen of recipes does: the cells are filled so that the player
 * only has to take the product, see {@link RecipeGridWriter}. Three rules keep the gesture honest:
 * <ul>
 *     <li><b>What already lies right is left alone</b> - a cell that holds one of the items its ingredient
 *         asks for counts as laid out, so a pattern a player started is not torn up;</li>
 *     <li><b>Everything else comes out of the inventory of the player</b>, one item per cell, the very
 *         arithmetic a click on a slot does - the gesture can only use what the player really has;</li>
 *     <li><b>An item that is missing is reported and not conjured</b>, unless the caller plays in creative
 *         mode, where nothing is paid for in the first place, see {@code GameMode}.</li>
 * </ul>
 * A recipe that is wider than the grid is laid out as far as it fits and the rest is reported as missing,
 * which is what a pattern of nine cells does in the two by two field of the player.
 */
public final class RecipeArranger {

    /**
     * What came of laying a recipe out.
     *
     * @param recipe recipe that was laid out
     * @param placed amount of cells that were filled
     * @param missing items the ingredients asked for and nobody had, in the order of the recipe
     */
    public record Arrangement(Recipe recipe, int placed, List<Item> missing) {

        /** Checks the fields, so a layout always names its recipe. */
        public Arrangement {
            Objects.requireNonNull(recipe, "recipe");
            missing = List.copyOf(missing);
        }

        /** {@code true} when every ingredient of the recipe lies in the grid now. */
        public boolean isComplete() {
            return missing.isEmpty();
        }

        @Override
        public String toString() {
            return "Arrangement(" + recipe.name() + ", " + placed + " cells"
                    + (isComplete() ? ", complete" : ", missing " + missing.size()) + ")";
        }
    }

    /**
     * One cell a recipe asks for.
     *
     * @param ingredient what belongs there
     * @param x column of the cell
     * @param y row of the cell
     */
    private record Place(Ingredient ingredient, int x, int y) {
    }

    private RecipeArranger() {
        // Utility class: never instantiated.
    }

    /** How many items of one kind fit into a single cell of a field, which is the size of a stack. */
    public static final int FULL_STACK = 64;

    /**
     * Lays a recipe into a grid.
     *
     * @param recipe recipe to lay out
     * @param into grid that is filled: the field of a table, the input of a machine or the field of the player
     * @param source inventory the items are taken from, {@code null} for a grid that is filled from nowhere
     * @param conjure {@code true} in creative mode, where a missing item is put into the cell anyway
     * @return what was placed and what is missing
     */
    public static Arrangement arrange(Recipe recipe, RecipeGridWriter into, Inventory source,
            boolean conjure) {
        Objects.requireNonNull(recipe, "recipe");
        Objects.requireNonNull(into, "into");

        List<Item> missing = new ArrayList<>();
        int placed = 0;
        for (Place place : placesOf(recipe, into)) {
            if (lay(place.ingredient(), into, place.x(), place.y(), source, conjure, 1)) {
                placed++;
            } else {
                missing.add(representative(place.ingredient()));
            }
        }
        return new Arrangement(recipe, placed, missing);
    }

    /**
     * Lays a recipe into a grid, with as many items of an ingredient in a cell as the request asks for.
     * <p>
     * A cell of a field holds a stack and not a lone item, and a player who holds a stack of planks means to
     * craft as often as that stack lasts them, which is what the shift key asks for. Items of one kind only
     * are gathered into a cell: an ingredient of a tag is satisfied by several items, and a stack of two
     * kinds is not a stack.
     *
     * @param recipe recipe to lay out
     * @param into grid that is filled
     * @param source inventory the items are taken from, {@code null} for a grid that is filled from nowhere
     * @param conjure {@code true} in creative mode, where a missing item is put into the cell anyway
     * @param copies how many items of an ingredient one cell takes at most
     * @return what was placed and what is missing
     */
    public static Arrangement arrange(Recipe recipe, RecipeGridWriter into, Inventory source,
            boolean conjure, int copies) {
        if (copies <= 1) {
            return arrange(recipe, into, source, conjure);
        }
        Objects.requireNonNull(recipe, "recipe");
        Objects.requireNonNull(into, "into");

        List<Item> missing = new ArrayList<>();
        int placed = 0;
        for (Place place : placesOf(recipe, into)) {
            if (lay(place.ingredient(), into, place.x(), place.y(), source, conjure, copies)) {
                placed++;
            } else {
                missing.add(representative(place.ingredient()));
            }
        }
        return new Arrangement(recipe, placed, missing);
    }

    /**
     * {@code true} when a recipe fits a grid whole.
     *
     * @param recipe recipe to lay out
     * @param grid grid it would be laid into
     * @return {@code true} when no ingredient falls outside the grid
     */
    public static boolean fits(Recipe recipe, RecipeGrid grid) {
        for (Place place : placesOf(recipe, grid)) {
            if (place.x() >= grid.width() || place.y() >= grid.height()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Where the ingredients of a recipe belong in a grid.
     * <p>
     * A pattern keeps its shape and is centred in the grid, which is what a player of the original game
     * expects of a recipe that is laid out for them: three planks over two sticks land in the middle
     * column of a field of three by three. A bag of ingredients has no shape, so its entries are laid out
     * row by row from the upper left cell.
     */
    private static List<Place> placesOf(Recipe recipe, RecipeGrid grid) {
        List<Place> places = new ArrayList<>();
        if (recipe instanceof ShapedRecipe shaped) {
            int offsetX = Math.max(0, (grid.width() - shaped.width()) / 2);
            int offsetY = Math.max(0, (grid.height() - shaped.height()) / 2);
            for (int row = 0; row < shaped.height(); row++) {
                for (int column = 0; column < shaped.width(); column++) {
                    Ingredient ingredient = shaped.ingredient(column, row);
                    if (!ingredient.isEmpty()) {
                        places.add(new Place(ingredient, offsetX + column, offsetY + row));
                    }
                }
            }
            return places;
        }
        int cell = 0;
        for (Ingredient ingredient : recipe.ingredients()) {
            places.add(new Place(ingredient, cell % grid.width(), cell / grid.width()));
            cell++;
        }
        return places;
    }

    /**
     * Fills one cell, {@code true} when it holds an item of the ingredient afterwards.
     *
     * @param copies how many items of the ingredient the cell takes at most
     */
    private static boolean lay(Ingredient ingredient, RecipeGridWriter into, int x, int y,
            Inventory source, boolean conjure, int copies) {
        if (x >= into.width() || y >= into.height()) {
            // The recipe is wider than the grid: the rest of it is reported as missing, see #fits.
            return false;
        }
        if (ingredient.matches(into.get(x, y))) {
            return true;
        }
        int limit = Math.max(1, copies);
        Item item = kindIn(source, ingredient);
        if (item == null && conjure) {
            item = representative(ingredient);
            if (item != null) {
                into.place(x, y, ItemStack.of(item, limit));
                return true;
            }
        }
        if (item == null) {
            return false;
        }
        int taken = takeMany(source, ingredient, item, limit);
        if (taken == 0) {
            return false;
        }
        into.place(x, y, ItemStack.of(item, taken));
        return true;
    }

    /** The item an inventory holds for an ingredient, {@code null} when it holds none of it. */
    private static Item kindIn(Inventory source, Ingredient ingredient) {
        if (source == null) {
            return null;
        }
        for (int slot = 0; slot < source.size(); slot++) {
            ItemStack stack = source.get(slot);
            if (!stack.isEmpty() && ingredient.matches(stack)) {
                return stack.item();
            }
        }
        return null;
    }

    /**
     * Takes up to {@code limit} items of one kind out of an inventory, {@code 0} when it holds none of it.
     * <p>
     * Only the kind that was picked is taken, even when another item of the same ingredient lies in an
     * earlier slot: a cell is filled with a stack of one kind and the rest of the inventory is left alone.
     */
    private static int takeMany(Inventory source, Ingredient ingredient, Item item, int limit) {
        int taken = 0;
        for (int slot = 0; slot < source.size() && taken < limit; slot++) {
            ItemStack stack = source.get(slot);
            if (stack.isEmpty() || !ingredient.matches(stack) || !item.equals(stack.item())) {
                continue;
            }
            int want = Math.min(limit - taken, stack.count());
            stack.setCount(stack.count() - want);
            if (stack.count() <= 0) {
                source.set(slot, ItemStack.EMPTY);
            }
            taken += want;
        }
        return taken;
    }

    /** An item that satisfies an ingredient, {@code null} for an ingredient that asks for nothing. */
    private static Item representative(Ingredient ingredient) {
        return ingredient.isEmpty() ? null : ingredient.items().iterator().next();
    }
}
