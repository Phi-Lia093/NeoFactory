package com.philia093.neofactory.gui.panel;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

/**
 * Draws an arrow that fills up with the progress of a process.
 * <p>
 * The sheet holds the same arrow twice: one picture in the dark grey of an empty
 * track and one in white. Drawing the dark one first and the bright one on top - cut
 * to the share of the progress - turns the pair into a bar that works for the arrow
 * of the crafting interface and for the gauge of a machine alike.
 * <p>
 * The bright arrow is cut once per possible width when the element is created, so
 * drawing it never allocates anything, no matter how often the progress changes.
 */
public final class ArrowElement {

    /** Width of an arrow in interface pixels. */
    public static final int WIDTH = PanelTextures.ARROW_WIDTH;

    /** Height of an arrow in interface pixels. */
    public static final int HEIGHT = PanelTextures.ARROW_HEIGHT;

    private final PanelTextures textures;

    /** The bright arrow, cut to the widths {@code 0} to {@link #WIDTH}. */
    private final TextureRegion[] parts = new TextureRegion[WIDTH + 1];

    /**
     * Cuts the two arrows out of the sheet.
     *
     * @param textures panel pictures providing the arrows
     */
    public ArrowElement(PanelTextures textures) {
        this.textures = textures;
        TextureRegion full = textures.arrowFull();
        if (full != null) {
            for (int part = 0; part <= WIDTH; part++) {
                parts[part] = new TextureRegion(full, 0, 0, part, HEIGHT);
            }
        }
    }

    /**
     * Draws the track and the part of the arrow that is done.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param x left edge in interface pixels
     * @param y lower edge in interface pixels
     * @param progress share of the arrow that is done, clamped to {@code 0..1}
     */
    public void draw(SpriteBatch batch, float x, float y, float progress) {
        TextureRegion empty = textures.arrowEmpty();
        if (empty != null) {
            batch.draw(empty, x, y, WIDTH, HEIGHT);
        }
        int part = Math.round(MathUtils.clamp(progress, 0.0f, 1.0f) * WIDTH);
        if (part <= 0 || parts[part] == null) {
            return;
        }
        batch.draw(parts[part], x, y, part, HEIGHT);
    }

    /** {@code true} when the arrows of the sheet could be used. */
    public boolean isComplete() {
        return parts[WIDTH] != null;
    }
}
