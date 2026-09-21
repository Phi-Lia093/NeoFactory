package com.philia093.neofactory.gui.panel;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the pictures of {@link PanelTextures} and writes a preview of them.
 * <p>
 * The panel is cut out of the sheet in nine cells, which is exactly what can be
 * checked without a window: the cells of the original size have to reproduce the
 * sheet pixel by pixel, a stretched panel has to keep its frame and fill its middle,
 * and a tiny panel has to stay inside its own edges.
 * <p>
 * The same code paints {@code core/build/reports/panel-preview.png}, so the look of
 * the panel, the slots, the dirt, the flame and the arrows can be reviewed without
 * starting the game.
 */
class PanelPreviewTest {

    /** Assets of the project, the tests run inside the core module. */
    private static final Path ASSETS = Path.of("..", "assets");

    /** The sheet the panel is cut from. */
    private static final Path SHEET = ASSETS.resolve(PanelTextures.SHEET + ".png");

    /** Picture the preview is written to. */
    private static final Path PREVIEW = Path.of("build", "reports", "panel-preview.png");

    /** Space between two elements of the preview and around it. */
    private static final int GAP = 6;

    /** Colour the preview is filled with, a dark grey that shows the frame. */
    private static final int BACKDROP = 0xFF202020;

    /** Zoom the second half of the preview is drawn with. */
    private static final int ZOOM = 2;

    // Where the elements sit inside the sheet, read from its pixels. The preview
    // uses the same numbers the game uses, see PanelTextures.
    private static final int SLOT_X = 176;
    private static final int SLOT_Y = 0;
    private static final int DIRT_X = 177;
    private static final int DIRT_Y = 19;
    private static final int FLAME_X = 196;
    private static final int FLAME_Y = 20;
    private static final int ARROW_FULL_X = 177;
    private static final int ARROW_FULL_Y = 36;
    private static final int ARROW_EMPTY_X = 177;
    private static final int ARROW_EMPTY_Y = 53;

    @Test
    void theOriginalPanelIsCopiedPixelByPixel() throws IOException {
        BufferedImage sheet = readSheet();
        BufferedImage panel = panel(sheet, PanelTextures.PANEL_WIDTH, PanelTextures.PANEL_HEIGHT);

        for (int y = 0; y < PanelTextures.PANEL_HEIGHT; y++) {
            for (int x = 0; x < PanelTextures.PANEL_WIDTH; x++) {
                assertEquals(sheet.getRGB(x, y), panel.getRGB(x, y), "pixel " + x + ", " + y);
            }
        }
    }

    @Test
    void aStretchedPanelKeepsItsFrameAndFillsItsMiddle() throws IOException {
        BufferedImage sheet = readSheet();
        int width = PanelTextures.PANEL_WIDTH + 40;
        int height = PanelTextures.PANEL_HEIGHT - 60;
        BufferedImage panel = panel(sheet, width, height);

        // The four corners are copied one to one and never stretched. The picture cuts
        // its corners at an angle, so a transparent pixel inside a corner belongs to
        // the design and is expected here.
        for (int offsetY = 0; offsetY < PanelTextures.BORDER; offsetY++) {
            for (int offsetX = 0; offsetX < PanelTextures.BORDER; offsetX++) {
                String at = " at " + offsetX + ", " + offsetY;
                int right = PanelTextures.PANEL_WIDTH - PanelTextures.BORDER + offsetX;
                int bottom = PanelTextures.PANEL_HEIGHT - PanelTextures.BORDER + offsetY;
                assertEquals(sheet.getRGB(offsetX, offsetY), panel.getRGB(offsetX, offsetY),
                        "the upper left corner" + at);
                assertEquals(sheet.getRGB(right, offsetY), panel.getRGB(width - PanelTextures.BORDER
                        + offsetX, offsetY), "the upper right corner" + at);
                assertEquals(sheet.getRGB(offsetX, bottom), panel.getRGB(offsetX,
                        height - PanelTextures.BORDER + offsetY), "the lower left corner" + at);
                assertEquals(sheet.getRGB(right, bottom), panel.getRGB(width - PanelTextures.BORDER
                        + offsetX, height - PanelTextures.BORDER + offsetY),
                        "the lower right corner" + at);
            }
        }

        // The middle is the plain fill of the picture, wherever it is stretched to.
        int fill = sheet.getRGB(PanelTextures.PANEL_WIDTH / 2, PanelTextures.PANEL_HEIGHT / 2);
        assertEquals(fill, panel.getRGB(width / 2, height / 2), "the middle is filled");
        assertEquals(fill, panel.getRGB(width - PanelTextures.BORDER - 1,
                height - PanelTextures.BORDER - 1), "the fill reaches the frame");
    }

