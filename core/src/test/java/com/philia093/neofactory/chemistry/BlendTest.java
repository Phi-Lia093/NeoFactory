package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the measured pile a piece of the industry is.
 * <p>
 * The two piles the tests are built around are the ones the module exists for: an ingot of iron, which is
 * one substance of a hundred millibuckets, and an ingot of bronze, which is copper and tin in one pile and
 * never a compound. Both are checked by the atoms they hold and not by their names, because the atoms are
 * what a reaction balances: an ingot of bronze has to come out as seventy five millibuckets of copper and
 * twenty five of tin, and a nugget of it as half of that again - seven and a half and two and a half, with
 * nothing lost to a rounding.
 */
class BlendTest {

    private static final Chemical IRON = Chemical.parse("[Fe]");
    private static final Chemical COPPER = Chemical.parse("[Cu]");
    private static final Chemical TIN = Chemical.parse("[Sn]");
    private static final Chemical SODIUM = Chemical.parse("[Na+]");

    @Test
    void anIngotOfIronIsAHundredMillibucketsOfIron() {
        Blend ingot = Blend.of(IRON, Amounts.INGOT);

        assertEquals(Fraction.of(100), ingot.total());
        assertTrue(ingot.isPure());
        assertEquals(Map.of("Fe", Fraction.of(100)), ingot.elementAmounts());
        assertEquals(Fraction.ZERO, ingot.charge());
    }

    @Test
    void anIngotOfBronzeIsCopperAndTinInOnePile() {
        Blend bronze = Blend.of(COPPER, Fraction.of(75)).plus(Blend.of(TIN, Fraction.of(25)));

        assertFalse(bronze.isPure(), "an alloy is a pile of substances and not one compound");
        assertEquals(Fraction.of(100), bronze.total());
        assertEquals(Map.of("Cu", Fraction.of(75), "Sn", Fraction.of(25)), bronze.elementAmounts());
    }

    @Test
    void aNuggetOfAnAlloySplitsExactly() {
        // A nugget is a tenth of an ingot, and a tenth of three quarters is not a decimal.
        Blend nugget = Blend.of(COPPER, Fraction.of(15, 2)).plus(Blend.of(TIN, Fraction.of(5, 2)));

        assertEquals(Fraction.of(10), nugget.total());
        assertEquals(Map.of("Cu", Fraction.of(15, 2), "Sn", Fraction.of(5, 2)),
                nugget.elementAmounts());
    }

    @Test
    void aBlockIsAHundredNuggetsAndHoldsTheSameAtoms() {
        Blend block = Blend.of(IRON, Amounts.BLOCK);
        Blend nuggets = Blend.empty();
        for (int count = 0; count < 100; count++) {
            nuggets = nuggets.plus(Blend.of(IRON, Amounts.NUGGET));
        }

        assertEquals(block.elementAmounts(), nuggets.elementAmounts(),
                "a thousand millibuckets either way, and not a hair lost");
        assertEquals(block.total(), nuggets.total());
    }

    @Test
    void anIonCarriesItsCharge() {
        Blend sodium = Blend.of(SODIUM, 100);

        assertEquals(Fraction.of(100), sodium.elementAmounts().get("Na"));
        assertEquals(Fraction.of(100), sodium.charge());
    }

    @Test
    void aPileIsAddedTakenAndScaled() {
        Blend bronze = Blend.of(COPPER, 75).plus(Blend.of(TIN, 25));

        assertEquals(Blend.of(COPPER, 150).plus(Blend.of(TIN, 50)), bronze.times(2));
        assertEquals(Fraction.of(200), bronze.times(2).total());
        assertTrue(bronze.minus(bronze).isEmpty());
        assertEquals(bronze, bronze.times(1));
    }

    @Test
    void aPileThatHoldsNothingOfASubstanceDoesNotHoldIt() {
        assertTrue(Blend.of(IRON, Fraction.ZERO).isEmpty());
        assertTrue(Blend.empty().amountOf(IRON).isZero(), "and an empty pile holds none of anything");
        assertEquals(Fraction.ZERO, Blend.empty().total());
    }
}
