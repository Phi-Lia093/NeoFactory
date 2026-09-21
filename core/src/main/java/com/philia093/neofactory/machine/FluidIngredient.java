package com.philia093.neofactory.machine;

import java.util.Objects;

/**
 * An amount of one kind of fluid, the fluid half of a recipe.
 * <p>
 * The record is to a tank what {@link com.philia093.neofactory.recipe.Ingredient} is to
 * a slot: it says what a recipe needs and how much of it, and a whole recipe is
 * therefore a list of ingredients for the slots plus a list of these for the tanks.
 * The same record also describes what a recipe gives back, which is why it knows both
 * whether a tank holds enough and whether a tank could take more.
 */
public record FluidIngredient(FluidType fluid, int amount) {

    /** Checks the fields, so a broken recipe fails while it is read and not later. */
    public FluidIngredient {
        Objects.requireNonNull(fluid, "fluid");
        if (amount <= 0) {
            throw new IllegalArgumentException("A fluid ingredient needs a positive amount: " + amount);
        }
    }

    /**
     * Creates an amount of a fluid.
     *
     * @param fluid kind of the fluid
     * @param amount amount in units of the game
     * @return the ingredient
     */
    public static FluidIngredient of(FluidType fluid, int amount) {
        return new FluidIngredient(fluid, amount);
    }

    /**
     * {@code true} when a tank holds at least this much of the fluid.
     *
     * @param tank tank that is offered, may be {@code null}
     * @return {@code true} when a recipe may take the fluid out of it
     */
    public boolean matches(FluidStorage tank) {
        return tank != null && tank.fluid() == fluid && tank.amount() >= amount;
    }

    /**
     * {@code true} when this much of the fluid still fits into a tank.
     *
     * @param tank tank that is offered, may be {@code null}
     * @return {@code true} when the tank is empty or already holds this fluid and has room
     */
    public boolean fits(FluidStorage tank) {
        if (tank == null) {
            return false;
        }
        if (tank.fluid() != null && tank.fluid() != fluid) {
            return false;
        }
        return tank.capacity() - tank.amount() >= amount;
    }

    @Override
    public String toString() {
        return amount + " x " + fluid.name();
    }
}
