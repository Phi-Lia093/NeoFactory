package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The priority of the ligands of a stereocentre and the configuration that follows from it: the R and S of a
 * tetrahedral atom and the E and Z of a double bond.
 * <p>
 * A molecule that is read with a chirality mark does not yet know whether it is the left hand or the right:
 * the mark only says which way the neighbours were <em>listed</em>, and which of R and S that turns out to
 * be depends on what the neighbours are. The rules of Cahn, Ingold and Prelog order the four ligands of an
 * atom - the highest atomic number first, and where two ligands start with the same atom, the comparison
 * carried out one sphere further out - and the ordered four decide the configuration.
 * <p>
 * <b>The order is found by refining a description of every atom over and over.</b> Every atom begins with the
 * atomic number of its element and is then described again and again by what its neighbours were described
 * as, until a round changes nothing. Two ligands that end up with the same description are the same to the
 * rules of priority, which is what tells a real stereocentre from an atom that merely has four bonds; the
 * refinement is run over the whole molecule once and the four ligands are read off it.
 * <p>
 * <b>Which configuration the four ligands make is settled by a scalar triple product and not by a picture.</b>
 * The four ligands of a tetrahedral atom are put at the corners of a tetrahedron in the order they were
 * listed, {@code @} one way round and {@code @@} the other, and the sign of the volume of the tetrahedron
 * their four ordered vectors make says whether the arrangement is R or S. It is arithmetic, so it may be
 * checked, and it never depends on how the molecule happens to be laid out on a page.
 */
public final class Cip {

    /** The ligand an implicit hydrogen stands for, since a hydrogen is not an atom of the graph. */
    public static final int HYDROGEN = -1;

    /** Priority of a hydrogen, below every atom of the periodic table. */
    private static final int HYDROGEN_RANK = -1;

    /** How close to zero the volume of a tetrahedron may be and still be a flat, and not a centre. */
    private static final double FLAT = 1e-9;

    private Cip() {
        // Utility class: never instantiated.
    }

    /**
     * The four ligands of a tetrahedral atom, in the order the string listed them.
     *
     * @param molecule molecule to read
     * @param atom index of the atom
     * @return the four ligands, {@link #HYDROGEN} for an implicit hydrogen, or {@code null} when the atom
     *         does not have four of them
     */
    public static int[] ligands(Molecule molecule, int atom) {
        Atom element = molecule.atom(atom);
        Set<Integer> bonded = new HashSet<>(molecule.neighbours(atom));
        List<Integer> ligands = new ArrayList<>();
        Set<Integer> named = new HashSet<>();
        for (int neighbour : element.writtenNeighbours()) {
            // A bond that was broken after the molecule was written no longer names a ligand.
            if (bonded.contains(neighbour) && named.add(neighbour)) {
                ligands.add(neighbour);
            }
        }
        // A bond that was made after the molecule was written is not in the listing, so it is added here;
        // where it stands does not matter, because a mark is worked out from the arrangement either way.
        List<Integer> unlisted = new ArrayList<>();
        for (int neighbour : molecule.neighbours(atom)) {
            if (!named.contains(neighbour)) {
                unlisted.add(neighbour);
            }
        }
        Collections.sort(unlisted);
        ligands.addAll(unlisted);
        if (element.hydrogens() > 0) {
            ligands.add(HYDROGEN);
        }
        if (ligands.size() != 4) {
            return null;
        }
        int[] four = new int[4];
        for (int index = 0; index < 4; index++) {
            four[index] = ligands.get(index);
        }
        return four;
    }

    /**
     * The priority of every atom of a molecule, higher for a ligand of higher priority.
     * <p>
     * The description of an atom is the atomic number of its element and, over and over, what its neighbours
     * were described as - the same refinement a canonical labelling uses, except that an atom is ranked by
     * the description itself and not by where it stands in it. An implicit hydrogen is a neighbour of the
     * lowest description there is, which is what puts a hydrogen below every other ligand.
     *
     * @param molecule molecule to rank
     * @return the rank of every atom
     */
    public static int[] ranks(Molecule molecule) {
        int count = molecule.atomCount();
        int[] rank = new int[count];
        for (int atom = 0; atom < count; atom++) {
            rank[atom] = Elements.atomicNumber(molecule.atom(atom).element());
        }
        for (int step = 0; step <= count; step++) {
            final int[] base = rank;
            Integer[] order = new Integer[count];
            for (int atom = 0; atom < count; atom++) {
                order[atom] = atom;
            }
            Arrays.sort(order, (first, second) -> compareKey(molecule, base, first, second));
            int[] fresh = new int[count];
            int dense = 0;
            for (int index = 0; index < count; index++) {
                if (index > 0 && compareKey(molecule, base, order[index - 1], order[index]) != 0) {
                    dense++;
                }
                fresh[order[index]] = dense;
            }
            rank = fresh;
        }
        return rank;
    }

