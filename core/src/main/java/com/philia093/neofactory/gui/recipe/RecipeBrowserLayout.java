package com.philia093.neofactory.gui.recipe;

import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.gui.panel.NeiTextures;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.machine.MachineMenu;
import com.philia093.neofactory.render.PixelFont;

/**
 * Geometry of the screen of recipes: what lies where and what a click hits.
 * <p>
 * The panel is the panel of the interface - the same picture a chest and the inventory of the player are
 * drawn with, stretched in nine cells - so the screen of recipes reads as part of the game and needs no art
 * of its own, see {@link com.philia093.neofactory.gui.panel.PanelTextures}.
 * <p>
 * The screen knows two shapes. <b>A page of a recipe</b> shows the ingredients in three columns and the
 * product beside them, with a foot that carries the two arrows and the number of the page. <b>A list</b>
 * shows the items of a category in a grid of nine columns, with a box to search them, a scroll bar on its
 * right edge and the same two arrows at the foot. Both are laid out here, and a click is answered from the
 * same numbers, so what a player sees and what they hit are always the same rectangle.
 * <p>
 * Coordinates are measured from the upper left corner of the panel with the Y axis pointing down.
 */
public final class RecipeBrowserLayout {

    /** Width of the panel, the width the art of the interface asks for. */
    public static final int WIDTH = PanelTextures.PANEL_WIDTH;

    /** Distance between the frame of the panel and the outermost cell. */
    public static final int PADDING = ContainerLayout.PADDING;

    /** Distance between the left edges of two neighbouring cells. */
    public static final int PITCH = ContainerLayout.SLOT_PITCH;

    /** Side length of the clickable part of a cell, the size of an icon. */
    public static final int CELL = Slot.size();

    /** Height of the band at the top of the panel that holds the name of what is shown. */
    public static final int TITLE_HEIGHT = 18;

    /** Amount of columns a page of a recipe is laid out in. */
    public static final int PAGE_COLUMNS = 3;

    /** Amount of rows a page of a recipe is laid out in, whatever the recipe asks for. */
    public static final int PAGE_ROWS = 3;

    /** Height of the foot of the panel, where the arrows and the number of the page lie. */
    public static final int FOOTER_HEIGHT = 22;

    /** X coordinate of the first column of a page. */
    public static final int FIELD_X = PADDING;

    /** Y coordinate of the first row of a page. */
    public static final int FIELD_Y = TITLE_HEIGHT;

    /** X coordinate of the arrow that points from the ingredients to the product. */
    public static final int ARROW_X = FIELD_X + PAGE_COLUMNS * PITCH - 2 + 6;

    /** Y coordinate of the arrow, centred on the middle row of the page. */
    public static final int ARROW_Y = FIELD_Y + PITCH;

    /** X coordinate of the cell that holds what the recipe makes. */
    public static final int RESULT_X = ARROW_X + PanelTextures.ARROW_WIDTH + 6;

    /** Y coordinate of the cell that holds what the recipe makes. */
    public static final int RESULT_Y = ARROW_Y;

    /** X coordinate of the arrow that walks to the page before this one. */
    public static final int PREV_X = PADDING;

    /** X coordinate of the arrow that walks to the page after this one. */
    public static final int NEXT_X = PADDING + 22;

    /** Y coordinate of the foot of the panel. */
    public static final int FOOTER_Y = FIELD_Y + PAGE_ROWS * PITCH + 4;

    /** Side length of the square one of the two page arrows is drawn in. */
    public static final int ARROW_SIZE = 16;

    /** X coordinate of the line that names the page, right of the two arrows. */
    public static final int PAGE_TEXT_X = NEXT_X + 22;

    /** Amount of columns of a list of items. */
    public static final int LIST_COLUMNS = 9;

    /** Amount of rows of a list of items that fit into the panel at once. */
    public static final int LIST_ROWS = 4;

    /** X coordinate of the first column of a list. */
    public static final int LIST_X = PADDING;

    /** Y coordinate of the first row of a list. */
    public static final int LIST_Y = TITLE_HEIGHT;

