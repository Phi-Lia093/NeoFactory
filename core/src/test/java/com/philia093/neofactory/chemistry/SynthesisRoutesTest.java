package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the routes a modern fibre and a modern medicine are really built along, one step after another.
 * <p>
 * <b>A rule is worth what it chains with, and a route is a chain of rules.</b> So what is asked here is not
 * one reaction but the whole of a route: benzene nitrated and reduced to aniline, and the aniline nitrated
 * and reduced again to a diamine; an aromatic alcohol oxidised the two steps to its acid; the acid and the
 * amine joined by the amide bond an aramid repeats; an ortho diamine closed with an aromatic acid into the
 * benzimidazole a fire-proof fibre repeats; and the three monomers of a rubber plastic, each made the way
 * the industry makes it.
 * <p>
 * <b>Each route is asserted step by step</b>, because a failure three steps in says nothing about which step
 * was wrong unless every step was checked on the way; and the answer to a step is read as the formulas of all
 * its substances and as the groups the carbon-bearing answer carries, never as a string of SMILES, which the
 * canonical form of a molecule is free to rearrange.
 */
class SynthesisRoutesTest {

    /** The engine over the rules the organic side ships with. */
    private static final PolarEngine ENGINE = new PolarEngine(PolarReactions.all());

    /** The acid a nitration is run over, and the acid itself that is poured in. */
    private static final Conditions NITRATION = Conditions.builder()
            .warmth(Warmth.HEATED).catalyst(Chemical.parse("OS(=O)(=O)O")).build();

    /** A vessel of hydrogen over nickel, which is what every reduction of these routes is run with. */
    private static final Conditions OVER_NICKEL = Conditions.at(Warmth.AMBIENT,
            Set.of(Chemical.parse("[Ni]")));

    /** A vessel over the nickel a dehydrogenation of an alkane is run with. */
    private static final Conditions OVER_HOT_NICKEL = Conditions.at(Warmth.HEATED,
            Set.of(Chemical.parse("[Ni]")));

    @Test
    void benzeneIsNitratedAndReducedToAniline() {
        Reaction nitrated = best(NITRATION, "c1ccccc1", "O[N+](=O)[O-]", "OS(=O)(=O)O");
        assertNotNull(nitrated, "benzene and nitric acid are nitrobenzene");
        assertEquals(Set.of("C6H5NO2", "H2O"), formulas(nitrated.products()));

        Reaction reduced = next(nitrated, OVER_NICKEL, "[H][H]", "[H][H]", "[H][H]");
        assertNotNull(reduced, "and the nitro group is taken down to an amine");
        assertEquals(Set.of("C6H7N", "H2O"), formulas(reduced.products()), "aniline");
        assertFalse(Sites.amines(onlyCarbonProduct(reduced).structure()).isEmpty(),
                "what came out of it carries an amine");
    }

    @Test
    void anilineIsNitratedAndReducedToADiamine() {
        Reaction nitrated = best(NITRATION, "Nc1ccccc1", "O[N+](=O)[O-]", "OS(=O)(=O)O");
        assertNotNull(nitrated, "aniline and nitric acid");
        assertEquals(Set.of("C6H6N2O2", "H2O"), formulas(nitrated.products()), "a nitroaniline");

        Reaction reduced = next(nitrated, OVER_NICKEL, "[H][H]", "[H][H]", "[H][H]");
        assertNotNull(reduced, "and the nitro group of it is taken down too");
        assertEquals(Set.of("C6H8N2", "H2O"), formulas(reduced.products()), "a phenylenediamine");
        assertEquals(2, Sites.amines(onlyCarbonProduct(reduced).structure()).size(),
                "and it carries two amines, which is what a fibre is woven from");
    }

