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
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;
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

    /** Folder holding the pictures of the items, relative to the asset root. */
    public static final String ITEMS_FOLDER = "items/";

    /**
     * Name of the picture the arm of the player is drawn from.
     * <p>
     * The arm is no block, so it does not live in the folder of the blocks: it is a region of the skin of
     * a body, cut to the shape of a layer like every other picture, see {@link #ARM_REGION} and
     * {@link FirstPersonHand}.
     */
    public static final String HAND = "entity/hand";

    /** File the picture of the hand is cut from, relative to the asset root. */
    public static final String SKIN = "entity/steve.png";

    /**
     * Region of that file carrying the arm of the body, in pixels.
     * <p>
     * The layout is the one the classic skin uses: X, Y, width and height of the arm inside a body
     * picture of sixty four by thirty two pixels. A test reads this region back out of the file, so a
     * skin that is packed differently fails there instead of showing a stray piece of a face as an arm.
     */
    public static final int[] ARM_REGION = {40, 16, 16, 16};

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
        // The pictures of the items travel with them: a tool or a material has no cube, so the hand of a
        // view holds it as a flat card of its own picture, see FirstPersonHand. Which folder that picture
        // lives in is decided by the file system - the picture of an item is its own one below items/, and
        // the picture of a block item is the picture of that block, which is collected above - so both
        // names are offered and the one without a file is skipped, see collect(String, Array).
        for (Item item : ItemRegistry.all()) {
            if (!item.texture().isEmpty()) {
                collect(item.texture(), names);
                collect(ITEMS_FOLDER + item.texture(), names);
            }
        }
        // The arm of the player is a picture of its own: a view from inside a body shows the arm of that
        // body, and the body picture travels with the art of the entities.
        collect(HAND, names);
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

    /**
     * Name of the layer a picture of an item lives in.
     *
     * @param item item whose picture is asked for
     * @return the name to hand to {@link #layer(String)}
     */
    public static String itemPicture(Item item) {
        return ITEMS_FOLDER + item.texture();
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
        if (picture.isEmpty() || layers.containsKey(picture) || !exists(picture)) {
            return;
        }
        layers.put(picture, names.size);
        names.add(picture);
    }

    /**
     * {@code true} when a picture of the game really exists.
     * <p>
     * Not every name an item carries is a file of its own: the items of a block show the picture of that
     * block, and a shape of a material may share the picture of another one. The array holds the pictures
     * that are there and says nothing about the rest - a name that has no file is skipped here instead of
     * stopping the game while it opens a world.
     *
     * @param picture name of the picture, see {@link #path(String)}
     * @return {@code true} when that file is there
     */
    private static boolean exists(String picture) {
        return Gdx.files.internal(path(picture)).exists();
    }

    /**
     * Path of a picture below the asset root.
     * <p>
     * The rule is the one the whole game follows: a name without a folder is a picture of a block and
     * lives below {@link #FOLDER}, and a name that names its folder is read from there - the pictures of
     * the items live below {@code items/} and the skin of a body below {@code entity/}.
     *
     * @param name name of the picture, either without a folder or with the one it lives in
     * @return the path of the file, relative to the asset root
     */
    public static String path(String name) {
        return name.indexOf('/') >= 0 ? name + ".png" : FOLDER + name + ".png";
    }

    /**
     * Reads one picture, cut to the shape of a layer.
     *
     * @param name name of the picture, relative to {@code blocks/} without extension
     * @return the pixels of one layer of the array
     */
    private static Pixmap load(String name) {
        if (HAND.equals(name)) {
            return loadRegion(SKIN, ARM_REGION);
        }
        // A picture with a folder of its own is read from there - the items live below items/ - and every
        // other name is a picture of a block, see FOLDER.
        Pixmap sheet = new Pixmap(Gdx.files.internal(path(name)));
        if (sheet.getWidth() == TILE && sheet.getHeight() == TILE) {
            return sheet;
        }
        Pixmap picture = new Pixmap(TILE, TILE, Pixmap.Format.RGBA8888);
        picture.setBlending(Pixmap.Blending.None);
        picture.drawPixmap(sheet, 0, 0, 0, 0, TILE, TILE);
        sheet.dispose();
        return picture;
    }

    /**
     * Reads a region of a picture below the asset root, cut to the shape of a layer.
     *
     * @param file file to read, relative to the asset root, with extension
     * @param region X, Y, width and height of the part to cut out, in pixels
     * @return the pixels of one layer of the array
     */
    private static Pixmap loadRegion(String file, int[] region) {
        Pixmap sheet = new Pixmap(Gdx.files.internal(file));
        Pixmap picture = new Pixmap(TILE, TILE, Pixmap.Format.RGBA8888);
        picture.setBlending(Pixmap.Blending.None);
        picture.drawPixmap(sheet, region[0], region[1], 0, 0, region[2], region[3]);
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
