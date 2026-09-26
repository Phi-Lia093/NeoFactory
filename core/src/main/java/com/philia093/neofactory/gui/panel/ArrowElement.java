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
 * <b>A bar that stands on its side</b> grows from the bottom of its cell upwards and is filled instead of cut,
 * see {@link #ArrowElement(TextureRegion, TextureRegion, boolean)}: the bright part of such a bar is a plain
 * block of colour, so stretching it is what makes it meet the top of its track exactly. Its track may be larger
 * than the bright part - the tall bar of a forge hammer is - and the picture of the element follows whichever of
 * the two is bigger.
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

    /** Picture of the bright arrow, the part that grows with the progress. */
    private final TextureRegion bright;

    /** {@code true} when this bar grows upwards instead of to the right. */
    private final boolean vertical;

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
        this(full, empty, false);
    }

    /**
     * Cuts a pair of arrows out of any sheet, either across or upwards.
     * <p>
     * A bar that stands on its side grows upwards from the bottom of its cell, and it is filled rather than
     * cut: the bright part of such a bar is a plain block of colour, so stretching it to the height the
     * progress asks for is what makes it meet the top of the track. The size of the element is the larger of
     * the two pictures on either axis, because the tall bar of a forge hammer carries a track that is bigger
     * than the bright part inside it.
     *
     * @param full the bright arrow, the part that grows with the progress
     * @param empty the dark track behind it
     * @param vertical {@code true} when the bar grows upwards, {@code false} for one that grows to the right
     */
    public ArrowElement(TextureRegion full, TextureRegion empty, boolean vertical) {
        this.track = empty;
        this.bright = full;
        this.vertical = vertical;
        this.width = Math.max(full != null ? full.getRegionWidth() : 0,
                empty != null ? empty.getRegionWidth() : 0);
        this.height = Math.max(full != null ? full.getRegionHeight() : 0,
                empty != null ? empty.getRegionHeight() : 0);
        // A vertical bar is never cut: the whole picture is drawn, stretched to the height the progress asks
        // for, so a track that is taller than the bright part still ends up full.
        this.parts = vertical ? new TextureRegion[0] : cut(full, width, height);
    }

    /** Cuts the bright arrow into one picture per possible part of the bar. */
    private static TextureRegion[] cut(TextureRegion full, int width, int height) {
        TextureRegion[] found = new TextureRegion[Math.max(0, width) + 1];
        if (full == null || width <= 0 || height <= 0) {
            return found;
        }
        int reachable = Math.min(width, full.getRegionWidth());
        for (int part = 0; part <= reachable; part++) {
            found[part] = new TextureRegion(full, 0, 0, part, full.getRegionHeight());
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
        float done = MathUtils.clamp(progress, 0.0f, 1.0f);
        if (bright == null || done <= 0.0f) {
            return;
        }
        if (vertical) {
            batch.draw(bright, x, y, width, height * done);
            return;
        }
        int part = Math.round(done * width);
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

    /** {@code true} when this bar grows upwards instead of to the right. */
    public boolean isVertical() {
        return vertical;
    }

    /** {@code true} when the bright arrow of the sheet could be used. */
    public boolean isComplete() {
        return bright != null && (vertical || parts.length > 0 && parts[parts.length - 1] != null);
    }
}
