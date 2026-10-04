package com.philia093.neofactory.gui;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.item.Batteries;
import com.philia093.neofactory.item.BatteryChemistry;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.render.BatteryIcon;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paints the cells of the industry the way a slot of the game shows them and writes the result to a picture.
 * <p>
 * The picture lies in {@code core/build/reports/battery-preview.png} and is meant to be looked at. Its upper
 * half is the fifteen cells of {@link Batteries}, one row per tier and a pair of columns per chemistry, the
 * first of the pair a full cell and the second a spent one, so the colours of the five chemistries and the
 * two sizes of a pack stand next to each other. Its lower half is one cell of each size drawn at every amount
 * of charge the game can show, so the fill of a window reads as the charge it stands for.
 * <p>
 * <b>Every tile is cut out of the very strip the game draws it from, with the very frame
 * {@link BatteryIcon} picks for an amount of charge.</b> The case is therefore no second copy of the
 * arithmetic of the icons: it fails when a preview would show a cell that does not follow what is left in it,
 * which is how the picture stays worth looking at, see {@code BatteryIconTest} for the art itself.
 */
class BatteryPreviewTest {

    /** Assets of the project, the tests run inside the core module. */
    private static final Path ASSETS = Path.of("..", "assets");

    /** Picture the preview is written to. */
    private static final Path PREVIEW = Path.of("build", "reports", "battery-preview.png");

    /** Side of one tile of the picture, the size an icon of the game is drawn at. */
    private static final int SIDE = Constants.ITEM_ICON_SIZE;

    /** Space around the tiles of the picture. */
    private static final int MARGIN = 4;

    /** Space between two tiles. */
    private static final int GAP = 2;

    /** Thickness of the line between the two halves of the picture. */
    private static final int RULE = 2;

    /** Colour the picture is filled with, a dark grey that shows the tiles. */
    private static final int BACKDROP = 0xFF202020;

    /** Colour of the line between the two halves. */
    private static final int SEPARATOR = 0xFF707070;

    /** Amounts of charge the lower half of the picture shows, from a spent cell to a full one. */
    private static final float[] CHARGES = {0.0f, 0.25f, 0.5f, 0.75f, 1.0f};

    /** Amount of sizes a pack of a battery comes in, the two rows of the lower half. */
    private static final int SIZES = 2;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    /** Width of the picture, the wider of its two halves. */
    private static int width() {
        return MARGIN + Math.max(BatteryChemistry.values().length * 2, CHARGES.length) * (SIDE + GAP);
    }

    /** Height of the picture, the two halves and the line between them. */
    private static int height() {
        return MARGIN + (Batteries.TIERS.size() + SIZES) * (SIDE + GAP) + RULE + MARGIN;
    }

    /** Left edge of a column of tiles. */
    private static int xOf(int column) {
        return MARGIN + column * (SIDE + GAP);
    }

    /** Upper edge of a row of tiles. */
    private static int yOf(int row) {
        return MARGIN + row * (SIDE + GAP);
    }

    @Test
    void theCellsOfTheIndustryArePaintedWithWhatIsLeftInThem() throws IOException {
        BufferedImage picture = new BufferedImage(width(), height(), BufferedImage.TYPE_INT_ARGB);
        fill(picture);

        // The fifteen cells of the table, full and spent: one row per tier, a pair of columns per chemistry
        // and the two ends of a charge next to each other, so the colour of a chemistry reads as what a player
        // sees at it and a pack of either size stands out.
        for (int row = 0; row < Batteries.TIERS.size(); row++) {
            Voltage tier = Batteries.TIERS.get(row);
            for (int chemistry = 0; chemistry < Batteries.of(tier).size(); chemistry++) {
                Batteries.Cell cell = Batteries.of(tier).get(chemistry);
                assertFalse(Arrays.equals(tileOf(cell, 1.0f), tileOf(cell, 0.0f)),
                        "a full cell and a spent one of " + cell + " are the same picture");

                drawTile(picture, cell, 1.0f, xOf(chemistry * 2), yOf(row));
                drawTile(picture, cell, 0.0f, xOf(chemistry * 2 + 1), yOf(row));
            }
        }

        // And the fill of a window, one cell of each size at every amount of charge the game shows. The line
        // between the two halves tells them apart, because a picture of sixteen cells and a charge in it
        // would read as one block otherwise.
        int rule = yOf(Batteries.TIERS.size()) - GAP;
        for (int x = MARGIN; x < width() - MARGIN; x++) {
            for (int y = rule; y < rule + RULE; y++) {
                picture.setRGB(x, y, SEPARATOR);
            }
        }
        for (int size = 0; size < SIZES; size++) {
            Batteries.Cell cell = Batteries.of(BatteryChemistry.LITHIUM,
                    size == 0 ? Voltage.LOW : Voltage.HIGH);
            for (int charge = 0; charge < CHARGES.length; charge++) {
                if (charge > 0) {
                    assertFalse(Arrays.equals(tileOf(cell, CHARGES[charge - 1]), tileOf(cell, CHARGES[charge])),
                            "the window of " + cell + " does not follow the charge of its stack");
                }
                drawTile(picture, cell, CHARGES[charge], xOf(charge), yOf(Batteries.TIERS.size() + size));
            }
        }

        ImageIO.write(picture, "png", PREVIEW.toFile());
        assertTrue(Files.isRegularFile(PREVIEW), "the preview was not written");
        assertTrue(shades(picture) > CHARGES.length + BatteryChemistry.values().length,
                "the preview holds fewer colours than the cells of the table");
    }

