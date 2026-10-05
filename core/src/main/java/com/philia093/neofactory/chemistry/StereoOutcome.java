package com.philia093.neofactory.chemistry;

/**
 * What a step of a mechanism does to the configuration of an atom it touches.
 * <p>
 * The stereochemistry of a reaction is decided by the geometry of the step and not by luck: a nucleophile
 * that must come in from behind turns the carbon over, a carbocation that is left open loses whatever
 * configuration it had, and two atoms that are added to the two ends of a double bond are added from the
 * same side or from opposite sides. Those are the outcomes a rule of a reaction names, and this is the name.
 * <p>
 * <b>Only the two outcomes of one atom are applied by the module today.</b> An inversion turns the chirality
 * of the atom over and a racemisation drops it, and both are the work of {@link Stereocentre}; the outcomes
 * that are about two atoms at once - the syn and the anti of an addition, the configuration of a bond that is
 * made - are named here and await the rules that know which of the two atoms stands where.
 */
public enum StereoOutcome {

    /** The atom keeps the configuration it had, the way a substitution that runs twice over does not. */
    RETAIN,

    /** The atom is turned over, the way a nucleophile that attacks from behind turns it. */
    INVERT,

    /** The atom loses its configuration, the way a centre left open as a carbocation does. */
    RACEMIZE,

    /** Two atoms are added from the same side, the way a hydrogenation adds its two hydrogens. */
    SYN,

    /** Two atoms are added from opposite sides, the way a bromine adds its two bromines. */
    ANTI,

    /** Nothing is said about the configuration. */
    NONE;

    /**
     * The chirality this outcome leaves on an atom that carried one.
     *
     * @param current the chirality the atom carries, one of the two ways or none
     * @return the chirality the atom keeps
     */
    public int chirality(int current) {
        if (this == INVERT) {
            if (current == Atom.CHIRAL_ONE) {
                return Atom.CHIRAL_TWO;
            }
            return current == Atom.CHIRAL_TWO ? Atom.CHIRAL_ONE : Atom.NO_CHIRALITY;
        }
        if (this == RACEMIZE) {
            return Atom.NO_CHIRALITY;
        }
        return current;
    }
}
