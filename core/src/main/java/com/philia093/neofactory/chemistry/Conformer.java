package com.philia093.neofactory.chemistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The shape a molecule takes in space: where every atom of it stands, in the three dimensions an atom really
 * has.
 * <p>
 * <b>The graph is not the shape.</b> Everything the rest of the module does is done on a graph of bonds -
 * which atom is joined to which - and a graph has no sides: a carbon with four different ligands is one atom
 * in it, and which of the two faces of a flat centre is the crowded one is written nowhere in it. A
 * nucleophile that has to choose between the two faces of a carbonyl, and a reaction that has to say which
 * hand the centre comes out with, are questions about the shape, so the shape has to be built.
 * <p>
 * <b>The shape is built from the ideal geometry of the elements and then let settle.</b> A bond is as long as
 * the two atoms of it are wide, an angle follows from what stands in the middle of it, a stereocentre keeps
 * the hand its mark wrote and a double bond keeps the side its slash wrote, and the bonds left over turn to
 * stand where they are least crowded - so what comes out is one shape of a molecule that may have many: the
 * one the ideal geometry falls into, and never a measured or a computed one. That is enough for the one
 * question the module asks of it, which is which side of a flat centre is the crowded one, see
 * {@link Steric}.
 * <p>
 * <b>The units are angstroms and degrees,</b> and nothing here is used to weigh anything. The lengths are the
 * ones a table of the trade prints and the constants of the settling are the game's own: a bond that is a
 * little long or an angle that is a little off costs nothing, because the only thing ever read off an answer
 * is the order of two distances.
 */
public final class Conformer {

    /** The width of each element, in angstroms, which is what two of them bond at. */
    private static final String[][] RADII = {
        {"H", "0.31"}, {"B", "0.84"}, {"C", "0.76"}, {"N", "0.71"}, {"O", "0.66"}, {"F", "0.57"},
        {"Si", "1.11"}, {"P", "1.07"}, {"S", "1.05"}, {"Cl", "1.02"}, {"Br", "1.20"}, {"I", "1.39"},
        {"Na", "1.66"}, {"Mg", "1.41"}, {"Al", "1.21"}, {"K", "2.03"}, {"Ca", "1.76"}, {"Fe", "1.32"},
        {"Cu", "1.32"}, {"Zn", "1.22"}, {"Ni", "1.24"}, {"Pt", "1.36"}, {"Sn", "1.39"}, {"Pb", "1.46"},
        {"Ag", "1.45"}, {"Au", "1.36"}, {"W", "1.37"}, {"Ti", "1.60"}, {"Cr", "1.39"}, {"Mn", "1.39"},
        {"Co", "1.26"}};

    /** What each bond order takes off a length: a double bond is shorter than a single one, a triple more. */
    private static final double[] SHORTENING = {0.0, 0.0, 0.13, 0.26};

    /** The angle a saturated atom holds its ligands at, in degrees. */
    private static final double TETRAHEDRAL = 109.47;

    /** How far two atoms that are no hydrogen are held apart at least, in angstroms. */
    private static final double HEAVY_TOUCH = 2.9;

    /** How far a hydrogen and another atom are held apart at least, in angstroms. */
    private static final double LIGHT_TOUCH = 2.3;

    private final Molecule molecule;
    private final double[][] positions;

    private Conformer(Molecule molecule, double[][] positions) {
        this.molecule = molecule;
        this.positions = positions;
    }

    /**
     * Builds the shape of a molecule.
     *
     * @param molecule molecule to build
     * @return the shape, every atom of it standing somewhere
     */
    public static Conformer build(Molecule molecule) {
        int count = molecule.atomCount();
        double[][] positions = new double[count][3];
        if (count > 0) {
            boolean[] placed = new boolean[count];
            int start = heaviest(molecule);
            placed[start] = true;
            Deque<Integer> pending = new ArrayDeque<>();
            pending.add(start);
            while (!pending.isEmpty()) {
                int atom = pending.poll();
                for (int neighbour : molecule.neighbours(atom)) {
                    if (!placed[neighbour]) {
                        placed[neighbour] = true;
                        place(molecule, positions, placed, neighbour, atom);
                        pending.add(neighbour);
                    }
                }
            }
        }
        settle(molecule, positions);
        return new Conformer(molecule, positions);
    }

