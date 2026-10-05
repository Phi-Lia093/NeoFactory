package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Checks the provisional key the catalog of the next stage matches on.
 * <p>
 * The key has to answer two questions and both are asked here, because one of them alone proves nothing.
 * Two strings that name the same molecule must settle on the same key - the equivalence - or a substance
 * written two ways would be carried twice; and two strings that name different molecules must settle on
 * different keys - the difference - or two substances would be folded into one. A key that answered every
 * string with the same value would pass every test of the first sort and fail every one of the second,
 * which is why the two are checked side by side and not the equivalence alone.
 */
class ProvisionalCanonicalizerTest {

    @Test
    void theSameMoleculeWrittenTwoWaysHasTheSameKey() {
        assertSameKey("C1=CC=CC=C1", "c1ccccc1", "benzene as a Kekulé ring and as an aromatic one");
        assertSameKey("CCO", "OCC", "ethanol read from either end");
        assertSameKey("CC(=O)O", "OC(=O)C", "acetic acid read from either end");
        assertSameKey("c1ccccc1C", "Cc1ccccc1", "toluene read from either atom");
        assertSameKey("c1ccncc1", "c1cccnc1", "pyridine read from another atom of its ring");
    }

    @Test
    void moleculesThatAreNotTheSameHaveDifferentKeys() {
        assertDifferentKey("CCO", "COC", "ethanol against dimethyl ether, the same elements");
        assertDifferentKey("Cc1ccccc1C", "Cc1cccc(C)c1", "ortho against meta xylene");
        assertDifferentKey("C/C=C/C", "C/C=C\\C", "a trans double bond against a cis one");
        assertDifferentKey("[13C]", "[12C]", "a heavier carbon against an ordinary one");
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
