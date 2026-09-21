package com.philia093.neofactory.gui.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.util.Constants;

/**
 * The pictures the interface panels are built from.
 * <p>
 * Everything lives in one sheet, {@code gui/inventory_icons.png}, which holds a
 * scalable panel, a slot, a tile of dirt, the flame of a furnace and a pair of
 * arrows:
 * <ul>
 *     <li>the panel is 176 by 166 pixels with a frame of {@link #BORDER} pixels, so
 *         it can be stretched to any size a container needs, see
 *         {@link #drawPanel(SpriteBatch, float, float, float, float)};</li>
 *     <li>a slot is {@link #SLOT_SIZE} pixels wide and covers its cell completely,
 *         including the bevel around the 16 pixels an item icon needs;</li>
 *     <li>the dirt tile is what a menu is tiled with, so the separate
 *         {@code gui/options_background.png} is no longer needed;</li>
 *     <li>the flame and the arrows are progress pictures. An arrow comes in two
 *         shades - {@link #arrowEmpty()} is the dark track and
 *         {@link #arrowFull()} the bright part that grows with the progress - so a
 *         single pair serves a crafting arrow and a machine gauge alike.</li>
 * </ul>
 * A missing picture never breaks a screen: the panel falls back to a plain grey
 * rectangle and the slots simply stay empty, so the game remains usable while the
 * art is being worked on.
 */
public final class PanelTextures implements ContainerAppearance {

    /** Sheet holding the panel and the gauges, without extension. */
    public static final String SHEET = BlockTextureCache.GUI_FOLDER + "inventory_icons";

    /** Thickness of the frame of the panel in pixels. */
    public static final int BORDER = 3;

    /** Width of the panel picture in pixels, also the width of a standard container. */
    public static final int PANEL_WIDTH = 176;

    /** Height of the panel picture in pixels, also the height of a standard container. */
    public static final int PANEL_HEIGHT = 166;

    /** Side length of a slot picture in pixels, the item icon sits inside it. */
    public static final int SLOT_SIZE = 18;

    /**
     * Pixels the slot picture reaches beyond the cell an item icon fills.
     * <p>
     * A slot is {@link #SLOT_SIZE} pixels wide and an icon is sixteen, so the bevel
     * around the icon is one pixel thick and two neighbouring slots share it.
     */
    public static final int SLOT_BEVEL = (SLOT_SIZE - Constants.ITEM_ICON_SIZE) / 2;

    /** Side length of the dirt tile in pixels. */
    public static final int DIRT_SIZE = 16;

    /** Side length of the flame picture in pixels. */
    public static final int FLAME_SIZE = 14;

    /** Width of an arrow in pixels. */
    public static final int ARROW_WIDTH = 23;

    /** Height of an arrow in pixels. */
    public static final int ARROW_HEIGHT = 16;

    /** Colour the panel falls back to when its picture is missing. */
    private static final Color FALLBACK_FILL = new Color(0.776f, 0.776f, 0.776f, 1.0f);

    private static final int BACKGROUND_X = 0;
    private static final int BACKGROUND_Y = 0;

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

    /** Middle of the panel, used when the picture is missing. */
    private final BlockTextureCache textures;

    /** The nine cells of the panel, ordered by column and then by row. */
    private final TextureRegion[] panel = new TextureRegion[NineSlice.COUNT * NineSlice.COUNT];

    private final TextureRegion slot;
    private final TextureRegion dirt;
    private final TextureRegion flame;
    private final TextureRegion arrowFull;
    private final TextureRegion arrowEmpty;

