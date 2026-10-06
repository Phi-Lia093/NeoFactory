package com.philia093.neofactory.chemistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A vessel laid out as one molecule, with the record of what came from where and of how an answer is read.
 * <p>
 * A rule of a reaction is written against atoms, and the atoms of a vessel belong to substances that stand
 * beside each other until the reaction joins them, so the two are read together here: the substances are
 * laid out as one molecule, every atom of it remembers the substance it came from, and a rule that has
 * carried its steps out hands back the molecule it left. What comes back is turned into a reaction by
 * splitting it into the pieces it fell into - a reaction is a pile of substances in and a pile out - and by
 * <b>counting only the pieces the reaction really touched</b>: a solvent that stood there and still stands
 * there is not part of what happened, and a reaction that named it would be a reaction that claims to have
 * done something it did not.
 *
 * <b>An atom remembers which molecule of its substance it came from, and not only which substance.</b> A
 * substance that stands in the vessel twice is laid out twice, so that a reaction between two molecules of
 * one substance is not read as a reaction of one molecule with itself; and a rule that carries a change out
 * on two atoms of <em>one</em> laid out molecule has run a reaction inside that molecule - the closing of a
 * ring, most often - which spends one molecule of it and not two. Which of the two happened is not something
 * a rule has to say: it names the atoms it touched, and the counting of molecules follows from where they
 * came from, see {@link #react(Molecule, int, int...)}.
 *
 * @param molecule everything that stands in the vessel, laid out as one molecule
 * @param origin which substance every atom of it came from
 * @param copies which molecule of that substance every atom of it came from
 * @param substances the substances, in the order they were laid out in
 */
public record Pot(Molecule molecule, int[] origin, int[] copies, List<Chemical> substances) {

    /** Copies what a caller might write to, so the pot stays what it was read from. */
    public Pot {
        origin = origin.clone();
        copies = copies.clone();
        substances = List.copyOf(substances);
    }

    /**
     * The substance an atom of the laid out pot came from.
     *
     * @param atom index of the atom
     * @return the substance it stood in
     */
    public Chemical substanceOf(int atom) {
        return substances.get(origin[atom]);
    }

    /**
     * {@code true} when two atoms came from one and the same molecule of the vessel.
     * <p>
     * Two atoms of one substance that stood in the vessel twice are <em>not</em> of one molecule, and a rule
     * that needs two molecules of something - the two alcohols of an acetal, the two esters of a Claisen -
     * asks this and is answered with what really stands there.
     *
     * @param first index of one atom
     * @param second index of the other
     * @return {@code true} when a single molecule of the vessel holds both
     */
    public boolean sharesAMolecule(int first, int second) {
        return origin[first] == origin[second] && copies[first] == copies[second];
    }

    /**
     * Reads what a rule did to the laid out pot back out as a reaction.
     * <p>
     * <b>The rule names the atoms it touched and the pot counts the molecules.</b> Every atom belongs to one
     * molecule of one substance, so the substances the vessel loses are the substances of the atoms named and
     * the amount of each is how many of their molecules those atoms came from - one, for a rule that joined
     * two places of a single molecule, and two, for a rule that joined two molecules of one substance. A rule
     * that had to work that out for itself would be a rule that could be wrong about it, and one of them
     * would be, which is why it is worked out here once.
     * <p>
     * What the vessel gains are the pieces the rewritten molecule fell into - but only those of them that
     * hold an atom of a molecule the rule named, so that whatever merely stood by is on neither side of the
     * answer. <b>The test is the molecule and not the substance.</b> A vessel may hold more of a reagent than
     * the rule spends - two atoms of a halogen under a light, three molecules of hydrogen where the reduction
     * takes three and one to spare - and the ones it did not touch are no part of what happened: a piece that
     * holds an atom of an untouched molecule of the very substance the rule did touch is a molecule that
     * stood by, and counting it would make the answer gain a substance the reaction never made.
     *
     * @param rewritten the molecule the rule left, its atoms numbered as the pot's
     * @param electrons electrons the reaction takes in, negative when it gives them out
     * @param touched the atoms the rule carried its change out on
     * @return the reaction
     * @throws IllegalArgumentException when the rewritten molecule is not the pot's atoms changed
     */
    public Reaction react(Molecule rewritten, int electrons, int... touched) {
        if (rewritten.atomCount() != molecule.atomCount()) {
            throw new IllegalArgumentException("A reaction changes bonds and never atoms");
        }
        Set<Integer> substancesTouched = new LinkedHashSet<>();
        Set<Long> molecules = new LinkedHashSet<>();
        for (int atom : touched) {
            substancesTouched.add(origin[atom]);
            molecules.add(moleculeKey(atom));
        }
        Mixture reactants = Mixture.empty();
        for (int substance : substancesTouched) {
            int count = 0;
            for (long molecule : molecules) {
                if ((int) (molecule >>> 32) == substance) {
                    count++;
                }
            }
            if (count > 0) {
                reactants = reactants.plus(Mixture.of(substances.get(substance), count));
            }
        }
        Set<Long> taking = new LinkedHashSet<>(molecules);
        Mixture produced = Mixture.empty();
        for (List<Integer> component : components(rewritten)) {
            if (takesPart(component, taking)) {
                produced = produced.plus(
                        Mixture.of(Chemical.of(Assemblies.fold(piece(rewritten, component))), 1));
            }
        }
        return Reaction.of(reactants, produced, electrons, List.of());
    }

    /** A key of the molecule of the vessel an atom came from, its substance and its copy together. */
    private long moleculeKey(int atom) {
        return ((long) origin[atom] << 32) | (copies[atom] & 0xffffffffL);
    }

    /** {@code true} when a piece of the rewritten molecule holds an atom of a molecule the rule named. */
    private boolean takesPart(List<Integer> component, Set<Long> taking) {
        for (int atom : component) {
            if (taking.contains(moleculeKey(atom))) {
                return true;
            }
        }
        return false;
    }

    /** The atoms of every separate piece of a molecule. */
    private static List<List<Integer>> components(Molecule molecule) {
        boolean[] seen = new boolean[molecule.atomCount()];
        List<List<Integer>> pieces = new ArrayList<>();
        for (int start = 0; start < molecule.atomCount(); start++) {
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

    /** One piece of a molecule cut out as a molecule of its own. */
    private static Molecule piece(Molecule molecule, List<Integer> atoms) {
        Map<Integer, Integer> renumbered = new HashMap<>();
        List<Atom> kept = new ArrayList<>(atoms.size());
        for (int index = 0; index < atoms.size(); index++) {
            renumbered.put(atoms.get(index), index);
            kept.add(molecule.atom(atoms.get(index)));
        }
        List<Bond> bonds = new ArrayList<>();
        for (Bond bond : molecule.bonds()) {
            Integer first = renumbered.get(bond.first());
            Integer second = renumbered.get(bond.second());
            if (first != null && second != null) {
                bonds.add(bond.isAromatic() ? Bond.aromatic(first, second)
                        : Bond.of(first, second, bond.order(), bond.stereo()));
            }
        }
        return new Molecule(kept, bonds);
    }
}