    @Test
    void theTwoStepsOfOxidisingAnAromaticAlcoholToItsAcid() {
        // The oxidation of an aromatic side chain, as a works runs it: the alcohol loses its hydrogen into
        // an aldehyde over hot metal, and the aldehyde is taken up to the acid by water - two operations,
        // the hydrogen of the first vented before the second is poured in.
        Reaction aldehyde = best(OVER_HOT_NICKEL, "OCc1ccccc1");
        assertNotNull(aldehyde, "benzyl alcohol over hot nickel");
        assertEquals(Set.of("C7H6O", "H2"), formulas(aldehyde.products()), "benzaldehyde");

        Reaction acid = best(Warmth.HEATED, "O=Cc1ccccc1", "O");
        assertNotNull(acid, "and the aldehyde with water over a flame");
        assertEquals(Set.of("C7H6O2", "H2"), formulas(acid.products()), "benzoic acid");
        assertEquals(1, Sites.acids(onlyCarbonProduct(acid).structure()).size(),
                "the acid an aramid is woven from");
    }

    @Test
    void anAcidAndAnAmineAreJoinedByTheAmideBondOfAFibre() {
        Reaction amide = best(Warmth.HEATED, "OC(=O)c1ccccc1", "Nc1ccccc1");

        assertNotNull(amide, "benzoic acid and aniline");
        assertEquals(Set.of("C13H11NO", "H2O"), formulas(amide.products()), "benzanilide and water");
        assertEquals(1, Sites.amides(onlyCarbonProduct(amide).structure()).size(),
                "the amide bond an aramid repeats");
    }

    @Test
    void anOrthoDiamineAndAnAromaticAcidCloseTheRingOfAHeatProofFibre() {
        // PBI: an ortho diamine and an aromatic diacid close a benzimidazole ring, which is the unit the
        // fibre repeats - the whole point of the intramolecular closure the table had to be taught.
        Reaction unit = best(Warmth.HEATED, "Nc1ccccc1N", "OC(=O)c1cccc(C(=O)O)c1");

        assertNotNull(unit, "an ortho diamine and an aromatic acid");
        assertEquals(Set.of("C14H10N2O2", "H2O"), formulas(unit.products()),
                "the benzimidazole with its other acid arm and the two waters read as one name");
        assertTrue(Rings.cycles(onlyCarbonProduct(unit).structure()).size() >= 2,
                "and what came out of it is a fused ring");
    }

    @Test
    void theThreeMonomersOfARubberPlastic() {
        // ABS is three monomers, and each of them is made the way the trade makes it.
        Reaction nitrile = best(Warmth.HEATED, "C#C", "C#N");
        assertNotNull(nitrile, "acetylene and hydrogen cyanide");
        assertEquals(Set.of("C3H3N"), formulas(nitrile.products()), "acrylonitrile");
        assertFalse(Sites.nitriles(onlyCarbonProduct(nitrile).structure()).isEmpty(),
                "which carries the nitrile");

        Reaction diene = best(OVER_HOT_NICKEL, "CCCC");
        assertNotNull(diene, "butane over hot nickel");
        Reaction twiceUnsaturated = next(diene, OVER_HOT_NICKEL);
        assertNotNull(twiceUnsaturated, "and the butene loses a second molecule of hydrogen");
        assertEquals(Set.of("C4H6", "H2"), formulas(twiceUnsaturated.products()), "butadiene");
        assertFalse(Sites.alkenes(onlyCarbonProduct(twiceUnsaturated).structure()).isEmpty(),
                "which is still an alkene");

        Reaction styrene = best(OVER_HOT_NICKEL, "CCc1ccccc1");
        assertNotNull(styrene, "ethylbenzene over hot nickel");
        assertEquals(Set.of("C8H8", "H2"), formulas(styrene.products()), "styrene");
        assertFalse(Sites.alkenes(onlyCarbonProduct(styrene).structure()).isEmpty(),
                "which is the alkene of the ring");
    }

    /** The one reaction an engine would run in a vessel of the conditions and the substances named. */
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

    /** The one substance of a pile that holds carbon. */
    private static Chemical onlyCarbonProduct(Reaction reaction) {
        List<Chemical> chemicals = List.copyOf(reaction.products().components().keySet());
        for (Chemical chemical : chemicals) {
            if (chemical.composition().formula().startsWith("C")) {
                return chemical;
            }
        }
        throw new IllegalStateException("no carbon in " + reaction.products());
    }
}
