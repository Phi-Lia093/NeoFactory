package com.philia093.neofactory.chemistry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The one place that decides whether a difference can be explained by whole pieces of a medium.
 * <p>
 * A reaction in water may spend a molecule of it or make one, and no balance knows which unless it can ask
 * whether what is left over is a whole number of water molecules. That question is asked twice in the
 * module - once of a change written in whole atoms, once of a change written in measured amounts - and it
 * is the same question both times, so it is answered here and nowhere else: a difference is explained when
 * it is a whole number of the substances of the medium added together, and not explained otherwise.
 * <p>
 * <b>The whole numbers are tried and given up on early.</b> One substance of the medium is taken a whole
 * number of times at a time and the rest is handed to the substances behind it, and a step is dropped the
 * moment the rest holds an element that no substance behind it can touch. Since a substance of the medium
 * is a molecule of a few atoms and a difference is never large, the walk is short; a difference that no
 * whole number can reach falls out of it without ever being named, which is what refuses a reaction that
 * quietly left a half of a water molecule behind.
 */
final class Lattice {

    private Lattice() {
        // Utility class: never instantiated.
    }

    /**
     * {@code true} when a difference is a whole number of the substances of a medium.
     *
     * @param difference what a reaction changed, in the amounts it is written in
     * @param background the substances of the medium, each one molecule or one piece of it
     * @return {@code true} when the difference is a whole number of them
     */
    static boolean inBackground(ElementAmounts difference, List<ElementAmounts> background) {
        if (difference.isEmpty()) {
            return true;
        }
        if (background.isEmpty()) {
            return false;
        }
        return reachable(difference, background, 0, bound(difference, background));
    }

    /**
     * A whole number big enough for every substance of the medium, and then a little.
     * <p>
     * A coefficient taken further than the largest amount of the difference cannot come back to it, because
     * every substance of the medium holds its atoms forward and not backward; the count is read off the
     * numerators alone, which is over a small common denominator and so never too small.
     */
    private static int bound(ElementAmounts difference, List<ElementAmounts> background) {
        long largest = 1;
        for (Fraction amount : difference.byElement().values()) {
            largest = Math.max(largest, Math.abs(amount.numerator()));
        }
        for (ElementAmounts vector : background) {
            for (Fraction amount : vector.byElement().values()) {
                largest = Math.max(largest, Math.abs(amount.numerator()));
            }
        }
        return (int) Math.min(Integer.MAX_VALUE - 1L, largest) + 1;
    }

    /** Tries every whole number of one substance and hands the rest to the ones behind it. */
    private static boolean reachable(ElementAmounts difference, List<ElementAmounts> background,
            int index, int bound) {
        if (index == background.size()) {
            return difference.isEmpty();
        }
        for (int coefficient = -bound; coefficient <= bound; coefficient++) {
            ElementAmounts rest = difference.minus(background.get(index).times(coefficient));
            if (reachableAtAll(rest, background, index + 1)
                    && reachable(rest, background, index + 1, bound)) {
                return true;
            }
        }
        return false;
    }

    /** Gives up early when an element of the rest is one no substance behind this one can touch. */
    private static boolean reachableAtAll(ElementAmounts rest, List<ElementAmounts> background,
            int from) {
        Set<String> covered = new HashSet<>();
        for (int index = from; index < background.size(); index++) {
            covered.addAll(background.get(index).elements());
        }
        for (String element : rest.elements()) {
            if (!covered.contains(element)) {
                return false;
            }
        }
        return true;
    }
}
