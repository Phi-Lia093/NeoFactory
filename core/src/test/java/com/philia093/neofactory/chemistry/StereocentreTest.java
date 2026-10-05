package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a step of a mechanism leaves of a centre.
 * <p>
 * An inversion is the statement that a centre and its mirror are one turn apart, so the molecule that comes
 * back has to be the other hand and not a molecule without one; a racemisation is the other statement, that
 * the centre is gone and what is left is the plain key; and an outcome that keeps the atom has to leave
 * everything exactly as it stood. The three together are what a rule of a reaction has to be able to say.
 */
class StereocentreTest {

    /** A carbon with four different ligands and a mark, the smallest centre there is. */
    private static final String CENTRE = "[C@H](F)(Cl)Br";

    @Test
    void anInversionTurnsACentreIntoItsMirror() {
        Molecule centre = SmilesParser.parse(CENTRE);
        char before = Cip.configuration(centre, 0);

        Molecule turned = Stereocentre.invert(centre, 0);
        char after = Cip.configuration(turned, 0);

        assertTrue(before == 'R' || before == 'S', "the carbon is a centre to begin with");
        assertNotEquals(before, after, "a centre and its inversion are the two hands");
        assertNotEquals(StereoKey.of(centre), StereoKey.of(turned),
                "and the two hands are two substances");
    }

    @Test
    void aRacemisationDropsTheConfiguration() {
        Molecule centre = SmilesParser.parse(CENTRE);

        Molecule racemic = Stereocentre.apply(centre, 0, StereoOutcome.RACEMIZE);

        assertEquals(0, Cip.configuration(racemic, 0), "the mark is gone");
        assertEquals(racemic.canonicalKey(), StereoKey.of(racemic),
                "a molecule without a configuration is the plain key again");
    }

    @Test
    void anAtomThatIsKeptIsLeftWhereItStood() {
        Molecule centre = SmilesParser.parse(CENTRE);

        Molecule kept = Stereocentre.apply(centre, 0, StereoOutcome.RETAIN);

        assertEquals(StereoKey.of(centre), StereoKey.of(kept), "nothing about the atom moved");
    }
}
