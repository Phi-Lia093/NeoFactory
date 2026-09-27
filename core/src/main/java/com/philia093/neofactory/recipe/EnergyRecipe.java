package com.philia093.neofactory.recipe;

/**
 * A recipe that draws energy, the one a machine of the electric age runs.
 * <p>
 * The game is built in ages and the machines of the age of steam burn fuel and steam, see
 * {@link SteamRecipe} and {@link com.philia093.neofactory.machine.FuelMachine}: a recipe of that age knows
 * how long it takes and nothing about volts. A recipe that is run by energy says so by implementing this
 * interface, and only then does a screen report what it costs - <b>the amount of energy it takes, the amount
 * it takes every tick and the voltage it asks for</b> - which is what the screen of recipes prints in the rows
 * the inventory of a machine would stand in, see {@code RecipeReport}.
 * <p>
 * The total of a recipe is not a number of its file: it is what it draws while it runs, so it follows from
 * {@link #euPerTick()} and {@link #seconds()} unless a recipe says otherwise.
 */
public interface EnergyRecipe {

    /** Energy the recipe draws every tick, always positive. */
    int euPerTick();

    /** Voltage the recipe asks for, the tier of the machine that may run it. */
    int voltage();

    /** Seconds one craft takes, the same number {@link SmeltingRecipe#seconds()} answers. */
    float seconds();

    /**
     * Energy the whole craft takes.
     *
     * @return the total in EU, the amount drawn while the recipe runs
     */
    default int totalEu() {
        // A tick is a twentieth of a second, so the energy of a craft is what it draws a tick times the
        // ticks it runs, rounded the way a player reads a number.
        return Math.round(euPerTick() * seconds() * 20.0f);
    }
}
