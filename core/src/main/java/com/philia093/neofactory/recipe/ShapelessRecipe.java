package com.philia093.neofactory.recipe;

import com.philia093.neofactory.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A recipe whose ingredients only have to be present.
 * <p>
 * The order and the places do not matter: the grid has to hold exactly one stack per
 * ingredient and every ingredient has to find a stack it accepts. A log and three
 * planks - or four clay balls - are such recipes.
 */
public final class ShapelessRecipe implements Recipe {

    private final String name;
    private final List<Ingredient> ingredients;
    private final ItemStack result;

    /**
     * Creates a recipe.
     *
     * @param name name of the recipe, the name of its file
     * @param ingredients one ingredient per stack the grid has to hold
     * @param result what the recipe makes
     */
    public ShapelessRecipe(String name, List<Ingredient> ingredients, ItemStack result) {
        this.name = Objects.requireNonNull(name, "name");
        this.ingredients = List.copyOf(Objects.requireNonNull(ingredients, "ingredients"));
        this.result = Objects.requireNonNull(result, "result");
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
        return RecipeType.CRAFTING_SHAPELESS;
    }

    @Override
    public ItemStack result() {
        return ItemStack.of(result.item(), result.count());
    }

    /** Ingredients this recipe accepts, one per used place of the grid. */
    public List<Ingredient> ingredients() {
        return ingredients;
    }

    @Override
    public boolean matches(RecipeGrid grid) {
        // Every ingredient needs a place of its own: four clay balls in a single slot are one
        // filled place and not the four places the recipe asks for. Counting the places is
        // what keeps a recipe from eating a whole stack for a single craft, and a field with
        // four slots still works when the player spreads the balls over all of them.
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
     * <p>
     * One place is used by one ingredient, so four ingredients need four filled places even
     * when one of them holds a whole stack.
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
        return "ShapelessRecipe(" + name + ", " + ingredients.size() + " ingredients -> "
                + result + ")";
    }
}
