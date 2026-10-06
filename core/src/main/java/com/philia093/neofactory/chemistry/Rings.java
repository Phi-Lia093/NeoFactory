package com.philia093.neofactory.chemistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The pieces a molecule falls apart into and the rings it carries, read off nothing but its bonds.
 * <p>
 * A molecule is a graph and most questions a drawing asks about it are graph questions: a benzene ring
 * has to be drawn as a ring and not as a straight chain, and two molecules that were written with a dot
 * between them have to be drawn side by side and not laid on top of one another. Both answers are found
 * here and nowhere else, so that the layout of a molecule never has to look at the graph twice.
 * <p>
 * <b>A ring is found by walking around it.</b> The bond between two atoms is taken away and the shortest
 * path between its ends is looked for; a path that exists closes a ring with the bond that was taken away.
 * Every bond of a molecule is asked that way, and the cycles that come out twice are kept once.
 * <p>
 * <b>Only the smallest rings are kept.</b> A cage holds rings that are made of other rings - the outline of
 * naphthalene is a ring of ten atoms although the molecule is two hexagons - and a drawing that treated the
 * outline as a ring of its own would have a ring where a chemist sees two. The cycles are therefore put
 * through the smallest set of smallest rings: they are taken from the shortest upwards and a cycle that can
 * be written as the sum of the ones already taken is dropped, which is what leaves exactly the rings the
 * molecule is really made of.
 */
public final class Rings {

    private Rings() {
        // Utility class: never instantiated.
    }

    /**
     * The connected pieces of a molecule, each one a sorted list of atom indices.
     * <p>
     * One piece for a molecule whose atoms are all joined by bonds, more for a string that names several
     * pieces with a dot between them. The order of the pieces follows the order their lowest atom was
     * written in, so a layout is deterministic.
     *
     * @param molecule molecule to take apart
     * @return the pieces, each one sorted
     */
    public static List<List<Integer>> components(Molecule molecule) {
        Objects.requireNonNull(molecule, "molecule");
        int count = molecule.atomCount();
        boolean[] seen = new boolean[count];
        List<List<Integer>> pieces = new ArrayList<>();
        for (int start = 0; start < count; start++) {
            if (seen[start]) {
                continue;
            }
            List<Integer> piece = new ArrayList<>();
            Deque<Integer> pending = new ArrayDeque<>();
            pending.push(start);
            seen[start] = true;
            while (!pending.isEmpty()) {
                int atom = pending.pop();
                piece.add(atom);
                for (int neighbour : molecule.neighbours(atom)) {
                    if (!seen[neighbour]) {
                        seen[neighbour] = true;
                        pending.push(neighbour);
                    }
                }
            }
            Collections.sort(piece);
            pieces.add(piece);
        }
        return pieces;
    }

    /**
     * The smallest rings of a molecule, each one the atoms of a ring in the order they are joined.
     * <p>
     * The rings come back shortest first, so a caller that draws them one after the other lays the tightest
     * ring of a molecule down before the ones built around it. An acyclic molecule has no ring at all.
     *
     * @param molecule molecule to look for rings in
     * @return the rings, each one an ordered list of atoms, shortest first
     */
    public static List<List<Integer>> cycles(Molecule molecule) {
        Objects.requireNonNull(molecule, "molecule");
        List<Bond> bonds = molecule.bonds();
        Map<Long, Integer> indexOfBond = new HashMap<>();
        for (int index = 0; index < bonds.size(); index++) {
            indexOfBond.put(pairKey(bonds.get(index).first(), bonds.get(index).second()), index);
        }

        List<List<Integer>> candidates = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < bonds.size(); index++) {
            Bond bond = bonds.get(index);
            List<Integer> path = shortestPath(molecule, bond.first(), bond.second(), index);
            if (path == null || path.size() < 3) {
                continue;
            }
            if (seen.add(ringKey(path))) {
                candidates.add(path);
            }
        }
        candidates.sort((first, second) -> Integer.compare(first.size(), second.size()));

