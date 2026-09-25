package com.philia093.neofactory.render.model;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks how a body moves, without opening a window.
 * <p>
 * A walk and a hit are curves, and a bone is a box that turns around its joint. Both are arithmetic, so
 * both are checked here: the two legs have to swing against each other, a hit has to throw the right
 * arm forward without moving the left one, and a head that follows a view must not break its neck. The
 * last test reads a corner of a bone back out of its matrix, which is what catches a joint that turns
 * around the middle of its box instead of around the shoulder.
 */
class HumanoidPoseTest {

    @Test
    void aStandingBodyIsAtItsRest() {
        HumanoidPose standing = HumanoidPose.standing();

        assertEquals(0.0f, standing.armRightPitch(), 1.0e-6f);
        assertEquals(0.0f, standing.armLeftPitch(), 1.0e-6f);
        assertEquals(0.0f, standing.legRightPitch(), 1.0e-6f);
        assertEquals(0.0f, standing.legLeftPitch(), 1.0e-6f);
        assertFalse(standing.isHitting());
    }

    @Test
    void theLegsOfAWalkSwingAgainstEachOther() {
        // A quarter of the way through the walk the sine of the step is at its highest, so one leg is
        // forward and the other one is back by the same amount.
        HumanoidPose pose = HumanoidPose.of(0.25f, 0.0f, 0.0f, 0.0f);

        assertEquals(HumanoidPose.LEG_SWING, pose.legRightPitch(), 1.0e-4f);
        assertEquals(-HumanoidPose.LEG_SWING, pose.legLeftPitch(), 1.0e-4f);
        assertEquals(-HumanoidPose.ARM_SWING, pose.armRightPitch(), 1.0e-4f,
                "an arm swings with the leg of the other side");
        assertEquals(HumanoidPose.ARM_SWING, pose.armLeftPitch(), 1.0e-4f);
        assertEquals(0.0f, HumanoidPose.of(0.5f, 0.0f, 0.0f, 0.0f).legRightPitch(), 1.0e-4f,
                "half way through the walk the limbs pass each other");
    }

    @Test
    void aHitThrowsTheRightArmForward() {
        HumanoidPose pose = HumanoidPose.of(0.0f, 0.5f, 0.0f, 0.0f);

        assertEquals(HumanoidPose.HIT_SWING, pose.armRightPitch(), 1.0e-3f,
                "the middle of a hit is the furthest the arm reaches");
        assertEquals(0.0f, pose.armLeftPitch(), 1.0e-6f, "and the other arm stays where it is");
        assertTrue(pose.isHitting());
        assertEquals(0.0f, HumanoidPose.of(0.0f, 1.0f, 0.0f, 0.0f).armRightPitch(), 1.0e-3f,
                "a hit ends where it started");
    }

    @Test
    void theHeadFollowsTheViewUpToALimit() {
        HumanoidPose up = HumanoidPose.of(0.0f, 0.0f, 0.0f, 45.0f);

        assertEquals(45.0f * HumanoidPose.HEAD_SHARE, up.headPitch(), 1.0e-4f,
                "a head follows a view that looks up");
        assertEquals(HumanoidPose.HEAD_LIMIT, HumanoidPose.of(0.0f, 0.0f, 0.0f, 90.0f).headPitch(),
                1.0e-4f, "and stops before the neck breaks");
        assertEquals(-HumanoidPose.HEAD_LIMIT,
                HumanoidPose.of(0.0f, 0.0f, 0.0f, -90.0f).headPitch(), 1.0e-4f);
    }

    @Test
    void aBoneTurnsAroundItsJoint() {
        // An arm of the body: the shoulder is the joint and the hand hangs below it. Turning the arm
        // by a quarter of a circle carries the hand forward, not sideways and not around the middle of
        // the box.
        Bone arm = HumanoidModel.of().bone(HumanoidModel.ARM_RIGHT);
        Matrix4 matrix = arm.matrixOf(null, 90.0f, 0.0f, 0.0f, new Matrix4());

        Vector3 hand = new Vector3(arm.pivotX(), HumanoidModel.HAND_Y, arm.pivotZ());
        hand.mul(matrix);

        assertEquals(arm.pivotX(), hand.x, 1.0e-4f, "the arm does not move sideways");
        assertEquals(arm.pivotY(), hand.y, 1.0e-4f, "the hand rises to the height of the shoulder");
        assertEquals(arm.pivotZ() - (arm.pivotY() - HumanoidModel.HAND_Y), hand.z, 1.0e-4f,
                "and reaches forward by the length of the arm");
    }

    @Test
    void aBoneFollowsTheBoneItHangsOn() {
        // A head on a body: the body is turned by a quarter of a circle, so the head - which does not
        // turn itself - ends up beside where it started.
        Bone body = HumanoidModel.of().bone(HumanoidModel.BODY);
        Bone head = HumanoidModel.of().bone(HumanoidModel.HEAD);

        Matrix4 bodyMatrix = body.matrixOf(null, 0.0f, 90.0f, 0.0f, new Matrix4());
        Matrix4 headMatrix = head.matrixOf(bodyMatrix, 0.0f, 0.0f, 0.0f, new Matrix4());

        Vector3 front = new Vector3(0.0f, 1.75f, -0.25f);
        front.mul(headMatrix);

        assertEquals(-0.25f, front.x, 1.0e-4f, "the face of the head looks west after the turn");
        assertEquals(1.75f, front.y, 1.0e-4f, "at the height it had");
    }
}
