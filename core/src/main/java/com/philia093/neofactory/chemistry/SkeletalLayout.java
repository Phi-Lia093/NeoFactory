package com.philia093.neofactory.chemistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Lays a molecule out on a page the way a chemist draws it, as a skeleton of bonds and no carbons.
 * <p>
 * A skeletal formula is the drawing an organic chemist reads instead of a formula: every vertex is a carbon,
 * a hydrogen is understood and not written, and the only letters on the page are the atoms that are not
 * carbon. Turning a molecule into such a drawing is two questions at once - where every atom stands and
 * what the picture will therefore look like - and only the first is asked here: the atoms are placed on a
 * page, in bond lengths, and what is painted from that is the business of a renderer.
 * <p>
 * <b>Rings are drawn as rings.</b> The rings of a molecule are found first, see {@link Rings}, and the
 * largest of them is laid down as a regular polygon so that a hexagon comes out a hexagon and not a chain
 * that happens to meet itself. A ring fused to one already placed is set down along the bond the two share,
 * a ring that hangs off a single atom is turned until it stands clear, and everything else grows outward
 * from the ring in the sixty degree steps a chain of carbon is drawn in.
 * <p>
 * <b>The layout is deterministic and is measured in bond lengths.</b> The same molecule is always laid out
 * the same way, because every choice - which ring goes first, which way a ring is turned, which side a
 * substituent takes - is settled by a rule and never by a hash order, so a drawing may be checked by its
 * numbers and not only by the eye. One bond is one unit long, whatever the picture afterwards costs.
 */
public final class SkeletalLayout {

    /** Length of one bond in the coordinates a layout hands out. */
    public static final double BOND_LENGTH = 1.0;

    /** How far two atoms that share no bond have to stand apart, in bond lengths. */
    private static final double MINIMUM_CLEARANCE = 0.7;

    /** Space between two molecules that were written with a dot, in bond lengths. */
    private static final double PIECE_GAP = 1.5;

    /** Angle a chain turns by at every atom, the sixty degrees a skeleton of carbon is drawn in. */
    private static final double TURN = Math.PI / 3.0;

    /** Directions a ring hanging off a single atom is tried in, spread around the circle. */
    private static final int VERTEX_DIRECTIONS = 24;

    private SkeletalLayout() {
        // Utility class: never instantiated.
    }

    /**
     * Lays a molecule out from the line it is written in.
     *
     * @param smiles the SMILES string of the molecule
     * @return the drawing
     * @throws SmilesException when the string cannot be read
     */
    public static SkeletalStructure layout(String smiles) {
        return layout(SmilesParser.parse(smiles));
    }

    /**
     * Lays a molecule out on a page.
     *
     * @param molecule molecule to lay out
     * @return the drawing, every bond one unit long
     */
    public static SkeletalStructure layout(Molecule molecule) {
        Objects.requireNonNull(molecule, "molecule");
        int count = molecule.atomCount();
        double[] x = new double[count];
        double[] y = new double[count];
        List<List<Integer>> rings = Rings.cycles(molecule);
        double shift = 0.0;
        for (List<Integer> piece : Rings.components(molecule)) {
            boolean[] placed = new boolean[count];
            placePiece(molecule, piece, ringsOf(piece, rings), x, y, placed);
            double lowX = Double.POSITIVE_INFINITY;
            double highX = Double.NEGATIVE_INFINITY;
            for (int atom : piece) {
                lowX = Math.min(lowX, x[atom]);
                highX = Math.max(highX, x[atom]);
            }
            double delta = shift - lowX;
            for (int atom : piece) {
                x[atom] += delta;
            }
            shift += (highX - lowX) + PIECE_GAP;
        }
        return new SkeletalStructure(molecule, x, y, rings);
    }

