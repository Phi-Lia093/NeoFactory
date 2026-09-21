package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Writes a sheet that shows how the block items of the game look once their tile is
 * folded into a cube.
 * <p>
 * The sheet is the review tool of the icon work: the upper row holds the plain tile
 * the game shows in a slot, the lower row the same blocks as a cube, four times as
 * large and with the tint of the block applied, so the icons can be judged without
 * starting the game. It is written to
 * {@code core/build/reports/block-icons-preview.png}, a second sheet with fewer
 * blocks at eight times the size goes to {@code block-icons-detail.png}.
 * <p>
 * The blocks come from the item registry, which keeps the sheet in step with the
 * game. A block item whose block is transparent - tall grass, leaves - is skipped,
 * because such a block keeps its flat picture in a slot.
 */
class BlockIconPreviewTest {

    /** Assets of the project, the tests run inside the core module. */
    private static final Path ASSETS = Path.of("..", "assets");

    /** Picture the sheet is written to. */
    private static final Path PREVIEW = Path.of("build", "reports", "block-icons-preview.png");

    /** Side length of one icon, the cell size of the interface. */
    private static final int TILE = Constants.ITEM_ICON_SIZE;

    /** Zoom the overview is drawn with, so the shape of a cube is readable. */
    private static final int SCALE = 4;

    /** Picture that shows a few blocks enlarged enough to judge the details. */
    private static final Path DETAILS = Path.of("build", "reports", "block-icons-detail.png");

    /** Zoom of the detail sheet, one source pixel becomes a block of its own. */
    private static final int DETAIL_SCALE = 8;

    /** Blocks of the detail sheet: a soil, a plant, a trunk and a built material. */
    private static final String[] DETAIL_BLOCKS = {"stone", "grass", "log_oak", "planks_oak"};

    /** Space between two cells and around the sheet. */
    private static final int GAP = 4;

    /** Amount of rows of a sheet: the plain tile and the cube below it. */
    private static final int ROWS = 2;

    /** Colour the sheet is filled with, a dark grey that shows empty corners. */
    private static final int BACKDROP = 0xFF202020;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theCubeIsWrittenNextToThePlainTile() throws IOException {
        List<Item> blocks = blockItems();
        assertFalse(blocks.isEmpty(), "no block item to review");

        BufferedImage sheet = writeSheet(PREVIEW, blocks, SCALE);

        assertTrue(Files.isRegularFile(PREVIEW), "the preview exists");
        assertEquals(GAP + blocks.size() * (TILE * SCALE + GAP), sheet.getWidth(), "columns");
        assertEquals(GAP + ROWS * (TILE * SCALE + GAP), sheet.getHeight(), "rows");
    }

    @Test
    void theShapeOfACubeIsWrittenEnlargedAsWell() throws IOException {
        List<Item> blocks = pick(blockItems(), DETAIL_BLOCKS);
        assertEquals(DETAIL_BLOCKS.length, blocks.size(), "every wanted block exists");

        BufferedImage sheet = writeSheet(DETAILS, blocks, DETAIL_SCALE);

        assertTrue(Files.isRegularFile(DETAILS), "the detail sheet exists");
        assertEquals(GAP + blocks.size() * (TILE * DETAIL_SCALE + GAP), sheet.getWidth(),
                "columns");
    }

    /**
     * Block items an icon is drawn for, in the order of the registry.
     *
     * @return the items whose block is opaque, so its tile can be folded
     */
    private static List<Item> blockItems() {
        List<Item> blocks = new ArrayList<>();
        for (Item item : ItemRegistry.all()) {
            if (item.isBlockItem() && item.hasTexture() && item.block() != null
                    && !item.block().isTransparent()) {
                blocks.add(item);
            }
        }
        return blocks;
    }

