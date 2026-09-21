package com.philia093.neofactory.machine;

/**
 * A machine that works on something over time and can show how far it is.
 * <p>
 * The interface is what a screen asks for: a machine without it has nothing to draw
 * while it runs. A furnace reports two values, the work it does towards its result and
 * the fuel that is burning right now.
 */
public interface ProgressMachine {

    /**
     * Share of the work that is done.
     *
     * @return a value between {@code 0} and {@code 1}
     */
    float craftProgress();

    /**
     * Share of the fuel that is left to burn.
     *
     * @return a value between {@code 0} and {@code 1}, {@code 0} while nothing burns
     */
    float burnProgress();
}
