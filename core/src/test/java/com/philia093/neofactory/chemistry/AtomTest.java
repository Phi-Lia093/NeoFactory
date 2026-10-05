package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what an atom carries beyond its element: where it stands in space and how many unpaired electrons
 * it holds.
 * <p>
 * The two facts are the ones a reaction is judged on and they are easy to lose: an ordinary atom written
 * without brackets has neither, a bracketed one keeps exactly the two it spelled out, and the answer has to
 * survive into the molecule, where the unpaired electrons come off the valence and leave the atom one
 * hydrogen fewer than its element would otherwise take. What is refused is asked as well - a third way to
 * write a stereocentre, a negative number of electrons - because an atom that quietly accepts those is an
 * atom that quietly builds a species nobody named.
 */
class AtomTest {

    @Test
    void anOrdinaryAtomCarriesNoChiralityAndNoRadical() {
        Atom carbon = Atom.bare("C", false);

        assertEquals(Atom.NO_CHIRALITY, carbon.chirality());
        assertFalse(carbon.isChiral());
        assertEquals(0, carbon.radicals());
        assertFalse(carbon.isRadical());
    }

    @Test
    void aBracketedAtomKeepsTheChiralityAndTheRadicalItWasWrittenWith() {
        Atom centre = Atom.bracketed("C", 0, 0, false, 1, 0, Atom.CHIRAL_TWO, 0);
        assertTrue(centre.isChiral());
        assertEquals(Atom.CHIRAL_TWO, centre.chirality());
        assertFalse(centre.isRadical());

        Atom radical = Atom.bracketed("C", 0, 0, false, 3, 0, Atom.NO_CHIRALITY, 1);
        assertTrue(radical.isRadical());
        assertEquals(1, radical.radicals());
        assertFalse(radical.isChiral());
    }

    @Test
    void anAtomThatMakesNoSenseIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> Atom.bracketed("C", 0, 0, false, 1, 0, 3, 0),
                "there are only two ways to write a tetrahedral atom");
        assertThrows(IllegalArgumentException.class,
                () -> Atom.bracketed("C", 0, 0, false, 1, 0, Atom.NO_CHIRALITY, -1),
                "an atom holds no negative electrons");
    }

    @Test
    void aRebuiltAtomCarriesItsRadicalIntoTheHydrogenCount() {
        // An ethyl radical: the carbon that lost the hydrogen holds two of them and one unpaired electron,
        // and the carbon beside it is an ordinary one holding three.
        Molecule radical = new Molecule(
                List.of(Atom.rebuilt("C", 0, 0, false, 0, Atom.NO_CHIRALITY, 1), Atom.bare("C", false)),
                List.of(Bond.of(0, 1, 1)));

        assertEquals(2, radical.atom(0).hydrogens(), "the radical carbon holds one hydrogen fewer");
        assertEquals(3, radical.atom(1).hydrogens(), "the carbon beside it is an ordinary one");
        assertEquals("C2H5", radical.composition().formula());
    }
}
