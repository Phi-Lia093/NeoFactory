package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Checks the canonical labeling the catalog of a later stage matches on.
 * <p>
 * The key has to answer two questions and both are asked here, because one of them alone proves nothing.
 * Two strings that name the same molecule must settle on the same key - the equivalence - or a substance
 * written two ways would be carried twice; and two strings that name different molecules must settle on
 * different keys - the difference - or two substances would be folded into one. A key that answered every
 * string with the same value would pass every test of the first sort and fail every one of the second,
 * which is why the two are checked side by side and not the equivalence alone. The symmetric molecules -
 * benzene read from any atom, isobutane with its three equal methyls - are the ones a refinement alone
 * cannot settle, so they are the ones the individualisation is really tested by.
 */
class CanonicalizerTest {

    @Test
    void theSameMoleculeWrittenTwoWaysHasTheSameKey() {
        assertSameKey("C1=CC=CC=C1", "c1ccccc1", "benzene as a Kekulé ring and as an aromatic one");
        assertSameKey("CCO", "OCC", "ethanol read from either end");
        assertSameKey("CC(=O)O", "OC(=O)C", "acetic acid read from either end");
        assertSameKey("c1ccccc1C", "Cc1ccccc1", "toluene read from either atom");
        assertSameKey("c1ccncc1", "c1cccnc1", "pyridine read from another atom of its ring");
        assertSameKey("CC(C)C", "C(C)(C)C", "isobutane with three equal methyls");
    }

    @Test
    void moleculesThatAreNotTheSameHaveDifferentKeys() {
        assertDifferentKey("CCO", "COC", "ethanol against dimethyl ether, the same elements");
        assertDifferentKey("Cc1ccccc1C", "Cc1cccc(C)c1", "ortho against meta xylene");
        assertDifferentKey("C/C=C/C", "C/C=C\\C", "a trans double bond against a cis one");
        assertDifferentKey("[13C]", "[12C]", "a heavier carbon against an ordinary one");
        assertDifferentKey("C1CC1", "C1CCC1", "cyclopropane against cyclobutane");
        assertDifferentKey("C1=CC=CC=C1", "C1=CC=CCC1", "benzene against cyclohexadiene");
    }

    @Test
    void theKeyOfSeveralPiecesIgnoresTheOrderOfThePieces() {
        // A salt written the other way round is the same salt, so it has to reach the same key.
        assertSameKey("[Na+].[Cl-]", "[Cl-].[Na+]", "a salt written ion first or ion last");
    }

    @Test
    void aRingWithTwoGroupsOnItIsLabelledAndTheLabellingStops() {
        // A methyl beside a sulfonic acid on a ring: the refinement of the colours hands them out in a
        // different order from one round to the next, so the loop that waits for the colours to settle has to
        // ask whether any two atoms were pulled apart and never whether the numbers stopped moving - a loop
        // that waited for the numbers would wait for ever on this molecule and label nothing.
        assertSameKey("Cc1ccccc1S(=O)(=O)O", "O=S(=O)(O)c1ccccc1C", "toluenesulfonic acid read either way");
    }

    private static void assertSameKey(String first, String second, String what) {
        assertEquals(SmilesParser.parse(first).canonicalKey(), SmilesParser.parse(second).canonicalKey(),
                what + ": " + first + " and " + second + " name the same molecule");
    }

    private static void assertDifferentKey(String first, String second, String what) {
        assertNotEquals(SmilesParser.parse(first).canonicalKey(),
                SmilesParser.parse(second).canonicalKey(),
                what + ": " + first + " and " + second + " name different molecules");
    }
}
