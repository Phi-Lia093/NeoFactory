package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that two stereoisomers are two substances and not one.
 * <p>
 * The plain key of a molecule is the smallest writing of its graph, and a graph has no hands, so the two
 * mirrors of alanine share it and the two ways of drawing butene share it as well; the stereo key has to be
 * the one that tells them apart. It is asked in the other direction too: a molecule that carries no
 * configuration at all keeps the plain key exactly, which is what keeps every substance of the catalog and
 * every stored world its own substance. Above all, a configuration has to survive being written out and read
 * back in, which is the one path a reaction and a screen both take.
 */
class StereoKeyTest {

    @Test
    void theTwoHandsOfAlanineAreTwoSubstances() {
        Molecule left = SmilesParser.parse("N[C@@H](C)C(=O)O");
        Molecule right = SmilesParser.parse("N[C@H](C)C(=O)O");

        assertEquals(left.canonicalKey(), right.canonicalKey(), "the plain key cannot tell them apart");
        assertNotEquals(StereoKey.of(left), StereoKey.of(right), "the stereo key can");
    }

    @Test
    void theTwoWaysOfDrawingADoubleBondAreTwoSubstances() {
        Molecule trans = SmilesParser.parse("C/C=C/C");
        Molecule cis = SmilesParser.parse("C/C=C\\C");

        // The marks of a double bond already stand in the plain key - they are part of the writing of the
        // graph - while the handedness of a centre does not, which is what the stereo key is for.
        assertNotEquals(StereoKey.of(trans), StereoKey.of(cis), "the stereo key can tell them apart");
    }

    @Test
    void aMoleculeWithNoConfigurationKeepsThePlainKey() {
        for (String smiles : new String[] {"CCO", "c1ccccc1", "CC(=O)O", "NCC(=O)O", "C1CCCCC1"}) {
            Molecule molecule = SmilesParser.parse(smiles);

            assertEquals(molecule.canonicalKey(), StereoKey.of(molecule), smiles);
        }
    }

    @Test
    void aConfigurationSurvivesBeingWrittenAndReadAgain() {
        // The chirality of a centre is written by the writer; the marks of a double bond are not written yet,
        // so only the centres are asked here.
        for (String smiles : new String[] {"N[C@@H](C)C(=O)O", "N[C@H](C)C(=O)O", "[C@H](F)(Cl)Br",
                "[C@@H](F)(Cl)Br"}) {
            Molecule once = SmilesParser.parse(smiles);
            String written = SmilesWriter.write(once);
            Molecule twice = SmilesParser.parse(written);

            assertTrue(StereoKey.of(once).contains("R") || StereoKey.of(once).contains("S"),
                    smiles + " has a centre");
            assertEquals(StereoKey.of(once), StereoKey.of(twice), smiles + " came back as " + written);
        }
    }
}
