package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Which of the two faces of a flat centre a ligand comes in on, read off the shape of the rest of the
 * molecule.
 * <p>
 * A carbonyl is attacked from one of its two sides, and which of them it is follows from what stands about
 * it: a nucleophile goes where there is room, so the side the rest of the molecule crowds least is the side
 * it comes in on. That is the whole of what the trade calls the induction of a neighbouring centre - Cram's
 * rule and the rest of its family are the same statement made about particular shapes - and it is a statement
 * about the shape, so it is read off the shape and never off the graph, see {@link Conformer}.
 * <p>
 * <b>What is measured is how much stands over each side,</b> every atom of the molecule counted by how close
 * it stands to the centre and by how squarely it stands over the side being measured. That is a crude measure
 * of crowding and is meant to be: all it has to do is put the two sides of one centre in the order a chemist
 * puts them in, and it is only ever asked about the molecule in front of it.
 * <p>
 * <b>The two kinds of centre are read by two different rules and this is only one of them.</b> Nothing here
 * says anything about a centre that is already a stereocentre and keeps the hand it was made with: a
 * nucleophile that attacks one of those is a question of how the two centres stand to one another, which is
 * a question about the shape as well, and it is answered the same way - by building the shape and asking it.
 */
public final class Steric {

    /** How squarely an atom has to stand over a side before it counts as standing over it. */
    private static final double OVER_THE_SIDE = 0.25;

    /** How far from a centre an atom is still counted as crowding it, in angstroms. */
    private static final double REACH = 6.0;

    private Steric() {
        // Utility class: never instantiated.
    }

    /**
     * The face of a flat centre a ligand comes in on: the one the rest of the molecule crowds least.
     *
     * @param molecule the molecule being attacked
     * @param centre the flat atom that is attacked
     * @param incoming the atom that comes in, which is no atom of the molecule yet
     * @return the face the ligand is more likely to arrive on
     */
    public static Face faceFor(Molecule molecule, int centre, int incoming) {
        Conformer shape = Conformer.build(molecule);
        List<Integer> held = new ArrayList<>();
        for (int neighbour : molecule.neighbours(centre)) {
            if (neighbour != incoming) {
                held.add(neighbour);
            }
        }
        if (held.size() < 3) {
            return Face.RE;
        }
        double[] normal = Geometry.unit(Geometry.cross(
                Geometry.minus(shape.position(held.get(0)), shape.position(centre)),
                Geometry.minus(shape.position(held.get(1)), shape.position(centre))));
        double above = crowding(shape, centre, incoming, normal);
        double below = crowding(shape, centre, incoming, Geometry.opposite(normal));
        double[] open = above <= below ? normal : Geometry.opposite(normal);
        return faceOf(molecule, shape, centre, held, open);
    }

    /**
     * How much of a molecule stands over one side of a flat centre.
     *
     * @param shape the shape of the molecule
     * @param centre the flat atom
     * @param incoming the atom that comes in, which is not counted
     * @param side the direction of the side, of length one
     * @return how crowded that side is, more for the more crowded
     */
    private static double crowding(Conformer shape, int centre, int incoming, double[] side) {
        double[] middle = shape.position(centre);
        double crowded = 0.0;
        for (int atom = 0; atom < shape.atomCount(); atom++) {
            if (atom == centre || atom == incoming) {
                continue;
            }
            double[] vector = Geometry.minus(shape.position(atom), middle);
            double apart = Geometry.length(vector);
            if (apart > REACH || apart < 0.2) {
                continue;
            }
            double over = Geometry.dot(vector, side) / apart;
            if (over <= OVER_THE_SIDE) {
                continue;
            }
            crowded += over / (apart * apart);
        }
        return crowded;
    }

    /**
     * Which of the two faces a side is, which follows from the priority of the ligands of the centre.
     * <p>
     * A face is named by the way the ligands read from it: looking at the flat centre, the side from which
     * they read clockwise, highest priority first, is {@link Face#RE}. Two of the ligaments make a turn about
     * the third, and a turn of the right hand points the way it is seen from - so the side the turn points at
     * is the one from which the ligands read the other way round, which is the other face.
     *
     * @param molecule the molecule
     * @param shape the shape of it
     * @param centre the flat atom
     * @param held the atoms held in the plane of the centre
     * @param open the side the ligand comes in from, of length one
     * @return the face
     */
    private static Face faceOf(Molecule molecule, Conformer shape, int centre, List<Integer> held,
            double[] open) {
        int[] ranks = Cip.ranks(molecule);
        List<Integer> ordered = new ArrayList<>(held);
        ordered.sort((first, second) -> Integer.compare(Cip.priority(ranks, second),
                Cip.priority(ranks, first)));
        double[] first = Geometry.minus(shape.position(ordered.get(0)), shape.position(centre));
        double[] second = Geometry.minus(shape.position(ordered.get(1)), shape.position(centre));
        double[] turn = Geometry.cross(first, second);
        return Geometry.dot(turn, open) > 0.0 ? Face.SI : Face.RE;
    }
}
