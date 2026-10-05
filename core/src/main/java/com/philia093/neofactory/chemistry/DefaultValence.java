package com.philia093.neofactory.chemistry;

import java.util.Map;

/**
 * How many bonds an atom of the organic subset holds, and how many hydrogens that leaves room for.
 * <p>
 * SMILES writes most of a molecule with the hydrogens left out - {@code C} is a carbon and not a lone one -
 * so something has to say how many of them come back. That something is the <b>default valence</b> of the
 * element: how many bonds it usually forms, from which the ones already drawn are subtracted and the rest
 * turn into hydrogens. Carbon is four, oxygen two, nitrogen three, and a bare {@code C} of a SMILES string
 * is methane before it is anything else.
 * <p>
 * <b>The default valence is not the highest valence an element can reach.</b> Sulfur forms two bonds in a
 * sulfide and six in a sulfate, phosphorus three in a phosphine and five in a phosphate, but those higher
 * numbers are not what a bare symbol means: they come from double bonds the string draws on purpose, and
 * the default is only the count the element falls back on when the string is silent. A table that wrote
 * "sulfur 2 or 4 or 6" would hand a sulfide the hydrogens of a sulfate and build a molecule nobody asked
 * for, so the table here holds one number per element and the double bonds do the rest.
 * <p>
 * <b>A charge moves the default valence by its own size.</b> A nitrogen that lost an electron holds one
 * more bond - the ammonium {@code [NH4+]} keeps four hydrogens - and an oxygen that gained one holds one
 * fewer, so the correction is added to the default and the result is what the hydrogens are counted
 * against. The correction is written for the bracketed atoms, where a charge and a hydrogen count are
 * spelled out together, and it is the reason {@code [NH4+]} comes back with four hydrogens rather than
 * three.
 * <p>
 * <b>An aromatic atom is counted another way, and a metal not at all.</b> A carbon of a benzene ring holds
 * three bonds to the ring and one hydrogen, not the four of an alkane carbon, so an aromatic carbon is
 * counted against three; the bare aromatic nitrogen of pyridine carries no hydrogen at all, which the same
 * rule reports because nitrogen of a ring does not reach for one. An element outside the organic subset -
 * every metal, every rare gas - is never handed an implicit hydrogen, because such an element is only ever
 * written between brackets with its atoms counted by hand, see {@link Elements#isOrganicSubset(String)}.
 */
public final class DefaultValence {

    /**
     * Bonds an element of the organic subset forms when the string draws nothing else.
     * <p>
     * One number per element, and deliberately not the highest the element can reach: the higher counts of
     * sulfur and phosphorus come from double bonds of the string and are not a property of the bare symbol,
     * see the class comment.
     */
    private static final Map<String, Integer> DEFAULT_VALENCE = Map.of(
            "B", 3, "C", 4, "N", 3, "O", 2, "P", 3, "S", 2, "F", 1, "Cl", 1, "Br", 1, "I", 1);

    private DefaultValence() {
        // Utility class: never instantiated.
    }

    /**
     * Bonds an element of the organic subset forms when the string draws nothing else.
     *
     * @param element symbol of the element
     * @return the default valence
     * @throws IllegalArgumentException when the element is no element of the organic subset
     */
    public static int defaultValence(String element) {
        Integer valence = DEFAULT_VALENCE.get(element);
        if (valence == null) {
            throw new IllegalArgumentException(
                    "The element " + element + " is no element of the organic subset");
        }
        return valence;
    }

    /**
     * Bonds an element falls back on once a charge is taken into account.
     * <p>
     * The charge is added to the default valence, which is what moves the ammonium nitrogen up to four and
     * the hydroxide oxygen down to one. The answer is never negative: an element that cannot hold a bond
     * holds none, and the caller reads that as "no hydrogen could be filled in".
     *
     * @param element symbol of the element, one of the organic subset
     * @param charge formal charge of the atom
     * @return the valence the hydrogens are counted against
     */
    public static int targetValence(String element, int charge) {
        return Math.max(0, defaultValence(element) + charge);
    }

    /**
     * Hydrogens an atom carries once its bonds are known.
     * <p>
     * A bare atom is handed as many hydrogens as the valence of its element leaves room for after the
     * bonds that are already drawn are counted, and never a negative number: an atom whose bonds already
     * fill it, or that a string wrote with too many of them, is simply given none. An aromatic atom of a
     * ring follows the aromatic count instead - a ring carbon three, a ring nitrogen none - and an element
     * outside the organic subset is handed none at all, which is what keeps a metal from growing a
     * hydrogen, see the class comment.
     *
     * @param element symbol of the element
     * @param charge formal charge of the atom
     * @param aromatic {@code true} when the atom lies in an aromatic ring
     * @param bondOrderSum shared pairs of every bond that ends at the atom
     * @return the number of hydrogens, never negative
     */
    public static int implicitHydrogens(String element, int charge, boolean aromatic,
            int bondOrderSum) {
        return implicitHydrogens(element, charge, aromatic, bondOrderSum, 0);
    }

    /**
     * Hydrogens an atom carries once its bonds and its unpaired electrons are known.
     * <p>
     * <b>An unpaired electron is a bond the atom never gets to form.</b> The methyl radical of a bromination
     * holds three bonds to hydrogen and one unpaired electron, not the four bonds of a methane carbon, so
     * every radical the atom bears is taken off the valence before the hydrogens are counted. A closed
     * shell - the overwhelming case - is the same rule with nothing taken off.
     *
     * @param element symbol of the element
     * @param charge formal charge of the atom
     * @param aromatic {@code true} when the atom lies in an aromatic ring
     * @param bondOrderSum shared pairs of every bond that ends at the atom
     * @param radicals unpaired electrons of the atom, {@code 0} for a closed shell
     * @return the number of hydrogens, never negative
     */
    public static int implicitHydrogens(String element, int charge, boolean aromatic,
            int bondOrderSum, int radicals) {
        if (aromatic) {
            // A ring carbon holds three bonds, a ring nitrogen of pyridine reaches for none.
            return element.equals("C") ? Math.max(0, 3 - bondOrderSum - radicals) : 0;
        }
        if (!Elements.isOrganicSubset(element)) {
            return 0;
        }
        return Math.max(0, targetValence(element, charge) - bondOrderSum - radicals);
    }
}
