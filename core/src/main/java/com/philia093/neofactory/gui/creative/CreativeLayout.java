package com.philia093.neofactory.gui.creative;

import com.philia093.neofactory.gui.InventoryLayout;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.util.Constants;

/**
 * Geometry of the creative inventory: what lies where and what a click hits.
 * <p>
 * The numbers are read from the art of the original game, which the sheet of the
 * interface carries since {@code build/verify/extract_creative.ps1} copied it there,
 * see {@link com.philia093.neofactory.gui.panel.CreativeTextures}. They are kept here
 * and not inside the drawing code, because the drawn panel and the slots a click can
 * hit have to be the same rectangle - the screen asks this class and paints from the
 * same answers.
 * <p>
 * Coordinates of the panel are measured from its upper left corner with the Y axis
 * pointing down, the way the art is stored; the tabs stand above that corner and
 * therefore have a negative Y.
 */
public final class CreativeLayout {

    /** Width of the panel picture in pixels. */
    public static final int PANEL_WIDTH = 195;

    /** Height of the panel picture in pixels. */
    public static final int PANEL_HEIGHT = 136;

    /** Amount of columns of the grid of items. */
    public static final int COLUMNS = CreativeInventory.COLUMNS;

    /** Amount of rows of the grid of items. */
    public static final int ROWS = CreativeInventory.ROWS;

    /** Side length of the clickable part of a slot in pixels, the size of an icon. */
    public static final int SLOT_SIZE = Constants.ITEM_ICON_SIZE;

    /** Distance between the left edges of two neighbouring slots in pixels. */
    public static final int SLOT_PITCH = 18;

    /** X coordinate of the first column of the grid, the left edge of an icon. */
    public static final int GRID_X = 9;

    /** Y coordinate of the first row of the grid, the upper edge of an icon. */
    public static final int GRID_Y = 18;

    /** Amount of slots of the hotbar row at the bottom of the panel. */
    public static final int HOTBAR_COLUMNS = PlayerInventory.HOTBAR_SLOTS;

    /** Y coordinate of the hotbar row, the upper edge of an icon. */
    public static final int HOTBAR_Y = 112;

    /** X coordinate of the scroll track on the right edge. */
    public static final int SCROLL_X = 175;

    /** Y coordinate of the upper end of the scroll track. */
    public static final int SCROLL_Y = 17;

    /** Width of the scroll track in pixels. */
    public static final int SCROLL_WIDTH = 12;

    /** Height of the scroll track in pixels. */
    public static final int SCROLL_HEIGHT = 111;

    /** Height of the thumb of the scroll bar in pixels. */
    public static final int SCROLL_THUMB_HEIGHT = 15;

    /** Width of a tab in pixels. */
    public static final int TAB_WIDTH = 27;

    /** Height of a tab in pixels, the part above the panel. */
    public static final int TAB_HEIGHT = 31;

    /** X coordinate of the first tab, relative to the left edge of the panel. */
    public static final int TAB_X = 4;

    /**
     * Distance between the left edges of two neighbouring tabs in pixels.
     * <p>
     * The value is one pixel smaller than {@link #TAB_WIDTH}, so two tabs overlap the
     * way the original game draws them instead of standing apart.
     */
    public static final int TAB_STRIDE = TAB_WIDTH - 1;

    /** Pixels a tab reaches into the panel, so it reads as sitting on its edge. */
    public static final int TAB_INSET = 2;

    /**
     * Largest amount of tabs that fit into one row in pixels.
     * <p>
     * The art gives a tab {@value #TAB_WIDTH} pixels and the rows keep the margin of
     * {@link #TAB_X} on both sides of the panel, which is room for {@value #TAB_COLUMNS}
     * of them.
     */
    public static final int TAB_COLUMNS = (PANEL_WIDTH - 2 * TAB_X - TAB_WIDTH) / TAB_STRIDE + 1;

    /**
     * Amount of rows the tabs are spread over.
     * <p>
     * The first row stands above the panel and the second one below it, the way the
     * original game frames a screen with more tabs than one row can hold.
     */
    public static final int TAB_ROWS = 2;

    /** Distance between the left edge of a tab and the icon drawn on it. */
    public static final int TAB_ICON_X = 6;

    /** Distance between the upper edge of a tab and the icon drawn on it. */
    public static final int TAB_ICON_Y = 6;

