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
        // The branch has walked to the end of the chain, and the one double bond of the product that has room
        // to settle settles trans: a Cope runs through a chair and the chain folds on one face of it.
        Molecule product = only(reaction.products()).structure();
        String skeleton = SmilesWriter.write(product).replace("/", "").replace("\\", "");
        assertEquals(Chemical.parse("C=CCCC=CC").structure().canonicalKey(),
                SmilesParser.parse(skeleton).canonicalKey(), "the branch has walked to the end of the chain");
        assertEquals('E', descriptorOf(product), "and the double bond that could settle settles trans");
    }

    @Test
    void aShortChainIsLeftAlone() {
        // An allyl group on its own is no sigmatropic chain: the six atoms a shift runs along are not there.
        assertEquals(null, best(Warmth.AMBIENT, "C=CCC(C)(C)C"),
                "a chain too short is no sigmatropic chain");
    }

    @Test
    void aRingClosedBetweenTwoFlatCentresComesOutWithAHandOnEachOfThem() {
        // A Diels-Alder of butadiene with propene: the dienophile carbon that carries the methyl comes out a
        // stereocentre, because the diene came in on one face of it and not the other.
        Reaction adduct = best("C=CC=C", "C=CC");
        assertNotNull(adduct, "butadiene and propene");
        assertTrue(chiralCentres(only(adduct.products()).structure()) >= 1,
                "the junction the diene attacked came out with a defined hand");

        // A chain of three double bonds closing on itself: the two ends become centres too, and the closing
        // is disrotatory, so both are written from one face of the vessel.
        Reaction ring = best("CC=CC=CC=C");
        assertNotNull(ring, "a substituted triene closing");
        assertTrue(chiralCentres(only(ring.products()).structure()) >= 1,
                "the ends of the chain came out with a hand as well");
    }

    /** How many atoms of a molecule are written as stereocentres. */
    private static int chiralCentres(Molecule molecule) {
        int centres = 0;
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            if (molecule.atom(atom).isChiral()) {
                centres++;
            }
        }
        return centres;
    }

    /** The configuration a molecule's double bond reads, or {@code 0} when it reads none. */
    private static char descriptorOf(Molecule molecule) {
        for (Bond bond : molecule.bonds()) {
            char descriptor = Cip.descriptor(molecule, bond);
            if (descriptor != 0) {
                return descriptor;
            }
        }
        return 0;
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
