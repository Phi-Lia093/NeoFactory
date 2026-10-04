package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.item.Batteries;
import com.philia093.neofactory.item.BatteryChemistry;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the strips the cells of the industry are drawn from and the frame of one a stack shows.
 * <p>
 * <b>A battery is the one icon the game reads out of its stack.</b> Its picture is a strip that holds the
 * window of the cell at every amount it may carry, and {@link BatteryIcon} picks the frame of the amount a
 * stack holds. What this case asks is whether the art and the code still agree: that a strip carries a
 * frame per amount of charge, that its window is filled from its bottom up, that the colour poured into it
 * is the colour of the chemistry, and that the frame a stack asks for is the frame that shows what is left
 * in it. None of it needs a window - the pictures are read the way the audit of the art reads them.
 * <p>
 * <b>The window of a cell is read out of the picture itself</b>, as every pixel that differs between the
 * first frame of the strip and the last one. The case therefore never repeats the window the script that
 * drew the strips had to be told, see {@code tools/verify/import_batteries.ps1}, and a pack whose window
 * moved is caught here as well.
 */
class BatteryIconTest {

    /** Art root of the project, the tests run inside the core module. */
    private static final Path ASSETS = TestRegistries.ASSETS;

    /** Side length of one frame of a strip, the size an icon is drawn at. */
    private static final int SIDE = Constants.ITEM_ICON_SIZE;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theStripOfACellCarriesAFrameForEveryAmountOfCharge() {
        for (Batteries.Cell cell : Batteries.all()) {
            BufferedImage strip = pictureOf(cell);
            int frames = framesOf(strip);
            Window window = windowOf(strip, frames);

            assertEquals(window.height, frames - 1,
                    cell + " carries a frame per amount of rows its window has, an empty cell included");
            assertEquals(Batteries.isSmall(cell.voltage()) ? 7 : 8, frames,
                    cell + " is drawn from the pack of its size, see Batteries#isSmall");
            assertEquals(Batteries.isSmall(cell.voltage()) ? 2 : 6, window.width,
                    "a cell of the low voltage shows a narrow column of charge and a larger one a plate");
        }
    }

    @Test
    void theWindowIsFilledFromItsBottomUp() {
        for (Batteries.Cell cell : Batteries.all()) {
            BufferedImage strip = pictureOf(cell);
            int frames = framesOf(strip);
            Window window = windowOf(strip, frames);

            for (int frame = 0; frame < frames; frame++) {
                assertEquals(window.height - frame, filledRowsOf(strip, frames, window, frame),
                        "frame " + frame + " of " + cell + " shows what is left of it");
            }
        }
    }

    @Test
    void theWindowIsPouredInTheColourOfItsChemistry() {
        for (Batteries.Cell cell : Batteries.all()) {
            BufferedImage strip = pictureOf(cell);
            int frames = framesOf(strip);
            Window window = windowOf(strip, frames);
            Color colour = cell.chemistry().colour();
            int empty = frames - 1;

            for (int frame = 0; frame < frames; frame++) {
                for (int row = 0; row < window.height; row++) {
                    // The rows of a window are emptied from its top down, so the rows below the frame are
                    // the ones the colour is still in, see the note on the script that drew the strips.
                    boolean poured = row >= frame;
                    for (int x = window.x; x < window.x + window.width; x++) {
                        int grey = pixel(strip, empty, x, window.y + row);
                        assertEquals(poured ? pour(grey, colour) : grey,
                                pixel(strip, frame, x, window.y + row),
                                "the window of " + cell + " in frame " + frame + " at row " + row);
                    }
                }
            }
        }
    }

    @Test
    void aStackIsDrawnFromTheFrameOfTheAmountItHolds() {
        for (Batteries.Cell cell : Batteries.all()) {
            BufferedImage strip = pictureOf(cell);
            int frames = framesOf(strip);
            Window window = windowOf(strip, frames);

            assertEquals(0, BatteryIcon.frameOf(cell, charged(cell, 1.0f), frames),
                    "a cell that holds everything is the first frame of its strip");
            assertEquals(frames - 1, BatteryIcon.frameOf(cell, charged(cell, 0.0f), frames),
                    "and one that is spent is the last one");
            assertEquals(frames - 1, BatteryIcon.frameOf(cell, charged(cell, 1.0f / cell.capacity()), frames),
                    "a cell with one unit left is a spent cell and not one that is full");
            assertEquals(0, BatteryIcon.frameOf(cell, ItemStack.EMPTY, frames),
                    "a slot that holds nothing is drawn as the item itself, which is the full cell");

            int half = BatteryIcon.frameOf(cell, charged(cell, 0.5f), frames);
            assertEquals(window.height / 2, filledRowsOf(strip, frames, window, half),
                    "a cell that is half way is drawn half full, see " + cell);
        }

        Batteries.Cell cell = Batteries.of(BatteryChemistry.LITHIUM, Voltage.HIGH);
        ItemStack stack = charged(cell, 0.5f);

        assertEquals(0, BatteryIcon.frameOf(null, stack, 8), "an item that is no battery has one picture");
        assertEquals(0, BatteryIcon.frameOf(cell, stack, 1), "and so does a picture of a single frame");
        assertEquals(0, BatteryIcon.frameOf(cell, null, 8), "a slot that holds nothing is frame zero");
    }