    /** X coordinate of the search box, the left edge of its frame. */
    public static final int SEARCH_X = 80;

    /** Y coordinate of the search box, the upper edge of its frame. */
    public static final int SEARCH_Y = 3;

    /** Width of the search box in pixels, its frame included. */
    public static final int SEARCH_WIDTH = 89;

    /** Height of the search box in pixels, its frame included. */
    public static final int SEARCH_HEIGHT = 12;

    /** Distance between the frame of the search box and the text inside it. */
    public static final int SEARCH_INSET = 3;

    /**
     * Pixels the interface keeps free at its lower edge for the hotbar of the player.
     * <p>
     * The bar stands on the bottom edge and is drawn below every screen, see
     * {@link com.philia093.neofactory.gui.HotbarGui}: one pixel more than its background,
     * which is where its selection frame reaches.
     */
    public static final int HOTBAR_MARGIN = InventoryLayout.HOTBAR_HEIGHT + 2;

    private CreativeLayout() {
        // Utility class: never instantiated.
    }

    /**
     * X coordinate of the left edge of the panel inside the interface.
     *
     * @param guiWidth width of the interface in virtual pixels
     * @return the coordinate, the panel is centred
     */
    public static float panelX(float guiWidth) {
        return Math.round((guiWidth - PANEL_WIDTH) * 0.5f);
    }

    /**
     * Y coordinate of the lower edge of the panel inside the interface.
     * <p>
     * The panel is centred on the room above the hotbar of the player and not on the whole
     * interface, so the row of tabs below it stays clear of the bar. That is also why the
     * panel does not move when a screen grows a second row of tabs: the frame around it
     * grows upwards and downwards at the same time, which leaves the panel itself where it
     * was, see {@link #tabY(int, int)}.
     *
     * @param guiHeight height of the interface in virtual pixels
     * @return the coordinate, measured from the bottom of the interface
     */
    public static float panelY(float guiHeight) {
        return Math.round(HOTBAR_MARGIN + (guiHeight - HOTBAR_MARGIN - PANEL_HEIGHT) * 0.5f);
    }

    /**
     * X coordinate of the icon of a slot of the grid, relative to the panel.
     *
     * @param index slot of the grid, {@code 0} to {@value CreativeInventory#PAGE_SIZE}
     *              minus one
     * @return the coordinate of its left edge
     */
    public static int slotIconX(int index) {
        return GRID_X + index % COLUMNS * SLOT_PITCH;
    }

    /**
     * Y coordinate of the icon of a slot of the grid, relative to the panel.
     *
     * @param index slot of the grid, {@code 0} to {@value CreativeInventory#PAGE_SIZE}
     *              minus one
     * @return the coordinate of its upper edge
     */
    public static int slotIconY(int index) {
        return GRID_Y + index / COLUMNS * SLOT_PITCH;
    }

    /**
     * X coordinate of the icon of a hotbar slot, relative to the panel.
     *
     * @param index slot of the hotbar, {@code 0} to {@value #HOTBAR_COLUMNS} minus one
     * @return the coordinate of its left edge
     */
    public static int hotbarIconX(int index) {
        return GRID_X + index * SLOT_PITCH;
    }

    /** Y coordinate of the icons of the hotbar row, relative to the panel. */
    public static int hotbarIconY() {
        return HOTBAR_Y;
    }

    /**
     * Amount of tabs of the upper row.
     * <p>
     * A screen whose tabs fit into one row keeps them there. A screen with more tabs
     * spreads them over two rows - the upper one above the panel and the lower one below
     * it - and splits them as evenly as it can: eight tabs become four above and four
     * below, so neither row looks heavier than the other.
     *
     * @param tabs amount of tabs the screen shows
     * @return the amount of tabs of the upper row
     */
    public static int tabColumns(int tabs) {
        return tabs <= TAB_COLUMNS ? Math.max(tabs, 0) : (Math.min(tabs, TAB_COLUMNS * TAB_ROWS) + 1) / 2;
    }

    /**
     * Row a tab stands in.
     *
     * @param index position of the tab
     * @param tabs amount of tabs the screen shows
     * @return {@code 0} for the row above the panel, {@code 1} for the row below it
     */
    public static int tabRow(int index, int tabs) {
        return index < tabColumns(tabs) ? 0 : 1;
    }

