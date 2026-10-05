package com.philia093.neofactory.chemistry;

import java.util.List;
import java.util.Objects;

/**
 * A molecule laid out on a page: where every atom stands, and which rings the drawing is built around.
 * <p>
 * The value is the seam between working out a drawing and painting it. Everything that is a matter of
 * chemistry and of geometry lives in it - the atoms, the places they stand, the rings the molecule is made
 * of - while nothing in it knows about a pixel, a colour or a font. A renderer is handed one of these and
 * paints it; a test is handed one and checks the geometry without ever opening a picture.
 * <p>
 * <b>The coordinates are measured in bond lengths.</b> One bond of a molecule is one unit long, so a ring
 * of six atoms is a hexagon of side one and the number of a coordinate says nothing about how large the
 * drawing will be; the renderer chooses the scale that fits a picture and the layout stays the same. The
 * vertical axis points up, the way the geometry of a molecule is drawn by hand, and a renderer that counts
 * its rows downwards flips it once.
 */
public final class SkeletalStructure {

    private final Molecule molecule;
    private final double[] x;
    private final double[] y;
    private final List<List<Integer>> rings;
    private final double minX;
    private final double minY;
    private final double maxX;
    private final double maxY;

    SkeletalStructure(Molecule molecule, double[] x, double[] y, List<List<Integer>> rings) {
        this.molecule = Objects.requireNonNull(molecule, "molecule");
        this.x = x.clone();
        this.y = y.clone();
        this.rings = List.copyOf(Objects.requireNonNull(rings, "rings"));
        double lowX = Double.POSITIVE_INFINITY;
        double lowY = Double.POSITIVE_INFINITY;
        double highX = Double.NEGATIVE_INFINITY;
        double highY = Double.NEGATIVE_INFINITY;
        for (int atom = 0; atom < this.x.length; atom++) {
            lowX = Math.min(lowX, this.x[atom]);
            lowY = Math.min(lowY, this.y[atom]);
            highX = Math.max(highX, this.x[atom]);
            highY = Math.max(highY, this.y[atom]);
        }
        if (this.x.length == 0) {
            lowX = 0.0;
            lowY = 0.0;
            highX = 0.0;
            highY = 0.0;
        }
        this.minX = lowX;
        this.minY = lowY;
        this.maxX = highX;
        this.maxY = highY;
    }

    /** The molecule this drawing is of. */
    public Molecule molecule() {
        return molecule;
    }

    /** How many atoms the drawing holds. */
    public int atomCount() {
        return x.length;
    }

    /**
     * Where an atom stands, across the page.
     *
     * @param atom index of the atom
     * @return the horizontal coordinate, in bond lengths
     */
    public double x(int atom) {
        return x[atom];
    }

    /**
     * Where an atom stands, up the page.
     *
     * @param atom index of the atom
     * @return the vertical coordinate, in bond lengths, positive upwards
     */
    public double y(int atom) {
        return y[atom];
    }

    /** The rings the drawing is built around, each one the atoms of a ring in bond order. */
    public List<List<Integer>> rings() {
        return rings;
    }

    /** Left edge of the drawing. */
    public double minX() {
        return minX;
    }

    /** Lower edge of the drawing. */
    public double minY() {
        return minY;
    }

    /** Right edge of the drawing. */
    public double maxX() {
        return maxX;
    }

    /** Upper edge of the drawing. */
    public double maxY() {
        return maxY;
    }

    /** How wide the drawing is, in bond lengths. */
    public double width() {
        return maxX - minX;
    }

    /** How tall the drawing is, in bond lengths. */
    public double height() {
        return maxY - minY;
    }
}
