package com.philia093.neofactory.chemistry;

/**
 * Which side of a flat centre a ligand comes in from.
 * <p>
 * An atom that is joined by a double bond is flat: its three ligands stand in one plane, and a fourth ligand
 * coming towards it finds two sides, one above the plane and one below. The two are not the same side and a
 * reaction that adds to a double bond gives the one or the other, so a rule of a reaction says which, and
 * this is the name it says. Which of the two is which is settled by the rules of priority: looking at the
 * flat centre with its three ligands in the plane, the side from which they read clockwise, highest priority
 * first, is the one called Re.
 */
public enum Face {

    /** The face from which the ligands of a flat centre read clockwise in the order of their priority. */
    RE,

    /** The other face, from which the same three read anticlockwise. */
    SI
}
