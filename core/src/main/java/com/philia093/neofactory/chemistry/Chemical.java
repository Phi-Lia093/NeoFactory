package com.philia093.neofactory.chemistry;

/**
 * One pure substance: a single element, or one compound of several, and never a mixture of the two.
 * <p>
 * The distinction the module turns on is not between a metal and a nonmetal but between a substance that is
 * <em>one</em> thing and a substance that is <em>several</em>. Water is one compound: its two hydrogens and
 * its oxygen are held together, take part in a reaction together and are balanced together. Bronze is not:
 * its copper and its tin are not bonded, they sit next to one another, and a reaction that touched the
 * bronze touches the copper and the tin on their own. A string like {@code Cu3Sn} hides that difference -
 * it reads like a compound - which is why an alloy is never a {@code Chemical} here but a {@link Mixture},
 * and why this interface is kept to the two honest cases: a substance of one element, and a compound.
 * <p>
 * <b>The two kinds are the two implementations.</b> {@link ElementChemical} is a substance all of whose
 * atoms are the same element - a bar of iron, a bottle of oxygen - and {@link CompoundChemical} is one
 * whose atoms are of several elements and joined into one piece. Which of the two a string means follows
 * from the graph alone and never from the name of the thing: {@code O=O} is an element although it is a
 * molecule, and {@code [OH-]} is a compound although it is one ion. {@link #kind()} reports what the
 * elements of the composition say, and nothing about a catalog or a shape reaches this decision.
 */
public interface Chemical {

    /** The two kinds a pure substance can be. */
    enum Kind {

        /** A substance whose atoms are all of one element, such as iron or oxygen. */
        ELEMENT,

        /** A substance whose atoms are of several elements and joined into one piece, such as water. */
        COMPOUND
    }

    /**
     * The molecule behind this substance, the graph a later reaction is worked on.
     * <p>
     * The structure is handed out and not hidden on purpose: the balance of an inorganic reaction can be
     * read off the composition alone, but an organic one has to look at which atom is bonded to which, and
     * a structure that is only known to the parser would have to be parsed again at that point. The graph
     * is immutable once a molecule is built, so handing it out costs nothing and loses nothing.
     *
     * @return the molecule of this substance, never {@code null}
     */
    Molecule structure();

    /**
     * The string this substance was read from.
     *
     * @return a SMILES string
     */
    String smiles();

    /** How much of every element this substance is made of, hydrogens included. */
    Composition composition();

    /** The formal charge of this substance, the sum over its atoms. */
    int charge();

    /** Which of the two kinds this substance is. */
    Kind kind();

    /**
     * The substance a molecule is.
     * <p>
     * The kind follows from the composition: a substance all of whose atoms are the same element is an
     * {@link ElementChemical}, and one of several elements is a {@link CompoundChemical}. A molecule of
     * several pieces is a {@link Mixture} and not a substance, so it is not built here, see {@link #parse}.
     *
     * @param molecule the molecule
     * @param smiles the string it was read from
     * @return the substance
     * @throws IllegalArgumentException when the molecule is empty or is more than one piece
     */
    static Chemical of(Molecule molecule, String smiles) {
        if (molecule.atomCount() == 0) {
            throw new IllegalArgumentException("A chemical is made of at least one atom");
        }
        if (molecule.composition().elements().size() == 1) {
            return new ElementChemical(molecule, smiles);
        }
        return new CompoundChemical(molecule, smiles);
    }

    /**
     * The substance a molecule is, for one that was never read from a string.
     * <p>
     * A template builds a molecule out of the pieces of another one and there is no string it was written
     * as, so the canonical key stands in as its spelling: it is unique to the molecule and settled, which
     * is all a caller that prints it or looks it up needs. A substance of the catalog has its real string,
     * see {@link Substance}.
     *
     * @param molecule the molecule
     * @return the substance
     * @throws IllegalArgumentException when the molecule is empty
     */
    static Chemical of(Molecule molecule) {
        return of(molecule, molecule.canonicalKey());
    }

    /**
     * Reads a substance from a SMILES string.
     * <p>
     * A string that names several pieces with a dot between them names a mixture - a salt of two ions, a
     * solution - and is refused here with a word for it, because a caller that meant a mixture wants a
     * {@link Mixture} and not a substance that quietly pretends to be one.
     *
     * @param smiles the string
     * @return the substance
     * @throws SmilesException when the string cannot be read or names several pieces
     */
    static Chemical parse(String smiles) {
        Molecule molecule = SmilesParser.parse(smiles);
        if (!molecule.isSingleComponent()) {
            throw new SmilesException("A chemical is one piece; several pieces are a Mixture: " + smiles);
        }
        return of(molecule, smiles);
    }
}
