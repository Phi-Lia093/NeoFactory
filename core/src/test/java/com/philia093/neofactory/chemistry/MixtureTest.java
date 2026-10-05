package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the pile an alloy is, the answer to what bronze really is.
 * <p>
 * Bronze is three copper and one tin lying side by side and not a compound of a formula, so its elements
 * add up to {@code Cu3Sn} while it stays two substances and never one. The tests pin the two faces of that:
 * the elements add up as they should, and the pile ignores the order its pieces were named in.
 */
class MixtureTest {

    private static final Chemical COPPER = Chemical.parse("[Cu]");
    private static final Chemical TIN = Chemical.parse("[Sn]");

    @Test
    void anAlloyIsAPileAndNotACompound() {
        Mixture bronze = Mixture.of(COPPER, 3).plus(Mixture.of(TIN, 1));

        assertEquals("Cu3Sn", bronze.composition().formula());
        assertEquals(2, bronze.size(), "it is two substances and not one");
        assertEquals(3, bronze.amountOf(COPPER));
        assertEquals(1, bronze.amountOf(TIN));
        assertEquals(0, bronze.charge());
        assertFalse(bronze.isEmpty());
    }

    @Test
    void theOrderOfThePiecesDoesNotMatter() {
        assertEquals(Mixture.of(COPPER, 3).plus(Mixture.of(TIN, 1)),
                Mixture.of(TIN, 1).plus(Mixture.of(COPPER, 3)));
    }

    @Test
    void scalingAndSubtractingThePile() {
        Mixture bronze = Mixture.of(COPPER, 3).plus(Mixture.of(TIN, 1));

        assertEquals("Cu6Sn2", bronze.times(2).composition().formula());
        assertTrue(bronze.minus(bronze).isEmpty());
        assertTrue(bronze.plus(Mixture.empty()).equals(bronze));
    }

    @Test
    void aZeroAmountHoldsNothing() {
        assertTrue(Mixture.of(COPPER, 0).isEmpty());
        assertTrue(Mixture.empty().isEmpty());
    }
}
