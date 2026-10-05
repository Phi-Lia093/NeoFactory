package com.philia093.neofactory.chemistry;

/**
 * A substance whose atoms are all of one element: iron, copper, oxygen, a gas of hydrogen.
 * <p>
 * An element is the floor a factory is built on. Every other substance is made of elements and can, in
 * principle, be taken apart into them again - by heat, by a current, by a reaction - while an element
 * cannot be taken apart into anything, because there is nothing under it. A balance therefore treats an
 * element as a terminal: it is what the whole tree of reactions rests on, and this class is the one that
 * says so, by calling itself an {@link Chemical.Kind#ELEMENT}.
 * <p>
 * That a substance is an element is a fact of its atoms and not of its name. {@code O=O} is a molecule of
 * two atoms and still an element, and so is {@code [Fe]} written as a bare atom between brackets; what
 * makes them elements is that a count of their composition finds a single element in it, which is the
 * whole of what this class is chosen by.
 */
public final class ElementChemical extends ChemicalBase {

    /**
     * Creates an element from its molecule.
     *
     * @param molecule a molecule of one element
     * @param smiles the string it was read from
     */
    ElementChemical(Molecule molecule, String smiles) {
        super(molecule, smiles);
    }

    @Override
    public Kind kind() {
        return Kind.ELEMENT;
    }
}
