package com.philia093.neofactory.chemistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

/**
 * Finds the aromatic rings of a molecule and marks them, so that two ways of writing one ring meet.
 * <p>
 * Benzene is written two ways. The one the industry prefers is the aromatic one, {@code c1ccccc1}, where
 * the six carbons are written in the lower case that says "these six share a blurred pair of electrons".
 * The other is the Kekulé one, {@code C1=CC=CC=C1}, where the same ring is drawn with three plain and three
 * double bonds that keep swapping places. Both are benzene; a module that stored them as two different
 * things would carry the same substance twice and balance a reaction against the wrong one, so exactly one
 * of them may survive into a molecule and this class is what makes that happen.
 * <p>
 * <b>Only the rings that are understood are touched.</b> The full rule for aromaticity counts the shared
 * electrons of a whole conjugated system, which for a ring joined to another one - naphthalene, anthracene -
 * is the system and not the ring; that rule is deliberately not attempted here. This class handles a single
 * ring of the common elements, which is benzene, pyridine, pyrrole, furan and thiophene and the rings the
 * catalog is built from today, and it leaves every other ring as it found it. A ring it does not understand
 * stays non-aromatic rather than being guessed at, because a guessed ring is a wrong species.
 * <p>
 * <b>A lower-case ring is taken at its word, a Kekulé one is counted.</b> A ring whose atoms were written
 * in the lower case is already aromatic and this class only makes sure its bonds say so; a ring drawn as an
 * all-carbon Kekulé ring is aromatic when it alternates single and double bonds around a size of four n plus
 * two, which is the benzene count. A ring that is neither - a cyclohexene with one double bond, a
 * cyclobutadiene with two across four atoms - is left alone.
 */
public final class Aromatizer {

    /** The elements a ring this class is willing to call aromatic may be built from. */
    private static final Set<String> RING_ELEMENTS = Set.of("B", "C", "N", "O", "P", "S");

    private Aromatizer() {
        // Utility class: never instantiated.
    }

    /**
     * Marks the aromatic rings of a molecule, called while the molecule is assembled.
     * <p>
     * The atoms and the bonds are written in place, which is why this runs before the {@link Molecule}
     * fills in the hydrogens: an aromatic carbon holds one hydrogen and an alkane carbon four, and the
     * count can only be right once this has decided which of the two every ring atom is.
     *
     * @param atoms atoms of the molecule, in the order the string named them
     * @param bonds bonds of the molecule, in the order the string drew them
     */
    public static void aromatize(List<Atom> atoms, List<Bond> bonds) {
        if (atoms.isEmpty() || bonds.isEmpty()) {
            return;
        }
        List<List<Integer>> bondOfAtom = buildAdjacency(atoms.size(), bonds);
        List<Integer> ring = singleRing(atoms.size(), bonds, bondOfAtom);
        if (ring == null) {
            return;
        }
        perceive(atoms, bonds, bondOfAtom, ring);
    }

    /** Indices of the bonds that end at every atom. */
    private static List<List<Integer>> buildAdjacency(int atomCount, List<Bond> bonds) {
        List<List<Integer>> adjacency = new ArrayList<>(atomCount);
        for (int index = 0; index < atomCount; index++) {
            adjacency.add(new ArrayList<>());
        }
        for (int index = 0; index < bonds.size(); index++) {
            Bond bond = bonds.get(index);
            adjacency.get(bond.first()).add(index);
            adjacency.get(bond.second()).add(index);
        }
        return adjacency;
    }

    /**
     * The atoms of the one simple ring of a molecule, or {@code null} when it has none or more than one.
     * <p>
     * The atoms that lie in no ring are peeled away one by one - an atom with a single bond cannot be part
     * of a ring, and once it is gone its neighbour may have become one too - and what is left is the ring
     * system. A system that is a single ring has every atom with exactly two bonds inside it, which is the
     * one shape this class reads; a fused or branched system fails that test and answers {@code null}, and
     * so does a molecule without a ring at all.
     *
     * @param atomCount amount of atoms
     * @param bonds bonds of the molecule
     * @param bondOfAtom adjacency
     * @return the ring atoms in the order they are joined, or {@code null}
     */
    private static List<Integer> singleRing(int atomCount, List<Bond> bonds,
            List<List<Integer>> bondOfAtom) {
        int[] degree = new int[atomCount];
        for (int index = 0; index < atomCount; index++) {
            degree[index] = bondOfAtom.get(index).size();
        }
        boolean[] peeled = new boolean[atomCount];
        Deque<Integer> leaves = new ArrayDeque<>();
        for (int index = 0; index < atomCount; index++) {
            if (degree[index] <= 1) {
                leaves.push(index);
            }
        }
        while (!leaves.isEmpty()) {
            int atom = leaves.pop();
            if (peeled[atom]) {
                continue;
            }
            peeled[atom] = true;
            for (int bondIndex : bondOfAtom.get(atom)) {
                int other = bonds.get(bondIndex).other(atom);
                if (!peeled[other] && --degree[other] <= 1) {
                    leaves.push(other);
                }
            }
        }
        List<Integer> ringAtoms = new ArrayList<>();
        for (int index = 0; index < atomCount; index++) {
            if (!peeled[index]) {
                ringAtoms.add(index);
            }
        }
        if (ringAtoms.isEmpty()) {
            return null;
        }
        for (int atom : ringAtoms) {
            int insideRing = 0;
            for (int bondIndex : bondOfAtom.get(atom)) {
                if (!peeled[bonds.get(bondIndex).other(atom)]) {
                    insideRing++;
                }
            }
            if (insideRing != 2) {
                return null;
            }
        }
        List<Integer> ring = walkRing(ringAtoms.get(0), bonds, bondOfAtom, peeled);
        if (ring == null || ring.size() != ringAtoms.size()) {
            // More than one ring, or a ring the walk could not close: not a shape this class reads.
            return null;
        }
        return ring;
    }

