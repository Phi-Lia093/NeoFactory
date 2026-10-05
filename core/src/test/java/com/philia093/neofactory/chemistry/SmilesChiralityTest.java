package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a string really hands its chirality to the molecule it names.
 * <p>
 * The {@code @} of a SMILES string is not a mark of its own: it says which way round the neighbours were
 * listed, so a reader that keeps the mark and throws the listing away has kept nothing at all. This asks
 * for both - the mark on the atom and the order of the neighbours - and asks it on a chain, on the two
 * mirror strings of alanine, and on a centre that sits in a ring, where the neighbour a ring digit stands
 * for belongs where the digit was written and not at the end of the list.
 */
class SmilesChiralityTest {

    @Test
    void aBracketAtomKeepsTheChiralityItWasWrittenWith() {
        Molecule alanine = SmilesParser.parse("N[C@@H](C)C(=O)O");
        Atom centre = alanine.atom(1);

        assertTrue(centre.isChiral(), "the carbon was written as a stereocentre");
        assertEquals(Atom.CHIRAL_TWO, centre.chirality(), "two at signs were written");
        assertEquals(1, centre.hydrogens(), "and it carries the hydrogen the string spelled out");
    }

    @Test
    void theTwoWaysAReWrittenAsTheTwoMarks() {
        assertEquals(Atom.CHIRAL_ONE, SmilesParser.parse("N[C@H](C)C(=O)O").atom(1).chirality());
        assertEquals(Atom.CHIRAL_TWO, SmilesParser.parse("N[C@@H](C)C(=O)O").atom(1).chirality());
    }

    @Test
    void theNeighboursAreKeptInTheOrderTheyWereWritten() {
        // N - C(H) - (C) - C(=O)O: the carbon came from the nitrogen, then the methyl, then the acid.
        Molecule alanine = SmilesParser.parse("N[C@@H](C)C(=O)O");

        assertArrayEquals(new int[] {0, 2, 3}, alanine.atom(1).writtenNeighbours());
    }

    @Test
    void aRingBondTakesThePlaceItsDigitWasWrittenAt() {
        // N - C(H) - ring: the ring digit stands before the chain, so the ring neighbour is listed second.
        Molecule ring = SmilesParser.parse("N[C@@H]1CCCC1");

        assertArrayEquals(new int[] {0, 5, 2}, ring.atom(1).writtenNeighbours());
    }

    @Test
    void anAtomThatIsNoCentreCarriesNothing() {
        Molecule ethanol = SmilesParser.parse("CCO");

        assertFalse(ethanol.atom(1).isChiral());
        assertEquals(0, ethanol.atom(1).writtenNeighbours().length);
    }
}
