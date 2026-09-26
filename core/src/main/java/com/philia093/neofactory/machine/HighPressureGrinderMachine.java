package com.philia093.neofactory.machine;

/**
 * The grinder of the age of steel, the same machine driven harder.
 * <p>
 * It reads the very recipes of the grinder of bronze and finishes a craft in half the time, which makes it
 * drink twice the steam every tick and the same steam on the craft, see {@link MachinePressure}. It is built
 * of steel and fed by the boiler of pressure.
 */
public final class HighPressureGrinderMachine extends GrinderMachine {

    /** Creates an empty grinder of steel. */
    public HighPressureGrinderMachine() {
        super(MachinePressure.HIGH);
    }
}
