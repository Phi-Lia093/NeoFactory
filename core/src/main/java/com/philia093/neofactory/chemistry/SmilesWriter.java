package com.philia093.neofactory.chemistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes a molecule back out as a SMILES string, the way a reader would have taken it in.
 * <p>
 * A molecule is a graph and a SMILES string is a walk over it, so writing is the act of finding a walk that
 * visits every atom and describes every bond: the bonds of the walk itself are written between the atoms,
 * and the bonds it steps over without walking - the closures of the rings - are written as a number at both
 * of their ends. The walk is the depth first walk of the graph, so a molecule written out and read back is
 * the same molecule and not a second one that happens to balance the same way.
 * <p>
 * <b>The chirality of a centre is worked out again for the walk that is written.</b> An {@code @} is not a
 * property of the atom but of the order its neighbours were written in, so a walk that lists them another
 * way needs the other mark: the mark of the atom that was read is kept together with the order it came with,
 * and the mark that is written is the one that describes the same arrangement under the new order. A walk
 * that cannot say what a centre is - a molecule whose centre was rebuilt without its neighbours - is written
 * without a mark rather than with a wrong one.
 * <p>
 * <b>What is written and what is not.</b> The elements of the organic subset are written bare when their
 * hydrogens follow from their bonds and the atom carries nothing else, and between brackets otherwise, with
 * the isotope, the charge and the hydrogens spelled out. The mark of a stereo double bond is not written
 * yet, and neither is an unpaired electron, which the language of the module has no letter for; a molecule
 * carrying those is written as its plain skeleton.
 */
public final class SmilesWriter {

    /** The ligand an implicit hydrogen stands for, in the order of a stereocentre. */
    private static final int HYDROGEN = -1;

    private SmilesWriter() {
        // Utility class: never instantiated.
    }

    /**
     * Writes a molecule as a SMILES string.
     *
     * @param molecule molecule to write
     * @return the string
     */
    public static String write(Molecule molecule) {
        StringBuilder out = new StringBuilder();
        List<List<Integer>> pieces = Rings.components(molecule);
        for (int index = 0; index < pieces.size(); index++) {
            if (index > 0) {
                out.append('.');
            }
            new Walk(molecule, pieces.get(index)).write(out);
        }
        return out.toString();
    }

    /** One depth first walk over one connected piece of a molecule, written as it goes. */
    private static final class Walk {

        private final Molecule molecule;
        private final List<Integer> piece;
        private final Map<Long, Integer> indexOfBond = new HashMap<>();
        private final boolean[] inWalk;
        private final int[] digit;
        private final int[] firstEnd;
        private final int[] parent;
        private final int[] visited;
        private final List<List<Integer>> children = new ArrayList<>();

        private Walk(Molecule molecule, List<Integer> piece) {
            this.molecule = molecule;
            this.piece = piece;
            int count = molecule.atomCount();
            List<Bond> bonds = molecule.bonds();
            this.inWalk = new boolean[bonds.size()];
            this.digit = new int[bonds.size()];
            this.firstEnd = new int[bonds.size()];
            Arrays.fill(this.firstEnd, -1);
            this.parent = new int[count];
            Arrays.fill(this.parent, -1);
            this.visited = new int[count];
            Arrays.fill(this.visited, -1);
            for (int atom = 0; atom < count; atom++) {
                children.add(new ArrayList<>());
            }
            for (int index = 0; index < bonds.size(); index++) {
                indexOfBond.put(pairKey(bonds.get(index).first(), bonds.get(index).second()), index);
            }
            walkTree();
            numberRings();
        }

        /** Finds the depth first walk of the piece and the order every atom is visited in. */
        private void walkTree() {
            Deque<Integer> pending = new ArrayDeque<>();
            int step = 0;
            visited[piece.get(0)] = -2;
            pending.push(piece.get(0));
            while (!pending.isEmpty()) {
                int atom = pending.pop();
                visited[atom] = step++;
                for (int bondIndex : molecule.bondsOf(atom)) {
                    int other = molecule.bonds().get(bondIndex).other(atom);
                    if (visited[other] != -1) {
                        continue;
                    }
                    visited[other] = -2;
                    inWalk[bondIndex] = true;
                    parent[other] = atom;
                    children.get(atom).add(other);
                    pending.push(other);
                }
            }
            for (List<Integer> kids : children) {
                Collections.sort(kids);
            }
        }

