package com.philia093.neofactory.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.ScreenUtils;
import com.philia093.neofactory.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.IntBuffer;

/**
 * Draws the icon a slot shows for a block by drawing the block itself.
 * <p>
 * The stack of pictures a cell can hold is flat, so a block used to be shown as its own tile folded into
 * a cube, see {@link BlockIconFactory}. The world, however, is drawn from the meshes of the block, with
 * the place of every face and its share of the light - so a slot that shows a block draws that same cube
 * off screen and keeps the picture of it, and what a player sees in the hand and in a slot is the very
 * block that stands in the world, seen from a corner above.
 * <p>
 * The cube is drawn once per kind of block into a frame of its own, and the icon is the picture of that
 * frame: a slot costs the drawing of a picture and no work at all after the first time. The camera
 * looks at the cube from {@link #DIRECTION} with a narrow field of view, which is the flat look of a
 * block item rather than the wide look of a room.
 * <p>
 * <b>Baking needs a graphics card.</b> Drawing off screen needs a frame buffer, so a renderer is only
 * made while the world itself is drawn as cubes, and a slot asked for a block before that keeps the
 * folded tile, see {@link BlockTextureCache#setBlockIconRenderer(BlockIconRenderer)}.
 */
public class BlockIconRenderer implements Disposable {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Side length of a baked icon, in pixels, finer than the cell it is drawn into. */
    public static final int SIZE = 64;

    /**
     * Direction the camera looks at the cube from, seen from the cube.
     * <p>
     * A share of height above the two sides that are shown is what makes the top face read as a top face:
     * without it the icon is a view along the horizon and the block looks like a wall.
     */
    private static final Vector3 DIRECTION = new Vector3(1f, 0.85f, 1f).nor();

    /** How far the camera stands from the middle of the cube, in blocks. */
    private static final float DISTANCE = 3.9f;

    /** Field of view of the icon camera, narrow so the cube keeps its flat look. */
    private static final float FIELD_OF_VIEW = 12f;

    /** Colour the distance fades into, put out of reach: an icon is never foggy. */
    private static final Color NO_FOG = new Color(0f, 0f, 0f, 1f);

    /** Distance the fog would start at, far behind the cube. */
    private static final float FOG_START = 100f;

    /** Distance everything would be fog by, far behind the cube. */
    private static final float FOG_END = 200f;

    /** Colour the frame is cleared to before the cube is drawn: nothing at all. */
    private static final Color NO_COLOUR = new Color(0f, 0f, 0f, 0f);

    /** Transparency a pixel of an icon has to reach to count as solid, out of {@code 255}. */
    private static final int SOLID_ALPHA = 250;

    private final BlockShader shader;
    private final BlockPictures pictures;

    /** Cube of every kind of block, meshed the way a lying item is, see {@link ItemCubeMeshes}. */
    private final ItemCubeMeshes cubes;

    /**
     * Frames the icons were drawn into, one per icon.
     * <p>
     * A frame keeps the drawing itself and the icon is the picture of that frame, so every icon owns the
     * frame it was drawn in: the drawing is never read back to the game.
     */
    private final Array<FrameBuffer> frames = new Array<>();

    /** Camera of the icon, looking at the middle of the cube from a corner above it. */
    private final PerspectiveCamera camera;

    /** Moves the cube to the middle of the frame, because a cube is meshed from the origin. */
    private final Matrix4 model = new Matrix4();

    /** Icon of every kind of block that was asked for, keyed by block id. */
    private final IntMap<TextureRegion> icons = new IntMap<>();

    /** Blocks whose icon could not be drawn, so they are never asked for again. */
    private final IntSet failed = new IntSet();

    /** Viewport the window was drawn through before a frame took over, put back after every bake. */
    private final IntBuffer viewport = BufferUtils.newIntBuffer(4);

