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

    /**
     * The same molecule with the blurred bonds of its rings written out as single and double ones.
     * <p>
     * Nothing can be added across a bond that is blurred, because there is no pair of electrons written on it
     * to be moved: an electrophile takes the ring of a benzene apart by taking one pair of it into a bond of
     * its own, and the pair has to be somewhere for that to be drawn. The rings are therefore written the way
     * Kekulé wrote them - alternating single and double bonds around the ring - and the reaction is drawn on
     * that, see {@link #aromatized} for the way back.
     * <p>
     * <b>Only a ring that can be written that way is touched.</b> A ring of an odd number of atoms cannot
     * alternate all the way round, and a ring that shares bonds with another - the two halves of naphthalene -
     * cannot be written without settling both at once; such a ring is left as it stands, and a rule that needs
     * it answers nothing rather than something wrong.
     *
     * @param molecule molecule to write out
     * @return the molecule with the rings of it written as single and double bonds
     */
    public static Molecule kekulized(Molecule molecule) {
        int count = molecule.atomCount();
        boolean[] aromatic = new boolean[count];
        for (int atom = 0; atom < count; atom++) {
            aromatic[atom] = molecule.atom(atom).isAromatic();
        }
        List<int[]> edges = new ArrayList<>();
        List<List<Integer>> edgesOfAtom = new ArrayList<>();
        for (int atom = 0; atom < count; atom++) {
            edgesOfAtom.add(new ArrayList<>());
        }
        for (Bond bond : molecule.bonds()) {
            if (aromatic[bond.first()] && aromatic[bond.second()]) {
                edgesOfAtom.get(bond.first()).add(edges.size());
                edgesOfAtom.get(bond.second()).add(edges.size());
                edges.add(new int[] {bond.first(), bond.second()});
            }
        }
        int[] need = new int[count];
        for (int atom = 0; atom < count; atom++) {
            if (!aromatic[atom]) {
                continue;
            }
            int wanted = 1;
            for (int bondIndex : molecule.bondsOf(atom)) {
                Bond bond = molecule.bonds().get(bondIndex);
                if (bond.order() > 1 && !aromatic[bond.other(atom)]) {
                    // The atom already carries a double bond of its own - a ring carbon of a ketone - so it
                    // takes no part in the alternating ring and is left without one.
                    wanted = 0;
                    break;
                }
            }
            need[atom] = wanted;
        }
        boolean[] matched = new boolean[count];
        boolean[] doubled = new boolean[edges.size()];
        boolean[] written = new boolean[count];
        boolean[] seen = new boolean[count];
        for (int start = 0; start < count; start++) {
            if (!aromatic[start] || seen[start]) {
                continue;
            }
            List<Integer> component = new ArrayList<>();
            List<Integer> pending = new ArrayList<>();
            pending.add(start);
            seen[start] = true;
            while (!pending.isEmpty()) {
                int atom = pending.remove(pending.size() - 1);
                component.add(atom);
                for (int edge : edgesOfAtom.get(atom)) {
                    int other = edges.get(edge)[0] == atom ? edges.get(edge)[1] : edges.get(edge)[0];
                    if (!seen[other]) {
                        seen[other] = true;
                        pending.add(other);
                    }
                }
            }
            List<Integer> order = new ArrayList<>();
            for (int atom : component) {
                if (need[atom] == 1) {
                    order.add(atom);
                }
            }
            for (int atom : component) {
                matched[atom] = false;
            }
            if (assignKekule(order, 0, need, edgesOfAtom, edges, matched, doubled)) {
                for (int atom : component) {
                    written[atom] = true;
                }
            }
        }
        List<Atom> atoms = new ArrayList<>();
        for (int atom = 0; atom < count; atom++) {
            Atom value = molecule.atom(atom);
            atoms.add(Atom.bracketed(value.element(), value.charge(), value.isotope(),
                    aromatic[atom] && !written[atom], value.hydrogens(), value.mapClass(),
                    value.chirality(), value.radicals()));
            atoms.get(atom).markWrittenOrder(value.writtenNeighbours());
        }
        Map<Long, Integer> edgeIndex = new HashMap<>();
        for (int index = 0; index < edges.size(); index++) {
            edgeIndex.put(pairKey(edges.get(index)[0], edges.get(index)[1]), index);
        }
        List<Bond> bonds = new ArrayList<>();
        for (Bond bond : molecule.bonds()) {
            Integer edge = edgeIndex.get(pairKey(bond.first(), bond.second()));
            if (edge == null) {
                bonds.add(Bond.of(bond.first(), bond.second(), bond.order(), bond.stereo()));
            } else if (!written[bond.first()]) {
                bonds.add(Bond.aromatic(bond.first(), bond.second()));
            } else {
                bonds.add(Bond.of(bond.first(), bond.second(), doubled[edge] ? 2 : 1, bond.stereo()));
            }
        }
        return new Molecule(atoms, bonds);
    }

    /**
     * Gives every atom of one aromatic ring system the one double bond it needs, or answers that it cannot.
     * <p>
     * The double bonds of a Kekulé ring are a pairing of its atoms, so what is looked for is a matching of
     * the aromatic bonds that covers every atom that needs one and touches no atom twice: an atom is taken
     * by the first bond it is paired with and the search moves on, and a choice that leads to a dead end is
     * taken back. A ring system that cannot be paired this way - a ring of an odd count of atoms, which no
     * alternation fits - answers {@code false}, and the caller leaves that system aromatic rather than
     * drawing a molecule that cannot exist.
     *
     * @param order the atoms that each still need a double bond, in the order they are tried
     * @param index how far the search has come
     * @param need per atom, {@code 1} when it still needs a double bond
     * @param edgesOfAtom the aromatic bonds that end at every atom
     * @param edges the aromatic bonds, each as its two atoms
     * @param matched per atom, whether a bond has already been given it
     * @param doubled per bond, whether it was made one of the double bonds
     * @return {@code true} when every atom got its bond
     */
    private static boolean assignKekule(List<Integer> order, int index, int[] need,
            List<List<Integer>> edgesOfAtom, List<int[]> edges, boolean[] matched, boolean[] doubled) {
        if (index == order.size()) {
            return true;
        }
        int atom = order.get(index);
        if (matched[atom]) {
            return assignKekule(order, index + 1, need, edgesOfAtom, edges, matched, doubled);
        }
        for (int edge : edgesOfAtom.get(atom)) {
            int other = edges.get(edge)[0] == atom ? edges.get(edge)[1] : edges.get(edge)[0];
            if (need[other] != 1 || matched[other]) {
                continue;
            }
            matched[atom] = true;
            matched[other] = true;
            doubled[edge] = true;
            if (assignKekule(order, index + 1, need, edgesOfAtom, edges, matched, doubled)) {
                return true;
            }
            matched[atom] = false;
            matched[other] = false;
            doubled[edge] = false;
        }
        return false;
    }

    /**
     * The same molecule with the rings of it read again: what is a ring of alternating bonds is blurred once
     * more, see {@link Aromatizer}.
     *
     * @param molecule molecule to read again
     * @return the molecule with its aromatic rings marked
     */
    public static Molecule aromatized(Molecule molecule) {
        List<Atom> atoms = new ArrayList<>();
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            Atom value = molecule.atom(atom);
            atoms.add(Atom.bracketed(value.element(), value.charge(), value.isotope(), false,
                    value.hydrogens(), value.mapClass(), value.chirality(), value.radicals()));
            atoms.get(atom).markWrittenOrder(value.writtenNeighbours());
        }
        List<Bond> bonds = new ArrayList<>();
        for (Bond bond : molecule.bonds()) {
            bonds.add(Bond.of(bond.first(), bond.second(), bond.order(), bond.stereo()));
        }
        Aromatizer.aromatize(atoms, bonds);
        return new Molecule(atoms, bonds);
    }

    /** {@code true} when every bond of a ring is one of the blurred ones. */
    private static boolean allAromatic(Molecule molecule, List<Integer> ring) {
        for (int step = 0; step < ring.size(); step++) {
            Bond bond = bondBetween(molecule, ring.get(step), ring.get((step + 1) % ring.size()));
            if (bond == null || !bond.isAromatic()) {
                return false;
            }
        }
        return true;
    }

    /** The bond between two atoms, or {@code null} when they share none. */
    private static Bond bondBetween(Molecule molecule, int first, int second) {
        for (int bondIndex : molecule.bondsOf(first)) {
            Bond bond = molecule.bonds().get(bondIndex);
            if (bond.other(first) == second) {
                return bond;
            }
        }
        return null;
    }

    /** A key of an unordered pair of atoms. */
    private static long pairKey(int first, int second) {
        int low = Math.min(first, second);
        int high = Math.max(first, second);
        return ((long) low << 32) | (high & 0xffffffffL);
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