    @Test
    void aTinyPanelStaysInsideItsOwnEdges() throws IOException {
        BufferedImage sheet = readSheet();
        int width = 5;
        int height = 4;
        BufferedImage panel = panel(sheet, width, height);

        // The frame shrinks to half of the target, so nothing is drawn outside of it:
        // every pixel of a tiny panel still comes from the picture itself.
        Set<Integer> colours = new HashSet<>();
        for (int y = 0; y < PanelTextures.PANEL_HEIGHT; y++) {
            for (int x = 0; x < PanelTextures.PANEL_WIDTH; x++) {
                colours.add(sheet.getRGB(x, y));
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                assertTrue(colours.contains(panel.getRGB(x, y)),
                        "pixel " + x + ", " + y + " comes from the picture");
            }
        }
    }

    @Test
    void theElementsOfTheSheetCanBeCutOut() throws IOException {
        BufferedImage sheet = readSheet();

        assertElement(sheet, SLOT_X, SLOT_Y, PanelTextures.SLOT_SIZE, PanelTextures.SLOT_SIZE,
                "slot");
        assertElement(sheet, DIRT_X, DIRT_Y, PanelTextures.DIRT_SIZE, PanelTextures.DIRT_SIZE,
                "dirt");
        assertElement(sheet, FLAME_X, FLAME_Y, PanelTextures.FLAME_SIZE, PanelTextures.FLAME_SIZE,
                "flame");
        assertElement(sheet, ARROW_FULL_X, ARROW_FULL_Y, PanelTextures.ARROW_WIDTH,
                PanelTextures.ARROW_HEIGHT, "bright arrow");
        assertElement(sheet, ARROW_EMPTY_X, ARROW_EMPTY_Y, PanelTextures.ARROW_WIDTH,
                PanelTextures.ARROW_HEIGHT, "dark arrow");
    }

    @Test
    void thePreviewIsWritten() throws IOException {
        BufferedImage sheet = readSheet();
        BufferedImage row = row(sheet);
        BufferedImage preview = new BufferedImage(GAP + row.getWidth() + GAP,
                GAP + row.getHeight() * (ZOOM + 1) + GAP, BufferedImage.TYPE_INT_ARGB);
        fill(preview, BACKDROP);

        blit(row, preview, GAP, GAP, 1);
        blit(row, preview, GAP, GAP + row.getHeight() + GAP, ZOOM);

        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(preview, "png", PREVIEW.toFile());
        System.out.println("panel preview written to " + PREVIEW.toAbsolutePath()
                + ": the upper row shows the panel and its elements one to one, the lower one"
                + " enlarged " + ZOOM + " times");

        assertTrue(Files.isRegularFile(PREVIEW), "the preview exists");
    }

