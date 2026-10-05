package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One pair of electrons moving, the curved arrow a chemist draws over a reaction.
 * <p>
 * Every reaction of an organic molecule is a handful of these and nothing else. A pair of electrons is in one
 * of two places at the start - held as a lone pair on an atom, or shared in a bond - and it goes to one of
 * two places at the end: it is shared in a bond that was not there before, or it is held as a lone pair that
 * was not there before. The nucleophile of a substitution is a lone pair on its way to a bond, the leaving
 * group is a bond on its way to a lone pair, and a shift of a hydride is a bond on its way to another bond.
 * <p>
 * <b>Where the electrons go decides the charges, and nothing has to be said twice.</b> A pair of electrons
 * that leaves an atom takes one electron of the count with it and a pair that arrives brings one, so the
 * formal charge of every atom follows from what its bonds and its lone pairs became. An arrow therefore
 * names no charge at all: it moves a pair, and the charges fall out of the move, which is what keeps the
 * charge of the whole molecule the same and a mechanism from quietly making one.
 * <p>
 * <b>The atoms are the same atoms afterwards.</b> An arrow changes bonds and never atoms, so the atoms of the
 * molecule keep their places, their elements, their isotopes and the chirality a reaction decided to leave on
 * them; only the bonds between them and the hydrogens that follow from those bonds are counted again, see
 * {@link Molecule} and {@link DefaultValence}. That is what makes a sequence of arrows a mechanism.
 */
public final class Arrow {

    /** Where the pair of electrons comes from. */
    public enum Donor {

        /** A lone pair the atom was holding. */
        LONE_PAIR,

        /** One pair of a bond the two atoms were sharing. */
        BOND
    }

    /** Where the pair of electrons goes. */
    public enum Acceptor {

        /** A bond the two atoms now share, whether or not they shared one before. */
        BOND,

        /** A lone pair the atom now holds. */
        LONE_PAIR
    }

    private final Donor donor;
    private final int donorFirst;
    private final int donorSecond;
    private final Acceptor acceptor;
    private final int acceptorFirst;
    private final int acceptorSecond;

    private Arrow(Donor donor, int donorFirst, int donorSecond, Acceptor acceptor, int acceptorFirst,
            int acceptorSecond) {
        this.donor = donor;
        this.donorFirst = donorFirst;
        this.donorSecond = donorSecond;
        this.acceptor = acceptor;
        this.acceptorFirst = acceptorFirst;
        this.acceptorSecond = acceptorSecond;
    }

    /**
     * A lone pair of an atom that goes into a bond, the nucleophile of a substitution or an addition.
     *
     * @param lonePairAtom atom the pair was held on
     * @param first one end of the bond the pair makes
     * @param second the other end of that bond
     * @return the arrow
     */
    public static Arrow fromLonePair(int lonePairAtom, int first, int second) {
        return new Arrow(Donor.LONE_PAIR, lonePairAtom, -1, Acceptor.BOND, first, second);
    }

    /**
     * A bond that breaks and leaves its pair on one atom, a leaving group.
     *
     * @param first one end of the bond that breaks
     * @param second the other end of it, the atom that keeps the pair
     * @return the arrow
     */
    public static Arrow toLonePair(int first, int second) {
        return new Arrow(Donor.BOND, first, second, Acceptor.LONE_PAIR, second, -1);
    }

    /**
     * A bond that moves into another bond, the shift of a hydride or of an alkyl group.
     *
     * @param donorFirst one end of the bond the pair leaves
     * @param donorSecond the other end of it
     * @param acceptorFirst one end of the bond the pair makes
     * @param acceptorSecond the other end of that bond
     * @return the arrow
     */
    public static Arrow betweenBonds(int donorFirst, int donorSecond, int acceptorFirst,
            int acceptorSecond) {
        return new Arrow(Donor.BOND, donorFirst, donorSecond, Acceptor.BOND, acceptorFirst,
                acceptorSecond);
    }

    /**
     * Pushes the pair of electrons through a molecule.
     *
     * @param molecule molecule the arrow is drawn on
     * @return the molecule the arrow leaves behind
     * @throws IllegalStateException when the arrow names a bond that is not there
     */
    public Molecule apply(Molecule molecule) {
        Objects.requireNonNull(molecule, "molecule");
        int count = molecule.atomCount();
        int[] orderChange = new int[count];
        int[] lonePairChange = new int[count];
        Map<Long, Integer> bondChange = new HashMap<>();
        if (donor == Donor.LONE_PAIR) {
            lonePairChange[donorFirst] -= 1;
        } else {
            orderChange[donorFirst] -= 1;
            orderChange[donorSecond] -= 1;
            add(bondChange, donorFirst, donorSecond, -1);
        }
        if (acceptor == Acceptor.LONE_PAIR) {
            lonePairChange[acceptorFirst] += 1;
        } else {
            orderChange[acceptorFirst] += 1;
            orderChange[acceptorSecond] += 1;
            add(bondChange, acceptorFirst, acceptorSecond, 1);
        }
        List<Atom> atoms = new ArrayList<>();
        for (int atom = 0; atom < count; atom++) {
            Atom old = molecule.atom(atom);
            int charge = old.charge() - (2 * lonePairChange[atom] + orderChange[atom]);
            Atom fresh = Atom.rebuilt(old.element(), charge, old.isotope(), old.isAromatic(),
                    old.mapClass(), old.chirality(), old.radicals());
            fresh.markWrittenOrder(old.writtenNeighbours());
            atoms.add(fresh);
        }
        List<Bond> bonds = new ArrayList<>();
        for (Bond bond : molecule.bonds()) {
            Integer change = bondChange.remove(pairKey(bond.first(), bond.second()));
            int order = bond.order() + (change == null ? 0 : change);
            if (order <= 0) {
                continue;
            }
            bonds.add(bond.isAromatic() ? Bond.aromatic(bond.first(), bond.second())
                    : Bond.of(bond.first(), bond.second(), order, bond.stereo()));
        }
        for (Map.Entry<Long, Integer> entry : bondChange.entrySet()) {
            if (entry.getValue() < 0) {
                throw new IllegalStateException("An arrow breaks a bond that is not there");
            }
            bonds.add(Bond.of((int) (entry.getKey() >>> 32), (int) (long) entry.getKey(),
                    entry.getValue()));
        }
        return new Molecule(atoms, bonds);
    }

    /** Adds to the change of the bond between two atoms. */
    private static void add(Map<Long, Integer> change, int first, int second, int amount) {
        change.merge(pairKey(first, second), amount, Integer::sum);
    }

    /** A key of an unordered pair of atoms. */
    private static long pairKey(int first, int second) {
        int low = Math.min(first, second);
        int high = Math.max(first, second);
        return ((long) low << 32) | (high & 0xffffffffL);
    }
}
