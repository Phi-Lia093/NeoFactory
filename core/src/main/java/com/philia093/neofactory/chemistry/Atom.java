package com.philia093.neofactory.chemistry;

/**
 * One atom of a molecule, the symbol a SMILES writes and everything that hangs on it.
 * <p>
 * An atom is more than its element. The same carbon is a different atom when it carries a charge, when it
 * is one of the six of a benzene ring, when it is a heavier isotope, and it holds a different number of
 * hydrogens depending on all of that; every one of those facts has to travel with the atom or a reaction
 * will balance two things that are not the same. The neighbourhood of an atom - what it is bonded to - is
 * not written here but in the {@link Molecule}, so an atom stays a small value and the graph stays the
 * place the graph belongs.
 * <p>
 * <b>A bare atom and a bracketed one are two different things.</b> SMILES writes the ten elements of the
 * organic subset without brackets and leaves their hydrogens unstated, to be filled in from the valence of
 * the element, see {@link DefaultValence}; every other element - a metal of the industry above all - is
 * written between brackets, which switches the implicit filling off and makes the atom carry exactly the
 * hydrogens that were spelled out. The flag {@link #isBracketed()} remembers which of the two an atom came
 * from, so the difference survives into the valence rule, and {@code [Na+]} never grows a hydrogen.
 * <p>
 * <b>Two fields are written after the atom is built.</b> Whether an atom lies in an aromatic ring is known
 * only once the whole ring has been read, so {@link Aromatizer} marks it afterwards, and the number of
 * hydrogens a bare atom carries follows from its bonds, so the {@link Molecule} fills it in while it is
 * assembled. Until then the atom reports {@link #UNRESOLVED_HYDROGEN}, which is a value no real atom has
 * and which therefore cannot be mistaken for a filled one.
 */
public final class Atom {

    /** Count of hydrogens of an atom whose bonds are not known yet. */
    public static final int UNRESOLVED_HYDROGEN = -1;

    private final String element;
    private final int charge;
    private final int isotope;
    private final boolean bracketed;

    /** The map number a template writes on this atom, {@code 0} when it carries none. */
    private final int mapClass;

    /** {@code true} when the atom lies in an aromatic ring, marked by {@link Aromatizer}. */
    private boolean aromatic;

    /** Hydrogens of the atom, {@link #UNRESOLVED_HYDROGEN} until the molecule fills it in. */
    private int hydrogens;

    private Atom(String element, int charge, int isotope, boolean aromatic, boolean bracketed,
            int hydrogens, int mapClass) {
        this.element = element;
        this.charge = charge;
        this.isotope = isotope;
        this.aromatic = aromatic;
        this.bracketed = bracketed;
        this.hydrogens = hydrogens;
        this.mapClass = mapClass;
    }

    /**
     * Creates one atom of the organic subset, written without brackets.
     * <p>
     * The hydrogens are left unresolved: a bare atom takes as many as the valence of its element leaves
     * room for, which the molecule works out once the bonds are known.
     *
     * @param element symbol of the element, one of the organic subset
     * @param aromatic {@code true} when the symbol was written in the lower case of an aromatic atom
     * @return the atom
     * @throws IllegalArgumentException when the element is no element of the organic subset
     */
    public static Atom bare(String element, boolean aromatic) {
        if (!Elements.isOrganicSubset(element)) {
            throw new IllegalArgumentException("A bare atom is one of the organic subset, not " + element);
        }
        return new Atom(element, 0, 0, aromatic, false, UNRESOLVED_HYDROGEN, 0);
    }

    /**
     * Creates one atom written between brackets, with everything it names spelled out.
     *
     * @param element symbol of the element
     * @param charge formal charge of the atom
     * @param isotope mass number, {@code 0} for the natural mix
     * @param aromatic {@code true} when the symbol was written in the lower case of an aromatic atom
     * @param hydrogens hydrogens the brackets spelled out
     * @return the atom
     * @throws IllegalArgumentException when the element is unknown or the hydrogens are negative
     */
    public static Atom bracketed(String element, int charge, int isotope, boolean aromatic,
            int hydrogens) {
        return bracketed(element, charge, isotope, aromatic, hydrogens, 0);
    }