    /** The strip of a cell, read from its file. */
    private static BufferedImage pictureOf(Batteries.Cell cell) {
        Item item = ItemRegistry.byName(Batteries.itemNameOf(cell));
        assertNotNull(item, Batteries.itemNameOf(cell) + " is a cell of the table and has no item");
        Path file = ASSETS.resolve(BlockTextureCache.resolvePath(item.texture()));
        try {
            BufferedImage image = ImageIO.read(file.toFile());
            assertNotNull(image, file + " cannot be read as a picture");
            return image;
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + file, e);
        }
    }

    /** Amount of frames a strip of a cell is stacked of. */
    private static int framesOf(BufferedImage strip) {
        assertEquals(0, strip.getHeight() % SIDE, "a strip is a whole number of frames, one above the other");
        assertEquals(SIDE, strip.getWidth(), "and one tile wide");
        return strip.getHeight() / SIDE;
    }

    /** One pixel of a frame of a strip, packed as ARGB. */
    private static int pixel(BufferedImage strip, int frame, int x, int y) {
        return strip.getRGB(x, frame * SIDE + y);
    }

    /** A stack of a cell that holds the given share of what it was built for. */
    private static ItemStack charged(Batteries.Cell cell, float share) {
        ItemStack stack = ItemStack.of(ItemRegistry.byName(Batteries.itemNameOf(cell)), 1);
        stack.setDamage(Math.round(cell.capacity() * (1.0f - share)));
        return stack;
    }

    /** The window of a cell inside its picture: the pixels that carry the colour of its chemistry. */
    private record Window(int x, int y, int width, int height) {
    }

    /**
     * Finds the window of a cell, which is every pixel that differs between a full cell and a spent one.
     * <p>
     * A window is a rectangle and the two frames are the two ends of the same charge, so what one of them
     * carries and the other does not is exactly the glass of the cell.
     */
    private static Window windowOf(BufferedImage strip, int frames) {
        int empty = frames - 1;
        int left = SIDE;
        int top = SIDE;
        int right = -1;
        int bottom = -1;
        for (int y = 0; y < SIDE; y++) {
            for (int x = 0; x < SIDE; x++) {
                if (pixel(strip, 0, x, y) == pixel(strip, empty, x, y)) {
                    continue;
                }
                left = Math.min(left, x);
                top = Math.min(top, y);
                right = Math.max(right, x);
                bottom = Math.max(bottom, y);
            }
        }
        assertTrue(right >= left && bottom >= top,
                "a full cell and a spent one differ in no pixel at all");
        return new Window(left, top, right - left + 1, bottom - top + 1);
    }

    /**
     * Amount of rows of the window a frame carries, counted from the bottom of the window up.
     * <p>
     * The count stops at the first row that is the spent cell again, so a frame that leaves a row empty in
     * the middle of its window is reported as the rows below it - which is what the checks of the fill read.
     */
    private static int filledRowsOf(BufferedImage strip, int frames, Window window, int frame) {
        int empty = frames - 1;
        int filled = 0;
        for (int row = window.height - 1; row >= 0; row--) {
            int y = window.y + row;
            boolean poured = false;
            for (int x = window.x; x < window.x + window.width; x++) {
                poured |= pixel(strip, frame, x, y) != pixel(strip, empty, x, y);
            }
            if (!poured) {
                break;
            }
            filled++;
        }
        return filled;
    }

    /**
     * The colour of a chemistry over one pixel of the pack, the arithmetic a window of fluid is drawn with.
     * <p>
     * A pixel read out of a picture is packed as {@code ARGB}, the way {@code BufferedImage} hands it over,
     * while the colour of a chemistry is a libGDX colour with channels between zero and one. The brightness
     * of the pixel is what the colour is multiplied with, so the shading the pack drew into its glass stays
     * readable once it is filled, see {@link CellIconFactory}.
     */
    private static int pour(int grey, Color colour) {
        int brightness = Math.max(red(grey), Math.max(green(grey), blue(grey)));
        return alpha(grey) << 24 | channel(colour.r * brightness) << 16
                | channel(colour.g * brightness) << 8 | channel(colour.b * brightness);
    }

    /** Rounds a colour channel and keeps it inside the range of a byte. */
    private static int channel(float value) {
        return Math.min(Math.max(Math.round(value), 0), 0xFF);
    }

    private static int alpha(int colour) {
        return (colour >>> 24) & 0xFF;
    }

    private static int red(int colour) {
        return (colour >>> 16) & 0xFF;
    }

    private static int green(int colour) {
        return (colour >>> 8) & 0xFF;
    }

    private static int blue(int colour) {
        return colour & 0xFF;
    }
}
