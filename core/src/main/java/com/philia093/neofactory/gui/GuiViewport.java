package com.philia093.neofactory.gui;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Viewport of the user interface.
 * <p>
 * The interface is drawn in virtual pixels that are blown up by a whole number, so
 * a slot of 16 pixels stays crisp and readable no matter how large the window is.
 * The factor follows the size of the window the way the original game picks its
 * interface scale: the smaller axis decides, because a window of
 * {@link #MIN_WIDTH} by {@link #MIN_HEIGHT} virtual pixels has to show the whole
 * interface.
 * <p>
 * Drawing and hit testing both go through this viewport, see
 * {@link #unproject(float, float, Vector2)} and {@link #project(Vector2)}. That is
 * what keeps a click on the slot the player sees, also in fullscreen, after the
 * window was resized or on a display with a different scale.
 */
public class GuiViewport extends ScreenViewport {

    /** Width in virtual pixels the interface is designed for. */
    public static final int MIN_WIDTH = 320;

    /** Height in virtual pixels the interface is designed for. */
    public static final int MIN_HEIGHT = 240;

    /** Largest factor the interface is blown up by. */
    public static final int MAX_SCALE = 4;

    /** Factor the interface is currently drawn with. */
    private int scale = 1;

    /** Creates a viewport, the factor is chosen by {@link #update(int, int, boolean)}. */
    public GuiViewport() {
        super();
    }

    @Override
    public void update(int screenWidth, int screenHeight, boolean centerCamera) {
        scale = chooseScale(screenWidth, screenHeight);
        // One virtual pixel covers "scale" pixels of the window, therefore the world
        // of the interface shrinks and everything drawn in it grows.
        setUnitsPerPixel(1.0f / scale);
        super.update(screenWidth, screenHeight, centerCamera);
    }

    /** Factor the interface is currently blown up by. */
    public int scale() {
        return scale;
    }

    /** Width of the interface in virtual pixels. */
    public float guiWidth() {
        return getWorldWidth();
    }

    /** Height of the interface in virtual pixels. */
    public float guiHeight() {
        return getWorldHeight();
    }

    /**
     * Picks the factor for a window size.
     * <p>
     * A window of 1280 by 800 pixels gets a factor of three, which turns the 176 by
     * 166 pixel container into a 528 by 498 pixel panel. The factor never drops
     * below one and never grows past {@link #MAX_SCALE}, so the interface stays
     * readable on a small window and does not swallow a very large one.
     *
     * @param screenWidth width of the window in pixels
     * @param screenHeight height of the window in pixels
     * @return the factor, {@code 1} to {@code MAX_SCALE}
     */
    public static int chooseScale(int screenWidth, int screenHeight) {
        int fitting = Math.min(screenWidth / MIN_WIDTH, screenHeight / MIN_HEIGHT);
        return Math.max(1, Math.min(fitting, MAX_SCALE));
    }

    /**
     * Converts a point of the window into the virtual pixels of the interface.
     *
     * @param screenX X coordinate of the window, measured from the left
     * @param screenY Y coordinate of the window, measured from the top, which is
     *                what libGDX reports for the mouse
     * @param out vector the result is written into
     * @return the same vector, holding the coordinate inside the interface
     */
    public Vector2 unproject(float screenX, float screenY, Vector2 out) {
        out.set(screenX, screenY);
        return super.unproject(out);
    }
}
