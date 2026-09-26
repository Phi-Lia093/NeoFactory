package com.philia093.neofactory.recipe;

import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.machine.MachineRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A recipe a machine of the age of steam performs over time.
 * <p>
 * It is the recipe of every machine that runs on steam: one or two items go in, one item comes out, and the
 * work takes a number of seconds and a number of millibuckets of steam, see {@link #steam()}. One class is
 * enough for all of them because a machine of that age only differs in the slots it offers and in the recipes
 * it reads - a grinder turns an ore into dust with a single ingredient, an alloy furnace mixes two of them into
 * one - so nothing of the recipe depends on the machine that runs it, see {@link MachineRecipe}.
 * <p>
 * <b>The ingredients are places, not stacks.</b> A recipe that names two ingredients needs two filled places of
 * the input of its machine and takes one item out of each of them, the way
 * {@link ShapelessRecipe} counts them: filling a single slot with a whole stack is not what a two-ingredient
 * recipe asks for.
 */
public final class SteamRecipe implements MachineRecipe {

    /** Seconds a recipe of the age of steam takes when its file does not say. */
    public static final float DEFAULT_SECONDS = 8.0f;

    private final String name;
    private final RecipeType type;
    private final List<Ingredient> ingredients;
    private final ItemStack result;
    private final float seconds;
    private final int steam;

    /**
     * Creates a recipe.
     *
     * @param name name of the recipe, the name of its file
     * @param type kind of the recipe, which is the machine that reads it
     * @param ingredients one ingredient per place of the input a craft fills
     * @param result what the machine makes
     * @param seconds time one craft takes
     * @param steam steam one craft spends, in millibuckets, {@code 0} for a recipe that spends none
     */
    public SteamRecipe(String name, RecipeType type, List<Ingredient> ingredients, ItemStack result,
            float seconds, int steam) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.ingredients = List.copyOf(Objects.requireNonNull(ingredients, "ingredients"));
        this.result = Objects.requireNonNull(result, "result");
        this.seconds = seconds > 0.0f ? seconds : DEFAULT_SECONDS;
        this.steam = Math.max(0, steam);
        if (this.ingredients.isEmpty()) {
            throw new IllegalArgumentException("A recipe needs at least one ingredient: " + name);
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public RecipeType type() {
        return type;
    }

    @Override
    public ItemStack result() {
        return ItemStack.of(result.item(), result.count());
    }

    /** Ingredients this recipe accepts, one per used place of the input. */
    public List<Ingredient> ingredients() {
        return ingredients;
    }

    @Override
    public float seconds() {
        return seconds;
    }

    /** Steam one craft of this recipe spends, in millibuckets. */
    @Override
    public int steam() {
        return steam;
    }

    @Override
    public boolean matches(RecipeGrid grid) {
        // One place is used by one ingredient, so a recipe of two ingredients needs two filled places even when
        // one of them holds a whole stack, see ShapelessRecipe.
        if (grid.filledPlaces() != ingredients.size()) {
            return false;
        }
        return countFound(grid, false) == ingredients.size();
    }

    @Override
    public void consume(RecipeGrid grid) {
        countFound(grid, true);
    }

    /**
     * Counts how many ingredients find a place of their own.
     *
     * @param grid grid to look at
     * @param consume {@code true} to take one item out of every used place
     * @return the amount of ingredients that were satisfied
     */
    private int countFound(RecipeGrid grid, boolean consume) {
        List<ItemStack> stacks = stacksOf(grid);
        boolean[] used = new boolean[stacks.size()];
        int found = 0;
        for (Ingredient ingredient : ingredients) {
            for (int index = 0; index < stacks.size(); index++) {
                if (used[index] || !ingredient.matches(stacks.get(index))) {
                    continue;
                }
                used[index] = true;
                found++;
                if (consume) {
                    ItemStack stack = stacks.get(index);
                    stack.setCount(stack.count() - 1);
                }
                break;
            }
        }
        return found;
    }

    /** Every stack of the grid that holds something, counted row by row. */
    private static List<ItemStack> stacksOf(RecipeGrid grid) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (!grid.get(x, y).isEmpty()) {
                    stacks.add(grid.get(x, y));
                }
            }
        }
        return stacks;
    }

    @Override
    public String toString() {
        return "SteamRecipe(" + name + ", " + ingredients.size() + " ingredients -> " + result + ", "
                + seconds + "s, " + steam + "mB)";
    }
}
