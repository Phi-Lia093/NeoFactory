package com.philia093.neofactory.machine;

/**
 * The compressor of the age of steel, the same machine driven harder.
 * <p>
 * It reads the very recipes of the compressor of bronze and presses an item together in half the time, at
 * twice the steam a tick and the same steam on the craft, see {@link MachinePressure}.
 */
public final class HighPressureCompressorMachine extends CompressorMachine {

    /** Creates an empty compressor of steel. */
    public HighPressureCompressorMachine() {
        super(MachinePressure.HIGH);
    }
}
