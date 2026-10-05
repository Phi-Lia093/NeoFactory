package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the reader that turns a SMILES string into a molecule.
 * <p>
 * The strings are checked by what they must turn into and not by the atoms in the order they were written:
 * a chain is checked by its count of atoms and bonds and, above all, by the formula that follows once the
 * hydrogens are filled in, which is what catches a wrong valence rule as surely as a wrong parse. The
 * strings that cannot be read are checked as well - an empty one, a bracket never closed, a ring never
 * closed, a branch never closed, a symbol that names no element - because a parser that accepts a broken
 * string is a parser that builds a wrong species, and the module refuses those outright.
 */
class SmilesParserTest {

    @Test
    void aChainIsReadWithItsHydrogens() {
        Molecule ethanol = SmilesParser.parse("CCO");

        assertEquals(3, ethanol.atomCount());
        assertEquals(2, ethanol.bonds().size());
        assertEquals("C2H6O", ethanol.composition().formula());
        assertEquals(0, ethanol.charge());
    }

    @Test
    void aBareAtomIsFilledFromItsValence() {
        assertEquals("CH4", SmilesParser.parse("C").composition().formula());
        assertEquals("H3N", SmilesParser.parse("N").composition().formula());
        assertEquals("H2O", SmilesParser.parse("O").composition().formula());
        assertEquals("O2", SmilesParser.parse("O=O").composition().formula());
    }

    @Test
    void aBracketAtomCarriesWhatItSpellsOut() {
        Molecule sodium = SmilesParser.parse("[Na+]");
        assertEquals("Na", sodium.composition().formula());
        assertEquals(1, sodium.charge());
        assertEquals(0, sodium.atom(0).hydrogens(), "a metal ion is handed no hydrogen");

        assertEquals("Cl", SmilesParser.parse("[Cl-]").composition().formula());
        assertEquals(-1, SmilesParser.parse("[Cl-]").charge());

        assertEquals("H4N", SmilesParser.parse("[NH4+]").composition().formula(), "four hydrogens");
        assertEquals("HO", SmilesParser.parse("[OH-]").composition().formula(), "one hydrogen");
        assertEquals(2, SmilesParser.parse("[Cu+2]").charge());
    }

    @Test
    void chlorineAndBromineAreTwoLetterSymbols() {
        Molecule chloromethane = SmilesParser.parse("CCl");

        assertEquals(2, chloromethane.atomCount());
        assertEquals("Cl", chloromethane.atom(1).element());
        assertEquals("CH3Cl", chloromethane.composition().formula());
    }

    @Test
    void aBranchStepsAsideAndComesBack() {
        // Acetic acid: a methyl, a carbonyl and a hydroxyl on one carbon.
        Molecule acid = SmilesParser.parse("CC(=O)O");

        assertEquals(4, acid.atomCount());
        assertEquals(3, acid.bonds().size());
        assertEquals("C2H4O2", acid.composition().formula());
    }

    @Test
    void aRingNumberIsReadAndClosed() {
        Molecule cyclohexane = SmilesParser.parse("C1CCCCC1");

        assertEquals(6, cyclohexane.atomCount());
        assertEquals(6, cyclohexane.bonds().size());
        assertEquals("C6H12", cyclohexane.composition().formula());
        assertEquals(1, cyclohexane.componentCount());
    }

    @Test
    void aTwoDigitRingNumberIsRead() {
        Molecule ring = SmilesParser.parse("C%10CCCCCCCCCC%10");

        assertEquals(11, ring.atomCount());
        assertEquals(11, ring.bonds().size());
        assertEquals(1, ring.componentCount());
    }

    @Test
    void aDotMakesSeveralPieces() {
        Molecule salt = SmilesParser.parse("[Na+].[Cl-]");

        assertEquals(2, salt.atomCount());
        assertEquals(0, salt.bonds().size());
        assertEquals(2, salt.componentCount());
        assertFalse(salt.isSingleComponent());
    }

    @Test
    void aStringThatCannotBeReadIsRefused() {
        assertThrows(SmilesException.class, () -> SmilesParser.parse(""), "an empty string");
        assertThrows(SmilesException.class, () -> SmilesParser.parse("[Na+"), "a bracket never closed");
        assertThrows(SmilesException.class, () -> SmilesParser.parse("C1CC"), "a ring never closed");
        assertThrows(SmilesException.class, () -> SmilesParser.parse("(C"), "a branch never closed");
        assertThrows(SmilesException.class, () -> SmilesParser.parse("C%"), "a ring number cut short");
        assertThrows(SmilesException.class, () -> SmilesParser.parse("Q"), "a symbol that names nothing");
        assertThrows(SmilesException.class, () -> SmilesParser.parse("[Xx]"), "an unknown element");
        assertThrows(SmilesException.class, () -> SmilesParser.parse(".C"), "a dot before any atom");
    }

    @Test
    void aLongChainIsReadWithoutOverflowingTheStack() {
        StringBuilder text = new StringBuilder("C");
        for (int index = 0; index < 2000; index++) {
            text.append("CC");
        }

        Molecule chain = SmilesParser.parse(text.toString());

        assertEquals(1 + 2 * 2000, chain.atomCount());
        assertTrue(chain.isSingleComponent());
    }
}
