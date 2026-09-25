package com.philia093.neofactory.render;

import com.badlogic.gdx.math.MathUtils;

/**
 * Where the hand of a first-person view stands while one frame is drawn.
 * <p>
 * The hand is part of the view, not of the world: it hangs in front of the eye to the lower right, it
 * sways with every step and it is thrown into the picture when the player hits something. All of that
 * is one small record of a place and an angle, which the renderer turns into a matrix - see
 * {@link FirstPersonHand} - and which a test reads back without a window.
 * <p>
 * <b>The swing curve.</b> A swing runs from {@code 0} to {@code 1}; the hand is at its rest at both
 * ends and reaches furthest into the picture in the middle, which is the one stroke the original game
 * shows for a hit. {@link #of(float, float)} folds that curve in, so a caller only says how far the
 * swing has run and which step of the walk cycle it is in.
 * <p>
 * <b>Two distances.</b> {@link #x()}, {@link #y()} and {@link #z()} are in blocks in the frame of the
 * eye - {@code z} is negative in front of it - and {@link #pitch()} is the angle the hand is turned
 * down by, in degrees.
 *
 * @param x distance to the right of the middle of the view, in blocks
 * @param y distance up from the middle of the view, in blocks
 * @param z distance into the view, negative in front of the eye, in blocks
 * @param pitch angle the hand is turned down by, in degrees
 */
public record HandPose(float x, float y, float z, float pitch) {

    /** Distance of a resting hand to the right of the middle of the view, in blocks. */
    public static final float REST_X = 0.36f;

    /**
     * Height of a resting hand below the middle of the view, in blocks.
     * <p>
     * The number is what the field of view of an eye allows: the view of the world is seventy degrees
     * across at the vertical axis, so a hand nearer than a block to the eye leaves the picture below
     * about four tenths of a block. The arm is long enough to reach into the frame from there, which is
     * how it shows without covering what the player looks at.
     */
    public static final float REST_Y = -0.26f;

    /** Distance of a resting hand in front of the eye, in blocks. */
    public static final float REST_Z = -0.42f;

    /** Highest sway of a walking hand to the sides, in blocks. */
    public static final float WALK_SWAY = 0.045f;

    /** Highest rise and fall of a walking hand, in blocks. */
    public static final float WALK_BOB = 0.035f;

    /** How far a swing lifts the hand, in blocks. */
    public static final float SWING_LIFT = 0.20f;

    /** How far a swing pushes the hand into the view, in blocks. */
    public static final float SWING_REACH = 0.16f;

    /** Angle a swing turns the hand down by at its strongest, in degrees. */
    public static final float SWING_PITCH = 50.0f;

    /** Seconds one swing takes from its start to its rest again. */
    public static final float SWING_SECONDS = 0.25f;

    /** Steps of the walk cycle one second of walking covers. */
    public static final float WALK_CYCLES_PER_SECOND = 1.6f;

    /** The hand as it hangs while the player stands still and hits nothing. */
    public static HandPose rest() {
        return of(0.0f, 0.0f);
    }

    /**
     * Places the hand for one frame.
     *
     * @param swing how far a swing has run, {@code 0} at rest and {@code 1} at the end of the stroke
     * @param walk step of the walk cycle, {@code 0} to {@code 1}, {@code 0} while standing still
     * @return the place and the angle of the hand
     */
    public static HandPose of(float swing, float walk) {
        // One stroke: nothing at both ends and the most in the middle, see the class comment.
        float stroke = MathUtils.sin((float) Math.PI * MathUtils.clamp(swing, 0.0f, 1.0f));
        float sway = MathUtils.sin(MathUtils.PI2 * walk) * WALK_SWAY;
        float bob = Math.abs(MathUtils.sin(MathUtils.PI * walk)) * WALK_BOB;
        return new HandPose(REST_X + sway,
                REST_Y - bob + stroke * SWING_LIFT,
                REST_Z - stroke * SWING_REACH,
                -stroke * SWING_PITCH);
    }

    /** {@code true} while this pose is a swing and not the place the hand rests at. */
    public boolean isSwinging() {
        return pitch != 0.0f;
    }

    @Override
    public String toString() {
        return "HandPose(" + x + ", " + y + ", " + z + ", pitch " + pitch + ")";
    }
}
