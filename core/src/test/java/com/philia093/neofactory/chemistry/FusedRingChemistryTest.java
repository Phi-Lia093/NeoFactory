package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the rings that are more than one ring: the fused systems a drawing of a fibre and a medicine is
 * full of, and the second way any ring may be written.
 * <p>
 * A ring used to be read only when a molecule held exactly one of them, so a naphthalene was left with no
 * aromatic ring at all once it had been drawn, redrawn and read again - and a ring written the Kekulé way was
 * read as an arrangement of plain and double bonds that happened to sit in a circle. What is asked here is
 * that every ring of a molecule is read on its own, fused ones included, that both ways of writing a ring
 * come out the same, and that the rings are still aromatic after a reaction has been run on them.
 */
class FusedRingChemistryTest {

    /** The engine over the rules the organic side ships with. */
    private static final PolarEngine ENGINE = new PolarEngine(PolarReactions.all());

    /** The acid a nitration is run over. */
    private static final Conditions NITRATION = Conditions.builder()
            .warmth(Warmth.HEATED).catalyst(Chemical.parse("OS(=O)(=O)O")).build();

    @Test
    void everyRingOfAFusedSystemIsReadOnItsOwn() {
        assertEquals(2, Sites.aromaticRings(materialize("c1ccc2ccccc2c1")).size(),
                "both hexagons of a naphthalene are aromatic");
        assertEquals(3, Sites.aromaticRings(materialize("c1ccc2cc3ccccc3cc2c1")).size(),
                "and all three of an anthracene");
        assertEquals(2, Sites.aromaticRings(materialize("c1ccc2[nH]ccc2c1")).size(),
                "and both of an indole, one of them holding a nitrogen");
    }

    @Test
    void aRingWrittenTheKekuleWayIsTheSameRing() {
        assertEquals(2, Sites.aromaticRings(materialize("C1=CC2=CC=CC=C2C=C1")).size(),
                "a naphthalene drawn with plain and double bonds is aromatic too");
        assertEquals(1, Sites.aromaticRings(materialize("C1=CC=NC=C1")).size(),
                "and so is a pyridine, whose ring holds a nitrogen");
    }

    @Test
    void theKekuleOfAFusedRingIsOneThatCouldExist() {
        for (String smiles : new String[] {"c1ccc2ccccc2c1", "c1ccc2cc3ccccc3cc2c1",
                "c1ccc2c(c1)ccc1ccccc12"}) {
            Molecule kekule = Assemblies.kekulized(materialize(smiles));
            for (int atom = 0; atom < kekule.atomCount(); atom++) {
                assertTrue(doublesOn(kekule, atom) <= 1,
                        smiles + ": atom " + atom + " is left with two double bonds");
                if (kekule.atom(atom).element().equals("C")) {
                    assertEquals(1, doublesOn(kekule, atom),
                            smiles + ": every carbon of a Kekule ring takes exactly one double bond");
                }
            }
        }
    }

    @Test
    void aFusedRingIsSubstitutedAndComesBackAromatic() {
        Reaction reaction = best(NITRATION, "c1ccc2ccccc2c1", "O[N+](=O)[O-]", "OS(=O)(=O)O");

        assertNotNull(reaction, "naphthalene and nitric acid over the acid it is run in");
        assertEquals(Set.of("C10H7NO2", "H2O"), formulas(reaction.products()), "a nitronaphthalene");
        assertEquals(2, Sites.aromaticRings(onlyCarbonProduct(reaction).structure()).size(),
                "and both rings of it are aromatic again once the reaction is over");
    }

    @Test
    void aBondSharedBetweenTwoRingsIsNoGroupOfItsOwn() {
        // A methyl on a naphthalene sends the nitro group beside itself, which is a position of the ring it
        // hangs on - and the two carbons the rings share are no methyl and send nothing.
        Reaction reaction = best(NITRATION, "Cc1cccc2ccccc12", "O[N+](=O)[O-]", "OS(=O)(=O)O");

        assertNotNull(reaction, "a methylnaphthalene and nitric acid");
        assertEquals(Set.of("C11H9NO2", "H2O"), formulas(reaction.products()));
    }

    /** How many double bonds end at an atom. */
    private static int doublesOn(Molecule molecule, int atom) {
        int doubles = 0;
        for (int bondIndex : molecule.bondsOf(atom)) {
            if (molecule.bonds().get(bondIndex).order() == 2) {
                doubles++;
            }
        }
        return doubles;
    }

    /** The molecule a string names, as a vessel would hold it. */
    private static Molecule materialize(String smiles) {
        return Assemblies.materialize(Chemical.parse(smiles).structure());
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