    /**
     * The camera an icon is drawn through.
     * <p>
     * It is built apart from the renderer because it is the whole look of an icon: where the cube of a
     * block lands in its frame and how much of the frame it fills follow from these numbers alone, and
     * {@link #reportAim()} writes the result down when a renderer is made.
     *
     * @return a camera looking at the middle of a cube from a corner above it
     */
    static PerspectiveCamera iconCamera() {
        PerspectiveCamera camera = new PerspectiveCamera(FIELD_OF_VIEW, SIZE, SIZE);
        camera.position.set(DIRECTION).scl(DISTANCE);
        camera.lookAt(0f, 0f, 0f);
        camera.near = DISTANCE - 1f;
        camera.far = DISTANCE + 1f;
        camera.update();
        return camera;
    }

    /**
     * Writes down where the middle of a cube lands in the frame of an icon.
     * <p>
     * Where an icon lands cannot be checked by a test, because the frame it is drawn into needs a graphics
     * card; it is checked by running the game, and this line is what makes that check possible: the middle
     * of the cube of every icon is dead ahead of the camera, so it has to land in the middle of the frame -
     * around {@code (0, 0)}, where {@code (-1, -1)} is the lower left corner of the frame and {@code (1, 1)}
     * its upper right one. A middle that lands anywhere else says the aim of the camera is wrong; a middle
     * that lands there says the drawing into the frame is what to look at.
     */
    private void reportAim() {
        float[] combined = camera.combined.val;
        float depth = combined[15];
        if (depth == 0f) {
            LOGGER.warn("The camera of a block icon has no depth, the middle of its cube is nowhere");
            return;
        }
        LOGGER.info("A block icon is seen from {} through a field of view of {} over a distance of {} of a "
                        + "frame; the middle of the cube lands at ({}, {})",
                camera.position, camera.fieldOfView, DISTANCE,
                combined[12] / depth, combined[13] / depth);
    }

    /**
     * Creates a renderer and the camera an icon is drawn through.
     *
     * @param shader program the world is drawn with, used for the same look
     * @param pictures pictures of the world the block is drawn from
     */
    public BlockIconRenderer(BlockShader shader, BlockPictures pictures) {
        this.shader = shader;
        this.pictures = pictures;
        this.cubes = new ItemCubeMeshes(pictures);
        this.camera = iconCamera();
        float half = ItemCubeMeshes.SIZE * 0.5f;
        this.model.idt().translate(-half, -half, -half)
                .scale(ItemCubeMeshes.SIZE, ItemCubeMeshes.SIZE, ItemCubeMeshes.SIZE);
        reportAim();
    }

    /**
     * Icon of a block, drawn on first use and kept afterwards.
     *
     * @param block block to show
     * @return the icon, or {@code null} when the picture cannot be drawn
     */
    public TextureRegion icon(Block block) {
        TextureRegion cached = icons.get(block.id());
        if (cached != null) {
            return cached;
        }
        if (failed.contains(block.id())) {
            return null;
        }
        FrameBuffer frame = new FrameBuffer(Pixmap.Format.RGBA8888, SIZE, SIZE, true);
        TextureRegion drawn = bake(block, frame);
        if (drawn == null) {
            frame.dispose();
            return null;
        }
        frames.add(frame);
        icons.put(block.id(), drawn);
        return drawn;
    }

