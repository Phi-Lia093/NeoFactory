package com.philia093.neofactory.gui.widget;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * A piece of the user interface that draws itself and reacts to the mouse.
 * <p>
 * Widgets work in the virtual pixels of
 * {@link com.philia093.neofactory.gui.GuiViewport}, the same coordinate system the
 * interface pictures are drawn in. The screen owning a widget therefore passes the
 * mouse position it already unprojected, and a click lands where the player sees
 * the widget.
 * <p>
 * The interface is intentionally small: a screen lays its widgets out, draws them
 * and offers the mouse and keyboard events to them. Everything else - how a button
 * looks, whether a field is focused - belongs to the implementation.
 */
public interface Widget {

    /**
     * Places the widget.
     *
     * @param x left edge in interface pixels
     * @param y lower edge in interface pixels, the interface measures upwards
     * @param width width in interface pixels
     * @param height height in interface pixels
     */
    void layout(float x, float y, float width, float height);

    /** Left edge of the widget in interface pixels. */
    float x();

    /** Lower edge of the widget in interface pixels. */
    float y();

    /** Width of the widget in interface pixels. */
    float width();

    /** Height of the widget in interface pixels. */
    float height();

    /**
     * Draws the widget.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     */
    void render(SpriteBatch batch, float mouseX, float mouseY);

    /**
     * Advances the widget by one frame.
     *
     * @param delta time since the last frame in seconds
     */
    void update(float delta);

    /**
     * Handles a mouse button press.
     *
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     * @param button mouse button that was pressed
     * @return {@code true} when the press was consumed
     */
    boolean touchDown(float mouseX, float mouseY, int button);

    /**
     * Handles a mouse button release.
     *
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     * @param button mouse button that was released
     * @return {@code true} when the release was consumed
     */
    boolean touchUp(float mouseX, float mouseY, int button);

    /**
     * {@code true} when a point lies inside the widget.
     *
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     */
    boolean contains(float mouseX, float mouseY);

    /** {@code true} while the widget is usable. */
    boolean isEnabled();

    /**
     * Enables or disables the widget.
     *
     * @param enabled {@code false} to draw it grey and to ignore clicks
     */
    void setEnabled(boolean enabled);

    /** {@code true} while the widget is drawn. */
    boolean isVisible();

    /**
     * Shows or hides the widget.
     *
     * @param visible {@code false} to skip drawing and clicking
     */
    void setVisible(boolean visible);
}
