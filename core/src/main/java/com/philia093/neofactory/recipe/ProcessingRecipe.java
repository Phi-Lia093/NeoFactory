package com.philia093.neofactory.recipe;

import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.machine.MachineRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A recipe a machine works through, of any age: one or two items go in, one item comes out.
 * <p>
 * <b>One file serves every machine that can run it.</b> A recipe says what it takes, what it makes, how long
 * a craft takes and how much power it draws a tick - and every machine that reads its group decides for
 * itself what that power means: a furnace that burns coal ignores it and works by the clock, a machine of the
 * age of steam turns it into millibuckets and drinks them, and a machine of the electric age pays it out of
 * its buffer and over-clocks it if its line is stronger, see {@code SmeltingMachine}, {@code SteamMachine}
 * and {@code ElectricMachine}. That is what makes a group a group: the furnace of bronze and the furnace of
 * the high voltage read the very same file and neither of them owns it.
 * <p>
 * <b>Steam is the energy of a recipe written in millibuckets.</b> Four millibuckets of steam are worth one
 * unit, so what a machine of the age of steam spends follows from {@link #energy()}, see
 * {@link MachineRecipe#steam()}.
 * <p>
 * <b>The ingredients are places, not stacks.</b> A recipe that names two ingredients needs two filled places
 * of the input of its machine and takes one item out of each of them: filling a single slot with a whole
 * stack is not what a two-ingredient recipe asks for, see {@link ShapelessRecipe}.
 */
public final class ProcessingRecipe implements MachineRecipe, EnergyRecipe {

    /** Seconds a recipe takes when its file does not say. */
    public static final float DEFAULT_SECONDS = 10.0f;

    private final String name;
    private final RecipeType type;
    private final List<Ingredient> ingredients;
    private final ItemStack result;
    private final float seconds;
    private final int euPerTick;
    private final int voltage;

    /**
     * Creates a recipe.
     *
     * @param name name of the recipe, the name of its file
     * @param type kind of the recipe, which is the group of machines that reads it
     * @param ingredients one ingredient per place of the input a craft fills
     * @param result what the machine makes
     * @param seconds time one craft takes
     * @param euPerTick power the recipe draws a tick, {@code 0} for a recipe that draws none
     * @param voltage voltage the recipe asks for, the tier of the machine that may run it
     */
    public ProcessingRecipe(String name, RecipeType type, List<Ingredient> ingredients, ItemStack result,
            float seconds, int euPerTick, int voltage) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.ingredients = List.copyOf(Objects.requireNonNull(ingredients, "ingredients"));
        this.result = Objects.requireNonNull(result, "result");
        this.seconds = seconds > 0.0f ? seconds : DEFAULT_SECONDS;
        this.euPerTick = Math.max(0, euPerTick);
        this.voltage = Math.max(0, voltage);
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
    @Override
    public List<Ingredient> ingredients() {
        return ingredients;
    }

    @Override
    public float seconds() {
        return seconds;
    }

    /** Power this recipe draws a tick, in units, {@code 0} for a recipe that draws none. */
    @Override
    public int euPerTick() {
        return euPerTick;
    }

    /** Voltage this recipe asks for, the tier of the machine that may run it. */
    @Override
    public int voltage() {
        return voltage;
    }

    /**
     * Energy one craft of this recipe costs, the total a machine of the electric age pays.
     * <p>
     * What a recipe draws is what it draws every tick, so the total follows from the power and the time and
     * is not a number of its file, see {@link EnergyRecipe#totalEu()}. The steam a machine of the age of
     * steam spends is the same number written in millibuckets, see {@link MachineRecipe#steam()}.
     */
    @Override
    public int energy() {
        return totalEu();
    }

    @Override
    public boolean matches(RecipeGrid grid) {
        // One place is used by one ingredient, so a recipe of two ingredients needs two filled places even
        // when one of them holds a whole stack, see ShapelessRecipe.
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
        return "ProcessingRecipe(" + name + ", " + ingredients.size() + " ingredients -> " + result + ", "
                + seconds + "s, " + euPerTick + "EU/t at " + voltage + "V)";
    }
}
