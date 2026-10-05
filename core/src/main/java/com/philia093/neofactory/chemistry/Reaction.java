package com.philia093.neofactory.chemistry;

import java.util.List;
import java.util.Objects;

/**
 * A reaction: what goes in, what comes out, how many electrons move and what stands around freely.
 * <p>
 * A reaction is written as two piles - the reactants and the products - and the balance of it is the one
 * rule every reaction of the module has to keep, see {@link Conservation}. Nothing in a reaction says how
 * fast it runs or whether it happens at all: a reaction that balances is one that could be written down,
 * and which of the balanced writings really happens is a question of heat and of a catalyst that a later
 * stage answers by ordering the candidates, see {@code InorganicRecipeBook}.
 * <p>
 * <b>Electrons are counted as a number and not as a substance.</b> A current is not made of atoms, so the
 * electrons a reaction takes from a circuit or hands to one cannot sit in a pile beside the substances; a
 * positive number says the reaction takes that many electrons in - it is a reduction - and a negative one
 * says it gives them out, which is what makes an electrolysis write its two half reactions against one
 * shared count, see {@code InorganicRecipeBook}.
 * <p>
 * <b>The background is what stands around freely.</b> A reaction in water may spend a molecule of it or
 * make one, and a reaction in an acid may take a proton or give one back, without the water or the proton
 * being a substance a player measures out: those are the substances of the background, and the balance
 * lets them move whole and unnumbered, see {@link Conservation#elementsBalanced(Reaction)}. A substance
 * that is really consumed in the sense a player counts - the water of an electrolysis that is poured in -
 * is written into the reactants like any other and is not left to the background.
 */
public final class Reaction {

    private final Mixture reactants;
    private final Mixture products;
    private final int electrons;
    private final List<Chemical> background;

    private Reaction(Mixture reactants, Mixture products, int electrons, List<Chemical> background) {
        this.reactants = Objects.requireNonNull(reactants, "reactants");
        this.products = Objects.requireNonNull(products, "products");
        this.electrons = electrons;
        this.background = List.copyOf(Objects.requireNonNull(background, "background"));
    }

    /**
     * Creates a reaction without a current and without a background.
     *
     * @param reactants what goes in
     * @param products what comes out
     * @return the reaction
     */
    public static Reaction of(Mixture reactants, Mixture products) {
        return new Reaction(reactants, products, 0, List.of());
    }

    /**
     * Creates a reaction.
     *
     * @param reactants what goes in
     * @param products what comes out
     * @param electrons electrons the reaction takes in, negative when it gives them out
     * @param background substances that may move whole and unnumbered
     * @return the reaction
     */
    public static Reaction of(Mixture reactants, Mixture products, int electrons,
            List<Chemical> background) {
        return new Reaction(reactants, products, electrons, background);
    }

    /** What goes into this reaction. */
    public Mixture reactants() {
        return reactants;
    }

    /** What comes out of this reaction. */
    public Mixture products() {
        return products;
    }

    /** Electrons this reaction takes in, negative when it gives them out. */
    public int electrons() {
        return electrons;
    }

    /** Substances that may move whole and unnumbered around this reaction. */
    public List<Chemical> background() {
        return background;
    }

    @Override
    public String toString() {
        return "Reaction(" + reactants + (electrons == 0 ? "" : (electrons > 0 ? " + " + electrons
                + "e-" : " - " + (-electrons) + "e-")) + " -> " + products + ", background "
                + background.size() + ")";
    }
}
