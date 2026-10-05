package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the two steps the organic side is built on, structure and configuration at once.
 * <p>
 * A substitution is asked for two things: that the nucleophile took the place of the leaving group, and that
 * the carbon came out turned over - which is asked as the difference between the step and the very same
 * arrows with the mark left alone, so that the answer is about the inversion and not about the order the
 * ligands happen to be listed in. An addition is asked for the other fact: that the two faces of a flat
 * centre are the two hands of the centre that comes out of it.
 */
class ElementaryStepTest {

    /** A hydroxide ion beside bromochlorofluoromethane: the oxygen, the carbon, then its three ligands. */
    private static final String POT = "[OH-].[C@H](Br)(F)Cl";

    @Test
    void aSubstitutionPutsTheNucleophileWhereTheLeavingGroupWas() {
        Molecule product = ElementaryStep.substitution("hydrolysis", 0, 1, 2).apply(SmilesParser.parse(POT));

        assertNotNull(bond(product, 1, 0), "the oxygen holds on to the carbon now");
        assertNull(bond(product, 1, 2), "and the bromide is gone");
        assertEquals(-1, product.atom(2).charge(), "the bromide left with the pair");
        assertEquals(0, product.atom(0).charge(), "and the oxygen is neutral again");
    }

    @Test
    void aSubstitutionTurnsTheCarbonOver() {
        Molecule pot = SmilesParser.parse(POT);
        Molecule arrowsOnly = Arrow.toLonePair(1, 2)
                .apply(Arrow.fromLonePair(0, 0, 1).apply(pot));
        Molecule product = ElementaryStep.substitution("hydrolysis", 0, 1, 2).apply(pot);

        char withoutTurn = Cip.configuration(arrowsOnly, 1);
        char withTurn = Cip.configuration(product, 1);

        assertTrue(withoutTurn == 'R' || withoutTurn == 'S', "the carbon is a centre to begin with");
        assertNotEquals(withoutTurn, withTurn,
                "a nucleophile that comes in from behind turns the carbon over");
    }

    @Test
    void anAdditionDecidesTheHandOfTheNewCentreByItsFace() {
        // Cyanide adds to the carbonyl of acetaldehyde; the two faces are the two hands.
        Molecule fromRe = ElementaryStep.addition("cyanohydrin", 0, 3, 4, Face.RE)
                .apply(SmilesParser.parse("[C-]#N.CC=O"));
        Molecule fromSi = ElementaryStep.addition("cyanohydrin", 0, 3, 4, Face.SI)
                .apply(SmilesParser.parse("[C-]#N.CC=O"));

        char re = Cip.configuration(fromRe, 3);
        char si = Cip.configuration(fromSi, 3);

        assertTrue(re == 'R' || re == 'S', "the carbon came out a centre");
        assertTrue(si == 'R' || si == 'S', "and so did the other one");
        assertNotEquals(re, si, "the two faces are the two hands");
    }

    @Test
    void anAdditionMakesTheBondItWasDrawnWith() {
        Molecule product = ElementaryStep.addition("cyanohydrin", 0, 3, 4, Face.RE)
                .apply(SmilesParser.parse("[C-]#N.CC=O"));

        assertNotNull(bond(product, 0, 3), "the cyanide holds on to the carbon now");
        assertEquals(1, bond(product, 3, 4).order(), "the carbonyl is a plain bond now");
        assertEquals(-1, product.atom(4).charge(), "and the oxygen took the pair");
    }

    @Test
    void aStepCarriesTheArrowsItIsDrawnWith() {
        assertEquals(2, ElementaryStep.substitution("hydrolysis", 0, 1, 2).arrows().size(),
                "a substitution is two arrows");
        assertEquals(2, ElementaryStep.addition("cyanohydrin", 0, 3, 4, Face.RE).arrows().size(),
                "and so is an addition");
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
