package com.philia093.neofactory.chemistry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A measured pile of substances: how much of each one, in millibuckets and in exact fractions.
 * <p>
 * A {@link Mixture} counts whole molecules, which is what a balance of a pot needs; a blend measures what
 * a workshop really moves around, and that is a matter of amounts. An ingot is a hundred millibuckets of
 * its metal, a block is a thousand, a nugget is ten, and a cell is a thousand of whatever is in it, so a
 * substance is never a count of pieces but a weight of millibuckets, see {@link Amounts}. This is the pile
 * a machine holds and the pile a reaction spends, and it is why the double of an ingot is one block and not
 * two of anything.
 * <p>
 * <b>A blend may be a pure substance or a mixture, and the same pile says both.</b> An ingot of iron is one
 * substance of a hundred millibuckets; an ingot of bronze is copper and tin in the same pile, three parts
 * to one, because an alloy is a mixture and never a compound, see {@link Mixture}. Writing both the same
 * way is what lets a salt - a lattice of two ions - and an alloy - two metals side by side - be carried by
 * one kind of item and handed to one kind of machine, see {@link ChemicalHolder}.
 * <p>
 * <b>The elements of a blend are what a balance is written in.</b> {@link #elementAmounts()} adds up the
 * atoms of every substance of the pile, in millibuckets, and those amounts are the conserved quantities of
 * a reaction: a pot may hand a substance on, split it or gather it, and the atoms it holds have to be the
 * same atoms before and behind, whatever way they were packed.
 */
public final class Blend {

    /** The pile that holds nothing. */
    private static final Blend EMPTY = new Blend(Map.of());

    /** Amount of every substance, without the empty ones. */
    private final Map<Chemical, Fraction> amounts;

    private Blend(Map<Chemical, Fraction> amounts) {
        this.amounts = amounts;
    }

    /**
     * Creates a pile from an amount per substance.
     *
     * @param amounts amount of every substance, empty amounts are dropped
     * @return the blend, empty when nothing is counted
     */
    public static Blend of(Map<Chemical, Fraction> amounts) {
        Map<Chemical, Fraction> kept = new LinkedHashMap<>();
        if (amounts != null) {
            amounts.forEach((chemical, amount) -> {
                Objects.requireNonNull(chemical, "chemical");
                Objects.requireNonNull(amount, "amount");
                if (!amount.isZero()) {
                    kept.put(chemical, amount);
                }
            });
        }
        return kept.isEmpty() ? EMPTY : new Blend(Collections.unmodifiableMap(kept));
    }

    /**
     * Creates a pile of one substance.
     *
     * @param chemical the substance
     * @param amount how much of it
     * @return the blend
     */
    public static Blend of(Chemical chemical, Fraction amount) {
        return of(Map.of(Objects.requireNonNull(chemical, "chemical"),
                Objects.requireNonNull(amount, "amount")));
    }

    /**
     * Creates a pile of a whole number of millibuckets of one substance.
     *
     * @param chemical the substance
     * @param millibuckets how much of it
     * @return the blend
     */
    public static Blend of(Chemical chemical, long millibuckets) {
        return of(chemical, Fraction.of(millibuckets));
    }

    /** The pile that holds nothing. */
    public static Blend empty() {
        return EMPTY;
    }

    /** Amount of every substance of this pile, without the empty ones. */
    public Map<Chemical, Fraction> components() {
        return amounts;
    }

    /**
     * How much of a substance this pile holds.
     *
     * @param chemical the substance
     * @return the amount in millibuckets, nothing when the pile holds none of it
     */
    public Fraction amountOf(Chemical chemical) {
        return amounts.getOrDefault(chemical, Fraction.ZERO);
    }

    /** How much of everything this pile holds, the amounts added up. */
    public Fraction total() {
        Fraction total = Fraction.ZERO;
        for (Fraction amount : amounts.values()) {
            total = total.plus(amount);
        }
        return total;
    }

    /** {@code true} when this pile holds nothing. */
    public boolean isEmpty() {
        return amounts.isEmpty();
    }

    /** {@code true} when this pile is one substance and nothing else. */
    public boolean isPure() {
        return amounts.size() == 1;
    }

    /**
     * Adds the amounts of another pile to this one.
     *
     * @param other pile to add
     * @return the sum, a new value
     */
    public Blend plus(Blend other) {
        Map<Chemical, Fraction> merged = new LinkedHashMap<>(amounts);
        other.amounts.forEach((chemical, amount) -> merged.merge(chemical, amount, Fraction::plus));
        merged.values().removeIf(Fraction::isZero);
        return of(merged);
    }

    /**
     * Takes the amounts of another pile away from this one.
     *
     * @param other pile to subtract
     * @return the difference, a new value
     */
    public Blend minus(Blend other) {
        Map<Chemical, Fraction> merged = new LinkedHashMap<>(amounts);
        other.amounts.forEach((chemical, amount) ->
                merged.merge(chemical, amount.negated(), Fraction::plus));
        merged.values().removeIf(Fraction::isZero);
        return of(merged);
    }

    /**
     * Takes this pile a number of times.
     *
     * @param factor amount to multiply with
     * @return the scaled pile, a new value
     */
    public Blend times(Fraction factor) {
        if (factor.isZero() || amounts.isEmpty()) {
            return EMPTY;
        }
        Map<Chemical, Fraction> scaled = new LinkedHashMap<>();
        amounts.forEach((chemical, amount) -> scaled.put(chemical, amount.times(factor)));
        return of(scaled);
    }

    /**
     * Takes this pile a whole number of times.
     *
     * @param factor whole number to multiply with
     * @return the scaled pile, a new value
     */
    public Blend times(long factor) {
        return times(Fraction.of(factor));
    }

    /**
     * The atoms of this pile, each element added up over every substance of it.
     * <p>
     * This is the value a balance is written in: the element of a substance is how many of it one molecule
     * holds, taken as many times as the pile holds the substance, so iron of an ingot comes out as a
     * hundred millibuckets of iron and bronze of an ingot as seventy five of copper and twenty five of tin.
     *
     * @return the amount of every element, empty amounts dropped
     */
    public Map<String, Fraction> elementAmounts() {
        Map<String, Fraction> elements = new LinkedHashMap<>();
        for (Map.Entry<Chemical, Fraction> entry : amounts.entrySet()) {
            for (Map.Entry<String, Integer> element : entry.getKey().composition().byElement()
                    .entrySet()) {
                Fraction amount = entry.getValue().times(Fraction.of(element.getValue()));
                elements.merge(element.getKey(), amount, Fraction::plus);
            }
        }
        elements.values().removeIf(Fraction::isZero);
        return elements;
    }

    /**
     * The charge of this pile, the substances added up with their amounts.
     *
     * @return the formal charge
     */
    public Fraction charge() {
        Fraction total = Fraction.ZERO;
        for (Map.Entry<Chemical, Fraction> entry : amounts.entrySet()) {
            total = total.plus(entry.getValue().times(Fraction.of(entry.getKey().charge())));
        }
        return total;
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder("Blend(");
        boolean first = true;
        for (Map.Entry<Chemical, Fraction> entry : amounts.entrySet()) {
            if (!first) {
                text.append(", ");
            }
            text.append(entry.getValue()).append("mb of ")
                    .append(entry.getKey().composition().formula());
            first = false;
        }
        return text.append(')').toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Blend)) {
            return false;
        }
        return amounts.equals(((Blend) o).amounts);
    }

    @Override
    public int hashCode() {
        return amounts.hashCode();
    }
}
