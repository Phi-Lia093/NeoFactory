package com.philia093.neofactory.chemistry;

/**
 * One bond between two atoms of a molecule, the line a SMILES writes between two symbols.
 * <p>
 * A bond is a pair of atoms and how tightly they hold on: one, two or three shared pairs. Which of the two
 * atoms is written first is a fact of the string and not of the molecule - {@code C-C} and {@code C(-C)}
 * name the same bond from two sides - so a bond remembers both ends and answers with the one a caller did
 * not hand in, see {@link #other(int)}. That is what lets the graph be walked from any atom without a
 * direction being baked into the data.
 * <p>
 * <b>A bond of an aromatic ring is not a single one of the three orders.</b> The ring of benzene is neither
 * single nor double but the two blurred together, so an aromatic bond is marked as such and reports an
 * order of one for counting, which is what makes an aromatic carbon hand out exactly one hydrogen instead
 * of two or none, see {@link DefaultValence}. The order and the aromatic mark are written by the aromatic
 * perception of {@link Aromatizer} while a molecule is built and are fixed from then on.
 */
public final class Bond {

    /** Stereo mark of a bond that names none. */
    public static final char NO_STEREO = '\0';

    private final int first;
    private final int second;
    private final char stereo;

    /** Shared pairs of the bond, one to three, or one for a bond of an aromatic ring. */
    private int order;

    /** {@code true} when the bond belongs to an aromatic ring. */
    private boolean aromatic;

    private Bond(int first, int second, int order, boolean aromatic, char stereo) {
        this.first = first;
        this.second = second;
        this.order = order;
        this.aromatic = aromatic;
        this.stereo = stereo;
    }

    /**
     * Creates a bond of a given order.
     *
     * @param first index of the first atom
     * @param second index of the second atom
     * @param order shared pairs, one to three
     * @return the bond
     * @throws IllegalArgumentException when both ends are the same atom or the order is not one to three
     */
    public static Bond of(int first, int second, int order) {
        return of(first, second, order, NO_STEREO);
    }

    /**
     * Creates a bond with a stereo mark, the {@code /} or {@code \} of a double bond.
     *
     * @param first index of the first atom
     * @param second index of the second atom
     * @param order shared pairs, one to three
     * @param stereo mark of the direction, {@link #NO_STEREO} for none
     * @return the bond
     * @throws IllegalArgumentException when both ends are the same atom or the order is not one to three
     */
    public static Bond of(int first, int second, int order, char stereo) {
        if (first == second) {
            throw new IllegalArgumentException("A bond joins two atoms, not the atom " + first);
        }
        if (order < 1 || order > 3) {
            throw new IllegalArgumentException("The order of a bond is one to three: " + order);
        }
        return new Bond(first, second, order, false, stereo);
    }

    /**
     * Creates one bond of an aromatic ring.
     *
     * @param first index of the first atom
     * @param second index of the second atom
     * @return the bond, aromatic and of order one
     */
    static Bond aromatic(int first, int second) {
        return new Bond(first, second, 1, true, NO_STEREO);
    }

    /** Index of the first atom of this bond. */
    public int first() {
        return first;
    }

    /** Index of the second atom of this bond. */
    public int second() {
        return second;
    }

    /** Shared pairs of this bond, one for a bond of an aromatic ring. */
    public int order() {
        return order;
    }

    /** {@code true} when this bond belongs to an aromatic ring. */
    public boolean isAromatic() {
        return aromatic;
    }

    /** Stereo mark of this bond, {@link #NO_STEREO} when it names none. */
    public char stereo() {
        return stereo;
    }

    /**
     * The end of this bond that is not the atom that was handed in.
     *
     * @param atom index of one end
     * @return the index of the other end
     * @throws IllegalArgumentException when the atom is no end of this bond
     */
    public int other(int atom) {
        if (atom == first) {
            return second;
        }
        if (atom == second) {
            return first;
        }
        throw new IllegalArgumentException("The atom " + atom + " is no end of the bond " + this);
    }

    /** {@code true} when this bond ends at the atom. */
    public boolean touches(int atom) {
        return atom == first || atom == second;
    }

    /**
     * Marks this bond as one of an aromatic ring, written by {@link Aromatizer} while a molecule is built.
     * <p>
     * The bond keeps a counting order of one, so the hydrogens an aromatic atom hands out follow from the
     * aromatic model and not from the double bond the ring was written with, see {@link #order()}.
     */
    void makeAromatic() {
        this.order = 1;
        this.aromatic = true;
    }

    @Override
    public String toString() {
        String mark = aromatic ? "aromatic" : "order " + order;
        return "Bond(" + first + "-" + second + ", " + mark + ")";
    }
}
