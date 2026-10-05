package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a molecule survives a walk out into a string and a walk back in.
 * <p>
 * A graph has many strings and a writer picks one of them, so what is asked here is not a letter for letter
 * match but that the molecule is the same one: the key of a molecule read, written and read again has to be
 * the key it started with. String idempotence is asked as well - writing a molecule that was just written
 * has to come back with the very same string - which is what tells that a ring, a branch and the chirality
 * of a centre are all settled by the walk and not by the order the atoms happened to be read in.
 */
class SmilesRoundTripTest {

    /** Strings a walk has to survive, a chain, a ring, a fused pair, a branch and a stereocentre among them. */
    private static final String[] MOLECULES = {
        "CCO", "CC(=O)O", "C=C", "C#C", "c1ccccc1", "Cc1ccccc1", "C1CCCCC1", "c1ccc2ccccc2c1",
        "Oc1ccccc1", "CC(=O)OCC", "n1ccccc1", "NCC(=O)O", "N[C@@H](C)C(=O)O", "N[C@H](C)C(=O)O",
        "CC(C)(C)O", "O=C=O", "CCOC(=O)C"
    };

    /** Acyclic strings, whose walk is the whole graph and therefore settled by the walk alone. */
    private static final String[] CHAINS = {
        "CCO", "CC(=O)O", "C=C", "C#C", "CC(C)(C)O", "O=C=O", "CCOC(=O)C",
        "N[C@@H](C)C(=O)O", "N[C@H](C)C(=O)O", "NCC(=O)O"
    };

    @Test
    void aMoleculeReadWrittenAndReadAgainIsTheSameMolecule() {
        for (String smiles : MOLECULES) {
            Molecule once = SmilesParser.parse(smiles);
            String written = SmilesWriter.write(once);
            Molecule twice = SmilesParser.parse(written);

            assertEquals(once.canonicalKey(), twice.canonicalKey(), smiles + " came back as " + written);
        }
    }

    @Test
    void aChainIsWrittenTheSameWayTwice() {
        for (String smiles : CHAINS) {
            String once = SmilesWriter.write(SmilesParser.parse(smiles));
            String twice = SmilesWriter.write(SmilesParser.parse(once));

            assertEquals(once, twice, smiles + " is not written the same way twice");
        }
    }

    @Test
    void theMarksOfADoubleBondAreWrittenAndReadBack() {
        for (String smiles : new String[] {"C/C=C/C", "C/C=C\\C", "F/C=C/F", "F/C=C\\F"}) {
            Molecule once = SmilesParser.parse(smiles);
            String written = SmilesWriter.write(once);
            Molecule twice = SmilesParser.parse(written);

            assertTrue(written.contains("/") || written.contains("\\"),
                    smiles + " kept its marks in " + written);
            assertEquals(StereoKey.of(once), StereoKey.of(twice), smiles + " came back as " + written);
        }
    }

    @Test
    void theTwoMirrorsOfAlanineAreWrittenApart() {
        String left = SmilesWriter.write(SmilesParser.parse("N[C@@H](C)C(=O)O"));
        String right = SmilesWriter.write(SmilesParser.parse("N[C@H](C)C(=O)O"));

        assertTrue(left.contains("@"), "a stereocentre is written with its mark: " + left);
        assertNotEquals(left, right, "two enantiomers are not the same string");
        assertEquals(Atom.CHIRAL_TWO, SmilesParser.parse(left).atom(1).chirality(), left);
        assertEquals(Atom.CHIRAL_ONE, SmilesParser.parse(right).atom(1).chirality(), right);
    }
}