    /** Width of the track of the scroll bar. */
    public static final int SCROLL_WIDTH = 12;

    /** X coordinate of the track of the scroll bar, on the right edge of the panel. */
    public static final int SCROLL_X = WIDTH - PADDING - SCROLL_WIDTH;

    /** Y coordinate of the upper end of the track of the scroll bar. */
    public static final int SCROLL_Y = LIST_Y;

    /** Height of the track of the scroll bar, as tall as the list it belongs to. */
    public static final int SCROLL_HEIGHT = LIST_ROWS * PITCH - 2;

    /** Height of the thumb of the scroll bar. */
    public static final int SCROLL_THUMB_HEIGHT = 15;

    /** Height of the box a player types a search into. */
    public static final int SEARCH_HEIGHT = 16;

    /** X coordinate of the search box. */
    public static final int SEARCH_X = PADDING;

    /** Width of the search box, right of the two page arrows of a list. */
    public static final int SEARCH_WIDTH = LIST_COLUMNS * PITCH - 2 - 2 * ARROW_SIZE - 8;

    /** Y coordinate of the box a player types a search into, below the list. */
    public static final int SEARCH_Y = LIST_Y + LIST_ROWS * PITCH + 4;

    /** Y coordinate of the two arrows that walk through the groups in the head of the panel. */
    public static final int GROUP_Y = 1;

    /** X coordinate of the arrow that walks to the group before this one. */
    public static final int GROUP_PREV_X = WIDTH - PADDING - 2 * ARROW_SIZE - 4;

    /** X coordinate of the arrow that walks on one group. */
    public static final int GROUP_NEXT_X = WIDTH - PADDING - ARROW_SIZE;

    /**
     * Y coordinate of the name of a machine, the line the recipe writes the machine into.
     * <p>
     * A recipe of a machine is shown <b>in the panel of that machine</b>: its own slots stand where the
     * machine stands them, and the two lines of the head are the ones a machine screen writes its name and
     * its status on, so the screen of recipes reads as the screen of the machine that makes the item.
     */
    public static final int TEMPLATE_NAME_Y = MachineMenu.TEXT_TOP;

    /** Height of the panel of a machine, the height a template is laid out in. */
    public static final int TEMPLATE_HEIGHT = MachineMenu.HEIGHT;

    /**
     * Y coordinate of the first line of what a recipe reports.
     * <p>
     * The report takes the rows the player inventory of a machine screen stands in: a recipe that is being
     * <b>looked at</b> has no inventory of a player to show, so those rows carry what the recipe costs -
     * how much energy it takes, how much of it a tick, at which voltage and how long it runs.
     */
    public static final int TEMPLATE_INFO_Y = MachineMenu.PLAYER_STORAGE_TOP;

    /** X coordinate of the column the report of a recipe stands in. */
    public static final int TEMPLATE_INFO_X = MachineMenu.PLAYER_LEFT;

    /** Height of one line of the report. */
    public static final int TEMPLATE_LINE_HEIGHT = PixelFont.ASCII_CELL_SIZE + 2;

    /** Amount of lines the report of a recipe holds. */
    public static final int TEMPLATE_INFO_LINES = 5;

    /** Width of the column the page and the two buttons are named in, on the right edge of the report. */
    public static final int TEMPLATE_HINT_WIDTH = 46;

    /**
     * X coordinate of the column that names the page and the two buttons.
     * <p>
     * It stands right of the report and never over it, which is what {@link #TEMPLATE_INFO_WIDTH} keeps.
     */
    public static final int TEMPLATE_HINT_X = WIDTH - PADDING - TEMPLATE_HINT_WIDTH;

    /** Width the report of a recipe may take, so it never runs into the column of the page. */
    public static final int TEMPLATE_INFO_WIDTH = TEMPLATE_HINT_X - TEMPLATE_INFO_X - 4;

    /** Distance between the left edges of the two buttons that turn a page. */
    public static final int TEMPLATE_HINT_BUTTONS = 20;

