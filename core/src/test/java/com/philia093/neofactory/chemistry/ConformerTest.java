package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the shape a molecule is built into, which is the one thing the graph cannot say.
 * <p>
 * A shape is asked the questions a table of the trade can answer without any arithmetic coming into it: the
 * length of a bond of each kind, the angle about an atom of each kind, the turn of a chain - which is
 * staggered and never flat, so that the two ends of it stand as far from one another as they can - and the
 * planarity of a ring and of a double bond. Then the two things the rest of the module leans on are asked:
 * that a marked centre comes out of the building with the hand the string wrote, and that the atoms of a
 * crowded chain stand apart rather than inside one another.
 */
class ConformerTest {

    @Test
    void aBondComesOutTheLengthItsTwoElementsAre() {
        Conformer shape = shape("CC");

        double apart = shape.distance(0, 1);
        assertTrue(apart > 1.4 && apart < 1.7, "a carbon to carbon bond is about one and a half: " + apart);
    }

    @Test
    void anAtomHoldsItsLigandsAtTheAngleItIsHeldBy() {
        // Saturated: the tetrahedral angle. Held by a double bond: flat. Water: the angle of water.
        assertAngle("CCC", 0, 1, 2, 105.0, 114.0, "a saturated atom holds its ligands at about 109.5");
        assertAngle("C=C", 0, 1, 4, 115.0, 125.0, "an atom held by a double bond holds them at about 120");
        assertAngle("O", 1, 0, 2, 100.0, 109.0, "water is about 104.5 degrees");
    }

    @Test
    void aChainComesOutStaggeredAndNeverFlat() {
        // Butane: the turn about a single bond comes out staggered - sixty, a hundred and eighty or three
        // hundred degrees - and never flat, which would put the two halves of the bond over one another.
        Conformer shape = shape("CCCC");

        double torsion = Math.abs(shape.dihedral(0, 1, 2, 3));
        // The staggered turns are sixty degrees and a hundred and eighty; the flat ones, which would put the
        // two halves of the bond over one another, are zero and a hundred and twenty.
        double nearest = 180.0;
        for (double turn : new double[] {60.0, 180.0}) {
            nearest = Math.min(nearest, Math.abs(torsion - turn));
        }
        assertTrue(nearest <= 30.0, "the turn of a saturated chain is a staggered one: " + torsion);
    }

    @Test
    void aRingComesOutFlat() {
        Conformer shape = shape("c1ccccc1");
        Molecule molecule = shape.molecule();
        List<Integer> ring = Sites.aromaticRings(molecule).get(0).atoms();

        double worst = 0.0;
        for (int step = 0; step < ring.size(); step++) {
            int first = ring.get(step);
            int second = ring.get((step + 1) % ring.size());
            int third = ring.get((step + 2) % ring.size());
            int fourth = ring.get((step + 3) % ring.size());
            worst = Math.max(worst, Math.abs(shape.dihedral(first, second, third, fourth)));
        }
        assertTrue(worst < 20.0, "the atoms of a benzene ring stand in one plane: " + worst);
    }

    @Test
    void aMarkedCentreKeepsTheHandTheStringWrote() {
        // The two hands of the same molecule, built and then read back off the shape: what the string said is
        // what the shape says, which is what every face read off a shape depends on.
        assertEquals('S', configuration("N[C@@H](C)C(=O)O"), "the shape of one alanine is its own string");
        assertEquals('R', configuration("N[C@H](C)C(=O)O"), "and the other string is the other hand");
    }

    @Test
    void aCrowdedChainDoesNotFoldItsAtomsIntoOneAnother() {
        Conformer shape = shape("CC(C)(C)C");

        for (int first = 0; first < shape.atomCount(); first++) {
            for (int second = first + 1; second < shape.atomCount(); second++) {
                if (shape.molecule().neighbours(first).contains(second)) {
                    continue;
                }
                assertTrue(shape.distance(first, second) > 1.4,
                        "two atoms that are not joined stand apart: " + first + " and " + second);
            }
        }
    }

    /** The shape of a molecule written as a string, its hydrogens written out so that they can be measured. */
    private static Conformer shape(String smiles) {
        return Conformer.build(Assemblies.materialize(Chemical.parse(smiles).structure()));
    }

    /** The angle three atoms of one molecule make, written out as a string. */
    private static void assertAngle(String smiles, int first, int corner, int second, double least,
            double most, String what) {
        double angle = shape(smiles).angle(first, corner, second);
        assertTrue(angle > least && angle < most, what + ": " + angle);
    }

    /**
     * The hand a built shape gives the centre of a molecule, read the way the rest of the module reads one:
     * the three ligands of highest priority are taken about the fourth, and the sign of the volume they make
     * is the hand, see {@link Cip}.
     */
    private static char configuration(String smiles) {
        Conformer shape = shape(smiles);
        Molecule molecule = shape.molecule();
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            if (molecule.atom(atom).chirality() != Atom.NO_CHIRALITY) {
                return byHand(shape, molecule, atom);
            }
        }
        throw new IllegalStateException("no centre in " + smiles);
    }

    /** The configuration a shape gives a centre, from the sign of the volume of its ligands. */
    private static char byHand(Conformer shape, Molecule molecule, int centre) {
        int[] ranks = Cip.ranks(molecule);
        List<Integer> ligands = new ArrayList<>();
        for (int ligand : Cip.ligands(molecule, centre)) {
            if (ligand != Cip.HYDROGEN) {
                ligands.add(ligand);
            }
        }
        ligands.sort((first, second) -> Integer.compare(Cip.priority(ranks, second),
                Cip.priority(ranks, first)));
        double[][] stood = new double[ligands.size()][];
        for (int index = 0; index < ligands.size(); index++) {
            stood[index] = Geometry.minus(shape.position(ligands.get(index)),
                    shape.position(centre));
        }
        double[] least = stood[stood.length - 1];
        double[] first = Geometry.minus(stood[0], least);
        double[] second = Geometry.minus(stood[1], least);
        double[] third = Geometry.minus(stood[2], least);
        return Geometry.dot(Geometry.cross(first, second), third) > 0.0 ? 'S' : 'R';
    }
}
