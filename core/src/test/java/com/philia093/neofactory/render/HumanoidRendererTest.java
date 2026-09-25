package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.philia093.neofactory.render.model.Bone;
import com.philia093.neofactory.render.model.HumanoidModel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a player really lands in the picture, without opening a window.
 * <p>
 * A body that is meshed, textured and drawn where nobody can see it looks exactly like a body that was
 * never drawn at all, and the game reports neither. The two places a body is placed are therefore
 * checked against a camera here: the arm of a view has to reach into the picture of the eye - together
 * with the item it holds - and the whole figure has to stand in front of a camera that looks at it from
 * behind.
 * <p>
 * The cameras of this check are written by hand: the camera of libGDX builds its frustum with a native
 * call, which a test without a window does not have, while the view and the projection of a camera are
 * plain arithmetic and are all a projection needs.
 */
class HumanoidRendererTest {

    /** Field of view of the cameras of the check, the one the world of the game is seen through. */
    private static final float FIELD_OF_VIEW = 70.0f;

    /** Near plane of the cameras of the check. */
    private static final float NEAR = 0.05f;

    /** Far plane of the cameras of the check. */
    private static final float FAR = 192.0f;

    /** Viewport of the tests, a window of the shape a player usually has. */
    private static final float WIDTH = 1280.0f;

    /** Height of the test viewport. */
    private static final float HEIGHT = 720.0f;

    /** Size, in blocks, of the block a hand holds: the one {@code HumanoidRenderer} draws it with. */
    private static final float HELD_SIZE = 0.5f;

    /** Degrees that block is turned by: about the vertical axis, the sideways one and the view. */
    private static final float BLOCK_TURN_Y = -38.0f;
    private static final float BLOCK_TURN_X = 18.0f;
    private static final float BLOCK_TURN_Z = -8.0f;

    @Test
    void theHandOfAnArmStandsWhereTheItemItHoldsHangs() {
        Camera camera = eye();
        Bone arm = HumanoidModel.of().bone(HumanoidModel.ARM_RIGHT);
        Matrix4 matrix = HumanoidRenderer.armMatrix(camera, HandPose.rest(), arm, new Matrix4());

        // A view draws no arm at all - the arm of boxes is only drawn where a camera looks at the body from
        // outside - so what has to stand well is the hand of that arm: the item of a player hangs on that
        // very spot, and a hand outside the picture is an item outside the picture.
        Vector3 hand = new Vector3(0.0f, HumanoidModel.HAND_Y, 0.0f).mul(matrix);
        assertTrue(insidePicture(camera, hand), "the hand of a view stands in the picture: " + hand);
        // A hand right on the eye would cover the whole picture: the arm reaches into the scene instead.
        assertTrue(depth(camera, hand) > 0.3f, "the hand of a view stands far enough from the eye to read "
                + "as a hand and not as a wall: " + depth(camera, hand));
        // And it hangs in the lower right of the picture, which is where a player looks for what they hold.
        float[] clip = multiply(camera.projection.val, multiply(camera.view.val, hand));
        assertTrue(clip[0] / clip[3] > 0.0f, "the hand of a view stands to the right of the middle of the "
                + "picture, at " + clip[0] / clip[3]);
        assertTrue(clip[1] / clip[3] < 0.0f, "and below it, at " + clip[1] / clip[3]);
    }

    @Test
    void theBlockOfTheHandIsInThePictureAndAtTheArm() {
        Camera camera = eye();
        Bone arm = HumanoidModel.of().bone(HumanoidModel.ARM_RIGHT);
        Matrix4 matrix = HumanoidRenderer.armMatrix(camera, HandPose.rest(), arm, new Matrix4());

        Vector3 held = new Vector3(0.0f, HumanoidModel.HAND_Y, 0.0f).mul(matrix);
        assertTrue(insidePicture(camera, held), "the hand of a view is drawn where it can be seen: "
                + held);
        Vector3 shoulder = new Vector3(0.0f, arm.pivotY(), 0.0f).mul(matrix);
        assertTrue(depth(camera, held) > depth(camera, shoulder), "the hand reaches further into the "
                + "picture than the shoulder, so the block hangs at the arm and not in it");
        // The hand of a view reaches towards the middle of the picture, not out of its right edge.
        float across = multiply(camera.projection.val, multiply(camera.view.val, held))[0]
                / multiply(camera.projection.val, multiply(camera.view.val, held))[3];
        assertTrue(across < 1.0f, "the hand of a view stays inside the picture, it stands at " + across);

        // The block that hand holds, placed by the very matrix the renderer uses, with the size and the
        // turns the renderer draws it with, see HumanoidRenderer: half a block, held in the frame of the
        // eye - whose up axis is the up axis of the block - and turned so that the eye sees its top face
        // beside two of its sides.
        Vector3 inEye = held.cpy().mul(camera.view);
        Matrix4 block = HumanoidRenderer.heldMatrix(camera, inEye, HELD_SIZE, BLOCK_TURN_Y, BLOCK_TURN_X,
                BLOCK_TURN_Z, new Matrix4());
        Vector3 middle = new Vector3().mul(block);
        Vector3 top = new Vector3(0.0f, HELD_SIZE * 0.5f, 0.0f).mul(block);
        assertTrue(top.y > middle.y, "the top of the held block stands above its middle: " + top
                + " over " + middle);
        assertTrue(insidePicture(camera, middle),
                "the block of a hand is drawn where it can be seen: " + middle);
        float[] clip = multiply(camera.projection.val, multiply(camera.view.val, middle));
        assertTrue(clip[0] / clip[3] > 0.1f && clip[0] / clip[3] < 0.9f,
                "a held block stands in the right half of the picture, it stands at " + clip[0] / clip[3]);
        assertTrue(clip[1] / clip[3] < -0.2f,
                "and in the lower part of it, it stands at " + clip[1] / clip[3]);
        // It is nearer to the eye than the hand itself, which is what makes it a block held up and not a
        // block lying in the world.
        assertTrue(depth(camera, middle) < depth(camera, held),
                "a held block stands nearer to the eye than the hand that holds it");
        // The turn about the sideways axis lifts the top of the block towards the eye, so the eye sees it:
        // in the frame of the eye, where the eye looks along its own negative Z axis, that is the positive
        // angle.
        Vector3 topEye = top.cpy().mul(camera.view);
        Vector3 middleEye = middle.cpy().mul(camera.view);
        assertTrue(topEye.z > middleEye.z,
                "the top of a held block leans towards the eye, so the eye sees it: " + topEye
                        + " over " + middleEye);
    }

