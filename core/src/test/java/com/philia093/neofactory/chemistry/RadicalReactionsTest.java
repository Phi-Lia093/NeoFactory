package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the reactions that are carried by unpaired electrons, one rule at a time and then as a chain.
 * <p>
 * A radical chain is three things and this asks for all three: a chain that is <em>started</em> - a halogen
 * molecule coming apart under a light into two atoms that carry an unpaired electron each; a chain that is
 * <em>carried</em> - one of those atoms adding to the double bond of a monomer and leaving the unpaired
 * electron on the far carbon, ready to take the next monomer; and a chain that is <em>stopped</em> - two
 * unpaired electrons pairing up into a plain bond. Between them they are the whole of what a polymer is made
 * by, and the last of them is the mirror of the first.
 */
class RadicalReactionsTest {

    /** The engine over the rules the organic side ships with. */
    private static final PolarEngine ENGINE = new PolarEngine(PolarReactions.all());

    /** A vessel a light stands over, which is what a halogen is broken by. */
    private static final Conditions IN_THE_LIGHT = Conditions.builder()
            .warmth(Warmth.HEATED).lighted(true).build();

    @Test
    void aHalogenUnderALightComesApartIntoTwoRadicals() {
        Reaction lit = best(IN_THE_LIGHT, "BrBr");

        assertNotNull(lit, "bromine under a light over a flame");
        assertEquals(Set.of("Br"), formulas(lit.products()), "two atoms of bromine read as one name");
        assertEquals(2, lit.products().amountOf(Chemical.parse("[Br]")), "and there are two of them");
        assertTrue(radicalIn(lit.products()), "each of them carrying an unpaired electron");

        assertNull(best(Warmth.HEATED, "BrBr"), "and the same vessel in the dark stands as it is");
    }

    @Test
    void aRadicalAddsAcrossADoubleBondAndLeavesOneBehind() {
        Reaction reaction = best(Warmth.HEATED, "[CH3]", "C=C");

        assertNotNull(reaction, "a methyl radical and ethene");
        assertEquals(Set.of("C3H7"), formulas(reaction.products()), "the propyl radical");
        assertTrue(radicalIn(reaction.products()), "which is still a radical");
    }

    @Test
    void twoRadicalsPairUpAndTheChainStops() {
        Reaction reaction = best(Warmth.AMBIENT, "[CH3]", "[CH3]");

        assertNotNull(reaction, "two methyl radicals");
        assertEquals(Set.of("C2H6"), formulas(reaction.products()), "ethane");
        assertFalse(radicalIn(reaction.products()), "and nothing is left a radical");
    }

    @Test
    void aRadicalChainGrowsOneMonomerAtATime() {
        // The growth of a chain, one operation after another: a radical takes on a monomer of styrene and
        // the unpaired electron moves to the far end of it, ready to take the next one.
        Reaction first = best(Warmth.HEATED, "[CH3]", "C=Cc1ccccc1");
        assertNotNull(first, "a methyl radical and a monomer of styrene");
        assertEquals(Set.of("C9H11"), formulas(first.products()), "one unit of the chain");
        assertTrue(radicalIn(first.products()), "which is carried by a radical");

        Reaction second = next(first, Warmth.HEATED, "C=Cc1ccccc1");
        assertNotNull(second, "and another monomer is taken on");
        assertEquals(Set.of("C17H19"), formulas(second.products()), "two units of it");
        assertTrue(radicalIn(second.products()), "with the radical moved along the chain");
    }

    /** {@code true} when any substance of a pile carries an unpaired electron. */
    private static boolean radicalIn(Mixture mixture) {
        for (Chemical chemical : mixture.components().keySet()) {
            if (!Sites.radicals(chemical.structure()).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** The one reaction an engine would run at a setting in a vessel of the substances named. */
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

    /** What an engine would run next on what one of its own reactions left, at one of the three settings. */
    private static Reaction next(Reaction previous, Warmth warmth, String... more) {
        return next(previous, Conditions.at(warmth), more);
    }

    /** What an engine would run next on what one of its own reactions left, with more poured into it. */
    private static Reaction next(Reaction previous, Conditions conditions, String... more) {
        Mixture pot = previous.products();
        for (String chemical : more) {
            pot = pot.plus(Mixture.of(Chemical.parse(chemical), 1));
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
}
