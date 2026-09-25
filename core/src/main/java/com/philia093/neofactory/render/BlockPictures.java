package com.philia093.neofactory.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

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
     * Folder holding the pictures of a break in progress, below {@link #FOLDER}.
     * <p>
     * The stages a block is shown coming apart in are no face of any model, so no blockstate names them:
     * they are collected here for {@code BreakOverlay}, one picture per stage.
     */
    public static final String DESTROY_STAGE_FOLDER = "destroy_stage/";

    /** Amount of pictures a break is shown with, one per stage, from {@code 0} to {@code 9}. */
    public static final int DESTROY_STAGES = 10;

    /**
     * Name of the picture the arm of the player is drawn from.
     * <p>
     * The arm is no block, so it does not live in the folder of the blocks: it is a region of the skin of
     * a body, cut to the shape of a layer like every other picture, see {@link #ARM_REGION} and
     * {@link HumanoidRenderer}.
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
        List<String> names = pictureNames(layers, BlockPictures::exists);
        if (names.isEmpty()) {
            LOGGER.warn("No picture of any block was found, the world cannot be drawn as cubes");
            return;
        }

        layerCount = names.size();
        int bodyFaces = 0;
        int bodyFacesWithPixels = 0;
        handle = Gdx.gl30.glGenTexture();
        Gdx.gl30.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, handle);
        Gdx.gl30.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, GL30.GL_RGBA, TILE, TILE, layerCount, 0,
                GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, null);
        for (int layer = 0; layer < layerCount; layer++) {
            String name = names.get(layer);
            Pixmap picture = load(name);
            if (SkinRegions.isSkinFace(name)) {
                bodyFaces++;
                int opaque = countOpaque(picture);
                if (opaque > 0) {
                    bodyFacesWithPixels++;
                    if (bodyFaces <= 3) {
                        LOGGER.info("The face {} lies in layer {} and holds {} of {} pixels", name,
                                layer, opaque, TILE * TILE);
                    }
                } else {
                    LOGGER.warn("The face {} lies in layer {} and holds no pixel at all, a body "
                            + "drawn from it is never seen", name, layer);
                }
            }
            Gdx.gl30.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, 0, 0, layer, TILE, TILE, 1,
                    GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, picture.getPixels());
            picture.dispose();
        }
        if (bodyFaces > 0) {
            LOGGER.info("{} of {} faces of a body carry pixels", bodyFacesWithPixels, bodyFaces);
            if (bodyFacesWithPixels < bodyFaces) {
                LOGGER.warn("The skin {} holds fewer pixels than a body needs, see SkinRegions",
                        SKIN);
            }
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

    /**
     * Name of the picture of one stage of a break in progress.
     *
     * @param stage stage of the break, {@code 0} to {@link #DESTROY_STAGES} minus one
     * @return the name to hand to {@link #layer(String)}
     */
    public static String destroyStagePicture(int stage) {
        return DESTROY_STAGE_FOLDER + "destroy_stage_" + stage;
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

    /**
     * Names of every picture the world may draw, in the order the array stacks them.
     * <p>
     * <b>The model of a state is what a cell is drawn from</b>, so every state of every block is walked
     * here and not only the model named after the block itself: the lower and the upper half of a slab
     * are models of their own, named by a file of
     * {@code assets/blockstates}. A picture the array does not hold is a face the mesher finds no layer
     * for, which is a block the world does not draw at all - a cell that stops a body while nothing is
     * drawn there, and a slot whose icon is baked from a block that draws nothing, which the interface bakes
     * again on every frame.
     * <p>
     * The pictures of the items travel with them: a tool or a material has no cube to stand for it, so its
     * picture is the only art it has and the drop of it is drawn as a board of that picture, one pixel thick,
     * see {@code ItemCubeMeshes}. The array carries it beside the pictures of the blocks. Which folder that
     * picture lives in is decided by the file system - the picture of an item is its own one below
     * {@code items/}, and the picture of a block item is the picture of that block, which is collected above
     * - so both names are offered and the one without a file is skipped, see
     * {@link #collect(String, List, ObjectMap, Predicate)}.
     * <p>
     * The faces of a body are cut out of the skin that draws it, one picture per face of every bone, and
     * the arm of a view is a piece of that skin of its own, see {@link SkinRegions} and
     * {@link HumanoidRenderer}.
     *
     * @param layers map the names are written into, empty for a caller that only reads the list
     * @param exists decides which name of the game has a file behind it, so the list can be read without
     *               a graphics card
     * @return the names, in the order the layers are numbered
     */
    static List<String> pictureNames(ObjectMap<String, Integer> layers, Predicate<String> exists) {
        List<String> names = new ArrayList<>();
        for (Block block : BlockRegistry.all()) {
            // Every face of every box of every model the block may be drawn with is a picture of its own,
            // overlays included: the grass keeps the layer of its biome colour in the array beside the
            // side it lies on.
            for (int state = 0; state < block.states().stateCount(); state++) {
                for (String picture : block.shown(state).model().pictures()) {
                    collect(picture, names, layers, exists);
                }
            }
            collect(block.texture(), names, layers, exists);
        }
        for (Item item : ItemRegistry.all()) {
            if (!item.texture().isEmpty()) {
                collect(item.texture(), names, layers, exists);
                collect(ITEMS_FOLDER + item.texture(), names, layers, exists);
            }
        }
        for (String face : SkinRegions.names()) {
            collect(face, names, layers, exists);
        }
        collect(HAND, names, layers, exists);
        // The stages a block is shown coming apart in are drawn over the cell that is being broken and are
        // no face of any model, so they are collected here, see BreakOverlay.
        for (int stage = 0; stage < DESTROY_STAGES; stage++) {
            collect(destroyStagePicture(stage), names, layers, exists);
        }
        return names;
    }

    /** Adds a picture to the list, once, in the order the layers are numbered. */
    private static void collect(String picture, List<String> names, ObjectMap<String, Integer> layers,
            Predicate<String> exists) {
        if (picture == null || picture.isEmpty() || layers.containsKey(picture)
                || !exists.test(picture)) {
            return;
        }
        layers.put(picture, names.size());
        names.add(picture);
    }

    /**
     * {@code true} when a picture of the game really exists.
     * <p>
     * Not every name an item carries is a file of its own: the items of a block show the picture of that
     * block, and a shape of a material may share the picture of another one. The array holds the pictures
     * that are there and says nothing about the rest - a name that has no file is skipped here instead of
     * stopping the game while it opens a world.
     * <p>
     * The hand is the one picture that is cut out of another file rather than living in one of its own,
     * see {@link #ARM_REGION}: its name has no file, so it is asked for the skin it is cut from.
     *
     * @param picture name of the picture, see {@link #path(String)}
     * @return {@code true} when that file is there
     */
    private static boolean exists(String picture) {
        if (HAND.equals(picture)) {
            return Gdx.files.internal(SKIN).exists();
        }
        if (SkinRegions.isSkinFace(picture)) {
            return Gdx.files.internal(SKIN).exists();
        }
        return Gdx.files.internal(path(picture)).exists();
    }

    /**
     * Path of a picture below the asset root.
     * <p>
     * The rule is the one the whole game follows: a name without a folder is a picture of a block and
     * lives below {@link #FOLDER}, and a name that starts with a folder of the asset root itself - the
     * pictures of the items below {@code items/}, the skin of a body below {@code entity/} - is read
     * from there. A folder that is <i>not</i> a folder of the asset root is a folder inside the
     * blocks: the art a block of several parts keeps together is named that way, so
     * {@code furnace/furnace_top} is read from {@code blocks/furnace/furnace_top.png}.
     *
     * @param name name of the picture, either without a folder or with the one it lives in
     * @return the path of the file, relative to the asset root
     */
    public static String path(String name) {
        return nameOfAFolderOfTheAssetRoot(name) ? name + ".png" : FOLDER + name + ".png";
    }

    /** Folders of the asset root a picture may name to place itself, see {@link #path(String)}. */
    private static final String[] ROOT_FOLDERS = {FOLDER, ITEMS_FOLDER, "entity/", "gui/", "map/",
            "font/", "misc/", "colormap/", "environment/"};

    /** {@code true} when a name starts with a folder of the asset root and is a path of its own. */
    private static boolean nameOfAFolderOfTheAssetRoot(String name) {
        for (String folder : ROOT_FOLDERS) {
            if (name.startsWith(folder)) {
                return true;
            }
        }
        return false;
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
        if (SkinRegions.isSkinFace(name)) {
            return loadSkinFace(name);
        }
        // A picture with a folder of its own is read from there - the items live below items/ - and every
        // other name is a picture of a block, see FOLDER.
        Pixmap sheet = new Pixmap(Gdx.files.internal(path(name)));
        if (sheet.getWidth() == TILE && sheet.getHeight() == TILE) {
            return sheet;
        }
        Pixmap picture = scaleToTile(sheet);
        sheet.dispose();
        return picture;
    }

    /**
     * Copies a picture into a layer, scaled to the size of a layer, pixel by pixel.
     * <p>
     * A sheet that is more than one tile - a strip of animation frames, a picture of a body - is fitted
     * into one layer here. The nearest neighbour is taken, which is what a picture of the art pack
     * wants: it holds whole pixels and no blur.
     *
     * @param sheet picture to copy, larger than a layer
     * @return the layer
     */
    private static Pixmap scaleToTile(Pixmap sheet) {
        Pixmap picture = new Pixmap(TILE, TILE, Pixmap.Format.RGBA8888);
        picture.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                picture.drawPixel(x, y, sheet.getPixel(x * sheet.getWidth() / TILE,
                        y * sheet.getHeight() / TILE));
            }
        }
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
        Pixmap picture = cutRegion(sheet, region);
        sheet.dispose();
        return picture;
    }

    /**
     * Copies one region of a sheet into a layer, pixel by pixel.
     * <p>
     * The pixels are read and written by their coordinates instead of through {@code drawPixmap}: that
     * call of libGDX takes four numbers of a source and of a destination rectangle, and a rectangle
     * asked for in the wrong order is a region of no size at all - a layer of nothing, and a face that
     * is drawn and never seen. A loop of sixteen by sixteen pixels cannot be read the wrong way.
     *
     * @param sheet picture to cut from
     * @param region X, Y, width and height of the part to cut out, in pixels
     * @return the layer, the region in its upper left corner and the rest empty
     */
    private static Pixmap cutRegion(Pixmap sheet, int[] region) {
        Pixmap picture = new Pixmap(TILE, TILE, Pixmap.Format.RGBA8888);
        picture.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < region[3]; y++) {
            for (int x = 0; x < region[2]; x++) {
                picture.drawPixel(x, y, sheet.getPixel(region[0] + x, region[1] + y));
            }
        }
        return picture;
    }

    /** Amount of pixels of a picture that are not fully transparent. */
    private static int countOpaque(Pixmap picture) {
        int opaque = 0;
        for (int y = 0; y < picture.getHeight(); y++) {
            for (int x = 0; x < picture.getWidth(); x++) {
                if (((picture.getPixel(x, y) >>> 24) & 0xFF) >= MIN_VISIBLE_ALPHA) {
                    opaque++;
                }
            }
        }
        return opaque;
    }

    /** Alpha below which a texel is thrown away by the shader, see {@code BlockShader}. */
    private static final int MIN_VISIBLE_ALPHA = 26;

    /**
     * Cuts one face of a body out of its skin.
     * <p>
     * The face is copied into its layer pixel by pixel and is <b>not</b> stretched: one pixel of the
     * skin is one pixel of a block, which is the density the original game draws a body at. A face of
     * four pixels across therefore covers four of the sixteen pixels along the arm it belongs to, and
     * the model of the body names the window inside the layer that does it, see
     * {@link HumanoidModel}. The rest of the layer stays empty, which costs nothing and keeps every
     * picture of the array the same size.
     *
     * @param name name of a face of a body, see {@link SkinRegions#name(String, BlockFace)}
     * @return the pixels of one layer of the array
     */
    private static Pixmap loadSkinFace(String name) {
        String rest = name.substring(SkinRegions.PREFIX.length() + 1);
        for (SkinRegions.Bone bone : SkinRegions.bones()) {
            BlockFace face = faceOf(rest, bone);
            if (face == null) {
                continue;
            }
            return loadRegion(SKIN, SkinRegions.rect(bone.name(), face));
        }
        throw new IllegalArgumentException("The skin holds no face called '" + name + "'");
    }

    /** The face a name behind the name of a bone stands for, {@code null} when it belongs elsewhere. */
    private static BlockFace faceOf(String rest, SkinRegions.Bone bone) {
        String prefix = bone.name() + "_";
        if (!rest.startsWith(prefix)) {
            return null;
        }
        BlockFace face = BlockFace.byName(rest.substring(prefix.length()));
        if (face == null) {
            throw new IllegalArgumentException("'" + rest + "' is no face of the bone " + bone.name());
        }
        return face;
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