    /** Walks the single ring once, in the order its atoms are joined. */
    private static List<Integer> walkRing(int start, List<Bond> bonds,
            List<List<Integer>> bondOfAtom, boolean[] peeled) {
        List<Integer> ring = new ArrayList<>();
        int previous = -1;
        int current = start;
        while (true) {
            ring.add(current);
            int next = -1;
            for (int bondIndex : bondOfAtom.get(current)) {
                int other = bonds.get(bondIndex).other(current);
                if (!peeled[other] && other != previous) {
                    next = other;
                    break;
                }
            }
            if (next == -1 || next == start) {
                break;
            }
            previous = current;
            current = next;
        }
        return ring.size() < 3 ? null : ring;
    }

    /**
     * Decides whether the one ring of a molecule is aromatic, and marks it if it is.
     * <p>
     * A ring written in the lower case of an aromatic atom is taken at its word; a ring drawn as an
     * all-carbon Kekulé ring is aromatic when the double bonds and the plain ones alternate around a size
     * of four n plus two. Every other ring - a ring of an element this class does not read, a ring of a
     * size the rule does not fit, a ring whose double bonds stand next to each other - is left as it was,
     * see the class comment.
     *
     * @param atoms atoms of the molecule
     * @param bonds bonds of the molecule
     * @param bondOfAtom adjacency
     * @param ring the atoms of the ring, in the order they are joined
     */
    private static void perceive(List<Atom> atoms, List<Bond> bonds,
            List<List<Integer>> bondOfAtom, List<Integer> ring) {
        boolean[] inRing = new boolean[atoms.size()];
        for (int atom : ring) {
            inRing[atom] = true;
            if (!RING_ELEMENTS.contains(atoms.get(atom).element())) {
                return;
            }
        }
        List<Integer> ringBonds = new ArrayList<>();
        for (int index = 0; index < bonds.size(); index++) {
            Bond bond = bonds.get(index);
            if (inRing[bond.first()] && inRing[bond.second()]) {
                ringBonds.add(index);
            }
        }
        if (everyAtomAromatic(atoms, ring)) {
            markAromatic(atoms, bonds, ring, ringBonds);
            return;
        }
        if (!isKekuleAromatic(atoms, bonds, bondOfAtom, ring, ringBonds, inRing)) {
            return;
        }
        markAromatic(atoms, bonds, ring, ringBonds);
    }

    /** {@code true} when every atom of the ring was written in the lower case of an aromatic atom. */
    private static boolean everyAtomAromatic(List<Atom> atoms, List<Integer> ring) {
        for (int atom : ring) {
            if (!atoms.get(atom).isAromatic()) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@code true} when an all-carbon ring is written as an alternating Kekulé ring of a size that fits.
     * <p>
     * The size has to be four n plus two, the count of shared electrons an aromatic ring holds, and the
     * double bonds have to be half of the ring and never two on one atom, which is what alternation means.
     * A ring that fails either test - a cyclohexene, a cyclobutadiene - is no aromatic ring.
     */
    private static boolean isKekuleAromatic(List<Atom> atoms, List<Bond> bonds,
            List<List<Integer>> bondOfAtom, List<Integer> ring, List<Integer> ringBonds,
            boolean[] inRing) {
        for (int atom : ring) {
            if (!atoms.get(atom).element().equals("C")) {
                return false;
            }
        }
        int size = ring.size();
        if (size % 4 != 2) {
            return false;
        }
        int doubles = 0;
        for (int bondIndex : ringBonds) {
            if (bonds.get(bondIndex).order() == 2) {
                doubles++;
            }
        }
        if (doubles != size / 2) {
            return false;
        }
        for (int atom : ring) {
            int doubleHere = 0;
            for (int bondIndex : bondOfAtom.get(atom)) {
                if (inRing[bonds.get(bondIndex).other(atom)]
                        && bonds.get(bondIndex).order() == 2) {
                    doubleHere++;
                }
            }
            if (doubleHere > 1) {
                return false;
            }
        }
        return true;
    }

    /** Marks every atom and every bond of a ring as aromatic. */
    private static void markAromatic(List<Atom> atoms, List<Bond> bonds, List<Integer> ring,
            List<Integer> ringBonds) {
        for (int atom : ring) {
            atoms.get(atom).markAromatic();
        }
        for (int bondIndex : ringBonds) {
            bonds.get(bondIndex).makeAromatic();
        }
    }
}
