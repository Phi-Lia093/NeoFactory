package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the exact amount the industry is measured in.
 * <p>
 * The whole point of a fraction is that it never rounds, so the tests are built around the numbers that a
 * decimal cannot hold: a third taken three times is one again, a tenth taken ten times is one again, and a
 * hundred millibuckets split three quarters and one quarter come back as seventy five and twenty five with
 * nothing lost. A value that rounded anywhere would answer a shade off one of these and fail.
 */
class FractionTest {

    @Test
    void anAmountIsReducedAndItsSignSitsOnTop() {
        assertEquals(Fraction.of(1, 2), Fraction.of(2, 4));
        assertEquals(Fraction.of(1, 2), Fraction.of(-1, -2));
        assertEquals(Fraction.of(-1, 2), Fraction.of(1, -2));
        assertEquals(1, Fraction.of(2, 4).numerator());
        assertEquals(2, Fraction.of(2, 4).denominator());
        assertTrue(Fraction.of(0, 5).isZero());
    }

    @Test
    void addingTakingAndScalingAreExact() {
        assertEquals(Fraction.of(1, 2), Fraction.of(1, 3).plus(Fraction.of(1, 6)));
        assertEquals(Fraction.of(1, 6), Fraction.of(1, 2).minus(Fraction.of(1, 3)));
        assertEquals(Fraction.of(1, 2), Fraction.of(2, 3).times(Fraction.of(3, 4)));
        assertEquals(Fraction.of(2), Fraction.of(1, 2).dividedBy(Fraction.of(1, 4)));
    }

    @Test
    void aThirdTakenThreeTimesIsOneAgain() {
        Fraction third = Fraction.of(1, 3);

        assertEquals(Fraction.ONE, third.plus(third).plus(third), "a third three times is the whole one");
        assertEquals(Fraction.ONE, third.times(Fraction.of(3)));
        assertEquals(Fraction.ONE, Fraction.of(1, 10).times(Fraction.of(10)),
                "a tenth of a nugget ten times is the nugget again");
    }

    @Test
    void aHundredMillibucketsSplitThreeToSevenAddsUp() {
        Fraction copper = Fraction.of(100).times(Fraction.of(3, 4));
        Fraction tin = Fraction.of(100).times(Fraction.of(1, 4));

        assertEquals(Fraction.of(75), copper, "copper of a bronze ingot");
        assertEquals(Fraction.of(25), tin, "tin of a bronze ingot");
        assertEquals(Fraction.of(100), copper.plus(tin), "and the two are the ingot again");
    }

    @Test
    void amountsAreComparedAndSignsRead() {
        assertTrue(Fraction.of(1, 3).compareTo(Fraction.of(1, 2)) < 0);
        assertTrue(Fraction.of(1, 2).compareTo(Fraction.of(1, 2)) == 0);
        assertTrue(Fraction.of(3, 2).isNegative() == false);
        assertTrue(Fraction.of(-1, 2).isNegative());
        assertEquals(Fraction.of(1, 2), Fraction.of(-1, 2).negated());
        assertTrue(Fraction.of(0).isZero());
        assertFalse(Fraction.of(1).isZero());
    }

    @Test
    void anAmountDescribesItself() {
        assertEquals("3/4", Fraction.of(3, 4).toString());
        assertEquals("100", Fraction.of(100).toString());
        assertEquals(0.75, Fraction.of(3, 4).doubleValue(), 1.0e-12);
    }

    @Test
    void anAmountUnderNothingIsRefused() {
        assertThrows(ArithmeticException.class, () -> Fraction.of(1, 0));
        assertThrows(ArithmeticException.class, () -> Fraction.ONE.dividedBy(Fraction.ZERO));
    }
}
