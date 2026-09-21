package com.philia093.neofactory.machine;

/**
 * A machine that burns fuel instead of drawing energy, asked by a screen for the time
 * that is left.
 * <p>
 * The furnace is the machine of the game that runs on a flame, and the screen of a
 * machine reports what is left of the current piece of fuel as a number - the game draws
 * no fire. Naming the question here keeps a screen from knowing which machine it looks
 * at, exactly like {@link ProgressMachine} does it for the work that is done.
 */
public interface FuelMachine {

    /**
     * Seconds the current piece of fuel still burns.
     *
     * @return the time in seconds, {@code 0} while nothing burns
     */
    float fuelSeconds();
}
