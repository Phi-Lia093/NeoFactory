package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.List;

/**
 * A polymer read as the one unit it is made of, so that a drawing of it needs to show that unit and a count.
 * <p>
 * A polymer of a vinyl monomer is a chain of one unit repeated - styrene gives {@code [-CH2-CH(Ph)-]} over
 * and over - and a chain of a thousand units is not a molecule this module may write down one atom at a time.
 * So a polymer is <b>written as one turn of its own chain</b>: the two ends of a single unit are joined to
 * one another, which closes it into a small ring that holds the same atoms a single unit holds and nothing
 * of the chain around it. That ring is one substance for every count - a polystyrene is a polystyrene whether
 * the factory ran the chain out to ten units or ten thousand - which is what makes it fit in a catalog and in
 * an inventory slot, see {@code Chemical}.
 * <p>
 * <b>What is recognised is the shape, and not a name.</b> A molecule is read as a polymer when it is one ring
 * of an even number of atoms whose atoms alternate - a plain carbon, then a carbon carrying one group - which
 * is the head of a vinyl unit joined to its own tail. A molecule of any other shape answers nothing and is
 * drawn as what it is.
 */
public final class Polymer {

    private Polymer() {
        // Utility class: never instantiated.
    }

    /**
     * The atoms of one turn of the chain, or {@code null} when the molecule is no polymer.
     * <p>
     * The unit is half of the ring - one plain carbon and the carbon that carries a group - together with
     * whatever hangs on them, which is the piece a drawing shows with a count beside it.
     *
     * @param molecule molecule to read
     * @return the atoms of one unit, or {@code null} when the molecule is no polymer written this way
     */
    public static List<Integer> repeatUnit(Molecule molecule) {
        for (List<Integer> ring : Rings.cycles(molecule)) {
            List<Integer> unit = unitOf(molecule, ring);
            if (unit != null) {
                return unit;
            }
        }
        return null;
    }

    /** The atoms of one turn of a chain, if the named ring is one, or {@code null} when it is not. */
    private static List<Integer> unitOf(Molecule molecule, List<Integer> ring) {
        int size = ring.size();
        if (size < 4 || size % 2 != 0) {
            return null;
        }
        boolean[] inRing = new boolean[molecule.atomCount()];
        for (int atom : ring) {
            inRing[atom] = true;
        }
        int[] groups = new int[size];
        for (int step = 0; step < size; step++) {
            int atom = ring.get(step);
            if (!molecule.atom(atom).element().equals("C")) {
                return null;
            }
            groups[step] = groupsOn(molecule, atom, inRing);
            if (groups[step] > 1) {
                return null;
            }
        }
        for (int step = 0; step < size; step++) {
            if (groups[step] == groups[(step + 1) % size]) {
                // Two carbons of one kind stand beside each other, so the ring is no head joined to a tail.
                return null;
            }
        }
        int start = 0;
        while (groups[start] != 0) {
            start++;
        }
        int plain = ring.get(start);
        int carrying = ring.get((start + 1) % size);
        List<Integer> unit = new ArrayList<>();
        unit.add(plain);
        unit.add(carrying);
        for (int neighbour : molecule.neighbours(carrying)) {
            if (!inRing[neighbour]) {
                unit.add(neighbour);
                for (int further : molecule.neighbours(neighbour)) {
                    if (!inRing[further] && further != carrying) {
                        unit.add(further);
                    }
                }
            }
        }
        return unit;
    }

    /** {@code true} when a molecule is a polymer written as one turn of its own chain. */
    public static boolean isPolymer(Molecule molecule) {
        return repeatUnit(molecule) != null;
    }

    /** How many atoms that are no hydrogen and lie outside the ring hang on an atom. */
    private static int groupsOn(Molecule molecule, int atom, boolean[] inRing) {
        int groups = 0;
        for (int neighbour : molecule.neighbours(atom)) {
            if (!inRing[neighbour] && !molecule.atom(neighbour).element().equals("H")) {
                groups++;
            }
        }
        return groups;
    }
}
