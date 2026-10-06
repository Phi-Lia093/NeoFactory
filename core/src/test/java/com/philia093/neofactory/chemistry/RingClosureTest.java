package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the reactions a molecule runs on itself, which is how the rings of the organic side are closed.
 * <p>
 * Every rule of the table joined two molecules until now, and a rule that joins two molecules of one
 * substance had to be told to spend two of them - which is right for an aldol and wrong for a lactone: a
 * molecule that carries an acid at one end and an alcohol at the other closes a ring out of <em>one</em>
 * molecule of itself. What is asked here is the two halves of that: that a molecule with two places to join
 * closes a ring and spends one of itself, and that a molecule whose two places stand too close together is
 * left alone rather than drawn as a ring of three that no geometry allows.
 */
class RingClosureTest {

    /** The engine over the rules the organic side ships with. */
    private static final PolarEngine ENGINE = new PolarEngine(PolarReactions.all());

    @Test
    void anAldolFoldsOneMoleculeIntoTARing() {
        // Hexane-2,5-dione: a methyl beside one carbonyl joins the far carbonyl of the same molecule.
        Reaction reaction = best("CC(=O)CCC(=O)C");

        assertNotNull(reaction, "the two ends of one molecule are an aldol");
        assertEquals(1, reaction.reactants().amountOf(Chemical.parse("CC(=O)CCC(=O)C")),
                "one molecule of it was spent and not two");
        assertFalse(Rings.cycles(only(reaction.products()).structure()).isEmpty(),
                "and what came out of it is a ring");
    }

    @Test
    void anAcidAndItsOwnAlcoholCloseALactone() {
        // 5-hydroxypentanoic acid over a flame: the ring of six a lactone is.
        Reaction reaction = best(Warmth.HEATED, "OCCCCC(=O)O");

        assertNotNull(reaction, "an acid and an alcohol of one molecule close a lactone");
        assertEquals(Set.of("C5H8O2", "H2O"), formulas(reaction.products()));
        assertEquals(1, reaction.reactants().amountOf(Chemical.parse("OCCCCC(=O)O")),
                "one molecule of it was spent");
        assertFalse(Rings.cycles(onlyCarbonProduct(reaction).structure()).isEmpty(),
                "the lactone is a ring");
    }

    @Test
    void anAmineAndItsOwnAcidCloseALactam() {
        Reaction reaction = best(Warmth.HEATED, "NCCCC(=O)O");

        assertNotNull(reaction, "an amine and an acid of one molecule close a lactam");
        assertEquals(Set.of("C4H7NO", "H2O"), formulas(reaction.products()));
        assertEquals(1, reaction.reactants().amountOf(Chemical.parse("NCCCC(=O)O")),
                "one molecule of it was spent");
    }

    @Test
    void aRingOfThreeIsNeverDrawn() {
        // Glycolic acid: the alcohol stands two bonds from the acid carbon, so the ring it would close is a
        // triangle - which is not a ring this table draws, whatever else the vessel does with it.
        Reaction reaction = best(Warmth.HEATED, "OCC(=O)O");

        if (reaction != null) {
            for (Chemical chemical : reaction.products().components().keySet()) {
                assertTrue(Rings.cycles(chemical.structure()).isEmpty(),
                        "nothing that came out of it is a ring: " + chemical.composition().formula());
            }
        }
    }

    @Test
    void twoMoleculesOfOneSubstanceAreStillTwoMolecules() {
        // Two molecules of ethanal: the aldol of two of them, which is what the counting had to keep working
        // for a pair that really is two molecules and not one.
        Reaction reaction = best("CC=O", "CC=O");

        assertNotNull(reaction, "two molecules of ethanal condense");
        assertEquals(2, reaction.reactants().amountOf(Chemical.parse("CC=O")),
                "two molecules of it were spent");
        assertEquals(Set.of("C4H8O2"), formulas(reaction.products()), "the aldol of the two of them");
    }

    /** The one reaction an engine would run in a vessel of the substances named by their strings. */
    private static Reaction best(String... smiles) {
        return best(Conditions.NONE, smiles);
    }

    /** The one reaction an engine would run at one of the three settings. */
    private static Reaction best(Warmth warmth, String... smiles) {
        return best(Conditions.at(warmth), smiles);
    }

    /** The one reaction an engine would run in a vessel of the conditions and the substances named. */
    private static Reaction best(Conditions conditions, String... smiles) {
        Mixture pot = Mixture.empty();
        for (String one : smiles) {
            pot = pot.plus(Mixture.of(Chemical.parse(one), 1));
        }
        return ENGINE.best(System.of(pot, Phase.LIQUID).with(conditions));
    }

    /** The formulas of the substances of a pile. */
    private static Set<String> formulas(Mixture mixture) {
        Set<String> formulas = new TreeSet<>();
        for (Chemical chemical : mixture.components().keySet()) {
            formulas.add(chemical.composition().formula());
        }
        return formulas;
    }

    /** The one substance of a pile. */
    private static Chemical only(Mixture mixture) {
        return mixture.components().keySet().iterator().next();
    }

    /** The one substance of a pile that holds carbon. */
    private static Chemical onlyCarbonProduct(Reaction reaction) {
        for (Chemical chemical : reaction.products().components().keySet()) {
            if (chemical.composition().formula().startsWith("C")) {
                return chemical;
            }
        }
        throw new IllegalStateException("no carbon in " + reaction.products());
    }
}
