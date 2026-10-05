package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.List;

/**
 * The rule a reaction of the industry keeps: the atoms of the pot come out, written in millibuckets.
 * <p>
 * A reactor is fed ingots and cells and not molecules, so the amounts it spends are measured amounts and the
 * quantities that balance are the atoms of those amounts, see {@link Blend#elementAmounts()}. This is the
 * same rule as {@link Conservation} and it is kept in the same place - a difference that is nothing, or a
 * whole number of the substances of the medium, and a charge that comes to the electrons moved - but it is
 * asked of measured amounts, where a pot may give up three quarters of a millibucket of copper and the
 * answer has to be exact, see {@link Fraction}.
 * <p>
 * <b>A molecule and a piece of a substance are the same scale.</b> One molecule of a substance is one
 * millibucket of its atoms, so a substance of the medium written as a whole molecule and a pot written in
 * measured amounts are told to the same lattice in the same units, and the two levels of the module cannot
 * come to different answers about the same pot.
 */
public final class BlendConservation {

    private BlendConservation() {
        // Utility class: never instantiated.
    }

    /**
     * What a reaction changes: the atoms of the products less the atoms of the reactants.
     *
     * @param reactants what goes in
     * @param products what comes out
     * @return the difference, in millibuckets of every element
     */
    public static ElementAmounts difference(Blend reactants, Blend products) {
        return products.elementAmounts().minus(reactants.elementAmounts());
    }

    /**
     * What a reaction changes in charge.
     *
     * @param reactants what goes in
     * @param products what comes out
     * @return the difference in charge
     */
    public static Fraction chargeDifference(Blend reactants, Blend products) {
        return products.charge().minus(reactants.charge());
    }

    /**
     * {@code true} when the atoms of a reaction come out, the medium allowed for.
     *
     * @param reactants what goes in
     * @param products what comes out
     * @param background substances that may move whole and unnumbered
     * @return {@code true} when the difference is a whole number of them
     */
    public static boolean elementsBalanced(Blend reactants, Blend products,
            List<Chemical> background) {
        List<ElementAmounts> vectors = new ArrayList<>(background.size());
        for (Chemical chemical : background) {
            vectors.add(ElementAmounts.of(chemical.composition()));
        }
        return Lattice.inBackground(difference(reactants, products), vectors);
    }

    /**
     * {@code true} when the charge of a reaction comes out, the electrons counted.
     *
     * @param reactants what goes in
     * @param products what comes out
     * @param electrons electrons the reaction takes in, negative when it gives them out
     * @return {@code true} when the charge changed by exactly the electrons moved
     */
    public static boolean chargeBalanced(Blend reactants, Blend products, int electrons) {
        return chargeDifference(reactants, products).equals(Fraction.of(-electrons));
    }

    /**
     * {@code true} when a reaction keeps the whole rule: the atoms and the charge.
     *
     * @param reactants what goes in
     * @param products what comes out
     * @param electrons electrons the reaction takes in, negative when it gives them out
     * @param background substances that may move whole and unnumbered
     * @return {@code true} when the reaction may be run
     */
    public static boolean balanced(Blend reactants, Blend products, int electrons,
            List<Chemical> background) {
        return elementsBalanced(reactants, products, background)
                && chargeBalanced(reactants, products, electrons);
    }

    /**
     * Refuses a reaction that does not balance, naming what is wrong with it.
     *
     * @param reactants what goes in
     * @param products what comes out
     * @param electrons electrons the reaction takes in, negative when it gives them out
     * @param background substances that may move whole and unnumbered
     * @throws IllegalStateException when the reaction does not balance
     */
    public static void check(Blend reactants, Blend products, int electrons,
            List<Chemical> background) {
        if (!balanced(reactants, products, electrons, background)) {
            throw new IllegalStateException("The reaction does not balance: the atoms changed by "
                    + difference(reactants, products) + " and the charge by "
                    + chargeDifference(reactants, products) + " while " + electrons
                    + " electrons moved");
        }
    }
}
