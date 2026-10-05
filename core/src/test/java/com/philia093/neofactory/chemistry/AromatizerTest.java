package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the aromatic perception, the one place two writings of a ring have to become one molecule.
 * <p>
 * The rule that carries the stage is the one under test first: the aromatic benzene and the Kekulé benzene
 * have to settle on the same molecule and the same key, or the same substance would be carried twice and a
 * reaction would be balanced against whichever of the two was written first. The rest of the tests pin the
 * rings this stage is willing to call aromatic and, just as important, the rings it is not.
 */
class AromatizerTest {

    @Test
    void theTwoWaysOfWritingBenzeneAreOneMolecule() {
        Molecule aromatic = SmilesParser.parse("c1ccccc1");
        Molecule kekule = SmilesParser.parse("C1=CC=CC=C1");

        assertEquals("C6H6", aromatic.composition().formula());
        assertEquals("C6H6", kekule.composition().formula());
        assertEquals(aromatic.canonicalKey(), kekule.canonicalKey(), "one substance, one key");
        assertEquals(aromatic, kekule);
    }

    @Test
    void aRingOfTheCommonElementsIsAromatic() {
        assertEquals("C6H6", SmilesParser.parse("c1ccccc1").composition().formula(), "benzene");
        assertEquals("C5H5N", SmilesParser.parse("c1ccncc1").composition().formula(), "pyridine");
        assertEquals("C4H5N", SmilesParser.parse("c1cc[nH]c1").composition().formula(), "pyrrole");
        assertEquals("C4H4O", SmilesParser.parse("c1ccoc1").composition().formula(), "furan");
        assertEquals("C4H4S", SmilesParser.parse("c1ccsc1").composition().formula(), "thiophene");
    }

    @Test
    void aRingThatIsNoAromaticRingIsLeftAlone() {
        // A ring of plain bonds, and a ring with a single double one, are neither of them aromatic.
        assertEquals("C6H12", SmilesParser.parse("C1CCCCC1").composition().formula(), "cyclohexane");
        assertEquals("C6H10", SmilesParser.parse("C1=CCCCC1").composition().formula(), "cyclohexene");
    }

    @Test
    void anAromaticRingKeepsTheHydrogensOfItsOwnModel() {
        // A ring carbon of benzene holds one hydrogen, not the four of an alkane carbon.
        Molecule benzene = SmilesParser.parse("c1ccccc1");

        assertTrue(benzene.atom(0).isAromatic());
        assertEquals(1, benzene.atom(0).hydrogens());
        assertTrue(benzene.bonds().get(0).isAromatic(), "the bonds of the ring say so as well");
    }
}