    /**
     * Creates one bracketed atom with the map number of a reaction template.
     *
     * @param element symbol of the element
     * @param charge formal charge of the atom
     * @param isotope mass number, {@code 0} for the natural mix
     * @param aromatic {@code true} when the symbol was written in the lower case of an aromatic atom
     * @param hydrogens hydrogens the brackets spelled out
     * @param mapClass map number of a template, {@code 0} for an atom that carries none
     * @return the atom
     * @throws IllegalArgumentException when the element is unknown or the hydrogens are negative
     */
    public static Atom bracketed(String element, int charge, int isotope, boolean aromatic,
            int hydrogens, int mapClass) {
        if (!Elements.isKnown(element)) {
            throw new IllegalArgumentException("Unknown element symbol: " + element);
        }
        if (hydrogens < 0) {
            throw new IllegalArgumentException("A bracketed atom counts its hydrogens: " + hydrogens);
        }
        return new Atom(element, charge, isotope, aromatic, true, hydrogens, mapClass);
    }

    /**
     * Creates an atom whose hydrogens a molecule works out again, for the rewrite of a template.
     * <p>
     * A reaction changes the bonds around an atom, so the hydrogens that were counted before the reaction
     * are the wrong count after it - a carbon that was double bonded and now is single holds two more than
     * it did. An atom that is carried through a rewrite is therefore not copied with the hydrogens it had
     * but built again with none counted, so that the {@link Molecule} it lands in fills them in from the
     * bonds it really turns out to have, see {@link DefaultValence}.
     *
     * @param element symbol of the element
     * @param charge formal charge of the atom
     * @param isotope mass number, {@code 0} for the natural mix
     * @param aromatic {@code true} when the atom lies in an aromatic ring
     * @return the atom, with its hydrogens to be worked out
     */
    static Atom rebuilt(String element, int charge, int isotope, boolean aromatic) {
        return rebuilt(element, charge, isotope, aromatic, 0);
    }

    /**
     * Creates an atom whose hydrogens a molecule works out again, carrying the map number of a pattern.
     *
     * @param element symbol of the element
     * @param charge formal charge of the atom
     * @param isotope mass number, {@code 0} for the natural mix
     * @param aromatic {@code true} when the atom lies in an aromatic ring
     * @param mapClass map number of a template, {@code 0} for an atom that carries none
     * @return the atom, with its hydrogens to be worked out
     */
    static Atom rebuilt(String element, int charge, int isotope, boolean aromatic, int mapClass) {
        return new Atom(element, charge, isotope, aromatic, true, UNRESOLVED_HYDROGEN, mapClass);
    }

    /** Symbol of the element of this atom. */
    public String element() {
        return element;
    }

    /** Formal charge of this atom, {@code 0} for a neutral one. */
    public int charge() {
        return charge;
    }

    /** Mass number of this atom, {@code 0} when it carries the natural mix of isotopes. */
    public int isotope() {
        return isotope;
    }

    /** {@code true} when the atom lies in an aromatic ring. */
    public boolean isAromatic() {
        return aromatic;
    }

    /** {@code true} when the atom was written between brackets with its charge and hydrogens spelled out. */
    public boolean isBracketed() {
        return bracketed;
    }

    /** The map number a reaction template writes on this atom, {@code 0} when it carries none. */
    public int mapClass() {
        return mapClass;
    }

    /**
     * Hydrogens of this atom.
     *
     * @return the number of hydrogens, {@link #UNRESOLVED_HYDROGEN} before the molecule filled it in
     */
    public int hydrogens() {
        return hydrogens;
    }

    /** Writes the hydrogens a molecule worked out for this bare atom. */
    void resolveHydrogens(int count) {
        this.hydrogens = count;
    }

    /** Writes the aromatic mark an {@link Aromatizer} found for this atom. */
    void markAromatic() {
        this.aromatic = true;
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder(element);
        if (isotope > 0) {
            text.insert(0, isotope);
        }
        if (hydrogens > 0) {
            text.append('H').append(hydrogens);
        }
        if (charge != 0) {
            text.append(charge > 0 ? '+' : '-');
            if (Math.abs(charge) > 1) {
                text.append(Math.abs(charge));
            }
        }
        if (aromatic) {
            text.append("(aromatic)");
        }
        return "Atom(" + text + ")";
    }
}