    /**
     * Reads the tile of an item, the frame it uses included.
     *
     * @param item item whose picture is read
     * @return the pixels of the tile, row by row, packed as RGBA8888
     * @throws IOException when the picture cannot be read
     */
    private static int[] readTile(Item item) throws IOException {
        Path file = ASSETS.resolve(BlockTextureCache.resolvePath(item.texture()));
        BufferedImage sheet = ImageIO.read(file.toFile());
        int[] tile = new int[TILE * TILE];
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                int argb = sheet.getRGB(x, y + item.iconFrame() * TILE);
                // ARGB8888 of the reader to the RGBA8888 of the game.
                tile[y * TILE + x] = (argb << 8) | (argb >>> 24);
            }
        }
        return tile;
    }

    /** Wraps the pixels of a tile the way the game wraps them. */
    private static BlockIconFactory.IconSource sourceOf(int[] tile) {
        return (x, y) -> tile[y * TILE + x];
    }

    /**
     * Draws the plain tile and every projection of one block into a sheet.
     *
     * @param file picture to write
     * @param blocks blocks to draw, one per column
     * @param scale zoom, one source pixel becomes this many pixels wide
     * @return the sheet, so the caller can check its geometry
     * @throws IOException when the picture cannot be written
     */
    private static BufferedImage writeSheet(Path file, List<Item> blocks, int scale)
            throws IOException {
        int cell = TILE * scale;
        BufferedImage sheet = new BufferedImage(GAP + blocks.size() * (cell + GAP),
                GAP + ROWS * (cell + GAP), BufferedImage.TYPE_INT_ARGB);
        fill(sheet, BACKDROP);

        for (int column = 0; column < blocks.size(); column++) {
            Item item = blocks.get(column);
            int[] tile = readTile(item);
            int left = GAP + column * (cell + GAP);
            draw(sheet, left, GAP, tile, item.tint(), scale);
            draw(sheet, left, GAP + cell + GAP,
                    BlockIconFactory.isometric(sourceOf(tile), TILE), item.tint(), scale);
        }

        Files.createDirectories(file.getParent());
        ImageIO.write(sheet, "png", file.toFile());
        System.out.println("block icon sheet written to " + file.toAbsolutePath() + ": "
                + blocks.size() + " blocks at " + scale + " times the size, the upper row"
                + " holds the plain tile, the lower one the cube");
        return sheet;
    }

    /**
     * Picks items by their technical name, in the order of the names.
     *
     * @param blocks items to pick from
     * @param names names of the wanted items
     * @return the picked items, a name that does not exist is skipped
     */
    private static List<Item> pick(List<Item> blocks, String[] names) {
        List<Item> picked = new ArrayList<>();
        for (String name : names) {
            for (Item item : blocks) {
                if (item.name().equals(name)) {
                    picked.add(item);
                    break;
                }
            }
        }
        return picked;
    }

    /**
     * Draws one icon into the sheet, enlarged and tinted like the game draws it.
     *
     * @param sheet picture the icon goes into
     * @param left left edge of the cell
     * @param top upper edge of the cell
     * @param icon pixels of the icon, row by row, packed as RGBA8888
     * @param tint colour multiplied with the icon
     * @param scale zoom, one source pixel becomes this many pixels wide
     */
    private static void draw(BufferedImage sheet, int left, int top, int[] icon, Color tint,
            int scale) {
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                int colour = tinted(icon[y * TILE + x], tint);
                if (alpha(colour) == 0) {
                    continue;
                }
                int[] block = new int[scale * scale];
                Arrays.fill(block, toArgb(colour));
                sheet.setRGB(left + x * scale, top + y * scale, scale, scale, block, 0, scale);
            }
        }
    }

    /** Multiplies an icon with the tint of its item, the way the batch does it. */
    private static int tinted(int colour, Color tint) {
        return pack(channel(red(colour) * tint.r), channel(green(colour) * tint.g),
                channel(blue(colour) * tint.b), channel(alpha(colour) * tint.a));
    }

    /** Fills the whole sheet with one colour. */
    private static void fill(BufferedImage sheet, int argb) {
        for (int y = 0; y < sheet.getHeight(); y++) {
            for (int x = 0; x < sheet.getWidth(); x++) {
                sheet.setRGB(x, y, argb);
            }
        }
    }

    /** RGBA8888 of the game to the ARGB8888 the picture writer expects. */
    private static int toArgb(int colour) {
        return (alpha(colour) << 24) | ((colour >>> 8) & 0xFFFFFF);
    }

    private static int channel(float value) {
        return Math.min(Math.max(Math.round(value), 0), 0xFF);
    }

    private static int red(int colour) {
        return (colour >>> 24) & 0xFF;
    }

    private static int green(int colour) {
        return (colour >>> 16) & 0xFF;
    }

    private static int blue(int colour) {
        return (colour >>> 8) & 0xFF;
    }

    private static int alpha(int colour) {
        return colour & 0xFF;
    }

    private static int pack(int r, int g, int b, int a) {
        return (r << 24) | (g << 16) | (b << 8) | a;
    }
}
