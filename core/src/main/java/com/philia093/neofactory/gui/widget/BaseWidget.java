package com.philia093.neofactory.gui.widget;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Position, size and state shared by every widget.
 * <p>
 * The base class owns the rectangle of a widget and the two flags a screen asks
 * about. Widgets that only draw something, such as a label, need nothing else;
 * widgets that react to the mouse override the two touch methods.
 */
public abstract class BaseWidget implements Widget {

    private float x;
    private float y;
    private float width;
    private float height;
    private boolean enabled = true;
    private boolean visible = true;

    @Override
    public void layout(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public float x() {
        return x;
    }

    @Override
    public float y() {
        return y;
    }

    @Override
    public float width() {
        return width;
    }

    @Override
    public float height() {
        return height;
    }

    @Override
    public boolean contains(float mouseX, float mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean touchDown(float mouseX, float mouseY, int button) {
        // Most widgets do not react to a press on their own.
        return false;
    }

    @Override
    public boolean touchUp(float mouseX, float mouseY, int button) {
        // Most widgets do not react to a release on their own.
        return false;
    }

    @Override
    public void update(float delta) {
        // Nothing changes on its own.
    }

    /** {@code true} when the mouse is over the widget and it is usable. */
    protected boolean isHovered(float mouseX, float mouseY) {
        return enabled && visible && contains(mouseX, mouseY);
    }

    /** {@code true} when a touch reached this widget at all. */
    protected boolean accepts(float mouseX, float mouseY) {
        return enabled && visible && contains(mouseX, mouseY);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void render(SpriteBatch batch, float mouseX, float mouseY) {
        // Widgets without a picture draw nothing.
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + x + ", " + y + ", " + width + " x " + height + ")";
    }
}
