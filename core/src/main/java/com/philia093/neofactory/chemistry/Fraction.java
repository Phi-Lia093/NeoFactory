package com.philia093.neofactory.chemistry;

import java.util.Objects;

/**
 * An amount of something, written as a whole number over a whole number and never as a decimal.
 * <p>
 * The amounts of the industry are counts of what a workshop moves around - a block, an ingot, a nugget, a
 * cell - and those counts do not divide evenly. A bronze ingot is three quarters copper and one quarter
 * tin, and a nugget is a tenth of that again, so the amount of tin in a nugget is a hundredth of a tenth:
 * a decimal cannot write that down without losing something, and a lost something grows every time a pot
 * is filled and emptied. A fraction is exact: three quarters is the pair three and four and not the number
 * the machine happens to round it to, and adding two of them back gives the whole one again, to the last
 * bit.
 * <p>
 * <b>The pair is reduced and the sign sits on top.</b> Two fourths and one half are one value and not two,
 * which is what lets two amounts that came by different ways be compared and found equal; and a negative
 * denominator is moved up so that the bottom is always positive.
 * <p>
 * <b>An amount that overflows whole numbers is refused rather than rounded.</b> A balance of a workshop
 * never reaches the limits of a whole number, and a value that came out of the machine's arithmetic cannot
 * be trusted anyway, so a multiplication or an addition that would not fit throws where it happens and no
 * silent wrap travels on into a reaction.
 */
public final class Fraction implements Comparable<Fraction> {

    /** The amount of nothing. */
    public static final Fraction ZERO = new Fraction(0, 1);

    /** The amount of one. */
    public static final Fraction ONE = new Fraction(1, 1);

    private final long numerator;
    private final long denominator;

    private Fraction(long numerator, long denominator) {
        if (denominator == 0) {
            throw new ArithmeticException("A fraction with nothing under it is no amount");
        }
        long top = numerator;
        long bottom = denominator;
        if (bottom < 0) {
            top = -top;
            bottom = -bottom;
        }
        long divisor = greatestCommonDivisor(Math.abs(top), bottom);
        this.numerator = top / divisor;
        this.denominator = bottom / divisor;
    }

    /**
     * A whole amount.
     *
     * @param value the whole number
     * @return the amount
     */
    public static Fraction of(long value) {
        if (value == 0) {
            return ZERO;
        }
        if (value == 1) {
            return ONE;
        }
        return new Fraction(value, 1);
    }

    /**
     * An amount of one whole number over another.
     *
     * @param numerator the count on top
     * @param denominator the count under it, never zero
     * @return the amount
     * @throws ArithmeticException when the denominator is zero
     */
    public static Fraction of(long numerator, long denominator) {
        return new Fraction(numerator, denominator);
    }

    /** The count on top of this amount. */
    public long numerator() {
        return numerator;
    }

    /** The count under it, always positive. */
    public long denominator() {
        return denominator;
    }

    /**
     * This amount added to another.
     *
     * @param other amount to add
     * @return the sum
     * @throws ArithmeticException when the result would not fit a whole number
     */
    public Fraction plus(Fraction other) {
        return new Fraction(Math.addExact(Math.multiplyExact(numerator, other.denominator),
                        Math.multiplyExact(other.numerator, denominator)),
                Math.multiplyExact(denominator, other.denominator));
    }

    /**
     * This amount less another.
     *
     * @param other amount to take away
     * @return the difference
     * @throws ArithmeticException when the result would not fit a whole number
     */
    public Fraction minus(Fraction other) {
        return new Fraction(Math.subtractExact(Math.multiplyExact(numerator, other.denominator),
                        Math.multiplyExact(other.numerator, denominator)),
                Math.multiplyExact(denominator, other.denominator));
    }

    /**
     * This amount taken a number of times.
     *
     * @param other amount to multiply with
     * @return the product
     * @throws ArithmeticException when the result would not fit a whole number
     */
    public Fraction times(Fraction other) {
        return new Fraction(Math.multiplyExact(numerator, other.numerator),
                Math.multiplyExact(denominator, other.denominator));
    }

    /**
     * This amount split into a number of parts.
     *
     * @param other amount to divide by, never nothing
     * @return the quotient
     * @throws ArithmeticException when the divisor is nothing or the result would not fit
     */
    public Fraction dividedBy(Fraction other) {
        if (other.isZero()) {
            throw new ArithmeticException("Splitting an amount into nothing is no amount");
        }
        return new Fraction(Math.multiplyExact(numerator, other.denominator),
                Math.multiplyExact(denominator, other.numerator));
    }

    /** This amount with the other sign. */
    public Fraction negated() {
        return numerator == 0 ? ZERO : new Fraction(-numerator, denominator);
    }

    /** {@code true} when this amount is nothing. */
    public boolean isZero() {
        return numerator == 0;
    }

    /** {@code true} when this amount is less than nothing. */
    public boolean isNegative() {
        return numerator < 0;
    }

    /**
     * The nearest decimal of this amount, for a screen and for a log only.
     * <p>
     * Arithmetic never goes through this: a decimal is a reading of an exact amount and not the amount
     * itself, and a balance that used it would drift a little every time a value was read.
     *
     * @return the decimal value
     */
    public double doubleValue() {
        return (double) numerator / denominator;
    }

    @Override
    public int compareTo(Fraction other) {
        return Long.compare(Math.multiplyExact(numerator, other.denominator),
                Math.multiplyExact(other.numerator, denominator));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Fraction)) {
            return false;
        }
        Fraction other = (Fraction) o;
        return numerator == other.numerator && denominator == other.denominator;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numerator, denominator);
    }

    @Override
    public String toString() {
        return denominator == 1 ? Long.toString(numerator) : numerator + "/" + denominator;
    }

    /** The largest whole number that divides both counts. */
    private static long greatestCommonDivisor(long first, long second) {
        long left = first;
        long right = second;
        while (right != 0) {
            long rest = left % right;
            left = right;
            right = rest;
        }
        return left == 0 ? 1 : left;
    }
}