        /** Numbers the bonds the walk steps over, one number per ring closure. */
        private void numberRings() {
            int next = 1;
            for (int bondIndex = 0; bondIndex < inWalk.length; bondIndex++) {
                if (inWalk[bondIndex]) {
                    continue;
                }
                Bond bond = molecule.bonds().get(bondIndex);
                if (!piece.contains(bond.first())) {
                    continue;
                }
                digit[bondIndex] = next++;
                firstEnd[bondIndex] = visited[bond.first()] <= visited[bond.second()]
                        ? bond.first() : bond.second();
            }
        }

        /** Writes the piece, starting at its first atom. */
        private void write(StringBuilder out) {
            emit(piece.get(0), out);
        }

        /** Writes one atom, the rings it closes and the branches it opens. */
        private void emit(int atom, StringBuilder out) {
            writeAtom(out, atom);
            for (int bondIndex : molecule.bondsOf(atom)) {
                if (inWalk[bondIndex] || digit[bondIndex] == 0) {
                    continue;
                }
                if (firstEnd[bondIndex] == atom) {
                    writeBond(out, molecule.bonds().get(bondIndex));
                }
                out.append(digitText(digit[bondIndex]));
            }
            List<Integer> kids = children.get(atom);
            for (int index = 0; index < kids.size(); index++) {
                int child = kids.get(index);
                int bondIndex = indexOfBond.get(pairKey(atom, child));
                boolean last = index == kids.size() - 1;
                if (!last) {
                    out.append('(');
                }
                writeBond(out, molecule.bonds().get(bondIndex));
                emit(child, out);
                if (!last) {
                    out.append(')');
                }
            }
        }

        /** Writes one atom, bare when its hydrogens follow from its bonds and between brackets when not. */
        private void writeAtom(StringBuilder out, int atom) {
            Atom element = molecule.atom(atom);
            if (isBare(atom, element)) {
                out.append(symbol(atom));
                return;
            }
            out.append('[');
            if (element.isotope() > 0) {
                out.append(element.isotope());
            }
            out.append(symbol(atom));
            int mark = stereoMark(atom);
            if (mark == Atom.CHIRAL_ONE) {
                out.append('@');
            } else if (mark == Atom.CHIRAL_TWO) {
                out.append("@@");
            }
            if (element.hydrogens() > 0) {
                out.append('H');
                if (element.hydrogens() > 1) {
                    out.append(element.hydrogens());
                }
            }
            if (element.charge() != 0) {
                out.append(element.charge() > 0 ? '+' : '-');
                if (Math.abs(element.charge()) > 1) {
                    out.append(Math.abs(element.charge()));
                }
            }
            out.append(']');
        }


        /** The symbol of an atom, in the lower case of an aromatic ring. */
        private String symbol(int atom) {
            Atom element = molecule.atom(atom);
            return element.isAromatic() ? element.element().toLowerCase() : element.element();
        }

        /** {@code true} when an atom may be written without brackets and still carry what it carries. */
        private boolean isBare(int atom, Atom element) {
            if (!Elements.isOrganicSubset(element.element()) || element.isChiral()
                    || element.charge() != 0 || element.isotope() != 0 || element.mapClass() != 0
                    || element.isRadical()) {
                return false;
            }
            return element.hydrogens() == DefaultValence.implicitHydrogens(element.element(), 0,
                    element.isAromatic(), bondOrderSum(atom));
        }

        /** Shared pairs of every bond that ends at an atom. */
        private int bondOrderSum(int atom) {
            int sum = 0;
            for (int bondIndex : molecule.bondsOf(atom)) {
                sum += molecule.bonds().get(bondIndex).order();
            }
            return sum;
        }

