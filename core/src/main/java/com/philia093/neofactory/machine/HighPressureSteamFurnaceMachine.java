package com.philia093.neofactory.machine;

/**
 * The steam furnace of the age of steel, the same machine driven harder.
 * <p>
 * It reads the very recipes of the furnace of bronze and melts an ore in half the time, which makes it drink
 * twice the steam every tick and the same steam on the craft, see {@link MachinePressure}. It is built of
 * steel and fed by the boiler of pressure.
 */
public final class HighPressureSteamFurnaceMachine extends SteamFurnaceMachine {

    /** Creates an empty steam furnace of steel. */
    public HighPressureSteamFurnaceMachine() {
        super(MachinePressure.HIGH);
    }
}
