package com.philia093.neofactory.gui.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.machine.MachineError;
import com.philia093.neofactory.machine.MachineStyle;
import com.philia093.neofactory.machine.ProgressKind;
import com.philia093.neofactory.machine.SlotKind;
import com.philia093.neofactory.render.BlockTextureCache;

/**
 * The pictures a machine screen is built from.
 * <p>
 * They live in {@code gui/machine_icons.png}, and the sheet carries three parts:
 * <ul>
 *     <li>the panels of a machine, {@link #WIDTH} by {@link #HEIGHT} pixels each with a frame
 *         of {@link #BORDER} pixels. The lower half holds the slots of the player
 *         inventory, the upper half is empty - that is where a machine stands the slots
 *         it works with, its gauges and its text. The sheet carries two of them, one per
 *         {@link MachineStyle}: the light grey panel at the top and the bronze one below it,
 *         and a machine is drawn in the one its screen names;</li>
 *     <li>a grid of icons to the right of the panels, {@link #ICON_COLUMNS} by {@link #ICON_ROWS}
 *         cells of {@link #ICON_CELL} pixels, read through {@link #icon(int, int)}. A screen uses
 *         them to describe what a slot is for or what state a machine is in, see
 *         {@link #SLOT_COLUMN}, {@link SlotKind} and {@link ProgressKind};</li>
 *     <li>one heavy track beyond the grid, the picture a bar that stands on its side is drawn from,
 *         see {@link #heavyTrack()}.</li>
 * </ul>
 * The panel is drawn the way every other panel is drawn, in the nine cells of
 * {@link NineSlice}, so it stretches to whatever size a layout asks for. The icons and
 * the plain slot of the style are cut out as they are: a slot that names no picture of its own is drawn with
 * the slot of the very panel the machine stands in, which is what turns the same screen into a grey one and a
 * bronze one, see {@link MachineStyle}.
 * <p>
 * A missing picture never breaks a screen: the panel falls back to a plain grey
 * rectangle and the slots stay empty, exactly like {@link PanelTextures} does it.
 */
public final class MachineTextures implements ContainerAppearance {

    /** Sheet holding the panel of a machine and its icons, without extension. */
    public static final String SHEET = BlockTextureCache.GUI_FOLDER + "machine_icons";

    /** Thickness of the frame of the panel in pixels. */
    public static final int BORDER = PanelTextures.BORDER;

    /** Width of the panel in pixels, the width a machine screen is designed for. */
    public static final int WIDTH = PanelTextures.PANEL_WIDTH;

    /** Height of the panel in pixels, the height a machine screen is designed for. */
    public static final int HEIGHT = PanelTextures.PANEL_HEIGHT;

    /** Side length of one cell of the icon grid in pixels. */
    public static final int ICON_CELL = 18;

    /** Amount of columns of the icon grid. */
    public static final int ICON_COLUMNS = 9;

    /** Amount of rows of the icon grid. */
    public static final int ICON_ROWS = 6;

    /** Column of the icon that is a slot, the bevel a machine slot is drawn with. */
    public static final int SLOT_COLUMN = 0;

    /** Row of the icon that is a slot. */
    public static final int SLOT_ROW = 0;

    /** Left edge of the icon grid inside the sheet. */
    private static final int GRID_X = 176;

    /** Upper edge of the icon grid inside the sheet. */
    private static final int GRID_Y = 0;

    /**
     * Left edge of the heavy track of the sheet, the one that belongs to a bar that stands on its side.
     * <p>
     * It stands in the tenth column of the sheet, one cell beyond the grid, because it is wider and taller
     * than a cell of the icons: the track of the bar of a forge hammer, see {@link ProgressKind#BRONZE_HAMMER}
     * and {@link #heavyTrack()}.
     */
    private static final int HEAVY_TRACK_X = GRID_X + ICON_COLUMNS * ICON_CELL;

    /** Upper edge of the heavy track of the sheet. */
    private static final int HEAVY_TRACK_Y = GRID_Y;

    /** Width of the heavy track of the sheet in pixels. */
    private static final int HEAVY_TRACK_WIDTH = 20;

