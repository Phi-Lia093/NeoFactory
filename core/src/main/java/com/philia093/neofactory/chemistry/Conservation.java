package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The one rule every reaction of the module has to keep: the atoms and the charge both have to come out.
 * <p>
 * A reaction moves atoms from one substance to another and never makes one out of nothing, so the count of
 * every element on the two sides has to match - up to the substances that stand around freely. The charge
 * has to come out too, and electrons count as charge: a reaction that takes two electrons in from a circuit
 * may leave the substances two charges heavier, and one that hands them out may leave them lighter, which
 * is the whole of what a current does, see {@link Reaction#electrons()}.
 * <p>
 * <b>The background is what may move whole and unnumbered.</b> In water, a reaction may spend a molecule of
 * the water or make one, and the water is in such excess that its own count is not what a balance is about;
 * the rule is therefore not that the elements match, but that whatever does not match is a whole number of
 * background molecules. Written out, the difference between the two sides has to be an integer combination
 * of the background substances and of nothing else - a difference of two hydrogens and one oxygen is a
 * molecule of water spent, and a difference of one hydrogen is a proton no water of the background can
 * explain, so a reaction that only names water is refused for it.
 * <p>
 * <b>A substance that is really counted is written into the reaction and not left to the background.</b>
 * The water of an electrolysis is poured in by a player and measured out, so it stands among the reactants
 * and is balanced like any other substance; leaving it to the background would let a reaction vanish a
 * bottle of it without saying so. The background is for the water a reaction of the industry is carried out
 * in - the medium - and not for the water it is made of.
 */
public final class Conservation {

    private Conservation() {
        // Utility class: never instantiated.
    }

    /**
     * What a reaction changes: the elements of the products less the elements of the reactants.
     *
     * @param reaction the reaction
     * @return the difference, which may count below zero
     */
    public static Composition difference(Reaction reaction) {
        return reaction.products().composition().minus(reaction.reactants().composition());
    }

    /**
     * What a reaction changes in charge: the charge of the products less the charge of the reactants.
     *
     * @param reaction the reaction
     * @return the difference
     */
    public static int chargeDifference(Reaction reaction) {
        return reaction.products().charge() - reaction.reactants().charge();
    }

    /**
     * {@code true} when the elements of a reaction come out, the background allowed for.
     *
     * @param reaction the reaction
     * @return {@code true} when the difference is a whole number of background substances
     */
    public static boolean elementsBalanced(Reaction reaction) {
        return inBackground(difference(reaction), reaction.background());
    }

    /**
     * {@code true} when the charge of a reaction comes out, the electrons counted.
     *
     * @param reaction the reaction
     * @return {@code true} when the charge the substances changed by is exactly the electrons moved
     */
    public static boolean chargeBalanced(Reaction reaction) {
        return chargeDifference(reaction) == -reaction.electrons();
    }

    /**
     * {@code true} when a reaction keeps the whole rule: the elements and the charge.
     *
     * @param reaction the reaction
     * @return {@code true} when the reaction may be run
     */
    public static boolean balanced(Reaction reaction) {
        return elementsBalanced(reaction) && chargeBalanced(reaction);
    }

    /**
     * Refuses a reaction that does not balance, naming what is wrong with it.
     * <p>
     * A solver builds candidates and a candidate that does not keep the rule is a wrong answer; failing
     * where it is found keeps a wrong reaction from travelling, which is why this is a hard check and not a
     * question a caller may forget to ask.
     *
     * @param reaction the reaction
     * @throws IllegalStateException when the reaction does not balance
     */
    public static void check(Reaction reaction) {
        if (!balanced(reaction)) {
            Composition difference = difference(reaction);
            throw new IllegalStateException("The reaction does not balance: the elements changed by "
                    + difference.formula() + " and the charge by " + chargeDifference(reaction)
                    + " while " + reaction.electrons() + " electrons moved");
        }
    }

    /**
     * {@code true} when a difference can be written as whole numbers of the background substances.
     * <p>
     * With no background at all the difference has to be nothing, which is a reaction in a closed vessel;
     * with one or a few background substances the difference has to be a whole number of them, which is
     * found by trying the whole numbers that could reach it, one substance at a time, and giving up as soon
     * as one substance can touch an element no substance below it can, see {@link #reachable}.
     *
     * @param difference what the reaction changed
     * @param background substances that may move whole and unnumbered
     * @return {@code true} when the difference is a whole number of them
     */
    private static boolean inBackground(Composition difference, List<Chemical> background) {
        if (difference.isEmpty()) {
            return true;
        }
        if (background.isEmpty()) {
            return false;
        }
        List<Composition> vectors = new ArrayList<>(background.size());
        for (Chemical chemical : background) {
            vectors.add(chemical.composition());
        }
        return reachable(difference, vectors, 0, bound(difference, vectors));
    }

    /** A whole number big enough for every background substance, and then a little. */
    private static int bound(Composition difference, List<Composition> vectors) {
        int largest = 0;
        for (int value : difference.byElement().values()) {
            largest = Math.max(largest, Math.abs(value));
        }
        for (Composition vector : vectors) {
            for (int value : vector.byElement().values()) {
                largest = Math.max(largest, Math.abs(value));
            }
        }
        return largest + 1;
    }

    /** Tries every whole number of one background substance and hands the rest to the ones behind it. */
    private static boolean reachable(Composition difference, List<Composition> vectors, int index,
            int bound) {
        if (index == vectors.size()) {
            return difference.isEmpty();
        }
        for (int coefficient = -bound; coefficient <= bound; coefficient++) {
            Composition rest = difference.minus(vectors.get(index).times(coefficient));
            if (reachableAtAll(rest, vectors, index + 1)
                    && reachable(rest, vectors, index + 1, bound)) {
                return true;
            }
        }
        return false;
    }

    /** Gives up early when an element of the rest is one no substance behind this one can touch. */
    private static boolean reachableAtAll(Composition rest, List<Composition> vectors, int from) {
        Set<String> covered = new HashSet<>();
        for (int index = from; index < vectors.size(); index++) {
            covered.addAll(vectors.get(index).elements());
        }
        for (String element : rest.elements()) {
            if (!covered.contains(element)) {
                return false;
            }
        }
        return true;
    }
}
