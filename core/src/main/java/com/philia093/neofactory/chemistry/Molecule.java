package com.philia093.neofactory.chemistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A molecule: the atoms a SMILES string names, the bonds between them and what follows from both.
 * <p>
 * The graph is the molecule. Everything a chemistry module needs about a species is read off it: how many
 * of every element it holds (hydrogens included, which the string left out), what charge it carries, how
 * many separate pieces it is made of, and a key by which two strings that name the same species are seen
 * to be the same. None of that is written down twice - each is counted from the atoms and the bonds - so
 * there is no second place for a molecule to disagree with itself.
 * <p>
 * <b>The hydrogens are filled in while the molecule is assembled.</b> A bare atom of the organic subset
 * arrives with its hydrogens unstated and takes them from {@link DefaultValence} once its bonds are known,
 * which is why the aromatic perception of {@link Aromatizer} has to run first: an aromatic carbon holds one
 * hydrogen and an alkane carbon four, and the count cannot be right until it is known which of the two the
 * atom is. The order - bonds, then aromaticity, then hydrogens - is the whole reason this class builds its
 * own adjacency rather than being handed one.
 * <p>
 * <b>A molecule may be more than one piece.</b> A salt, {@code [Na+].[Cl-]}, is two ions that share no
 * bond; the dot of a SMILES string asks for exactly that. Such a molecule is a {@link Mixture} of ions and
 * not a compound of its own, which {@link #isSingleComponent()} reports so that {@link Chemical} can refuse
 * it. Whether several pieces are one substance or several is a question of the catalog and not of the
 * graph, and the graph only says how many there are.
 */
public final class Molecule {

    private final List<Atom> atoms;
    private final List<Bond> bonds;

    /** Indices of the bonds that end at every atom, the adjacency the rest is walked with. */
    private final List<List<Integer>> bondOfAtom;

    private final Composition composition;
    private final int charge;

    Molecule(List<Atom> atoms, List<Bond> bonds) {
        this.atoms = List.copyOf(atoms);
        this.bonds = List.copyOf(bonds);
        this.bondOfAtom = buildAdjacency();
        resolveHydrogens();
        this.composition = countElements();
        this.charge = countCharge();
    }

    /** Builds the bond index of every atom, checking that every bond ends at a real atom. */
    private List<List<Integer>> buildAdjacency() {
        List<List<Integer>> adjacency = new ArrayList<>(atoms.size());
        for (int index = 0; index < atoms.size(); index++) {
            adjacency.add(new ArrayList<>());
        }
        for (int index = 0; index < bonds.size(); index++) {
            Bond bond = bonds.get(index);
            checkAtom(bond.first());
            checkAtom(bond.second());
            adjacency.get(bond.first()).add(index);
            adjacency.get(bond.second()).add(index);
        }
        return adjacency;
    }

    /** Refuses a bond that ends outside the atoms of this molecule. */
    private void checkAtom(int atom) {
        if (atom < 0 || atom >= atoms.size()) {
            throw new IllegalArgumentException("The atom " + atom + " is outside the molecule");
        }
    }

    /**
     * Fills in the hydrogens of every bare atom, once the bonds and the aromatic rings are known.
     * <p>
     * A bracketed atom already carries the hydrogens its brackets spelled out and is left alone; a bare
     * atom asks {@link DefaultValence} how many its element leaves room for, which depends on the charge,
     * on whether it lies in an aromatic ring and on how many bonds already end at it.
     */
    private void resolveHydrogens() {
        for (int index = 0; index < atoms.size(); index++) {
            Atom atom = atoms.get(index);
            if (atom.hydrogens() != Atom.UNRESOLVED_HYDROGEN) {
                continue;
            }
            atom.resolveHydrogens(DefaultValence.implicitHydrogens(atom.element(), atom.charge(),
                    atom.isAromatic(), bondOrderSum(index)));
        }
    }

    /** Shared pairs of every bond that ends at an atom. */
    private int bondOrderSum(int atom) {
        int sum = 0;
        for (int bondIndex : bondOfAtom.get(atom)) {
            sum += bonds.get(bondIndex).order();
        }
        return sum;
    }

    /** Counts the elements of this molecule, the filled-in hydrogens included. */
    private Composition countElements() {
        Map<String, Integer> counts = new TreeMap<>();
        for (Atom atom : atoms) {
            counts.merge(atom.element(), 1, Integer::sum);
            if (atom.hydrogens() > 0) {
                counts.merge("H", atom.hydrogens(), Integer::sum);
            }
        }
        return Composition.of(counts);
    }

    /** Adds the formal charges of every atom. */
    private int countCharge() {
        int total = 0;
        for (Atom atom : atoms) {
            total += atom.charge();
        }
        return total;
    }

    /** Amount of atoms this molecule is made of. */
    public int atomCount() {
        return atoms.size();
    }

    /**
     * One atom of this molecule.
     *
     * @param index index of the atom
     * @return the atom
     * @throws IndexOutOfBoundsException when no atom has that index
     */
    public Atom atom(int index) {
        return atoms.get(index);
    }

    /** Every bond of this molecule, in the order the string drew them. */
    public List<Bond> bonds() {
        return bonds;
    }

    /**
     * Index of every bond that ends at an atom.
     *
     * @param atom index of the atom
     * @return the bond indices, an empty list for an atom that bonds with nothing
     */
    public List<Integer> bondsOf(int atom) {
        return Collections.unmodifiableList(bondOfAtom.get(atom));
    }

    /**
     * Every atom that shares a bond with an atom.
     *
     * @param atom index of the atom
     * @return the indices of its neighbours, in the order the bonds were added
     */
    public List<Integer> neighbours(int atom) {
        List<Integer> neighbours = new ArrayList<>(bondOfAtom.get(atom).size());
        for (int bondIndex : bondOfAtom.get(atom)) {
            neighbours.add(bonds.get(bondIndex).other(atom));
        }
        return neighbours;
    }

    /** How much of every element this molecule is made of, hydrogens included. */
    public Composition composition() {
        return composition;
    }

    /** Formal charge of this molecule, the sum over its atoms. */
    public int charge() {
        return charge;
    }

    /**
     * How many separate pieces this molecule is made of.
     * <p>
     * One for a molecule whose atoms are all joined by bonds, more for a string that names several pieces
     * with a dot between them. The count is what tells a compound from a {@link Mixture} of ions, see the
     * class comment.
     *
     * @return the amount of connected pieces, {@code 0} for an empty molecule
     */
    public int componentCount() {
        boolean[] seen = new boolean[atoms.size()];
        int components = 0;
        for (int start = 0; start < atoms.size(); start++) {
            if (seen[start]) {
                continue;
            }
            components++;
            Deque<Integer> pending = new ArrayDeque<>();
            pending.push(start);
            seen[start] = true;
            while (!pending.isEmpty()) {
                int atom = pending.pop();
                for (int neighbour : neighbours(atom)) {
                    if (!seen[neighbour]) {
                        seen[neighbour] = true;
                        pending.push(neighbour);
                    }
                }
            }
        }
        return components;
    }

    /** {@code true} when every atom of this molecule is joined to the rest by bonds. */
    public boolean isSingleComponent() {
        return componentCount() == 1;
    }

    /**
     * A key under which two strings that name the same molecule meet.
     * <p>
     * The key is the canonical labeling of the graph, so it never depends on the order an atom was written
     * in and never folds two molecules that are not the same into one, see {@link Canonicalizer}. The
     * catalog of substances matches on it.
     *
     * @return the key, never {@code null}
     */
    public String canonicalKey() {
        return Canonicalizer.key(this);
    }

    @Override
    public String toString() {
        return "Molecule(" + atoms.size() + " atoms, " + bonds.size() + " bonds, "
                + composition.formula() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Molecule)) {
            return false;
        }
        return canonicalKey().equals(((Molecule) o).canonicalKey());
    }

    @Override
    public int hashCode() {
        return canonicalKey().hashCode();
    }
}
