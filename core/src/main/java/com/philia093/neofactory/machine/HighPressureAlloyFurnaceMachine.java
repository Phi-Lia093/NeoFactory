package com.philia093.neofactory.machine;

/**
 * The alloy furnace of the age of steel, the same machine driven harder.
 * <p>
 * It reads the very recipes of the alloy furnace of bronze and melts two metals together in half the time, at
 * twice the steam a tick and the same steam on the craft, see {@link MachinePressure}.
 */
public final class HighPressureAlloyFurnaceMachine extends AlloyFurnaceMachine {

    /** Creates an empty alloy furnace of steel. */
    public HighPressureAlloyFurnaceMachine() {
        super(MachinePressure.HIGH);
    }
}