    /** Height of the heavy track of the sheet in pixels. */
    private static final int HEAVY_TRACK_HEIGHT = 24;

    /**
     * Left edge of an icon inside the sheet.
     *
     * @param column column of the cell, counted from the left
     * @return the coordinate in pixels
     */
    public static int iconX(int column) {
        return GRID_X + column * ICON_CELL;
    }

    /**
     * Upper edge of an icon inside the sheet.
     *
     * @param row row of the cell, counted from the top
     * @return the coordinate in pixels
     */
    public static int iconY(int row) {
        return GRID_Y + row * ICON_CELL;
    }

    /** Colour the panel falls back to when its picture is missing. */
    private static final Color FALLBACK_FILL = new Color(0.776f, 0.776f, 0.776f, 1.0f);

    /** Style this set of pictures draws a machine in. */
    private final MachineStyle style;

    private final BlockTextureCache textures;

    /** The nine cells of the panel, ordered by column and then by row. */
    private final TextureRegion[] panel = new TextureRegion[NineSlice.COUNT * NineSlice.COUNT];

    /** Icons of the grid, keyed by {@code column + row * ICON_COLUMNS}. */
    private final TextureRegion[] icons = new TextureRegion[ICON_COLUMNS * ICON_ROWS];

    /** The plain slot of the style, the bevel a machine slot without a picture of its own is drawn with. */
    private final TextureRegion plainSlot;

    /** The heavy track of a bar that stands on its side, see {@link ProgressKind#vertical()}. */
    private final TextureRegion heavyTrack;

    /**
     * Cuts the pictures of the sheet out.
     *
     * @param textures texture cache providing {@code gui/machine_icons.png}
     * @param style panel of the sheet this set of pictures is cut from, see {@link MachineStyle}
     */
    public MachineTextures(BlockTextureCache textures, MachineStyle style) {
        this.textures = textures;
        this.style = style;
        TextureRegion sheet = textures.region(SHEET);
        if (sheet == null) {
            plainSlot = null;
            heavyTrack = null;
            return;
        }
        int middleWidth = WIDTH - 2 * BORDER;
        int middleHeight = HEIGHT - 2 * BORDER;
        int[] columnStart = {0, BORDER, WIDTH - BORDER};
        int[] columnSize = {BORDER, middleWidth, BORDER};
        int[] rowStart = {style.panelY(), style.panelY() + BORDER, style.panelY() + HEIGHT - BORDER};
        int[] rowSize = {BORDER, middleHeight, BORDER};
        for (int column = 0; column < NineSlice.COUNT; column++) {
            for (int row = 0; row < NineSlice.COUNT; row++) {
                panel[column + row * NineSlice.COUNT] = new TextureRegion(sheet,
                        columnStart[column], rowStart[row],
                        columnSize[column], rowSize[row]);
            }
        }
        for (int row = 0; row < ICON_ROWS; row++) {
            for (int column = 0; column < ICON_COLUMNS; column++) {
                int x = GRID_X + column * ICON_CELL;
                int y = GRID_Y + row * ICON_CELL;
                if (x + ICON_CELL > sheet.getRegionWidth()
                        || y + ICON_CELL > sheet.getRegionHeight()) {
                    continue;
                }
                icons[column + row * ICON_COLUMNS] =
                        new TextureRegion(sheet, x, y, ICON_CELL, ICON_CELL);
            }
        }
        // The plain slot of a style is a slot of its own panel, so it is cut out of the panel and not out of
        // the grid of the icons, see MachineStyle#slotX.
        if (style.slotX() + ICON_CELL <= sheet.getRegionWidth()
                && style.slotY() + ICON_CELL <= sheet.getRegionHeight()) {
            plainSlot = new TextureRegion(sheet, style.slotX(), style.slotY(), ICON_CELL, ICON_CELL);
        }
        else {
            plainSlot = null;
        }
        if (HEAVY_TRACK_X + HEAVY_TRACK_WIDTH <= sheet.getRegionWidth()
                && HEAVY_TRACK_Y + HEAVY_TRACK_HEIGHT <= sheet.getRegionHeight()) {
            heavyTrack = new TextureRegion(sheet, HEAVY_TRACK_X, HEAVY_TRACK_Y, HEAVY_TRACK_WIDTH,
                    HEAVY_TRACK_HEIGHT);
        }
        else {
            heavyTrack = null;
        }
    }