    @Test
    void theFigureStandsInFrontOfACameraThatLooksAtItFromBehind() {
        // The camera of the view from behind: behind the eye, a little above it and looking at it.
        Camera camera = camera(new Vector3(0.5f, 1.62f + 1.1f, 0.5f - 4.5f),
                new Vector3(0.0f, -0.25f, 1.0f));

        HumanoidModel model = HumanoidModel.of();
        Matrix4 body = HumanoidRenderer.bodyMatrix(0.5f, 0.0f, 0.5f, 0.0f, new Matrix4());

        for (Bone bone : model.bones()) {
            Matrix4 matrix = bone.matrixOf(body, 0.0f, 0.0f, 0.0f, new Matrix4());
            int seen = 0;
            for (Vector3 corner : corners(bone)) {
                corner.mul(matrix);
                if (insidePicture(camera, corner)) {
                    seen++;
                }
            }
            assertTrue(seen > 0, "the bone " + bone.name() + " of the figure is drawn in the picture");
        }
    }

    /** A camera standing in the world where a player stands, looking south. */
    private static Camera eye() {
        return camera(new Vector3(0.5f, 1.62f, 0.5f), new Vector3(0.0f, 0.0f, 1.0f));
    }

    /** A camera with the view and the projection of a camera, built without its frustum. */
    private static Camera camera(Vector3 position, Vector3 direction) {
        // The frustum of a camera of libGDX is built with a native call, which a test without a window
        // does not have, so the update of this camera does nothing and the matrices are set by hand.
        Camera camera = new Camera() {
            @Override
            public void update() {
                // Nothing to build: the view and the projection are written below.
            }

            @Override
            public void update(boolean updateFrustum) {
                // Nothing to build, see update().
            }
        };
        camera.viewportWidth = WIDTH;
        camera.viewportHeight = HEIGHT;
        camera.position.set(position);
        camera.direction.set(direction).nor();
        camera.up.set(0.0f, 1.0f, 0.0f);
        camera.view.setToLookAt(camera.position,
                new Vector3(camera.position).add(camera.direction), camera.up);
        camera.projection.setToProjection(NEAR, FAR, FIELD_OF_VIEW, WIDTH / HEIGHT);
        camera.combined.set(camera.projection).mul(camera.view);
        return camera;
    }

    /** The corners of the mesh of a bone, in the coordinates of the model. */
    private static List<Vector3> corners(Bone bone) {
        MeshData mesh = bone.build(picture -> 0);
        float[] vertices = mesh.vertexFloats();
        List<Vector3> found = new ArrayList<>();
        for (int corner = 0; corner < mesh.vertexCount(); corner++) {
            int base = corner * MeshData.FLOATS_PER_VERTEX;
            found.add(new Vector3(vertices[base], vertices[base + 1], vertices[base + 2]));
        }
        return found;
    }

    /**
     * {@code true} when a corner in the world is in front of the camera and inside its picture.
     * <p>
     * The arithmetic is written out here because the projection of libGDX is a native call, which a
     * test without a window does not have.
     */
    private static boolean insidePicture(Camera camera, Vector3 world) {
        float[] eye = multiply(camera.view.val, world);
        float[] clip = multiply(camera.projection.val, eye);
        return eye[2] < 0.0f && clip[3] > 0.0f
                && Math.abs(clip[0] / clip[3]) <= 1.0f
                && Math.abs(clip[1] / clip[3]) <= 1.0f;
    }

    /** Distance of a corner from the camera, in view space. */
    private static float depth(Camera camera, Vector3 world) {
        return -multiply(camera.view.val, world)[2];
    }

    /** Matrix times vector, a matrix of libGDX being written column by column. */
    private static float[] multiply(float[] matrix, Vector3 vector) {
        return multiply(matrix, new float[] {vector.x, vector.y, vector.z, 1.0f});
    }

    /** Matrix times a point of four coordinates, a matrix of libGDX being written column by column. */
    private static float[] multiply(float[] matrix, float[] point) {
        float[] result = new float[4];
        for (int row = 0; row < 4; row++) {
            result[row] = matrix[row] * point[0] + matrix[4 + row] * point[1]
                    + matrix[8 + row] * point[2] + matrix[12 + row] * point[3];
        }
        return result;
    }
}