    /** Compares two atoms under the descriptions one round old. */
    private static int compareKey(Molecule molecule, int[] base, int first, int second) {
        int byAtom = Integer.compare(base[first], base[second]);
        if (byAtom != 0) {
            return byAtom;
        }
        int[] firstSphere = neighbourRanks(molecule, first, base);
        int[] secondSphere = neighbourRanks(molecule, second, base);
        int length = Math.min(firstSphere.length, secondSphere.length);
        for (int index = 0; index < length; index++) {
            int byNeighbour = Integer.compare(firstSphere[index], secondSphere[index]);
            if (byNeighbour != 0) {
                return byNeighbour;
            }
        }
        return Integer.compare(firstSphere.length, secondSphere.length);
    }

    /** The descriptions of the neighbours of an atom, the highest first, hydrogen included. */
    private static int[] neighbourRanks(Molecule molecule, int atom, int[] base) {
        List<Integer> ranks = new ArrayList<>();
        for (int neighbour : molecule.neighbours(atom)) {
            ranks.add(base[neighbour]);
        }
        for (int count = 0; count < molecule.atom(atom).hydrogens(); count++) {
            ranks.add(HYDROGEN_RANK);
        }
        int[] sphere = new int[ranks.size()];
        for (int index = 0; index < sphere.length; index++) {
            sphere[index] = ranks.get(index);
        }
        Arrays.sort(sphere);
        for (int index = 0; index < sphere.length / 2; index++) {
            int swap = sphere[index];
            sphere[index] = sphere[sphere.length - 1 - index];
            sphere[sphere.length - 1 - index] = swap;
        }
        return sphere;
    }

    /** The priority of one ligand under a set of ranks. */
    public static int priority(int[] ranks, int ligand) {
        return ligand == HYDROGEN ? HYDROGEN_RANK : ranks[ligand];
    }

    /**
     * The configuration of a tetrahedral atom, {@code R} or {@code S}.
     *
     * @param molecule molecule to read
     * @param atom index of the atom
     * @return {@code 'R'}, {@code 'S'}, or {@code 0} when the atom is no marked stereocentre
     */
    public static char configuration(Molecule molecule, int atom) {
        int chirality = molecule.atom(atom).chirality();
        if (chirality == Atom.NO_CHIRALITY) {
            return 0;
        }
        return configurationFor(molecule, atom, chirality);
    }

    /**
     * The configuration an atom would come out with if it were written the given way.
     *
     * @param molecule molecule to read
     * @param atom index of the atom
     * @param chirality the way the neighbours are taken to be written, one of the two or none
     * @return {@code 'R'}, {@code 'S'}, or {@code 0} when the atom is no centre
     */
    private static char configurationFor(Molecule molecule, int atom, int chirality) {
        int[] ligands = ligands(molecule, atom);
        if (ligands == null) {
            return 0;
        }
        int[] ranks = ranks(molecule);
        if (!distinct(ranks, ligands)) {
            return 0;
        }
        double[][] place = tetrahedron(chirality);
        Integer[] order = {0, 1, 2, 3};
        Arrays.sort(order, (first, second) -> Integer.compare(priority(ranks, ligands[second]),
                priority(ranks, ligands[first])));
        double volume = volume(place[order[0]], place[order[1]], place[order[2]], place[order[3]]);
        if (volume > FLAT) {
            return 'S';
        }
        return volume < -FLAT ? 'R' : 0;
    }

