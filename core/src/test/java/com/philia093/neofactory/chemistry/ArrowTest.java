package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks that a pair of electrons pushed through a molecule leaves the right molecule and the right charges.
 * <p>
 * The mechanism asked for is the substitution every course begins with: a hydroxide comes in at the carbon
 * of methyl bromide and the bromide leaves, so the pot is asked for methanol and a bromide ion and the
 * charges are asked for one by one - the oxygen that gave its pair away is neutral, the carbon that lost a
 * bond gained the charge for it, and the bromide that kept the pair is negative. An arrow that names a bond
 * that is not there is refused, and an arrow is asked to keep the charge of the molecule it was drawn on,
 * which is the whole reason no arrow names a charge of its own.
 */
class ArrowTest {

    /** The pot of the substitution: a hydroxide ion beside methyl bromide, as three atoms and one bond. */
    private static final String POT = "[OH-].[CH3]Br";

    @Test
    void anSn2MechanismLeavesMethanolAndABromide() {
        Molecule pot = SmilesParser.parse(POT);

        Molecule product = Mechanism.of("substitution",
                Arrow.fromLonePair(0, 0, 1),
                Arrow.toLonePair(1, 2)).apply(pot);

        assertEquals(StereoKey.of(SmilesParser.parse("CO.[Br-]")), StereoKey.of(product),
                "a hydroxide and methyl bromide give methanol and a bromide");
        assertEquals(pot.charge(), product.charge(), "and the charge came out of it");
    }

    @Test
    void theBondThatIsMadeIsThereAndTheOneThatBreaksIsNot() {
        Molecule pot = SmilesParser.parse(POT);

        Molecule product = Mechanism.of("substitution",
                Arrow.fromLonePair(0, 0, 1),
                Arrow.toLonePair(1, 2)).apply(pot);

        assertNotNull(bond(product, 0, 1), "the oxygen holds on to the carbon now");
        assertNull(bond(product, 1, 2), "and the carbon let the bromide go");
    }

    @Test
    void oneArrowMovesTheChargeAndNeverMakesIt() {
        Molecule pot = SmilesParser.parse(POT);

        Molecule after = Arrow.fromLonePair(0, 0, 1).apply(pot);

        assertEquals(pot.charge(), after.charge(), "the charge of the whole molecule did not move");
        assertEquals(0, after.atom(0).charge(), "the oxygen that gave its pair away is neutral");
        assertEquals(-1, after.atom(1).charge(), "and the carbon that took the bond is not");
    }

    @Test
    void anArrowThatBreaksABondThatIsNotThereIsRefused() {
        Molecule twoIons = SmilesParser.parse("[Na+].[Cl-]");

        assertThrows(IllegalStateException.class, () -> Arrow.toLonePair(0, 1).apply(twoIons));
    }

    @Test
    void aMechanismWithoutAnArrowIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> Mechanism.of("nothing"));
    }

    @Test
    void aBondThatSplitsEvenlyLeavesARadicalOnEachOfItsAtoms() {
        // A molecule of bromine under a light, which is how a radical chain is started.
        Molecule molecule = SmilesParser.parse("[Br][Br]");

        Molecule product = Arrow.homolysis(0, 1).apply(molecule);

        assertNull(bond(product, 0, 1), "the bond is gone");
        assertEquals(1, product.atom(0).radicals(), "and each atom kept one of its electrons");
        assertEquals(1, product.atom(1).radicals());
        assertEquals(0, product.charge(), "with no charge made on either of them");
    }

    @Test
    void twoRadicalsPairUpIntoABond() {
        Molecule molecule = SmilesParser.parse("[CH3].[CH3]");

        Molecule product = Mechanism.of("radicalCoupling", Arrow.couple(0, 1)).apply(molecule);

        assertNotNull(bond(product, 0, 1), "the two carbons are held together now");
        assertEquals(0, product.atom(0).radicals(), "and neither is a radical any more");
        assertEquals(0, product.atom(1).radicals());
        assertEquals(0, product.charge());
    }

    @Test
    void anUnpairedElectronAndHalfOfADoubleBondMakeABondAndLeaveOneBehind() {
        // A methyl radical and ethene, as the two single-electron arrows the addition is drawn with.
        Molecule molecule = SmilesParser.parse("[CH3].C=C");

        Molecule product = Mechanism.of("radicalAddition",
                Arrow.fromRadical(0, 0, 1),
                Arrow.toRadical(1, 2, 2)).apply(molecule);

        assertNotNull(bond(product, 0, 1), "the radical holds the carbon of the double bond now");
        assertEquals(0, product.atom(0).radicals(), "and is a radical no more");
        assertEquals(1, product.atom(2).radicals(), "the far carbon is the one left a radical");
        assertEquals(0, product.charge(), "and no charge was made");
    }

    @Test
    void anArrowThatSpendsARadicalThatIsNotThereIsRefused() {
        Molecule closedShell = SmilesParser.parse("C=C");

        assertThrows(IllegalStateException.class, () -> Arrow.fromRadical(0, 0, 1).apply(closedShell));
    }

    /** The bond between two atoms, or {@code null} when they share none. */
    private static Bond bond(Molecule molecule, int first, int second) {
        for (int bondIndex : molecule.bondsOf(first)) {
            Bond bond = molecule.bonds().get(bondIndex);
            if (bond.other(first) == second) {
                return bond;
            }
        }
        return null;
    }
}
