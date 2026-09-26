package com.philia093.neofactory.machine;

/**
 * The extractor of the age of steel, the same machine driven harder.
 * <p>
 * It reads the very recipes of the extractor of bronze and squeezes an item out in half the time, at twice the
 * steam a tick and the same steam on the craft, see {@link MachinePressure}.
 */
public final class HighPressureExtractorMachine extends ExtractorMachine {

    /** Creates an empty extractor of steel. */
    public HighPressureExtractorMachine() {
        super(MachinePressure.HIGH);
    }
}
