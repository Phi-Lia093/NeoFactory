package com.philia093.neofactory.chemistry;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * How much of every element a species is made of, the vector every balance is written in.
 * <p>
 * A molecule, a reactant, a whole side of a reaction: all of them are compared by counting their elements,
 * so the count belongs in a value of its own and not inside a formula string that has to be read again
 * every time a solver looks at it. Two species are the same substance of the same amount (of the same
 * element skeleton at least) exactly when this value says so, which is what lets the balance of a reaction
 * be a subtraction of two of them.
 * <p>
 * <b>The counts are whole numbers and may be negative.</b> A species has a count of zero or more of every
 * element, but the difference of the two sides of a reaction - the change a reaction works - is written the
 * same way and does go negative, so one value serves both and a balance is one subtraction rather than a
 * second kind of table. A count of zero is dropped, so an empty value means "nothing of no element" and a
 * reaction that changed nothing is an empty value.
 * <p>
 * <b>The formula is written in Hill order</b> - carbon first, hydrogen second, the rest alphabetically -
 * which is the one order that makes two species of the same makeup print the same string, and the order a
 * player reads in a tooltip.
 */
public final class Composition {

    /** The value of nothing of no element, shared by the whole module. */
    private static final Composition EMPTY = new Composition(Map.of());

    /** Count of every element, alphabetically, without the zeroes. */
    private final Map<String, Integer> counts;

    private Composition(Map<String, Integer> counts) {
        this.counts = counts;
    }

    /**
     * Creates a composition from a count per element.
     *
     * @param counts count of every element, zeroes are dropped
     * @return the composition, empty when nothing is counted
     * @throws IllegalArgumentException when an element symbol is unknown
     */
    public static Composition of(Map<String, Integer> counts) {
        Map<String, Integer> kept = new TreeMap<>();
        if (counts != null) {
            counts.forEach((element, count) -> {
                if (!Elements.isKnown(element)) {
                    throw new IllegalArgumentException("Unknown element symbol: " + element);
                }
                if (count != null && count != 0) {
                    kept.put(element, count);
                }
            });
        }
        return kept.isEmpty() ? EMPTY : new Composition(Collections.unmodifiableMap(kept));
    }

    /**
     * Creates a composition of one element.
     *
     * @param element symbol of the element
     * @param count how much of it, zero for none
     * @return the composition
     */
    public static Composition of(String element, int count) {
        return of(Map.of(element, count));
    }

    /**
     * Reads a formula such as {@code "Cu3Sn"} into the elements it names.
     * <p>
     * This is the formula a material of the game carries as a string, written the way a player reads it -
     * a symbol followed by how many of it, one after another - and not a molecule of SMILES. A formula that
     * names something this plain reading cannot follow - a repeat of a group, {@code "(C2H4)n"} - is
     * refused, and a caller that has no chemistry to name answers with the empty composition instead, which
     * is what a wood and a clay of the game do.
     *
     * @param formula the formula, empty for nothing
     * @return the elements the formula counts
     * @throws IllegalArgumentException when a symbol is unknown or the formula is not that plain
     */
    public static Composition parse(String formula) {
        java.util.Objects.requireNonNull(formula, "formula");
        Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        int index = 0;
        while (index < formula.length()) {
            char symbol = formula.charAt(index);
            if (!Character.isUpperCase(symbol)) {
                throw new IllegalArgumentException(
                        "A formula is written as symbols and counts: " + formula);
            }
            int from = index;
            index++;
            if (index < formula.length() && Character.isLowerCase(formula.charAt(index))) {
                index++;
            }
            String element = formula.substring(from, index);
            if (!Elements.isKnown(element)) {
                throw new IllegalArgumentException("Unknown element symbol: " + element);
            }
            int digitsFrom = index;
            while (index < formula.length() && Character.isDigit(formula.charAt(index))) {
                index++;
            }
            int count = index > digitsFrom
                    ? Integer.parseInt(formula.substring(digitsFrom, index)) : 1;
            counts.merge(element, count, Integer::sum);
        }
        return of(counts);
    }

