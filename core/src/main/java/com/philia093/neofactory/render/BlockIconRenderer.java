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

import java.nio.ByteBuffer;
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
 * The cube is drawn once per kind of block into a frame of its own and the pixels of that frame become a
 * texture, so a slot costs the drawing of a picture and no work at all after the first time. The camera
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


    private final BlockShader shader;
    private final BlockPictures pictures;

    /** Cube of every kind of block, meshed the way a lying item is, see {@link ItemCubeMeshes}. */
    private final ItemCubeMeshes cubes;

    /** Frame the cube is drawn into, as large as an icon. */
    private final FrameBuffer frame;

    /** Camera of the icon, looking at the middle of the cube from a corner above it. */
    private final PerspectiveCamera camera = new PerspectiveCamera(FIELD_OF_VIEW, SIZE, SIZE);

    /** Moves the cube to the middle of the frame, because a cube is meshed from the origin. */
    private final Matrix4 model = new Matrix4();

    /** Icon of every kind of block that was asked for, keyed by block id. */
    private final IntMap<TextureRegion> icons = new IntMap<>();

    /** Blocks whose icon could not be drawn, so they are never asked for again. */
    private final IntSet failed = new IntSet();

    /** Viewport the window was drawn through before a frame took over, put back after every bake. */
    private final IntBuffer viewport = BufferUtils.newIntBuffer(4);

    /** Textures behind {@link #icons}, released by {@link #dispose()}. */
    private final Array<Texture> textures = new Array<>();

    /**
     * Creates a renderer and the frame an icon is drawn into.
     *
     * @param shader program the world is drawn with, used for the same look
     * @param pictures pictures of the world the block is drawn from
     */
    public BlockIconRenderer(BlockShader shader, BlockPictures pictures) {
        this.shader = shader;
        this.pictures = pictures;
        this.cubes = new ItemCubeMeshes(pictures);
        this.frame = new FrameBuffer(Pixmap.Format.RGBA8888, SIZE, SIZE, true);
        float half = ItemCubeMeshes.SIZE * 0.5f;
        this.model.idt().translate(-half, -half, -half)
                .scale(ItemCubeMeshes.SIZE, ItemCubeMeshes.SIZE, ItemCubeMeshes.SIZE);
        this.camera.position.set(DIRECTION).scl(DISTANCE);
        this.camera.lookAt(0f, 0f, 0f);
        this.camera.near = DISTANCE - 1f;
        this.camera.far = DISTANCE + 1f;
        this.camera.update();
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
        TextureRegion drawn = bake(block);
        if (drawn == null) {
            return null;
        }
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
    private TextureRegion bake(Block block) {
        Gdx.gl.glGetIntegerv(GL20.GL_VIEWPORT, viewport);
        try {
            frame.begin();
            try {
                draw(block);
                // The frame has to be read while it is still the bound one, because reading reads the
                // frame that is bound - the screen itself as soon as this one is let go.
                return read();
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
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        shader.begin(camera, pictures, NO_FOG, FOG_START, FOG_END);
        for (Mesh mesh : cubes.cubeOf(block)) {
            shader.render(mesh, model);
        }
        shader.end();
    }

    /**
     * Reads the frame and turns it into a texture the size of an icon.
     * <p>
     * A frame is read from its lower left corner while a row of a picture starts at its upper left one, so
     * the reader is asked to turn the rows around; without that every icon would be drawn upside down.
     */
    private TextureRegion read() {
        // The rows come turned around from the reader, so the picture starts at its upper left corner
        // where a frame keeps its lower one.
        byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, SIZE, SIZE, true);
        Pixmap icon = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
        try {
            // A picture is filled through the buffer it owns: the reader of a frame can only write into
            // a direct buffer, and a copy that lives in the heap is refused.
            ByteBuffer target = icon.getPixels();
            target.clear();
            target.put(pixels);
            target.position(0);
            Texture texture = new Texture(icon);
            texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            textures.add(texture);
            return new TextureRegion(texture);
        } finally {
            icon.dispose();
        }
    }

    @Override
    public void dispose() {
        for (Texture texture : textures) {
            texture.dispose();
        }
        textures.clear();
        icons.clear();
        cubes.dispose();
        frame.dispose();
    }

    @Override
    public String toString() {
        return "BlockIconRenderer(" + icons.size + " icons)";
    }
}

