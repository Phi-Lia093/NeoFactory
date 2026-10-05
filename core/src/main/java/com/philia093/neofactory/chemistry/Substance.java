package com.philia093.neofactory.chemistry;

import java.util.Objects;

/**
 * One substance of the catalog: a molecule, the name a player reads at it and the phase it stands in.
 * <p>
 * A substance is what the module knows and what a reaction is allowed to make. Its identity is the molecule
 * and the phase together - two writings of benzene are one substance, and water and water vapour are two -
 * and the name is only the label a screen prints, so the catalog is looked up by the molecule and the phase
 * and never by the name, which is the line that keeps a reaction from making something the catalog never
 * heard of, see {@link Substances}.
 * <p>
 * The molecule is what everything else is read from: how much of every element the substance is made of,
 * what charge it carries and where two of its atoms sit. Nothing of that is written down twice here, so a
 * substance cannot disagree with itself, and there is no second place a formula could be typed wrong in.
 *
 * @param name the name a screen prints, never blank
 * @param chemical the molecule of the substance
 * @param phase the state it stands in
 */
public record Substance(String name, Chemical chemical, Phase phase) {

    /** Checks the fields, so a broken line of the catalog fails where it is written. */
    public Substance {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(chemical, "chemical");
        Objects.requireNonNull(phase, "phase");
        if (name.isBlank()) {
            throw new IllegalArgumentException("A substance needs a name");
        }
    }

    /** The canonical key of the molecule of this substance, one half of what it is looked up by. */
    public String canonicalKey() {
        return chemical.structure().canonicalKey();
    }

    /** The formula of this substance, as a screen reads it. */
    public String formula() {
        return chemical.composition().formula();
    }

    /** The charge of this substance. */
    public int charge() {
        return chemical.charge();
    }

    @Override
    public String toString() {
        return name + "(" + formula() + ", " + phase + ")";
    }
}
