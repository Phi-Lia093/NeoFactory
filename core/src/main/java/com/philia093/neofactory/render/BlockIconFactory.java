package com.philia093.neofactory.render;

/**
 * Turns the picture of a block into the icon of a block item.
 * <p>
 * The world is a view from above, so a block owns a single picture and nothing
 * else: there is no separate face for a side or a bottom. An item that lies in a
 * slot, however, reads much better when it looks like the object it stands for, so
 * this factory folds the flat tile into a small cube. The faces the view does not
 * meet are derived from the tile itself - the edge of the picture is pulled into
 * the depth and darkened a little - which keeps the icon in step with the block
 * without asking the art pack for a second picture.
 * <p>
 * Brightness, not geometry, tells the faces apart: the top keeps the colours of the
 * tile, the face towards the light keeps {@link #LIT_FACE_LIGHT} of them and the one
 * turned away keeps {@link #SHADED_FACE_LIGHT}. Because the factor is baked into the
 * pixels, the tint of a block is still applied on top of it at draw time, which is
 * what keeps the grey scale sheets of grass and leaves green.
 * <p>
 * The class is plain arithmetic on {@code int} pixels and never touches libGDX, so
 * every projection can be checked without a window, see
 * {@code BlockIconFactoryTest}. Colours are packed as RGBA8888, the format
 * {@code Pixmap} uses.
 */
public final class BlockIconFactory {

    /** Brightness of the top face, the one the light falls on from above. */
    public static final float TOP_LIGHT = 1.0f;

    /** Brightness of the face that looks towards the light. */
    public static final float LIT_FACE_LIGHT = 0.72f;

    /** Brightness of the face that is turned away from the light. */
    public static final float SHADED_FACE_LIGHT = 0.55f;

    /** Brightness the lowest edge of a face keeps, the fall off adds the depth. */
    public static final float BOTTOM_FALLOFF = 0.78f;

    /** Largest value a colour channel may hold. */
    private static final int CHANNEL_MAX = 0xFF;

    private BlockIconFactory() {
        // Utility class: never instantiated.
    }

    /**
     * Source of the pixels of one tile, origin in the upper left corner.
     * <p>
     * The interface keeps this factory free of libGDX: the game wraps a
     * {@code Pixmap} in it, a test can wrap a plain array.
     */
    @FunctionalInterface
    public interface IconSource {

        /**
         * Returns the colour of one pixel.
         *
         * @param x column, counted from the left
         * @param y row, counted from the top
         * @return the colour packed as RGBA8888
         */
        int pixel(int x, int y);
    }

    /**
     * Folds the tile into a cube seen from a corner.
     * <p>
     * The top face is the picture turned by forty five degrees and squeezed to half
     * of its height: a source pixel at {@code (u, v)} lands on the column
     * {@code (u - v) / 2 + size / 2} and the row {@code (u + v) / 4}, which turns the
     * square into a diamond of twice the width. The two side faces are the lower and
     * the right edge of the picture pulled down by half the icon: the left face
     * samples the bottom row, the right face the rightmost column, and both walk one
     * row into the picture per step of the depth.
     *
     * @param source pixels of the block tile
     * @param size side length of the icon in pixels
     * @return the pixels of the icon, row by row, packed as RGBA8888
     */
    public static int[] isometric(IconSource source, int size) {
        int[] icon = new int[size * size];
        int centreX = size / 2;
        int height = size / 2;

        // The top face. Two source pixels share a column and four share a row, so
        // every cell of the diamond is written and none is left empty.
        for (int v = 0; v < size; v++) {
            for (int u = 0; u < size; u++) {
                int x = ((u - v) >> 1) + centreX;
                int y = (u + v) >> 2;
                if (x < 0 || x >= size || y < 0 || y >= size) {
                    continue;
                }
                icon[y * size + x] = shade(source.pixel(u, v), TOP_LIGHT);
            }
        }

        // The left face. Its upper edge follows the lower left edge of the diamond,
        // one row lower for every two columns towards the middle.
        for (int x = 0; x < centreX; x++) {
            int top = size / 4 + (x >> 1);
            for (int depth = 0; depth < height; depth++) {
                int y = top + depth;
                if (y >= size) {
                    break;
                }
                int u = Math.min(size - 1, x * 2);
                int v = Math.max(0, size - 1 - depth);
                icon[y * size + x] = shade(source.pixel(u, v),
                        faceLight(LIT_FACE_LIGHT, depth, height));
            }
        }

        // The right face, mirrored: its upper edge follows the lower right edge of
        // the diamond and it samples the rightmost column of the picture.
        for (int x = centreX; x < size; x++) {
            int top = size / 2 - ((x - centreX) >> 1);
            for (int depth = 0; depth < height; depth++) {
                int y = top + depth;
                if (y >= size) {
                    break;
                }
                int u = Math.max(0, size - 1 - depth);
                int v = Math.max(0, size - 1 - (x - centreX) * 2);
                icon[y * size + x] = shade(source.pixel(u, v),
                        faceLight(SHADED_FACE_LIGHT, depth, height));
            }
        }
        return icon;
    }