    /** Side of the square a button of the page is drawn in, the art of NEI. */
    public static final int TEMPLATE_BUTTON_SIZE = NeiTextures.BUTTON_SIZE;

    /**
     * Y coordinate of the row that carries the page and its two buttons.
     * <p>
     * It lies below the report and not beside it, which is what keeps the two from standing on each other
     * however long a line of the report turns out to be: the report takes the rows the inventory of a machine
     * stands in and the row under them carries {@code < X/Y >}.
     */
    public static final int TEMPLATE_PAGE_Y = TEMPLATE_INFO_Y
            + TEMPLATE_INFO_LINES * TEMPLATE_LINE_HEIGHT;

    /** X coordinate of the button that walks on one page, on the right edge of the panel. */
    public static final int TEMPLATE_NEXT_X = WIDTH - PADDING - TEMPLATE_BUTTON_SIZE;

    /** X coordinate of the number of the page, between the two buttons. */
    public static final int TEMPLATE_PAGE_TEXT_X = TEMPLATE_NEXT_X - 4 - 3 * PixelFont.ASCII_CELL_SIZE;

    /** X coordinate of the button that walks one page back, left of the number of the page. */
    public static final int TEMPLATE_PREV_X = TEMPLATE_PAGE_TEXT_X - 2 - TEMPLATE_BUTTON_SIZE;

    private RecipeBrowserLayout() {
        // Utility class: never instantiated.
    }

    /**
     * Y coordinate of one line of the report of a recipe.
     *
     * @param line line index, {@code 0} for the first one
     * @return the upper edge of that line in pixels
     */
    public static int templateLineY(int line) {
        return TEMPLATE_INFO_Y + line * TEMPLATE_LINE_HEIGHT;
    }

    /**
     * Width a line of the screen takes.
     * <p>
     * It is counted with the cell of the font, which is the widest a glyph of it can be and therefore the
     * widest a line can be, so a line that fits by this measure fits where it is drawn. A screen of this game
     * has to keep its lines inside its panel and no card is asked whether a line runs out of it, see
     * {@code NeiPreviewTest}.
     *
     * @param text line of the screen
     * @return the width in pixels
     */
    public static int textWidth(String text) {
        return text.length() * PixelFont.ASCII_CELL_SIZE;
    }

    /**
     * Amount of glyphs of a line that fit between the frames of the panel.
     *
     * @return the amount, at least one
     */
    public static int headGlyphs() {
        return Math.max(1, (WIDTH - 2 * PADDING) / PixelFont.ASCII_CELL_SIZE);
    }

    /**
     * Trims a line to what the panel holds.
     *
     * @param text line of the screen
     * @return the line, its end left out when it would run out of the panel
     */
    public static String trimToPanel(String text) {
        return text.length() <= headGlyphs() ? text : text.substring(0, headGlyphs());
    }

    /**
     * {@code true} when the report of a recipe fits the rows of the machine panel it is written in.
     *
     * @return {@code true} when every line of the report lies above the lower edge of the panel
     */
    public static boolean templateReportFits() {
        return templateLineY(TEMPLATE_INFO_LINES - 1) + TEMPLATE_LINE_HEIGHT
                <= TEMPLATE_HEIGHT - PADDING;
    }

    /**
     * Height of the panel.
     *
     * @param list {@code true} for a list of items, {@code false} for a page of a recipe
     * @return the height in pixels
     */
    public static int height(boolean list) {
        return list ? SEARCH_Y + SEARCH_HEIGHT + PADDING : TEMPLATE_HEIGHT;
    }

    /** X coordinate of one column of a page. */
    public static int cellX(int column) {
        return FIELD_X + column * PITCH;
    }

    /** Y coordinate of one row of a page. */
    public static int cellY(int row) {
        return FIELD_Y + row * PITCH;
    }

    /** X coordinate of one column of a list. */
    public static int listX(int column) {
        return LIST_X + column * PITCH;
    }

    /** Y coordinate of one row of a list. */
    public static int listY(int row) {
        return LIST_Y + row * PITCH;
    }