    /** The rings that lie inside one piece of a molecule. */
    private static List<List<Integer>> ringsOf(List<Integer> piece, List<List<Integer>> rings) {
        Set<Integer> atoms = new HashSet<>(piece);
        List<List<Integer>> inside = new ArrayList<>();
        for (List<Integer> ring : rings) {
            if (atoms.contains(ring.get(0))) {
                inside.add(ring);
            }
        }
        return inside;
    }

    /** Lays one connected piece of a molecule down, rings first and the rest around them. */
    private static void placePiece(Molecule molecule, List<Integer> piece, List<List<Integer>> pieceRings,
            double[] x, double[] y, boolean[] placed) {
        if (pieceRings.isEmpty()) {
            int root = rootOf(molecule, piece);
            x[root] = 0.0;
            y[root] = 0.0;
            placed[root] = true;
        } else {
            List<List<Integer>> ordered = new ArrayList<>(pieceRings);
            ordered.sort(Comparator.comparingInt((List<Integer> ring) -> ring.size()).reversed());
            placePolygon(ordered.get(0), x, y, placed, 0.0, 0.0, Math.PI / 2.0);
            for (int index = 1; index < ordered.size(); index++) {
                placeRing(ordered.get(index), x, y, placed);
            }
        }
        grow(molecule, piece, x, y, placed);
    }

    /** Sets a ring down against the atoms of it that stand somewhere already. */
    private static void placeRing(List<Integer> ring, double[] x, double[] y, boolean[] placed) {
        List<Integer> shared = new ArrayList<>();
        for (int atom : ring) {
            if (placed[atom]) {
                shared.add(atom);
            }
        }
        if (shared.size() >= 2) {
            List<Integer> ordered = orderedCycle(ring, shared.get(0), shared.get(1));
            if (ordered != null) {
                placePolygonByEdge(ordered, x, y, placed);
                return;
            }
        }
        if (!shared.isEmpty()) {
            placePolygonAtVertex(ring, shared.get(0), x, y, placed);
            return;
        }
        placePolygon(ring, x, y, placed, 0.0, 2.0 * BOND_LENGTH, 0.0);
    }

    /** Sets a ring down as a regular polygon, its atoms in bond order around the edge. */
    private static void placePolygon(List<Integer> ring, double[] x, double[] y, boolean[] placed,
            double centerX, double centerY, double startAngle) {
        int sides = ring.size();
        double radius = 1.0 / (2.0 * Math.sin(Math.PI / sides));
        for (int index = 0; index < sides; index++) {
            double angle = startAngle + index * 2.0 * Math.PI / sides;
            int atom = ring.get(index);
            x[atom] = centerX + radius * Math.cos(angle);
            y[atom] = centerY + radius * Math.sin(angle);
            placed[atom] = true;
        }
    }

