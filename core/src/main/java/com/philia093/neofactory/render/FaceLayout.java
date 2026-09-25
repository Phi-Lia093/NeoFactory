package com.philia093.neofactory.render;

import com.philia093.neofactory.block.BlockFace;

/**
 * Where a corner of a face sits inside the picture of that face.
 * <p>
 * Every quad the game draws - the face of a block, a box of a body, the card of an item - is drawn
 * from a picture, and the picture has to stand upright on the face. That is one rule, and this class
 * is where it is written down, so a block and a body read their pictures the same way.
 * <p>
 * <b>A corner is asked for the coordinates it has inside its face, not for the order it is walked
 * in.</b> The order exists for the winding a graphics card needs and has nothing to do with the
 * picture, so a picture that followed it would lie sideways on two of the four sides of a cube.
 * <p>
 * <b>The four sides read left to right, seen from outside.</b> Looking at the north face means
 * looking along {@code +Z}, where the {@code +X} axis runs to the left, so that face counts its own
 * {@code u} the other way round; the east face counts the {@code -Z} axis for the same reason.
 * <p>
 * <b>A face that looks up or down lies as the picture lies.</b> There the two axes of the face are the
 * two horizontal axes of the world, so a floor of grass and the top of a log read the way they were
 * drawn.
 */
public final class FaceLayout {

    private FaceLayout() {
        // Utility class: never instantiated.
    }

    /**
     * Position of a corner across the picture of a face, {@code 0} at its left edge.
     *
     * @param face face of a box
     * @param x position of the corner along X, {@code 0} or {@code 1}
     * @param y position of the corner along Y, {@code 0} or {@code 1}
     * @param z position of the corner along Z, {@code 0} or {@code 1}
     * @return the position along the picture, {@code 0} or {@code 1}
     */
    public static float across(BlockFace face, float x, float y, float z) {
        return switch (face) {
            case TOP, BOTTOM, SOUTH -> x;
            case NORTH -> 1.0f - x;
            case WEST -> z;
            case EAST -> 1.0f - z;
        };
    }

    /**
     * Position of a corner up the picture of a face, {@code 1} at its top edge.
     *
     * @param face face of a box
     * @param x position of the corner along X, {@code 0} or {@code 1}
     * @param y position of the corner along Y, {@code 0} or {@code 1}
     * @param z position of the corner along Z, {@code 0} or {@code 1}
     * @return the position up the picture, {@code 0} or {@code 1}
     */
    public static float up(BlockFace face, float x, float y, float z) {
        return switch (face) {
            // A face that looks up or down lies flat, so the height of the box is not what its picture
            // climbs along; the picture climbs along the second horizontal axis instead.
            case TOP, BOTTOM -> z;
            default -> y;
        };
    }

    /**
     * Turns a spot inside a face by a quarter of a circle.
     * <p>
     * A model may turn the picture of a face, which is what a texture with a direction of its own
     * needs - a stair that points the other way, a machine whose front is drawn upright.
     *
     * @param rotation quarter turns the picture is turned by, {@code 0}, {@code 90}, {@code 180} or
     *                 {@code 270}, counter clockwise as seen from outside
     * @param across position of the corner across the picture, {@code 0} to {@code 1}
     * @param down position of the corner down the picture, {@code 0} to {@code 1}
     * @param into place the two coordinates are written into, so a build allocates nothing per corner
     */
    public static void turn(int rotation, float across, float down, float[] into) {
        switch (rotation) {
            case 90 -> {
                into[0] = down;
                into[1] = 1.0f - across;
            }
            case 180 -> {
                into[0] = 1.0f - across;
                into[1] = 1.0f - down;
            }
            case 270 -> {
                into[0] = 1.0f - down;
                into[1] = across;
            }
            default -> {
                into[0] = across;
                into[1] = down;
            }
        }
    }
}
