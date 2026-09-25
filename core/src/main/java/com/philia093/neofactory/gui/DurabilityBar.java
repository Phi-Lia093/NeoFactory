package com.philia093.neofactory.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.item.Damageable;

/**
 * The little bar that shows how much life is left in a piece of the inventory.
 * <p>
 * The bar sits on the lower left edge of the icon of a slot, a dark background with a coloured
 * strip in front of it, and it is drawn for everything that wears out and not only for a tool: a
 * mortar or a screwdriver shows its life the same way, which is what {@link Damageable} is asked.
 * A piece that is untouched draws nothing, exactly like the original game: a full bar would say
 * nothing a player does not already know.
 * <p>
 * The strip walks from green over yellow to red while the piece is used, and it is the very hue
 * the original game uses - the part of the life that is left decides a colour between red at the
 * end and green at the start. The arithmetic is plain, so the bar can be measured and coloured
 * without a window, see {@code DurabilityBarTest}.
 */
public final class DurabilityBar {

    /** Width of a full bar in pixels, the width of an icon minus its border. */
    public static final int WIDTH = 13;

    /** Height of the coloured strip in pixels. */
    public static final int HEIGHT = 1;

    /** Height of the background behind the strip in pixels. */
    public static final int BACKGROUND_HEIGHT = 2;

    /** Distance of the bar from the left edge of an icon. */
    public static final int OFFSET_X = 2;

    /**
     * Distance of the strip from the lower edge of an icon.
     * <p>
     * The bar sits on the lower left edge of an icon and the background reaches one pixel below the
     * strip. Both coordinates are measured from the lower edge, which is the direction the interface of
     * libGDX grows in - the original game measures the same bar from the upper edge, which is why its
     * number is {@code 13} and this one is {@code 2}.
     */
    public static final int OFFSET_Y = 2;

    /** Colour of the background, shared and never mutated. */
    public static final Color BACKGROUND = new Color(0.0f, 0.0f, 0.0f, 1.0f);

    /** Colour the strip is computed into, reused by every slot so drawing a grid allocates nothing. */
    private static final Color SCRATCH = new Color(Color.WHITE);

    private DurabilityBar() {
        // Utility class: never instantiated.
    }

    /**
     * Width of the strip for a piece.
     *
     * @param worn piece to measure, may be {@code null}
     * @return the width in pixels, {@code 0} for a piece that never wears out or is {@code null}
     */
    public static int widthOf(Damageable worn) {
        if (worn == null || !worn.isDamageable()) {
            return 0;
        }
        return Math.round(WIDTH * (1.0f - worn.wear()));
    }

    /**
     * Colour of the strip for a piece.
     *
     * @param worn piece to colour, may be {@code null}
     * @return green while the piece is nearly new, yellow in the middle and red at the end,
     *         white for a piece that never wears out or is {@code null}
     */
    public static Color colorOf(Damageable worn) {
        if (worn == null || !worn.isDamageable()) {
            return new Color(Color.WHITE);
        }
        return hueOf(worn, new Color(Color.WHITE));
    }

    /**
     * Draws the bar onto the lower left edge of an icon.
     *
     * @param batch batch switched to the interface projection
     * @param pixel single white pixel the bar is painted with, {@code null} draws nothing
     * @param worn piece to show, may be {@code null}
     * @param x left edge of the icon of the slot
     * @param y lower edge of the icon of the slot
     */
    public static void draw(SpriteBatch batch, TextureRegion pixel, Damageable worn, float x, float y) {
        if (pixel == null || worn == null || !worn.isDamageable() || worn.damage() <= 0) {
            return;
        }
        batch.setColor(BACKGROUND);
        batch.draw(pixel, x + OFFSET_X, y + OFFSET_Y - 1, WIDTH, BACKGROUND_HEIGHT);
        batch.setColor(hueOf(worn, SCRATCH));
        batch.draw(pixel, x + OFFSET_X, y + OFFSET_Y, widthOf(worn), HEIGHT);
        batch.setColor(Color.WHITE);
    }

    /**
     * Colour of the strip of a piece, written into the given colour.
     * <p>
     * The part of the life that is left decides the hue, which is the very one the original game
     * uses: green for a piece that is nearly new, red for one at the end of its life.
     *
     * @param worn piece to colour
     * @param target colour to write into
     * @return the target colour
     */
    private static Color hueOf(Damageable worn, Color target) {
        return fromHue((1.0f - worn.wear()) / 3.0f, target);
    }

    /**
     * Full bright, fully saturated colour of a hue.
     *
     * @param hue hue from {@code 0} for red over {@code 1 / 3} for green to {@code 1}
     * @param target colour to write into
     * @return the target colour, its alpha is {@code 1}
     */
    private static Color fromHue(float hue, Color target) {
        float sector = (hue - (float) Math.floor(hue)) * 6.0f;
        int step = (int) Math.floor(sector);
        float part = sector - step;
        float falling = 1.0f - part;
        switch (step) {
            case 0:
                return target.set(1.0f, part, 0.0f, 1.0f);
            case 1:
                return target.set(falling, 1.0f, 0.0f, 1.0f);
            case 2:
                return target.set(0.0f, 1.0f, part, 1.0f);
            case 3:
                return target.set(0.0f, falling, 1.0f, 1.0f);
            case 4:
                return target.set(part, 0.0f, 1.0f, 1.0f);
            default:
                return target.set(1.0f, 0.0f, falling, 1.0f);
        }
    }
}