    /**
     * Slot of a list under a point of the panel.
     *
     * @param localX X coordinate relative to the panel
     * @param localY Y coordinate relative to the panel, measured downwards
     * @return the slot, {@code 0} to {@code LIST_COLUMNS * LIST_ROWS} minus one, or {@code -1} beside it
     */
    public static int listCellAt(int localX, int localY) {
        if (localX < LIST_X || localY < LIST_Y) {
            return -1;
        }
        int column = (localX - LIST_X) / PITCH;
        int row = (localY - LIST_Y) / PITCH;
        if (column >= LIST_COLUMNS || row >= LIST_ROWS) {
            return -1;
        }
        if (localX - LIST_X - column * PITCH >= CELL || localY - LIST_Y - row * PITCH >= CELL) {
            // The bevel between two cells belongs to neither of them.
            return -1;
        }
        return row * LIST_COLUMNS + column;
    }

    /**
     * {@code true} when a point lies on the page itself, which is where a click lays a recipe out.
     *
     * @param localX X coordinate relative to the panel
     * @param localY Y coordinate relative to the panel, measured downwards
     * @return {@code true} for the area that holds the ingredients and the product
     */
    public static boolean isOnPage(int localX, int localY) {
        return localX >= FIELD_X && localX < RESULT_X + CELL && localY >= FIELD_Y
                && localY < FIELD_Y + PAGE_ROWS * PITCH;
    }

    /**
     * Y coordinate of the two page arrows.
     * <p>
     * A page carries them in its foot; a list shares its last row with the search box, so they stand beside
     * the box there, see {@link #SEARCH_Y}.
     *
     * @param list {@code true} for a list of items, {@code false} for a page of a recipe
     * @return the coordinate of the upper edge of the squares
     */
    public static int pageArrowY(boolean list) {
        return list ? SEARCH_Y : FOOTER_Y;
    }

    /** {@code true} when a point lies on the arrow that walks back one page. */
    public static boolean isOnPrevious(int localX, int localY, boolean list) {
        return isOnArrow(localX, localY, PREV_X, pageArrowY(list));
    }

    /** {@code true} when a point lies on the arrow that walks on one page. */
    public static boolean isOnNext(int localX, int localY, boolean list) {
        return isOnArrow(localX, localY, NEXT_X, pageArrowY(list));
    }

    /** {@code true} when a point lies on the search box. */
    public static boolean isOnSearch(int localX, int localY) {
        return localX >= SEARCH_X && localX < SEARCH_X + SEARCH_WIDTH && localY >= SEARCH_Y
                && localY < SEARCH_Y + SEARCH_HEIGHT;
    }

    /** {@code true} when a point lies on the track of the scroll bar. */
    public static boolean isOnScroll(int localX, int localY) {
        return localX >= SCROLL_X && localX < SCROLL_X + SCROLL_WIDTH && localY >= SCROLL_Y
                && localY < SCROLL_Y + SCROLL_HEIGHT;
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

    /** {@code true} when a point lies on the arrow that walks back one group. */
    public static boolean isOnGroupPrevious(int localX, int localY) {
        return isOnGroupArrow(localX, localY, GROUP_PREV_X);
    }

    /** {@code true} when a point lies on the arrow that walks on one group. */
    public static boolean isOnGroupNext(int localX, int localY) {
        return isOnGroupArrow(localX, localY, GROUP_NEXT_X);
    }

    /** {@code true} when a point lies on the square of one of the two group arrows. */
    private static boolean isOnGroupArrow(int localX, int localY, int arrowX) {
        return localX >= arrowX && localX < arrowX + ARROW_SIZE && localY >= GROUP_Y
                && localY < GROUP_Y + ARROW_SIZE;
    }

    /** {@code true} when a point lies on the square of one of the two page arrows. */
    private static boolean isOnArrow(int localX, int localY, int arrowX, int arrowY) {
        return localX >= arrowX && localX < arrowX + ARROW_SIZE && localY >= arrowY
                && localY < arrowY + ARROW_SIZE;
    }
}
