package com.philia093.neofactory.render;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.philia093.neofactory.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Loads and caches the pixel art textures used by the game: the block tiles of
 * the world, the icon of every item and the pictures of the user interface.
 * <p>
 * Every picture is loaded through the shared {@link AssetManager}, so the same
 * image is never uploaded to the GPU twice. Regions are registered lazily by
 * name, which keeps startup fast while still avoiding repeated lookups during
 * rendering.
 * <p>
 * The cache also owns the few textures the game creates itself, for example the
 * single white pixel a tooltip is filled with. Those are released by
 * {@link #dispose()}, because the {@link AssetManager} does not know them.
 */
public class BlockTextureCache implements Disposable {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Folder holding every block texture, relative to the asset root. */
    private static final String BLOCK_FOLDER = "blocks/";

    /** Folder holding the map icons used for the player marker. */
    private static final String MAP_FOLDER = "map/";

    /** Folder holding the screens and the widgets of the user interface. */
    public static final String GUI_FOLDER = "gui/";

    private final AssetManager assets;
    private final ObjectMap<String, TextureRegion> regions = new ObjectMap<>();
    private final ObjectMap<String, TextureRegion> icons = new ObjectMap<>();

    /** Single white pixel, created on first use, {@code null} while unused. */
    private TextureRegion whitePixel;

    /** Texture behind {@link #whitePixel}, {@code null} while unused. */
    private Texture whitePixelTexture;


    /**
     * Creates a cache backed by the given asset manager.
     *
     * @param assets shared asset manager of the game
     */
    public BlockTextureCache(AssetManager assets) {
        this.assets = assets;
    }

    /**
     * Returns a single texture region, loading the image on first use.
     *
     * @param name texture name relative to {@code assets/}, without extension
     * @return the loaded region or {@code null} when the image is missing
     */
    public TextureRegion region(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        TextureRegion cached = regions.get(name);
        if (cached != null) {
            return cached;
        }

        String path = resolvePath(name);
        if (path == null) {
            LOGGER.warn("No texture path known for '{}'", name);
            return null;
        }
        try {
            if (!assets.isLoaded(path, Texture.class)) {
                assets.load(path, Texture.class);
                assets.finishLoadingAsset(path);
            }
        } catch (RuntimeException e) {
            LOGGER.error("Unable to load texture '{}'", path, e);
            return null;
        }

        Texture texture = assets.get(path, Texture.class);
        // Pixel art must not be blurred, see NeoFactoryGame.applyPixelArtSettings.
        texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        TextureRegion region = new TextureRegion(texture);
        regions.put(name, region);
        return region;
    }

    /**
     * Returns a cell of a texture sheet, used for the animated water and the map
     * icons.
     *
     * @param name texture name relative to {@code assets/}, without extension
     * @param cellSize side length of a single cell in pixels
     * @param index zero based cell index, counted row by row from the top left
     * @return the requested cell or {@code null} when the image is missing
     */
    public TextureRegion sheetCell(String name, int cellSize, int index) {
        TextureRegion region = region(name);
        if (region == null) {
            return null;
        }
        Texture texture = region.getTexture();
        int columns = Math.max(1, texture.getWidth() / cellSize);
        int column = index % columns;
        int row = index / columns;
        return new TextureRegion(texture, column * cellSize, row * cellSize, cellSize, cellSize);
    }

    /** Returns a cell of {@code map/map_icons.png}. */
    public TextureRegion mapIcon(int index) {
        return sheetCell(MAP_FOLDER + "map_icons", Constants.MAP_ICON_CELL_SIZE, index);
    }

    /**
     * Resolves the asset path of a texture name.
     * <p>
     * Names may be given either as {@code "grass_top"} or as a path such as
     * {@code "map/map_icons"}.
     *
     * @param name texture name or relative path
     * @return the path of the image, or {@code null} when the name is empty
     */
    public static String resolvePath(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (name.contains("/")) {
            return name + ".png";
        }
        return BLOCK_FOLDER + name + ".png";
    }

    /**
     * Cuts a rectangle out of a texture sheet.
     * <p>
     * The coordinates start at the top left corner of the image, which is the way
     * the sheets of the interface are documented.
     *
     * @param name texture name or path, without extension
     * @param x left edge in pixels
     * @param y top edge in pixels
     * @param width width in pixels
     * @param height height in pixels
     * @return the requested region, or {@code null} when the image is missing
     */
    public TextureRegion region(String name, int x, int y, int width, int height) {
        TextureRegion sheet = region(name);
        if (sheet == null) {
            return null;
        }
        return new TextureRegion(sheet, x, y, width, height);
    }

    /**
     * Returns the icon of an item, loading and caching it on first use.
     * <p>
     * An item icon is 16 by 16 pixels. A few pictures of the art pack are strips
     * of animation frames - the clock and the compass - so the icon is cut out by
     * the frame index, while a static picture always uses frame zero.
     *
     * @param name texture name or path, without extension
     * @param frame zero based frame inside the sheet
     * @return the icon, or {@code null} when the image is missing or the frame
     *         lies outside the sheet
     */
    public TextureRegion iconRegion(String name, int frame) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String key = name + '#' + frame;
        TextureRegion cached = icons.get(key);
        if (cached != null) {
            return cached;
        }
        TextureRegion sheet = region(name);
        if (sheet == null) {
            return null;
        }
        int frames = Math.max(1, sheet.getTexture().getHeight() / Constants.ITEM_ICON_SIZE);
        if (frame < 0 || frame >= frames) {
            LOGGER.warn("Icon '{}' holds {} frames, frame {} does not exist", name, frames, frame);
            return null;
        }
        TextureRegion icon = sheetCell(name, Constants.ITEM_ICON_SIZE, frame);
        icons.put(key, icon);
        return icon;
    }

    /**
     * Returns a single white pixel.
     * <p>
     * The user interface stretches it into plain rectangles, for example the
     * background of a tooltip, which is cheaper than shipping a picture for them.
     * The pixel is created on first use and released by {@link #dispose()}.
     */
    public TextureRegion whitePixel() {
        if (whitePixel == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            pixmap.fill();
            whitePixelTexture = new Texture(pixmap);
            pixmap.dispose();
            whitePixelTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            whitePixel = new TextureRegion(whitePixelTexture);
        }
        return whitePixel;
    }

    /** Amount of regions cached so far, the item icons included. */
    public int cachedRegionCount() {
        return regions.size + icons.size;
    }

    /** Logs how many textures were loaded, called once the game is running. */
    public void logStatistics() {
        LOGGER.info("Texture cache holds {} regions and {} item icons", regions.size, icons.size);
    }

    @Override
    public void dispose() {
        // Pictures loaded through the asset manager are released by the manager
        // itself, only the white pixel this cache made belongs to it.
        if (whitePixelTexture != null) {
            whitePixelTexture.dispose();
            whitePixelTexture = null;
            whitePixel = null;
        }
        icons.clear();
    }
}