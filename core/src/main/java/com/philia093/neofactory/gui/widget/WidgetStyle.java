package com.philia093.neofactory.gui.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.render.BlockTextureCache;

import java.util.Objects;

/**
 * The pictures and colours the widgets are drawn with.
 * <p>
 * Buttons and text fields are cut out of {@code gui/widgets.png}, the sheet the
 * original game uses: every widget is a 200 by 20 rectangle, and the rows hold the
 * states a widget can be in. Using the sheet instead of drawing coloured
 * rectangles keeps the menu looking like the rest of the interface.
 * <p>
 * The colours are not baked into the pictures: a disabled button is the same
 * picture drawn with a grey tint, which is what lets a widget be greyed out
 * without a second set of images.
 */
public final class WidgetStyle {

    /** Width of every widget in the sheet. */
    public static final int WIDGET_WIDTH = 200;

    /** Height of every widget in the sheet. */
    public static final int WIDGET_HEIGHT = 20;

    /** Colour of a normal widget picture. */
    public static final Color NORMAL_TINT = new Color(1.0f, 1.0f, 1.0f, 1.0f);

    /** Colour of a hovered widget picture, slightly brighter. */
    public static final Color HOVER_TINT = new Color(1.0f, 1.0f, 1.0f, 1.0f);

    /** Colour of a disabled widget picture. */
    public static final Color DISABLED_TINT = new Color(0.45f, 0.45f, 0.45f, 1.0f);

    /** Colour a text field is drawn with while it is not focused. */
    public static final Color FIELD_TINT = new Color(0.8f, 0.8f, 0.8f, 1.0f);

    /** Colour a focused text field is drawn with, a little brighter than a plain one. */
    public static final Color FIELD_FOCUSED_TINT = new Color(1.0f, 1.0f, 1.0f, 1.0f);

    /** Colour of the text on a widget. */
    public static final Color TEXT_COLOR = new Color(0.88f, 0.88f, 0.88f, 1.0f);

    /** Colour of the text on a disabled widget. */
    public static final Color DISABLED_TEXT_COLOR = new Color(0.45f, 0.45f, 0.45f, 1.0f);

    /** Colour of the text inside a text field, which the player typed. */
    public static final Color FIELD_TEXT_COLOR = new Color(0.88f, 0.88f, 0.88f, 1.0f);

    /** Colour of the hint shown in an empty text field. */
    public static final Color FIELD_HINT_COLOR = new Color(0.55f, 0.55f, 0.55f, 1.0f);

    /** Colour of the cursor of a text field. */
    public static final Color CURSOR_COLOR = new Color(0.9f, 0.9f, 0.9f, 1.0f);

    /** Colour of the text of the selected entry of a list. */
    public static final Color SELECTED_TEXT_COLOR = new Color(1.0f, 1.0f, 0.6f, 1.0f);

    /** Colour of the second line of a list entry, for example the date. */
    public static final Color SECONDARY_TEXT_COLOR = new Color(0.65f, 0.65f, 0.65f, 1.0f);

    /** Colour of the frame drawn around a list entry that is hovered. */
    public static final Color HOVER_FRAME_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.35f);

    /** Colour of the frame drawn around the selected entry of a list. */
    public static final Color SELECTION_FRAME_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.7f);

    // The rows of gui/widgets.png are not all buttons. The sheet starts with the hotbar
    // and the hotbar selector, and after the three button rows it holds the icons of the
    // interface - y 106 for example is the language button, a blue and white globe. Only
    // the rows listed here are buttons: cutting any other row draws the wrong picture,
    // which is why the number of rows is kept short instead of being guessed.

    /** Y coordinate of the flat, dark row the sheet draws for a disabled button. */
    private static final int FLAT_BOX_Y = 46;

    /** Y coordinate of the normal button row in the sheet. */
    private static final int BUTTON_Y = 66;

    /** Y coordinate of the highlighted button row in the sheet. */
    private static final int BUTTON_HIGHLIGHTED_Y = 86;

    private final TextureRegion buttons;

    /** Flat, dark box of the sheet, {@code null} when the picture is missing. */
    private final TextureRegion flatBox;

    private final BlockTextureCache textures;

    private TextureRegion buttonNormal;
    private TextureRegion buttonHighlighted;

    /**
     * Creates a style.
     *
     * @param textures texture cache providing {@code gui/widgets.png}
     */
    public WidgetStyle(BlockTextureCache textures) {
        this.textures = Objects.requireNonNull(textures, "textures");
        this.buttons = textures.region(BlockTextureCache.GUI_FOLDER + "widgets");
        if (buttons != null) {
            flatBox = new TextureRegion(buttons, 0, FLAT_BOX_Y, WIDGET_WIDTH, WIDGET_HEIGHT);
            buttonNormal = new TextureRegion(buttons, 0, BUTTON_Y, WIDGET_WIDTH, WIDGET_HEIGHT);
            buttonHighlighted = new TextureRegion(buttons, 0, BUTTON_HIGHLIGHTED_Y, WIDGET_WIDTH,
                    WIDGET_HEIGHT);
        } else {
            flatBox = null;
        }
    }

    /**
     * Returns the picture of a button.
     * <p>
     * There are only two pictures to pick from: the plain one and the highlighted one,
     * which the sheet brightens with a blue tint. A button that is held down uses the
     * highlighted picture too, which is what the original game does - it has no picture
     * of its own for that state.
     *
     * @param hovered {@code true} while the mouse is over it
     * @param pressed {@code true} while it is held down
     * @param enabled {@code false} for a greyed out button
     * @return the picture, or {@code null} when the sheet is missing
     */
    public TextureRegion button(boolean hovered, boolean pressed, boolean enabled) {
        return hovered || pressed ? buttonHighlighted : buttonNormal;
    }

    /**
     * Returns the picture of a text field.
     * <p>
     * The sheet has no box of its own for a field that is dark enough to read text on,
     * so the flat box it draws for a disabled button is reused: the same shape a button
     * has, only flat, which is what a field of the original game looks like as well. A
     * focused field is the same picture drawn brighter, see {@link #FIELD_FOCUSED_TINT}.
     *
     * @param focused {@code true} while the field takes keyboard input
     * @return the picture, or {@code null} when the sheet is missing
     */
    public TextureRegion field(boolean focused) {
        return flatBox;
    }

    /**
     * Returns a single white pixel.
     * <p>
     * Widgets stretch it into plain rectangles and one pixel wide lines, for example
     * the blinking cursor of a text field or the frame of a selected list entry.
     *
     * @return the pixel, or {@code null} when it could not be created
     */
    public TextureRegion whitePixel() {
        return textures.whitePixel();
    }

    /** {@code true} when the pictures of the sheet could be cut out. */
    public boolean isComplete() {
        return buttonNormal != null && buttonHighlighted != null && flatBox != null;
    }
}
