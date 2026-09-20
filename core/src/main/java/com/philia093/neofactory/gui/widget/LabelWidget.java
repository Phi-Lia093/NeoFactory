package com.philia093.neofactory.gui.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.philia093.neofactory.render.PixelFont;

import java.util.Objects;

/**
 * A piece of text that belongs to a screen.
 * <p>
 * A label draws nothing but its text: headings, hints and the lines of a list are
 * all labels. The text can be centred on the label or aligned with its left edge,
 * and it is always drawn with a shadow, which keeps it readable on top of the
 * panorama of the title screen.
 */
public class LabelWidget extends BaseWidget {

    private final PixelFont font;

    /** Text of the label, replaced by {@link #setText(String)}. */
    private String text;

    private final boolean centered;

    /** Colour of the text, copied from the default of the font. */
    private final Color color = new Color(WidgetStyle.TEXT_COLOR);

    /** Factor the text is drawn with, the shared font is left untouched. */
    private float scale = 1.0f;

    /**
     * Creates a label.
     *
     * @param font font used for the text
     * @param text text to draw
     * @param centered {@code true} to centre the text, {@code false} to align it left
     */
    public LabelWidget(PixelFont font, String text, boolean centered) {
        this.font = Objects.requireNonNull(font, "font");
        this.text = Objects.requireNonNull(text, "text");
        this.centered = centered;
    }

    /** Text of this label. */
    public String text() {
        return text;
    }

    /**
     * Replaces the text.
     *
     * @param text new text, {@code null} counts as empty
     */
    public void setText(String text) {
        this.text = text == null ? "" : text;
    }

    /** Colour of the text, may be changed by the screen. */
    public Color color() {
        return color;
    }

    /**
     * Sets the size of the text.
     * <p>
     * The font is shared by the whole game, so a label that wants larger text scales
     * it while drawing and restores it afterwards. That keeps a heading from
     * enlarging every other text on the screen.
     *
     * @param scale factor applied to the glyphs, must be positive
     */
    public void setScale(float scale) {
        if (scale <= 0.0f) {
            throw new IllegalArgumentException("Label scale must be positive: " + scale);
        }
        this.scale = scale;
    }

    /** Height of the text in interface pixels. */
    public float textHeight() {
        return font.glyphHeight() * scale;
    }

    @Override
    public void render(SpriteBatch batch, float mouseX, float mouseY) {
        if (!isVisible()) {
            return;
        }
        font.setColor(color);
        float previous = font.scale();
        font.setScale(previous * scale);
        float glyphHeight = font.glyphHeight();
        float textX = centered ? font.centeredX(text, x() + width() * 0.5f) : x();
        font.drawShadowed(batch, text, textX, y() + (height() + glyphHeight) * 0.5f);
        font.setScale(previous);
    }

    @Override
    public String toString() {
        return "LabelWidget('" + text + "')";
    }
}
