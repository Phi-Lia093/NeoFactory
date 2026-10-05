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
 * <p>
 * <b>An atom may be a stereocentre and it may be a radical, and both have to travel with it.</b> The
 * stereochemistry of a molecule is the whole of what an organic reaction is judged on - one nucleophile
 * attacks a carbon from one face and not the other, one leaving group departs from behind - so an atom
 * carries the chirality it was written with, which the {@link SmilesParser} reads from the {@code @} of a
 * bracket atom and which a later stage turns from "the way the neighbours were written" into an absolute
 * configuration, see {@link #chirality()}. A radical is the same kind of fact: an atom with unpaired
 * electrons holds fewer bonds than its element usually does, and a count that forgot them would hand a
 * methyl radical four hydrogens instead of three, see {@link #radicals()} and {@link DefaultValence}.
 */
public final class Atom {

    /** Count of hydrogens of an atom whose bonds are not known yet. */
    public static final int UNRESOLVED_HYDROGEN = -1;

    /** Chirality of an atom that was written with none. */
    public static final int NO_CHIRALITY = 0;

    /** The first of the two ways a tetrahedral atom may be written, the {@code @} of a SMILES string. */
    public static final int CHIRAL_ONE = 1;

    /** The second of the two ways, the mirror of {@link #CHIRAL_ONE}, the {@code @@} of a SMILES string. */
    public static final int CHIRAL_TWO = 2;

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

    /**
     * The chirality the atom was written with, {@link #NO_CHIRALITY} when it carries none.
     * <p>
     * The two values are the two ways a tetrahedral atom may be read off its string and are a statement of
     * the order its neighbours were written in, not of an absolute configuration: which of R and S that
     * turns out to be depends on what the neighbours are, see {@link Cip}. The value stays on the atom so
     * that a rewrite can decide to keep it, invert it or drop it.
     */
    private int chirality;

    /**
     * Unpaired electrons of the atom, {@code 0} for the closed shell every ordinary atom has.
     * <p>
     * A radical is a real species of a reaction - the bromine atom of a chain, the carbon left behind when
     * a bond homolyses - and each unpaired electron is one bond the atom does not get to form, so the
     * valence rule has to be told, see {@link DefaultValence}.
     */
    private int radicals;

    private Atom(String element, int charge, int isotope, boolean aromatic, boolean bracketed,
            int hydrogens, int mapClass, int chirality, int radicals) {
        this.element = element;
        this.charge = charge;
        this.isotope = isotope;
        this.aromatic = aromatic;
        this.bracketed = bracketed;
        this.hydrogens = hydrogens;
        this.mapClass = mapClass;
        this.chirality = chirality;
        this.radicals = radicals;
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
        return new Atom(element, 0, 0, aromatic, false, UNRESOLVED_HYDROGEN, 0, NO_CHIRALITY, 0);
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
        return bracketed(element, charge, isotope, aromatic, hydrogens, mapClass, NO_CHIRALITY, 0);
    }

    /**
     * Creates one bracketed atom with the chirality and the unpaired electrons it was written with.
     * <p>
     * This is the whole atom a string may name: an element, what it carries, the way its neighbours were
     * written when it is a stereocentre, and how many unpaired electrons it holds when it is a radical. The
     * chirality is left as the string spelled it and is turned into a configuration later, see
     * {@link #chirality()}.
     *
     * @param element symbol of the element
     * @param charge formal charge of the atom
     * @param isotope mass number, {@code 0} for the natural mix
     * @param aromatic {@code true} when the symbol was written in the lower case of an aromatic atom
     * @param hydrogens hydrogens the brackets spelled out
     * @param mapClass map number of a template, {@code 0} for an atom that carries none
     * @param chirality the chirality the atom was written with, {@link #NO_CHIRALITY} for none
     * @param radicals unpaired electrons of the atom, {@code 0} for a closed shell
     * @return the atom
     * @throws IllegalArgumentException when the element is unknown, the hydrogens are negative or the two
     *         counts are out of range
     */
    public static Atom bracketed(String element, int charge, int isotope, boolean aromatic,
            int hydrogens, int mapClass, int chirality, int radicals) {
        if (!Elements.isKnown(element)) {
            throw new IllegalArgumentException("Unknown element symbol: " + element);
        }
        if (hydrogens < 0) {
            throw new IllegalArgumentException("A bracketed atom counts its hydrogens: " + hydrogens);
        }
        if (chirality != NO_CHIRALITY && chirality != CHIRAL_ONE && chirality != CHIRAL_TWO) {
            throw new IllegalArgumentException("A chirality is one of the two ways or none: " + chirality);
        }
        if (radicals < 0) {
            throw new IllegalArgumentException("An atom holds no negative electrons: " + radicals);
        }
        return new Atom(element, charge, isotope, aromatic, true, hydrogens, mapClass, chirality, radicals);
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
        return rebuilt(element, charge, isotope, aromatic, mapClass, NO_CHIRALITY, 0);
    }

    /**
     * Creates an atom whose hydrogens a molecule works out again, with a chirality and a radical.
     * <p>
     * A step of a mechanism may leave an atom a radical or hand it a place in space, and the atom it builds
     * then is not the atom it was: the hydrogens follow from the bonds and the unpaired electrons once the
     * new molecule is assembled, and the chirality is whatever the step decided and never whatever the atom
     * happened to carry before. That is why the two are named here rather than copied.
     *
     * @param element symbol of the element
     * @param charge formal charge of the atom
     * @param isotope mass number, {@code 0} for the natural mix
     * @param aromatic {@code true} when the atom lies in an aromatic ring
     * @param mapClass map number of a template, {@code 0} for an atom that carries none
     * @param chirality the chirality the step leaves on the atom, {@link #NO_CHIRALITY} for none
     * @param radicals unpaired electrons the step leaves on the atom, {@code 0} for a closed shell
     * @return the atom, with its hydrogens to be worked out
     */
    static Atom rebuilt(String element, int charge, int isotope, boolean aromatic, int mapClass,
            int chirality, int radicals) {
        return new Atom(element, charge, isotope, aromatic, true, UNRESOLVED_HYDROGEN, mapClass,
                chirality, radicals);
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

    /**
     * The chirality this atom was written with, {@link #NO_CHIRALITY} when it carries none.
     * <p>
     * The value says which way round the neighbours of a tetrahedral atom were written and is turned into
     * R or S by {@link Cip} once the neighbours are known. An atom of a molecule that is not a stereocentre
     * carries none, and an atom whose bonds are rewritten keeps, loses or flips this value by the rule of
     * the reaction and never by accident.
     *
     * @return {@link #NO_CHIRALITY}, {@link #CHIRAL_ONE} or {@link #CHIRAL_TWO}
     */
    public int chirality() {
        return chirality;
    }

    /** {@code true} when the atom was written as a stereocentre. */
    public boolean isChiral() {
        return chirality != NO_CHIRALITY;
    }

    /** Unpaired electrons of this atom, {@code 0} for a closed shell. */
    public int radicals() {
        return radicals;
    }

    /** {@code true} when the atom holds at least one unpaired electron. */
    public boolean isRadical() {
        return radicals > 0;
    }

    /** Writes the chirality a reader found on this atom. */
    void markChirality(int chirality) {
        this.chirality = chirality;
    }

    /** Writes the unpaired electrons a reader found on this atom, an {@code [O]}, an {@code [CH3]}. */
    void markRadicals(int radicals) {
        this.radicals = radicals;
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
        if (chirality != NO_CHIRALITY) {
            text.append(chirality == CHIRAL_ONE ? "(@)" : "(@@)");
        }
        if (radicals > 0) {
            text.append("(radical ").append(radicals).append(')');
        }
        return "Atom(" + text + ")";
    }
}