    /** The molecule this is the shape of. */
    public Molecule molecule() {
        return molecule;
    }

    /** How many atoms the shape is of. */
    public int atomCount() {
        return positions.length;
    }

    /**
     * Where an atom stands.
     *
     * @param atom index of the atom
     * @return a copy of its three coordinates, in angstroms
     */
    public double[] position(int atom) {
        return positions[atom].clone();
    }

    /**
     * How far apart two atoms stand.
     *
     * @param first index of one atom
     * @param second index of the other
     * @return the distance in angstroms
     */
    public double distance(int first, int second) {
        return Geometry.distance(positions[first], positions[second]);
    }

    /**
     * The angle three atoms make, the middle one at the corner.
     *
     * @param first index of one atom
     * @param corner index of the atom at the corner
     * @param second index of the other
     * @return the angle in degrees, {@code 0} when a pair of the three stands on top of another
     */
    public double angle(int first, int corner, int second) {
        return Geometry.angle(positions[first], positions[corner], positions[second]);
    }

    /**
     * The angle four atoms make, the turn of the first pair seen from the second.
     *
     * @param first index of one atom
     * @param second index of the second
     * @param third index of the third
     * @param fourth index of the fourth
     * @return the torsion in degrees, between {@code -180} and {@code 180}
     */
    public double dihedral(int first, int second, int third, int fourth) {
        return Geometry.dihedral(positions[first], positions[second], positions[third],
                positions[fourth]);
    }

    @Override
    public String toString() {
        return "Conformer(" + molecule.atomCount() + " atoms)";
    }

