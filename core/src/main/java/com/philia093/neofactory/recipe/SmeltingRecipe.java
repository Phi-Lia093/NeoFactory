package com.philia093.neofactory.recipe;

import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.machine.MachineRecipe;

import java.util.Objects;

/**
 * A recipe a machine performs over time.
 * <p>
 * The recipe takes a single item and turns it into another one, which is what a
 * furnace does with an ore. The grid a machine offers to it therefore holds exactly one
 * filled place: the input slot. How long the work takes is part of the recipe, so the
 * same machine can smelt a stone slowly and dry a sponge quickly without any code of
 * its own.
 */
public final class SmeltingRecipe implements MachineRecipe {

    /** Seconds a smelting recipe takes when its file does not say. */
    public static final float DEFAULT_SECONDS = 10.0f;

    private final String name;
    private final Ingredient input;
    private final ItemStack result;
    private final float seconds;

    /**
     * Creates a recipe.
     *
     * @param name name of the recipe, the name of its file
     * @param input item the machine takes
     * @param result what the machine makes
     * @param seconds time one craft takes
     */
    public SmeltingRecipe(String name, Ingredient input, ItemStack result, float seconds) {
        this.name = Objects.requireNonNull(name, "name");
        this.input = Objects.requireNonNull(input, "input");
        this.result = Objects.requireNonNull(result, "result");
        this.seconds = seconds > 0.0f ? seconds : DEFAULT_SECONDS;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public RecipeType type() {
        return RecipeType.SMELTING;
    }

    @Override
    public ItemStack result() {
        return ItemStack.of(result.item(), result.count());
    }

    /** Item this recipe takes. */
    public Ingredient input() {
        return input;
    }

    /** Seconds one craft takes. */
    public float seconds() {
        return seconds;
    }

    /**
     * {@code true} when the grid holds exactly one stack this recipe takes.
     *
     * @param grid grid of the machine, normally the one that holds its input slot
     */
    @Override
    public boolean matches(RecipeGrid grid) {
        if (grid.filledPlaces() != 1) {
            return false;
        }
        return input.matches(onlyStack(grid));
    }

    @Override
    public void consume(RecipeGrid grid) {
        if (!matches(grid)) {
            return;
        }
        ItemStack stack = onlyStack(grid);
        stack.setCount(stack.count() - 1);
    }

    /** The single stack of a grid that holds something. */
    private static ItemStack onlyStack(RecipeGrid grid) {
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (!grid.get(x, y).isEmpty()) {
                    return grid.get(x, y);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public String toString() {
        return "SmeltingRecipe(" + name + ", " + input + " -> " + result + ", " + seconds + "s)";
    }
}
