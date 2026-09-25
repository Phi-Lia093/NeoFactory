package com.philia093.neofactory.block;

import java.util.Locale;

/**
 * One of the six faces of a cube.
 * <p>
 * The game looked at the world from above until now, so a block was one picture and the direction
 * it was seen from never mattered. A world of cubes is looked at from every side, and each of the
 * six faces of a block carries three things of its own:
 * <ul>
 *     <li>the picture the art pack drew for it - the top of a log is its rings and its side the
 *         bark, see {@link FaceSet}</li>
 *     <li>the way it lies in space, which the mesher needs to place its four corners, see
 *         {@link #cornerX(int)}</li>
 *     <li>how much light it catches, because a face that looks away from the sky is darker than
 *         the one looking up - the fixed shading the original game uses</li>
 * </ul>
 * <p>
 * <b>The axes follow the original game:</b> X points east, Y points up and Z points south. Y is
 * therefore the axis a chunk is built along, and the two horizontal axes are X and Z. The flat
 * engine used X and Y for the ground and had no vertical axis at all, which is the one change this
 * class brings with it: the old {@code y} of a block coordinate becomes the new {@code z}, and the
 * old {@code layer} becomes the new {@code y}.
 * <p>
 * <b>The order of the constants may never change.</b> A face is stored as an index inside a meshed
 * chunk, so reordering the constants or removing one would turn a stored world inside out.
 */
public enum BlockFace {

    /**
     * The face looking down.
     * <p>
     * The darkest one, because it only ever sees the light that bounced off the ground.
     */
    BOTTOM(0, -1, 0, 0.5f, 1, 0, 0, 0, 0, 1),

    /** The face looking up, which catches the whole light of the sky. */
    TOP(0, 1, 0, 1.0f, 0, 0, 1, 1, 0, 0),

    /** The face looking north, the one with the smaller Z. */
    NORTH(0, 0, -1, 0.8f, 0, 1, 0, 1, 0, 0),

    /** The face looking south, the one with the larger Z. */
    SOUTH(0, 0, 1, 0.8f, 1, 0, 0, 0, 1, 0),

    /** The face looking west, the one with the smaller X. */
    WEST(-1, 0, 0, 0.6f, 0, 0, 1, 0, 1, 0),

    /** The face looking east, the one with the larger X. */
    EAST(1, 0, 0, 0.6f, 0, 1, 0, 0, 0, 1);

    /** The four faces around a block, the ones a picture of a side covers. */
    public static final BlockFace[] SIDES = {NORTH, SOUTH, WEST, EAST};

    /** Every face, in the order a mesh and a state store them. */
    public static final BlockFace[] ALL = values();

    private final int x;
    private final int y;
    private final int z;
    private final float shade;
    /**
     * The two tangents of the face, the axes its four corners are walked along.
     * <p>
     * Both point along a positive axis, so the four corners stay inside the unit cube, and their
     * cross product is the normal of the face, so the walk runs counter clockwise as seen from
     * outside - which is the winding a graphics card keeps. Walking a face along a negative axis
     * would put a corner outside the block, and swapping the two would turn the face inside out.
     */
    private final int[] first;
    private final int[] second;
    private final int axis;

    BlockFace(int x, int y, int z, float shade, int firstX, int firstY, int firstZ, int secondX,
            int secondY, int secondZ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.shade = shade;
        this.first = new int[] {firstX, firstY, firstZ};
        this.second = new int[] {secondX, secondY, secondZ};
        this.axis = x != 0 ? 0 : y != 0 ? 1 : 2;
    }

    /** Offset of this face along the X axis, {@code -1}, {@code 0} or {@code 1}. */
    public int x() {
        return x;
    }

    /** Offset of this face along the Y axis, {@code -1}, {@code 0} or {@code 1}. */
    public int y() {
        return y;
    }

    /** Offset of this face along the Z axis, {@code -1}, {@code 0} or {@code 1}. */
    public int z() {
        return z;
    }

