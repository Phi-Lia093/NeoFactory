package com.philia093.neofactory.support;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.gui.DurabilityBar;
import com.philia093.neofactory.gui.panel.NineSlice;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.render.PixelFont;
import com.philia093.neofactory.util.Constants;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Draws a screen of the game into a picture, so its layout can be looked at without starting the game.
 * <p>
 * The helper draws with the very pictures the game draws with - the panel of {@code gui/inventory_icons.png}
 * stretched in nine cells, the bevel of a slot, the small cube a block item is folded into, the bar of a
 * worn tool and the bitmap of the font - so a preview tells whether a screen is laid out the way it is
 * meant to be and whether its text reads on its panel. The screens that want a picture of themselves keep
 * their own test and only ask this class for the pieces, see {@code ChestPreviewTest} and
 * {@code CraftingPreviewTest}.
 */
public final class GuiPreview {

    /** Space between the picture and the panel. */
    public static final int MARGIN = 8;

    /** Colour a picture is filled with, a dark grey that shows the frame of the panel. */
    public static final int BACKDROP = 0xFF202020;

    /** The sheet holding the panel, the slots and the arrow. */
    public static final Path SHEET = TestRegistries.ASSETS.resolve(PanelTextures.SHEET + ".png");

    /** The sheet of the bitmap font the game draws with. */
    public static final Path FONT = TestRegistries.ASSETS.resolve(PixelFont.ASCII_PAGE + ".png");

    /** X coordinate of the slot bevel inside {@link #SHEET}. */
    private static final int SLOT_X = 176;

    /** Y coordinate of the slot bevel inside {@link #SHEET}. */
    private static final int SLOT_Y = 0;

    /** Side of one glyph of the font in pixels. */
    private static final int FONT_GLYPH = PixelFont.ASCII_CELL_SIZE;

    /** Amount of glyphs a row of the font sheet holds. */
    private static final int FONT_COLUMNS = 16;

    /** Pixels added behind a glyph, so two characters do not touch. */
    private static final int FONT_SPACING = 1;

    /** Advance of a space, the cell of a space holds no pixels. */
    private static final int FONT_SPACE_ADVANCE = 4;

    /** Pixels with a lower alpha count as invisible, like the font does it. */
    private static final int ALPHA_MIN = 8;

    /** Colour of the shadow behind a line of text, the dark edge that makes it read. */
    private static final int SHADOW = 0xFF404040;

    private GuiPreview() {
        // Utility class: never instantiated.
    }

    /** Reads a picture of the assets. */
    public static BufferedImage read(Path file) throws IOException {
        BufferedImage picture = ImageIO.read(file.toFile());
        if (picture == null) {
            throw new IllegalStateException("Unable to read " + file);
        }
        return picture;
    }

    /** Creates a picture of that size, filled with the backdrop of a preview. */
    public static BufferedImage newPicture(int width, int height) {
        BufferedImage picture = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        fill(picture, BACKDROP);
        return picture;
    }

    /** Stretches the nine cells of the panel picture into a target size. */
    public static void drawPanel(BufferedImage sheet, BufferedImage target, int x, int y, int width,
            int height) {
        NineSlice slice = new NineSlice(width, height, PanelTextures.BORDER);
        int[] columnStart = {0, PanelTextures.BORDER,
                PanelTextures.PANEL_WIDTH - PanelTextures.BORDER};
        int[] columnSize = {PanelTextures.BORDER,
                PanelTextures.PANEL_WIDTH - 2 * PanelTextures.BORDER, PanelTextures.BORDER};
        int[] rowStart = {0, PanelTextures.BORDER,
                PanelTextures.PANEL_HEIGHT - PanelTextures.BORDER};
        int[] rowSize = {PanelTextures.BORDER,
                PanelTextures.PANEL_HEIGHT - 2 * PanelTextures.BORDER, PanelTextures.BORDER};

        for (int column = 0; column < NineSlice.COUNT; column++) {
            for (int row = 0; row < NineSlice.COUNT; row++) {
                stretch(sheet, target, columnStart[column], rowStart[row], columnSize[column],
                        rowSize[row], x + slice.x(column), y + slice.y(row), slice.width(column),
                        slice.height(row));
            }
        }
    }

