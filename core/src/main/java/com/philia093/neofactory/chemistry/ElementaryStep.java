package com.philia093.neofactory.chemistry;

import java.util.List;
import java.util.Objects;

/**
 * One step of a mechanism, with a name, the arrows it is drawn with and what it leaves of a configuration.
 * <p>
 * An arrow is a pair of electrons moving and says nothing about what the move means; a step is the move and
 * its meaning together. The nucleophile that comes in and the leaving group that goes out are two arrows of
 * one substitution, and what the step adds to them is the one fact the arrows cannot carry: that a carbon
 * attacked from behind comes out turned over. An addition to a flat centre is a single arrow and the other
 * fact: that the hand the new centre takes is decided by which face the ligand came in from.
 * <p>
 * <b>A step is small and named, and a mechanism is a list of them.</b> It knows only what it does - the
 * atoms it touches and the configuration it leaves - so a rule of a reaction may name the steps it is made
 * of and be read back as the drawing a chemist would make of it, and a test may ask one step at a time.
 */
public final class ElementaryStep {

    private final String name;
    private final List<Arrow> arrows;
    private final int centre;
    private final int incoming;
    private final Face face;
    private final StereoOutcome outcome;

    private ElementaryStep(String name, List<Arrow> arrows, int centre, int incoming, Face face,
            StereoOutcome outcome) {
        this.name = Objects.requireNonNull(name, "name");
        this.arrows = List.copyOf(Objects.requireNonNull(arrows, "arrows"));
        if (this.arrows.isEmpty()) {
            throw new IllegalArgumentException("A step is at least one arrow: " + name);
        }
        this.centre = centre;
        this.incoming = incoming;
        this.face = face;
        this.outcome = Objects.requireNonNull(outcome, "outcome");
    }

    /**
     * A substitution at a saturated carbon: the nucleophile comes in and the leaving group goes out.
     * <p>
     * The two arrows are drawn at once, which is the step a chemist draws with two curved arrows from the
     * same moment; what follows from them is the inversion of the carbon, the way a nucleophile that must
     * come in from behind the leaving group turns the atom over.
     *
     * @param name the name of the step, for a log line and a test
     * @param nucleophile the atom that brings the pair of electrons
     * @param carbon the carbon that is attacked
     * @param leavingGroup the atom that takes the pair of the breaking bond
     * @return the step
     */
    public static ElementaryStep substitution(String name, int nucleophile, int carbon, int leavingGroup) {
        return new ElementaryStep(name, List.of(
                Arrow.fromLonePair(nucleophile, nucleophile, carbon),
                Arrow.toLonePair(carbon, leavingGroup)),
                carbon, -1, null, StereoOutcome.INVERT);
    }

    /**
     * An addition to a flat centre: a ligand comes in from one of its two faces.
     * <p>
     * Two arrows are drawn for the one moment: the lone pair of the ligand on its way into a bond, and the
     * pair of the double bond on its way to the atom that held it - the oxygen of a carbonyl, which is left
     * carrying the negative charge until the workup gives it back a hydrogen. Which hand the centre comes
     * out with is not in either arrow but in the face, and it is worked out from the three ligands that were
     * already in the plane, see {@link Cip#afterFace}.
     *
     * @param name the name of the step
     * @param nucleophile the atom that brings the pair of electrons
     * @param centre the flat atom that is attacked
     * @param acceptor the atom that takes the pair of the double bond, the oxygen of a carbonyl
     * @param face the face the ligand comes in from
     * @return the step
     */
    public static ElementaryStep addition(String name, int nucleophile, int centre, int acceptor,
            Face face) {
        return new ElementaryStep(name, List.of(
                Arrow.fromLonePair(nucleophile, nucleophile, centre),
                Arrow.toLonePair(centre, acceptor)),
                centre, nucleophile, Objects.requireNonNull(face, "face"), StereoOutcome.NONE);
    }

    /** The name of this step. */
    public String name() {
        return name;
    }

    /** The arrows this step is drawn with. */
    public List<Arrow> arrows() {
        return arrows;
    }

    /**
     * Carries the step out on a molecule.
     *
     * @param molecule molecule the step is drawn on
     * @return the molecule the step leaves, its configuration included
     */
    public Molecule apply(Molecule molecule) {
        Objects.requireNonNull(molecule, "molecule");
        Molecule product = molecule;
        for (Arrow arrow : arrows) {
            product = arrow.apply(product);
        }
        if (face != null) {
            char configuration = Cip.afterFace(product, centre, incoming, face);
            return configuration == 0 ? product : Stereocentre.set(product, centre, configuration);
        }
        if (outcome != StereoOutcome.NONE && outcome != StereoOutcome.RETAIN) {
            return Stereocentre.apply(product, centre, outcome);
        }
        return product;
    }
}