    /**
     * Axis this face looks along.
     *
     * @return {@code 0} for X, {@code 1} for Y and {@code 2} for Z
     */
    public int axis() {
        return axis;
    }

    /** {@code true} when this face looks up or down instead of sideways. */
    public boolean isVertical() {
        return axis == 1;
    }

    /**
     * Share of the light this face catches, before any light of the world is added.
     * <p>
     * The four numbers are the ones the original game uses: a top face is fully lit, the two faces
     * along the Z axis carry four fifths, the two along X three fifths and a bottom face one half.
     * A cube of one colour therefore reads as a cube and not as a hexagon.
     */
    public float shade() {
        return shade;
    }

    /** The face on the other side of the block, the one that looks back. */
    public BlockFace opposite() {
        switch (this) {
            case BOTTOM:
                return TOP;
            case TOP:
                return BOTTOM;
            case NORTH:
                return SOUTH;
            case SOUTH:
                return NORTH;
            case WEST:
                return EAST;
            default:
                return WEST;
        }
    }

    /**
     * Coordinate of one corner of this face along the X axis.
     * <p>
     * The four corners are walked counter clockwise as seen from outside the block, which is the
     * winding a graphics card needs to tell a front face from a back face: corner {@code 0} first,
     * then the corner one step along the first tangent, then the one across the face, then the one
     * step along the second tangent.
     *
     * @param corner corner of the face, {@code 0} to {@code 3}
     * @return the coordinate, {@code 0} or {@code 1}
     */
    public int cornerX(int corner) {
        return component(0, corner);
    }

    /**
     * Coordinate of one corner of this face along the Y axis.
     *
     * @param corner corner of the face, {@code 0} to {@code 3}
     * @return the coordinate, {@code 0} or {@code 1}
     */
    public int cornerY(int corner) {
        return component(1, corner);
    }

    /**
     * Coordinate of one corner of this face along the Z axis.
     *
     * @param corner corner of the face, {@code 0} to {@code 3}
     * @return the coordinate, {@code 0} or {@code 1}
     */
    public int cornerZ(int corner) {
        return component(2, corner);
    }

    /**
     * Coordinate of one corner of this face along one axis of the block.
     *
     * @param axis axis to read, {@code 0} for X, {@code 1} for Y and {@code 2} for Z
     * @param corner corner of the face, {@code 0} to {@code 3}
     * @return the coordinate, {@code 0} or {@code 1}
     * @throws IndexOutOfBoundsException when the corner is not one of the four
     */
    private int component(int axis, int corner) {
        if (corner < 0 || corner > 3) {
            throw new IndexOutOfBoundsException("Corner of a face out of range: " + corner);
        }
        if (axis == this.axis) {
            // The face lies on the far side of the block when it looks along a positive axis and on
            // the near side when it looks along a negative one.
            return x + y + z > 0 ? 1 : 0;
        }
        // Corner 0 sits at the first tangent, 1 one step along it, 2 across the face and 3 one step
        // along the second tangent.
        int alongFirst = corner == 1 || corner == 2 ? 1 : 0;
        int alongSecond = corner == 2 || corner == 3 ? 1 : 0;
        return first[axis] * alongFirst + second[axis] * alongSecond;
    }

    /**
     * The face a name stands for.
     * <p>
     * A model file names the faces the way the original game does, so {@code down} and {@code up} are
     * accepted for the two faces that look along the vertical axis beside the names of the constant
     * itself, see {@link #BOTTOM} and {@link #TOP}.
     *
     * @param name name such as {@code "top"} or {@code "down"}, in any case
     * @return the face, or {@code null} when no face is called that
     */
    public static BlockFace byName(String name) {
        if (name == null) {
            return null;
        }
        String wanted = name.trim().toLowerCase(Locale.ROOT);
        if ("down".equals(wanted)) {
            return BOTTOM;
        }
        if ("up".equals(wanted)) {
            return TOP;
        }
        for (BlockFace face : ALL) {
            if (face.toString().equals(wanted)) {
                return face;
            }
        }
        return null;
    }

    /** Name of this face as it is written in a file, lower case. */
    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