    /** The composition of nothing of no element. */
    public static Composition empty() {
        return EMPTY;
    }

    /**
     * Adds the counts of another composition to this one.
     *
     * @param other composition to add
     * @return the sum, a new value
     */
    public Composition plus(Composition other) {
        Map<String, Integer> merged = new TreeMap<>(counts);
        other.counts.forEach((element, count) -> merged.merge(element, count, Integer::sum));
        merged.values().removeIf(count -> count == 0);
        return of(merged);
    }

    /**
     * Takes the counts of another composition away from this one.
     * <p>
     * This is the subtraction one side of a reaction is written against the other, so the answer may hold
     * negative counts; the caller decides what a negative number of an element means, this value only
     * reports it.
     *
     * @param other composition to subtract
     * @return the difference, a new value
     */
    public Composition minus(Composition other) {
        Map<String, Integer> merged = new TreeMap<>(counts);
        other.counts.forEach((element, count) -> merged.merge(element, -count, Integer::sum));
        merged.values().removeIf(count -> count == 0);
        return of(merged);
    }

    /**
     * Scales every count, the way a formula is multiplied to balance a reaction.
     *
     * @param factor whole number to multiply with
     * @return the scaled value, a new value
     */
    public Composition times(int factor) {
        if (factor == 0 || counts.isEmpty()) {
            return EMPTY;
        }
        Map<String, Integer> scaled = new TreeMap<>();
        counts.forEach((element, count) -> scaled.put(element, count * factor));
        return of(scaled);
    }

    /** {@code true} when this composition counts nothing of no element. */
    public boolean isEmpty() {
        return counts.isEmpty();
    }

    /**
     * {@code true} when every count is greater than zero.
     * <p>
     * The composition of a real species passes this; a difference written the other way round does not,
     * which is the question a caller asks before it prints a formula.
     *
     * @return {@code true} when nothing is counted less than once
     */
    public boolean isPositive() {
        for (int count : counts.values()) {
            if (count < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * How much of an element this composition counts.
     *
     * @param element symbol of the element
     * @return the count, {@code 0} when the element is not in it
     */
    public int amountOf(String element) {
        return counts.getOrDefault(element, 0);
    }

    /** The elements this composition counts, alphabetically. */
    public Set<String> elements() {
        return counts.keySet();
    }

    /** The count of every element, alphabetically, without the zeroes. */
    public Map<String, Integer> byElement() {
        return counts;
    }

    /**
     * The formula of this composition, in Hill order.
     * <p>
     * Carbon first and hydrogen second when there is carbon, the rest alphabetically; every element
     * alphabetically when there is none, which is the one order that writes two compositions of the same
     * makeup as one string and the order a player reads in a tooltip.
     *
     * @return a string such as {@code "C2H6O"}, empty for a composition of nothing
     */
    public String formula() {
        if (counts.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        boolean hasCarbon = counts.containsKey("C");
        if (hasCarbon) {
            appendElement(text, "C");
            appendElement(text, "H");
        }
        for (String element : counts.keySet()) {
            if (hasCarbon && (element.equals("C") || element.equals("H"))) {
                continue;
            }
            appendElement(text, element);
        }
        return text.toString();
    }

    /** Writes one element and its count, which is left out for a single atom. */
    private void appendElement(StringBuilder text, String element) {
        Integer count = counts.get(element);
        if (count == null) {
            return;
        }
        text.append(element);
        if (count != 1) {
            text.append(count);
        }
    }

    @Override
    public String toString() {
        return "Composition(" + formula() + (isPositive() ? "" : ", signed " + counts) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Composition)) {
            return false;
        }
        Composition other = (Composition) o;
        return counts.equals(other.counts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(counts);
    }
}