    /**
     * The atoms of a ring written from one of them, so that two named atoms come out next to each other.
     *
     * @param ring the ring, in bond order
     * @param first atom the ring is to start at
     * @param second atom that has to follow it
     * @return the ring from {@code first} through {@code second}, or {@code null} when the two are not joined
     */
    private static List<Integer> orderedCycle(List<Integer> ring, int first, int second) {
        int size = ring.size();
        int at = ring.indexOf(first);
        if (at < 0) {
            return null;
        }
        boolean forward = ring.get((at + 1) % size) == second;
        boolean backward = ring.get((at + size - 1) % size) == second;
        if (!forward && !backward) {
            return null;
        }
        List<Integer> ordered = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            int step = forward ? at + index : at - index;
            ordered.add(ring.get(((step % size) + size) % size));
        }
        return ordered;
    }

    /**
     * Sets a fused ring down along the bond it shares with a ring that stands already.
     * <p>
     * The two shared atoms fix the first edge of the polygon and the rest follow around it in steps of the
     * same turn; of the two ways that turn may go, the one that leaves the new ring furthest from everything
     * else on the page is taken, which keeps two drawings of a fused pair from folding onto each other.
     *
     * @param ordered ring written from one shared atom through the other
     * @param x coordinates, written in place
     * @param y coordinates, written in place
     * @param placed which atoms stand somewhere already, written in place
     */
    private static void placePolygonByEdge(List<Integer> ordered, double[] x, double[] y, boolean[] placed) {
        int sides = ordered.size();
        double x0 = x[ordered.get(0)];
        double y0 = y[ordered.get(0)];
        double x1 = x[ordered.get(1)];
        double y1 = y[ordered.get(1)];
        Set<Integer> ringAtoms = new HashSet<>(ordered);
        double turn = 2.0 * Math.PI / sides;
        double[] bestX = null;
        double[] bestY = null;
        double best = Double.NEGATIVE_INFINITY;
        for (double sign : new double[] {1.0, -1.0}) {
            double[] trialX = new double[sides];
            double[] trialY = new double[sides];
            trialX[0] = x0;
            trialY[0] = y0;
            trialX[1] = x1;
            trialY[1] = y1;
            double edgeX = x1 - x0;
            double edgeY = y1 - y0;
            double cos = Math.cos(sign * turn);
            double sin = Math.sin(sign * turn);
            for (int index = 2; index < sides; index++) {
                double nextX = edgeX * cos - edgeY * sin;
                double nextY = edgeX * sin + edgeY * cos;
                trialX[index] = trialX[index - 1] + nextX;
                trialY[index] = trialY[index - 1] + nextY;
                edgeX = nextX;
                edgeY = nextY;
            }
            double score = Double.POSITIVE_INFINITY;
            for (int index = 2; index < sides; index++) {
                score = Math.min(score, clearance(trialX[index], trialY[index], x, y, placed, ringAtoms));
            }
            if (score > best) {
                best = score;
                bestX = trialX;
                bestY = trialY;
            }
        }
        for (int index = 0; index < sides; index++) {
            int atom = ordered.get(index);
            x[atom] = bestX[index];
            y[atom] = bestY[index];
            placed[atom] = true;
        }
    }

    /** Sets a ring down on a single atom it shares, turned until it stands clear of the rest of the page. */
    private static void placePolygonAtVertex(List<Integer> ring, int vertex, double[] x, double[] y,
            boolean[] placed) {
        int sides = ring.size();
        double radius = 1.0 / (2.0 * Math.sin(Math.PI / sides));
        Set<Integer> ringAtoms = new HashSet<>(ring);
        int at = ring.indexOf(vertex);
        double[] bestX = null;
        double[] bestY = null;
        double best = Double.NEGATIVE_INFINITY;
        for (int step = 0; step < VERTEX_DIRECTIONS; step++) {
            double direction = step * 2.0 * Math.PI / VERTEX_DIRECTIONS;
            double centerX = x[vertex] - radius * Math.cos(direction);
            double centerY = y[vertex] - radius * Math.sin(direction);
            double[] trialX = new double[sides];
            double[] trialY = new double[sides];
            for (int index = 0; index < sides; index++) {
                double angle = direction + (index - at) * 2.0 * Math.PI / sides;
                trialX[index] = centerX + radius * Math.cos(angle);
                trialY[index] = centerY + radius * Math.sin(angle);
            }
            double score = Double.POSITIVE_INFINITY;
            for (int index = 0; index < sides; index++) {
                score = Math.min(score, clearance(trialX[index], trialY[index], x, y, placed, ringAtoms));
            }
            if (score > best) {
                best = score;
                bestX = trialX;
                bestY = trialY;
            }
        }
        for (int index = 0; index < sides; index++) {
            int atom = ring.get(index);
            x[atom] = bestX[index];
            y[atom] = bestY[index];
            placed[atom] = true;
        }
    }

    /** Grows the rest of a piece outward from what already stands, one atom at a time. */
    private static void grow(Molecule molecule, List<Integer> piece, double[] x, double[] y,
            boolean[] placed) {
        Deque<Integer> pending = new ArrayDeque<>();
        for (int atom : piece) {
            if (placed[atom]) {
                pending.add(atom);
            }
        }
        while (!pending.isEmpty()) {
            int anchor = pending.poll();
            for (int neighbour : molecule.neighbours(anchor)) {
                if (placed[neighbour]) {
                    continue;
                }
                placeNeighbour(molecule, anchor, neighbour, x, y, placed);
                placed[neighbour] = true;
                pending.add(neighbour);
            }
        }
    }

    /** Places one atom one bond away from an atom that stands already, at the clearest angle. */
    private static void placeNeighbour(Molecule molecule, int anchor, int neighbour, double[] x, double[] y,
            boolean[] placed) {
        Set<Integer> ignore = Set.of(anchor, neighbour);
        double bestX = 0.0;
        double bestY = 0.0;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (double angle : candidateAngles(molecule, anchor, x, y, placed)) {
            double placeX = x[anchor] + BOND_LENGTH * Math.cos(angle);
            double placeY = y[anchor] + BOND_LENGTH * Math.sin(angle);
            double score = clearance(placeX, placeY, x, y, placed, ignore);
            if (score > bestScore) {
                bestScore = score;
                bestX = placeX;
                bestY = placeY;
            }
            if (score >= MINIMUM_CLEARANCE) {
                break;
            }
        }
        x[neighbour] = bestX;
        y[neighbour] = bestY;
    }

    /** The angles an atom is tried at, from the sixty degree steps a skeleton of carbon is drawn in. */
    private static double[] candidateAngles(Molecule molecule, int anchor, double[] x, double[] y,
            boolean[] placed) {
        List<Integer> standing = new ArrayList<>();
        for (int neighbour : molecule.neighbours(anchor)) {
            if (placed[neighbour]) {
                standing.add(neighbour);
            }
        }
        if (standing.isEmpty()) {
            return new double[] {Math.PI / 2.0};
        }
        if (standing.size() == 1) {
            int from = standing.get(0);
            double incoming = Math.atan2(y[anchor] - y[from], x[anchor] - x[from]);
            return new double[] {incoming + TURN, incoming - TURN, incoming, incoming + 2.0 * TURN,
                    incoming - 2.0 * TURN, incoming + Math.PI};
        }
        double meanX = 0.0;
        double meanY = 0.0;
        for (int neighbour : standing) {
            double dx = x[neighbour] - x[anchor];
            double dy = y[neighbour] - y[anchor];
            double length = Math.hypot(dx, dy);
            meanX += dx / length;
            meanY += dy / length;
        }
        double outward = Math.atan2(-meanY, -meanX);
        return new double[] {outward, outward + TURN, outward - TURN, outward + 2.0 * TURN,
                outward - 2.0 * TURN, outward + Math.PI};
    }

    /** How far a place stands from the nearest atom that already stands there, ignoring named atoms. */
    private static double clearance(double placeX, double placeY, double[] x, double[] y, boolean[] placed,
            Set<Integer> ignore) {
        double closest = Double.POSITIVE_INFINITY;
        for (int atom = 0; atom < x.length; atom++) {
            if (!placed[atom] || ignore.contains(atom)) {
                continue;
            }
            closest = Math.min(closest, Math.hypot(placeX - x[atom], placeY - y[atom]));
        }
        return closest;
    }

    /** The atom a chain of a piece is grown from: an end of it when there is one, the plainest atom otherwise. */
    private static int rootOf(Molecule molecule, List<Integer> piece) {
        for (int atom : piece) {
            if (molecule.neighbours(atom).size() == 1) {
                return atom;
            }
        }
        int root = piece.get(0);
        int fewest = Integer.MAX_VALUE;
        for (int atom : piece) {
            int degree = molecule.neighbours(atom).size();
            if (degree < fewest) {
                fewest = degree;
                root = atom;
            }
        }
        return root;
    }
}