    /**
     * The mark an atom has to be written with to come out with a configuration.
     * <p>
     * A step of a reaction does not know which way round the neighbours of the atom it built will end up
     * being listed - that is a fact about the walk that wrote the molecule - but it does know which hand it
     * means to make. The answer is therefore the mark that names that hand under the listing the molecule
     * really has, which is what lets a rule say "this carbon comes out S" and be obeyed.
     *
     * @param molecule molecule to read
     * @param atom index of the atom
     * @param configuration the hand wanted, {@code 'R'} or {@code 'S'}
     * @return {@link Atom#CHIRAL_ONE} or {@link Atom#CHIRAL_TWO}, or {@link Atom#NO_CHIRALITY} when the atom
     *         is no centre at all
     */
    public static int markFor(Molecule molecule, int atom, char configuration) {
        if (configurationFor(molecule, atom, Atom.CHIRAL_ONE) == configuration) {
            return Atom.CHIRAL_ONE;
        }
        if (configurationFor(molecule, atom, Atom.CHIRAL_TWO) == configuration) {
            return Atom.CHIRAL_TWO;
        }
        return Atom.NO_CHIRALITY;
    }

    /**
     * The configuration a flat centre takes when a ligand comes in from one of its faces.
     * <p>
     * An atom joined by a double bond is flat: its three ligands stand in one plane and the incoming one
     * comes from above or below it. The three are put in the order of their priority and spread a third of a
     * turn apart, the incoming ligand is set down in front, and which way round the three spread is the whole
     * difference between the two faces, see {@link Face}. The four ligands that result are then ordered by
     * their priority and the sign of the volume of the tetrahedron decides R or S, exactly as it does for a
     * centre that was written down, see {@link #configuration}.
     *
     * @param molecule molecule the attack was carried out on, the incoming ligand included
     * @param centre index of the flat atom that was attacked
     * @param incoming index of the ligand that came in
     * @param face the face it came in from
     * @return {@code 'R'}, {@code 'S'}, or {@code 0} when the centre does not have three other ligands
     */
    public static char afterFace(Molecule molecule, int centre, int incoming, Face face) {
        List<Integer> others = new ArrayList<>();
        for (int neighbour : molecule.neighbours(centre)) {
            if (neighbour != incoming) {
                others.add(neighbour);
            }
        }
        if (molecule.atom(centre).hydrogens() > 0) {
            others.add(HYDROGEN);
        }
        if (others.size() != 3) {
            return 0;
        }
        int[] ranks = ranks(molecule);
        others.sort((first, second) -> Integer.compare(priority(ranks, second), priority(ranks, first)));
        double radius = 2.0 * Math.sqrt(2.0) / 3.0;
        double turn = face == Face.RE ? -1.0 : 1.0;
        double[][] corners = new double[4][3];
        corners[0] = new double[] {0.0, 0.0, 1.0};
        for (int index = 0; index < 3; index++) {
            double angle = turn * index * 2.0 * Math.PI / 3.0;
            corners[index + 1] = new double[] {radius * Math.cos(angle), radius * Math.sin(angle),
                    -1.0 / 3.0};
        }
        int[] ligands = {incoming, others.get(0), others.get(1), others.get(2)};
        Integer[] order = {0, 1, 2, 3};
        Arrays.sort(order, (first, second) -> Integer.compare(priority(ranks, ligands[second]),
                priority(ranks, ligands[first])));
        double volume = volume(corners[order[0]], corners[order[1]], corners[order[2]], corners[order[3]]);
        if (volume > FLAT) {
            return 'S';
        }
        return volume < -FLAT ? 'R' : 0;
    }

    /**
     * The atoms of a molecule that are tetrahedral stereocentres.
     * <p>
     * A centre has four ligands that the rules of priority tell apart, is not part of an aromatic ring and
     * hangs on single bonds only: an atom with two bonds of the same kind is flat, and a centre that is flat
     * is no centre at all. Whether the atom was written with a mark is not asked here, so an unmarked centre
     * is found as well, which is what a rule of a reaction needs to know before it decides to racemise one.
     *
     * @param molecule molecule to look over
     * @return the indices of the centres
     */
    public static List<Integer> tetrahedral(Molecule molecule) {
        int[] ranks = ranks(molecule);
        List<Integer> centres = new ArrayList<>();
        for (int atom = 0; atom < molecule.atomCount(); atom++) {
            int[] ligands = ligands(molecule, atom);
            if (ligands == null || molecule.atom(atom).isAromatic() || !sp3(molecule, atom)) {
                continue;
            }
            if (distinct(ranks, ligands)) {
                centres.add(atom);
            }
        }
        return centres;
    }

    /** {@code true} when an atom hangs on single bonds only. */
    private static boolean sp3(Molecule molecule, int atom) {
        for (int bondIndex : molecule.bondsOf(atom)) {
            if (molecule.bonds().get(bondIndex).order() != 1) {
                return false;
            }
        }
        return true;
    }

