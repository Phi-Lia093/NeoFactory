package com.philia093.neofactory.fluid;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.render.CellIconFactory;
import com.philia093.neofactory.render.PixelFont;
import com.philia093.neofactory.support.TestIcons;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paints the fluids of the game the way the world and the inventory draw them and writes the
 * result to a picture.
 * <p>
 * The test does what the renderer does: it takes the grey scale sheet of a fluid, multiplies it
 * with {@link Fluid#color()} and lays the frames of the animation next to each other. Below them
 * stand the items that carry a fluid, drawn from their own picture in the tint of the fluid they
 * hold. The result lies in {@code core/build/reports/fluid-preview.png} and is meant to be looked
 * at.
 * <p>
 * Two more checks run on the same arithmetic: the water of the picture has to come out blue and
 * the lava orange, which is what proves the sheet carries brightness only, and the frames of a
 * sheet have to differ, or the water of a lake would stand still.
 */
class FluidPreviewTest {

    /** Assets of the project, the tests run inside the core module. */
    private static final Path ASSETS = Path.of("..", "assets");

    /** Picture the preview is written to. */
    private static final Path PREVIEW = Path.of("build", "reports", "fluid-preview.png");

    /** The sheet of the bitmap font the game draws with. */
    private static final Path FONT_SHEET = ASSETS.resolve(PixelFont.ASCII_PAGE + ".png");

    /** Zoom one cell is drawn with, so the frames of a fluid are readable. */
    private static final int ZOOM = 3;

    /** Cells of a fluid the preview shows. */
    private static final int FRAMES_SHOWN = 4;

    /** Side of one cell of the picture in pixels. */
    private static final int CELL = Constants.ITEM_ICON_SIZE * ZOOM;

    /** Space between two cells, and around the whole picture. */
    private static final int GAP = 6;
    private static final int MARGIN = 10;

    /** Height of a line of text with its shadow and the space below it. */
    private static final int LABEL_HEIGHT = 12;

    /** Dark grey the picture is filled with, the backdrop the fluids show up against. */
    private static final int BACKDROP = 0xFF202020;

    /** Colour of a line of text and of the shadow behind it. */
    private static final int LABEL = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF404040;

    /** Side of one glyph of the font sheet, glyphs per row and the space behind them. */
    private static final int FONT_GLYPH = PixelFont.ASCII_CELL_SIZE;
    private static final int FONT_COLUMNS = 16;
    private static final int FONT_SPACING = 1;

    /** Advance of a space, the cell of a space holds no pixels. */
    private static final int FONT_SPACE_ADVANCE = 5;

    /** Pixels with a lower alpha count as invisible, like the font does it. */
    private static final int ALPHA_MIN = 8;

    /** Largest value a colour channel may hold. */
    private static final int CHANNEL_MAX = 0xFF;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theFluidsOfTheGameArePaintedAsTheGameDrawsThem() throws IOException {
        BufferedImage picture = new BufferedImage(width(), height(), BufferedImage.TYPE_INT_ARGB);
        fill(picture, BACKDROP);

        int y = MARGIN;
        for (Fluid fluid : Fluids.all()) {
            drawText(picture, MARGIN, y, fluid.name() + "  " + fluid.frames() + " frames, range "
                    + fluid.range() + ", every " + fluid.tickInterval() + " ticks");
            int row = y + LABEL_HEIGHT;
            for (int shown = 0; shown < FRAMES_SHOWN; shown++) {
                int frame = shown * fluid.frames() / FRAMES_SHOWN;
                drawCell(picture, MARGIN + shown * (CELL + GAP), row, tinted(fluid, frame));
            }
            y = row + CELL + GAP;
        }

        drawText(picture, MARGIN, y, "buckets and cells");
        int row = y + LABEL_HEIGHT;
        List<Item> carriers = List.of(Items.BUCKET, Items.WATER_BUCKET, Items.LAVA_BUCKET,
                Items.FLUID_CELL, Items.WATER_CELL, Items.LAVA_CELL);
        for (int index = 0; index < carriers.size(); index++) {
            drawCell(picture, MARGIN + index * (CELL + GAP), row, iconOf(carriers.get(index)));
        }

        write(picture);

        assertTrue(Files.size(PREVIEW) > 0, "the preview was written");
    }

    @Test
    void theGreySheetBecomesBlueWaterAndOrangeLava() {
        int water = brightest(tintedSafe(Fluids.WATER, 0));
        int lava = brightest(tintedSafe(Fluids.LAVA, 0));

        assertTrue(blue(water) > red(water), "the grey sheet of water comes out blue");
        assertTrue(red(lava) > blue(lava), "and the one of lava orange");
        assertTrue(red(lava) > red(water), "lava is the warmer of the two");
    }

    @Test
    void theFramesOfASheetAreNotAllTheSame() {
        for (Fluid fluid : Fluids.all()) {
            int[] first = tintedSafe(fluid, 0);
            int[] second = tintedSafe(fluid, fluid.frames() / 2);

            assertFalse(Arrays.equals(first, second),
                    fluid + " looks the same in every frame, so its lake would stand still");
        }
    }

    /** Reads one frame of a fluid, drawn as the world draws it. */
    private static int[] tintedSafe(Fluid fluid, int frame) {
        try {
            return tinted(fluid, frame);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read the sheet of " + fluid, e);
        }
    }

    /**
     * Pixels of one frame of a fluid, drawn the way the world draws it.
     * <p>
     * The sheet holds brightness only and is multiplied with the colour of the fluid, which is
     * exactly what {@code SpriteBatch#setColor} does with a texture.
     *
     * @param fluid fluid to draw
     * @param frame cell of the sheet to draw
     * @return the pixels, row by row, packed as RGBA8888
     * @throws IOException when the sheet cannot be read
     */
    private static int[] tinted(Fluid fluid, int frame) throws IOException {
        BufferedImage sheet = read(ASSETS.resolve(BlockTextureCache.resolvePath(fluid.stillTexture())));
        int size = Constants.ITEM_ICON_SIZE;
        int[] pixels = new int[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                pixels[y * size + x] = blend(sheet.getRGB(x, y + frame * size), fluid.color());
            }
        }
        return pixels;
    }

    /** Pixels of the icon of an item, drawn in the tint of the item. */
    private static int[] iconOf(Item item) {
        int[] flat = TestIcons.icon(item);
        if (item.isFluidContainer() && item.container().paintsItsWindow()) {
            // A filled cell shows its fluid in the window and keeps its grey steel around it,
            // while a bucket is drawn by the art pack and is painted over by nothing.
            return CellIconFactory.colour(flat, Constants.ITEM_ICON_SIZE,
                    item.container().content().color());
        }
        int[] pixels = new int[flat.length];
        for (int index = 0; index < flat.length; index++) {
            pixels[index] = blend(argb(flat[index]), item.tint());
        }
        return pixels;
    }

    /**
     * Multiplies a pixel with a colour, the way the batch does it while it draws.
     *
     * @param argb pixel of a picture, as {@code BufferedImage} stores it
     * @param tint colour to multiply with
     * @return the pixel, packed as RGBA8888
     */
    private static int blend(int argb, Color tint) {
        float alpha = ((argb >>> 24) & CHANNEL_MAX) * tint.a;
        int red = channel(((argb >> 16) & CHANNEL_MAX) * tint.r);
        int green = channel(((argb >> 8) & CHANNEL_MAX) * tint.g);
        int blue = channel((argb & CHANNEL_MAX) * tint.b);
        return (red << 24) | (green << 16) | (blue << 8) | channel(alpha);
    }

    /** RGBA8888 of the game to the ARGB8888 a picture uses. */
    private static int argb(int colour) {
        return ((colour & CHANNEL_MAX) << 24) | ((colour >>> 8) & 0xFFFFFF);
    }

    /** Rounds a colour channel and keeps it inside the range of a byte. */
    private static int channel(float value) {
        return Math.min(Math.max(Math.round(value), 0), CHANNEL_MAX);
    }

    /** Width of the picture: the wider of the two rows of cells. */
    private static int width() {
        int cells = Math.max(FRAMES_SHOWN, 6);
        return MARGIN * 2 + cells * CELL + (cells - 1) * GAP;
    }

    /** Height of the picture: a labelled row per fluid, then the row of the carriers. */
    private static int height() {
        int rows = Fluids.all().size() + 1;
        return MARGIN * 2 + rows * (LABEL_HEIGHT + CELL) + (rows - 1) * GAP;
    }

    /** Brightest pixel of a cell, the one that carries the colour of the fluid. */
    private static int brightest(int[] pixels) {
        int found = 0;
        int best = -1;
        for (int colour : pixels) {
            if (((colour >>> 24) & CHANNEL_MAX) == 0) {
                continue;
            }
            int value = Math.max(red(colour), Math.max(green(colour), blue(colour)));
            if (value > best) {
                best = value;
                found = colour;
            }
        }
        return found;
    }

    /** Red share of a pixel packed as RGBA8888. */
    private static int red(int colour) {
        return (colour >>> 24) & CHANNEL_MAX;
    }

    /** Green share of a pixel packed as RGBA8888. */
    private static int green(int colour) {
        return (colour >>> 16) & CHANNEL_MAX;
    }

    /** Blue share of a pixel packed as RGBA8888. */
    private static int blue(int colour) {
        return (colour >>> 8) & CHANNEL_MAX;
    }

    /** Draws one cell of the picture, enlarged by {@link #ZOOM}. */
    private static void drawCell(BufferedImage picture, int x, int y, int[] pixels) {
        int size = Constants.ITEM_ICON_SIZE;
        for (int row = 0; row < CELL; row++) {
            for (int column = 0; column < CELL; column++) {
                int colour = pixels[(row / ZOOM) * size + column / ZOOM];
                if (((colour >>> 24) & CHANNEL_MAX) == 0) {
                    continue;
                }
                int targetX = x + column;
                int targetY = y + row;
                if (targetX < picture.getWidth() && targetY < picture.getHeight()) {
                    picture.setRGB(targetX, targetY, argb(colour));
                }
            }
        }
    }

    /** Fills a whole picture with one colour. */
    private static void fill(BufferedImage image, int colour) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, colour);
            }
        }
    }

    /** Writes one line of text with a shadow, using the glyphs of the font sheet. */
    private static void drawText(BufferedImage picture, int x, int y, String text)
            throws IOException {
        BufferedImage font = read(FONT_SHEET);
        int pen = x;
        for (int index = 0; index < text.length(); index++) {
            int cell = text.charAt(index) & 0xFF;
            int cellX = (cell % FONT_COLUMNS) * FONT_GLYPH;
            int cellY = (cell / FONT_COLUMNS) * FONT_GLYPH;
            blitGlyph(picture, font, cellX, cellY, pen + 1, y + 1, SHADOW);
            blitGlyph(picture, font, cellX, cellY, pen, y, LABEL);
            pen += advance(font, cellX, cellY) + FONT_SPACING;
        }
    }

    /** Copies one glyph of the sheet into the picture in a single colour. */
    private static void blitGlyph(BufferedImage picture, BufferedImage font, int cellX, int cellY,
            int x, int y, int colour) {
        for (int row = 0; row < FONT_GLYPH; row++) {
            for (int column = 0; column < FONT_GLYPH; column++) {
                if (((font.getRGB(cellX + column, cellY + row) >>> 24) & CHANNEL_MAX) < ALPHA_MIN) {
                    continue;
                }
                int targetX = x + column;
                int targetY = y + row;
                if (targetX >= 0 && targetY >= 0 && targetX < picture.getWidth()
                        && targetY < picture.getHeight()) {
                    picture.setRGB(targetX, targetY, colour);
                }
            }
        }
    }

    /** Pixels a glyph advances, measured by scanning it like the font does. */
    private static int advance(BufferedImage font, int cellX, int cellY) {
        int right = 0;
        for (int row = 0; row < FONT_GLYPH; row++) {
            for (int column = 0; column < FONT_GLYPH; column++) {
                if (((font.getRGB(cellX + column, cellY + row) >>> 24) & CHANNEL_MAX) > ALPHA_MIN) {
                    right = Math.max(right, column + 1);
                }
            }
        }
        return right == 0 ? FONT_SPACE_ADVANCE : right;
    }

    /** Reads a picture of the assets. */
    private static BufferedImage read(Path file) throws IOException {
        BufferedImage picture = ImageIO.read(file.toFile());
        if (picture == null) {
            throw new IllegalStateException("Unable to read " + file);
        }
        return picture;
    }

    /** Writes the preview into the report folder of the build. */
    private static void write(BufferedImage picture) throws IOException {
        Files.createDirectories(PREVIEW.getParent());
        if (!ImageIO.write(picture, "png", PREVIEW.toFile())) {
            throw new IllegalStateException("Unable to write " + PREVIEW);
        }
    }
}
