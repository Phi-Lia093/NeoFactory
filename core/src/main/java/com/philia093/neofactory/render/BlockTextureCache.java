package com.philia093.neofactory.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.item.Item;
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
 * The cache also owns the few textures the game creates itself: the single white
 * pixel a tooltip is filled with and the cubes folded from a block tile, see
 * {@link #itemIcon(Item)}. Those are released by {@link #dispose()}, because the
 * {@link AssetManager} does not know them.
 */
public class BlockTextureCache implements Disposable {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Folder holding every block texture, relative to the asset root. */
    private static final String BLOCK_FOLDER = "blocks/";

    /** Folder holding the map icons used for the player marker. */
    private static final String MAP_FOLDER = "map/";

    /** Folder holding the screens and the widgets of the user interface. */
    public static final String GUI_FOLDER = "gui/";

    /** Thickness of the outline a dropped item is drawn with, in pixels of the icon. */
    private static final int OUTLINE_BORDER = 1;

    /** Colour of that outline, a white that reads on a floor of any colour. */
    private static final int OUTLINE_COLOUR = 0xFFFFFFFF;

    private final AssetManager assets;
    private final ObjectMap<String, TextureRegion> regions = new ObjectMap<>();
    private final ObjectMap<String, TextureRegion> icons = new ObjectMap<>();

    /** Cells of every animated sheet, cut once and reused, see {@link #frameRegion}. */
    private final ObjectMap<String, TextureRegion[]> animationFrames = new ObjectMap<>();

    /** Filled cells, painted once per item, see {@link #fluidCellIcon(Item)}. */
    private final ObjectMap<String, TextureRegion> cellIcons = new ObjectMap<>();

    /** Names of the icons that could not be read, so each of them is reported once and not per slot. */
    private final ObjectSet<String> missingIcons = new ObjectSet<>();

    /** Cubes folded from a block tile, keyed by texture name and frame. */
    private final ObjectMap<String, TextureRegion> foldedIcons = new ObjectMap<>();

    /**
     * Draws the icon of a block from the block itself, {@code null} while there is none.
     * <p>
     * It is set while the world is drawn as cubes, because drawing off screen needs a frame buffer; a
     * slot asked for a block before that is given the folded tile instead, see {@link #itemIcon(Item)}.
     */
    private BlockIconRenderer blockIconRenderer;

    /** Outlined icons of dropped items, keyed by texture name and frame. */
    private final ObjectMap<String, TextureRegion> itemOutlines = new ObjectMap<>();

    /** Textures behind {@link #foldedIcons}, released by {@link #dispose()}. */
    private final Array<Texture> foldedTextures = new Array<>();

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
     * Returns one frame of an animated sheet, cutting the sheet the first time it is asked.
     * <p>
     * A block with an animation owns a sheet of frames stacked upwards, and the whole sheet is
     * cut into its cells once and kept: a lake asks for a frame of every cell it covers on
     * every frame of the game, and cutting the picture again each time would fill the heap
     * with regions that are thrown away right after they were drawn.
     * <p>
     * The amount of cells is read from the picture, so a sheet that grew a frame does not have
     * to be announced twice.
     *
     * @param name texture name or path, without extension
     * @param frame index of the wanted frame, wrapped into the sheet
     * @return the frame, or {@code null} when the picture is missing
     */
    public TextureRegion frameRegion(String name, int frame) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        TextureRegion[] cells = animationFrames.get(name);
        if (cells == null) {
            TextureRegion sheet = region(name);
            if (sheet == null) {
                return null;
            }
            int height = sheet.getTexture().getHeight();
            int count = Math.max(1, height / Constants.ITEM_ICON_SIZE);
            cells = new TextureRegion[count];
            for (int index = 0; index < count; index++) {
                cells[index] = sheetCell(name, Constants.ITEM_ICON_SIZE, index);
            }
            animationFrames.put(name, cells);
        }
        return cells[Math.floorMod(frame, cells.length)];
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
     * Returns the icon the interface shows for an item.
     * <p>
     * The world is seen from above, so a block owns a single flat tile. In a slot the
     * tile of a block that fills a whole cell is folded into a small cube, see
     * {@link BlockIconFactory}, which is what makes the item read as the object it
     * stands for. Every other item - a tool, a material and a block that does not
     * fill its cell, such as tall grass or leaves - keeps its flat picture.
     *
     * @param item item to draw
     * @return the icon, or {@code null} when the picture is missing
     */
    public TextureRegion itemIcon(Item item) {
        TextureRegion icon = iconOrNull(item);
        if (icon == null && missingIcons.add(item.name())) {
            // A slot with nothing in it is a player who cannot tell what they carry, so a picture that
            // cannot be read is reported - once, because this is asked for every slot of every screen.
            LOGGER.warn("The icon of '{}' cannot be read, so a slot holding it stays empty", item.name());
        }
        return icon;
    }

    /** The icon of an item, or {@code null} when none of the ways to a picture worked. */
    private TextureRegion iconOrNull(Item item) {
        if (item.isBlockItem() && item.block() != null && !item.block().isTransparent()) {
            // A world of cubes shows the block itself, see BlockIconRenderer; the folded tile is what
            // stands in for it while there is no graphics card that can draw one, or in the flat view.
            TextureRegion cube = blockIconRenderer == null ? null : blockIconRenderer.icon(item.block());
            if (cube == null) {
                cube = blockIcon(item.texture(), item.iconFrame());
            }
            if (cube != null) {
                return cube;
            }
        }
        if (item.isFluidContainer() && item.container().paintsItsWindow()) {
            TextureRegion filled = fluidCellIcon(item);
            if (filled != null) {
                return filled;
            }
        }
        return iconRegion(item.texture(), item.iconFrame());
    }

    /**
     * Installs the renderer that draws the icon of a block from the block itself.
     * <p>
     * The caller owns the renderer and hands its work over here, because the interface asks this cache
     * for every icon it draws. While none is installed the folded tile is used, which is what the flat
     * view and a machine without a frame buffer keep, see {@link BlockIconRenderer}.
     *
     * @param renderer renderer to ask for the icons of blocks, {@code null} to fold the tiles again
     */
    public void setBlockIconRenderer(BlockIconRenderer renderer) {
        this.blockIconRenderer = renderer;
    }

    /**
     * Draws the cell of a fluid with the window painted in the colour of the fluid.
     * <p>
     * A full cell is not a picture of the art pack but one that is painted here: the grey scale
     * cell with its window in the colour of the fluid inside, see
     * {@link CellIconFactory}. The result is kept per item, so the picture of a water cell is
     * built once and handed out for the rest of the session, and the tint of the item stays white
     * because the colour is already in the picture.
     *
     * @param item item that carries a fluid
     * @return the icon, or {@code null} when the picture of the cell cannot be read
     */
    private TextureRegion fluidCellIcon(Item item) {
        Fluid fluid = item.container().content();
        String path = resolvePath(item.texture());
        if (fluid == null || path == null) {
            return null;
        }
        TextureRegion cached = cellIcons.get(item.name());
        if (cached != null) {
            return cached;
        }

        int size = Constants.ITEM_ICON_SIZE;
        Pixmap filled = null;
        try {
            int[] coloured = CellIconFactory.colour(readTile(path, item.iconFrame(), size), size,
                    fluid.color());
            filled = new Pixmap(size, size, Pixmap.Format.RGBA8888);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    filled.drawPixel(x, y, coloured[y * size + x]);
                }
            }
            Texture texture = new Texture(filled);
            texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            foldedTextures.add(texture);
            TextureRegion region = new TextureRegion(texture);
            cellIcons.put(item.name(), region);
            return region;
        } catch (RuntimeException e) {
            LOGGER.error("Unable to paint the cell of '{}'", item.name(), e);
            return null;
        } finally {
            if (filled != null) {
                filled.dispose();
            }
        }
    }

    /**
     * Folds the tile of a block into a cube and caches the result.
     *
     * @param name texture name or path, without extension
     * @param frame zero based frame inside the sheet
     * @return the icon, or {@code null} when the picture cannot be used
     */
    private TextureRegion blockIcon(String name, int frame) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String key = name + '#' + frame;
        TextureRegion cached = foldedIcons.get(key);
        if (cached != null) {
            return cached;
        }
        TextureRegion tile = iconRegion(name, frame);
        if (tile == null) {
            return null;
        }
        TextureRegion icon = foldTile(name, frame);
        if (icon == null) {
            // Without pixels there is nothing to fold, the flat tile still works.
            return tile;
        }
        foldedIcons.put(key, icon);
        return icon;
    }

    /**
     * Reads the pixels of a tile and folds them into a cube.
     * <p>
     * The cube is folded at {@link BlockIconFactory#FOLDED_SIZE} and not at the size of the
     * cell that shows it: the tile is spread over that many pixels first, so the steps along
     * the slanted edges of the cube are four pixels wide instead of sixteen. The texture is
     * drawn into a sixteen pixel cell with a smooth filter, so what the player sees is a
     * cube whose edges fade instead of a staircase, see
     * {@link BlockIconFactory#downscale(int[], int, int)}.
     *
     * @param name texture name or path, without extension
     * @param frame zero based frame inside the sheet
     * @return the icon, or {@code null} when the picture cannot be read
     */
    private TextureRegion foldTile(String name, int frame) {
        String path = resolvePath(name);
        if (path == null) {
            return null;
        }
        int size = BlockIconFactory.FOLDED_SIZE;
        Pixmap folded = null;
        try {
            int[] pixels = iconPixels(path, frame, Constants.ITEM_ICON_SIZE, size, true);
            folded = new Pixmap(size, size, Pixmap.Format.RGBA8888);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    folded.drawPixel(x, y, pixels[y * size + x]);
                }
            }
            Texture texture = new Texture(folded);
            // The cube is finer than the cell it is shown in, so the scaling is left to the
            // graphics card: dropping a texture that is several times the size of a cell is
            // what turns the steps of its edges into soft ones.
            texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
            foldedTextures.add(texture);
            return new TextureRegion(texture);
        } catch (RuntimeException e) {
            LOGGER.error("Unable to fold the icon of '{}'", name, e);
            return null;
        } finally {
            if (folded != null) {
                folded.dispose();
            }
        }
    }

    /**
     * Returns the icon of an item with a bright border around it.
     * <p>
     * A dropped item is drawn with this outline first and its icon on top, which is what
     * keeps it readable on a floor of any colour. The picture is {@code 2} pixels larger
     * than the icon, so a caller draws it one pixel offset of the icon in every direction.
     *
     * @param item item to draw
     * @return the outlined icon, or {@code null} when the picture cannot be read
     */
    public TextureRegion itemOutline(Item item) {
        if (item == null || !item.hasTexture()) {
            return null;
        }
        String key = item.texture() + '#' + item.iconFrame();
        TextureRegion cached = itemOutlines.get(key);
        if (cached != null) {
            return cached;
        }
        TextureRegion outline = buildOutline(item);
        if (outline == null) {
            return null;
        }
        itemOutlines.put(key, outline);
        return outline;
    }

    /** Reads the icon of an item and grows the outline around it. */
    private TextureRegion buildOutline(Item item) {
        String path = resolvePath(item.texture());
        if (path == null) {
            return null;
        }
        int size = Constants.ITEM_ICON_SIZE;
        int border = OUTLINE_BORDER;
        int grown = size + 2 * border;
        Pixmap outlined = null;
        try {
            int[] icon = iconPixels(path, item.iconFrame(), size, isCube(item));
            int[] pixels = BlockIconFactory.outline(icon, size, border, OUTLINE_COLOUR);
            outlined = new Pixmap(grown, grown, Pixmap.Format.RGBA8888);
            for (int y = 0; y < grown; y++) {
                for (int x = 0; x < grown; x++) {
                    outlined.drawPixel(x, y, pixels[y * grown + x]);
                }
            }
            Texture texture = new Texture(outlined);
            texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            foldedTextures.add(texture);
            return new TextureRegion(texture);
        } catch (RuntimeException e) {
            LOGGER.error("Unable to outline the icon of '{}'", item.name(), e);
            return null;
        } finally {
            if (outlined != null) {
                outlined.dispose();
            }
        }
    }

    /** {@code true} when an item is a block that fills its cell, see {@link #itemIcon}. */
    private static boolean isCube(Item item) {
        return item.isBlockItem() && item.block() != null && !item.block().isTransparent();
    }

    /**
     * Reads the icon of a tile, folding a block into its cube when asked.
     *
     * @param path path relative to the asset root, extension included
     * @param frame zero based frame inside the sheet
     * @param size side length of the icon in pixels
     * @param cube {@code true} to fold the tile into the cube of a block item
     * @return the pixels, row by row, packed as RGBA8888
     */
    private static int[] iconPixels(String path, int frame, int size, boolean cube) {
        return iconPixels(path, frame, size, size, cube);
    }

    /**
     * Reads the icon of a tile, folding a block into its cube when asked.
     * <p>
     * The picture and the icon may have different sizes: a cube folded at several times the
     * size of the cell that shows it simply spreads every pixel of the tile over the cells a
     * step of {@code size / tileSize} reaches.
     *
     * @param path path relative to the asset root, extension included
     * @param frame zero based frame inside the sheet
     * @param tileSize side length of one frame of the picture in pixels
     * @param size side length of the icon in pixels
     * @param cube {@code true} to fold the tile into the cube of a block item
     * @return the pixels, row by row, packed as RGBA8888
     */
    private static int[] iconPixels(String path, int frame, int tileSize, int size,
            boolean cube) {
        int[] tile = readTile(path, frame, tileSize);
        if (!cube) {
            return tile;
        }
        int step = Math.max(1, size / tileSize);
        return BlockIconFactory.isometric((x, y) -> tile[(y / step) * tileSize + (x / step)],
                size);
    }

    /**
     * Reads the pixels of one frame of a picture.
     * <p>
     * The picture is read from the file again instead of asking the asset manager,
     * because a texture that reached the GPU does not hand its pixels out.
     *
     * @param path path relative to the asset root, extension included
     * @param frame zero based frame inside the sheet
     * @param size side length of a frame in pixels
     * @return the pixels, row by row, packed as RGBA8888
     */
    private static int[] readTile(String path, int frame, int size) {
        Pixmap sheet = new Pixmap(Gdx.files.internal(path));
        try {
            int[] tile = new int[size * size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    tile[y * size + x] = sheet.getPixel(x, y + frame * size);
                }
            }
            return tile;
        } finally {
            sheet.dispose();
        }
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

    /** Amount of regions cached so far, the item icons and the cubes included. */
    public int cachedRegionCount() {
        return regions.size + icons.size + foldedIcons.size;
    }

    /** Logs how many textures were loaded, called once the game is running. */
    public void logStatistics() {
        LOGGER.info("Texture cache holds {} regions, {} item icons and {} folded block icons",
                regions.size, icons.size, foldedIcons.size);
    }

    @Override
    public void dispose() {
        // Pictures loaded through the asset manager are released by the manager
        // itself, only the textures this cache made belong to it.
        if (whitePixelTexture != null) {
            whitePixelTexture.dispose();
            whitePixelTexture = null;
            whitePixel = null;
        }
        for (Texture texture : foldedTextures) {
            texture.dispose();
        }
        foldedTextures.clear();
        foldedIcons.clear();
        itemOutlines.clear();
        icons.clear();
    }
}