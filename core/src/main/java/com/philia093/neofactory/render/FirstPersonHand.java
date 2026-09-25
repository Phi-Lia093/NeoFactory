package com.philia093.neofactory.render;

import com.badlogic.gdx.Gdx;
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
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.world.Section;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

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

    /** Size of the card a tool or a material is held as, in blocks across. */
    private static final float CARD_SCALE = 0.8f;

    /** Distance the fog of the hand pass starts at, far enough that a hand is never fogged. */
    private static final float NO_FOG_START = 100.0f;

    /** Distance everything of the hand pass would be fog by, which nothing of it reaches. */
    private static final float NO_FOG_END = 200.0f;

    /** Colour the hand pass would fade into, which it never does. */
    private static final Color NO_FOG_COLOR = new Color(1.0f, 1.0f, 1.0f, 1.0f);

    private final BlockPictures pictures;
    private final ItemCubeMeshes cubes;

    /** The arm, meshed while the picture of the hand was found, {@code null} before that. */
    private Array<Mesh> arms;

    /** Layer the picture of the hand lives in, {@code -1} while there is none. */
    private int armLayer = -1;

    /** Set once the missing picture of the hand was reported, so the log holds one line. */
    private boolean reportedMissingPicture;

    /** Set once the place of the arm was reported, so the log holds one line. */
    private boolean reportedPlacement;

    /** Reused matrix of the pose of this frame. */
    private final Matrix4 pose = new Matrix4();

    /** Reused matrix of the arm, and of the block it holds. */
    private final Matrix4 model = new Matrix4();
    private final Matrix4 held = new Matrix4();
    private final Matrix4 placed = new Matrix4();

    /** Squeezes the unit cube of a block into the shape of an arm, see {@link HandMeshes}. */
    private final Matrix4 armBox = new Matrix4();

    /** The card of every kind of item that was held once, keyed by its name. */
    private final ObjectMap<String, Mesh> cards = new ObjectMap<>();

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
        // The mesh of the arm is the unit cube of a block, so the shape of an arm is this matrix: the box
        // spans the width and the thickness of an arm around the middle of it and reaches its length into
        // the view, which is the negative Z axis.
        armBox.idt()
                .translate(-HandMeshes.WIDTH * 0.5f, -HandMeshes.THICKNESS * 0.5f, -HandMeshes.LENGTH)
                .scale(HandMeshes.WIDTH, HandMeshes.THICKNESS, HandMeshes.LENGTH);
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
        return arms != null;
    }

    /**
     * Draws the arm and the block it holds in front of the camera.
     *
     * @param camera camera the world is seen through, the hand is drawn in its frame
     * @param shader shader the world is drawn with, the hand is drawn with it too
     * @param heldStack stack the player holds, empty for a bare hand
     */
    public void render(PerspectiveCamera camera, BlockShader shader, ItemStack heldStack) {
        if (arms == null && !build()) {
            return;
        }
        if (!reportedPlacement) {
            reportedPlacement = true;
            reportPlacement(camera);
        }
        HandPose at = currentPose();
        pose.idt()
                .translate(at.x(), at.y(), at.z())
                .rotate(Vector3.X, at.pitch());
        // The mesh of the arm stands around the origin of the eye, so the matrix that places it is the
        // inverse of the view: the arm follows the eye wherever it looks.
        model.set(camera.view).inv().mul(pose);

        shader.begin(camera, pictures, NO_FOG_COLOR, NO_FOG_START, NO_FOG_END);
        // A hand is a box a player looks at from outside, but the card of a tool is one surface and the
        // pass may run with culling left on by the pass before it: an arm that is culled is an arm that is
        // drawn and never seen, see BlockIconRenderer, which turns culling off for the same reason.
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        for (Mesh mesh : arms) {
            placed.set(model).mul(armBox);
            shader.render(mesh, placed);
        }
        renderHeld(heldStack, shader);
        shader.end();
    }

    /** Draws the block a hand holds at the end of the arm, or the card of any other item. */
    private void renderHeld(ItemStack stack, BlockShader shader) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Block block = stack.item().block();
        if (block == null) {
            // A tool or a material has no cube: it is held as a flat card of its own picture.
            Mesh card = cardOf(stack.item());
            if (card != null) {
                held.idt().translate(0.0f, 0.0f, -HELD_AHEAD).scale(CARD_SCALE, CARD_SCALE, CARD_SCALE);
                placed.set(model).mul(held);
                shader.render(card, placed);
            }
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

    /** The card of an item, meshed on first use, {@code null} when the array holds no picture of it. */
    private Mesh cardOf(Item item) {
        Mesh card = cards.get(item.name());
        if (card != null) {
            return card;
        }
        int layer = pictures.layer(BlockPictures.itemPicture(item));
        if (layer < 0) {
            // The picture of an item may be the picture of a block it shows, which lives in the folder of
            // the blocks, see BlockPictures.
            layer = pictures.layer(item.texture());
        }
        if (layer < 0) {
            return null;
        }
        Color tint = item.tint();
        MeshData data = HandMeshes.flatItem(layer, tint.r, tint.g, tint.b);
        Mesh built = new Mesh(true, data.vertexCount(), data.indexCount(), BlockShader.ATTRIBUTES);
        built.setVertices(data.vertexFloats(), 0, data.vertexCount() * MeshData.FLOATS_PER_VERTEX);
        built.setIndices(data.indexShorts(), 0, data.indexCount());
        cards.put(item.name(), built);
        return built;
    }

    /** Meshes the arm, once the picture of the hand was found in the array. */
    private boolean build() {
        armLayer = pictures.layer(BlockPictures.HAND);
        if (armLayer < 0) {
            if (!reportedMissingPicture) {
                reportedMissingPicture = true;
                LOGGER.warn("The array holds no picture of the hand, it was cut from {}: the arm is not drawn",
                        BlockPictures.SKIN);
            }
            return false;
        }
        // The arm is meshed the way the cube of an item is meshed: one block of the world, alone in an
        // empty section, with every face of it asking for the picture of the hand, see SectionMesher. What
        // the mesh holds is therefore the unit cube of a block and the model matrix squeezes it into the
        // shape of an arm - the very triangles the world is drawn from, which is why an arm cannot be
        // drawn differently from a block.
        Section section = new Section(0);
        section.setRawId(0, 0, 0, Blocks.STONE.id());
        SectionMesher.Blocks blocks = (x, y, z) -> x == 0 && y == 0 && z == 0
                ? Blocks.STONE
                : Blocks.AIR;
        List<MeshData> data = SectionMesher.build(section, 0, 0, 0, blocks, name -> armLayer);
        arms = new Array<>();
        for (MeshData mesh : data) {
            Mesh uploaded = new Mesh(true, mesh.vertexCount(), mesh.indexCount(), BlockShader.ATTRIBUTES);
            uploaded.setVertices(mesh.vertexFloats(), 0, mesh.vertexCount() * MeshData.FLOATS_PER_VERTEX);
            uploaded.setIndices(mesh.indexShorts(), 0, mesh.indexCount());
            arms.add(uploaded);
        }
        LOGGER.info("Meshed the arm of the player from layer {}: {} corners, {} triangles",
                armLayer, data.get(0).vertexCount(), data.get(0).indexCount() / 3);
        return true;
    }

    /**
     * Reports where the arm stands in the picture, once.
     * <p>
     * A hand is placed in the frame of the eye, so a wrong place means a hand that is drawn and never
     * seen. The line names the pose and the two shares of the picture the far end of the arm reaches, so
     * a report of a missing arm says at once whether it stands in the frame at all.
     *
     * @param camera camera the world is seen through
     */
    private void reportPlacement(PerspectiveCamera camera) {
        HandPose at = currentPose();
        float aspect = camera.viewportHeight <= 0.0f ? 1.0f : camera.viewportWidth / camera.viewportHeight;
        float halfHeight = (float) Math.tan(Math.toRadians(camera.fieldOfView) * 0.5);
        float halfWidth = halfHeight * aspect;
        float depth = -(at.z() - HandMeshes.LENGTH);
        int across = Math.round(50.0f + 50.0f * at.x() / (depth * halfWidth));
        int down = Math.round(50.0f - 50.0f * at.y() / (depth * halfHeight));
        LOGGER.info("The arm of the player: pose {}, eye of {} degrees over {}x{}, its end reaches "
                        + "{} % across and {} % down the picture",
                at, camera.fieldOfView, Math.round(camera.viewportWidth),
                Math.round(camera.viewportHeight), across, down);
    }

    /** Layer the picture of the hand lives in, {@code -1} while it was not looked up. */
    public int armLayer() {
        return armLayer;
    }

    @Override
    public void dispose() {
        if (arms != null) {
            for (Mesh mesh : arms) {
                mesh.dispose();
            }
            arms = null;
        }
        for (Mesh card : cards.values()) {
            card.dispose();
        }
        cards.clear();
        armLayer = -1;
    }

    @Override
    public String toString() {
        return "FirstPersonHand(" + (isReady() ? "drawn" : "no arm") + ", " + currentPose() + ")";
    }
}
