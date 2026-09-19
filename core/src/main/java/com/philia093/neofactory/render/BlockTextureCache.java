package com.philia093.neofactory.render;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ObjectMap;
import com.philia093.neofactory.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Loads and caches the pixel art textures used by the game.
 * <p>
 * Every texture is loaded through the shared {@link AssetManager}, so the same
 * image is never uploaded to the GPU twice. Regions are registered lazily by
 * name, which keeps startup fast while still avoiding repeated lookups during
 * rendering.
 */
public class BlockTextureCache {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Folder holding every block texture, relative to the asset root. */
    private static final String BLOCK_FOLDER = "blocks/";

    /** Folder holding the map icons used for the player marker. */
    private static final String MAP_FOLDER = "map/";

    private final AssetManager assets;
    private final ObjectMap<String, TextureRegion> regions = new ObjectMap<>();

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

    /** Amount of regions cached so far. */
    public int cachedRegionCount() {
        return regions.size;
    }

    /** Logs how many textures were loaded, called once the game is running. */
    public void logStatistics() {
        LOGGER.info("Texture cache holds {} regions", regions.size);
    }
}