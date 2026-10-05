package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the engine that finds the reactions of the organic side by the groups the rules ask for.
 * <p>
 * Each family is asked for the substances a vessel really loses and gains - ethane out of ethene and
 * hydrogen, a vicinal dichloride out of ethene and chlorine, the alcohol of Markovnikov out of propene and
 * water - and the answer is read as the substances it names, which is what a machine would have to be fed.
 * Where a rule of the industry says which end of a double bond a ligand goes to, that is asked as well, and
 * the last thing asked is the one the whole design turns on: a vessel no rule covers is answered with no
 * reaction at all and never with something like one.
 */
class PolarEngineTest {

    /** The engine over the rules the organic side ships with. */
    private static final PolarEngine ENGINE = new PolarEngine(PolarReactions.all());

    @Test
    void aHydrogenAddsAcrossADoubleBond() {
        Reaction reaction = best("C=C", "[H][H]");

        assertNotNull(reaction, "ethene and hydrogen are an alkane");
        assertEquals(Set.of("C2H6"), formulas(reaction.products()));
        assertEquals(reaction.reactants().charge(), reaction.products().charge());
    }

    @Test
    void aHalogenAddsAcrossADoubleBond() {
        Reaction reaction = best("C=C", "ClCl");

        assertNotNull(reaction, "ethene and chlorine are a vicinal dichloride");
        assertEquals(Set.of("C2H4Cl2"), formulas(reaction.products()));

        Molecule product = only(reaction.products()).structure();
        for (int atom = 0; atom < product.atomCount(); atom++) {
            if (product.atom(atom).element().equals("C")) {
                assertEquals(1, halogensOn(product, atom),
                        "each carbon of the double bond took one halogen");
            }
        }
    }

    @Test
    void theHydrogenOfAnAcidGoesToTheCarbonThatCarriesMoreOfThem() {
        // Propene and hydrogen chloride: the hydrogen takes the end carbon and the chlorine the middle one.
        Reaction reaction = best("CC=C", "[H]Cl");

        assertNotNull(reaction, "propene and hydrogen chloride");
        assertEquals(Set.of("C3H7Cl"), formulas(reaction.products()));

        Molecule product = only(reaction.products()).structure();
        int carbon = carbonCarrying(product, "Cl");
        assertEquals(2, carbonNeighbours(product, carbon),
                "the chlorine stands on the carbon between two others");
    }

    @Test
    void theHydroxylOfWaterGoesToTheCarbonThatCarriesFewerHydrogens() {
        Reaction reaction = best("CC=C", "[H]O[H]");

        assertNotNull(reaction, "propene and water are propan-2-ol");
        assertEquals(Set.of("C3H8O"), formulas(reaction.products()));

        Molecule product = only(reaction.products()).structure();
        int carbon = carbonCarrying(product, "O");
        assertEquals(2, carbonNeighbours(product, carbon), "the hydroxyl is on the middle carbon");
    }

    @Test
    void aHydroxideTakesThePlaceOfABromide() {
        Reaction reaction = best("[OH-]", "[C@H](Br)(F)Cl");

        assertNotNull(reaction, "a hydroxide and bromochlorofluoromethane");
        assertEquals(Set.of("Br", "CH2ClFO"), formulas(reaction.products()));
        assertEquals(reaction.reactants().charge(), reaction.products().charge(), "the charge came out");
    }

    @Test
    void aCyanideAddsToACarbonylAndTheCentreItLeavesHasAHand() {
        Reaction reaction = best("[C-]#N", "CC=O");

        assertNotNull(reaction, "a cyanide and ethanal");
        assertEquals(Set.of("C3H4NO"), formulas(reaction.products()));
        assertEquals(reaction.reactants().charge(), reaction.products().charge());

        Molecule product = only(reaction.products()).structure();
        assertTrue(StereoKey.of(product).contains("R") || StereoKey.of(product).contains("S"),
                "the carbon the cyanide came into is a centre whose hand the face decided");
    }

    @Test
    void theHydrogenOfAnAlcoholAddsToACarbonyl() {
        // Ethanal and methanol written with its hydrogen: the two are one molecule, a hemiacetal.
        Reaction reaction = best("CC=O", "CO[H]");

        assertNotNull(reaction, "ethanal and methanol are a hemiacetal");
        assertEquals(Set.of("C3H8O2"), formulas(reaction.products()));
    }

    @Test
    void anAlcoholLosesWaterAndLeavesADoubleBond() {
        // Ethanol with the hydrogen of its far carbon written out, which is the one that leaves with the
        // group.
        Reaction reaction = best("C([H])CO");

        assertNotNull(reaction, "an alcohol loses water");
        assertEquals(Set.of("C2H4", "H2O"), formulas(reaction.products()));
    }

    @Test
    void aVesselNoRuleCoversIsLeftAlone() {
        assertNull(best("c1ccccc1"), "nothing is extrapolated out of a ring of carbon");
        assertNull(best("CCCC"), "nothing is extrapolated out of an alkane");
        assertNull(best("CCO"), "an alcohol whose hydrogen was not written out loses nothing");
    }

    @Test
    void everyRuleOfTheTableNamesTheGroupsItNeeds() {
        List<ReactionRule> rules = PolarReactions.all();

        assertEquals(8, rules.size(), "the table of the polar families");
        for (ReactionRule rule : rules) {
            assertTrue(!rule.needs().isEmpty(), rule.name() + " names no group at all");
        }
    }

    /** The one reaction an engine would run in a vessel of the substances named by their strings. */
    private static Reaction best(String... smiles) {
        Mixture pot = Mixture.empty();
        for (String one : smiles) {
            pot = pot.plus(Mixture.of(Chemical.parse(one), 1));
        }
        return ENGINE.best(System.of(pot, Phase.LIQUID));
    }

    /** The formulas of the substances of a pile, which is what a balance is read off. */
    private static Set<String> formulas(Mixture mixture) {
        Set<String> formulas = new TreeSet<>();
        for (Chemical chemical : mixture.components().keySet()) {
            formulas.add(chemical.composition().formula());
        }
        return formulas;
    }

    /** The one substance of a pile, which every family but the one of moisture leaves behind. */
    private static Chemical only(Mixture mixture) {
        List<Chemical> chemicals = List.copyOf(mixture.components().keySet());
        assertEquals(1, chemicals.size(), "one substance is left");
        return chemicals.get(0);
    }

    /** How many halogens hang on an atom. */
    private static int halogensOn(Molecule molecule, int atom) {
        int count = 0;
        for (int neighbour : molecule.neighbours(atom)) {
            switch (molecule.atom(neighbour).element()) {
                case "F":
                case "Cl":
                case "Br":
                case "I":
                    count++;
                    break;
                default:
                    break;
            }
        }
        return count;
    }

    /** The carbon that carries a named element. */
    private static int carbonCarrying(Molecule molecule, String element) {
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            if (!molecule.atom(atom).element().equals("C")) {
                continue;
            }
            for (int neighbour : molecule.neighbours(atom)) {
                if (molecule.atom(neighbour).element().equals(element)) {
                    return atom;
                }
            }
        }
        return -1;
    }

    /** How many carbons hang on an atom. */
    private static int carbonNeighbours(Molecule molecule, int atom) {
        int count = 0;
        for (int neighbour : molecule.neighbours(atom)) {
            if (molecule.atom(neighbour).element().equals("C")) {
                count++;
            }
        }
        return count;
    }
}
