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
 * @param molecule everything that stands in the vessel, laid out as one molecule
 * @param origin which substance every atom of it came from
 * @param substances the substances, in the order they were laid out in
 */
public record Pot(Molecule molecule, int[] origin, List<Chemical> substances) {

    /** Copies what a caller might write to, so the pot stays what it was read from. */
    public Pot {
        origin = origin.clone();
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
     * Reads what a rule did to the laid out pot back out as a reaction.
     * <p>
     * The substances the rule names are what the vessel loses, and what it gains are the pieces the
     * rewritten molecule fell into - but only those of them that hold an atom of a substance the rule named,
     * so that whatever merely stood by is on neither side of the answer.
     *
     * @param reactants the substances the rule touched
     * @param rewritten the molecule the rule left, its atoms numbered as the pot's
     * @param electrons electrons the reaction takes in, negative when it gives them out
     * @return the reaction
     * @throws IllegalArgumentException when the rewritten molecule is not the pot's atoms changed
     */
    public Reaction react(List<Chemical> reactants, Molecule rewritten, int electrons) {
        if (rewritten.atomCount() != molecule.atomCount()) {
            throw new IllegalArgumentException("A reaction changes bonds and never atoms");
        }
        Set<Chemical> taking = new LinkedHashSet<>(reactants);
        Mixture consumed = Mixture.empty();
        for (Chemical chemical : taking) {
            consumed = consumed.plus(Mixture.of(chemical, 1));
        }
        Mixture produced = Mixture.empty();
        for (List<Integer> component : components(rewritten)) {
            if (takesPart(component, taking)) {
                produced = produced.plus(Mixture.of(Chemical.of(piece(rewritten, component)), 1));
            }
        }
        return Reaction.of(consumed, produced, electrons, List.of());
    }

    /** {@code true} when a piece of the rewritten molecule holds an atom of a substance the rule named. */
    private boolean takesPart(List<Integer> component, Set<Chemical> taking) {
        for (int atom : component) {
            if (taking.contains(substances.get(origin[atom]))) {
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