    /** The atom to build a molecule from, which is the one that carries the most bonds. */
    private static int heaviest(Molecule molecule) {
        int best = 0;
        int most = -1;
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            int bonds = molecule.neighbours(atom).size();
            if (bonds > most) {
                most = bonds;
                best = atom;
            }
        }
        return best;
    }

    /**
     * Stands one atom where the ideal geometry of what it hangs on leaves it.
     * <p>
     * <b>A stereocentre is placed the way its mark says and never by the angles.</b> The mark of an atom is a
     * statement about the order its ligands were written in, so a centre that carries one is placed from the
     * tetrahedron that order names, see {@link #tetrahedron}. Everything else falls into the plane or the cone
     * the ideal angle of its parent leaves, and the settling that follows turns it to where it is least
     * crowded.
     *
     * @param molecule molecule being built
     * @param positions the coordinates so far
     * @param placed which atoms stand already
     * @param child the atom to stand
     * @param parent the atom already standing that carries it
     */
    private static void place(Molecule molecule, double[][] positions, boolean[] placed, int child,
            int parent) {
        double length = bondLength(molecule, parent, child);
        if (molecule.atom(parent).chirality() != Atom.NO_CHIRALITY
                && tetrahedron(molecule, positions, placed, parent)) {
            return;
        }
        List<Integer> references = new ArrayList<>();
        for (int neighbour : molecule.neighbours(parent)) {
            if (neighbour != child && placed[neighbour]) {
                references.add(neighbour);
            }
        }
        if (references.isEmpty()) {
            positions[child] = new double[] {length, 0.0, 0.0};
            return;
        }
        double angle = bondAngle(molecule, parent);
        double[] one = Geometry.unit(Geometry.minus(positions[references.get(0)], positions[parent]));
        double[] turned = Geometry.turned(one, Geometry.anyPerpendicular(one), angle);
        if (references.size() == 1) {
            positions[child] = Geometry.plus(positions[parent], Geometry.scale(turned, length));
            return;
        }
        // Two ligands already stand about the parent, so the new one goes to the far side of the plane they
        // make - which is staggered for a saturated atom and flat for one held by a double bond.
        double[] axis = Geometry.cross(one,
                Geometry.unit(Geometry.minus(positions[references.get(1)], positions[parent])));
        positions[child] = Geometry.plus(positions[parent],
                Geometry.scale(Geometry.turned(turned, axis, 180.0), length));
    }

    /**
     * Stands every ligand of a marked centre the way the mark reads, with the first of the order up.
     * <p>
     * The four ligands of a tetrahedral centre stand at {@code 109.47} degrees of one another, and the mark
     * says which way round the three of them not at the top are read when the atom is looked at from the
     * first of the order - so that is the way they are put, see {@link Cip#ligands} and {@link Face}. A
     * hydrogen left to the valence of its element is a ligand of the mark and not a place in the shape, so it
     * is counted and never stood.
     *
     * @param molecule molecule being built
     * @param positions the coordinates so far
     * @param placed which atoms stand already
     * @param centre the marked centre
     * @return {@code true} when the centre is marked and its ligands were stood
     */
    private static boolean tetrahedron(Molecule molecule, double[][] positions, boolean[] placed,
            int centre) {
        int[] ligands = Cip.ligands(molecule, centre);
        int standing = -1;
        int others = 0;
        for (int index = 0; index < ligands.length; index++) {
            if (ligands[index] == Cip.HYDROGEN || !placed[ligands[index]]) {
                continue;
            }
            if (standing < 0) {
                standing = index;
            } else {
                others++;
            }
        }
        if (standing < 0 || others > 0) {
            // Nothing of the centre stands yet, or more than one ligand of it does: either way the frame of
            // it is not this atom's to lay down, and the atom is placed by the angles like any other.
            return false;
        }
        double[] axis = Geometry.unit(Geometry.minus(positions[ligands[standing]], positions[centre]));
        double[] across = Geometry.anyPerpendicular(axis);
        double[] other = Geometry.cross(axis, across);
        double turn = molecule.atom(centre).chirality() == Atom.CHIRAL_ONE ? 1.0 : -1.0;
        double cone = Math.cos(Math.toRadians(TETRAHEDRAL));
        double skirt = Math.sin(Math.toRadians(TETRAHEDRAL));
        int step = 0;
        for (int index = 0; index < ligands.length; index++) {
            if (ligands[index] == Cip.HYDROGEN || index == standing) {
                continue;
            }
            step++;
            double azimuth = Math.toRadians(120.0 * step * turn);
            double[] direction = Geometry.plus(
                    Geometry.plus(Geometry.scale(axis, cone),
                            Geometry.scale(across, skirt * Math.cos(azimuth))),
                    Geometry.scale(other, skirt * Math.sin(azimuth)));
            positions[ligands[index]] = Geometry.plus(positions[centre],
                    Geometry.scale(direction, bondLength(molecule, centre, ligands[index])));
            placed[ligands[index]] = true;
        }
        return true;
    }

    /** {@code true} when an atom has not been stood anywhere yet. */
    private static boolean isFree(double[][] positions, int atom) {
        return positions[atom][0] == 0.0 && positions[atom][1] == 0.0 && positions[atom][2] == 0.0;
    }

    /** How long the bond between two atoms is, from the width of the two of them and its order. */
    private static double bondLength(Molecule molecule, int first, int second) {
        Bond bond = null;
        for (int index : molecule.bondsOf(first)) {
            if (molecule.bonds().get(index).other(first) == second) {
                bond = molecule.bonds().get(index);
            }
        }
        int order = bond == null ? 1 : Math.max(1, Math.min(3, bond.order()));
        double shortening = bond != null && bond.isAromatic() ? 0.07 : SHORTENING[order];
        return radiusOf(molecule.atom(first).element()) + radiusOf(molecule.atom(second).element())
                - shortening;
    }

    /** The width of an element, in angstroms. */
    private static double radiusOf(String element) {
        for (String[] row : RADII) {
            if (row[0].equals(element)) {
                return Double.parseDouble(row[1]);
            }
        }
        return 1.30;
    }

    /** The angle an atom holds its ligands at, from what it is held by. */
    private static double bondAngle(Molecule molecule, int atom) {
        Atom value = molecule.atom(atom);
        int highest = 0;
        for (int index : molecule.bondsOf(atom)) {
            highest = Math.max(highest, molecule.bonds().get(index).order());
        }
        if (highest >= 3) {
            return 180.0;
        }
        if (highest == 2 || value.isAromatic()) {
            return 120.0;
        }
        if (value.element().equals("O")) {
            return 104.5;
        }
        if (value.element().equals("N") || value.element().equals("S")) {
            return 107.0;
        }
        return TETRAHEDRAL;
    }

    /**
     * Lets the shape settle: every atom is moved a little, again and again, until nothing about the shape is
     * strained any more.
     * <p>
     * <b>A shape is built and then let go, because the ideal geometry cannot be laid down all at once.</b> A
     * chain is placed from its first atom outwards and every later atom is placed from the ones before it, so
     * a ring closes on a bond that is longer than its ideal and a crowded chain comes out of the building
     * spread the way nothing real is spread. Every atom is therefore tried a little to each side and the
     * moves that cost less are kept, over and over with the steps growing shorter - which is the oldest way
     * there is of letting a shape find itself, and all the module needs of one.
     * <p>
     * <b>A move that would turn a centre over is refused.</b> The hand a stereocentre was placed with is not a
     * preference to be minimised: it is what the string said, so the sign of every marked centre is read
     * before the settling and a move that would change it is taken back, whatever it costs in strain.
     *
     * @param molecule molecule being built
     * @param positions the coordinates, moved in place
     */
    private static void settle(Molecule molecule, double[][] positions) {
        if (molecule.atomCount() < 4) {
            return;
        }
        int[] hands = handsOf(molecule, positions);
        boolean[] held = markedLigands(molecule);
        boolean[] ring = inRings(molecule);
        int[][] pairs = touchingPairs(molecule, ring);
        double[][] built = new double[positions.length][];
        for (int atom = 0; atom < positions.length; atom++) {
            built[atom] = positions[atom].clone();
        }
        for (double size : new double[] {0.25, 0.12, 0.06}) {
            for (int sweep = 0; sweep < 60; sweep++) {
                boolean moved = false;
                for (int atom = 0; atom < molecule.atomCount(); atom++) {
                    if (held[atom]) {
                        continue;
                    }
                    double[] before = positions[atom].clone();
                    double best = strain(molecule, positions, pairs, ring);
                    double[] chosen = before;
                    for (int axis = 0; axis < 3; axis++) {
                        for (double side : new double[] {-1.0, 1.0}) {
                            double[] trial = before.clone();
                            trial[axis] += side * size;
                            positions[atom] = trial;
                            double cost = strain(molecule, positions, pairs, ring);
                            if (cost < best - 1.0e-6 && keepsItsHands(molecule, positions, hands)) {
                                best = cost;
                                chosen = trial;
                            }
                        }
                        positions[atom] = before;
                    }
                    positions[atom] = chosen;
                    moved |= chosen != before;
                }
                if (!moved) {
                    break;
                }
            }
        }
        if (!keepsItsHands(molecule, positions, hands)) {
            // A shape whose settling turned a centre over is thrown away and the one the building laid down
            // is kept: that one was stood from the mark of every centre and cannot have turned, and a shape
            // that is a little more strained is worth more than a centre with the wrong hand on it.
            for (int atom = 0; atom < positions.length; atom++) {
                positions[atom] = built[atom].clone();
            }
        }
    }

    /** A number times itself. */
    private static double square(double value) {
        return value * value;
    }

    /**
     * What a shape costs in strain, less being better.
     * <p>
     * Four things are strained about an atom: a bond that is not the length its two elements come to, an
     * angle that is not the angle its middle atom holds, a bond turned where it would rather not be, and two
     * atoms that are not joined standing closer than they can. The weights are the game's own and are only
     * ever compared with one another, since all that is ever read off a shape is which of two of its sides
     * is the more crowded, see {@link Steric}.
     *
     * @param molecule molecule being built
     * @param positions the coordinates
     * @param pairs the pairs of atoms that are not joined, with the distance they are kept apart at
     * @param ring which atoms lie in a ring
     * @return the strain, lower for a better shape
     */
    private static double strain(Molecule molecule, double[][] positions, int[][] pairs,
            boolean[] ring) {
        double total = 0.0;
        for (Bond bond : molecule.bonds()) {
            double length = Geometry.distance(positions[bond.first()], positions[bond.second()]);
            total += 60.0 * square(length - bondLength(molecule, bond.first(), bond.second()));
        }
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            List<Integer> neighbours = molecule.neighbours(atom);
            double ideal = bondAngle(molecule, atom);
            for (int first = 0; first < neighbours.size(); first++) {
                for (int second = first + 1; second < neighbours.size(); second++) {
                    double angle = Geometry.angle(positions[neighbours.get(first)], positions[atom],
                            positions[neighbours.get(second)]);
                    total += 0.05 * square(angle - ideal);
                }
            }
        }
        total += torsionStrain(molecule, positions, ring);
        for (int[] pair : pairs) {
            double apart = Geometry.distance(positions[pair[0]], positions[pair[1]]);
            double touch = pair[2] / 100.0;
            if (apart < touch) {
                total += 20.0 * square(touch - apart);
            }
        }
        return total;
    }

    /**
     * What the turns of the bonds cost.
     * <p>
     * <b>A single bond turns to the staggered turns and a double bond does not turn at all.</b> A bond held
     * by two atoms is a plane: E says the two halves stand across from one another and Z says they stand side
     * by side, and a bond held by an aromatic ring is a plane whichever way the string wrote it. What is left
     * is the single bond of a chain, which is happiest at every third turn - which is what makes a chain come
     * out of the building staggered instead of spread.
     *
     * @param molecule molecule being built
     * @param positions the coordinates
     * @param ring which atoms lie in a ring
     * @return the strain of the turns
     */
    private static double torsionStrain(Molecule molecule, double[][] positions, boolean[] ring) {
        double total = 0.0;
        for (Bond bond : molecule.bonds()) {
            List<Integer> from = new ArrayList<>(molecule.neighbours(bond.first()));
            List<Integer> to = new ArrayList<>(molecule.neighbours(bond.second()));
            from.remove(Integer.valueOf(bond.second()));
            to.remove(Integer.valueOf(bond.first()));
            if (from.isEmpty() || to.isEmpty()) {
                continue;
            }
            double torsion = Geometry.dihedral(positions[from.get(0)], positions[bond.first()],
                    positions[bond.second()], positions[to.get(0)]);
            if (bond.isAromatic()) {
                total += 20.0 * (1.0 - Math.cos(Math.toRadians(2.0 * torsion)));
            } else if (bond.order() == 2) {
                if (Cip.descriptor(molecule, bond) == 'Z') {
                    total += 20.0 * (1.0 - Math.cos(Math.toRadians(torsion)));
                } else {
                    total += 20.0 * (1.0 + Math.cos(Math.toRadians(torsion)));
                }
            } else if (bond.order() == 1 && !ring[bond.first()] && !ring[bond.second()]) {
                total += 0.6 * (1.0 + Math.cos(Math.toRadians(3.0 * torsion)));
            }
        }
        return total;
    }

    /** The pairs of atoms that are not joined and stand within reach of one another. */
    private static int[][] touchingPairs(Molecule molecule, boolean[] ring) {
        List<int[]> pairs = new ArrayList<>();
        for (int first = 0; first < molecule.atomCount(); first++) {
            for (int second = first + 1; second < molecule.atomCount(); second++) {
                if (isNear(molecule, first, second) || (ring[first] && ring[second])) {
                    // Two atoms that share an atom, and the any two of a ring, are held where they stand by
                    // the bonds between them; what is measured here is what is left over.
                    continue;
                }
                boolean light = molecule.atom(first).element().equals("H")
                        || molecule.atom(second).element().equals("H");
                double touch = light ? LIGHT_TOUCH : HEAVY_TOUCH;
                pairs.add(new int[] {first, second, (int) Math.round(touch * 100.0)});
            }
        }
        return pairs.toArray(new int[0][]);
    }

    /** {@code true} when two atoms are joined, or are both joined to an atom in common. */
    private static boolean isNear(Molecule molecule, int first, int second) {
        for (int index : molecule.bondsOf(first)) {
            if (molecule.bonds().get(index).other(first) == second) {
                return true;
            }
        }
        for (int neighbour : molecule.neighbours(first)) {
            for (int other : molecule.neighbours(second)) {
                if (neighbour == other) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The hand of every marked centre of a shape. */
    private static int[] handsOf(Molecule molecule, double[][] positions) {
        int[] hands = new int[molecule.atomCount()];
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            hands[atom] = handOf(molecule, positions, atom);
        }
        return hands;
    }

    /**
     * {@code true} for every atom that is a ligand of a centre that carries a mark.
     * <p>
     * <b>The ligands of a marked centre are held where the building stood them.</b> The tetrahedron of such a
     * centre was laid down from the mark itself and is already the right shape; moving one of its four corners
     * to make some other part of the molecule happier would turn the centre over, and a centre turned over is
     * a molecule that is not the one the string asked for. What is left of the molecule settles around them.
     *
     * @param molecule molecule being built
     * @return one flag per atom
     */
    private static boolean[] markedLigands(Molecule molecule) {
        boolean[] held = new boolean[molecule.atomCount()];
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            if (molecule.atom(atom).chirality() == Atom.NO_CHIRALITY) {
                continue;
            }
            for (int ligand : Cip.ligands(molecule, atom)) {
                if (ligand != Cip.HYDROGEN) {
                    held[ligand] = true;
                }
            }
        }
        return held;
    }

    /**
     * The hand of one centre of a shape, as the sign of the volume its stood ligands make.
     * <p>
     * <b>The sign is what is kept and never the configuration.</b> Which of R and S a centre is follows from
     * the priority of its ligands, which is a matter of the graph and cannot change while the shape moves;
     * what a move could do is turn the whole centre over, and that changes the sign of the volume its ligands
     * make - so the sign of every marked centre is read before the settling and none of them may change.
     *
     * @param molecule molecule being built
     * @param positions the coordinates
     * @param centre the atom
     * @return {@code 1} or {@code -1} for a marked centre, {@code 0} for one that carries no mark
     */
    private static int handOf(Molecule molecule, double[][] positions, int centre) {
        if (molecule.atom(centre).chirality() == Atom.NO_CHIRALITY) {
            return 0;
        }
        List<double[]> stood = new ArrayList<>();
        for (int ligand : Cip.ligands(molecule, centre)) {
            if (ligand != Cip.HYDROGEN) {
                stood.add(Geometry.minus(positions[ligand], positions[centre]));
            }
        }
        if (stood.size() < 3) {
            return 0;
        }
        double volume = Geometry.dot(Geometry.cross(stood.get(0), stood.get(1)), stood.get(2));
        return volume > 0.0 ? 1 : -1;
    }

    /** {@code true} when every marked centre of a shape still has the hand it had. */
    private static boolean keepsItsHands(Molecule molecule, double[][] positions, int[] hands) {
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            if (hands[atom] != 0 && handOf(molecule, positions, atom) != hands[atom]) {
                return false;
            }
        }
        return true;
    }

    /** {@code true} for every atom that lies in a ring of a molecule. */
    private static boolean[] inRings(Molecule molecule) {
        boolean[] ring = new boolean[molecule.atomCount()];
        for (List<Integer> cycle : Rings.cycles(molecule)) {
            for (int atom : cycle) {
                ring[atom] = true;
            }
        }
        return ring;
    }
}

