package com.philia093.neofactory.chemistry;

/**
 * The arithmetic of three dimensions, the little of it the shape of a molecule needs.
 * <p>
 * A shape is a list of coordinates and every question asked of one - how far apart two atoms stand, what
 * angle three make, which way a bond is turned - is one of a handful of small sums. They are written here
 * once so that the building of a shape and the reading of it do not each have their own, and every one of
 * them takes arrays of three as they are and never looks inside a molecule.
 */
final class Geometry {

    /** The origin, for a caller that has nothing to move from. */
    static final double[] ZERO = {0.0, 0.0, 0.0};

    private Geometry() {
        // Utility class: never instantiated.
    }

    /** The vector from one point to another. */
    static double[] minus(double[] to, double[] from) {
        return new double[] {to[0] - from[0], to[1] - from[1], to[2] - from[2]};
    }

    /** A point moved along a vector. */
    static double[] plus(double[] point, double[] move) {
        return new double[] {point[0] + move[0], point[1] + move[1], point[2] + move[2]};
    }

    /** A vector scaled. */
    static double[] scale(double[] vector, double factor) {
        return new double[] {vector[0] * factor, vector[1] * factor, vector[2] * factor};
    }

    /** A vector turned the other way. */
    static double[] opposite(double[] vector) {
        return scale(vector, -1.0);
    }

    /** How long a vector is. */
    static double length(double[] vector) {
        return Math.sqrt(dot(vector, vector));
    }

    /** A vector of length one, or the x axis when the vector has no length at all. */
    static double[] unit(double[] vector) {
        double length = length(vector);
        if (length < 1.0e-9) {
            return new double[] {1.0, 0.0, 0.0};
        }
        return scale(vector, 1.0 / length);
    }

    /** The sum of the products of two vectors, term by term. */
    static double dot(double[] first, double[] second) {
        return first[0] * second[0] + first[1] * second[1] + first[2] * second[2];
    }

    /** The vector standing square on the two others. */
    static double[] cross(double[] first, double[] second) {
        return new double[] {
            first[1] * second[2] - first[2] * second[1],
            first[2] * second[0] - first[0] * second[2],
            first[0] * second[1] - first[1] * second[0]};
    }

    /** How far apart two points stand. */
    static double distance(double[] first, double[] second) {
        return length(minus(first, second));
    }

    /** The angle two points make about a corner, in degrees. */
    static double angle(double[] first, double[] corner, double[] second) {
        double[] one = minus(first, corner);
        double[] other = minus(second, corner);
        double product = dot(one, other);
        double lengths = length(one) * length(other);
        if (lengths < 1.0e-9) {
            return 0.0;
        }
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, product / lengths))));
    }

    /**
     * The torsion of four points: the angle between the plane of the first three and the plane of the last
     * three, as seen looking along the bond the middle two make.
     *
     * @param first the first point
     * @param second the second
     * @param third the third
     * @param fourth the fourth
     * @return the torsion in degrees, between {@code -180} and {@code 180}, {@code 0} when a plane is not one
     */
    static double dihedral(double[] first, double[] second, double[] third, double[] fourth) {
        double[] axis = minus(third, second);
        if (length(axis) < 1.0e-9) {
            return 0.0;
        }
        double[] one = minus(first, second);
        double[] other = minus(fourth, third);
        double[] across = cross(axis, cross(one, axis));
        double[] along = cross(axis, cross(other, axis));
        double product = dot(across, along);
        double lengths = length(across) * length(along);
        if (lengths < 1.0e-9) {
            return 0.0;
        }
        double sine = dot(cross(across, along), axis) / (lengths * length(axis));
        double angle = Math.atan2(sine, product / lengths);
        return Math.toDegrees(angle);
    }

    /** A vector standing square on another, of length one. */
    static double[] anyPerpendicular(double[] vector) {
        double[] other = Math.abs(vector[0]) < 0.9 ? new double[] {1.0, 0.0, 0.0}
                : new double[] {0.0, 1.0, 0.0};
        return unit(cross(vector, other));
    }

    /**
     * A vector turned about an axis, by the rule of Rodrigues.
     *
     * @param vector the vector to turn
     * @param axis the axis to turn it about, of any length but not none
     * @param degrees how far to turn it
     * @return the vector turned
     */
    static double[] turned(double[] vector, double[] axis, double degrees) {
        double[] unit = unit(axis);
        double angle = Math.toRadians(degrees);
        double cosine = Math.cos(angle);
        double sine = Math.sin(angle);
        double[] first = scale(vector, cosine);
        double[] second = scale(cross(unit, vector), sine);
        double[] third = scale(unit, dot(unit, vector) * (1.0 - cosine));
        return plus(plus(first, second), third);
    }
}
