package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the layout of a molecule as arithmetic and not by eye.
 * <p>
 * A drawing that looks right and is not would be a drawing some later picture could not be trusted on, so
 * every rule of the layout is asked as a number: every bond is one bond long whatever the molecule, the ring
 * of a molecule comes out a regular polygon and not a chain that happens to meet itself, a chain bends at
 * the angle a skeleton of carbon is drawn at, and no two atoms are laid on top of one another. The same
 * molecule laid out twice is asked to come out the same, because a picture that moved between two runs
 * could not be pinned down by a test at all.
 */
class SkeletalLayoutTest {

    /** How close two numbers of the layout have to be to count as the same. */
    private static final double FINE = 1e-9;

    /** The strings a layout is asked about, a ring, a chain, a fused pair and a branch among them. */
    private static final String[] MOLECULES = {
        "CCO", "C=C", "C#C", "c1ccccc1", "C1CCCCC1", "Cc1ccccc1", "CC(=O)O",
        "c1ccc2ccccc2c1", "CC(=O)OCC", "CC(C)(C)O", "Oc1ccccc1", "NCC(=O)O"
    };

    @Test
    void everyBondIsOneBondLong() {
        for (String smiles : MOLECULES) {
            Molecule molecule = SmilesParser.parse(smiles);
            SkeletalStructure structure = SkeletalLayout.layout(molecule);
            for (Bond bond : molecule.bonds()) {
                assertEquals(SkeletalLayout.BOND_LENGTH,
                        distance(structure, bond.first(), bond.second()), FINE,
                        smiles + " draws the bond " + bond + " one bond long");
            }
        }
    }

    @Test
    void aRingComesOutAsARegularPolygon() {
        Molecule benzene = SmilesParser.parse("c1ccccc1");
        SkeletalStructure structure = SkeletalLayout.layout(benzene);

        double centerX = 0.0;
        double centerY = 0.0;
        for (int atom = 0; atom < benzene.atomCount(); atom++) {
            centerX += structure.x(atom);
            centerY += structure.y(atom);
        }
        centerX /= benzene.atomCount();
        centerY /= benzene.atomCount();

        double radius = Math.hypot(structure.x(0) - centerX, structure.y(0) - centerY);
        for (int atom = 1; atom < benzene.atomCount(); atom++) {
            assertEquals(radius,
                    Math.hypot(structure.x(atom) - centerX, structure.y(atom) - centerY), FINE,
                    "every atom of a ring stands as far from its middle as the next");
        }
    }

    @Test
    void aChainBendsByTheAngleASkeletonIsDrawnAt() {
        Molecule ethanol = SmilesParser.parse("CCO");
        SkeletalStructure structure = SkeletalLayout.layout(ethanol);

        double incoming = Math.atan2(structure.y(0) - structure.y(1), structure.x(0) - structure.x(1));
        double outgoing = Math.atan2(structure.y(2) - structure.y(1), structure.x(2) - structure.x(1));
        double bend = Math.abs(Math.toDegrees(incoming - outgoing));
        if (bend > 180.0) {
            bend = 360.0 - bend;
        }
        assertEquals(120.0, bend, 1e-6, "a chain bends the sixty degrees a skeleton of carbon turns by");
    }

    @Test
    void noTwoAtomsAreDrawnOnTopOfOneAnother() {
        for (String smiles : MOLECULES) {
            Molecule molecule = SmilesParser.parse(smiles);
            SkeletalStructure structure = SkeletalLayout.layout(molecule);
            for (int first = 0; first < molecule.atomCount(); first++) {
                for (int second = first + 1; second < molecule.atomCount(); second++) {
                    if (bondBetween(molecule, first, second) != null) {
                        continue;
                    }
                    assertTrue(distance(structure, first, second) > 0.5,
                            smiles + " draws two atoms that share no bond on top of one another");
                }
            }
        }
    }

    @Test
    void theSameMoleculeIsAlwaysDrawnTheSameWay() {
        for (String smiles : MOLECULES) {
            SkeletalStructure once = SkeletalLayout.layout(smiles);
            SkeletalStructure twice = SkeletalLayout.layout(smiles);
            for (int atom = 0; atom < once.atomCount(); atom++) {
                assertEquals(once.x(atom), twice.x(atom), 0.0, smiles + " moved between two layouts");
                assertEquals(once.y(atom), twice.y(atom), 0.0, smiles + " moved between two layouts");
            }
        }
    }

    /** Distance between two atoms of a drawing. */
    private static double distance(SkeletalStructure structure, int first, int second) {
        return Math.hypot(structure.x(first) - structure.x(second),
                structure.y(first) - structure.y(second));
    }

    /** The bond between two atoms, or {@code null} when they share none. */
    private static Bond bondBetween(Molecule molecule, int first, int second) {
        for (int bondIndex : molecule.bondsOf(first)) {
            Bond bond = molecule.bonds().get(bondIndex);
            if (bond.other(first) == second) {
                return bond;
            }
        }
        return null;
    }
}
