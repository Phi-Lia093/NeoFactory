package com.philia093.neofactory.gui;

/**
 * Geometry shared by the menus of the game.
 * <p>
 * Every menu stacks buttons of the same size in the middle of the interface, which
 * is what the original game does and what makes the menus look like one family. The
 * numbers live here instead of in each screen, so a change of the button size does
 * not have to be repeated five times.
 * <p>
 * All values are interface pixels, the unit {@link GuiViewport} draws in.
 */
public final class MenuLayout {

    /** Width of a menu button in interface pixels. */
    public static final float BUTTON_WIDTH = 200.0f;

    /** Height of a menu button in interface pixels. */
    public static final float BUTTON_HEIGHT = 20.0f;

    /** Vertical gap between two buttons in interface pixels. */
    public static final float BUTTON_GAP = 4.0f;

    /**
     * Height of a button that has to share its screen with a list, in interface pixels.
     * <p>
     * A stack of five full sized buttons is taller than the world list menu can spare:
     * at a small interface the buttons would reach into the list and hide the worlds.
     * The list menus therefore use this shorter button, which keeps the whole menu
     * visible without hiding any of the buttons.
     */
    public static final float SHORT_BUTTON_HEIGHT = 15.0f;

    /** Vertical gap between two short buttons in interface pixels. */
    public static final float SHORT_BUTTON_GAP = 2.0f;

    /** Size the label of a short button is drawn with, so that it fits its flatter box. */
    public static final float SHORT_BUTTON_SCALE = SHORT_BUTTON_HEIGHT / BUTTON_HEIGHT;

    /** Width of a text field in interface pixels. */
    public static final float FIELD_WIDTH = 200.0f;

    /** Height of a text field in interface pixels. */
    public static final float FIELD_HEIGHT = 20.0f;

    /** Distance between a label and the widget below it in interface pixels. */
    public static final float LABEL_GAP = 6.0f;

    private MenuLayout() {
        // Utility class: never instantiated.
    }

    /**
     * X coordinate that centres a widget of a given width.
     *
     * @param interfaceWidth width of the interface in virtual pixels
     * @param widgetWidth width of the widget
     * @return the left edge of the widget
     */
    public static float centeredX(float interfaceWidth, float widgetWidth) {
        return Math.round((interfaceWidth - widgetWidth) * 0.5f);
    }

    /**
     * Total height of a stack of buttons.
     *
     * @param count amount of buttons in the stack
     * @return the height in interface pixels
     */
    public static float stackHeight(int count) {
        return stackHeight(count, BUTTON_HEIGHT, BUTTON_GAP);
    }

    /**
     * Total height of a stack of buttons of a given size.
     *
     * @param count amount of buttons in the stack
     * @param buttonHeight height of one button in interface pixels
     * @param gap vertical gap between two buttons in interface pixels
     * @return the height in interface pixels
     */
    public static float stackHeight(int count, float buttonHeight, float gap) {
        if (count <= 0) {
            return 0.0f;
        }
        return count * buttonHeight + (count - 1) * gap;
    }

    /**
     * Lower edge of one button of a stack.
     * <p>
     * The stack is centred on the given point and the buttons are counted from the
     * top, which is how the menus of the original game read.
     *
     * @param centerY Y coordinate the stack is centred on
     * @param index button index, {@code 0} is the topmost button
     * @param count amount of buttons in the stack
     * @return the lower edge of that button
     */
    public static float stackY(float centerY, int index, int count) {
        return stackY(centerY, index, count, BUTTON_HEIGHT, BUTTON_GAP);
    }

    /**
     * Lower edge of one button of a stack of a given size.
     *
     * @param centerY Y coordinate the stack is centred on
     * @param index button index, {@code 0} is the topmost button
     * @param count amount of buttons in the stack
     * @param buttonHeight height of one button in interface pixels
     * @param gap vertical gap between two buttons in interface pixels
     * @return the lower edge of that button
     */
    public static float stackY(float centerY, int index, int count, float buttonHeight, float gap) {
        float top = centerY + stackHeight(count, buttonHeight, gap) * 0.5f;
        return top - (index + 1) * buttonHeight - index * gap;
    }

    /**
     * Upper edge of a stack of buttons, the line below which a list has to end.
     *
     * @param centerY Y coordinate the stack is centred on
     * @param count amount of buttons in the stack
     * @param buttonHeight height of one button in interface pixels
     * @param gap vertical gap between two buttons in interface pixels
     * @return the upper edge of the topmost button
     */
    public static float stackTop(float centerY, int count, float buttonHeight, float gap) {
        return centerY + stackHeight(count, buttonHeight, gap) * 0.5f;
    }

    /**
     * Y coordinate of the label that belongs to a widget.
     *
     * @param widgetY lower edge of the widget
     * @param widgetHeight height of the widget
     * @param labelHeight height of the label
     * @return the lower edge of the label, which sits above the widget
     */
    public static float labelY(float widgetY, float widgetHeight, float labelHeight) {
        return widgetY + widgetHeight + LABEL_GAP - labelHeight * 0.5f;
    }
}
