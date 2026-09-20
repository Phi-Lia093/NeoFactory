package com.philia093.neofactory.gui.widget;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.render.PixelFont;

import java.util.Objects;

/**
 * A field the player can type into, used to name a world or to enter a seed.
 * <p>
 * The field only holds text: it does not know how a screen manages focus, so the
 * screen decides which field receives the keys, see
 * {@link com.philia093.neofactory.screen.WidgetScreen}. Typing is limited by the
 * amount of characters the field may hold, which keeps a name that is too long for
 * the world list from being entered in the first place.
 * <p>
 * Enter submits the field, which is a short cut for pressing the button the field
 * belongs to.
 */
public class TextFieldWidget extends BaseWidget {

    /** Amount of characters a field may hold. */
    public static final int MAX_LENGTH = 32;

    /** Seconds the cursor stays visible, and hidden, before it blinks again. */
    private static final float BLINK_INTERVAL = 0.5f;

    /** Distance between the edge of the field and its text. */
    private static final float PADDING = 4.0f;

    private final PixelFont font;
    private final WidgetStyle style;
    private final StringBuilder text = new StringBuilder();

    /** Text shown greyed out while the field is empty. */
    private String hint = "";

    /** Focus decides whether the field takes keys and shows a cursor. */
    private boolean focused;

    /** Seconds since the field was focused, drives the blinking cursor. */
    private float elapsed;

    /** Action run when the player presses enter, may be {@code null}. */
    private Runnable onSubmit;

    /**
     * Creates a text field.
     *
     * @param font font used for the text
     * @param style pictures and colours of the widget sheet
     */
    public TextFieldWidget(PixelFont font, WidgetStyle style) {
        this.font = Objects.requireNonNull(font, "font");
        this.style = Objects.requireNonNull(style, "style");
    }

    /** Text the player typed. */
    public String text() {
        return text.toString();
    }

    /**
     * Replaces the text.
     *
     * @param value new text, cut to {@link #MAX_LENGTH} characters
     */
    public void setText(String value) {
        text.setLength(0);
        if (value != null) {
            appendFitting(value);
        }
    }

    /** Empties the field. */
    public void clear() {
        text.setLength(0);
    }

    /**
     * Sets the text shown while the field is empty.
     *
     * @param hint hint to show, may be {@code null}
     */
    public void setHint(String hint) {
        this.hint = hint == null ? "" : hint;
    }

    /** {@code true} while the field takes keyboard input. */
    public boolean isFocused() {
        return focused;
    }

    /**
     * Sets whether the field takes keyboard input.
     *
     * @param focused {@code true} to show the cursor and accept keys
     */
    public void setFocused(boolean focused) {
        this.focused = focused;
        this.elapsed = 0.0f;
    }

    /**
     * Sets the action run when the player presses enter.
     *
     * @param onSubmit action to run, may be {@code null}
     */
    public void setOnSubmit(Runnable onSubmit) {
        this.onSubmit = onSubmit;
    }

    @Override
    public void update(float delta) {
        if (focused) {
            elapsed += delta;
        }
    }

    @Override
    public void render(SpriteBatch batch, float mouseX, float mouseY) {
        if (!isVisible()) {
            return;
        }
        TextureRegion picture = style.field(focused);
        if (picture != null) {
            // A field is a plain box: it is never highlighted by hovering, only a
            // focused one is drawn brighter so the player sees which name is typed.
            batch.setColor(isEnabled()
                    ? (focused ? WidgetStyle.FIELD_FOCUSED_TINT : WidgetStyle.FIELD_TINT)
                    : WidgetStyle.DISABLED_TINT);
            batch.draw(picture, x(), y(), width(), height());
            batch.setColor(Color.WHITE);
        }

        float glyphHeight = font.glyphHeight();
        float textY = y() + (height() + glyphHeight) * 0.5f;
        float textX = x() + PADDING;
        boolean empty = text.length() == 0;
        font.setColor(empty ? WidgetStyle.FIELD_HINT_COLOR : WidgetStyle.FIELD_TEXT_COLOR);
        font.drawShadowed(batch, empty ? hint : text.toString(), textX, textY);

        if (focused && elapsed % (BLINK_INTERVAL * 2.0f) < BLINK_INTERVAL) {
            TextureRegion pixel = style.whitePixel();
            if (pixel != null) {
                float cursorX = textX + font.width(text.toString());
                batch.setColor(WidgetStyle.CURSOR_COLOR);
                batch.draw(pixel, cursorX, textY - glyphHeight, font.scale(), glyphHeight);
                batch.setColor(Color.WHITE);
            }
        }
    }

    @Override
    public boolean touchDown(float mouseX, float mouseY, int button) {
        return accepts(mouseX, mouseY);
    }

    /**
     * Handles a key that does not produce a character.
     *
     * @param keyCode key code, see {@link Input.Keys}
     * @return {@code true} when the key was consumed
     */
    public boolean handleKeyDown(int keyCode) {
        if (!focused || !isEnabled()) {
            return false;
        }
        if (keyCode == Input.Keys.BACKSPACE) {
            if (text.length() > 0) {
                text.setLength(text.length() - 1);
            }
            return true;
        }
        if (keyCode == Input.Keys.ENTER || keyCode == Input.Keys.NUMPAD_ENTER) {
            if (onSubmit != null) {
                onSubmit.run();
            }
            return true;
        }
        return false;
    }

    /**
     * Appends a typed character.
     *
     * @param character character the keyboard produced
     * @return {@code true} when the character was consumed
     */
    public boolean handleKeyTyped(char character) {
        if (!focused || !isEnabled()) {
            return false;
        }
        // Control characters are keys and not text, and a full field refuses more.
        if (character < ' ') {
            return true;
        }
        if (text.length() >= MAX_LENGTH) {
            return true;
        }
        appendFitting(String.valueOf(character));
        return true;
    }

    /**
     * Appends as much of a text as fits into the field.
     *
     * @param value text to append
     */
    private void appendFitting(String value) {
        for (int i = 0; i < value.length() && text.length() < MAX_LENGTH; i++) {
            char character = value.charAt(i);
            if (character >= ' ') {
                text.append(character);
            }
        }
    }

    @Override
    public String toString() {
        return "TextFieldWidget('" + text + "')";
    }
}
