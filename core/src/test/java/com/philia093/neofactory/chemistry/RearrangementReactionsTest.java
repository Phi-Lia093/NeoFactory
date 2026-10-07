package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Checks the rearrangements a molecule runs on itself, in which nothing is added and nothing leaves and only
 * the arrangement of the atoms changes.
 * <p>
 * Two reactions of the table are one and the same move - six electrons going round a ring of six - and they
 * are named for what stands in the middle of the chain: the Claisen rearrangement, whose chain holds an
 * oxygen and whose answer is a carbonyl, and the Cope rearrangement, whose chain is carbon throughout and
 * whose answer is another diene. What is asked here is that each of them comes out the way the trade finds
 * it: the allyl vinyl ether an aldehyde, and the branched diene a plain one.
 */
class RearrangementReactionsTest {

    /** The engine over the rules the organic side ships with. */
    private static final PolarEngine ENGINE = new PolarEngine(PolarReactions.all());

    @Test
    void theClaisenRearrangementOfAnAllylVinylEther() {
        Reaction reaction = best("C=CCOC=C");

        assertNotNull(reaction, "an allyl vinyl ether over a flame");
        assertEquals(Set.of("C5H8O"), formulas(reaction.products()), "the same atoms, rearranged");
        assertFalse(Sites.carbonyls(only(reaction.products()).structure()).isEmpty(),
                "and what came out of it carries the carbonyl of pent-4-enal");
        assertEquals(0, reaction.products().charge(), "with nothing charged anywhere");
    }

    @Test
    void theCopeRearrangementOfA15Diene() {
        Reaction reaction = best("C=CC(C)CC=C");

        assertNotNull(reaction, "a branched 1,5-diene over a flame");
        assertEquals(Set.of("C7H12"), formulas(reaction.products()), "the same atoms, rearranged");
        assertEquals(StereoKey.of(Chemical.parse("CC=CCCC=C").structure()),
                StereoKey.of(only(reaction.products()).structure()),
                "and the branch has walked to the end of the chain");
    }

    @Test
    void aShortChainIsLeftAlone() {
        // An allyl group on its own is no sigmatropic chain: the six atoms a shift runs along are not there.
        assertEquals(null, best(Warmth.AMBIENT, "C=CCC(C)(C)C"),
                "a chain too short is no sigmatropic chain");
    }

    /** The one reaction an engine would run at a flame in a vessel of the substances named. */
    private static Reaction best(String... smiles) {
        return best(Warmth.HEATED, smiles);
    }

    /** The one reaction an engine would run at a setting in a vessel of the substances named. */
    private static Reaction best(Warmth warmth, String... smiles) {
        Mixture pot = Mixture.empty();
        for (String one : smiles) {
            pot = pot.plus(Mixture.of(Chemical.parse(one), 1));
        }
        return ENGINE.best(System.of(pot, Phase.LIQUID).with(Conditions.at(warmth)));
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
        List<Chemical> chemicals = List.copyOf(mixture.components().keySet());
        assertEquals(1, chemicals.size(), "one substance is left");
        return chemicals.get(0);
    }
}