        /** Writes the mark of a bond, nothing at all for a plain bond of the walk. */
        private void writeBond(StringBuilder out, Bond bond) {
            if (bond.stereo() != Bond.NO_STEREO) {
                // The mark of a stereo double bond stands on the plain bonds beside it, and the mark is the
                // whole of what the bond is written as: a slash already means a single bond, drawn a way.
                out.append(bond.stereo());
                return;
            }
            if (bond.isAromatic()) {
                return;
            }
            if (bond.order() == 1) {
                // Two aromatic atoms are joined by an aromatic bond when nothing is written, so a plain
                // single bond between them has to say so.
                if (molecule.atom(bond.first()).isAromatic()
                        && molecule.atom(bond.second()).isAromatic()) {
                    out.append('-');
                }
                return;
            }
            out.append(bond.order() == 2 ? '=' : '#');
        }

        /** The mark a centre is written with under this walk, the other one when the order is turned over. */
        private int stereoMark(int atom) {
            Atom element = molecule.atom(atom);
            if (!element.isChiral()) {
                return Atom.NO_CHIRALITY;
            }
            List<Integer> original = originalLigands(atom);
            List<Integer> written = writtenLigands(atom);
            if (original.size() != 4 || written.size() != 4) {
                return Atom.NO_CHIRALITY;
            }
            Map<Integer, Integer> place = new HashMap<>();
            for (int index = 0; index < 4; index++) {
                if (place.put(original.get(index), index) != null) {
                    return Atom.NO_CHIRALITY;
                }
            }
            int[] order = new int[4];
            for (int index = 0; index < 4; index++) {
                Integer at = place.get(written.get(index));
                if (at == null) {
                    return Atom.NO_CHIRALITY;
                }
                order[index] = at;
            }
            if (inversions(order) % 2 == 0) {
                return element.chirality();
            }
            return element.chirality() == Atom.CHIRAL_ONE ? Atom.CHIRAL_TWO : Atom.CHIRAL_ONE;
        }

        /** The neighbours of a centre in the order the string that built it listed them. */
        private List<Integer> originalLigands(int atom) {
            Atom element = molecule.atom(atom);
            List<Integer> ligands = new ArrayList<>();
            for (int neighbour : element.writtenNeighbours()) {
                ligands.add(neighbour);
            }
            if (element.hydrogens() > 0) {
                ligands.add(HYDROGEN);
            }
            return ligands;
        }

        /** The neighbours of a centre in the order this walk writes them. */
        private List<Integer> writtenLigands(int atom) {
            Atom element = molecule.atom(atom);
            List<Integer> ligands = new ArrayList<>();
            if (parent[atom] >= 0) {
                ligands.add(parent[atom]);
            }
            for (int bondIndex : molecule.bondsOf(atom)) {
                if (!inWalk[bondIndex] && digit[bondIndex] != 0) {
                    ligands.add(molecule.bonds().get(bondIndex).other(atom));
                }
            }
            ligands.addAll(children.get(atom));
            if (element.hydrogens() > 0) {
                ligands.add(HYDROGEN);
            }
            return ligands;
        }

        /** How many pairs of a permutation stand the wrong way round, its parity as a count. */
        private static int inversions(int[] order) {
            int count = 0;
            for (int first = 0; first < order.length; first++) {
                for (int second = first + 1; second < order.length; second++) {
                    if (order[first] > order[second]) {
                        count++;
                    }
                }
            }
            return count;
        }

        /** A ring number as it is written, one digit or two after a percent. */
        private static String digitText(int digit) {
            return digit < 10 ? String.valueOf(digit) : "%" + digit;
        }
    }

    /** A key of an unordered pair of atoms, for looking a bond up. */
    private static long pairKey(int first, int second) {
        int low = Math.min(first, second);
        int high = Math.max(first, second);
        return ((long) low << 32) | (high & 0xffffffffL);
    }
}

