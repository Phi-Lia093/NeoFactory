package com.philia093.neofactory.gui.recipe;

import com.philia093.neofactory.gui.panel.NineSlice;
import com.philia093.neofactory.render.PixelFont;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paints the panel of a recipe into a picture and holds every line of it inside the panel.
 * <p>
 * The screen of recipes is drawn with the art of NEI by a card, which no test of this project has: the panel
 * is therefore cut and laid out here with the arithmetic the screen uses and the very pictures of the pack -
 * the nine cells of the frame, the bevel of a slot, the lines of the report at
 * {@link RecipeBrowserLayout#templateLineY(int)} and the page in the column
 * {@link RecipeBrowserLayout#TEMPLATE_HINT_X} - so what a player would see can be looked at and, more to the
 * point, measured: <b>a line that would run out of the panel fails here</b> instead of on a screen.
 * <p>
 * The width of a line is counted with the cell of the font, which is the widest a line of the game can be -
 * the font advances less for most of its glyphs - so a line that fits here fits where it is drawn.
 */
class NeiPreviewTest {

    /** Picture the preview is written to. */
    private static final Path PREVIEW = Path.of("build", "reports", "nei-preview.png");

    /** Nine cells of the panel picture of NEI. */
    private static final Path BACKGROUND = Path.of("gui", "nei", "gui_background.png");

    /** Colour the report and the page are written in. */
    private static final int TEXT = 0xFFFFFFFF;

    /** Colour the name of the recipe is written in. */
    private static final int TITLE = 0xFF404040;

    /** Colour of a cell a recipe is laid out in. */
    private static final int CELL = 0xFFB0B0B0;

    @Test
    void thePanelOfARecipeIsWrittenToAPictureAndEveryLineOfItFits() throws IOException {
        BufferedImage panel = new BufferedImage(RecipeBrowserLayout.WIDTH,
                RecipeBrowserLayout.TEMPLATE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        drawFrame(panel, read(BACKGROUND));
        drawCells(panel);

        String title = "Iron Ingot";
        List<String> report = new RecipeReport().time(12.0f).steam(1500).lines();
        String page = "1/3";

        drawLine(panel, RecipeBrowserLayout.PADDING, 2, title, TITLE);
        for (int line = 0; line < report.size(); line++) {
            drawLine(panel, RecipeBrowserLayout.TEMPLATE_INFO_X,
                    RecipeBrowserLayout.templateLineY(line), report.get(line), TEXT);
        }
        drawLine(panel, RecipeBrowserLayout.TEMPLATE_HINT_X, RecipeBrowserLayout.TEMPLATE_INFO_Y, "<", TEXT);
        drawLine(panel, RecipeBrowserLayout.TEMPLATE_HINT_X + RecipeBrowserLayout.TEMPLATE_HINT_BUTTONS,
                RecipeBrowserLayout.TEMPLATE_INFO_Y, ">", TEXT);
        drawLine(panel, RecipeBrowserLayout.TEMPLATE_HINT_X,
                RecipeBrowserLayout.TEMPLATE_INFO_Y + RecipeBrowserLayout.TEMPLATE_LINE_HEIGHT, page, TEXT);

        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(panel, "png", PREVIEW.toFile());
        System.out.println("nei preview written to " + PREVIEW.toAbsolutePath() + ": report " + report
                + ", page " + page);

        assertTrue(Files.isRegularFile(PREVIEW), "the preview exists");
        assertTrue(width(title) <= RecipeBrowserLayout.WIDTH - 2 * RecipeBrowserLayout.PADDING,
                "the name of the recipe fits the head of the panel: " + width(title));
        for (String line : report) {
            assertTrue(width(line) <= RecipeBrowserLayout.TEMPLATE_INFO_WIDTH,
                    "\"" + line + "\" fits the column of the report: " + width(line) + " of "
                            + RecipeBrowserLayout.TEMPLATE_INFO_WIDTH);
        }
        assertTrue(width(page) <= RecipeBrowserLayout.TEMPLATE_HINT_WIDTH,
                "the page fits the column of the page: " + width(page));
        assertTrue(RecipeBrowserLayout.TEMPLATE_HINT_X + RecipeBrowserLayout.TEMPLATE_HINT_WIDTH
                        <= RecipeBrowserLayout.WIDTH - RecipeBrowserLayout.PADDING,
                "the column of the page lies inside the panel");
        assertTrue(RecipeBrowserLayout.templateReportFits(),
                "every line of the report lies above the lower edge of the panel");
        assertEquals(RecipeBrowserLayout.headGlyphs(),
                RecipeBrowserLayout.trimToPanel(title + " made by " + "grinding").length(),
                "a line longer than the panel is left out at the frame of it");
        assertTrue(RecipeBrowserLayout.textWidth(RecipeBrowserLayout.trimToPanel(title))
                        <= RecipeBrowserLayout.WIDTH - 2 * RecipeBrowserLayout.PADDING,
                "and what is left of it fits");
    }

    /** Reads a picture of the pack. */
    private static BufferedImage read(Path relative) throws IOException {
        Path file = TestRegistries.ASSETS.resolve(relative);
        assertTrue(Files.isRegularFile(file), "the pack carries " + relative);
        return ImageIO.read(file.toFile());
    }

    /** Lays the nine cells of the panel over the picture, the way the screen stretches them. */
    private static void drawFrame(BufferedImage panel, BufferedImage sheet) {
        int cell = sheet.getWidth() / NineSlice.COUNT;
        NineSlice slice = new NineSlice(RecipeBrowserLayout.WIDTH,
                RecipeBrowserLayout.TEMPLATE_HEIGHT, cell);
        for (int column = 0; column < NineSlice.COUNT; column++) {
            for (int row = 0; row < NineSlice.COUNT; row++) {
                int width = slice.width(column);
                int height = slice.height(row);
                if (width == 0 || height == 0) {
                    continue;
                }
                blit(sheet, panel, column * cell, row * cell, cell, cell, slice.x(column),
                        slice.y(row), width, height);
            }
        }
    }

    /** Marks the cells a recipe is laid out in and the cell of its product. */
    private static void drawCells(BufferedImage panel) {
        for (int row = 0; row < RecipeBrowserLayout.PAGE_ROWS; row++) {
            for (int column = 0; column < RecipeBrowserLayout.PAGE_COLUMNS; column++) {
                fill(panel, RecipeBrowserLayout.cellX(column) - 1, RecipeBrowserLayout.cellY(row) - 1,
                        RecipeBrowserLayout.CELL + 2, RecipeBrowserLayout.CELL + 2, CELL);
            }
        }
        fill(panel, RecipeBrowserLayout.RESULT_X - 1, RecipeBrowserLayout.RESULT_Y - 1,
                RecipeBrowserLayout.CELL + 2, RecipeBrowserLayout.CELL + 2, CELL);
    }

    /** Draws a line of text as the bar it covers, which is what says whether it fits. */
    private static void drawLine(BufferedImage panel, int left, int top, String text, int colour) {
        fill(panel, left, top, width(text), PixelFont.ASCII_CELL_SIZE, colour);
    }

    /** Width a line of text takes, counted with the widest a glyph of the font can be. */
    private static int width(String text) {
        return text.length() * PixelFont.ASCII_CELL_SIZE;
    }

    /** Fills a rectangle of the picture. */
    private static void fill(BufferedImage picture, int x, int y, int width, int height, int argb) {
        for (int row = y; row < y + height; row++) {
            for (int column = x; column < x + width; column++) {
                if (column < 0 || row < 0 || column >= picture.getWidth()
                        || row >= picture.getHeight()) {
                    continue;
                }
                picture.setRGB(column, row, argb);
            }
        }
    }

    /** Copies a rectangle of a source into a target, sampling the nearest pixel. */
    private static void blit(BufferedImage source, BufferedImage target, int sourceX, int sourceY,
            int sourceWidth, int sourceHeight, int targetX, int targetY, int targetWidth,
            int targetHeight) {
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int column = sourceX + Math.min(sourceWidth - 1, x * sourceWidth / targetWidth);
                int row = sourceY + Math.min(sourceHeight - 1, y * sourceHeight / targetHeight);
                int targetColumn = targetX + x;
                int targetRow = targetY + y;
                if (targetColumn < 0 || targetRow < 0 || targetColumn >= target.getWidth()
                        || targetRow >= target.getHeight()) {
                    continue;
                }
                int argb = source.getRGB(column, row);
                if ((argb >>> 24) < 128) {
                    continue;
                }
                target.setRGB(targetColumn, targetRow, argb);
            }
        }
    }
}
