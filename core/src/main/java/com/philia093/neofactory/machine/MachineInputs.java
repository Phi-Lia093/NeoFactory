package com.philia093.neofactory.machine;

import com.philia093.neofactory.fluid.FluidStorage;
import com.philia093.neofactory.recipe.RecipeGrid;

import java.util.Objects;

/**
 * The simple {@link MachineInput} a machine hands to a recipe: a grid of input slots
 * and the tanks a recipe may drain.
 * <p>
 * The class holds no logic of its own, the defaults of {@link MachineInput} do the
 * work. It exists so that a machine can build its views once, when it is created, and
 * hand the same instances out every time a recipe is asked.
 */
public final class MachineInputs implements MachineInput {

    private final RecipeGrid items;
    private final FluidStorage[] tanks;

    /**
     * Creates an input view.
     *
     * @param items grid of the input slots
     * @param tanks tanks a recipe may drain, in the order they are offered
     */
    public MachineInputs(RecipeGrid items, FluidStorage... tanks) {
        this.items = Objects.requireNonNull(items, "items");
        this.tanks = tanks == null ? new FluidStorage[0] : tanks.clone();
    }

    @Override
    public RecipeGrid items() {
        return items;
    }

    @Override
    public int tankCount() {
        return tanks.length;
    }

    @Override
    public FluidStorage tank(int index) {
        return tanks[index];
    }

    @Override
    public String toString() {
        return "MachineInputs(" + items + ", " + tanks.length + " tanks)";
    }
}
