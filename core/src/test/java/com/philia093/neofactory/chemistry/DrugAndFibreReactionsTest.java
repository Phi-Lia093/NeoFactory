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
 * Checks the rules that were added so that the organic side can carry a drug and a fibre, one rule at a time.
 * <p>
 * Each rule is asked for the substances a vessel loses and gains - the amine an alkyl halide is turned into,
 * the ester an acid chloride hands an alcohol, the acid an aldehyde is oxidised to, the diene an alkane is
 * dehydrogenated to - and, where the answer is a shape and not only a formula, for the group the answer
 * carries: an amide key from an amine and an acid chloride, a ring closed out of an ortho diamine. The two
 * rules a vessel could answer two ways are asked both ways round, and every rule that a reagent of its own
 * is written for is also asked without that reagent, so that a rule that answered anyway would be caught.
 */
class DrugAndFibreReactionsTest {

    /** The engine over the rules the organic side ships with. */
    private static final PolarEngine ENGINE = new PolarEngine(PolarReactions.all());

    /** A vessel of hydrogen over nickel, which is what every reduction of the table is run with. */
    private static final Conditions OVER_NICKEL = Conditions.at(Warmth.AMBIENT,
            Set.of(Chemical.parse("[Ni]")));

    /** A vessel over the nickel a dehydrogenation of an alkane is run with. */
    private static final Conditions OVER_HOT_NICKEL = Conditions.at(Warmth.HEATED,
            Set.of(Chemical.parse("[Ni]")));

    @Test
    void anAmineTakesThePlaceOfABromide() {
        Reaction reaction = best(Warmth.HEATED, "CCBr", "CN");

        assertNotNull(reaction, "bromoethane and methylamine");
        assertEquals(Set.of("C3H9N", "BrH"), formulas(reaction.products()),
                "the amine and the acid that left");
        assertEquals(1, aminesOn(onlyCarbonProduct(reaction), 2),
                "the nitrogen came out holding two carbons");
    }

    @Test
    void anArylBromideGivesNoAmine() {
        assertNull(best(Warmth.HEATED, "Brc1ccccc1", "CN"),
                "a bromide on a ring is no leaving group for an alkylation");
    }

    @Test
    void anAcidChlorideHandsAnAlcoholAnEster() {
        Reaction reaction = best(Warmth.HEATED, "CC(=O)Cl", "CO");

        assertNotNull(reaction, "an acid chloride and methanol");
        assertEquals(Set.of("C3H6O2", "ClH"), formulas(reaction.products()),
                "the ester and the acid that left");
        assertEquals(1, Sites.esters(onlyCarbonProduct(reaction).structure()).size(),
                "what came out of it carries an ester");
    }

    @Test
    void anAcidChlorideAndAnAmineGiveAnAmide() {
        Reaction reaction = best(Warmth.HEATED, "CC(=O)Cl", "CN");

        assertNotNull(reaction, "an acid chloride and methylamine");
        assertEquals(Set.of("C3H7NO", "ClH"), formulas(reaction.products()),
                "the amide and the acid that left");
        assertEquals(1, Sites.amides(onlyCarbonProduct(reaction).structure()).size(),
                "what came out of it carries an amide");
    }

    @Test
    void anAldehydeIsOxidisedToAnAcid() {
        Reaction reaction = best(Warmth.HEATED, "C=O", "O");

        assertNotNull(reaction, "methanal and water");
        assertEquals(Set.of("CH2O2", "H2"), formulas(reaction.products()),
                "the acid and the hydrogen that left");
        assertEquals(1, Sites.acids(onlyCarbonProduct(reaction).structure()).size(),
                "what came out of it is an acid");
    }

    @Test
    void anAlkaneLosesHydrogenAndLeavesADoubleBond() {
        Reaction reaction = best(OVER_HOT_NICKEL, "CCCC");

        assertNotNull(reaction, "butane over hot nickel");
        assertEquals(Set.of("C4H8", "H2"), formulas(reaction.products()), "a butene and hydrogen");
        assertFalse(Sites.alkenes(onlyCarbonProduct(reaction).structure()).isEmpty(),
                "the single bond came up to a double one");
    }

    @Test
    void hydrogenCyanideAddsAcrossAnAlkyne() {
        Reaction reaction = best(Warmth.HEATED, "C#C", "C#N");

        assertNotNull(reaction, "acetylene and hydrogen cyanide");
        assertEquals(Set.of("C3H3N"), formulas(reaction.products()), "acrylonitrile");
        assertFalse(Sites.nitriles(onlyCarbonProduct(reaction).structure()).isEmpty(),
                "the answer carries the nitrile of the cyanide");
    }

    @Test
    void anAllylicHydrogenMovesToASecondAlkene() {
        Reaction reaction = best(Warmth.HEATED, "C=CC", "C=C");

        assertNotNull(reaction, "propene and ethene");
        assertEquals(Set.of("C5H10"), formulas(reaction.products()), "pentene");
        assertFalse(Sites.alkenes(onlyCarbonProduct(reaction).structure()).isEmpty(),
                "and it is still an alkene");
    }

    @Test
    void anOrthoDiamineAndAnAcidCloseABenzimidazole() {
        Reaction reaction = best(Warmth.HEATED, "Nc1ccccc1N", "CC(=O)O");

        assertNotNull(reaction, "an ortho diamine and acetic acid");
        assertEquals(Set.of("C8H8N2", "H2O"), formulas(reaction.products()),
                "2-methylbenzimidazole and the two waters read as one name");
        assertTrue(Rings.cycles(onlyCarbonProduct(reaction).structure()).size() >= 2,
                "the two rings of a benzimidazole");
    }

    @Test
    void aNitroGroupIsReducedAllTheWayToAnAmine() {
        Reaction reaction = best(OVER_NICKEL, "O=[N+]([O-])c1ccccc1", "[H][H]", "[H][H]", "[H][H]");

        assertNotNull(reaction, "nitrobenzene and three molecules of hydrogen");
        assertEquals(Set.of("C6H7N", "H2O"), formulas(reaction.products()),
                "aniline and the two waters read as one name");
        assertEquals(3, reaction.reactants().amountOf(Chemical.parse("[H][H]")),
                "three molecules of hydrogen were spent");
        assertFalse(Sites.amines(onlyCarbonProduct(reaction).structure()).isEmpty(),
                "what came out of it carries an amine");
    }

    @Test
    void anAmineTakesThePlaceOfAHalogenOnARingThatPullsElectrons() {
        Reaction reaction = best(Warmth.HEATED, "O=[N+]([O-])c1ccccc1F", "CN");

        assertNotNull(reaction, "a nitro group beside a fluoride and methylamine");
        assertEquals(Set.of("C7H8N2O2", "FH"), formulas(reaction.products()),
                "the arylamine and the acid that left");
        assertFalse(Sites.amines(onlyCarbonProduct(reaction).structure()).isEmpty(),
                "the nitrogen went onto the ring");
    }

    /** How many atoms of a named element hang on the atoms of a group the molecule carries. */
    private static int aminesOn(Chemical chemical, int carbons) {
        Molecule molecule = chemical.structure();
        int count = 0;
        for (Site amine : Sites.amines(molecule)) {
            int hanging = 0;
            for (int neighbour : molecule.neighbours(amine.atom(0))) {
                if (molecule.atom(neighbour).element().equals("C")) {
                    hanging++;
                }
            }
            if (hanging == carbons) {
                count++;
            }
        }
        return count;
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