    /**
     * {@code true} when a tab stands below the panel.
     * <p>
     * Such a tab is painted with the very picture of an upper tab, turned by half a circle
     * and mirrored back left to right, so its straight edge points upwards and it sits on
     * the lower edge of the panel, see
     * {@link com.philia093.neofactory.gui.creative.CreativeTextures}.
     *
     * @param index position of the tab
     * @param tabs amount of tabs the screen shows
     * @return {@code true} for a tab of the lower row
     */
    public static boolean isTabBelow(int index, int tabs) {
        return tabRow(index, tabs) > 0;
    }

    /**
     * X coordinate of a tab, relative to the panel.
     *
     * @param index position of the tab
     * @param tabs amount of tabs the screen shows
     * @return the coordinate of its left edge, the row it stands in starts at {@link #TAB_X}
     */
    public static int tabX(int index, int tabs) {
        int column = tabRow(index, tabs) == 0 ? index : index - tabColumns(tabs);
        return TAB_X + column * TAB_STRIDE;
    }

    /**
     * Y coordinate of the upper edge of a tab, relative to the panel.
     * <p>
     * A tab of the upper row lies above the panel and therefore has a negative Y. A tab
     * of the lower row reaches below the lower edge of the panel, which is where the two
     * rows together frame it.
     *
     * @param index position of the tab
     * @param tabs amount of tabs the screen shows
     * @return the coordinate, measured downwards from the upper edge of the panel
     */
    public static int tabY(int index, int tabs) {
        return tabRow(index, tabs) == 0 ? TAB_INSET - TAB_HEIGHT : PANEL_HEIGHT - TAB_INSET;
    }

    /**
     * Y coordinate of the lower edge of a tab inside the interface.
     * <p>
     * The layout measures a tab downwards from the upper edge of the panel, while the
     * interface measures upwards from the lower edge of the window, see
     * {@link com.philia093.neofactory.gui.GuiViewport}: this is the one place where the two
     * are turned into each other, so the picture of a tab and the rectangle a click reaches
     * can never drift apart.
     *
     * @param panelTop Y coordinate of the upper edge of the panel inside the interface
     * @param index position of the tab
     * @param tabs amount of tabs the screen shows
     * @return the coordinate of the lower edge of the tab, which is what a picture is
     *         drawn at
     */
    public static float tabBottom(float panelTop, int index, int tabs) {
        return panelTop - tabY(index, tabs) - TAB_HEIGHT;
    }

    /**
     * X coordinate of the icon of a tab inside the interface.
     * <p>
     * A tab below the panel is mirrored back left to right, see
     * {@link #isTabBelow(int, int)}, which is why its icon sits at the same distance from
     * its left edge as the icon of a tab above it.
     *
     * @param panelX X coordinate of the left edge of the panel inside the interface
     * @param index position of the tab
     * @param tabs amount of tabs the screen shows
     * @return the coordinate of the left edge of the icon
     */
    public static float tabIconX(float panelX, int index, int tabs) {
        return panelX + tabX(index, tabs) + TAB_ICON_X;
    }

    /**
     * Y coordinate of the lower edge of the icon of a tab inside the interface.
     * <p>
     * Mirroring a tab left to right turns it upside down but not the other way round, so
     * the icon of a tab below the panel stays at the edge the picture was turned towards.
     *
     * @param panelTop Y coordinate of the upper edge of the panel inside the interface
     * @param index position of the tab
     * @param tabs amount of tabs the screen shows
     * @return the coordinate of the lower edge of the icon
     */
    public static float tabIconBottom(float panelTop, int index, int tabs) {
        float bottom = tabBottom(panelTop, index, tabs);
        return isTabBelow(index, tabs)
                ? bottom + TAB_ICON_Y
                : bottom + TAB_HEIGHT - TAB_ICON_Y - SLOT_SIZE;
    }

