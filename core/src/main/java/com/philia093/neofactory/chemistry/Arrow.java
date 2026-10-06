package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Electrons moving through a molecule, the curved arrow a chemist draws over a reaction.
 * <p>
 * Every reaction of an organic molecule is a handful of these and nothing else. A pair of electrons is in one
 * of two places at the start - held as a lone pair on an atom, or shared in a bond - and it goes to one of
 * two places at the end: it is shared in a bond that was not there before, or it is held as a lone pair that
 * was not there before. The nucleophile of a substitution is a lone pair on its way to a bond, the leaving
 * group is a bond on its way to a lone pair, and a shift of a hydride is a bond on its way to another bond.
 * <p>
 * <b>An arrow may carry one electron instead of a pair, which is the arrow of a radical.</b> Half of the
 * chemistry of an industry is run by unpaired electrons - a chain halogenation, the growth of a polymer, the
 * homolysis a peroxide starts a reaction with - and none of it can be written with pairs alone: a bond that
 * splits evenly leaves a radical on each of its two atoms, and a radical that comes in on a double bond
 * leaves one behind on the far carbon. Such an arrow is drawn with a single barb, and the electron it
 * carries is taken from or left as an unpaired electron, see {@link #fromRadical} and {@link #toRadical}.
 * The two halves of a bond that splits, and the two radicals that close into one, are one move each, see
 * {@link #homolysis} and {@link #couple}.
 * <p>
 * <b>Where the electrons go decides the charges, and nothing has to be said twice.</b> A pair of electrons
 * that leaves an atom takes one electron of the count with it and a pair that arrives brings one, so the
 * formal charge of every atom follows from what its bonds, its lone pairs and its unpaired electrons became.
 * An arrow therefore names no charge at all and never a radical count either: it moves electrons, and both
 * fall out of the move, which is what keeps the charge of the whole molecule the same and a mechanism from
 * quietly making one.
 * <p>
 * <b>The atoms are the same atoms afterwards.</b> An arrow changes bonds and never atoms, so the atoms of the
 * molecule keep their places, their elements, their isotopes and the chirality a reaction decided to leave on
 * them; only the bonds between them and the hydrogens that follow from those bonds are counted again, see
 * {@link Molecule} and {@link DefaultValence}. That is what makes a sequence of arrows a mechanism.
 */
public final class Arrow {

    /** A bond that an arrow made or broke, keyed by its two atoms. */
    private final Map<Long, Integer> bondChange;

    /** How many lone pairs an arrow took off or left on an atom. */
    private final Map<Integer, Integer> lonePairChange;

    /** How many unpaired electrons an arrow took off or left on an atom. */
    private final Map<Integer, Integer> radicalChange;

    /** How much of an atom's share of the bonds around it an arrow took off or added. */
    private final Map<Integer, Integer> orderChange;

    private Arrow(Map<Long, Integer> bondChange, Map<Integer, Integer> lonePairChange,
            Map<Integer, Integer> radicalChange, Map<Integer, Integer> orderChange) {
        this.bondChange = bondChange;
        this.lonePairChange = lonePairChange;
        this.radicalChange = radicalChange;
        this.orderChange = orderChange;
    }

    /** An empty move, the start of every arrow a factory below builds. */
    private static Arrow empty() {
        return new Arrow(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    /** Changes the shared pairs of the bond between two atoms, on both of their counts. */
    private Arrow withBond(int first, int second, int amount) {
        bondChange.merge(pairKey(first, second), amount, Integer::sum);
        orderChange.merge(first, amount, Integer::sum);
        orderChange.merge(second, amount, Integer::sum);
        return this;
    }

    /** Gives an atom a lone pair more, or takes one of its own away. */
    private Arrow withLonePair(int atom, int amount) {
        lonePairChange.merge(atom, amount, Integer::sum);
        return this;
    }

    /** Leaves an atom an unpaired electron more, or takes one of its own away. */
    private Arrow withRadical(int atom, int amount) {
        radicalChange.merge(atom, amount, Integer::sum);
        return this;
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
        return empty().withLonePair(lonePairAtom, -1).withBond(first, second, 1);
    }

    /**
     * A bond that breaks and leaves its pair on one atom, a leaving group.
     *
     * @param first one end of the bond that breaks
     * @param second the other end of it, the atom that keeps the pair
     * @return the arrow
     */
    public static Arrow toLonePair(int first, int second) {
        return empty().withBond(first, second, -1).withLonePair(second, 1);
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
        return empty().withBond(donorFirst, donorSecond, -1).withBond(acceptorFirst, acceptorSecond, 1);
    }

    /**
     * A bond that breaks and leaves one of its two electrons on each of its atoms, the homolysis a radical
     * chain is started with.
     * <p>
     * The pair of a bond is shared, so a bond that comes apart evenly is not two arrows but one: each atom
     * keeps the electron it contributed, and both come out with an unpaired electron and neither with a
     * charge. An arrow that could only move a whole pair would have to hand both electrons to one atom, which
     * is a heterolysis and a different reaction.
     *
     * @param first one end of the bond that breaks
     * @param second the other end of it
     * @return the arrow
     */
    public static Arrow homolysis(int first, int second) {
        return empty().withBond(first, second, -1).withRadical(first, 1).withRadical(second, 1);
    }

    /**
     * Two radicals whose unpaired electrons pair up into a bond, the termination a radical chain is ended by.
     *
     * @param first one atom that brought an unpaired electron
     * @param second the other
     * @return the arrow
     */
    public static Arrow couple(int first, int second) {
        return empty().withRadical(first, -1).withRadical(second, -1).withBond(first, second, 1);
    }

    /**
     * An unpaired electron of an atom that goes into a bond, the first half of a radical adding to a double
     * bond.
     * <p>
     * One electron makes a bond only together with another, so this arrow is drawn beside one that supplies
     * the other half - the pair of the double bond, one electron of which stays behind as a radical on the
     * far carbon, see {@link #toRadical}.
     *
     * @param radicalAtom atom the unpaired electron was held on
     * @param first one end of the bond the electron makes
     * @param second the other end of that bond
     * @return the arrow
     */
    public static Arrow fromRadical(int radicalAtom, int first, int second) {
        return empty().withRadical(radicalAtom, -1).withBond(first, second, 1);
    }

    /**
     * A bond that parts with one of its two electrons, which is left as an unpaired electron on an atom.
     *
     * @param first one end of the bond
     * @param second the other end of it
     * @param radicalAtom the atom the unpaired electron is left on
     * @return the arrow
     */
    public static Arrow toRadical(int first, int second, int radicalAtom) {
        return empty().withBond(first, second, -1).withRadical(radicalAtom, 1);
    }

    /**
     * Pushes the electrons through a molecule.
     *
     * @param molecule molecule the arrow is drawn on
     * @return the molecule the arrow leaves behind
     * @throws IllegalStateException when the arrow names a bond or a radical that is not there
     */
    public Molecule apply(Molecule molecule) {
        Objects.requireNonNull(molecule, "molecule");
        int count = molecule.atomCount();
        List<Atom> atoms = new ArrayList<>();
        for (int atom = 0; atom < count; atom++) {
            Atom old = molecule.atom(atom);
            int lonePairs = lonePairChange.getOrDefault(atom, 0);
            int radicals = radicalChange.getOrDefault(atom, 0);
            int order = orderChange.getOrDefault(atom, 0);
            int charge = old.charge() - (2 * lonePairs + radicals + order);
            int held = old.radicals() + radicals;
            if (held < 0) {
                throw new IllegalStateException("An arrow spends a radical that is not there");
            }
            Atom fresh = Atom.rebuilt(old.element(), charge, old.isotope(), old.isAromatic(),
                    old.mapClass(), old.chirality(), held);
            fresh.markWrittenOrder(old.writtenNeighbours());
            atoms.add(fresh);
        }
        List<Bond> bonds = new ArrayList<>();
        Map<Long, Integer> remaining = new HashMap<>(bondChange);
        for (Bond bond : molecule.bonds()) {
            Integer change = remaining.remove(pairKey(bond.first(), bond.second()));
            int order = bond.order() + (change == null ? 0 : change);
            if (order <= 0) {
                continue;
            }
            bonds.add(bond.isAromatic() ? Bond.aromatic(bond.first(), bond.second())
                    : Bond.of(bond.first(), bond.second(), order, bond.stereo()));
        }
        for (Map.Entry<Long, Integer> entry : remaining.entrySet()) {
            if (entry.getValue() < 0) {
                throw new IllegalStateException("An arrow breaks a bond that is not there");
            }
            bonds.add(Bond.of((int) (entry.getKey() >>> 32), (int) (long) entry.getKey(),
                    entry.getValue()));
        }
        return new Molecule(atoms, bonds);
    }

    /** A key of an unordered pair of atoms. */
    private static long pairKey(int first, int second) {
        int low = Math.min(first, second);
        int high = Math.max(first, second);
        return ((long) low << 32) | (high & 0xffffffffL);
    }
}
