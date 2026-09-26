package com.philia093.neofactory.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
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
import com.philia093.neofactory.block.BlockRegistry;
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
 * frame: a slot costs the drawing of a picture and no work at all after the first time. The camera looks
 * at the cube from a corner above it with parallel rays, which is the flat look of a block item - no edge
 * of it converges and no face of it is squashed towards the viewer - and the frame itself is measured off
 * the cube, so a block is drawn as large as the cell allows.
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
     * without it the icon is a view along the horizon and the block looks like a wall. The share here lifts
     * the eye about thirty degrees above that horizon, which is the corner the original game draws a block
     * item from.
     */
    private static final Vector3 DIRECTION = new Vector3(1f, 0.85f, 1f).nor();

    /**
     * How far the camera stands from the middle of the cube, in blocks.
     * <p>
     * The rays of an icon run parallel, see {@link #iconCamera()}, so the distance has no say in how large
     * a block looks; it only has to put the cube between the near and the far plane of the camera.
     */
    private static final float DISTANCE = 3.3f;

    /**
     * Share of the frame the cube covers at its widest.
     * <p>
     * The frame is square while the outline of a cube seen from this corner is taller than it is wide, so
     * the height of the outline is what sizes the frame and a little room is left at the sides. Almost the
     * whole height with a couple of pixels of margin is what makes a block read in a cell of
     * {@code Constants.ITEM_ICON_SIZE} pixels, where the flat picture of every other item fills its cell
     * from edge to edge.
     */
    private static final float FILL = 0.96f;

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

    /**
     * Least share of an icon the shape of a block has to cover to be worth showing.
     * <p>
     * The frame is the cell a block stands in, so a thin block covers little of it: a tiny pipe is a few
     * pixels wide and a plant is a few leaves in the middle of a slot. Such a drawing is what a player
     * expects - a slot says how large a thing is by the room it leaves - so the number here is low and only
     * catches a frame that stayed empty, where the tile the block was folded from reads better than an
     * empty slot.
     */
    private static final float MINIMUM_COVERAGE = 0.05f;

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

    /**
     * Camera of the icon: a flat look at the cell of a block from a corner above it, see
     * {@link #iconCamera()}.
     */
    private final OrthographicCamera camera;

    /** Moves the shape to the middle of the frame, because a block is meshed from the origin of its cell. */
    private final Matrix4 model = new Matrix4();

    /** Icon of every kind of block that was asked for, keyed by block id. */
    private final IntMap<TextureRegion> icons = new IntMap<>();

    /** Blocks whose icon could not be drawn, so they are never asked for again. */
    private final IntSet failed = new IntSet();

    /** Viewport the window was drawn through before a frame took over, put back after every bake. */
    private final IntBuffer viewport = BufferUtils.newIntBuffer(4);

    /**
     * Builds the camera an icon is drawn through: a flat, square look at the cell from a corner above it.
     * <p>
     * The rays of a picture of a block run parallel instead of meeting in an eye, which is the look the block
     * icons of the original game have: every edge of the block keeps its length and no face of it is squashed
     * towards the viewer. The frame is measured off the <b>cell</b> the shape is drawn in - its eight corners
     * are put on the two axes of the view and the square that holds them all with a share of {@link #FILL} of
     * it is the frame - and not off the shape of the block: a tiny pipe is a few pixels wide, so a slot shows
     * a thin tube in the middle of it and a player reads the size of the pipe from the room it leaves, see
     * {@link #icon(Block)}.
     * <p>
     * The frame is put on the middle of that outline and not on the middle of the cell, so the little room it
     * leaves is shared between the top and the bottom of it. The mesh of an icon is scaled by
     * {@link ItemCubeMeshes#SIZE}, so the size of the cell in the frame is that very number.
     *
     * @return a camera looking at a cell from a corner above it with parallel rays
     */
    static OrthographicCamera iconCamera() {
        Vector3 forward = new Vector3(DIRECTION).scl(-1f);
        // The right hand of the view is the up of the world across the way the camera looks and its up is
        // across both; of the two ways to read that up, the one that keeps the world upright is taken.
        Vector3 right = new Vector3(Vector3.Y).crs(forward).nor();
        Vector3 up = new Vector3(forward).crs(right).nor();
        if (up.y < 0f) {
            up.scl(-1f);
            right.scl(-1f);
        }
        float half = ItemCubeMeshes.SIZE * 0.5f;
        float above = 0f;
        float below = 0f;
        float beside = 0f;
        Vector3 corner = new Vector3();
        for (int x = -1; x <= 1; x += 2) {
            for (int y = -1; y <= 1; y += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    corner.set(x * half, y * half, z * half);
                    float high = corner.dot(up);
                    above = Math.max(above, high);
                    below = Math.max(below, -high);
                    beside = Math.max(beside, Math.abs(corner.dot(right)));
                }
            }
        }
        Vector3 middle = new Vector3(up).scl((above - below) * 0.5f);
        OrthographicCamera camera = new OrthographicCamera();
        camera.viewportWidth = Math.max(above + below, 2f * beside) / FILL;
        camera.viewportHeight = camera.viewportWidth;
        camera.position.set(DIRECTION).scl(DISTANCE).add(middle);
        camera.lookAt(middle);
        camera.near = DISTANCE - 1f;
        camera.far = DISTANCE + 1f;
        camera.update();
        return camera;
    }

    /**
     * Writes down how the icons are looked at.
     * <p>
     * How an icon looks cannot be checked by a test, because the frame it is drawn into needs a graphics
     * card; the numbers it is drawn from are written down here instead, so the frame an icon is built from
     * can be read next to what a player sees. {@link #prime()} then reports how many icons came out well
     * and every icon that came out too poor to show is reported by {@link #reachedTheFrame(Block)}.
     */
    private void reportLook() {
        LOGGER.info("A block icon is looked at with parallel rays from {}, a frame of {} blocks holds the "
                        + "whole cell with {} of it covered",
                camera.position, camera.viewportWidth, FILL);
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
        reportLook();
    }

    /**
     * Draws the icon of every kind of block that fills its cell, before any slot asks for one.
     * <p>
     * An icon is drawn into a frame of its own, and the interface draws the picture of that frame in the
     * very frame it was drawn in when the first slot that holds a block is reached. Nothing about that
     * should matter - a picture of a frame is a picture like any other - but a slot that asked for the
     * very first icon of a world stayed empty until a screen that holds many blocks, such as the
     * inventory, asked for one: a screen full of icons is what a player should never have to open to see
     * what they carry. So every icon is drawn here instead, while the world is being put together and no
     * frame is half drawn, and a slot only ever asks for an icon that is already there.
     * <p>
     * A block that does not fill its cell - grass, leaves, anything with a shape of its own - has no cube
     * to draw and is left alone: the interface folds its tile instead, see {@link BlockTextureCache}.
     */
    public void prime() {
        int drawn = 0;
        for (Block block : BlockRegistry.all()) {
            if (block.isTransparent()) {
                continue;
            }
            if (icon(block) != null) {
                drawn++;
            }
        }
        LOGGER.info("{} of the {} kinds of block are drawn as themselves in a slot, the rest keep their "
                + "folded tile", drawn, BlockRegistry.count());
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
            // A frame that stayed too empty is a drawing that cannot be shown, and the block is remembered
            // as one that cannot be drawn: a bake that is tried again would build a frame, ask the card
            // for its pixels and report the failure once per slot and frame, which is a screen no player
            // can use. The block keeps the folded tile instead, see BlockTextureCache.
            failed.add(block.id());
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
     * Checks what the cube of a block put into the frame, and reports it when it is too little.
     * <p>
     * A frame that stays empty, or one that is filled with nothing but see-through pixels, is an icon that
     * cannot be seen - and where an icon will look is one of the few things of this game that no test can
     * check, because the frame it is drawn into needs a graphics card. So a block whose drawing did not
     * arrive keeps the tile it used to be folded from instead of leaving an empty slot behind: a slot with
     * an old icon in it is a look to report, a slot with nothing in it is a player who cannot tell what
     * they carry. Only a drawing that is too poor to show is written down - {@link #prime()} reports how
     * many icons came out well as a whole.
     * <p>
     * The count is taken while the frame is still the bound one, because reading reads the frame that is
     * bound.
     *
     * @param block block that was drawn
     * @return {@code true} when the frame holds a picture worth showing in a slot
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
        if (covered < SIZE * SIZE * MINIMUM_COVERAGE) {
            LOGGER.warn("The cube of '{}' covers only {} of the {} pixels of its icon, which is too little "
                    + "to read; the block keeps its folded tile", block.name(), covered, SIZE * SIZE);
            return false;
        }
        LOGGER.debug("The icon of '{}' covers {} of the {} pixels of its frame, {} of them solid",
                block.name(), covered, SIZE * SIZE, solid);
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