    /**
     * Tab under a point.
     * <p>
     * Two neighbouring tabs overlap by a pixel and the right one is drawn on top, so
     * the tabs are asked from the right: a click never hits a tab that lies behind
     * another one. Both rows are asked, the one above the panel and the one below it.
     *
     * @param localX X coordinate relative to the panel
     * @param localY Y coordinate relative to the panel, measured downwards
     * @param tabs amount of tabs the screen shows
     * @return the tab index, or {@code -1} when the point is beside every tab
     */
    public static int tabAt(int localX, int localY, int tabs) {
        int row = tabRowAt(localY, tabs);
        if (row < 0) {
            return -1;
        }
        for (int index = tabs - 1; index >= 0; index--) {
            if (tabRow(index, tabs) != row) {
                continue;
            }
            int x = tabX(index, tabs);
            if (localX >= x && localX < x + TAB_WIDTH) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Row of tabs a point lies on.
     *
     * @param localY Y coordinate relative to the panel, measured downwards
     * @param tabs amount of tabs the screen shows
     * @return {@code 0} above the panel, {@code 1} below it, {@code -1} beside both rows
     */
    private static int tabRowAt(int localY, int tabs) {
        if (tabs <= 0) {
            return -1;
        }
        if (localY >= tabY(0, tabs) && localY < TAB_INSET) {
            return 0;
        }
        if (tabs > TAB_COLUMNS) {
            int below = tabY(tabs - 1, tabs);
            if (localY >= below && localY < below + TAB_HEIGHT) {
                return 1;
            }
        }
        return -1;
    }

    /**
     * {@code true} when a point lies on the scroll track.
     *
     * @param localX X coordinate relative to the panel
     * @param localY Y coordinate relative to the panel, measured downwards
     * @return {@code true} when a click would reach the scroll bar
     */
    public static boolean isOnScrollTrack(int localX, int localY) {
        return localX >= SCROLL_X && localX < SCROLL_X + SCROLL_WIDTH
                && localY >= SCROLL_Y && localY < SCROLL_Y + SCROLL_HEIGHT;
    }

    /**
     * Y coordinate of the thumb of the scroll bar.
     *
     * @param firstRow first row of the list that is shown
     * @param maxRow last row the list may start at
     * @return the coordinate of the upper edge of the thumb, relative to the panel
     */
    public static int scrollThumbY(int firstRow, int maxRow) {
        int travel = SCROLL_HEIGHT - SCROLL_THUMB_HEIGHT;
        if (maxRow <= 0 || travel <= 0) {
            return SCROLL_Y;
        }
        int step = Math.round(travel * (float) firstRow / maxRow);
        return SCROLL_Y + Math.max(0, Math.min(step, travel));
    }

    /**
     * Row the scroll bar asks for when its track is clicked.
     *
     * @param localY Y coordinate relative to the panel, measured downwards
     * @param maxRow last row the list may start at
     * @return the row, clamped to the list
     */
    public static int rowAtScrollY(int localY, int maxRow) {
        int travel = SCROLL_HEIGHT - SCROLL_THUMB_HEIGHT;
        if (maxRow <= 0 || travel <= 0) {
            return 0;
        }
        int offset = localY - SCROLL_Y - SCROLL_THUMB_HEIGHT / 2;
        int clamped = Math.max(0, Math.min(offset, travel));
        return Math.round(maxRow * (float) clamped / travel);
    }

    /**
     * {@code true} when a point lies on the search box.
     *
     * @param localX X coordinate relative to the panel
     * @param localY Y coordinate relative to the panel, measured downwards
     * @return {@code true} when a click would reach the box
     */
    public static boolean isOnSearchBox(int localX, int localY) {
        return localX >= SEARCH_X && localX < SEARCH_X + SEARCH_WIDTH
                && localY >= SEARCH_Y && localY < SEARCH_Y + SEARCH_HEIGHT;
    }

    /**
     * {@code true} when a point lies on the panel or on a tab above it.
     * <p>
     * A click beside the screen is what throws the carried stack away, see
     * {@link com.philia093.neofactory.gui.container.ContainerMenu#dropCursor()}, so the
     * tabs count as part of the screen and not as beside it.
     *
     * @param localX X coordinate relative to the panel
     * @param localY Y coordinate relative to the panel, measured downwards
     * @param tabs amount of tabs the screen shows
     * @return {@code true} when the point belongs to the screen
     */
    public static boolean isOnScreen(int localX, int localY, int tabs) {
        if (tabAt(localX, localY, tabs) >= 0) {
            return true;
        }
        return localX >= 0 && localX < PANEL_WIDTH && localY >= 0 && localY < PANEL_HEIGHT;
    }
}