    /**
     * Paints the panel in four sizes next to the elements of the sheet.
     *
     * @param sheet the sheet the pictures are cut from
     * @return a picture of one row, in the pixels of the interface
     */
    private static BufferedImage row(BufferedImage sheet) {
        int height = PanelTextures.PANEL_HEIGHT;
        int width = 4 * (PanelTextures.PANEL_WIDTH + GAP) + 2 * PanelTextures.SLOT_SIZE + GAP
                + 32 + GAP + 2 * PanelTextures.FLAME_SIZE + GAP
                + 3 * (PanelTextures.ARROW_WIDTH + GAP) + GAP;
        BufferedImage row = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int x = 0;

        // The panel, as it is stored and then stretched to three other sizes.
        x = drawCell(row, sheet, PanelTextures.PANEL_WIDTH, PanelTextures.PANEL_HEIGHT,
                PanelTextures.PANEL_WIDTH, PanelTextures.PANEL_HEIGHT, x);
        x = drawCell(row, sheet, PanelTextures.PANEL_WIDTH, PanelTextures.PANEL_HEIGHT,
                200, 120, x);
        x = drawCell(row, sheet, PanelTextures.PANEL_WIDTH, PanelTextures.PANEL_HEIGHT,
                120, 200, x);
        x = drawCell(row, sheet, PanelTextures.PANEL_WIDTH, PanelTextures.PANEL_HEIGHT,
                60, 40, x);

        // The elements: a slot, the dirt in a small patch, the flame and the arrows.
        x = drawCell(row, sheet, PanelTextures.SLOT_SIZE, PanelTextures.SLOT_SIZE,
                2 * PanelTextures.SLOT_SIZE, 2 * PanelTextures.SLOT_SIZE, x);
        x = drawCell(row, sheet, PanelTextures.DIRT_SIZE, PanelTextures.DIRT_SIZE, 32, 32, x);
        x = drawCell(row, sheet, PanelTextures.FLAME_SIZE, PanelTextures.FLAME_SIZE,
                2 * PanelTextures.FLAME_SIZE, 2 * PanelTextures.FLAME_SIZE, x);
        drawArrow(sheet, row, x, 0.0f);
        x += PanelTextures.ARROW_WIDTH + GAP;
        drawArrow(sheet, row, x, 1.0f);
        x += PanelTextures.ARROW_WIDTH + GAP;
        drawArrow(sheet, row, x, 0.5f);
        return row;
    }

    /**
     * Draws one element of the sheet and returns the left edge of the next one.
     * <p>
     * The element is recognised by its size, which is unique in the sheet: the panel
     * is the only picture of 176 by 166 pixels, the slot the only one of 18, the dirt
     * the only one of 16 and the flame the only one of 14.
     *
     * @param target picture the element goes into
     * @param sheet sheet the element is cut from
     * @param sourceWidth width of the element in the sheet
     * @param sourceHeight height of the element in the sheet
     * @param width width the element is drawn with
     * @param height height the element is drawn with
     * @param x left edge the element is drawn at
     * @return the left edge of the following element
     */
    private static int drawCell(BufferedImage target, BufferedImage sheet, int sourceWidth,
            int sourceHeight, int width, int height, int x) {
        int sourceX;
        int sourceY;
        if (sourceWidth == PanelTextures.PANEL_WIDTH && sourceHeight == PanelTextures.PANEL_HEIGHT) {
            // The panel is the only element that is stretched in nine cells.
            blitAt(panel(sheet, width, height), target, x, 0);
            return x + width + GAP;
        } else if (sourceWidth == PanelTextures.SLOT_SIZE) {
            sourceX = SLOT_X;
            sourceY = SLOT_Y;
        } else if (sourceWidth == PanelTextures.DIRT_SIZE) {
            sourceX = DIRT_X;
            sourceY = DIRT_Y;
        } else {
            sourceX = FLAME_X;
            sourceY = FLAME_Y;
        }
        stretch(sheet, target, sourceX, sourceY, sourceWidth, sourceHeight, x, 0, width, height);
        return x + width + GAP;
    }

    /** Draws the arrows at a progress between zero and one, the way a gauge does. */
    private static void drawArrow(BufferedImage sheet, BufferedImage target, int x, float progress) {
        int y = PanelTextures.PANEL_HEIGHT - PanelTextures.ARROW_HEIGHT;
        stretch(sheet, target, ARROW_EMPTY_X, ARROW_EMPTY_Y, PanelTextures.ARROW_WIDTH,
                PanelTextures.ARROW_HEIGHT, x, y, PanelTextures.ARROW_WIDTH,
                PanelTextures.ARROW_HEIGHT);
        int part = Math.round(progress * PanelTextures.ARROW_WIDTH);
        if (part > 0) {
            stretch(sheet, target, ARROW_FULL_X, ARROW_FULL_Y, part, PanelTextures.ARROW_HEIGHT,
                    x, y, part, PanelTextures.ARROW_HEIGHT);
        }
    }

