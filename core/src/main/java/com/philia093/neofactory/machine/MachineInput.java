package com.philia093.neofactory.machine;

import com.philia093.neofactory.recipe.RecipeGrid;

/**
 * The input of a machine, seen by a recipe.
 * <p>
 * A recipe never touches a machine: it is offered this view and may say whether it
 * recognises the content and what it would take out of it. The view is what makes one
 * recipe work for a furnace, whose input is a single slot, and for a reactor, whose
 * input is several slots and a tank, without either the recipe or the machine knowing
 * the other one.
 * <p>
 * A view that a machine hands out while it looks for a recipe must stay valid, but
 * nothing in it may be changed by {@link #hasFluid(FluidType, int)}.
 */
public interface MachineInput {

    /** Items the machine offers, one place per input slot. */
    RecipeGrid items();

    /** Amount of tanks a recipe may take fluid out of. */
    int tankCount();

    /**
     * One of those tanks.
     *
     * @param index tank index, {@code 0 <= index < tankCount()}
     * @return the tank
     */
    FluidStorage tank(int index);

    /**
     * {@code true} when one of the tanks holds at least this much of a fluid.
     *
     * @param fluid kind of the fluid
     * @param amount amount in units of the game
     * @return {@code true} when a recipe could be satisfied
     */
    default boolean hasFluid(FluidType fluid, int amount) {
        for (int index = 0; index < tankCount(); index++) {
            FluidStorage tank = tank(index);
            if (tank.fluid() == fluid && tank.amount() >= amount) {
                return true;
            }
        }
        return false;
    }

    /**
     * Takes a fluid out of the first tank that holds enough of it.
     *
     * @param fluid kind of the fluid
     * @param amount amount that is wanted
     * @return the amount that was taken, {@code 0} when no tank held it
     */
    default int drain(FluidType fluid, int amount) {
        for (int index = 0; index < tankCount(); index++) {
            FluidStorage tank = tank(index);
            if (tank.fluid() == fluid && tank.amount() > 0) {
                return tank.drain(amount, false);
            }
        }
        return 0;
    }
}
