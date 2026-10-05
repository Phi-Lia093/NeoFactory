package com.philia093.neofactory.chemistry;

import java.util.Objects;

/**
 * The part of a substance that does not depend on which of the two kinds it is.
 * <p>
 * An element and a compound answer almost every question the same way - both are a molecule, a string, a
 * composition and a charge - and differ only in the one word {@link Chemical#kind()} answers with. Writing
 * that shared half twice would be writing the equality and the key twice, which is where the two would
 * drift apart and a bar of iron would stop matching another bar of iron, so the shared half lives here and
 * each kind adds its own word.
 */
abstract class ChemicalBase implements Chemical {

    private final Molecule molecule;
    private final String smiles;

    /**
     * Creates a substance from its molecule.
     *
     * @param molecule the molecule of the substance
     * @param smiles the string it was read from
     */
    ChemicalBase(Molecule molecule, String smiles) {
        this.molecule = Objects.requireNonNull(molecule, "molecule");
        this.smiles = Objects.requireNonNull(smiles, "smiles");
    }

    @Override
    public final Molecule structure() {
        return molecule;
    }

    @Override
    public final String smiles() {
        return smiles;
    }

    @Override
    public final Composition composition() {
        return molecule.composition();
    }

    @Override
    public final int charge() {
        return molecule.charge();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Chemical)) {
            return false;
        }
        Chemical other = (Chemical) o;
        return kind() == other.kind() && molecule.equals(other.structure());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(kind(), molecule.canonicalKey());
    }

    @Override
    public final String toString() {
        return kind() + "(" + composition().formula() + ", " + smiles + ")";
    }
}
