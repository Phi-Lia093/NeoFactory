package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the one substance the module is built around, and the line it draws against a mixture.
 * <p>
 * An element and a compound are told apart by their composition and not by their name, so oxygen written as
 * a molecule is still an element, and a hydroxide ion is still a compound. The line that matters most is
 * the last: a string of several pieces is a mixture and is refused, because a mixture that was quietly read
 * as a compound would balance as one thing while it is several, which is exactly the mistake an alloy like
 * bronze would draw the module into.
 */
class ChemicalTest {

    @Test
    void anElementAndACompoundAreToldApartByTheirComposition() {
        assertEquals(Chemical.Kind.ELEMENT, Chemical.parse("O=O").kind(), "oxygen is an element");
        assertEquals(Chemical.Kind.ELEMENT, Chemical.parse("[Fe]").kind(), "so is a bare iron atom");
        assertEquals(Chemical.Kind.COMPOUND, Chemical.parse("O").kind(), "water is a compound");
        assertEquals(Chemical.Kind.COMPOUND, Chemical.parse("[OH-]").kind(), "so is a hydroxide ion");
    }

    @Test
    void aChemicalCarriesItsStructureItsFormulaAndItsCharge() {
        Chemical hydroxide = Chemical.parse("[OH-]");

        // The hydrogen of a hydroxide is a count on the oxygen and not an atom of its own.
        assertEquals(1, hydroxide.structure().atomCount());
        assertEquals("HO", hydroxide.composition().formula());
        assertEquals(-1, hydroxide.charge());
        assertEquals("[OH-]", hydroxide.smiles());
    }

    @Test
    void twoWritingsOfOneSubstanceAreTheSameChemical() {
        assertEquals(Chemical.parse("c1ccccc1"), Chemical.parse("C1=CC=CC=C1"));
    }

    @Test
    void severalPiecesAreNoChemical() {
        SmilesException error = assertThrows(SmilesException.class, () -> Chemical.parse("[Na+].[Cl-]"));

        assertTrue(error.getMessage().contains("Mixture"), error.getMessage());
        // The molecule behind it is still readable, and it reports its two pieces.
        assertEquals(2, SmilesParser.parse("[Na+].[Cl-]").componentCount());
    }
}
