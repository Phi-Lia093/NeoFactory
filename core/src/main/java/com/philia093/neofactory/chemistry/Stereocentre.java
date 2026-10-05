package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * What a step of a mechanism leaves of a centre: the atom turned over, or turned into nothing at all.
 * <p>
 * A step that breaks a bond at a stereocentre and makes another one there - the whole of a substitution, most
 * of an addition, every rearrangement - does not leave the atom where it found it, and the rule of the step
 * says which way round it comes out. That is a fact about the atom and not about a screen: it is the atom's
 * own record of its two ways round that is turned over, so a molecule that had a left hand comes back with a
 * right one and everything else about it is untouched.
 * <p>
 * <b>The neighbours are not moved and the order they were listed in is not changed.</b> An inversion is
 * exactly the statement that swapping two ligands of a tetrahedral atom gives the other hand, which here is
 * the same statement as turning the mark over, so the record of how the neighbours were written stays as it
 * was and the configuration that follows from it flips, see {@link Cip}.
 */
public final class Stereocentre {

    private Stereocentre() {
        // Utility class: never instantiated.
    }

    /**
     * Leaves the outcome of a step on one atom of a molecule.
     *
     * @param molecule molecule the step was carried out on
     * @param atom the atom the step touched
     * @param outcome what the step does to a configuration
     * @return the molecule with the outcome on that atom, a new value
     */
    public static Molecule apply(Molecule molecule, int atom, StereoOutcome outcome) {
        Objects.requireNonNull(molecule, "molecule");
        Objects.requireNonNull(outcome, "outcome");
        return withChirality(molecule, atom, outcome.chirality(molecule.atom(atom).chirality()));
    }

    /**
     * Turns an atom over, the same as {@link StereoOutcome#INVERT}.
     *
     * @param molecule molecule to turn an atom of
     * @param atom the atom to turn over
     * @return the molecule with the atom the other way round
     */
    public static Molecule invert(Molecule molecule, int atom) {
        return apply(molecule, atom, StereoOutcome.INVERT);
    }

    /**
     * Writes a hand on an atom, whatever it carried before.
     * <p>
     * An inversion is a statement about the atom and not about the molecule, but a step of a reaction that
     * builds a centre has no old hand to turn over: it knows only which one it means to make. This is that
     * step's way of saying so - the mark is worked out for the listing the molecule really has, so the hand
     * that comes out is the hand that was asked for, see {@link Cip#markFor}.
     *
     * @param molecule molecule to write on
     * @param atom the atom to write a hand on
     * @param configuration the hand wanted, {@code 'R'} or {@code 'S'}
     * @return the molecule with that hand on that atom
     */
    public static Molecule set(Molecule molecule, int atom, char configuration) {
        Objects.requireNonNull(molecule, "molecule");
        return withChirality(molecule, atom, Cip.markFor(molecule, atom, configuration));
    }

    /** Every atom of a molecule built again, one of them with a named chirality. */
    private static Molecule withChirality(Molecule molecule, int atom, int chirality) {
        List<Atom> atoms = new ArrayList<>(molecule.atomCount());
        for (int index = 0; index < molecule.atomCount(); index++) {
            Atom old = molecule.atom(index);
            int value = index == atom ? chirality : old.chirality();
            Atom fresh = Atom.rebuilt(old.element(), old.charge(), old.isotope(), old.isAromatic(),
                    old.mapClass(), value, old.radicals());
            fresh.markWrittenOrder(old.writtenNeighbours());
            atoms.add(fresh);
        }
        return new Molecule(atoms, new ArrayList<>(molecule.bonds()));
    }
}
