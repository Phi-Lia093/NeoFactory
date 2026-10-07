package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * What a step of a mechanism leaves of a double bond: its two substituents on one side of it or on opposite.
 * <p>
 * A double bond does not turn about its own axis, so the groups standing at its two ends are fixed there, and
 * a reaction that makes one - an elimination that takes water out of an alcohol, a Wittig that takes the
 * oxygen off a carbonyl - has to say which of the two ways round they settle. That is a fact about the bond
 * and not about a screen, and it is a fact the reaction knows: the hydrogen a base takes off is the one that
 * stood across from the leaving group, and the two halves of an ylide come together the way the reagent holds
 * them. So a rule writes the configuration it leaves, the same way a rule that builds a centre writes the
 * hand, see {@link Stereocentre}.
 * <p>
 * <b>The mark is not on the double bond but on the bonds beside it,</b> which is how the language says it:
 * {@code /} and {@code \} stand on the substituents and say which side each of them is on, and two marks that
 * point the same way put the two substituents apart. See {@link Cip#descriptor} for reading them back.
 */
public final class DoubleBond {

    private DoubleBond() {
        // Utility class: never instantiated.
    }

    /**
     * Writes a double bond with the configuration a step leaves it in.
     * <p>
     * A bond with no room to turn has no two ways round and is left as it was: a double bond of a ring is
     * held where the ring puts it, and one whose ends carry a single group apiece - the two hydrogens of
     * ethene - has no pair of substituents to be on a side of at all. Any mark another bond of the same end
     * carried is taken off, because the mark that is read is the first one found and a stale one would be
     * read in place of this.
     *
     * @param molecule molecule to write on
     * @param bond the double bond to write
     * @param descriptor the configuration wanted, {@code 'E'} or {@code 'Z'}
     * @return the molecule with the bond settling that way, a new value
     */
    public static Molecule set(Molecule molecule, Bond bond, char descriptor) {
        Objects.requireNonNull(molecule, "molecule");
        if (bond == null || bond.order() != 2 || bond.isAromatic()
                || (descriptor != 'E' && descriptor != 'Z') || onARing(molecule, bond)) {
            return molecule;
        }
        int first = markable(molecule, bond.first(), bond.second());
        int second = markable(molecule, bond.second(), bond.first());
        if (first < 0 || second < 0) {
            return molecule;
        }
        List<Bond> bonds = new ArrayList<>(molecule.bonds());
        for (int index = 0; index < bonds.size(); index++) {
            if (index == first || index == second) {
                continue;
            }
            Bond standing = bonds.get(index);
            if ((standing.touches(bond.first()) || standing.touches(bond.second()))
                    && standing.stereo() != Bond.NO_STEREO) {
                bonds.set(index, Bond.of(standing.first(), standing.second(), standing.order(),
                        Bond.NO_STEREO));
            }
        }
        Bond one = molecule.bonds().get(first);
        bonds.set(first, Bond.of(one.first(), one.second(), one.order(), '/'));
        Bond two = molecule.bonds().get(second);
        bonds.set(second, Bond.of(two.first(), two.second(), two.order(), descriptor == 'E' ? '/' : '\\'));
        return new Molecule(atomsOf(molecule), bonds);
    }

    /** The bond at one end of a double bond a mark may stand on, or {@code -1} when there is none. */
    private static int markable(Molecule molecule, int end, int other) {
        for (int bondIndex : molecule.bondsOf(end)) {
            Bond bond = molecule.bonds().get(bondIndex);
            if (bond.other(end) != other && bond.order() == 1 && !bond.isAromatic()) {
                return bondIndex;
            }
        }
        return -1;
    }

    /** {@code true} when a bond is held by a ring and so has no two ways round. */
    private static boolean onARing(Molecule molecule, Bond bond) {
        for (List<Integer> cycle : Rings.cycles(molecule)) {
            if (cycle.contains(bond.first()) && cycle.contains(bond.second())) {
                return true;
            }
        }
        return false;
    }

    /** Every atom of a molecule, for a molecule to be built again with the same ones. */
    private static List<Atom> atomsOf(Molecule molecule) {
        List<Atom> atoms = new ArrayList<>(molecule.atomCount());
        for (int index = 0; index < molecule.atomCount(); index++) {
            atoms.add(molecule.atom(index));
        }
        return atoms;
    }
}
