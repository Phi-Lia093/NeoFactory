package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Several molecules laid out as one, so that something may happen between them.
 * <p>
 * A vessel holds substances and not a molecule: an acid and an alcohol are two things that stand beside each
 * other until one of them gives the other a hydrogen. A reaction between two of them is therefore carried
 * out on the two laid out as one molecule - the atoms numbered one after the other, the bonds moved along
 * with them, everything else about every atom exactly as it stood - and what comes out of the move is read
 * back as the substances it turned out to be, see {@link Pot}.
 * <p>
 * <b>The atoms are copied and never taken.</b> What stands in a vessel is what a player put there and may
 * be read again by the next question asked about it, so the laid out pot is made of copies; and the copy
 * keeps the hydrogens of the atom it came from, because a molecule that is laid out is not a molecule that
 * was worked out again, see {@link Atom#copied}.
 */
public final class Assemblies {

    private Assemblies() {
        // Utility class: never instantiated.
    }

    /**
     * Lays several molecules out as one, one after the other.
     *
     * @param molecules molecules to lay out
     * @return the whole of them as one molecule, whose atoms are numbered by the order they were listed in
     */
    public static Molecule join(List<Molecule> molecules) {
        List<Atom> atoms = new ArrayList<>();
        List<Bond> bonds = new ArrayList<>();
        for (Molecule molecule : molecules) {
            int offset = atoms.size();
            for (int atom = 0; atom < molecule.atomCount(); atom++) {
                Atom copy = Atom.copied(molecule.atom(atom));
                copy.markWrittenOrder(shift(molecule.atom(atom).writtenNeighbours(), offset));
                atoms.add(copy);
            }
            for (Bond bond : molecule.bonds()) {
                bonds.add(move(bond, offset));
            }
        }
        return new Molecule(atoms, bonds);
    }

    /**
     * Writes out every hydrogen a molecule leaves to its valence, so that an arrow may name one.
     * <p>
     * An arrow moves a pair of electrons from an atom to an atom, and a hydrogen left to the valence of its
     * neighbour is not an atom at all: there is nothing for an arrow to point at, and a step that has to take
     * a hydrogen off a carbon - the elimination of an alcohol, the proton of an aldol - could not be drawn for
     * the commonest molecule there is. Every hydrogen is therefore written out as an atom of its own while a
     * vessel is laid out, and the atoms of the molecule that stood there keep their numbers, so that what a
     * rule names still means what it said.
     *
     * @param molecule molecule to write the hydrogens of
     * @return the same molecule with every hydrogen an atom of its own
     */
    public static Molecule materialize(Molecule molecule) {
        int count = molecule.atomCount();
        List<Atom> atoms = new ArrayList<>(count);
        for (int atom = 0; atom < count; atom++) {
            Atom value = molecule.atom(atom);
            // The atom is written between brackets with no hydrogen of its own, so the ones written out are
            // the only ones it has and nothing is counted twice.
            atoms.add(Atom.bracketed(value.element(), value.charge(), value.isotope(), value.isAromatic(), 0,
                    value.mapClass(), value.chirality(), value.radicals()));
        }
        List<Bond> bonds = new ArrayList<>(molecule.bonds());
        for (int atom = 0; atom < count; atom++) {
            Atom original = molecule.atom(atom);
            List<Integer> order = new ArrayList<>();
            for (int neighbour : original.writtenNeighbours()) {
                order.add(neighbour);
            }
            for (int index = 0; index < original.hydrogens(); index++) {
                int hydrogen = atoms.size();
                atoms.add(Atom.bracketed("H", 0, 0, false, 0, 0, Atom.NO_CHIRALITY, 0));
                bonds.add(Bond.of(atom, hydrogen, 1));
                order.add(hydrogen);
            }
            atoms.get(atom).markWrittenOrder(toIntArray(order));
        }
        return new Molecule(atoms, bonds);
    }

    /** {@code true} when an atom is a hydrogen that hangs on exactly one atom that is no hydrogen itself. */
    private static boolean isLooseHydrogen(Molecule molecule, int atom) {
        Atom value = molecule.atom(atom);
        if (!value.element().equals("H") || value.charge() != 0 || value.isotope() != 0
                || value.isRadical() || molecule.neighbours(atom).size() != 1) {
            return false;
        }
        return !molecule.atom(molecule.neighbours(atom).get(0)).element().equals("H");
    }

    /** An ordered list of atom indices as a plain array. */
    private static int[] toIntArray(List<Integer> values) {
        int[] array = new int[values.size()];
        for (int index = 0; index < array.length; index++) {
            array[index] = values.get(index);
        }
        return array;
    }

    /**
     * Folds every hydrogen that hangs on a single atom back into the count of that atom.
     * <p>
     * What a rule hands over is read back as the substances it turned out to be, and a substance is written
     * the way the catalog writes it: an alcohol is {@code CCO} and not {@code C([H])([H])C([H])([H])O[H]},
     * and the two are the same substance only if their hydrogens are counted the same way. Folding one back
     * moves the ligands of a centre about, though, so the hand of every centre is read <em>before</em> the
     * folding and written again after it: a centre comes out with the hand it had, see {@link Cip#markFor}.
     *
     * @param molecule molecule to fold the hydrogens of
     * @return the same molecule with its hydrogens left to their valence
     */
    public static Molecule fold(Molecule molecule) {
        int count = molecule.atomCount();
        Map<Integer, Character> hands = new HashMap<>();
        for (int atom : Cip.tetrahedral(molecule)) {
            char configuration = Cip.configuration(molecule, atom);
            if (configuration != 0) {
                hands.put(atom, configuration);
            }
        }
        boolean[] loose = new boolean[count];
        int[] absorbed = new int[count];
        for (int atom = 0; atom < count; atom++) {
            if (isLooseHydrogen(molecule, atom)) {
                loose[atom] = true;
                absorbed[molecule.neighbours(atom).get(0)]++;
            }
        }
        Map<Integer, Integer> renumbered = new HashMap<>();
        List<Atom> atoms = new ArrayList<>();
        for (int atom = 0; atom < count; atom++) {
            if (loose[atom]) {
                continue;
            }
            renumbered.put(atom, atoms.size());
            Atom value = molecule.atom(atom);
            atoms.add(Atom.bracketed(value.element(), value.charge(), value.isotope(), value.isAromatic(),
                    value.hydrogens() + absorbed[atom], value.mapClass(), value.chirality(),
                    value.radicals()));
        }
        List<Bond> bonds = new ArrayList<>();
        for (Bond bond : molecule.bonds()) {
            Integer first = renumbered.get(bond.first());
            Integer second = renumbered.get(bond.second());
            if (first == null || second == null) {
                continue;
            }
            bonds.add(bond.isAromatic() ? Bond.aromatic(first, second)
                    : Bond.of(first, second, bond.order(), bond.stereo()));
        }
        for (int atom = 0; atom < count; atom++) {
            if (loose[atom]) {
                continue;
            }
            List<Integer> order = new ArrayList<>();
            for (int neighbour : molecule.atom(atom).writtenNeighbours()) {
                Integer kept = renumbered.get(neighbour);
                if (kept != null) {
                    order.add(kept);
                }
            }
            atoms.get(renumbered.get(atom)).markWrittenOrder(toIntArray(order));
        }
        Molecule folded = new Molecule(atoms, bonds);
        for (Map.Entry<Integer, Character> hand : hands.entrySet()) {
            Integer moved = renumbered.get(hand.getKey());
            if (moved != null) {
                folded = Stereocentre.set(folded, moved, hand.getValue());
            }
        }
        return folded;
    }

    /** A bond of one molecule, moved to where that molecule stands in a laid out one. */
    private static Bond move(Bond bond, int offset) {
        if (bond.isAromatic()) {
            return Bond.aromatic(offset + bond.first(), offset + bond.second());
        }
        return Bond.of(offset + bond.first(), offset + bond.second(), bond.order(), bond.stereo());
    }

    /** The order an atom's neighbours were written in, moved along with the atom. */
    private static int[] shift(int[] order, int offset) {
        int[] moved = new int[order.length];
        for (int index = 0; index < order.length; index++) {
            moved[index] = order[index] < 0 ? order[index] : order[index] + offset;
        }
        return moved;
    }
}
