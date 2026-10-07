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
 * Checks the reactions a reagent of its own is written for, in which the group the reagent brings is put on
 * the carbon a carbonyl carried.
 * <p>
 * Two of them are asked here. The Wittig reaction turns a carbonyl into a double bond exactly where it stood,
 * the oxygen being carried off whole by a phosphorus; the Grignard reaction puts a carbon where the carbonyl
 * was and leaves the oxygen on the metal. Both are asked for the substances a vessel loses and gains, and
 * for the shape the answer carries.
 */
class CarbonylReactionsTest {

    /** The engine over the rules the organic side ships with. */
    private static final PolarEngine ENGINE = new PolarEngine(PolarReactions.all());

    /** A phosphorane: a phosphorus held to three rings and to one carbon by a double bond. */
    private static final String YLIDE = "C=P(c1ccccc1)(c1ccccc1)c1ccccc1";

    /** The phosphine oxide the ylide leaves behind. */
    private static final String PHOSPHINE_OXIDE = "O=P(c1ccccc1)(c1ccccc1)c1ccccc1";

    @Test
    void aWittigReactionPutsADoubleBondWhereTheCarbonylStood() {
        Reaction reaction = best(Warmth.AMBIENT, YLIDE, "CC=O");

        assertNotNull(reaction, "a phosphorane and ethanal");
        assertEquals(Set.of("C3H6", "C18H15OP"), formulas(reaction.products()),
                "propene and the phosphine oxide");
        assertFalse(Sites.alkenes(onlyCarbonProduct(reaction).structure()).isEmpty(),
                "and the carbon the oxygen hung on is part of a double bond now");
    }

    @Test
    void aGrignardReagentPutsACarbonWhereTheCarbonylStood() {
        Reaction reaction = best(Warmth.AMBIENT, "C[Mg]Br", "CC=O");

        assertNotNull(reaction, "methylmagnesium bromide and ethanal");
        Chemical product = only(reaction.products());
        assertEquals(Chemical.parse("CC(C)O[Mg]Br").composition().formula(),
                product.composition().formula(), "the magnesium alkoxide of propan-2-ol holds three carbons");
        assertTrue(Sites.carbonyls(product.structure()).isEmpty(), "and the carbonyl is gone");
    }

    @Test
    void aGrignardReagentNeedsACarbonylToAttack() {
        assertNull(best(Warmth.AMBIENT, "C[Mg]Br", "CCCC"),
                "a reagent with nothing to attack is left alone");
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

    /** The one substance of a pile that holds carbon and is no reagent. */
    private static Chemical onlyCarbonProduct(Reaction reaction) {
        List<Chemical> chemicals = List.copyOf(reaction.products().components().keySet());
        for (Chemical chemical : chemicals) {
            String formula = chemical.composition().formula();
            if (formula.startsWith("C") && !formula.contains("P")) {
                return chemical;
            }
        }
        throw new IllegalStateException("no carbon in " + reaction.products());
    }
}
