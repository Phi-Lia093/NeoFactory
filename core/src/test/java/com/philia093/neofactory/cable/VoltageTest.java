package com.philia093.neofactory.cable;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the ladder of the voltages: the order of the rungs, the numbers behind them and what a tier says
 * about the tiers below it.
 * <p>
 * The ladder is what a machine asks about a line - a line of a higher tier feeds a machine of a lower one -
 * so the order of the enum is the whole of the contract and the numbers are the table of the industry.
 */
class VoltageTest {

    @Test
    void everyTierIsFourTimesTheOneBelowIt() {
        assertEquals(8, Voltage.ULTRA_LOW.euPerTick(), "the first line of a workshop");
        assertEquals(32, Voltage.LOW.euPerTick(), "the line of the age of steam");
        assertEquals(128, Voltage.MEDIUM.euPerTick());
        assertEquals(512, Voltage.HIGH.euPerTick());
        assertEquals(2048, Voltage.EXTREME.euPerTick());
        assertEquals(8192, Voltage.INSANE.euPerTick());
        assertEquals(32768, Voltage.LUDICROUS.euPerTick());
        assertEquals(131072, Voltage.ZERO_POINT_MODULE.euPerTick());
        assertEquals(524288, Voltage.ULTIMATE.euPerTick());
        assertEquals(2097152, Voltage.ULTRA_HIGH.euPerTick());
        assertEquals(8388608, Voltage.ULTRA_EXCESSIVE.euPerTick());
        assertEquals(33554432, Voltage.ULTRA_IMMENSE.euPerTick());
        assertEquals(134217728, Voltage.ULTRA_MASSIVE.euPerTick(), "the top of the ladder");
        Voltage[] ladder = Voltage.values();
        for (int i = 1; i < ladder.length; i++) {
            assertEquals(ladder[i - 1].euPerTick() * 4, ladder[i].euPerTick(),
                    ladder[i] + " stands one step of four above " + ladder[i - 1]);
        }
    }

    @Test
    void aLineOfATierFeedsAMachineOfTheTiersBelowIt() {
        assertTrue(Voltage.LOW.isAtLeast(Voltage.ULTRA_LOW));
        assertTrue(Voltage.ULTRA_LOW.isAtLeast(Voltage.ULTRA_LOW), "a line feeds its own tier");
        assertFalse(Voltage.ULTRA_LOW.isAtLeast(Voltage.LOW),
                "a machine of low voltage never runs on a line of the ultra low one");
        assertTrue(Voltage.ULTRA_MASSIVE.isAtLeast(Voltage.ULTRA_LOW));
        for (Voltage tier : Voltage.values()) {
            for (Voltage other : Voltage.values()) {
                assertEquals(tier.ordinal() >= other.ordinal(), tier.isAtLeast(other),
                        tier + " against " + other);
            }
        }
    }

    @Test
    void everyTierCarriesANameOfItsOwn() {
        Set<String> names = new HashSet<>();
        Set<String> words = new HashSet<>();
        for (Voltage tier : Voltage.values()) {
            assertTrue(names.add(tier.fileName()), "two tiers share the name " + tier.fileName());
            assertTrue(words.add(tier.displayName()), "two tiers read as " + tier.displayName());
            assertEquals(tier.fileName(), tier.toString());
        }
        assertEquals("ulv", Voltage.ULTRA_LOW.fileName());
        assertEquals("luv", Voltage.LUDICROUS.fileName());
        assertEquals("zpm", Voltage.ZERO_POINT_MODULE.fileName());
        assertEquals("Ultra Low Voltage", Voltage.ULTRA_LOW.displayName());
        assertEquals("Zero Point Module", Voltage.ZERO_POINT_MODULE.displayName());
    }
}
