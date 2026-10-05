package com.philia093.neofactory.chemistry;

/**
 * A substance whose atoms are of several elements and are joined into one piece: water, an acid, an ion.
 * <p>
 * A compound is what a reaction makes and what a reaction takes apart. Its atoms hold on to one another,
 * which is why an electrolyzer can spend a current to pull them apart and a reactor can build a new one
 * out of the pieces, and why the whole of it - every element of it, in the count it holds them - is what
 * has to balance. A compound is one piece by definition: a molecule of several pieces is a {@link Mixture}
 * and not a compound, which is the line that keeps bronze from being mistaken for one, see {@link Chemical}.
 * <p>
 * A compound may be neutral or an ion. {@code O} is water and carries no charge, {@code [OH-]} is a
 * hydroxide ion and carries one, and both are compounds because both are of several elements joined into a
 * single piece; the charge is a fact the balance reads off {@link #charge()} and not a reason to treat the
 * ion as anything other than a compound.
 */
public final class CompoundChemical extends ChemicalBase {

    /**
     * Creates a compound from its molecule.
     *
     * @param molecule a molecule of several elements
     * @param smiles the string it was read from
     */
    CompoundChemical(Molecule molecule, String smiles) {
        super(molecule, smiles);
    }

    @Override
    public Kind kind() {
        return Kind.COMPOUND;
    }
}
