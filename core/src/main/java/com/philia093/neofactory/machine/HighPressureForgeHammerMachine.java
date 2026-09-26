package com.philia093.neofactory.machine;

/**
 * The forge hammer of the age of steel, the same machine driven harder.
 * <p>
 * It reads the very recipes of the forge hammer of bronze and beats an ingot into shape in half the time, at
 * twice the steam a tick and the same steam on the craft, see {@link MachinePressure}.
 */
public final class HighPressureForgeHammerMachine extends ForgeHammerMachine {

    /** Creates an empty forge hammer of steel. */
    public HighPressureForgeHammerMachine() {
        super(MachinePressure.HIGH);
    }
}
