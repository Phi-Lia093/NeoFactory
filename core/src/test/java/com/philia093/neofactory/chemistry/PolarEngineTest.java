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
        assertNull(best("CCCC"), "nothing is extrapolated out of an alkane with nothing to react with");
        assertNull(best("CC=O"), "an aldehyde with nothing to add is left alone");
    }

    @Test
    void anAlcoholWhoseHydrogensWereLeftToTheirValenceIsReadTheSameWay() {
        // The hydrogens are written out while a vessel is laid out, so a string that leaves them to the
        // valence is read exactly as one that spells them out.
        Reaction spelt = best("C([H])CO");
        Reaction plain = best("CCO");

        assertNotNull(plain, "ethanol loses water whether or not its hydrogens were written out");
        assertEquals(Set.of("C2H4", "H2O"), formulas(plain.products()));
        assertEquals(spelt.products(), plain.products(), "the two strings are the same vessel");
    }

    @Test
    void everyRuleOfTheTableNamesTheGroupsItNeeds() {
        List<ReactionRule> rules = PolarReactions.all();

        assertEquals(23, rules.size(), "the table of the polar families");
        for (ReactionRule rule : rules) {
            assertTrue(!rule.needs().isEmpty(), rule.name() + " names no group at all");
        }
    }

    @Test
    void aCarbonylTakesHydrogenAndComesOutAnAlcohol() {
        Reaction reaction = best("CC=O", "[H][H]");

        assertNotNull(reaction, "ethanal and hydrogen are ethanol");
        assertEquals(Set.of("C2H6O"), formulas(reaction.products()));
    }

    @Test
    void aMethanolLosesHydrogenBecauseItHasNoCarbonToLoseWaterWith() {
        // Methanol has no carbon beside the one the hydroxyl hangs on, so it cannot dehydrate; what is left
        // for it is the oxidation, and it comes out formaldehyde and hydrogen.
        Reaction reaction = best("CO");

        assertNotNull(reaction, "methanol comes out formaldehyde");
        assertEquals(Set.of("CH2O", "H2"), formulas(reaction.products()));
    }

    @Test
    void twoAldehydesGiveAnAldol() {
        // Two molecules of ethanal stand in the vessel, so one may take the hydrogen off the other.
        Reaction reaction = best("CC=O", "CC=O");

        assertNotNull(reaction, "ethanal condenses with itself");
        assertEquals(Set.of("C4H8O2"), formulas(reaction.products()));
        assertEquals(Mixture.of(Chemical.parse("CC=O"), 2), reaction.reactants(),
                "and it takes two molecules of it to do so");
    }

    @Test
    void aDieneAndADoubleBondCloseIntoARingOfSix() {
        Reaction reaction = best("C=CC=C", "C=C");

        assertNotNull(reaction, "butadiene and ethene are cyclohexene");
        assertEquals(Set.of("C6H10"), formulas(reaction.products()));

        Molecule product = only(reaction.products()).structure();
        int carbons = 0;
        for (int atom = 0; atom < product.atomCount(); atom++) {
            if (product.atom(atom).element().equals("C")) {
                carbons++;
            }
        }
        assertEquals(6, carbons, "the ring is made of all six carbons");
    }

    @Test
    void anAcidAndAnAlcoholGiveAnEster() {
        Reaction reaction = best("CC(=O)O", "CO");

        assertNotNull(reaction, "ethanoic acid and methanol are methyl ethanoate");
        assertEquals(Set.of("C3H6O2", "H2O"), formulas(reaction.products()));
    }

    @Test
    void anEsterAndWaterGiveTheAcidAndTheAlcoholBack() {
        Reaction reaction = best("CC(=O)OC", "[H]O[H]");

        assertNotNull(reaction, "methyl ethanoate and water");
        assertEquals(Set.of("C2H4O2", "CH4O"), formulas(reaction.products()));
    }

    @Test
    void aHalogenTakesThePlaceOfAHydrogenOnAnAlkane() {
        Reaction reaction = best("CC", "ClCl");

        assertNotNull(reaction, "ethane and chlorine");
        assertEquals(Set.of("C2H5Cl", "ClH"), formulas(reaction.products()),
                "the acid of a halogen and a hydrogen is written with the halogen first");
    }

    @Test
    void twoEstersJoinAtACarbonAndGiveAKetoEster() {
        Reaction reaction = best("CC(=O)OC", "CC(=O)OC");

        assertNotNull(reaction, "methyl ethanoate condenses with itself");
        assertEquals(Set.of("C5H8O3", "CH4O"), formulas(reaction.products()),
                "the keto ester and the alcohol of the group that left");
    }

    @Test
    void anAmineAndAnAldehydeLoseWaterAndGiveAnImine() {
        Reaction reaction = best("CC=O", "CN");

        assertNotNull(reaction, "ethanal and methylamine are an imine");
        assertEquals(Set.of("C3H7N", "H2O"), formulas(reaction.products()));
    }

    @Test
    void aCarbonylAndAnEnoneJoinIntoTwoCarbonylsThreeApart() {
        // Propenal and ethanal: the conjugate addition, which is what an enolate does with an enone.
        Reaction reaction = best("C=CC=O", "CC=O");

        assertNotNull(reaction, "propenal and ethanal join into a two carbonyl chain of five carbons");
        assertEquals(Set.of("C5H8O2"), formulas(reaction.products()));
    }

    @Test
    void anAlkylHalidePutsItsAlkylWhereTheHydrogenStood() {
        Reaction reaction = best("CC(=O)C", "CBr");

        assertNotNull(reaction, "propanone and bromomethane are butanone and the acid");
        assertEquals(Set.of("C4H8O", "BrH"), formulas(reaction.products()));
    }

    @Test
    void aCyanideAttacksTheAldehydeAndNotTheKetoneBesideIt() {
        // An aldehyde beside a ketone is a vessel where two places would do, and only the readier one reacts.
        Reaction reaction = best("CC=O", "CC(=O)C", "[C-]#N");

        assertNotNull(reaction, "the cyanide comes into one of the two carbonyls");
        assertEquals(Set.of("C3H4NO"), formulas(reaction.products()),
                "the cyanohydrin of the aldehyde: the ketone was left where it stood");
    }

    @Test
    void theHalogenThatLeavesMostEasilyIsTheOneThatLeaves() {
        // A chloride beside a bromide with a hydroxide in the vessel: the bromide goes and the chloride stays.
        Reaction reaction = best("ClCCBr", "[OH-]");

        assertNotNull(reaction, "the hydroxide takes the place of one of the two halogens");
        assertEquals(Set.of("C2H5ClO", "Br"), formulas(reaction.products()),
                "the bromide left and the chloride is still there");
    }

    @Test
    void aNucleophileReachesTheCarbonThatIsNotCrowded() {
        // Two bromides on carbons of different crowding: the plainer one is reached first.
        Reaction reaction = best("CC(Br)CCBr", "[OH-]");

        assertNotNull(reaction, "the hydroxide takes the place of one of the two bromides");
        Molecule product = null;
        for (Chemical chemical : reaction.products().components().keySet()) {
            if (chemical.composition().formula().startsWith("C")) {
                product = chemical.structure();
            }
        }
        assertNotNull(product, "the alcohol is one of the substances that come out");
        int carbon = carbonCarrying(product, "O");
        assertEquals(2, Sites.hydrogensOn(product, carbon),
                "the alcohol stands on the carbon that carries two hydrogens and not on the one that "
                        + "carries one");
    }

    @Test
    void sulfurTrioxideTakesThePlaceOfAHydrogenOnARing() {
        Reaction reaction = best("c1ccccc1", "O=S(=O)=O");

        assertNotNull(reaction, "benzene and sulfur trioxide are benzenesulfonic acid");
        assertEquals(Set.of("C6H6O3S"), formulas(reaction.products()));
        assertEquals(reaction.reactants().charge(), reaction.products().charge(), "the charge came out");

        Molecule product = only(reaction.products()).structure();
        assertEquals(6, Sites.aromaticRings(product).get(0).size(),
                "the ring is a ring again and not a chain of alternating bonds");
    }

    @Test
    void aGroupOnARingSendsTheElectrophileBesideItself() {
        // Toluene: a methyl lends electrons to the ring, so the sulfur goes beside it - or across from it -
        // and never past a carbon.
        Reaction reaction = best("Cc1ccccc1", "O=S(=O)=O");

        assertNotNull(reaction, "toluene and sulfur trioxide are toluenesulfonic acid");
        assertEquals(Set.of("C7H8O3S"), formulas(reaction.products()));

        Molecule product = only(reaction.products()).structure();
        List<Integer> ring = Sites.aromaticRings(product).get(0).atoms();
        int distance = ringDistance(ring, ringCarbonCarrying(product, ring, "S"),
                ringCarbonCarrying(product, ring, "C"));
        assertEquals(1, distance, "the sulfur stands beside the methyl and not past a carbon of it");
    }

    @Test
    void aGroupThatPullsElectronsSendsTheElectrophilePastACarbon() {
        // Nitrobenzene: the nitro group empties the positions beside it, so the sulfur goes a carbon on.
        Reaction reaction = best("O=[N+]([O-])c1ccccc1", "O=S(=O)=O");

        assertNotNull(reaction, "nitrobenzene and sulfur trioxide");
        assertEquals(Set.of("C6H5NO5S"), formulas(reaction.products()));

        Molecule product = only(reaction.products()).structure();
        List<Integer> ring = Sites.aromaticRings(product).get(0).atoms();
        int distance = ringDistance(ring, ringCarbonCarrying(product, ring, "S"),
                ringCarbonCarrying(product, ring, "N"));
        assertEquals(2, distance, "the sulfur stands one carbon past the nitro group");
    }

    @Test
    void aRingWithNoElectrophileInTheVesselIsLeftAlone() {
        // The groups are there but the sulfur trioxide is not, and a rule that needs a reagent of its own
        // answers nothing rather than guessing one.
        Reaction reaction = best("c1ccccc1", "CS(=O)(=O)O");

        assertNull(reaction, "a ring and an acid that is no electrophile are not a reaction");
    }

    @Test
    void anAmineTakesAnAcidOverIntoAnAmide() {
        // Acetic acid and methylamine are the amide and the water - and never an imine of an acid, which is
        // not a reaction anyone has ever run: a nucleophile looking for a carbonyl walks past an acid.
        Reaction reaction = best("CC(=O)O", "CN");

        assertNotNull(reaction, "an acid and an amine are an amide");
        assertEquals(Set.of("C3H7NO", "H2O"), formulas(reaction.products()));
    }

    @Test
    void aCarbonylAndTwoAlcoholsAreAnAcetal() {
        Reaction reaction = best("CC=O", "CCO", "CCO");

        assertNotNull(reaction, "an aldehyde and two alcohols are an acetal");
        assertEquals(Set.of("C6H14O2", "H2O"), formulas(reaction.products()));
    }

    @Test
    void oneAlcoholIsNotEnoughForAnAcetal() {
        // The hemiacetal is what one alcohol gives: the rule for the acetal stands above it and answers
        // nothing at all when the second alcohol is not in the vessel.
        Reaction reaction = best("CC=O", "CCO");

        assertNotNull(reaction, "an aldehyde and one alcohol are a hemiacetal");
        assertEquals(Set.of("C4H10O2"), formulas(reaction.products()));
    }

    @Test
    void aSecondaryAmineTakesAKetoneOverIntoAnEnamine() {
        // Propanone and dimethylamine: the double bond comes out between the carbonyl carbon and the carbon
        // beside it, because an amine of two carbons has no hydrogen left to lose off its nitrogen.
        Reaction reaction = best("CC(=O)C", "CNC");

        assertNotNull(reaction, "a ketone and an amine of two carbons are an enamine");
        assertEquals(Set.of("C5H11N", "H2O"), formulas(reaction.products()));
    }

    @Test
    void aRuleThatNeedsAFlameDoesNotRunAtRoomTemperature() {
        // An alcohol loses its water over a flame; the same vessel on a bench is another answer entirely.
        Reaction overAFlame = best(Warmth.HEATED, "CCO");

        assertNotNull(overAFlame, "ethanol over a flame loses water");
        assertEquals(Set.of("C2H4", "H2O"), formulas(overAFlame.products()));

        Reaction standingThere = best(Warmth.AMBIENT, "CCO");
        assertNull(standingThere, "and on a bench at room temperature it does nothing at all");
    }

    @Test
    void aRuleThatNeedsAMetalDoesNotRunWithoutIt() {
        // Ethene and hydrogen: over nickel they are an alkane, over copper they stand as they are.
        Reaction overNickel = best(Warmth.AMBIENT, Set.of(Chemical.parse("[Ni]")), "C=C", "[H][H]");

        assertNotNull(overNickel, "hydrogen is added over a metal");
        assertEquals(Set.of("C2H6"), formulas(overNickel.products()));

        Reaction overCopper = best(Warmth.AMBIENT, Set.of(Chemical.parse("[Cu]")), "C=C", "[H][H]");
        assertNull(overCopper, "and copper is not the metal for it");
    }

    @Test
    void aRouteWrittenForWaterDoesNotRunInSomethingElse() {
        // Ethyl acetate is hydrolysed in water and not in an alcohol, which would only put it back.
        Reaction inWater = best(Warmth.HEATED, Chemical.parse("O"), "CCOC(C)=O", "O");

        assertNotNull(inWater, "ethyl acetate in water over a flame");
        assertEquals(Set.of("C2H4O2", "C2H6O"), formulas(inWater.products()));

        Reaction inEthanol = best(Warmth.HEATED, Chemical.parse("CCO"), "CCOC(C)=O", "O");
        assertNull(inEthanol, "and ethanol is not water");
    }

    /** The one reaction an engine would run in a vessel of the substances named by their strings. */
    private static Reaction best(String... smiles) {
        return best(Conditions.NONE, smiles);
    }

    /** The one reaction an engine would run at one of the three settings. */
    private static Reaction best(Warmth warmth, String... smiles) {
        return best(Conditions.at(warmth), smiles);
    }

    /** The one reaction an engine would run over the catalysts that stand in the vessel. */
    private static Reaction best(Warmth warmth, Set<Chemical> catalysts, String... smiles) {
        return best(Conditions.at(warmth, catalysts), smiles);
    }

    /** The one reaction an engine would run in a medium. */
    private static Reaction best(Warmth warmth, Chemical medium, String... smiles) {
        return best(Conditions.at(warmth, medium), smiles);
    }

    /** The one reaction an engine would run in a vessel of the conditions and the substances named. */
    private static Reaction best(Conditions conditions, String... smiles) {
        Mixture pot = Mixture.empty();
        for (String one : smiles) {
            pot = pot.plus(Mixture.of(Chemical.parse(one), 1));
        }
        return ENGINE.best(System.of(pot, Phase.LIQUID).with(conditions));
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

    /** How far apart two atoms of a ring stand, counted the short way round it. */
    private static int ringDistance(List<Integer> ring, int first, int second) {
        int apart = Math.abs(ring.indexOf(first) - ring.indexOf(second));
        return Math.min(apart, ring.size() - apart);
    }

    /** The carbon of a ring that carries a group holding a named element. */
    private static int ringCarbonCarrying(Molecule molecule, List<Integer> ring, String element) {
        for (int atom : ring) {
            if (!molecule.atom(atom).element().equals("C")) {
                continue;
            }
            for (int neighbour : molecule.neighbours(atom)) {
                if (!ring.contains(neighbour) && molecule.atom(neighbour).element().equals(element)) {
                    return atom;
                }
            }
        }
        return -1;
    }
}