    /**
     * Cuts the pictures of the sheet out.
     *
     * @param textures texture cache providing {@code gui/inventory_icons.png}
     */
    public PanelTextures(BlockTextureCache textures) {
        this.textures = textures;
        TextureRegion sheet = textures.region(SHEET);
        if (sheet != null) {
            int middleWidth = PANEL_WIDTH - 2 * BORDER;
            int middleHeight = PANEL_HEIGHT - 2 * BORDER;
            int[] columnStart = {0, BORDER, PANEL_WIDTH - BORDER};
            int[] columnSize = {BORDER, middleWidth, BORDER};
            int[] rowStart = {0, BORDER, PANEL_HEIGHT - BORDER};
            int[] rowSize = {BORDER, middleHeight, BORDER};
            for (int column = 0; column < NineSlice.COUNT; column++) {
                for (int row = 0; row < NineSlice.COUNT; row++) {
                    panel[column + row * NineSlice.COUNT] = new TextureRegion(sheet,
                            BACKGROUND_X + columnStart[column], BACKGROUND_Y + rowStart[row],
                            columnSize[column], rowSize[row]);
                }
            }
            slot = new TextureRegion(sheet, SLOT_X, SLOT_Y, SLOT_SIZE, SLOT_SIZE);
            dirt = new TextureRegion(sheet, DIRT_X, DIRT_Y, DIRT_SIZE, DIRT_SIZE);
            flame = new TextureRegion(sheet, FLAME_X, FLAME_Y, FLAME_SIZE, FLAME_SIZE);
            arrowFull = new TextureRegion(sheet, ARROW_FULL_X, ARROW_FULL_Y, ARROW_WIDTH,
                    ARROW_HEIGHT);
            arrowEmpty = new TextureRegion(sheet, ARROW_EMPTY_X, ARROW_EMPTY_Y, ARROW_WIDTH,
                    ARROW_HEIGHT);
        } else {
            slot = null;
            dirt = null;
            flame = null;
            arrowFull = null;
            arrowEmpty = null;
        }
    }

    /**
     * Draws the panel stretched to any size a container asks for.
     * <p>
     * The four corners keep their pixels, the edges are stretched along one axis and
     * the middle is filled by stretching the plain centre of the picture. That is what
     * lets a single picture serve a small inventory and a tall machine screen.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param x left edge in interface pixels
     * @param y lower edge in interface pixels, the interface measures upwards
     * @param width width the panel is stretched to
     * @param height height the panel is stretched to
     */
    public void drawPanel(SpriteBatch batch, float x, float y, float width, float height) {
        if (panel[0] == null) {
            drawFallback(batch, x, y, width, height);
            return;
        }
        NineSlice slice = new NineSlice(Math.round(width), Math.round(height), BORDER);
        for (int column = 0; column < NineSlice.COUNT; column++) {
            int cellWidth = slice.width(column);
            if (cellWidth == 0) {
                continue;
            }
            for (int row = 0; row < NineSlice.COUNT; row++) {
                int cellHeight = slice.height(row);
                if (cellHeight == 0) {
                    continue;
                }
                // The grid is measured downwards from the upper edge of the picture,
                // the interface upwards from its lower edge.
                batch.draw(panel[column + row * NineSlice.COUNT],
                        x + slice.x(column), y + height - slice.y(row) - cellHeight,
                        cellWidth, cellHeight);
            }
        }
    }

    /**
     * Draws the bevel of one slot, so an empty slot still reads as a place an item
     * goes into.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param x left edge of the cell the slot belongs to
     * @param y lower edge of that cell
     */
    public void drawSlot(SpriteBatch batch, float x, float y, int column, int row) {
        if (slot != null) {
            // The picture covers the cell and its bevel, which reaches one pixel
            // beyond the sixteen pixels an item icon uses on every side.
            batch.draw(slot, x - SLOT_BEVEL, y - SLOT_BEVEL, SLOT_SIZE, SLOT_SIZE);
        }
    }

    /** Tile a menu is tiled with, {@code null} when the sheet is missing. */
    public TextureRegion dirt() {
        return dirt;
    }

    /** Flame of a furnace, {@code null} when the sheet is missing. */
    public TextureRegion flame() {
        return flame;
    }

    /**
     * The bright arrow, the part that grows with the progress.
     *
     * @return the picture, {@code null} when the sheet is missing
     */
    public TextureRegion arrowFull() {
        return arrowFull;
    }

    /**
     * The dark arrow, the track an empty progress bar shows.
     *
     * @return the picture, {@code null} when the sheet is missing
     */
    public TextureRegion arrowEmpty() {
        return arrowEmpty;
    }

    /** {@code true} when the sheet could be cut out and every element is available. */
    public boolean isComplete() {
        return panel[0] != null && slot != null && dirt != null && flame != null
                && arrowFull != null && arrowEmpty != null;
    }

    /** Draws a plain grey rectangle in place of the missing panel picture. */
    private void drawFallback(SpriteBatch batch, float x, float y, float width, float height) {
        TextureRegion pixel = textures.whitePixel();
        if (pixel == null) {
            return;
        }
        batch.setColor(FALLBACK_FILL);
        batch.draw(pixel, x, y, width, height);
        batch.setColor(Color.WHITE);
    }
}
