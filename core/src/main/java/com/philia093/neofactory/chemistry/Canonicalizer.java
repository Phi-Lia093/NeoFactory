package com.philia093.neofactory.chemistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The canonical labeling of a molecule: one order of its atoms that follows from the graph alone.
 * <p>
 * Two strings that describe the same molecule may name its atoms in any order - a chain read from either
 * end, a ring started at any of its atoms, a branch written before or after the one beside it - and a key
 * that is to stand for the substance has to come out the same for all of them. The key is therefore not a
 * property of the string but of the graph: this class finds the one ordering of the atoms that the graph
 * itself dictates, writes the whole molecule down in that order, and does so again for every symmetric
 * ordering that could stand in its place, keeping the smallest of them.
 * <p>
 * <b>The order is found by refinement and, where refinement stops, by a choice.</b> Every atom is first
 * told apart by what it is - its element, its charge, its isotope, its ring, its hydrogens, how many bonds
 * it has - and then, over and over, by what its neighbours are, until two rounds of that say the same
 * thing: two atoms that still share a description are then known to be symmetric, and the graph alone
 * cannot tell them apart, see {@link #refine}. When such a pair is all that is left, one of the two is
 * picked out by hand - individualised - and the refinement is run again, which usually pulls every atom of
 * the molecule apart at once; the search tries both choices and keeps the smaller writing. A molecule whose
 * symmetry does not fall to one choice is searched further, a choice at a time, and the smallest writing
 * over every leaf of that search is the canonical one.
 * <p>
 * <b>The smallest writing is what makes the key unique.</b> Of two symmetric orderings the two writings are
 * the same, so keeping the smaller changes nothing; of two orderings of one molecule that are not symmetric
 * the writings differ, and the smallest of them is the one every other string of the same molecule also
 * reaches. A molecule that is not the same as another therefore cannot reach the same writing, and a key
 * that two different substances shared would fold them into one.
 */
public final class Canonicalizer {

    private Canonicalizer() {
        // Utility class: never instantiated.
    }

    /**
     * The canonical key of a molecule.
     *
     * @param molecule molecule to label
     * @return the key, the smallest writing of the molecule, never {@code null}
     */
    public static String key(Molecule molecule) {
        if (molecule.atomCount() == 0) {
            return "A[]B[]";
        }
        List<List<Integer>> components = components(molecule);
        if (components.size() == 1) {
            return label(molecule);
        }
        // Several pieces cannot be pulled apart by any refinement, so each is labelled on its own and the
        // labels are sorted, which is what makes the key of a salt independent of the order of its ions.
        List<String> keys = new ArrayList<>(components.size());
        for (List<Integer> component : components) {
            keys.add(label(componentOf(molecule, component)));
        }
        Collections.sort(keys);
        return String.join("|", keys);
    }

    /** Labels one connected molecule, refinement first and the choice where it stops. */
    private static String label(Molecule molecule) {
        int[] colours = refine(molecule, initialColours(molecule));
        return search(molecule, colours, new HashMap<>());
    }

    /** The atom indices of every connected piece of a molecule. */
    private static List<List<Integer>> components(Molecule molecule) {
        boolean[] seen = new boolean[molecule.atomCount()];
        List<List<Integer>> components = new ArrayList<>();
        for (int start = 0; start < molecule.atomCount(); start++) {
            if (seen[start]) {
                continue;
            }
            List<Integer> component = new ArrayList<>();
            Deque<Integer> pending = new ArrayDeque<>();
            pending.push(start);
            seen[start] = true;
            while (!pending.isEmpty()) {
                int atom = pending.pop();
                component.add(atom);
                for (int neighbour : molecule.neighbours(atom)) {
                    if (!seen[neighbour]) {
                        seen[neighbour] = true;
                        pending.push(neighbour);
                    }
                }
            }
            Collections.sort(component);
            components.add(component);
        }
        return components;
    }

    /** One connected piece of a molecule, cut out as a molecule of its own. */
    private static Molecule componentOf(Molecule molecule, List<Integer> component) {
        Map<Integer, Integer> renumbered = new HashMap<>();
        List<Atom> atoms = new ArrayList<>(component.size());
        for (int index = 0; index < component.size(); index++) {
            renumbered.put(component.get(index), index);
            atoms.add(molecule.atom(component.get(index)));
        }
        List<Bond> bonds = new ArrayList<>();
        for (Bond bond : molecule.bonds()) {
            Integer first = renumbered.get(bond.first());
            Integer second = renumbered.get(bond.second());
            if (first == null || second == null) {
                continue;
            }
            if (bond.isAromatic()) {
                bonds.add(Bond.aromatic(first, second));
            } else {
                bonds.add(Bond.of(first, second, bond.order(), bond.stereo()));
            }
        }
        return new Molecule(atoms, bonds);
    }

    /** The first description of every atom, before any neighbour is looked at. */
    private static int[] initialColours(Molecule molecule) {
        String[] description = new String[molecule.atomCount()];
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            description[atom] = invariant(molecule, atom);
        }
        return rank(description);
    }

    /**
     * Refines the colours of a molecule until two rounds say the same thing.
     * <p>
     * One round describes every atom again by what it already carries together with the colours its
     * neighbours carry, and hands two atoms one colour when the two descriptions match. A round that
     * changes nothing means two atoms of one colour have neighbours of the same colours throughout the
     * molecule - they are symmetric and the graph cannot tell them apart - which is what stops the loop and
     * hands the rest to the individualisation of {@link #search}.
     *
     * @param molecule molecule whose atoms are coloured
     * @param colours current colour per atom
     * @return the colours no round changes any more
     */
    private static int[] refine(Molecule molecule, int[] colours) {
        int[] current = colours;
        while (true) {
            String[] description = new String[molecule.atomCount()];
            for (int atom = 0; atom < molecule.atomCount(); atom++) {
                List<String> neighbours = new ArrayList<>();
                for (int bondIndex : molecule.bondsOf(atom)) {
                    Bond bond = molecule.bonds().get(bondIndex);
                    neighbours.add(bondMark(bond) + '@' + current[bond.other(atom)]);
                }
                Collections.sort(neighbours);
                description[atom] = current[atom] + ">" + neighbours;
            }
            int[] refined = rank(description);
            if (splitsNothingFurther(refined, current)) {
                return refined;
            }
            current = refined;
        }
    }

    /**
     * {@code true} when a round has pulled no two atoms apart that the round before it had not.
     * <p>
     * <b>The colours are compared as a partition and never as numbers.</b> The number a round hands an atom
     * is only the place its description came in a sorted list, so two rounds that separate exactly the same
     * pairs of atoms may still number them differently - and a loop that waited for the numbers to stop
     * moving would wait for ever on a molecule whose descriptions sort in a different order from one round to
     * the next. What the loop is really waiting for is that no pair of atoms that shared a colour before
     * carries two different ones now, and that is what is asked here.
     *
     * @param refined the colours this round handed out
     * @param before the colours of the round before it
     * @return {@code true} when the two separate the same pairs of atoms
     */
    private static boolean splitsNothingFurther(int[] refined, int[] before) {
        for (int first = 0; first < refined.length; first++) {
            for (int second = first + 1; second < refined.length; second++) {
                if ((refined[first] == refined[second]) != (before[first] == before[second])) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Turns the descriptions of the atoms into colours, two atoms of one description sharing one. */
    private static int[] rank(String[] description) {
        TreeMap<String, Integer> ranks = new TreeMap<>();
        for (String value : description) {
            ranks.putIfAbsent(value, 0);
        }
        int next = 0;
        for (String value : ranks.keySet()) {
            ranks.put(value, next++);
        }
        int[] colours = new int[description.length];
        for (int atom = 0; atom < description.length; atom++) {
            colours[atom] = ranks.get(description[atom]);
        }
        return colours;
    }

    /** {@code true} when every atom carries a colour of its own, so the order of the atoms is settled. */
    private static boolean discrete(int[] colours) {
        boolean[] seen = new boolean[colours.length];
        for (int colour : colours) {
            if (seen[colour]) {
                return false;
            }
            seen[colour] = true;
        }
        return true;
    }

    /**
     * The smallest writing of a molecule over every way its symmetric atoms are pulled apart.
     * <p>
     * A molecule whose atoms all carry a colour of their own is written down as it stands and that writing
     * is the answer. One that still has two atoms of one colour has them pulled apart by hand, one at a
     * time, the refinement run again under each choice and the search carried on below it; the smallest
     * writing that comes back from all of them is the answer. A state that was reached before answers with
     * what it answered then, so the search walks a tree and not every path of it twice.
     *
     * @param molecule molecule to write
     * @param colours colour of every atom
     * @param memo answers already found, by the colours they were found under
     * @return the smallest writing of the molecule under these colours
     */
    private static String search(Molecule molecule, int[] colours, Map<String, String> memo) {
        if (discrete(colours)) {
            return write(molecule, colours);
        }
        String state = Arrays.toString(colours);
        String known = memo.get(state);
        if (known != null) {
            return known;
        }
        int highest = 0;
        for (int colour : colours) {
            highest = Math.max(highest, colour);
        }
        String best = null;
        for (int atom : atomsOfColour(colours, targetCell(colours))) {
            int[] chosen = colours.clone();
            chosen[atom] = highest + 1;
            String candidate = search(molecule, refine(molecule, chosen), memo);
            if (best == null || candidate.compareTo(best) < 0) {
                best = candidate;
            }
        }
        memo.put(state, best);
        return best;
    }

    /** The colour of the smallest set of atoms the refinement could not pull apart, so of the next choice. */
    private static int targetCell(int[] colours) {
        Map<Integer, Integer> sizes = new TreeMap<>();
        for (int colour : colours) {
            sizes.merge(colour, 1, Integer::sum);
        }
        int target = -1;
        int smallest = Integer.MAX_VALUE;
        for (Map.Entry<Integer, Integer> entry : sizes.entrySet()) {
            if (entry.getValue() > 1 && entry.getValue() < smallest) {
                smallest = entry.getValue();
                target = entry.getKey();
            }
        }
        return target;
    }

    /** The atoms that carry one colour, in ascending order. */
    private static List<Integer> atomsOfColour(int[] colours, int colour) {
        List<Integer> atoms = new ArrayList<>();
        for (int atom = 0; atom < colours.length; atom++) {
            if (colours[atom] == colour) {
                atoms.add(atom);
            }
        }
        return atoms;
    }

    /**
     * Writes a molecule in the order its colours give the atoms.
     * <p>
     * Every atom is written by what it is, in the settled order, and behind them the bonds are written as a
     * table: for every pair of atoms, the mark of the bond they share or a zero when they share none. Two
     * writings of the same molecule are compared as text, which is why the smaller of two is a well defined
     * one and the smallest is the same for every string that names the molecule.
     */
    private static String write(Molecule molecule, int[] colours) {
        Integer[] order = new Integer[colours.length];
        for (int atom = 0; atom < colours.length; atom++) {
            order[atom] = atom;
        }
        Arrays.sort(order, (first, second) -> Integer.compare(colours[first], colours[second]));
        StringBuilder text = new StringBuilder();
        for (int atom : order) {
            text.append(invariant(molecule, atom)).append(';');
        }
        text.append('|');
        for (int first = 0; first < order.length; first++) {
            for (int second = first + 1; second < order.length; second++) {
                Bond bond = bondBetween(molecule, order[first], order[second]);
                text.append(bond == null ? '0' : bondMark(bond));
            }
            text.append(';');
        }
        return text.toString();
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

    /** What an atom is, before any neighbour is looked at. */
    private static String invariant(Molecule molecule, int atom) {
        Atom value = molecule.atom(atom);
        String description = value.element() + ':' + value.charge() + ':' + value.isotope() + ':'
                + value.hydrogens() + (value.isAromatic() ? ":a" : ":-");
        if (value.isRadical()) {
            // An unpaired electron is as much of what an atom is as a charge is, and two molecules that
            // differ in it are two substances and not one. The count is written after everything else and
            // only when there is one, so that a molecule of the closed shell keeps the very key it had
            // before radicals could be moved at all.
            description = description + ":r" + value.radicals();
        }
        return description;
    }

    /** How a bond is written: its order, its aromatic mark and its stereo. */
    private static String bondMark(Bond bond) {
        String order = bond.isAromatic() ? "a" : String.valueOf(bond.order());
        return bond.stereo() == Bond.NO_STEREO ? order : order + bond.stereo();
    }
}
