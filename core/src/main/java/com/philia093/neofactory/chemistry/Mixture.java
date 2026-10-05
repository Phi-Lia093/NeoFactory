package com.philia093.neofactory.chemistry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A pile of substances: how many of each one, side by side and not bonded to one another.
 * <p>
 * A mixture is the honest answer to what a bar of bronze is. Its copper and its tin are not joined - if
 * they were, bronze would be a compound with a formula and would dissolve into one thing - they simply sit
 * next to each other, and a reaction that touches the bronze touches the copper and the tin on their own.
 * Writing the pile as copper three and tin one, and never as {@code Cu3Sn}, is what keeps a balance from
 * treating an alloy as a molecule and an electrolyzer from looking for a bond to break in it.
 * <p>
 * The pile is also what a side of a reaction is: the reactants are a mixture and the products are another,
 * and the balance of the reaction is a subtraction of the two, see {@link Conservation}. The whole of what
 * a mixture answers is therefore what the two questions a balance asks need - the elements it is made of
 * and the charge it carries - both of them the sum over the substances it holds, each taken as many times
 * as the pile holds it.
 * <p>
 * The substances of a mixture are the keys of a map and the amounts its values, so a mixture is equal to
 * another one exactly when it holds the same substances in the same amounts, whatever order they were
 * listed in: writing copper then tin and tin then copper name one pile and not two.
 */
public final class Mixture {

    /** The pile that holds nothing, shared by the whole module. */
    private static final Mixture EMPTY = new Mixture(Map.of());

    /** Amount of every substance, without the zeroes. */
    private final Map<Chemical, Integer> components;

    private Mixture(Map<Chemical, Integer> components) {
        this.components = components;
    }

    /**
     * Creates a pile from an amount per substance.
     *
     * @param components amount of every substance, zeroes are dropped
     * @return the mixture, empty when nothing is counted
     */
    public static Mixture of(Map<Chemical, Integer> components) {
        Map<Chemical, Integer> kept = new LinkedHashMap<>();
        if (components != null) {
            components.forEach((chemical, amount) -> {
                Objects.requireNonNull(chemical, "chemical");
                if (amount != null && amount != 0) {
                    kept.put(chemical, amount);
                }
            });
        }
        return kept.isEmpty() ? EMPTY : new Mixture(Collections.unmodifiableMap(kept));
    }

    /**
     * Creates a pile that holds one substance.
     *
     * @param chemical the substance
     * @param amount how much of it, zero for none
     * @return the mixture
     */
    public static Mixture of(Chemical chemical, int amount) {
        return of(Map.of(Objects.requireNonNull(chemical, "chemical"), amount));
    }

    /** The pile that holds nothing. */
    public static Mixture empty() {
        return EMPTY;
    }

    /** Amount of every substance in this pile, without the zeroes. */
    public Map<Chemical, Integer> components() {
        return components;
    }

    /**
     * How much of a substance this pile holds.
     *
     * @param chemical the substance
     * @return the amount, {@code 0} when the pile holds none of it
     */
    public int amountOf(Chemical chemical) {
        return components.getOrDefault(chemical, 0);
    }

    /** {@code true} when this pile holds nothing. */
    public boolean isEmpty() {
        return components.isEmpty();
    }

    /** How many different substances this pile holds. */
    public int size() {
        return components.size();
    }

    /**
     * The elements of this pile, the substances summed with their amounts.
     *
     * @return the composition of the whole pile
     */
    public Composition composition() {
        Composition total = Composition.empty();
        for (Map.Entry<Chemical, Integer> entry : components.entrySet()) {
            total = total.plus(entry.getKey().composition().times(entry.getValue()));
        }
        return total;
    }

    /**
     * The charge of this pile, the substances summed with their amounts.
     *
     * @return the formal charge of the whole pile
     */
    public int charge() {
        int total = 0;
        for (Map.Entry<Chemical, Integer> entry : components.entrySet()) {
            total += entry.getKey().charge() * entry.getValue();
        }
        return total;
    }

    /**
     * Adds the amounts of another pile to this one.
     *
     * @param other pile to add
     * @return the sum, a new value
     */
    public Mixture plus(Mixture other) {
        Map<Chemical, Integer> merged = new LinkedHashMap<>(components);
        other.components.forEach((chemical, amount) -> merged.merge(chemical, amount, Integer::sum));
        merged.values().removeIf(amount -> amount == 0);
        return of(merged);
    }

    /**
     * Takes the amounts of another pile away from this one.
     *
     * @param other pile to subtract
     * @return the difference, a new value
     */
    public Mixture minus(Mixture other) {
        Map<Chemical, Integer> merged = new LinkedHashMap<>(components);
        other.components.forEach((chemical, amount) -> merged.merge(chemical, -amount, Integer::sum));
        merged.values().removeIf(amount -> amount == 0);
        return of(merged);
    }

    /**
     * Scales the whole pile, the way a reaction is multiplied to balance.
     *
     * @param factor whole number to multiply with
     * @return the scaled pile, a new value
     */
    public Mixture times(int factor) {
        if (factor == 0 || components.isEmpty()) {
            return EMPTY;
        }
        Map<Chemical, Integer> scaled = new LinkedHashMap<>();
        components.forEach((chemical, amount) -> scaled.put(chemical, amount * factor));
        return of(scaled);
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder("Mixture(");
        boolean first = true;
        for (Map.Entry<Chemical, Integer> entry : components.entrySet()) {
            if (!first) {
                text.append(", ");
            }
            text.append(entry.getValue()).append(" x ").append(entry.getKey().composition().formula());
            first = false;
        }
        return text.append(')').toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Mixture)) {
            return false;
        }
        return components.equals(((Mixture) o).components);
    }

    @Override
    public int hashCode() {
        return components.hashCode();
    }
}
