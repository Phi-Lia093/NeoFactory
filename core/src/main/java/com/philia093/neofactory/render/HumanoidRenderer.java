package com.philia093.neofactory.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.render.model.Bone;
import com.philia093.neofactory.render.model.HumanoidModel;
import com.philia093.neofactory.render.model.HumanoidPose;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Draws a body of boxes: the player, seen from outside or from inside.
 * <p>
 * The body is the model of {@link HumanoidModel}, meshed once per bone and drawn with a matrix per
 * bone, see {@link Bone#matrixOf}: the walk swings the limbs, a hit throws the right arm forward and
 * the head follows the view, all of it decided by {@link HumanoidPose}.
 * <p>
 * <b>From inside a body a hand shows the block it holds and no arm.</b> {@link #renderHand} draws that
 * block in the frame of the eye and at the place {@link HandPose} asks for - the hand of the arm, where
 * that arm would end - and it is <i>turned</i> the way the eye is, not the way the arm is, see
 * {@link #heldMatrix}. That is what keeps a block in a hand instead of in front of it, and what shows the
 * top of that block beside its two sides. The bare arm is drawn only where a camera looks at the body from
 * outside.
 * <p>
 * <b>A tool is not drawn in a hand.</b> An item that is not a block has no shape of its own, and a plate of
 * its picture across the lower right of the view read worse than nothing; a thing of one picture belongs to
 * the interface, which draws it as its icon, see {@code GuiItemRenderer}. Holding one therefore leaves the
 * hand empty, which is the one state of a hand this game draws nothing for.
 * <p>
 * <b>A body is drawn where its feet are.</b> The body stands at the position of the player, turned by
 * its view and scaled by {@link HumanoidModel#SCALE}; the meshes are asked for on first use and kept,
 * so a body costs six draws and nothing else per frame.
 */
public class HumanoidRenderer implements Disposable {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Distance the fog of the first-person pass starts at, far enough that an arm is never fogged. */
    private static final float NO_FOG_START = 100.0f;

    /** Distance everything of the first-person pass would be fog by, which nothing of it reaches. */
    private static final float NO_FOG_END = 200.0f;

    /** Colour the first-person pass would fade into, which it never does. */
    private static final Color NO_FOG_COLOR = new Color(1.0f, 1.0f, 1.0f, 1.0f);

    /**
     * Degrees the model of a body is turned by before the compass of its view is applied.
     * <p>
     * The model of a body faces north on its own and a player who looks south faces away from the
     * camera, which is the very turn the original game makes as well: {@code 180 - bodyYaw}.
     */
    private static final float BODY_TURN = 180.0f;

    private final BlockPictures pictures;
    private final ItemCubeMeshes cubes;

    /** Meshes of every bone, built while the skin of the body was found. */
    private final ObjectMap<String, Array<Mesh>> boneMeshes = new ObjectMap<>();

    /** Set once the missing skin was reported, so the log holds one line. */
    private boolean reportedMissingSkin;

    /** Step of the walk cycle, {@code 0} to {@code 1}. */
    private float walk;

    /** Seconds into the hit that runs right now, negative while the arm is at rest. */
    private float swing = -1.0f;

    /** Reused matrices of one frame, so drawing a body fills no heap. */
    private final Matrix4 body = new Matrix4();
    private final Matrix4 bone = new Matrix4();
    private final Matrix4 limb = new Matrix4();
    private final Matrix4 placed = new Matrix4();
    private final Matrix4 itemWorld = new Matrix4();

    /** Place of the hand of the arm in the frame of the eye, where the item it holds stands. */
    private final Vector3 hand = new Vector3();

    /** The body this renderer draws, built together with its meshes, {@code null} before that. */
    private HumanoidModel model;

    /** Set by {@link #build()}: {@code true} when every bone of the body found its skin. */
    private boolean ready;

    /**
     * Creates a renderer.
     *
     * @param pictures pictures of the world, asked for the layer of a skin
     * @param cubes cubes of the items, asked for the block a hand holds
     */
    public HumanoidRenderer(BlockPictures pictures, ItemCubeMeshes cubes) {
        this.pictures = pictures;
        this.cubes = cubes;
    }

    /**
     * Advances the walk cycle and the hit of the body.
     *
     * @param delta time since the last frame in seconds
     * @param walking {@code true} while the body moves over the ground
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

    /** Starts a hit, called when the player breaks or builds a block. */
    public void swing() {
        swing = 0.0f;
    }

    /** {@code true} while a hit runs, which a status line may report. */
    public boolean isHitting() {
        return swing >= 0.0f;
    }

    /** How far a hit has run, {@code 0} at rest and {@code 1} at the end of the stroke. */
    public float swingProgress() {
        return swing < 0.0f ? 0.0f : swing / HandPose.SWING_SECONDS;
    }

    /** The pose of the body of this frame. */
    public HumanoidPose currentPose(float yaw, float pitch) {
        return HumanoidPose.of(walk, swingProgress(), 0.0f, pitch);
    }

    /** {@code true} while the skin of the body was found in the array of pictures. */
    public boolean isReady() {
        if (model == null) {
            ready = build();
        }
        return ready;
    }

    /** The body this renderer draws, {@code null} before {@link #isReady()} was asked. */
    public HumanoidModel model() {
        isReady();
        return model;
    }

    /**
     * The matrix that carries a body of a given compass of view.
     * <p>
     * The body is drawn around its own feet: the model is scaled first, turned towards the compass of
     * the view, and then carried to where the feet stand.
     *
     * @param x world X coordinate of the feet of the body
     * @param y world Y coordinate of the feet of the body
     * @param z world Z coordinate of the feet of the body
     * @param yaw degrees the body is turned by, the compass of its view
     * @param into matrix to write into
     * @return {@code into}
     */
    public static Matrix4 bodyMatrix(float x, float y, float z, float yaw, Matrix4 into) {
        return into.idt().translate(x, y, z)
                .rotate(0.0f, 1.0f, 0.0f, BODY_TURN - yaw)
                .scale(HumanoidModel.SCALE, HumanoidModel.SCALE, HumanoidModel.SCALE);
    }

    /**
     * Degrees the arm of a view is turned forward by.
     * <p>
     * An arm hangs from its shoulder, so an arm that is not turned reaches straight down and is out of
     * the picture before it ends. The original game draws the arm of a view pointing into the scene,
     * and so does this: the hand has to end up in front of the eye and inside the picture, with the block
     * the player holds at the end of it.
     */
    private static final float ARM_FORWARD = 90.0f;

    /**
     * Degrees the arm of a view is turned towards the middle of the picture by.
     * <p>
     * The shoulder stands at the lower right, so the arm has to lean the other way for its hand - and
     * with it the block - to reach towards the middle, which is the diagonal the original game draws.
     */
    private static final float ARM_ASIDE = 25.0f;

    /**
     * The matrix that carries the arm of a view from inside a body.
     * <p>
     * The arm is drawn in the frame of the eye, so the matrix is the inverse of the view: it follows the
     * eye wherever it looks. {@link HandPose} names the place the <b>shoulder</b> stands - the joint the
     * arm hangs from - and the arm is then turned forward and towards the middle, which is what makes it
     * reach into the scene from the lower right with the hand in front of the eye.
     * <p>
     * <b>A joint and not a middle.</b> The shoulder is a whole arm away from the hand, so carrying the
     * <i>middle</i> of the arm to the place the hand pose names would put the shoulder a hand's width
     * from the eye - and an arm at that distance fills the picture.
     *
     * @param camera camera the world is seen through
     * @param pose place the shoulder stands at, in the frame of the eye
     * @param arm bone of the arm
     * @param into matrix to write into
     * @return {@code into}
     */
    public static Matrix4 armMatrix(Camera camera, HandPose pose, Bone arm, Matrix4 into) {
        into.set(camera.view).inv();
        into.translate(pose.x(), pose.y(), pose.z());
        // The turn to the side is written first, so it is applied last and leans the arm that already
        // points into the scene towards the middle of the picture.
        into.rotate(0.0f, 1.0f, 0.0f, ARM_ASIDE);
        into.rotate(1.0f, 0.0f, 0.0f, pose.pitch() + ARM_FORWARD);
        into.scale(HumanoidModel.SCALE, HumanoidModel.SCALE, HumanoidModel.SCALE);
        into.translate(-arm.pivotX(), -arm.pivotY(), -arm.pivotZ());
        return into;
    }

    /** Layer of one face of a bone, or a negative value when the array holds no skin. */
    public int layerOf(String picture) {
        return pictures.layer(picture);
    }

    /**
     * Draws the whole body where it stands.
     * <p>
     * The caller has to have begun the pass of the world, see
     * {@link BlockShader#begin(PerspectiveCamera, BlockPictures, Color, float, float)}; the body is
     * drawn with the very shader and the very pictures the terrain is drawn with, so it fades into the
     * sky like a block does and needs no pass of its own.
     *
     * @param shader shader of the pass that is running
     * @param x world X coordinate of the feet of the body
     * @param y world Y coordinate of the feet of the body
     * @param z world Z coordinate of the feet of the body
     * @param yaw degrees the body is turned by, the compass of its view
     * @param pose angles of every bone
     */
    public void renderBody(BlockShader shader, float x, float y, float z, float yaw,
            HumanoidPose pose) {
        if (!isReady()) {
            return;
        }
        bodyMatrix(x, y, z, yaw, body);
        // The body and the legs hang on the feet of the model, the head and the arms on the body.
        drawBone(model.bone(HumanoidModel.BODY), body, 0.0f, pose.bodyYaw(), 0.0f, bone, shader);
        drawBone(model.bone(HumanoidModel.HEAD), bone, pose.headPitch(), pose.headYaw(), 0.0f,
                limb, shader);
        drawBone(model.bone(HumanoidModel.ARM_RIGHT), bone, pose.armRightPitch(), 0.0f, 0.0f, limb,
                shader);
        drawBone(model.bone(HumanoidModel.ARM_LEFT), bone, pose.armLeftPitch(), 0.0f, 0.0f, limb,
                shader);
        drawBone(model.bone(HumanoidModel.LEG_RIGHT), body, pose.legRightPitch(), 0.0f, 0.0f, limb,
                shader);
        drawBone(model.bone(HumanoidModel.LEG_LEFT), body, pose.legLeftPitch(), 0.0f, 0.0f, limb,
                shader);
    }

    /** Draws one bone with the matrix its joint asks for, hanging on the matrix of its parent. */
    private void drawBone(Bone bone, Matrix4 parent, float pitchX, float yawY, float rollZ,
            Matrix4 into, BlockShader shader) {
        bone.matrixOf(parent, pitchX, yawY, rollZ, into);
        for (Mesh mesh : boneMeshes.get(bone.name())) {
            shader.render(mesh, into);
        }
    }

    /**
     * Draws what the hand of the body a player is inside holds: the block, and nothing else.
     * <p>
     * <b>A view draws no arm at all.</b> The arm of the body is only drawn where a camera looks at the
     * body from outside, see {@link #renderBody}: from inside, a bare arm of boxes reaching across the
     * picture reads worse than no arm, so the hand shows the block it carries and nothing else.
     * <p>
     * The block is drawn in the frame of the eye - it is nearer to the eye than anything the world can put
     * in front of it, and it is drawn without fog, because the distance a corner of it has from the camera
     * means nothing - and it stands where the hand of the arm would end, see {@link #armMatrix}, so it
     * sways with every step and is thrown into the picture when the player hits something.
     * <p>
     * <b>A hand that holds no block draws nothing, and the pass runs all the same.</b> The state this pass
     * leaves is the state of the frame, so skipping it for an empty hand would hand the pass after it a
     * state nobody asked for. The interface of the game sets up its own state as well, see
     * {@code GameScreen#renderInterface}.
     *
     * @param camera camera the world is seen through, the block is drawn in its frame
     * @param shader shader the world is drawn with, the block is drawn with it too
     * @param heldStack stack the player holds, empty - or a tool - for a hand that shows nothing
     */
    public void renderHand(PerspectiveCamera camera, BlockShader shader, ItemStack heldStack) {
        if (!isReady()) {
            return;
        }
        Bone arm = model.bone(HumanoidModel.ARM_RIGHT);
        HandPose at = HandPose.of(swingProgress(), walk);
        // The hand stands around the origin of the eye: the matrix that places it is the inverse of the
        // view, so it follows the eye wherever it looks, and the shoulder is carried to the place the hand
        // pose names, see armMatrix. Nothing of the arm is drawn, but its bones place the block.
        armMatrix(camera, at, arm, placed);

        shader.begin(camera, pictures, NO_FOG_COLOR, NO_FOG_START, NO_FOG_END);
        // The pass may run with culling left on by the pass before it, and the faces of a cube of many
        // sides are what a cube shows: a face that is culled is a face that is drawn and never seen.
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        renderHeld(camera, shader, heldStack);
        shader.end();
    }

    /** Share of a block drawn in a hand, in blocks: half a block held near the eye fills the corner. */
    private static final float HELD_BLOCK_SIZE = 0.5f;

    /** Distance to the right of the hand a block in it stands, in blocks. */
    private static final float HELD_RIGHT = 0.32f;

    /** Distance below the hand a block in it stands, in blocks. */
    private static final float HELD_DOWN = -0.13f;

    /** Distance a block in a hand stands nearer to the eye than the hand itself, in blocks. */
    private static final float HELD_CLOSER = 0.12f;

    /**
     * Degrees a held block is turned by: about the vertical axis, the sideways one and the view.
     * <p>
     * A block the eye looks straight at is one flat square; the turns make a cube of it. The turn about
     * the vertical axis shows two sides at once, the turn about the sideways axis leans the top towards
     * the eye - in the frame of the eye the <b>positive</b> angle is the one that lifts the top towards
     * the eye and the negative one leans it away - so the top face is seen as well, and the small roll
     * keeps the whole thing from looking ruled.
     */
    private static final float BLOCK_TURN_Y = -38.0f;
    private static final float BLOCK_TURN_X = 18.0f;
    private static final float BLOCK_TURN_Z = -8.0f;

    /**
     * Places the block a hand holds, in the frame of the eye.
     * <p>
     * The block stands where the arm ends - the hand, see {@link HumanoidModel#HAND_Y} - so it sways and
     * swings with the arm, but it is turned the way the <b>eye</b> is and not the way the arm is: the arm
     * points into the scene while it is held up, so its own up axis runs along the arm, and a block hung on
     * that axis would be a block standing on its head.
     * <p>
     * <b>The place is given in the frame of the eye</b> and not in the world, which is the frame the arm
     * is placed in as well: a point of the world would be turned with the view and left behind by it.
     *
     * @param camera camera the world is seen through
     * @param hand place of the hand in the frame of the eye, the frame of a view
     * @param size size of the block in blocks
     * @param turnY degrees it is turned about the vertical axis of the eye
     * @param turnX degrees it is turned about the sideways axis of the eye
     * @param turnZ degrees it is rolled about the view of the eye
     * @param into matrix to write into
     * @return {@code into}
     */
    public static Matrix4 heldMatrix(Camera camera, Vector3 hand, float size, float turnY, float turnX,
            float turnZ, Matrix4 into) {
        into.set(camera.view).inv();
        into.translate(hand.x + HELD_RIGHT, hand.y + HELD_DOWN, hand.z + HELD_CLOSER);
        into.rotate(0.0f, 1.0f, 0.0f, turnY);
        into.rotate(1.0f, 0.0f, 0.0f, turnX);
        into.rotate(0.0f, 0.0f, 1.0f, turnZ);
        into.scale(size, size, size);
        return into;
    }

    /**
     * Draws the block a hand holds at the end of the arm.
     * <p>
     * The arm itself was placed by the caller, see {@link #armMatrix}, and the block hangs on the hand of
     * that very matrix: nothing of the block is placed apart from the arm it is held with.
     * <p>
     * <b>Only a block is drawn.</b> An item that is not a block has no shape of its own and is left out,
     * see the class comment: the hand of a view is empty while a player holds a tool or a material.
     *
     * @param camera camera the world is seen through, the block is drawn in its frame
     * @param shader shader of the pass that is running
     * @param heldStack stack the player holds, empty for a bare hand
     */
    private void renderHeld(PerspectiveCamera camera, BlockShader shader, ItemStack heldStack) {
        if (heldStack == null || heldStack.isEmpty()) {
            return;
        }
        Block block = heldStack.item().block();
        if (block == null) {
            return;
        }
        // The hand of the arm carried into the frame of the eye, where the block hangs: it follows every
        // step and every swing of the arm without a pose of its own. The matrix of the arm maps the model
        // of the body to the world, so the view takes the place of the hand back into its own frame.
        hand.set(0.0f, HumanoidModel.HAND_Y, 0.0f).mul(placed).mul(camera.view);
        heldMatrix(camera, hand, HELD_BLOCK_SIZE, BLOCK_TURN_Y, BLOCK_TURN_X, BLOCK_TURN_Z, itemWorld);
        // A cube is meshed from the corner of a block and not from its middle, so the middle of the block
        // is carried to the place of the item, one half block along every axis of the block itself.
        itemWorld.translate(-0.5f, -0.5f, -0.5f);
        for (Mesh mesh : cubes.cubeOf(block)) {
            shader.render(mesh, itemWorld);
        }
    }

    /** Builds the body and uploads the mesh of every bone, once the skin is in the array. */
    private boolean build() {
        model = HumanoidModel.of();
        boneMeshes.clear();
        boolean complete = true;
        for (Bone bone : model.bones()) {
            MeshData data = bone.build(pictures::layer);
            Array<Mesh> meshes = new Array<>(1);
            if (data.isEmpty()) {
                complete = false;
            } else {
                Mesh uploaded = new Mesh(true, data.vertexCount(), data.indexCount(),
                        BlockShader.ATTRIBUTES);
                uploaded.setVertices(data.vertexFloats(), 0,
                        data.vertexCount() * MeshData.FLOATS_PER_VERTEX);
                uploaded.setIndices(data.indexShorts(), 0, data.indexCount());
                meshes.add(uploaded);
            }
            boneMeshes.put(bone.name(), meshes);
        }
        if (!complete && !reportedMissingSkin) {
            reportedMissingSkin = true;
            LOGGER.warn("The array holds no skin of a body, cut from {}: no player is drawn",
                    SkinRegions.SKIN);
        }
        return complete;
    }

    @Override
    public void dispose() {
        for (Array<Mesh> meshes : boneMeshes.values()) {
            for (Mesh mesh : meshes) {
                mesh.dispose();
            }
            meshes.clear();
        }
        boneMeshes.clear();
        model = null;
        ready = false;
    }

    @Override
    public String toString() {
        return "HumanoidRenderer(" + (isReady() ? "drawn" : "no skin") + ", " + boneMeshes.size
                + " bones)";
    }
}
