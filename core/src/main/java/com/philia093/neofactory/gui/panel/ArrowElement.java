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

    /** Width of the arrow of the interface in interface pixels. */
    public static final int WIDTH = PanelTextures.ARROW_WIDTH;

    /** Height of the arrow of the interface in interface pixels. */
    public static final int HEIGHT = PanelTextures.ARROW_HEIGHT;

    /** Picture of the dark track, drawn behind the bright arrow. */
    private final TextureRegion track;

    /** Width of this arrow in pixels. */
    private final int width;

    /** Height of this arrow in pixels. */
    private final int height;

    /** The bright arrow, cut to the widths {@code 0} to {@link #width()}. */
    private final TextureRegion[] parts;

    /**
     * Cuts the two arrows of the interface out of its sheet.
     *
     * @param panel panel pictures providing the arrows
     */
    public ArrowElement(PanelTextures panel) {
        this(panel.arrowFull(), panel.arrowEmpty());
    }

    /**
     * Cuts a pair of arrows out of any sheet.
     * <p>
     * A machine screen draws its progress from the arrows of its own sheet, which come in a
     * pair per kind of process, see
     * {@link com.philia093.neofactory.machine.ProgressKind}. The size of the element follows
     * the pictures, so a pair of another size needs no other code.
     *
     * @param full the bright arrow, the part that grows with the progress
     * @param empty the dark track behind it
     */
    public ArrowElement(TextureRegion full, TextureRegion empty) {
        this.track = empty;
        this.width = full != null ? full.getRegionWidth() : 0;
        this.height = full != null ? full.getRegionHeight() : 0;
        this.parts = cut(full, width, height);
    }

    /** Cuts the bright arrow into one picture per possible part of the bar. */
    private static TextureRegion[] cut(TextureRegion full, int width, int height) {
        TextureRegion[] found = new TextureRegion[Math.max(0, width) + 1];
        if (full == null || width <= 0 || height <= 0) {
            return found;
        }
        for (int part = 0; part <= width; part++) {
            found[part] = new TextureRegion(full, 0, 0, part, height);
        }
        return found;
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
        if (track != null) {
            batch.draw(track, x, y, width, height);
        }
        int part = Math.round(MathUtils.clamp(progress, 0.0f, 1.0f) * width);
        if (part <= 0 || part >= parts.length || parts[part] == null) {
            return;
        }
        batch.draw(parts[part], x, y, part, height);
    }

    /** Width of this arrow in pixels, {@code 0} when its picture is missing. */
    public int width() {
        return width;
    }

    /** Height of this arrow in pixels, {@code 0} when its picture is missing. */
    public int height() {
        return height;
    }

    /** {@code true} when both arrows of the sheet could be used. */
    public boolean isComplete() {
        return parts.length > 0 && parts[parts.length - 1] != null;
    }
}
