package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
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
 * <b>Every ring of a molecule is read, fused ones included.</b> The rings are taken from {@link Rings}, which
 * answers the smallest set of smallest rings - a naphthalene is two hexagons and not one ring of ten - and
 * each of them is decided on its own. A molecule may therefore carry a benzene beside a cyclohexane, or two
 * rings joined along one bond, and every ring that is aromatic comes out marked while the rings around it
 * are left as they were. What is not attempted is the shared count of a whole conjugated system: a ring is
 * called aromatic for the electrons its own six atoms bring, which is what the trade means by benzene,
 * pyridine, pyrrole, furan and thiophene.
 * <p>
 * <b>A lower-case ring is taken at its word, a Kekulé one is counted.</b> A ring whose atoms were written
 * in the lower case is already aromatic and this class only makes sure its bonds say so; a ring drawn with
 * plain and double bonds is aromatic when they alternate around a size of four n plus two, which is the
 * benzene count - and a ring written any other way is left alone rather than guessed at, because a guessed
 * ring is a wrong species.
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
        List<List<Integer>> aromatic = new ArrayList<>();
        for (List<Integer> ring : Rings.cycles(atoms.size(), bonds)) {
            if (isAromaticRing(atoms, bonds, ring)) {
                aromatic.add(ring);
            }
        }
        // Every ring is decided before any of them is marked, because marking one blurs the bonds it shares
        // with the next - and the next has to be read from the plain bonds it was drawn with.
        for (List<Integer> ring : aromatic) {
            markAromatic(atoms, bonds, ring);
        }
    }

    /**
     * Decides whether one ring is aromatic.
     * <p>
     * A ring written in the lower case of an aromatic atom is taken at its word; a ring drawn with plain and
     * double bonds is aromatic when its atoms are all content, see {@link #isKekuleAromatic}. Every other
     * ring - a ring of an element this class does not read, a ring of a size the rule does not fit, a ring
     * whose atoms are not content - is left as it was.
     *
     * @param atoms atoms of the molecule
     * @param bonds bonds of the molecule
     * @param ring the atoms of the ring, in the order they are joined
     * @return {@code true} when the ring is aromatic
     */
    private static boolean isAromaticRing(List<Atom> atoms, List<Bond> bonds, List<Integer> ring) {
        for (int atom : ring) {
            if (!RING_ELEMENTS.contains(atoms.get(atom).element())) {
                return false;
            }
        }
        return everyAtomAromatic(atoms, ring) || isKekuleAromatic(atoms, bonds, ring);
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
     * {@code true} when a ring drawn with plain and double bonds is one of the aromatic ones.
     * <p>
     * The ring is read by its atoms and not by its own bonds alone, because a ring joined along one bond to
     * another - a naphthalene - shares that bond and may hold fewer double bonds than half of itself while
     * still being aromatic. So what is asked is that every atom of the ring is content: it carries exactly
     * one double bond, or it is an element that lends the ring a pair of its own electrons instead - which is
     * how a pyridine nitrogen and a furan oxygen are written. A ring of the wrong size, or one whose atoms
     * are not all content, is no aromatic ring: a cyclohexene has plain carbons in it and a cyclobutadiene
     * a size the rule of four n plus two does not fit.
     *
     * @param atoms atoms of the molecule
     * @param bonds bonds of the molecule
     * @param ring the atoms of the ring, in the order they are joined
     * @return {@code true} when the ring is aromatic
     */
    private static boolean isKekuleAromatic(List<Atom> atoms, List<Bond> bonds, List<Integer> ring) {
        int size = ring.size();
        if (size % 4 != 2 && size != 5) {
            return false;
        }
        for (int atom : ring) {
            int doubles = 0;
            for (Bond bond : bonds) {
                if (bond.order() == 2 && bond.touches(atom)) {
                    doubles++;
                }
            }
            if (doubles > 1) {
                return false;
            }
            if (doubles == 0 && !lendsAPair(atoms.get(atom).element())) {
                return false;
            }
        }
        return true;
    }

    /** {@code true} when an element of a ring may take part in it with a lone pair instead of a double bond. */
    private static boolean lendsAPair(String element) {
        return element.equals("N") || element.equals("O") || element.equals("S")
                || element.equals("P");
    }

    /** Marks every atom of a ring as aromatic and every bond between two of them as one of the blurred ones. */
    private static void markAromatic(List<Atom> atoms, List<Bond> bonds, List<Integer> ring) {
        boolean[] inRing = new boolean[atoms.size()];
        for (int atom : ring) {
            inRing[atom] = true;
            atoms.get(atom).markAromatic();
        }
        for (Bond bond : bonds) {
            if (inRing[bond.first()] && inRing[bond.second()]) {
                bond.makeAromatic();
            }
        }
    }
}
