package com.philia093.neofactory.block.model;

import com.philia093.neofactory.block.BlockFace;

import java.util.Objects;

/**
 * One box of a model, with a picture for each of its six faces.
 * <p>
 * A block of the world is not always a whole cube: a plant is two crossed planes, a slab is half a
 * cube, a fence post is a thin box. A model is therefore a list of boxes, and one box is a piece of
 * the block between two corners, both of them given in sixteenths of a block - the unit the art of
 * the game is drawn in, so {@code 0} is one edge of the block and {@code 16} the other one.
 * <p>
 * <b>A box may be turned.</b> {@link #rotation()} turns the box around an axis that runs through
 * its origin, the way the crossed planes of a plant stand at forty five degrees. A box without a
 * rotation stands straight.
 * <p>
 * <b>Only a whole cube is culled.</b> The mesher hides a face of a box when a neighbour hides it,
 * and that question is only answered by a box that fills the block: the face of a slab does not lie
 * on the border of its block even when it looks in that direction. A box that is smaller than a
 * block therefore draws all of its faces, see {@link #isWholeCube()}.
 */
public final class ModelBox {

    /** Amount of sixteenths one block is measured in. */
    public static final float UNITS = 16.0f;

    private final float fromX;
    private final float fromY;
    private final float fromZ;
    private final float toX;
    private final float toY;
    private final float toZ;
    private final ModelFace[] faces = new ModelFace[BlockFace.ALL.length];
    private final Rotation rotation;
    private final boolean shade;

    /**
     * Creates a box.
     *
     * @param fromX lower corner along X, {@code 0} to {@code 16}
     * @param fromY lower corner along Y, {@code 0} to {@code 16}
     * @param fromZ lower corner along Z, {@code 0} to {@code 16}
     * @param toX upper corner along X, {@code fromX} to {@code 16}
     * @param toY upper corner along Y, {@code fromY} to {@code 16}
     * @param toZ upper corner along Z, {@code fromZ} to {@code 16}
     * @param rotation turn of this box, {@code null} for a box that stands straight
     * @param shade {@code true} when the light of a face falls off with its direction, which is
     *              what gives a cube its shape; a plant is drawn without it
     * @throws IllegalArgumentException when the corners do not form a box inside the block
     */
    public ModelBox(float fromX, float fromY, float fromZ, float toX, float toY, float toZ,
            Rotation rotation, boolean shade) {
        this.fromX = check(fromX, "from X");
        this.fromY = check(fromY, "from Y");
        this.fromZ = check(fromZ, "from Z");
        this.toX = check(toX, "to X");
        this.toY = check(toY, "to Y");
        this.toZ = check(toZ, "to Z");
        if (toX < fromX || toY < fromY || toZ < fromZ) {
            throw new IllegalArgumentException("A box reaches from its lower corner to a higher "
                    + "one: " + from(fromX, fromY, fromZ) + " to " + from(toX, toY, toZ));
        }
        if (toX == fromX && toY == fromY && toZ == fromZ) {
            throw new IllegalArgumentException("A box of '" + from(fromX, fromY, fromZ)
                    + "' has no size at all");
        }
        this.rotation = rotation;
        this.shade = shade;
    }

    /** Lower corner of this box along X, in sixteenths of a block. */
    public float fromX() {
        return fromX;
    }

    /** Lower corner of this box along Y, in sixteenths of a block. */
    public float fromY() {
        return fromY;
    }

    /** Lower corner of this box along Z, in sixteenths of a block. */
    public float fromZ() {
        return fromZ;
    }

    /** Upper corner of this box along X, in sixteenths of a block. */
    public float toX() {
        return toX;
    }

    /** Upper corner of this box along Y, in sixteenths of a block. */
    public float toY() {
        return toY;
    }

    /** Upper corner of this box along Z, in sixteenths of a block. */
    public float toZ() {
        return toZ;
    }

    /** Turn of this box, {@code null} for a box that stands straight. */
    public Rotation rotation() {
        return rotation;
    }

    /** {@code true} when the light of a face falls off with the direction it looks in. */
    public boolean shaded() {
        return shade;
    }

    /**
     * Names the picture of one face.
     *
     * @param face face to name
     * @param picture picture to draw there, {@code null} leaves the face out
     */
    public void setFace(BlockFace face, ModelFace picture) {
        faces[Objects.requireNonNull(face, "face").ordinal()] = picture;
    }

    /**
     * Picture of one face.
     *
     * @param face face to read
     * @return the face, or {@code null} when this box does not draw it
     */
    public ModelFace face(BlockFace face) {
        return faces[Objects.requireNonNull(face, "face").ordinal()];
    }

    /** {@code true} when this box draws the given face. */
    public boolean hasFace(BlockFace face) {
        return face(face) != null;
    }

    /** Amount of faces this box draws. */
    public int faceCount() {
        int count = 0;
        for (ModelFace face : faces) {
            if (face != null) {
                count++;
            }
        }
        return count;
    }

    /** {@code true} when this box is the whole block, standing straight, so a neighbour may hide it. */
    public boolean isWholeCube() {
        return rotation == null && fromX == 0.0f && fromY == 0.0f && fromZ == 0.0f
                && toX == UNITS && toY == UNITS && toZ == UNITS;
    }

    /**
     * Turn of a box around one of the three axes of the world.
     *
     * @param originX X of the axis the turn runs through, in sixteenths of a block
     * @param originY Y of the axis the turn runs through, in sixteenths of a block
     * @param originZ Z of the axis the turn runs through, in sixteenths of a block
     * @param axis axis the turn runs through, {@code 'x'}, {@code 'y'} or {@code 'z'}
     * @param angle degrees the box is turned by, positive is counter clockwise seen from the
     *              positive end of the axis
     */
    public record Rotation(float originX, float originY, float originZ, char axis, float angle) {

        /** Axes a box may be turned around, the ones a model file may name. */
        private static final String AXES = "xyz";

        /** Checks the turn, so a broken file fails while it is read. */
        public Rotation {
            if (AXES.indexOf(axis) < 0) {
                throw new IllegalArgumentException("A box is turned around x, y or z and not around "
                        + axis);
            }
        }

        /** {@code true} when this turn moves nothing, which is how a straight box is written. */
        public boolean isStraight() {
            return angle == 0.0f;
        }

        @Override
        public String toString() {
            return "Rotation(" + originX + ", " + originY + ", " + originZ + " around " + axis
                    + " by " + angle + " degrees)";
        }
    }

    /** Corner of a box as a string, used in a report. */
    private static String from(float x, float y, float z) {
        return "[" + x + ", " + y + ", " + z + "]";
    }

    /** A corner of a box, kept inside the block it belongs to. */
    private static float check(float value, String where) {
        if (value < 0.0f || value > UNITS) {
            throw new IllegalArgumentException("The " + where + " of a box is " + value
                    + ", but a box lies between 0 and " + UNITS);
        }
        return value;
    }

    @Override
    public String toString() {
        return "ModelBox(" + from(fromX, fromY, fromZ) + " to " + from(toX, toY, toZ) + ", "
                + faceCount() + " faces" + (rotation == null ? "" : ", " + rotation) + ")";
    }
}
