package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A key under which two strings that name the same molecule meet, until the full labeling replaces it.
 * <p>
 * The catalog of substances is matched by a key and not by a name, because the same species may be written
 * two ways - a Kekulé ring and an aromatic one, a chain read from either end - and a name will not see that
 * the two are one. The key therefore has to be a property of the graph itself and never of the order the
 * atoms were written in: this class walks the neighbourhood of every atom outwards, over and over, until
 * nothing changes, and then writes the whole molecule down as the sorted list of what it found. Two strings
 * that describe the same molecule settle on the same list, whatever order they named the atoms in.
 * <p>
 * <b>This key is deliberately provisional.</b> The walk is a graph invariant, which is enough to let the
 * catalog of a first stage run, but it is not a complete one: two molecules that are not the same can
 * settle on the same list when they are symmetric enough, and nothing here takes them apart. The full
 * canonical labeling - the refinement with the individualisation that breaks a tie - is the work of the
 * next stage and will be dropped in behind the very same method, which is why this one is named for what
 * it is and not for what it will become.
 * <p>
 * <b>The key keeps the charge, the isotope and the stereo.</b> Two molecules that differ only in a formal
 * charge, in a heavier isotope or in the two sides of a double bond are two different molecules, and a key
 * that dropped any of the three would fold them together. Every one of them is written into the descriptor
 * of an atom or of a bond, so the key sees them.
 */
public final class ProvisionalCanonicalizer {

    private ProvisionalCanonicalizer() {
        // Utility class: never instantiated.
    }

    /**
     * The key of a molecule.
     *
     * @param molecule molecule to describe
     * @return the key, never {@code null}
     */
    public static String key(Molecule molecule) {
        int atomCount = molecule.atomCount();
        if (atomCount == 0) {
            return "A[]B[]";
        }
        String[] descriptor = new String[atomCount];
        for (int atom = 0; atom < atomCount; atom++) {
            descriptor[atom] = describe(molecule, atom);
        }
        for (int round = 0; round < atomCount; round++) {
            String[] refined = new String[atomCount];
            for (int atom = 0; atom < atomCount; atom++) {
                refined[atom] = refine(molecule, atom, descriptor);
            }
            if (Arrays.equals(refined, descriptor)) {
                break;
            }
            descriptor = refined;
        }
        return writeKey(molecule, descriptor);
    }

    /** The starting descriptor of an atom: what it is, before any neighbour is looked at. */
    private static String describe(Molecule molecule, int atom) {
        Atom value = molecule.atom(atom);
        return value.element() + ':' + value.charge() + ':' + value.isotope() + ':'
                + (value.isAromatic() ? 'a' : '-') + ':' + value.hydrogens() + ':'
                + molecule.bondsOf(atom).size();
    }

    /** Grows an atom's descriptor by everything its neighbours said in the round before. */
    private static String refine(Molecule molecule, int atom, String[] descriptor) {
        List<String> neighbours = new ArrayList<>();
        for (int bondIndex : molecule.bondsOf(atom)) {
            Bond bond = molecule.bonds().get(bondIndex);
            neighbours.add(bondMark(bond) + descriptor[bond.other(atom)]);
        }
        Collections.sort(neighbours);
        return descriptor[atom] + '|' + neighbours;
    }

    /** How a bond is written into the key: its order, its aromatic mark and its stereo. */
    private static String bondMark(Bond bond) {
        String order = bond.isAromatic() ? "a" : String.valueOf(bond.order());
        return bond.stereo() == Bond.NO_STEREO ? order : order + bond.stereo();
    }

    /** Writes the final descriptors of the atoms and of the bonds out as one sorted key. */
    private static String writeKey(Molecule molecule, String[] descriptor) {
        List<String> atoms = new ArrayList<>(Arrays.asList(descriptor));
        Collections.sort(atoms);
        List<String> bonds = new ArrayList<>();
        for (Bond bond : molecule.bonds()) {
            String first = descriptor[bond.first()];
            String second = descriptor[bond.second()];
            String low = first.compareTo(second) <= 0 ? first : second;
            String high = first.compareTo(second) <= 0 ? second : first;
            bonds.add(low + bondMark(bond) + high);
        }
        Collections.sort(bonds);
        return "A" + atoms + "B" + bonds;
    }
}