        Map<Integer, BitSet> basis = new HashMap<>();
        List<List<Integer>> smallest = new ArrayList<>();
        for (List<Integer> cycle : candidates) {
            BitSet reduced = maskOf(cycle, indexOfBond);
            while (true) {
                int pivot = reduced.nextSetBit(0);
                if (pivot < 0) {
                    break;
                }
                BitSet row = basis.get(pivot);
                if (row == null) {
                    basis.put(pivot, reduced);
                    smallest.add(cycle);
                    break;
                }
                reduced.xor(row);
            }
        }
        return smallest;
    }

    /**
     * How many bonds apart two atoms of a molecule stand, counted the short way.
     * <p>
     * A rule that joins two places of one molecule closes a ring, and a ring is only a ring at all if it is
     * big enough: what is asked here is how far the two places are from one another, so that a rule may
     * refuse to close a ring of three and wait for two places that are four or five bonds apart.
     *
     * @param molecule molecule to walk
     * @param first index of one atom
     * @param second index of the other
     * @return the number of bonds on the shortest path, {@code -1} when no path joins them
     */
    public static int distance(Molecule molecule, int first, int second) {
        Objects.requireNonNull(molecule, "molecule");
        if (first == second) {
            return 0;
        }
        List<Integer> path = shortestPath(molecule, first, second, -1);
        return path == null ? -1 : path.size() - 1;
    }

    /**
     * The shortest path between two atoms that does not walk over one bond.
     *
     * @param molecule molecule to walk
     * @param from atom the path starts at
     * @param to atom the path ends at
     * @param skipBond bond that may not be used
     * @return the atoms of the path, both ends included, or {@code null} when no path exists
     */
    private static List<Integer> shortestPath(Molecule molecule, int from, int to, int skipBond) {
        int count = molecule.atomCount();
        boolean[] seen = new boolean[count];
        int[] parent = new int[count];
        Arrays.fill(parent, -1);
        Deque<Integer> pending = new ArrayDeque<>();
        pending.add(from);
        seen[from] = true;
        boolean found = false;
        while (!pending.isEmpty()) {
            int atom = pending.poll();
            if (atom == to) {
                found = true;
                break;
            }
            for (int bondIndex : molecule.bondsOf(atom)) {
                if (bondIndex == skipBond) {
                    continue;
                }
                int neighbour = molecule.bonds().get(bondIndex).other(atom);
                if (!seen[neighbour]) {
                    seen[neighbour] = true;
                    parent[neighbour] = atom;
                    pending.add(neighbour);
                }
            }
        }
        if (!found) {
            return null;
        }
        List<Integer> path = new ArrayList<>();
        for (int atom = to; atom != -1; atom = parent[atom]) {
            path.add(atom);
        }
        Collections.reverse(path);
        return path;
    }

    /** The bonds a cycle is made of, as a set of bond indices. */
    private static BitSet maskOf(List<Integer> cycle, Map<Long, Integer> indexOfBond) {
        BitSet mask = new BitSet();
        int size = cycle.size();
        for (int index = 0; index < size; index++) {
            Integer bond = indexOfBond.get(pairKey(cycle.get(index), cycle.get((index + 1) % size)));
            if (bond != null) {
                mask.set(bond);
            }
        }
        return mask;
    }

    /** A key by which a cycle is recognised however it was walked. */
    private static String ringKey(List<Integer> atoms) {
        List<Integer> sorted = new ArrayList<>(atoms);
        Collections.sort(sorted);
        return sorted.toString();
    }

    /** A key of an unordered pair of atoms, for looking a bond up. */
    private static long pairKey(int first, int second) {
        int low = Math.min(first, second);
        int high = Math.max(first, second);
        return ((long) low << 32) | (high & 0xffffffffL);
    }
}
