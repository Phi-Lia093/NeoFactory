package com.philia093.neofactory.chemistry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * How much of every element a pile holds, in millibuckets and in exact fractions.
 * <p>
 * This is the value a reaction of the industry balances in. Its {@link Composition} counts the atoms of one
 * molecule and is a whole number; this counts what a pot really holds, which is a measured amount of a
 * substance taken as many times as the pile holds it, so the same element may come out as three quarters
 * of a millibucket or as twenty five of them. The atoms of a pot are the same atoms before and behind a
 * reaction, whatever substance they were packed in, and this is where that is written down.
 * <p>
 * <b>An element that is not held is not written.</b> A pot that was emptied of its copper does not hold
 * nothing of copper and nothing of no element, so an amount that reached zero is dropped and an empty value
 * means the pot holds no atoms at all. Two values are the same substance of the same amount exactly when
 * every element they hold is held in the same amount, which is why the value is compared and not the pot.
 * <p>
 * <b>The amounts may be negative.</b> The change a reaction works is written as one of these values taken
 * away from another, and a pot that lost its copper changed by a negative amount of copper; one value
 * serves the pot and the change, exactly as {@link Composition} serves a molecule and a difference.
 */
public final class ElementAmounts {

    /** The value of no atoms at all, shared by the whole module. */
    private static final ElementAmounts EMPTY = new ElementAmounts(Map.of());

    /** Amount of every element, without the zeroes. */
    private final Map<String, Fraction> amounts;

    private ElementAmounts(Map<String, Fraction> amounts) {
        this.amounts = amounts;
    }

    /**
     * Creates a value from an amount per element.
     *
     * @param amounts amount of every element, zeroes are dropped
     * @return the value, empty when nothing is held
     * @throws IllegalArgumentException when an element symbol is unknown
     */
    public static ElementAmounts of(Map<String, Fraction> amounts) {
        Map<String, Fraction> kept = new LinkedHashMap<>();
        if (amounts != null) {
            amounts.forEach((element, amount) -> {
                if (!Elements.isKnown(element)) {
                    throw new IllegalArgumentException("Unknown element symbol: " + element);
                }
                if (amount != null && !amount.isZero()) {
                    kept.put(element, amount);
                }
            });
        }
        return kept.isEmpty() ? EMPTY : new ElementAmounts(Collections.unmodifiableMap(kept));
    }

    /**
     * Creates a value of one element.
     *
     * @param element symbol of the element
     * @param amount how much of it
     * @return the value
     */
    public static ElementAmounts of(String element, Fraction amount) {
        return of(Map.of(element, amount));
    }

    /**
     * The value a composition of a molecule is, one molecule of it holding one millibucket of atoms.
     * <p>
     * A molecule of a catalog substance is one millibucket of whatever it is made of, so its atoms are
     * exactly the counts its composition holds; this is what puts the molecule level and the measured level
     * of the module on one scale, see {@link Blend}.
     *
     * @param composition the composition of a molecule
     * @return the value of one molecule of it
     */
    public static ElementAmounts of(Composition composition) {
        Map<String, Fraction> amounts = new LinkedHashMap<>();
        composition.byElement().forEach((element, count) -> amounts.put(element, Fraction.of(count)));
        return of(amounts);
    }

    /** The value of no atoms at all. */
    public static ElementAmounts empty() {
        return EMPTY;
    }

    /** Amount of every element, without the zeroes. */
    public Map<String, Fraction> byElement() {
        return amounts;
    }

    /**
     * How much of an element this value holds.
     *
     * @param element symbol of the element
     * @return the amount, nothing when the element is not held
     */
    public Fraction amountOf(String element) {
        return amounts.getOrDefault(element, Fraction.ZERO);
    }

    /** The elements this value holds, in the order they were added. */
    public Set<String> elements() {
        return amounts.keySet();
    }

    /** {@code true} when this value holds no atoms at all. */
    public boolean isEmpty() {
        return amounts.isEmpty();
    }

    /**
     * {@code true} when every element is held in a positive amount.
     *
     * @return {@code true} when nothing is held below zero
     */
    public boolean isPositive() {
        for (Fraction amount : amounts.values()) {
            if (amount.isNegative()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Adds the amounts of another value to this one.
     *
     * @param other value to add
     * @return the sum, a new value
     */
    public ElementAmounts plus(ElementAmounts other) {
        Map<String, Fraction> merged = new LinkedHashMap<>(amounts);
        other.amounts.forEach((element, amount) -> merged.merge(element, amount, Fraction::plus));
        merged.values().removeIf(Fraction::isZero);
        return of(merged);
    }

    /**
     * Takes the amounts of another value away from this one.
     *
     * @param other value to subtract
     * @return the difference, a new value
     */
    public ElementAmounts minus(ElementAmounts other) {
        Map<String, Fraction> merged = new LinkedHashMap<>(amounts);
        other.amounts.forEach((element, amount) ->
                merged.merge(element, amount.negated(), Fraction::plus));
        merged.values().removeIf(Fraction::isZero);
        return of(merged);
    }

    /**
     * Takes every amount a number of times.
     *
     * @param factor amount to multiply with
     * @return the scaled value, a new value
     */
    public ElementAmounts times(Fraction factor) {
        if (factor.isZero() || amounts.isEmpty()) {
            return EMPTY;
        }
        Map<String, Fraction> scaled = new LinkedHashMap<>();
        amounts.forEach((element, amount) -> scaled.put(element, amount.times(factor)));
        return of(scaled);
    }

    /**
     * Takes every amount a whole number of times.
     *
     * @param factor whole number to multiply with
     * @return the scaled value, a new value
     */
    public ElementAmounts times(long factor) {
        return times(Fraction.of(factor));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ElementAmounts)) {
            return false;
        }
        return amounts.equals(((ElementAmounts) o).amounts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amounts);
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder("ElementAmounts(");
        boolean first = true;
        for (Map.Entry<String, Fraction> entry : amounts.entrySet()) {
            if (!first) {
                text.append(", ");
            }
            text.append(entry.getKey()).append(' ').append(entry.getValue()).append("mb");
            first = false;
        }
        return text.append(')').toString();
    }
}