    /**
     * Grows a border of one colour around an icon.
     * <p>
     * Every place that is empty in the icon but touches a visible pixel becomes the border
     * colour, so the result is the outline of the icon: it is what a dropped item is drawn
     * with, which lifts it off the ground it lies on. The border is only grown into empty
     * places, a pixel of the icon itself is never touched.
     *
     * @param icon pixels of the icon, row by row, packed as RGBA8888
     * @param size side length of the icon in pixels
     * @param border thickness of the outline in pixels
     * @param colour colour of the outline, packed as RGBA8888
     * @return the outlined picture, {@code size + 2 * border} pixels wide and high
     */
    public static int[] outline(int[] icon, int size, int border, int colour) {
        int grown = size + 2 * border;
        int[] outlined = new int[grown * grown];
        for (int y = 0; y < grown; y++) {
            for (int x = 0; x < grown; x++) {
                if (isVisible(icon, size, x - border, y - border)) {
                    outlined[y * grown + x] = icon[(y - border) * size + x - border];
                } else if (touchesVisible(icon, size, x - border, y - border)) {
                    outlined[y * grown + x] = colour;
                }
            }
        }
        return outlined;
    }

    /** {@code true} when a pixel of an icon is visible, a place outside is not. */
    private static boolean isVisible(int[] icon, int size, int x, int y) {
        return x >= 0 && y >= 0 && x < size && y < size && alpha(icon[y * size + x]) != 0;
    }

    /** {@code true} when any of the eight neighbours of a place belongs to the icon. */
    private static boolean touchesVisible(int[] icon, int size, int x, int y) {
        for (int offsetY = -1; offsetY <= 1; offsetY++) {
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                if (offsetX == 0 && offsetY == 0) {
                    continue;
                }
                if (isVisible(icon, size, x + offsetX, y + offsetY)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Brightness of a face at one step of the depth.
     *
     * @param base brightness of the face at its upper edge
     * @param step step of the depth, {@code 0} at the upper edge
     * @param steps amount of steps the depth is made of
     * @return the brightness to multiply the colour of that pixel with
     */
    private static float faceLight(float base, int step, int steps) {
        float fade = steps > 1 ? (float) step / (steps - 1) : 0.0f;
        return base * (1.0f - fade * (1.0f - BOTTOM_FALLOFF));
    }

    /**
     * Multiplies the colour channels of a pixel, the opacity stays untouched.
     *
     * @param colour pixel packed as RGBA8888
     * @param light factor to multiply red, green and blue with
     * @return the shaded pixel, packed as RGBA8888
     */
    private static int shade(int colour, float light) {
        if (light == TOP_LIGHT) {
            return colour;
        }
        return rgba(channel(red(colour) * light), channel(green(colour) * light),
                channel(blue(colour) * light), alpha(colour));
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