    /** {@code true} when the four ligands are told apart by the rules of priority. */
    private static boolean distinct(int[] ranks, int[] ligands) {
        for (int first = 0; first < 4; first++) {
            for (int second = first + 1; second < 4; second++) {
                if (priority(ranks, ligands[first]) == priority(ranks, ligands[second])) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * The configuration of a double bond, {@code E} or {@code Z}.
     * <p>
     * A double bond is not turned by a mark of its own but by the marks of the bonds that leave its two ends:
     * the language writes {@code /} and {@code \} on the substituents and says thereby which side of the
     * bond the substituents stand on. Two marks that point the same way put the two substituents on opposite
     * sides, which is the E of the industry, and two that point apart put them on the same side, the Z.
     *
     * @param molecule molecule to read
     * @param bond the bond
     * @return {@code 'E'}, {@code 'Z'}, or {@code 0} when the bond is no marked double bond
     */
    public static char descriptor(Molecule molecule, Bond bond) {
        if (bond.order() != 2 || bond.isAromatic()) {
            return 0;
        }
        int[] first = marked(molecule, bond.first(), bond.second());
        int[] second = marked(molecule, bond.second(), bond.first());
        if (first == null || second == null) {
            return 0;
        }
        return (char) first[1] == (char) second[1] ? 'E' : 'Z';
    }

    /** The substituent of one end of a double bond that carries a mark, and the mark, or {@code null}. */
    private static int[] marked(Molecule molecule, int end, int other) {
        for (int bondIndex : molecule.bondsOf(end)) {
            Bond bond = molecule.bonds().get(bondIndex);
            if (bond.other(end) == other || bond.stereo() == Bond.NO_STEREO) {
                continue;
            }
            return new int[] {bond.other(end), bond.stereo()};
        }
        return null;
    }

    /**
     * The corners of a tetrahedron for the four neighbours of a centre, in the order they were listed.
     * <p>
     * The first ligand stands in front and the other three behind it, spread a third of a turn apart; the two
     * marks are the two ways round that spreading may go, which is the whole of what {@code @} and
     * {@code @@} mean.
     *
     * @param chirality {@link Atom#CHIRAL_ONE} or {@link Atom#CHIRAL_TWO}
     * @return one corner per ligand, in the order of the ligands
     */
    private static double[][] tetrahedron(int chirality) {
        double radius = 2.0 * Math.sqrt(2.0) / 3.0;
        double turn = chirality == Atom.CHIRAL_ONE ? 1.0 : -1.0;
        double[][] corners = new double[4][3];
        corners[0] = new double[] {0.0, 0.0, 1.0};
        corners[1] = new double[] {radius, 0.0, -1.0 / 3.0};
        corners[2] = new double[] {radius * Math.cos(turn * 2.0 * Math.PI / 3.0),
                radius * Math.sin(turn * 2.0 * Math.PI / 3.0), -1.0 / 3.0};
        corners[3] = new double[] {radius * Math.cos(turn * 4.0 * Math.PI / 3.0),
                radius * Math.sin(turn * 4.0 * Math.PI / 3.0), -1.0 / 3.0};
        return corners;
    }

    /**
     * The signed volume of the tetrahedron four corners make, the sign that says R or S.
     * <p>
     * The three highest ligands are taken as they stand away from the lowest, and the sign of the volume of
     * the three vectors they make is the sign of the arrangement: positive is the one the four ligands of a
     * centre are written S in, negative the one they are written R in.
     *
     * @param highest corner of the highest ligand
     * @param second corner of the second
     * @param third corner of the third
     * @param lowest corner of the lowest
     * @return the signed volume
     */
    private static double volume(double[] highest, double[] second, double[] third, double[] lowest) {
        double[] first = away(highest, lowest);
        double[] other = away(second, lowest);
        double[] last = away(third, lowest);
        return first[0] * (other[1] * last[2] - other[2] * last[1])
                - first[1] * (other[0] * last[2] - other[2] * last[0])
                + first[2] * (other[0] * last[1] - other[1] * last[0]);
    }

    /** One corner seen from another. */
    private static double[] away(double[] corner, double[] from) {
        return new double[] {corner[0] - from[0], corner[1] - from[1], corner[2] - from[2]};
    }
}
