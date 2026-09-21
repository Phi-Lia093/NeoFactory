package com.philia093.neofactory.chat;

/**
 * Measures and cuts a line so that it fits into the input field.
 * <p>
 * A line that is longer than the field is not wrapped, the beginning is dropped
 * instead and the end stays visible - the cursor sits there, and a player who types
 * a long command wants to see what they are typing now. The measurement is given
 * as a function because the width of a text depends on the font, see
 * {@link com.philia093.neofactory.render.PixelFont#width(String)}, which in turn
 * needs a window. That keeps the cutting testable on its own.
 */
public final class ChatText {

    /** Measures the width of a text in pixels. */
    public interface Width {

        /**
         * Width of a text.
         *
         * @param text text to measure
         * @return the width in pixels
         */
        float of(String text);
    }

    private ChatText() {
        // Utility class: never instantiated.
    }

    /**
     * Index of the first character of a text that is drawn.
     * <p>
     * Everything before the index does not fit into the given width and is dropped.
     *
     * @param text text the player typed
     * @param available width the field offers for the text, in pixels
     * @param width measures a text
     * @return the index of the first visible character, {@code text.length()} when
     *         not even one character fits
     */
    public static int firstVisibleIndex(String text, float available, Width width) {
        if (text == null || text.isEmpty() || available <= 0.0f) {
            return text == null ? 0 : text.length();
        }
        int start = 0;
        while (start < text.length() && width.of(text.substring(start)) > available) {
            start++;
        }
        return start;
    }

    /**
     * The beginning of a text that fits into a width.
     *
     * @param text text to cut
     * @param available width the row offers for the text, in pixels
     * @param width measures a text
     * @return the visible part of the text, empty when nothing fits
     */
    public static String head(String text, float available, Width width) {
        if (text == null || text.isEmpty() || available <= 0.0f) {
            return "";
        }
        int end = text.length();
        while (end > 0 && width.of(text.substring(0, end)) > available) {
            end--;
        }
        return text.substring(0, end);
    }

    /**
     * The end of a text that fits into a width.
     *
     * @param text text the player typed
     * @param available width the field offers for the text, in pixels
     * @param width measures a text
     * @return the visible part of the text, empty when nothing fits
     */
    public static String tail(String text, float available, Width width) {
        if (text == null) {
            return "";
        }
        return text.substring(firstVisibleIndex(text, available, width));
    }
}
