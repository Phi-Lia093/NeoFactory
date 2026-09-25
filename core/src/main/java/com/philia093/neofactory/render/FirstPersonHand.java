package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The hand of the player, as a view from inside a body sees it.
 * <p>
 * A first-person view has no body to look at, so what it shows of the player is the arm: one box that
 * hangs in the lower right of the picture, sways with every step and is thrown forward when the player
 * hits something, see {@link HandPose} for the place and {@link HandMeshes} for the shape. The block the
 * player holds sits at the end of it, drawn as the small cube a dropped item is drawn as, see
 * {@link ItemCubeMeshes}.
 * <p>
 * <b>The hand is drawn in the frame of the eye.</b> Its mesh stands in the coordinates of the arm - a box
 * around the origin reaching along the negative Z axis - and the matrix that places it is the inverse of
 * the view of the camera times the pose, so the arm follows the eye wherever it turns. It is drawn after
 * the world with the depth buffer cleared, because a hand is nearer to the eye than anything the world
 * can put in front of it, and it is drawn without fog: the distance a corner of it has from the camera
 * means nothing.
 * <p>
 * A material or a tool has no cube to show and a hand that holds one shows the arm alone, the same way
 * such an item is left out of the world, see {@link ItemCubeMeshes}.
 */
public class FirstPersonHand implements Disposable {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Side length of the block a hand holds, in blocks: a little smaller than a block of the world. */
    public static final float HELD_SIZE = 0.36f;

    /** Distance the held block hangs in front of the end of the arm, in blocks. */
    private static final float HELD_AHEAD = 0.06f;

    /** Distance the fog of the hand pass starts at, far enough that a hand is never fogged. */
    private static final float NO_FOG_START = 100.0f;

    /** Distance everything of the hand pass would be fog by, which nothing of it reaches. */
    private static final float NO_FOG_END = 200.0f;

    /** Colour the hand pass would fade into, which it never does. */
    private static final Color NO_FOG_COLOR = new Color(1.0f, 1.0f, 1.0f, 1.0f);

    private final BlockPictures pictures;
    private final ItemCubeMeshes cubes;

    /** The arm, meshed while the picture of the hand was found, {@code null} before that. */
    private Mesh arm;

    /** Layer the picture of the hand lives in, {@code -1} while there is none. */
    private int armLayer = -1;

    /** Set once the missing picture of the hand was reported, so the log holds one line. */
    private boolean reportedMissingPicture;

    /** Reused matrix of the pose of this frame. */
    private final Matrix4 pose = new Matrix4();

    /** Reused matrix of the arm, and of the block it holds. */
    private final Matrix4 model = new Matrix4();
    private final Matrix4 held = new Matrix4();
    private final Matrix4 placed = new Matrix4();

    /** Step of the walk cycle, {@code 0} to {@code 1}. */
    private float walk;

    /** Seconds into the swing that runs right now, negative while the hand is at rest. */
    private float swing = -1.0f;

    /**
     * Creates the hand of a view.
     *
     * @param pictures pictures of the world, asked for the layer of the hand
     * @param cubes cubes of the items, asked for the block a hand holds
     */
    public FirstPersonHand(BlockPictures pictures, ItemCubeMeshes cubes) {
        this.pictures = pictures;
        this.cubes = cubes;
    }

    /**
     * Advances the walk cycle and the swing of the hand.
     *
     * @param delta time since the last frame in seconds
     * @param walking {@code true} while the body of the player moves over the ground
     */
    public void update(float delta, boolean walking) {
        if (walking) {
            walk = (walk + delta * HandPose.WALK_CYCLES_PER_SECOND) % 1.0f;
        }
        if (swing >= 0.0f) {
            swing += delta;
            if (swing >= HandPose.SWING_SECONDS) {
                swing = -1.0f;
            }
        }
    }

    /** Starts a swing, called when the player breaks or builds a block. */
    public void swing() {
        swing = 0.0f;
    }

    /** {@code true} while a swing runs, which a status line may report. */
    public boolean isSwinging() {
        return swing >= 0.0f;
    }

    /** Where the hand stands right now, the pose this frame is drawn with. */
    public HandPose currentPose() {
        return HandPose.of(swing < 0.0f ? 0.0f : swing / HandPose.SWING_SECONDS, walk);
    }

    /** {@code true} while the arm was meshed, which needs the picture of the hand in the array. */
    public boolean isReady() {
        return arm != null;
    }

    /**
     * Draws the arm and the block it holds in front of the camera.
     *
     * @param camera camera the world is seen through, the hand is drawn in its frame
     * @param shader shader the world is drawn with, the hand is drawn with it too
     * @param heldStack stack the player holds, empty for a bare hand
     */
    public void render(PerspectiveCamera camera, BlockShader shader, ItemStack heldStack) {
        if (arm == null && !build()) {
            return;
        }
        HandPose at = currentPose();
        pose.idt()
                .translate(at.x(), at.y(), at.z())
                .rotate(Vector3.X, at.pitch());
        // The mesh of the arm stands around the origin of the eye, so the matrix that places it is the
        // inverse of the view: the arm follows the eye wherever it looks.
        model.set(camera.view).inv().mul(pose);

        shader.begin(camera, pictures, NO_FOG_COLOR, NO_FOG_START, NO_FOG_END);
        shader.render(arm, model);
        renderHeld(heldStack, shader);
        shader.end();
    }

    /** Draws the block a hand holds at the end of the arm. */
    private void renderHeld(ItemStack stack, BlockShader shader) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Block block = stack.item().block();
        if (block == null) {
            // A material or a tool has no cube, see ItemCubeMeshes: the hand shows the arm alone.
            return;
        }
        float half = HELD_SIZE * 0.5f;
        held.idt()
                .translate(-half, -half, -HandMeshes.LENGTH - HELD_AHEAD - HELD_SIZE * 2.0f)
                .scale(HELD_SIZE, HELD_SIZE, HELD_SIZE);
        placed.set(model).mul(held);
        for (Mesh mesh : cubes.cubeOf(block)) {
            shader.render(mesh, placed);
        }
    }

    /** Meshes the arm, once the picture of the hand was found in the array. */
    private boolean build() {
        armLayer = pictures.layer(BlockPictures.HAND);
        if (armLayer < 0) {
            if (!reportedMissingPicture) {
                reportedMissingPicture = true;
                LOGGER.warn("The array holds no picture of the hand, the arm is not drawn");
            }
            return false;
        }
        MeshData data = HandMeshes.arm(armLayer);
        arm = new Mesh(true, data.vertexCount(), data.indexCount(), BlockShader.ATTRIBUTES);
        arm.setVertices(data.vertexFloats(), 0, data.vertexCount() * MeshData.FLOATS_PER_VERTEX);
        arm.setIndices(data.indexShorts(), 0, data.indexCount());
        LOGGER.info("Meshed the arm of the player from layer {}", armLayer);
        return true;
    }

    /** Layer the picture of the hand lives in, {@code -1} while it was not looked up. */
    public int armLayer() {
        return armLayer;
    }

    @Override
    public void dispose() {
        if (arm != null) {
            arm.dispose();
            arm = null;
        }
        armLayer = -1;
    }

    @Override
    public String toString() {
        return "FirstPersonHand(" + (isReady() ? "drawn" : "no arm") + ", " + currentPose() + ")";
    }
}
