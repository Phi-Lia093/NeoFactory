package com.philia093.neofactory.machine;

/**
 * A machine that writes a line of its own in the corner of its screen.
 * <p>
 * Most machines have nothing to say and leave the upper right corner of their panel empty, and the furnace
 * reports what is left of its fuel, see {@link FuelMachine}. A machine that is read by a number of its own -
 * the temperature of a boiler - answers here instead, which keeps a screen from knowing which machine it
 * looks at.
 */
public interface StatusMachine {

    /**
     * Line the machine writes in the upper right corner of its panel.
     *
     * @return the text, empty when the machine has nothing to show
     */
    String statusText();
}
