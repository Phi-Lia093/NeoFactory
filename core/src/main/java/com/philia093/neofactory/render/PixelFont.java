package com.philia093.neofactory.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Draws text with the bitmaps of the {@code font} folder.
 * <p>
 * The art pack follows the layout of the original game: {@code font/ascii.png} is
 * a sheet of 16 by 16 cells holding the first 256 code points in cells of 8 by 8
 * pixels, and every {@code font/unicode_page_xx.png} is a sheet of 16 by 16 cells
 * of 16 by 16 pixels holding the code points {@code xx00} to {@code xxff}. A code
 * point therefore names its own picture: the page is the high byte of the code
 * point and the cell is the low byte. That covers the whole basic multilingual
 * plane, Chinese included; a character above U+FFFF is drawn one half at a time
 * and ends up as the replacement glyph.
 * <p>
 * Unicode pages are loaded the first time a character needs them, and a page that
 * does not exist is remembered, so a missing picture is reported once instead of
 * once per frame. The pictures themselves are cut out of the shared
 * {@link BlockTextureCache}, the font only measures and places them.
 * <p>
 * Glyph widths are measured by scanning the sheet, see
 * {@link #scanAdvance(GlyphPixels, int, int, int, int)}: the art pack does not
 * ship the table of advances the original game uses.
 * <p>
 * A text is drawn from a top left corner, which is the origin libGDX uses for its
 * own fonts, and the coordinates are screen pixels, so a caller has to switch the
 * batch to the projection of the user interface viewport before drawing.
 */
public class PixelFont implements Disposable {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Path of the page holding the basic Latin characters, without extension. */
    public static final String ASCII_PAGE = "font/ascii";

    /** Cell size of the ASCII page, which holds 16 by 16 cells. */
    public static final int ASCII_CELL_SIZE = 8;

    /** Cell size of every Unicode page, which holds 16 by 16 cells. */
    public static final int UNICODE_CELL_SIZE = 16;

    /** Highest code point stored by {@link #ASCII_PAGE}. */
    private static final int ASCII_PAGE_LAST = 0xFF;

    /** Path pattern of a Unicode page, the argument is the page number in hex. */
    private static final String UNICODE_PAGE_FORMAT = "font/unicode_page_%02x";

    /** Glyph drawn for a code point whose page is missing. */
    private static final char REPLACEMENT = '?';

    /** Pixels with a lower alpha count as invisible while a glyph is measured. */
    private static final int ALPHA_THRESHOLD = 8;

    /** Pixels added behind a measured glyph, so two characters do not touch. */
    private static final int GLYPH_SPACING = 1;

    /** Advance of a space, its cell in the ASCII page holds no pixels. */
    private static final int SPACE_ADVANCE = 4;

    /** Amount of spaces a tab advances. */
    private static final int TAB_COLUMNS = 4;

    /** Height of a glyph in pixels. */
    private static final int GLYPH_HEIGHT = 8;

    /** Height of a line in pixels, one pixel more than a glyph for the leading. */
    private static final int LINE_HEIGHT = GLYPH_HEIGHT + 1;

    /** Factor the text colour is darkened by for the shadow behind it. */
    private static final float SHADOW_FACTOR = 0.25f;

    private final BlockTextureCache textures;

    /** Page holding the first 256 code points, always loaded. */
    private final GlyphPage asciiPage;

    /** Unicode pages that were loaded on demand, keyed by their page number. */
    private final Map<Integer, GlyphPage> unicodePages = new HashMap<>();

    /** Page numbers that turned out not to exist, so they are not looked up twice. */
    private final Set<Integer> missingPages = new HashSet<>();

    /** Colour the text is drawn with. */
    private final Color color = new Color(Color.WHITE);

    /** Scratch colour of the shadow pass, reused to avoid garbage while drawing. */
    private final Color shadowColor = new Color();

    private float scale = 1.0f;

    /**
     * Creates a font.
     *
     * @param textures shared texture cache the glyph pictures are cut from
     */
    public PixelFont(BlockTextureCache textures) {
        this.textures = textures;
        this.asciiPage = new GlyphPage(textures, ASCII_PAGE, ASCII_CELL_SIZE);
    }

    /**
     * Sets the size of the text.
     * <p>
     * Whole numbers keep the pixels of the bitmaps crisp, a fractional scale
     * blurs them.
     *
     * @param scale factor applied to glyphs and advances, must be positive
     */
    public void setScale(float scale) {
        if (scale <= 0.0f) {
            throw new IllegalArgumentException("Font scale must be positive: " + scale);
        }
        this.scale = scale;
    }

    /** Current size of the text. */
    public float scale() {
        return scale;
    }

    /** Live colour of the text, do not mutate directly. */
    public Color color() {
        return color;
    }

    /**
     * Sets the colour of the text.
     *
     * @param color colour to copy, alpha included
     */
    public void setColor(Color color) {
        this.color.set(color);
    }

    /**
     * Height of a glyph in pixels.
     * <p>
     * A line is one pixel higher than a glyph, see {@link #lineHeight()}. A caller
     * that centres text on a button or a row needs the height of the glyph itself,
     * not of the line.
     */
    public float glyphHeight() {
        return GLYPH_HEIGHT * scale;
    }

    /** Height of a line in pixels, useful to stack several lines of text. */
    public float lineHeight() {
        return LINE_HEIGHT * scale;
    }

    /**
     * Measures the width of a text.
     * <p>
     * Line breaks are taken into account by returning the width of the widest
     * line, which is what a caller needs to centre a text or to size a tooltip.
     *
     * @param text text to measure, may be {@code null}
     * @return the width in pixels
     */
    public float width(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0f;
        }
        float widest = 0.0f;
        float current = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '\n') {
                widest = Math.max(widest, current);
                current = 0.0f;
                continue;
            }
            if (character == '\r') {
                continue;
            }
            current += advanceOf(character) * scale;
        }
        return Math.max(widest, current);
    }

    /**
     * Returns the left edge that centres a text around a point.
     *
     * @param text text to place
     * @param centerX point the text is centred on
     * @return the X coordinate to draw the text at
     */
    public float centeredX(String text, float centerX) {
        return centerX - width(text) * 0.5f;
    }

    /** Amount of loaded pages, the ASCII page included, used for logging. */
    public int loadedPageCount() {
        return 1 + unicodePages.size();
    }

    /**
     * Draws a text with the current colour.
     *
     * @param batch batch to draw into, switched to the interface projection
     * @param text text to draw, may be {@code null}
     * @param x left edge of the first glyph
     * @param y top edge of the first line
     */
    public void draw(SpriteBatch batch, String text, float x, float y) {
        drawPass(batch, text, x, y, color);
    }

    /**
     * Draws a text with a shadow behind it.
     * <p>
     * The shadow is the same text, moved one pixel to the right and one pixel down
     * and darkened, which is what keeps a name readable on top of a bright
     * background. The interface measures Y upwards, so the shadow is drawn at a
     * lower coordinate than the text.
     *
     * @param batch batch to draw into, switched to the interface projection
     * @param text text to draw, may be {@code null}
     * @param x left edge of the first glyph
     * @param y top edge of the first line
     */
    public void drawShadowed(SpriteBatch batch, String text, float x, float y) {
        shadowColor.set(color.r * SHADOW_FACTOR, color.g * SHADOW_FACTOR,
                color.b * SHADOW_FACTOR, color.a);
        drawPass(batch, text, x + scale, y - scale, shadowColor);
        drawPass(batch, text, x, y, color);
    }

    /**
     * Draws every glyph of a text in a single colour.
     *
     * @param batch batch to draw into
     * @param text text to draw
     * @param x left edge of the first glyph
     * @param y top edge of the first line
     * @param passColor colour of this pass
     */
    private void drawPass(SpriteBatch batch, String text, float x, float y, Color passColor) {
        if (text == null || text.isEmpty()) {
            return;
        }
        batch.setColor(passColor);
        float cursorX = x;
        float lineTop = y;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '\n') {
                cursorX = x;
                lineTop -= lineHeight();
                continue;
            }
            if (character == '\r') {
                continue;
            }
            if (!isBlank(character)) {
                TextureRegion region = regionOf(character);
                if (region != null) {
                    // A region is as wide as the cell it was cut from, so the size
                    // is read from the picture instead of being a constant: the
                    // ASCII page holds 8 pixel cells, a Unicode page 16 pixel ones.
                    float size = region.getRegionWidth() * scale;
                    batch.draw(region, cursorX, lineTop - size, size, size);
                }
            }
            cursorX += advanceOf(character) * scale;
        }
    }

    /**
     * Resolves the picture of a character.
     *
     * @param character character to draw
     * @return the glyph, or the replacement glyph when the character has no picture
     */
    private TextureRegion regionOf(char character) {
        GlyphPage page = pageFor(character);
        TextureRegion region = page == null ? null : page.region(indexOf(character));
        return region == null ? asciiPage.region(indexOf(REPLACEMENT)) : region;
    }

    /**
     * Returns the horizontal space a character takes.
     *
     * @param character character to measure
     * @return the advance in pixels
     */
    private int advanceOf(char character) {
        if (isBlank(character)) {
            return character == '\t' ? SPACE_ADVANCE * TAB_COLUMNS : SPACE_ADVANCE;
        }
        GlyphPage page = pageFor(character);
        int advance = page == null ? 0 : page.advance(indexOf(character));
        if (advance > GLYPH_SPACING) {
            return advance;
        }
        // Unused cells and characters of a missing page fall back to '?'.
        return asciiPage.advance(indexOf(REPLACEMENT));
    }

    /** {@code true} for the characters that hold no pixels of their own. */
    private static boolean isBlank(char character) {
        return character == ' ' || character == '\t' || character == '\u00A0';
    }

    /**
     * Returns the page holding a character, loading it when it is used for the
     * first time.
     *
     * @param character character to look up
     * @return the page, or {@code null} when the picture of that page is missing
     */
    private GlyphPage pageFor(char character) {
        if (character <= ASCII_PAGE_LAST) {
            return asciiPage;
        }
        int pageNumber = character >>> 8;
        GlyphPage page = unicodePages.get(pageNumber);
        if (page != null) {
            return page;
        }
        if (missingPages.contains(pageNumber)) {
            return null;
        }
        String name = String.format(UNICODE_PAGE_FORMAT, pageNumber);
        page = new GlyphPage(textures, name, UNICODE_CELL_SIZE);
        if (!page.isLoaded()) {
            // Remember the gap, otherwise every frame would try to load it again.
            missingPages.add(pageNumber);
            LOGGER.warn("No font page for character '{}' (U+{}), drawing '{}' instead",
                    character, String.format("%04X", (int) character), REPLACEMENT);
            return null;
        }
        unicodePages.put(pageNumber, page);
        LOGGER.debug("Loaded font page {}, {} pages in use", name, loadedPageCount());
        return page;
    }

    /**
     * Returns the cell of a character inside its page.
     * <p>
     * A page holds exactly 256 cells, so the low byte of the code point is the
     * cell index, both for the ASCII page and for a Unicode page.
     *
     * @param character character to place
     * @return the cell index, {@code 0} to {@code 255}
     */
    private static int indexOf(char character) {
        return character & 0xFF;
    }

    /**
     * Scans how wide a glyph is.
     * <p>
     * The art pack does not ship the table of glyph widths the original game
     * uses, so the width is measured instead: the scan walks the columns of a cell
     * from the right and stops at the first column that holds a visible pixel. One
     * pixel of spacing is added on top, which is what keeps two neighbouring
     * characters apart, and the result never exceeds the cell, so the full width
     * pictures of the Unicode pages stay untouched.
     *
     * @param pixels pixels of the sheet
     * @param cellX left edge of the cell
     * @param cellY top edge of the cell
     * @param cellSize width and height of the cell
     * @param spacing pixels added behind the drawn glyph
     * @return the advance in pixels, {@code spacing} when the cell is empty
     */
    public static int scanAdvance(GlyphPixels pixels, int cellX, int cellY, int cellSize, int spacing) {
        for (int column = cellSize - 1; column >= 0; column--) {
            for (int row = 0; row < cellSize; row++) {
                if (pixels.alphaAt(cellX + column, cellY + row) > ALPHA_THRESHOLD) {
                    return Math.min(column + 1 + spacing, cellSize);
                }
            }
        }
        return spacing;
    }

    /**
     * Access to the pixels of a glyph sheet.
     * <p>
     * The interface keeps the measurement independent of libGDX, so the widths of
     * a sheet can be checked against the real picture without a graphics card.
     */
    @FunctionalInterface
    public interface GlyphPixels {

        /**
         * Reads the alpha value of one pixel.
         *
         * @param x column of the sheet, counted from the left
         * @param y row of the sheet, counted from the top
         * @return the alpha between {@code 0} and {@code 255}
         */
        int alphaAt(int x, int y);
    }

    /**
     * A single sheet of glyphs together with the advance of every cell.
     * <p>
     * The widths are measured once, while the page is loaded, and the pictures are
     * cut out of the sheet the first time a cell is used. A page that does not
     * exist is reported as not loaded instead of throwing, so a caller can fall
     * back to another glyph.
     */
    private static final class GlyphPage {

        /** Amount of cells a page holds, a page covers one block of 256 code points. */
        private static final int CELL_COUNT = 256;

        private final BlockTextureCache textures;
        private final String name;
        private final int cellSize;
        private final int[] advances = new int[CELL_COUNT];
        private final TextureRegion[] regions = new TextureRegion[CELL_COUNT];
        private final boolean loaded;

        private GlyphPage(BlockTextureCache textures, String name, int cellSize) {
            this.textures = textures;
            this.name = name;
            this.cellSize = cellSize;
            this.loaded = scan(name, cellSize, advances);
        }

        /** {@code true} when the picture of this page exists. */
        private boolean isLoaded() {
            return loaded;
        }

        /**
         * Returns the advance of a cell.
         *
         * @param index cell index
         * @return the advance in pixels, {@code 0} when the cell lies outside the sheet
         */
        private int advance(int index) {
            return index >= 0 && index < advances.length ? advances[index] : 0;
        }

        /**
         * Returns the picture of a cell, cutting it out on first use.
         *
         * @param index cell index
         * @return the glyph, or {@code null} when the cell holds no pixels
         */
        private TextureRegion region(int index) {
            if (index < 0 || index >= advances.length || advances[index] <= GLYPH_SPACING) {
                return null;
            }
            if (regions[index] == null) {
                regions[index] = textures.sheetCell(name, cellSize, index);
            }
            return regions[index];
        }

        /**
         * Measures every cell of a sheet.
         *
         * @param name texture name of the page, without extension
         * @param cellSize width and height of a cell in pixels
         * @param advances array the measured advances are written into
         * @return {@code true} when the sheet was read, {@code false} when it does
         *         not exist
         */
        private static boolean scan(String name, int cellSize, int[] advances) {
            String path = BlockTextureCache.resolvePath(name);
            try {
                Pixmap pixmap = new Pixmap(Gdx.files.internal(path));
                try {
                    int columns = Math.max(1, pixmap.getWidth() / cellSize);
                    for (int index = 0; index < advances.length; index++) {
                        int cellX = index % columns * cellSize;
                        int cellY = index / columns * cellSize;
                        if (cellY + cellSize > pixmap.getHeight()) {
                            advances[index] = 0;
                            continue;
                        }
                        GlyphPixels pixels = (x, y) -> pixmap.getPixel(x, y) & 0xFF;
                        advances[index] = scanAdvance(pixels, cellX, cellY, cellSize, GLYPH_SPACING);
                    }
                } finally {
                    pixmap.dispose();
                }
                return true;
            } catch (RuntimeException e) {
                LOGGER.debug("Font page '{}' is not available: {}", name, e.getMessage());
                return false;
            }
        }
    }

    @Override
    public void dispose() {
        // The glyph pictures belong to the shared texture cache, so nothing has to
        // be released here. The page caches are dropped to let them be collected.
        unicodePages.clear();
        missingPages.clear();
    }
}




