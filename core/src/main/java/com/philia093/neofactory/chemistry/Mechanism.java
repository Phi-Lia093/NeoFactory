package com.philia093.neofactory.chemistry;

import java.util.List;
import java.util.Objects;

/**
 * A sequence of arrows, the mechanism of one reaction written out step by step.
 * <p>
 * A reaction is rarely one move. A substitution is a nucleophile coming in and a leaving group going out, a
 * rearrangement is a bond shifting and a cation following it, and the product is what is left when the last
 * arrow has been drawn. A mechanism is those arrows in the order they are drawn, and applying it is nothing
 * more than pushing them one after the other, see {@link Arrow}.
 * <p>
 * <b>The electrons are conserved, and the mechanism is where that is insisted on.</b> No single arrow changes
 * the charge of a molecule - it moves a pair of electrons and the charges follow - but a sequence of them
 * could still be written wrong, and a wrong mechanism is worse than none because it looks like chemistry.
 * The charge of the molecule before the first arrow and after the last one therefore has to be the same, and
 * a mechanism that quietly made or unmade a charge is refused where it is applied.
 */
public final class Mechanism {

    private final String name;
    private final List<Arrow> arrows;

    private Mechanism(String name, List<Arrow> arrows) {
        this.name = Objects.requireNonNull(name, "name");
        this.arrows = List.copyOf(Objects.requireNonNull(arrows, "arrows"));
        if (this.arrows.isEmpty()) {
            throw new IllegalArgumentException("A mechanism is at least one arrow: " + name);
        }
    }

    /**
     * Creates a mechanism from the arrows it is drawn with.
     *
     * @param name the name of the mechanism, for a log line and a test
     * @param arrows the arrows, in the order they are drawn
     * @return the mechanism
     * @throws IllegalArgumentException when no arrow is named
     */
    public static Mechanism of(String name, Arrow... arrows) {
        return new Mechanism(name, List.of(arrows));
    }

    /**
     * Creates a mechanism from a list of arrows.
     *
     * @param name the name of the mechanism
     * @param arrows the arrows, in the order they are drawn
     * @return the mechanism
     */
    public static Mechanism of(String name, List<Arrow> arrows) {
        return new Mechanism(name, arrows);
    }

    /** The name of this mechanism. */
    public String name() {
        return name;
    }

    /** The arrows of this mechanism, in the order they are drawn. */
    public List<Arrow> arrows() {
        return arrows;
    }

    /**
     * Pushes every arrow through a molecule, in the order they are drawn.
     *
     * @param molecule molecule the mechanism starts from
     * @return the molecule the last arrow leaves
     * @throws IllegalStateException when the arrows do not conserve the charge
     */
    public Molecule apply(Molecule molecule) {
        Objects.requireNonNull(molecule, "molecule");
        Molecule current = molecule;
        for (Arrow arrow : arrows) {
            current = arrow.apply(current);
        }
        if (current.charge() != molecule.charge()) {
            throw new IllegalStateException("The mechanism changed the charge: " + name);
        }
        return current;
    }
}
