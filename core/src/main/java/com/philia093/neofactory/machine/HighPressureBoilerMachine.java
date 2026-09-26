package com.philia093.neofactory.machine;

/**
 * The boiler of the age of steel, the same boiler at another pressure.
 * <p>
 * It makes three hundred millibuckets of steam a second - two and a half times what the boiler of bronze makes
 * - and it is what feeds the machines of pressure, which drink twice the steam of a bronze machine every tick.
 * A piece of fuel burns twice as long here as in a furnace, which is less than the boiler of bronze gets, so
 * the boiler of pressure is what a player builds when a single boiler of bronze can no longer keep up.
 */
public final class HighPressureBoilerMachine extends SteamBoilerMachine {

    /** Creates the boiler of the age of steel. */
    public HighPressureBoilerMachine() {
        super(MachinePressure.HIGH, STEEL_STEAM_PER_SECOND, STEEL_FUEL_SHARE);
    }
}
