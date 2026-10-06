package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the reactions that were always within reach of the engine and had never been written down, and the
 * chains they make when a vessel is run through more than one operation.
 * <p>
 * <b>The value of a rule is what it chains with.</b> A nitrile hydrolysed once is an imidic acid and
 * hydrolysed twice is an acid; an alkyne hydrated is an enol and tautomerised is a ketone; an aldehyde
 * condensed with an amine is an imine and reduced is an amine - and the second of each pair is only possible
 * because the first hands over a substance the next rule can read. So what is asked here is the step and then
 * the step after it, which is the shape a route of the game really has.
 */
class ChainReactionsTest {

    /** The engine over the rules the organic side ships with. */
    private static final PolarEngine ENGINE = new PolarEngine(PolarReactions.all());

    /** A vessel of hydrogen over nickel: what every reduction of the table is run with. */
    private static final Conditions OVER_NICKEL = Conditions.at(Warmth.AMBIENT,
            Set.of(Chemical.parse("[Ni]")));

    @Test
    void anEnolFallsIntoTheCarbonylItStandsFor() {
        Reaction reaction = best(Warmth.AMBIENT, "CC(O)=C");

        assertNotNull(reaction, "the enol of propanone");
        assertEquals(Set.of("C3H6O"), formulas(reaction.products()));
        assertTrue(Sites.enols(only(reaction.products()).structure()).isEmpty(),
                "and what came out of it is no enol any more");
        assertFalse(Sites.carbonyls(only(reaction.products()).structure()).isEmpty(),
                "it is the carbonyl the enol was the shape of");
    }

    @Test
    void hydratingAnAlkyneGivesAKetoneAndNotAnAlcohol() {
        // Propyne and water: the water goes on the Markovnikov way, so what stands there first is an enol -
        // and the tautomerism of the table is what turns it into propanone.
        Reaction enol = best(Warmth.HEATED, "CC#C", "O");

        assertNotNull(enol, "propyne and water over a flame");
        assertEquals(Set.of("C3H6O"), formulas(enol.products()));
        Reaction ketone = next(enol, Warmth.AMBIENT);

        assertNotNull(ketone, "and the enol then falls into its carbonyl");
        assertFalse(Sites.carbonyls(only(ketone.products()).structure()).isEmpty(),
                "which is propanone");
    }

    @Test
    void hydrogenGoesAcrossAnAlkyneAndLeavesAnAlkene() {
        Reaction reaction = best(OVER_NICKEL, "CC#C", "[H][H]");

        assertNotNull(reaction, "propyne and hydrogen over nickel");
        assertEquals(Set.of("C3H6"), formulas(reaction.products()), "propene");
        assertFalse(Sites.alkenes(only(reaction.products()).structure()).isEmpty(),
                "the triple bond came down to a double one");
    }

    @Test
    void aNitrileBecomesAnAmideInTwoStepsAndAnAcidInThree() {
        Reaction imidic = best(Warmth.HEATED, "CC#N", "O");

        assertNotNull(imidic, "acetonitrile and water");
        assertEquals(Set.of("C2H5NO"), formulas(imidic.products()),
                "the hydroxyl on the carbon of the nitrile, which is what the trade calls the imidic acid");

        Reaction amide = next(imidic, Warmth.AMBIENT);
        assertNotNull(amide, "which tautomerises");
        assertFalse(Sites.amides(only(amide.products()).structure()).isEmpty(), "into the amide");

        Reaction acid = next(amide, Warmth.HEATED, "O");
        assertNotNull(acid, "and the amide is hydrolysed in its turn");
        assertEquals(Set.of("C2H4O2", "H3N"), formulas(acid.products()), "acetic acid and ammonia");
    }

    @Test
    void anAmineIsMadeOutOfACarbonylInTwoSteps() {
        // The reductive amination: the condensation first, the hydrogen second, which is the two-step route
        // the trade runs every day and the reason a ketone and an amine are worth putting in one vessel.
        Reaction imine = best(Warmth.AMBIENT, "CC=O", "CN");

        assertNotNull(imine, "ethanal and methylamine");
        assertEquals(Set.of("C3H7N", "H2O"), formulas(imine.products()));

        Reaction amine = next(imine, OVER_NICKEL, "[H][H]");
        assertNotNull(amine, "and the imine takes hydrogen");
        assertEquals(Set.of("C3H9N"), formulas(amine.products()),
                "the amine the carbonyl and the amine of the vessel were condensed into");
    }

    @Test
    void anEsterChangesHandsWithoutGoingBackToTheAcid() {
        Reaction reaction = best(Warmth.HEATED, "CCOC(C)=O", "CO");

        assertNotNull(reaction, "ethyl acetate and methanol");
        assertEquals(Set.of("C3H6O2", "C2H6O"), formulas(reaction.products()),
                "methyl acetate and ethanol");
    }

    @Test
    void anAmineTakesAnEsterOverIntoAnAmide() {
        Reaction reaction = best(Warmth.HEATED, "CCOC(C)=O", "CN");

        assertNotNull(reaction, "ethyl acetate and methylamine");
        assertEquals(Set.of("C3H7NO", "C2H6O"), formulas(reaction.products()),
                "the amide and the alcohol");
    }

    @Test
    void anImineIsTakenBackToItsCarbonylByWater() {
        Reaction reaction = best(Warmth.HEATED, "CC=NC", "O");

        assertNotNull(reaction, "an imine and water");
        assertEquals(Set.of("C2H4O", "CH5N"), formulas(reaction.products()),
                "the carbonyl it came from and the amine");
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

    /** What an engine would run next on what one of its own reactions left, with more poured into it. */
    private static Reaction next(Reaction previous, Warmth warmth, String... more) {
        return next(previous, Conditions.at(warmth), more);
    }

    /** What an engine would run next on what one of its own reactions left, at named conditions. */
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

    /** The one substance of a pile. */
    private static Chemical only(Mixture mixture) {
        return mixture.components().keySet().iterator().next();
    }
}
