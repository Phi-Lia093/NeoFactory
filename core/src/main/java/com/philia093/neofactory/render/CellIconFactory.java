package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Color;

/**
 * Paints the window of a fluid cell in the colour of the fluid it holds.
 * <p>
 * A cell is one picture: the steel of the container with a window in the middle, and only that
 * window belongs to the fluid. Everything around it stays what the art pack drew, which is why a
 * cell of water and a cell of oil look like the same object with something else inside - and why
 * a single grey scale picture serves every fluid the game will ever have.
 * <p>
 * The window keeps its own shading: the fluid is drawn as its colour multiplied with the
 * brightness of the pixel in the picture, so the little gradient the art pack drew into the glass
 * survives the pour.
 * <p>
 * The arithmetic is plain {@code int} pixels and never touches libGDX beyond the colour it is
 * handed, so the picture of a filled cell can be checked without a window, see
 * {@code CellIconFactoryTest}.
 */
public final class CellIconFactory {

    /**
     * Left edge of the window inside the picture of a cell.
     * <p>
     * The window is measured in the {@value com.philia093.neofactory.util.Constants#ITEM_ICON_SIZE}
     * pixel picture of a cell, with the origin in its upper left corner: it starts two pixels left
     * of the middle and is two pixels wide and ten pixels tall, so the glass of the cell shows a
     * narrow column of fluid and the steel around it stays visible.
     */
    public static final int WINDOW_X = 7;

    /** Upper edge of the window inside the picture of a cell. */
    public static final int WINDOW_Y = 3;

    /** Width of the window in pixels. */
    public static final int WINDOW_WIDTH = 2;

    /** Height of the window in pixels. */
    public static final int WINDOW_HEIGHT = 10;

    /** Largest value a colour channel may hold. */
    private static final int CHANNEL_MAX = 0xFF;

    private CellIconFactory() {
        // Utility class: never instantiated.
    }

    /**
     * Colours the window of a cell, the steel around it is left as it is drawn.
     *
     * @param icon pixels of the cell, row by row, packed as RGBA8888
     * @param size side length of the cell in pixels
     * @param fluid colour of the fluid inside
     * @return the pixels of the filled cell, row by row, packed as RGBA8888
     */
    public static int[] colour(int[] icon, int size, Color fluid) {
        int[] filled = new int[icon.length];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int index = y * size + x;
                filled[index] = isWindow(x, y) ? paint(icon[index], fluid) : icon[index];
            }
        }
        return filled;
    }

    /**
     * {@code true} when a pixel of the picture lies inside the window of the cell.
     *
     * @param x column, counted from the left of the picture
     * @param y row, counted from the top of the picture
     * @return {@code true} when the fluid shows through that pixel
     */
    public static boolean isWindow(int x, int y) {
        return x >= WINDOW_X && x < WINDOW_X + WINDOW_WIDTH
                && y >= WINDOW_Y && y < WINDOW_Y + WINDOW_HEIGHT;
    }

    /**
     * Colour of one pixel of the window.
     *
     * @param colour pixel of the picture, packed as RGBA8888
     * @param fluid colour of the fluid inside
     * @return the pixel, packed as RGBA8888
     */
    private static int paint(int colour, Color fluid) {
        int brightness = Math.max(red(colour), Math.max(green(colour), blue(colour)));
        return rgba(channel(fluid.r * brightness), channel(fluid.g * brightness),
                channel(fluid.b * brightness), alpha(colour));
    }

    /** Rounds a colour channel and keeps it inside the range of a byte. */
    private static int channel(float value) {
        return Math.min(Math.max(Math.round(value), 0), CHANNEL_MAX);
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

    /** Opacity of a pixel packed as RGBA8888. */
    private static int alpha(int colour) {
        return colour & CHANNEL_MAX;
    }

    /** Packs four channels into a pixel, the format {@code Pixmap} uses. */
    private static int rgba(int r, int g, int b, int a) {
        return (r << 24) | (g << 16) | (b << 8) | a;
    }
}