    /**
     * Draws the cube of a block off screen and turns the frame into a texture.
     * <p>
     * Drawing into a frame leaves the viewport of the window behind - the frame is drawn through its own
     * and libGDX sets the window back to its whole size afterwards, not to the one the interface asks for.
     * The interface is drawn through a viewport of its own, so a bake that moved it would take the whole
     * interface off the screen; that is why the viewport is read before and put back after, whatever
     * happens in between.
     */
    private TextureRegion bake(Block block, FrameBuffer frame) {
        Gdx.gl.glGetIntegerv(GL20.GL_VIEWPORT, viewport);
        try {
            frame.begin();
            try {
                draw(block);
                if (!reachedTheFrame(block)) {
                    return null;
                }
                // What the frame holds is the icon: the drawing stays where it is drawn and is handed to
                // the interface as the picture of the frame.
                return pictureOf(frame);
            } finally {
                frame.end();
            }
        } catch (RuntimeException e) {
            // A driver that cannot draw off screen costs the folded tile and not the game, and the same
            // block is never drawn again: a failure would otherwise be reported once per slot per frame.
            failed.add(block.id());
            LOGGER.warn("Unable to draw the icon of block '{}', it keeps its folded tile", block.name(), e);
            return null;
        } finally {
            Gdx.gl.glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
            // The interface draws with blending and without depth, which is the state it is given back.
            Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glEnable(GL20.GL_BLEND);
        }
    }

    /** Draws the cube into the frame that is bound. */
    private void draw(Block block) {
        Gdx.gl.glClearColor(NO_COLOUR.r, NO_COLOUR.g, NO_COLOUR.b, NO_COLOUR.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        shader.begin(camera, pictures, NO_FOG, FOG_START, FOG_END);
        for (Mesh mesh : cubes.cubeOf(block)) {
            shader.render(mesh, model);
        }
        shader.end();
    }

    /**
     * Checks what the cube of a block put into the frame, and reports it.
     * <p>
     * A frame that stays empty, or one that is filled with nothing but see-through pixels, is an icon that
     * cannot be seen - and where an icon will look is one of the few things of this game that no test can
     * check, because the frame it is drawn into needs a graphics card. So every icon reports itself once,
     * when it is drawn, and a block whose drawing did not arrive keeps the tile it used to be folded from
     * instead of leaving an empty slot behind: a slot with an old icon in it is a look to report, a slot
     * with nothing in it is a player who cannot tell what they carry.
     * <p>
     * The count is taken while the frame is still the bound one, because reading reads the frame that is
     * bound.
     *
     * @param block block that was drawn
     * @return {@code true} when the frame holds solid pixels, which is what an icon needs
     */
    private static boolean reachedTheFrame(Block block) {
        byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, SIZE, SIZE, false);
        int covered = 0;
        int solid = 0;
        for (int at = 3; at < pixels.length; at += 4) {
            int alpha = pixels[at] & 0xFF;
            if (alpha > 0) {
                covered++;
            }
            if (alpha >= SOLID_ALPHA) {
                solid++;
            }
        }
        LOGGER.info("The icon of '{}' covers {} of the {} pixels of its frame, {} of them solid",
                block.name(), covered, SIZE * SIZE, solid);
        if (covered == 0) {
            LOGGER.warn("The cube of '{}' never reached the frame of its icon, so the frame is empty; the "
                    + "block keeps its folded tile", block.name());
            return false;
        }
        if (solid == 0) {
            LOGGER.warn("Every pixel the cube of '{}' reached its frame with is see-through, so the icon "
                    + "would be invisible; the block keeps its folded tile", block.name());
            return false;
        }
        return true;
    }

    /**
     * The picture of what was drawn into a frame, ready for the interface.
     * <p>
     * A frame is counted from its lower left corner the way the card counts, while the interface draws a
     * picture from its upper left corner: the rows of this region are turned around once, so that the top
     * of the cube is the top of the icon.
     *
     * @param frame frame holding the drawing of one icon
     * @return the picture of that frame
     */
    private static TextureRegion pictureOf(FrameBuffer frame) {
        Texture texture = frame.getColorBufferTexture();
        texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        TextureRegion picture = new TextureRegion(texture);
        picture.flip(false, true);
        return picture;
    }

    @Override
    public void dispose() {
        for (FrameBuffer frame : frames) {
            frame.dispose();
        }
        frames.clear();
        icons.clear();
        cubes.dispose();
    }

    @Override
    public String toString() {
        return "BlockIconRenderer(" + icons.size + " icons)";
    }
}