    /**
     * Cuts the nine cells of the panel picture and stretches them into a target size.
     *
     * @param sheet the sheet the panel is cut from
     * @param width width the panel is drawn with
     * @param height height the panel is drawn with
     * @return the panel
     */
    private static BufferedImage panel(BufferedImage sheet, int width, int height) {
        BufferedImage panel = new BufferedImage(Math.max(1, width), Math.max(1, height),
                BufferedImage.TYPE_INT_ARGB);
        NineSlice slice = new NineSlice(width, height, PanelTextures.BORDER);
        int[] cellStart = {0, PanelTextures.BORDER,
                PanelTextures.PANEL_WIDTH - PanelTextures.BORDER};
        int[] cellSize = {PanelTextures.BORDER,
                PanelTextures.PANEL_WIDTH - 2 * PanelTextures.BORDER, PanelTextures.BORDER};
        int[] rowStart = {0, PanelTextures.BORDER,
                PanelTextures.PANEL_HEIGHT - PanelTextures.BORDER};
        int[] rowSize = {PanelTextures.BORDER,
                PanelTextures.PANEL_HEIGHT - 2 * PanelTextures.BORDER, PanelTextures.BORDER};

        for (int column = 0; column < NineSlice.COUNT; column++) {
            for (int row = 0; row < NineSlice.COUNT; row++) {
                stretch(sheet, panel, cellStart[column], rowStart[row], cellSize[column],
                        rowSize[row], slice.x(column), slice.y(row), slice.width(column),
                        slice.height(row));
            }
        }
        return panel;
    }

    /** Copies a rectangle of a source into a target, sampling the nearest pixel. */
    private static void stretch(BufferedImage source, BufferedImage target, int sourceX,
            int sourceY, int sourceWidth, int sourceHeight, int targetX, int targetY,
            int targetWidth, int targetHeight) {
        if (targetWidth <= 0 || targetHeight <= 0) {
            return;
        }
        for (int y = 0; y < targetHeight; y++) {
            int row = sourceY + Math.min(sourceHeight - 1, y * sourceHeight / targetHeight);
            for (int x = 0; x < targetWidth; x++) {
                int column = sourceX + Math.min(sourceWidth - 1, x * sourceWidth / targetWidth);
                target.setRGB(targetX + x, targetY + y, source.getRGB(column, row));
            }
        }
    }

    /** Copies a whole picture into a target without scaling it. */
    private static void blitAt(BufferedImage source, BufferedImage target, int x, int y) {
        for (int row = 0; row < source.getHeight(); row++) {
            for (int column = 0; column < source.getWidth(); column++) {
                if (x + column < target.getWidth() && y + row < target.getHeight()) {
                    target.setRGB(x + column, y + row, source.getRGB(column, row));
                }
            }
        }
    }

    /** Copies a picture into a target, enlarged by a whole number. */
    private static void blit(BufferedImage source, BufferedImage target, int x, int y, int scale) {
        for (int row = 0; row < source.getHeight(); row++) {
            for (int column = 0; column < source.getWidth(); column++) {
                int colour = source.getRGB(column, row);
                for (int dy = 0; dy < scale; dy++) {
                    for (int dx = 0; dx < scale; dx++) {
                        int targetX = x + column * scale + dx;
                        int targetY = y + row * scale + dy;
                        if (targetX < target.getWidth() && targetY < target.getHeight()) {
                            target.setRGB(targetX, targetY, colour);
                        }
                    }
                }
            }
        }
    }

    /** Fills a whole picture with one colour. */
    private static void fill(BufferedImage image, int argb) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, argb);
            }
        }
    }

    /** Checks that a rectangle of the sheet exists and holds visible pixels. */
    private static void assertElement(BufferedImage sheet, int x, int y, int width, int height,
            String name) {
        assertTrue(x + width <= sheet.getWidth() && y + height <= sheet.getHeight(),
                name + " fits into the sheet");
        int visible = 0;
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                if (((sheet.getRGB(x + column, y + row) >>> 24) & 0xFF) != 0) {
                    visible++;
                }
            }
        }
        assertTrue(visible > width * height / 4, name + " holds visible pixels: " + visible);
    }

    /** Reads the sheet the panel is cut from. */
    private static BufferedImage readSheet() throws IOException {
        BufferedImage sheet = ImageIO.read(SHEET.toFile());
        assertNotNull(sheet, "the sheet " + SHEET + " can be read");
        return sheet;
    }
}
