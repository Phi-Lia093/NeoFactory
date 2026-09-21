package com.philia093.neofactory.gui.panel;

/**
 * Splits a target rectangle into the nine cells a scalable panel is drawn from.
 * <p>
 * The picture of a panel is a frame around a plain middle: the four corners keep
 * their pixels, the four edges are stretched along a single axis and the middle is
 * stretched along both. This class only does the arithmetic - where each of the
 * nine cells begins and how large it is - which keeps the drawing code short and
 * lets the geometry be checked without a window, see {@code NineSliceTest}.
 * <p>
 * Coordinates are measured from the upper left corner of the target rectangle with
 * the Y axis pointing down, the direction the art of the game is stored in. The
 * drawing code turns them into the upwards axis of the interface.
 */
public final class NineSlice {

    /** Amount of columns, and of rows, a panel is made of. */
    public static final int COUNT = 3;

    private final int[] columnStart = new int[COUNT];
    private final int[] columnSize = new int[COUNT];
    private final int[] rowStart = new int[COUNT];
    private final int[] rowSize = new int[COUNT];
    private final int border;

    /**
     * Splits a rectangle.
     * <p>
     * The frame is shrunk when the target is too small to hold it twice, so a tiny
     * panel keeps its corners and loses the middle instead of drawing outside of
     * itself.
     *
     * @param width width of the target rectangle in pixels, never below zero
     * @param height height of the target rectangle in pixels, never below zero
     * @param border thickness of the frame of the picture in pixels
     */
    public NineSlice(int width, int height, int border) {
        int safeWidth = Math.max(0, width);
        int safeHeight = Math.max(0, height);
        this.border = Math.max(0, Math.min(border, Math.min(safeWidth, safeHeight) / 2));
        split(safeWidth, columnStart, columnSize);
        split(safeHeight, rowStart, rowSize);
    }

    /** Thickness of the frame that is actually used, the request may be larger. */
    public int border() {
        return border;
    }

    /**
     * Left edge of one column of the grid.
     *
     * @param column column index, {@code 0} left, {@code 1} middle, {@code 2} right
     * @return the offset from the left edge of the target rectangle in pixels
     */
    public int x(int column) {
        return columnStart[check(column)];
    }

    /**
     * Width of one column of the grid.
     *
     * @param column column index, {@code 0} left, {@code 1} middle, {@code 2} right
     * @return the width in pixels, {@code 0} when the column has no room left
     */
    public int width(int column) {
        return columnSize[check(column)];
    }

    /**
     * Upper edge of one row of the grid.
     *
     * @param row row index, {@code 0} top, {@code 1} middle, {@code 2} bottom
     * @return the offset from the upper edge of the target rectangle in pixels
     */
    public int y(int row) {
        return rowStart[check(row)];
    }

    /**
     * Height of one row of the grid.
     *
     * @param row row index, {@code 0} top, {@code 1} middle, {@code 2} bottom
     * @return the height in pixels, {@code 0} when the row has no room left
     */
    public int height(int row) {
        return rowSize[check(row)];
    }

    /**
     * {@code true} when a cell of the grid is large enough to be drawn.
     *
     * @param column column index of the cell
     * @param row row index of the cell
     * @return {@code true} when the cell covers at least one pixel
     */
    public boolean isVisible(int column, int row) {
        return width(column) > 0 && height(row) > 0;
    }

    /** Fills the offsets and the sizes of the three columns or rows of one axis. */
    private void split(int size, int[] start, int[] extent) {
        int middle = Math.max(0, size - 2 * border);
        start[0] = 0;
        start[1] = border;
        start[2] = size - border;
        extent[0] = border;
        extent[1] = middle;
        extent[2] = border;
    }

    /** Verifies that an index names a column or a row of the grid. */
    private static int check(int index) {
        if (index < 0 || index >= COUNT) {
            throw new IllegalArgumentException("No such column or row: " + index);
        }
        return index;
    }
}
