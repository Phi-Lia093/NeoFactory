package com.philia093.neofactory.gui.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.render.PixelFont;

import java.util.Objects;

/**
 * A button of the interface.
 * <p>
 * The picture comes from {@code gui/widgets.png} and is stretched to the size the
 * screen gave the button, so a button may be as wide as the menu needs it without
 * shipping another picture. The label is centred on the button, which is why the
 * button measures it with {@link PixelFont#centeredX(String, float)} instead of
 * trusting a fixed width.
 * <p>
 * A button runs when the mouse is released over it, the way the original game
 * behaves: pressing inside and releasing outside cancels the click.
 */
public class ButtonWidget extends BaseWidget {

    private final PixelFont font;
    private final WidgetStyle style;

    /** Text shown on the button, replaced by {@link #setLabel(String)}. */
    private String label;

    private final Runnable action;

    /** {@code true} while the button is held down. */
    private boolean pressed;

    /** Factor the label is drawn with, see {@link #setScale(float)}. */
    private float scale = 1.0f;

    /**
     * Creates a button.
     *
     * @param font font used for the label
     * @param style pictures and colours of the widget sheet
     * @param label text shown on the button
     * @param action action to run when the button is clicked, may be {@code null}
     */
    public ButtonWidget(PixelFont font, WidgetStyle style, String label, Runnable action) {
        this.font = Objects.requireNonNull(font, "font");
        this.style = Objects.requireNonNull(style, "style");
        this.label = Objects.requireNonNull(label, "label");
        this.action = action;
    }

    /** Text shown on the button. */
    public String label() {
        return label;
    }

    /**
     * Replaces the text shown on the button.
     *
     * @param label new text, {@code null} counts as empty
     */
    public void setLabel(String label) {
        this.label = label == null ? "" : label;
    }

    /** {@code true} while the button is held down. */
    public boolean isPressed() {
        return pressed;
    }

    /**
     * Sets the size of the label.
     * <p>
     * A button that a screen makes smaller than the picture in the sheet needs a
     * smaller label as well, or the text no longer fits between its edges. The font is
     * shared by the whole game, so the button scales it while drawing and restores it
     * afterwards, exactly like {@link LabelWidget#setScale(float)} does.
     *
     * @param scale factor applied to the glyphs, must be positive
     */
    public void setScale(float scale) {
        if (scale <= 0.0f) {
            throw new IllegalArgumentException("Button scale must be positive: " + scale);
        }
        this.scale = scale;
    }

    /** Factor the label is drawn with. */
    public float scale() {
        return scale;
    }

    @Override
    public void render(SpriteBatch batch, float mouseX, float mouseY) {
        if (!isVisible()) {
            return;
        }
        boolean hovered = isHovered(mouseX, mouseY);
        TextureRegion picture = style.button(hovered, pressed, isEnabled());
        // A held button sinks by one pixel, which is how the original game shows a press.
        float sink = pressed ? -1.0f : 0.0f;
        if (picture != null) {
            batch.setColor(isEnabled()
                    ? (hovered ? WidgetStyle.HOVER_TINT : WidgetStyle.NORMAL_TINT)
                    : WidgetStyle.DISABLED_TINT);
            batch.draw(picture, x(), y() + sink, width(), height());
            batch.setColor(Color.WHITE);
        }

        font.setColor(isEnabled() ? WidgetStyle.TEXT_COLOR : WidgetStyle.DISABLED_TEXT_COLOR);
        float previous = font.scale();
        font.setScale(previous * scale);
        float glyphHeight = font.glyphHeight();
        font.drawShadowed(batch, label,
                font.centeredX(label, x() + width() * 0.5f),
                y() + sink + (height() + glyphHeight) * 0.5f);
        font.setScale(previous);
    }

    @Override
    public boolean touchDown(float mouseX, float mouseY, int button) {
        if (!accepts(mouseX, mouseY)) {
            return false;
        }
        pressed = true;
        return true;
    }

    @Override
    public boolean touchUp(float mouseX, float mouseY, int button) {
        if (!pressed) {
            return false;
        }
        boolean completed = accepts(mouseX, mouseY);
        pressed = false;
        if (completed && action != null) {
            action.run();
        }
        return true;
    }

    @Override
    public String toString() {
        return "ButtonWidget('" + label + "')";
    }
}
