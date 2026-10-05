package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the rules of priority against molecules whose configuration is known chemistry and not an opinion.
 * <p>
 * The anchors are the two hands of alanine, which the industry has called S and R for a century, and the two
 * ways of drawing a double bond, which the language itself fixes: the same mark twice is the E of trans
 * difluoroethene and two marks that point apart are the Z of the cis one. What the rules are asked for on top
 * of that is that they are a rule and not a table - a centre and its mirror have to come out opposite, and a
 * molecule with no mark at all has to come back with no configuration.
 */
class CipTest {

    @Test
    void theTwoHandsOfAlanineAreSAndR() {
        assertEquals('S', Cip.configuration(SmilesParser.parse("N[C@@H](C)C(=O)O"), 1),
                "L-alanine");
        assertEquals('R', Cip.configuration(SmilesParser.parse("N[C@H](C)C(=O)O"), 1),
                "D-alanine");
    }

    @Test
    void aCentreAndItsMirrorComeOutOpposite() {
        Molecule left = SmilesParser.parse("[C@H](F)(Cl)Br");
        Molecule right = SmilesParser.parse("[C@@H](F)(Cl)Br");

        char one = Cip.configuration(left, 0);
        char other = Cip.configuration(right, 0);
        assertTrue(one == 'R' || one == 'S', "the carbon of bromochlorofluoromethane is a centre");
        assertNotEquals(one, other, "the mirror of a centre is the other configuration");
    }

    @Test
    void aMoleculeWithoutAMarkHasNoConfiguration() {
        // A nitrogen of an amine is no stereocentre, and neither is a carbon whose four ligands are alike.
        assertEquals(0, Cip.configuration(SmilesParser.parse("NCC"), 0), "a nitrogen is no centre");
        assertEquals(0, Cip.configuration(SmilesParser.parse("N[C@H](C)C(=O)O"), 0),
                "the nitrogen beside the centre carries no mark");
        assertEquals(0, Cip.configuration(SmilesParser.parse("C[C@](C)(C)C"), 1),
                "an atom whose ligands are alike is no centre");
    }

    @Test
    void theSameMarkTwiceIsEAndTwoMarksApartIsZ() {
        assertEquals('E', descriptor("F/C=C/F"), "E-difluoroethene");
        assertEquals('Z', descriptor("F/C=C\\F"), "Z-difluoroethene");
        assertEquals('E', descriptor("C/C=C/C"), "trans-2-butene");
        assertEquals('Z', descriptor("C/C=C\\C"), "cis-2-butene");
    }

    @Test
    void aDoubleBondWithoutMarksHasNoConfiguration() {
        assertEquals(0, descriptor("CC=CC"), "a double bond nobody marked is no configuration");
    }

    /** The configuration of the double bond of a molecule written with one. */
    private static char descriptor(String smiles) {
        Molecule molecule = SmilesParser.parse(smiles);
        for (Bond bond : molecule.bonds()) {
            char descriptor = Cip.descriptor(molecule, bond);
            if (descriptor != 0) {
                return descriptor;
            }
        }
        return 0;
    }
}
