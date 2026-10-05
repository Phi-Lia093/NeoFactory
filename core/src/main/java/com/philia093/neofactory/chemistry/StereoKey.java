package com.philia093.neofactory.chemistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A key that tells two stereoisomers apart, where the plain key of a molecule cannot.
 * <p>
 * The canonical key of a molecule is the smallest writing of its graph, and a graph has no hands: the two
 * mirrors of lactic acid have the very same key, because every atom is bonded to the very same atoms. A
 * module that is to reason about configuration has to be able to say that they are two substances, so this
 * key is the plain one with the configuration of every stereocentre added to it.
 * <p>
 * <b>The configuration is written down together with what stands around it.</b> An {@code R} on its own says
 * nothing - two molecules may each have one R centre and still differ - so every centre is written as its
 * configuration beside the branches that hang off it, each branch cut out of the molecule and labelled
 * canonically, and the same is done for the two ends of a stereo double bond. The descriptions are then put
 * in order, so that the key does not depend on which atom of the molecule happened to be numbered first.
 * <p>
 * <b>A molecule with no configuration keeps the key it always had.</b> A molecule that is neither chiral nor
 * has stereo double bonds comes back with the plain canonical key exactly, so nothing that was written
 * before this exists - the whole catalog of substances, every stored world - changes its identity.
 */
public final class StereoKey {

    private StereoKey() {
        // Utility class: never instantiated.
    }

    /**
     * The key of a molecule, its configuration included.
     *
     * @param molecule molecule to label
     * @return the key, the plain key exactly when the molecule has no configuration
     */
    public static String of(Molecule molecule) {
        List<String> descriptors = new ArrayList<>();
        for (int atom : Cip.tetrahedral(molecule)) {
            char configuration = Cip.configuration(molecule, atom);
            if (configuration != 0) {
                descriptors.add(configuration + ":" + centreKey(molecule, atom));
            }
        }
        for (Bond bond : molecule.bonds()) {
            char descriptor = Cip.descriptor(molecule, bond);
            if (descriptor != 0) {
                descriptors.add(descriptor + ":" + doubleBondKey(molecule, bond));
            }
        }
        if (descriptors.isEmpty()) {
            return molecule.canonicalKey();
        }
        Collections.sort(descriptors);
        return molecule.canonicalKey() + "|" + String.join(";", descriptors);
    }

    /** What stands around a centre, the four branches cut out and put in order. */
    private static String centreKey(Molecule molecule, int centre) {
        List<String> branches = new ArrayList<>();
        for (int ligand : Cip.ligands(molecule, centre)) {
            branches.add(ligand == Cip.HYDROGEN ? "H" : cut(molecule, centre, ligand).canonicalKey());
        }
        Collections.sort(branches);
        return String.join(",", branches);
    }

    /** A double bond, each end written as the branches that hang off it, the two ends put in order. */
    private static String doubleBondKey(Molecule molecule, Bond bond) {
        List<String> ends = new ArrayList<>();
        ends.add(endsOf(molecule, bond.first(), bond.second()));
        ends.add(endsOf(molecule, bond.second(), bond.first()));
        Collections.sort(ends);
        return ends.get(0) + "=" + ends.get(1);
    }

    /** The branches that hang off one end of a double bond, the end it is joined to left out. */
    private static String endsOf(Molecule molecule, int end, int other) {
        List<String> branches = new ArrayList<>();
        for (int neighbour : molecule.neighbours(end)) {
            if (neighbour != other) {
                branches.add(cut(molecule, end, neighbour).canonicalKey());
            }
        }
        for (int count = 0; count < molecule.atom(end).hydrogens(); count++) {
            branches.add("H");
        }
        Collections.sort(branches);
        return String.join(",", branches);
    }

    /**
     * One branch of a molecule cut out, with the atom it hangs on left behind.
     *
     * @param molecule molecule to cut
     * @param centre atom the branch hangs on, left out of the piece
     * @param start atom the branch starts at
     * @return the branch as a molecule of its own
     */
    private static Molecule cut(Molecule molecule, int centre, int start) {
        boolean[] mine = new boolean[molecule.atomCount()];
        Deque<Integer> pending = new ArrayDeque<>();
        mine[start] = true;
        pending.push(start);
        while (!pending.isEmpty()) {
            int atom = pending.pop();
            for (int neighbour : molecule.neighbours(atom)) {
                if (neighbour != centre && !mine[neighbour]) {
                    mine[neighbour] = true;
                    pending.push(neighbour);
                }
            }
        }
        Map<Integer, Integer> renumbered = new HashMap<>();
        List<Atom> atoms = new ArrayList<>();
        for (int atom = 0; atom < mine.length; atom++) {
            if (mine[atom]) {
                renumbered.put(atom, atoms.size());
                atoms.add(molecule.atom(atom));
            }
        }
        List<Bond> bonds = new ArrayList<>();
        for (Bond bond : molecule.bonds()) {
            Integer first = renumbered.get(bond.first());
            Integer second = renumbered.get(bond.second());
            if (first != null && second != null) {
                bonds.add(bond.isAromatic() ? Bond.aromatic(first, second)
                        : Bond.of(first, second, bond.order()));
            }
        }
        return new Molecule(atoms, bonds);
    }
}
