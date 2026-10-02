package com.philia093.neofactory.energy;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.machine.EnergyStorage;

/**
 * A buffer of energy that knows the tier it was built for.
 * <p>
 * A machine of the industry is built for one tier of the power, the tier its recipes ask for, and a line
 * that carries <b>more</b> than that does not feed it - it destroys it. The question is asked with
 * {@link #accepted()} and the answer a line gives is {@link #accepts(Voltage)}: a machine of high voltage
 * runs on a line of the middle voltage as well, because a better machine takes a worse line, while a line of
 * the extreme voltage on a machine of the middle one leaves a hole in the workshop, see {@link EnergyNet}.
 */
public interface EnergyAcceptor extends EnergyStorage {

    /**
     * Highest tier this machine takes, the tier its recipes ask for.
     *
     * @return the tier
     */
    Voltage accepted();

    /**
     * {@code true} when a line of a tier feeds this machine without destroying it.
     * <p>
     * A machine takes its own tier and every one below it. Nothing above it is ever handed over: the line
     * and the machine are both lost instead, which is what makes a player check the tier of a line before it
     * is connected to anything.
     *
     * @param line tier of the line
     * @return {@code true} when the machine survives the line
     */
    default boolean accepts(Voltage line) {
        return accepted().isAtLeast(line);
    }
}
