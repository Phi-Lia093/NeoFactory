package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.List;

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
