package com.philia093.neofactory.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Every picture of the world, stacked into one texture array.
 * <p>
 * A world of cubes draws thousands of faces from a few dozen pictures. In an atlas of one texture they
 * would bleed into each other while the card filters them and a mipmap would mix a stone into the plank
 * beside it; one texture per picture would split a batch of faces into as many draw calls as the batch
 * has pictures. A texture array is neither: every picture is a layer of one texture, a face names the
 * layer it wants, and the filtering stays inside that layer.
 * <p>
 * The array is built once while the game starts and holds one picture of sixteen by sixteen pixels per
 * layer. A sheet of an animation is a strip of frames, so a layer holds its first frame until frames
 * are looked up per tick. A texture array is a feature of OpenGL 3.0, which the launcher asks for.
 */
public class BlockPictures implements Disposable {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Folder holding the pictures of the blocks, relative to the asset root. */
    public static final String FOLDER = "blocks/";

    /** Side length of one picture in pixels, also the side of a block. */
    public static final int TILE = Constants.TILE_SIZE;

    /** Layer of every picture, filled while the array is built. */
    private final ObjectMap<String, Integer> layers = new ObjectMap<>();

    /** Handle of the texture array, {@code 0} while it holds nothing. */
    private int handle;

    /** Amount of pictures the array holds. */
    private int layerCount;

    /** Collects every picture of the game and uploads it as one layer, called once during startup. */
    public void build() {
        Array<String> names = new Array<>();
        for (Block block : BlockRegistry.all()) {
            for (BlockFace face : BlockFace.ALL) {
                collect(block.faces().face(face), names);
                collect(block.faces().overlay(face), names);
            }
            collect(block.texture(), names);
        }
        if (names.size == 0) {
            LOGGER.warn("No picture of any block was found, the world cannot be drawn as cubes");
            return;
        }

        layerCount = names.size;
        handle = Gdx.gl30.glGenTexture();
        Gdx.gl30.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, handle);
        Gdx.gl30.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, GL30.GL_RGBA, TILE, TILE, layerCount, 0,
                GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, null);
        for (int layer = 0; layer < layerCount; layer++) {
            Pixmap picture = load(names.get(layer));
            Gdx.gl30.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, 0, 0, layer, TILE, TILE, 1,
                    GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, picture.getPixels());
            picture.dispose();
        }
        // Pixel art: one texel of a picture is one pixel of a block, so nothing is interpolated.
        Gdx.gl30.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_NEAREST);
        Gdx.gl30.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_NEAREST);
        Gdx.gl30.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
        LOGGER.info("Stacked {} pictures of the world into one texture array", layerCount);
    }

    /**
     * Layer a picture lives in.
     *
     * @param picture name of the picture, relative to {@code blocks/} without extension
     * @return the layer, or {@code -1} when the array does not hold that picture
     */
    public int layer(String picture) {
        Integer layer = layers.get(picture);
        return layer == null ? -1 : layer;
    }

    /** Handle of the texture array, {@code 0} before it was built. */
    public int textureHandle() {
        return handle;
    }

    /** Amount of pictures the array holds. */
    public int layerCount() {
        return layerCount;
    }

    /**
     * Makes the array the one a shader samples.
     *
     * @param unit texture unit to bind it to
     */
    public void bind(int unit) {
        Gdx.gl30.glActiveTexture(GL30.GL_TEXTURE0 + unit);
        Gdx.gl30.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, handle);
    }

    /** Adds a picture to the list, once, in the order the layers are numbered. */
    private void collect(String picture, Array<String> names) {
        if (picture.isEmpty() || layers.containsKey(picture)) {
            return;
        }
        layers.put(picture, names.size);
        names.add(picture);
    }

    /**
     * Reads one picture, cut to the shape of a layer.
     *
     * @param name name of the picture, relative to {@code blocks/} without extension
     * @return the pixels of one layer of the array
     */
    private static Pixmap load(String name) {
        Pixmap sheet = new Pixmap(Gdx.files.internal(FOLDER + name + ".png"));
        if (sheet.getWidth() == TILE && sheet.getHeight() == TILE) {
            return sheet;
        }
        Pixmap picture = new Pixmap(TILE, TILE, Pixmap.Format.RGBA8888);
        picture.setBlending(Pixmap.Blending.None);
        picture.drawPixmap(sheet, 0, 0, 0, 0, TILE, TILE);
        sheet.dispose();
        return picture;
    }

    @Override
    public void dispose() {
        if (handle != 0) {
            Gdx.gl30.glDeleteTexture(handle);
            handle = 0;
        }
        layers.clear();
        layerCount = 0;
    }

    @Override
    public String toString() {
        return "BlockPictures(" + layerCount + " layers)";
    }
}