    /** Draws the icon of a cell at an amount of charge into a corner of the picture. */
    private static void drawTile(BufferedImage picture, Batteries.Cell cell, float charge, int x, int y) {
        int[] tile = tileOf(cell, charge);
        for (int row = 0; row < SIDE; row++) {
            for (int column = 0; column < SIDE; column++) {
                int pixel = tile[row * SIDE + column];
                // A picture of the game is stored with an alpha channel, so a tile is laid over the backdrop
                // and the see through part of a cell really stays the backdrop.
                if ((pixel >>> 24) != 0) {
                    picture.setRGB(x + column, y + row, pixel);
                }
            }
        }
    }

    /**
     * The icon of a cell at an amount of charge, row by row, packed the way a picture of a pixel is read.
     * <p>
     * The frame is what {@link BatteryIcon} picks out of the charge of a stack of the cell, which is how the
     * picture and the game stay the same reading.
     */
    private static int[] tileOf(Batteries.Cell cell, float charge) {
        int[] strip = stripOf(cell);
        int frames = strip.length / (SIDE * SIDE);
        int frame = BatteryIcon.frameOf(cell, charged(cell, charge), frames);
        return Arrays.copyOfRange(strip, frame * SIDE * SIDE, (frame + 1) * SIDE * SIDE);
    }

    /** The whole strip of a cell, frame by frame, read from its file. */
    private static int[] stripOf(Batteries.Cell cell) {
        Item item = ItemRegistry.byName(Batteries.itemNameOf(cell));
        Path file = ASSETS.resolve(BlockTextureCache.resolvePath(item.texture()));
        BufferedImage picture;
        try {
            picture = ImageIO.read(file.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + file, e);
        }
        int[] strip = new int[picture.getWidth() * picture.getHeight()];
        for (int y = 0; y < picture.getHeight(); y++) {
            for (int x = 0; x < picture.getWidth(); x++) {
                strip[y * picture.getWidth() + x] = picture.getRGB(x, y);
            }
        }
        return strip;
    }

    /** A stack of a cell that holds the given share of what it was built for. */
    private static ItemStack charged(Batteries.Cell cell, float share) {
        ItemStack stack = ItemStack.of(ItemRegistry.byName(Batteries.itemNameOf(cell)), 1);
        stack.setDamage(Math.round(cell.capacity() * (1.0f - share)));
        return stack;
    }

    /** Fills the picture with the backdrop. */
    private static void fill(BufferedImage picture) {
        for (int y = 0; y < picture.getHeight(); y++) {
            for (int x = 0; x < picture.getWidth(); x++) {
                picture.setRGB(x, y, BACKDROP);
            }
        }
    }

    /** Amount of colours the picture is painted in, the check that a preview shows something. */
    private static int shades(BufferedImage picture) {
        Set<Integer> shades = new HashSet<>();
        for (int y = 0; y < picture.getHeight(); y++) {
            for (int x = 0; x < picture.getWidth(); x++) {
                shades.add(picture.getRGB(x, y));
            }
        }
        return shades.size();
    }
}
