package com.philia093.neofactory.render.model;

import com.badlogic.gdx.math.MathUtils;

/**
 * How the bones of a body are turned while one frame is drawn.
 * <p>
 * A walk is two legs that swing against each other and two arms that swing with them, a hit is an arm
 * that is thrown forward, and a view is a head that turns with it. All of that is one small record of
 * angles in degrees, which the renderer turns into the matrix of every bone - see
 * {@link Bone#matrixOf} - and which a test reads back without a window.
 * <p>
 * <b>The swing of a walk is a wave.</b> {@link #walk} runs from {@code 0} to {@code 1} once per
 * two steps, and the sine of a full circle through it is what moves the limbs: both are at their
 * place at the ends of the walk and furthest apart in the middle, which is exactly what a step
 * looks like.
 * <p>
 * <b>A hit is a stroke.</b> {@link #swing} runs from {@code 0} to {@code 1} once per hit, and the
 * arm is at rest at both ends and thrown furthest in the middle, which is the same curve the hand of
 * a view uses, see {@link com.philia093.neofactory.render.HandPose}.
 *
 * @param headYaw degrees the head is turned by around the neck
 * @param headPitch degrees the head is turned up by
 * @param armRightPitch degrees the right arm is swung by, positive is forward
 * @param armLeftPitch degrees the left arm is swung by, positive is forward
 * @param legRightPitch degrees the right leg is swung by, positive is forward
 * @param legLeftPitch degrees the left leg is swung by, positive is forward
 * @param bodyYaw degrees the body is turned by around its middle
 */
public record HumanoidPose(float headYaw, float headPitch, float armRightPitch, float armLeftPitch,
        float legRightPitch, float legLeftPitch, float bodyYaw) {

    /** Highest swing of a leg, in degrees, at a walk of full speed. */
    public static final float LEG_SWING = 33.0f;

    /** Highest swing of an arm, in degrees, at a walk of full speed. */
    public static final float ARM_SWING = 24.0f;

    /** How far a hit throws the right arm forward, in degrees. */
    public static final float HIT_SWING = 92.0f;

    /** Share of the pitch of a view the head follows. */
    public static final float HEAD_SHARE = 0.45f;

    /** Highest pitch of a head, in degrees, before it looks like a broken neck. */
    public static final float HEAD_LIMIT = 35.0f;

    /** The body at its rest, standing still and hitting nothing. */
    public static HumanoidPose standing() {
        return of(0.0f, 0.0f, 0.0f, 0.0f);
    }

    /**
     * Places the bones for one frame.
     *
     * @param walk step of the walk cycle, {@code 0} to {@code 1}, {@code 0} while standing still
     * @param swing how far a hit has run, {@code 0} at rest and {@code 1} at the end of the stroke
     * @param yaw degrees the head is turned by around the neck, seen relative to the body
     * @param pitch degrees the head is turned up by, which is the pitch of the view
     * @return the angles of every bone
     */
    public static HumanoidPose of(float walk, float swing, float yaw, float pitch) {
        float step = MathUtils.sin(MathUtils.PI2 * MathUtils.clamp(walk, 0.0f, 1.0f));
        float stroke = MathUtils.sin((float) Math.PI * MathUtils.clamp(swing, 0.0f, 1.0f));
        return new HumanoidPose(yaw,
                MathUtils.clamp(pitch * HEAD_SHARE, -HEAD_LIMIT, HEAD_LIMIT),
                -step * ARM_SWING + stroke * HIT_SWING,
                step * ARM_SWING,
                step * LEG_SWING,
                -step * LEG_SWING,
                0.0f);
    }

    /** {@code true} while this pose is a hit and not the rest of a standing body. */
    public boolean isHitting() {
        return Math.abs(armRightPitch) > ARM_SWING;
    }

    @Override
    public String toString() {
        return "HumanoidPose(head " + headYaw + "/" + headPitch + ", arm " + armRightPitch + "/"
                + armLeftPitch + ", leg " + legRightPitch + "/" + legLeftPitch + ")";
    }
}