    /** Draws the bevel of one slot, the picture reaches one pixel beyond the cell. */
    public static void drawSlot(BufferedImage sheet, BufferedImage target, int x, int y) {
        stretch(sheet, target, SLOT_X, SLOT_Y, PanelTextures.SLOT_SIZE, PanelTextures.SLOT_SIZE,
                x - PanelTextures.SLOT_BEVEL, y - PanelTextures.SLOT_BEVEL, PanelTextures.SLOT_SIZE,
                PanelTextures.SLOT_SIZE);
    }

    /** Draws the arrow that points from a field to its result, empty. */
    public static void drawArrow(BufferedImage sheet, BufferedImage target, int x, int y) {
        stretch(sheet, target, 177, 53, PanelTextures.ARROW_WIDTH, PanelTextures.ARROW_HEIGHT, x, y,
                PanelTextures.ARROW_WIDTH, PanelTextures.ARROW_HEIGHT);
    }

    /** Draws the icon of a stack, a block as a small cube, everything else as it is. */
    public static void drawItem(BufferedImage target, int x, int y, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        int size = Constants.ITEM_ICON_SIZE;
        int[] icon = TestIcons.icon(stack.item());
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                int colour = icon[row * size + column];
                if ((colour & 0xFF) == 0) {
                    continue;
                }
                int targetX = x + column;
                int targetY = y + row;
                if (targetX < target.getWidth() && targetY < target.getHeight()) {
                    target.setRGB(targetX, targetY, toArgb(colour));
                }
            }
        }
    }

    /**
     * Draws the bar of a piece that wears out, with the geometry and the colours of the game.
     * <p>
     * The picture counts downwards, the way the art of the project is stored, so the row of the bar is
     * mirrored: what the interface reaches upwards from the lower edge of an icon is a row above the
     * lower edge of that icon here, see {@link DurabilityBar}.
     */
    public static void drawDurabilityBar(BufferedImage target, int x, int y, ItemStack stack) {
        if (!stack.isDamageable() || stack.damage() <= 0) {
            return;
        }
        int barX = x + DurabilityBar.OFFSET_X;
        int barY = y + Constants.ITEM_ICON_SIZE - DurabilityBar.OFFSET_Y - DurabilityBar.HEIGHT;
        fillRectangle(target, barX, barY, DurabilityBar.WIDTH, DurabilityBar.BACKGROUND_HEIGHT,
                toArgb(DurabilityBar.BACKGROUND));
        fillRectangle(target, barX, barY, DurabilityBar.widthOf(stack), DurabilityBar.HEIGHT,
                toArgb(DurabilityBar.colorOf(stack)));
    }

    /** Width a line takes in pixels of the interface. */
    public static int textWidth(BufferedImage font, String text) {
        int width = 0;
        for (char character : text.toCharArray()) {
            width += advance(font, character) + FONT_SPACING;
        }
        return width;
    }

    /**
     * Writes a line the way a screen does: the bitmap of the game, white with a dark shadow behind it.
     *
     * @param target picture to draw into
     * @param font the sheet of the font, {@code font/ascii.png}
     * @param text line to write
     * @param x left edge of the line
     * @param y upper edge of the line
     */
    public static void drawText(BufferedImage target, BufferedImage font, String text, int x,
            int y) {
        int pen = x;
        for (char character : text.toCharArray()) {
            int cell = character & 0xFF;
            int cellX = (cell % FONT_COLUMNS) * FONT_GLYPH;
            int cellY = (cell / FONT_COLUMNS) * FONT_GLYPH;
            blitGlyph(target, font, cellX, cellY, pen + 1, y + 1, SHADOW);
            blitGlyph(target, font, cellX, cellY, pen, y, 0xFFFFFFFF);
            pen += advance(font, character) + FONT_SPACING;
        }
    }

    /** Copies one glyph of the sheet into the picture in a single colour. */
    private static void blitGlyph(BufferedImage target, BufferedImage font, int cellX, int cellY,
            int x, int y, int colour) {
        for (int row = 0; row < FONT_GLYPH; row++) {
            for (int column = 0; column < FONT_GLYPH; column++) {
                if (((font.getRGB(cellX + column, cellY + row) >>> 24) & 0xFF) < ALPHA_MIN) {
                    continue;
                }
                int targetX = x + column;
                int targetY = y + row;
                if (targetX < 0 || targetY < 0 || targetX >= target.getWidth()
                        || targetY >= target.getHeight()) {
                    continue;
                }
                target.setRGB(targetX, targetY, colour);
            }
        }
    }

    /** Pixels a glyph of the sheet advances, measured by scanning it like the font does. */
    private static int advance(BufferedImage font, char character) {
        int cell = character & 0xFF;
        int cellX = (cell % FONT_COLUMNS) * FONT_GLYPH;
        int cellY = (cell / FONT_COLUMNS) * FONT_GLYPH;
        int right = 0;
        for (int row = 0; row < FONT_GLYPH; row++) {
            for (int column = 0; column < FONT_GLYPH; column++) {
                if (((font.getRGB(cellX + column, cellY + row) >>> 24) & 0xFF) > ALPHA_MIN) {
                    right = Math.max(right, column + 1);
                }
            }
        }
        return right == 0 ? FONT_SPACE_ADVANCE : right;
    }

    /** Fills a whole picture with one colour. */
    private static void fill(BufferedImage image, int argb) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, argb);
            }
        }
    }

    /** Fills a rectangle of a picture with one colour, ignoring what reaches beyond it. */
    private static void fillRectangle(BufferedImage target, int x, int y, int width, int height,
            int argb) {
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                int targetX = x + column;
                int targetY = y + row;
                if (targetX < 0 || targetY < 0 || targetX >= target.getWidth()
                        || targetY >= target.getHeight()) {
                    continue;
                }
                target.setRGB(targetX, targetY, argb);
            }
        }
    }

    /** Copies a rectangle of a source into a target, sampling the nearest pixel. */
    private static void stretch(BufferedImage source, BufferedImage target, int sourceX, int sourceY,
            int sourceWidth, int sourceHeight, int targetX, int targetY, int targetWidth,
            int targetHeight) {
        for (int y = 0; y < targetHeight; y++) {
            int row = sourceY + Math.min(sourceHeight - 1, y * sourceHeight / targetHeight);
            for (int x = 0; x < targetWidth; x++) {
                int column = sourceX + Math.min(sourceWidth - 1, x * sourceWidth / targetWidth);
                int targetColumn = targetX + x;
                int targetRow = targetY + y;
                if (targetColumn < 0 || targetRow < 0 || targetColumn >= target.getWidth()
                        || targetRow >= target.getHeight()) {
                    continue;
                }
                int colour = source.getRGB(column, row);
                if (((colour >>> 24) & 0xFF) == 0) {
                    continue;
                }
                target.setRGB(targetColumn, targetRow, colour);
            }
        }
    }

    /** ARGB of a colour of the game, rounded into the eight bit channels a picture uses. */
    private static int toArgb(Color colour) {
        int alpha = Math.round(colour.a * 255.0f);
        int red = Math.round(colour.r * 255.0f);
        int green = Math.round(colour.g * 255.0f);
        int blue = Math.round(colour.b * 255.0f);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /** RGBA8888 of the game to the ARGB8888 the picture writer expects. */
    private static int toArgb(int colour) {
        return ((colour & 0xFF) << 24) | ((colour >>> 8) & 0xFFFFFF);
    }
}