    /** Style this set of pictures draws a machine in. */
    public MachineStyle style() {
        return style;
    }

    @Override
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
                // The grid is measured downwards from the upper edge of the picture, the
                // interface upwards from its lower edge.
                batch.draw(panel[column + row * NineSlice.COUNT],
                        x + slice.x(column), y + height - slice.y(row) - cellHeight,
                        cellWidth, cellHeight);
            }
        }
    }

    @Override
    public void drawSlot(SpriteBatch batch, float x, float y, int column, int row) {
        TextureRegion picture = column == ContainerAppearance.DEFAULT_ICON
                ? plainSlot
                : icon(column, row);
        if (picture != null) {
            // The picture covers the cell and its bevel, which reaches one pixel beyond the
            // sixteen pixels an item icon uses on every side.
            batch.draw(picture, x - PanelTextures.SLOT_BEVEL, y - PanelTextures.SLOT_BEVEL,
                    ICON_CELL, ICON_CELL);
        }
    }

    /**
     * The plain slot of this style, the slot of the panel itself.
     *
     * @return the picture, or {@code null} when the sheet is missing
     */
    public TextureRegion plainSlot() {
        return plainSlot;
    }

    /**
     * The heavy track of a bar that stands on its side.
     * <p>
     * It is larger than the bright part it belongs to, so the two of them are drawn in the cells the bar names
     * plus this one picture, see {@link #arrows(ProgressKind)}.
     *
     * @return the picture, or {@code null} when the sheet is missing
     */
    public TextureRegion heavyTrack() {
        return heavyTrack;
    }

    /**
     * The picture of one kind of slot, as this style draws it.
     * <p>
     * A kind that names a cell of the bronze column of the sheet is drawn from it, a kind of the age of
     * electricity from the grey cell, and the plain kind from {@link #plainSlot()} - which is what makes the
     * same machine screen show bronze slots on a bronze panel and grey ones on a grey panel.
     *
     * @param kind kind of the slot
     * @return the picture, or {@code null} when the sheet is missing
     */
    public TextureRegion icon(SlotKind kind) {
        if (kind == SlotKind.GENERIC) {
            return plainSlot;
        }
        return icon(kind.column(style), kind.row(style));
    }

    /**
     * The picture of an error a machine reports.
     *
     * @param error error of the machine
     * @return the picture, {@code null} for {@link MachineError#NONE} and when the sheet is
     *         missing
     */
    public TextureRegion icon(MachineError error) {
        return error.isError() ? icon(error.column(), error.row()) : null;
    }

    /**
     * The two arrows of one pair of progress bars.
     * <p>
     * A bar that stands on its side is drawn from the heavy track of the sheet instead of the cell of the
     * grid, because a tall bar of a machine of the bronze age is larger than the bright part inside it, see
     * {@link ProgressKind#vertical()}.
     *
     * @param kind kind of the bar
     * @return an element that draws the track and the part that is done
     */
    public ArrowElement arrows(ProgressKind kind) {
        TextureRegion bright = icon(kind.fullColumn(), kind.fullRow());
        TextureRegion track = kind.vertical() ? heavyTrack
                : icon(kind.emptyColumn(), kind.emptyRow());
        return new ArrowElement(bright, track, kind.vertical());
    }

    /**
     * One icon of the grid.
     *
     * @param column column of the cell, counted from the left
     * @param row row of the cell, counted from the top
     * @return the picture, or {@code null} when the sheet is missing or the cell lies
     *         outside of it
     */
    public TextureRegion icon(int column, int row) {
        if (column < 0 || column >= ICON_COLUMNS || row < 0 || row >= ICON_ROWS) {
            return null;
        }
        return icons[column + row * ICON_COLUMNS];
    }

    /** {@code true} when the sheet could be cut out and holds the panel and the plain slot. */
    public boolean isComplete() {
        return panel[0] != null && plainSlot != null;
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
