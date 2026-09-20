package com.philia093.neofactory.gui.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.render.PixelFont;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A scrollable list of rows.
 * <p>
 * The list owns the scrolling, the selection and the frames around a row; what a
 * row shows is decided by its {@link ListEntry}, see
 * {@link com.philia093.neofactory.screen.WorldSelectScreen} for an example.
 * <p>
 * Rows that lie outside the box of the list are skipped instead of being clipped,
 * which is enough because the list is drawn as a plain rectangle without a
 * background picture of its own.
 */
public class ScrollListWidget extends BaseWidget {

    /** Seconds within which a second click counts as a double click. */
    private static final float DOUBLE_CLICK_TIME = 0.4f;

    /** Height of a single row in interface pixels. */
    private static final float ROW_HEIGHT = 36.0f;

    /** Thickness of the frame drawn around a row. */
    private static final float FRAME = 1.0f;

    private final PixelFont font;
    private final WidgetStyle style;
    private final List<ListEntry> entries = new ArrayList<>();

    /** Amount the list is scrolled down, in interface pixels. */
    private float scroll;

    /** Index of the selected row, {@code -1} while nothing is selected. */
    private int selected = -1;

    /** Seconds since the last click, used to spot a double click. */
    private float sinceClick = Float.MAX_VALUE;

    /** {@code true} when the last press was the second click of a double click. */
    private boolean doubleClick;

    /** Action run when a row is activated by a double click. */
    private Runnable onActivate;

    /**
     * Creates a list.
     *
     * @param font font the rows draw their text with
     * @param style pictures and colours of the widget sheet
     */
    public ScrollListWidget(PixelFont font, WidgetStyle style) {
        this.font = Objects.requireNonNull(font, "font");
        this.style = Objects.requireNonNull(style, "style");
    }

    /**
     * Replaces the rows.
     * <p>
     * A selection that points outside the new list is dropped, so a screen never
     * holds a selection that refers to nothing.
     *
     * @param newEntries rows to show
     */
    public void setEntries(List<? extends ListEntry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        if (selected >= entries.size()) {
            selected = entries.isEmpty() ? -1 : entries.size() - 1;
        }
        clampScroll();
    }

    /** Rows of this list, unmodifiable. */
    public List<ListEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    /** {@code true} while the list holds no row. */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Index of the selected row, {@code -1} while nothing is selected. */
    public int selectedIndex() {
        return selected;
    }

    /**
     * Selects a row.
     *
     * @param index row to select, a value outside the list clears the selection
     */
    public void setSelectedIndex(int index) {
        selected = index >= 0 && index < entries.size() ? index : -1;
    }

    /** Row that is currently selected, {@code null} while nothing is selected. */
    public ListEntry selectedEntry() {
        return selected >= 0 && selected < entries.size() ? entries.get(selected) : null;
    }

    /**
     * Sets the action run when a row is activated.
     *
     * @param onActivate action to run, may be {@code null}
     */
    public void setOnActivate(Runnable onActivate) {
        this.onActivate = onActivate;
    }

    /** Amount the list is scrolled down, in interface pixels. */
    public float scroll() {
        return scroll;
    }

    /** Height the rows need in total, which may be more than the list shows. */
    public float contentHeight() {
        return entries.size() * ROW_HEIGHT;
    }

    /**
     * Scrolls the list.
     *
     * @param amount pixels to scroll, positive scrolls down
     */
    public void scrollBy(float amount) {
        scroll += amount;
        clampScroll();
    }

    /**
     * Scrolls the list by a number of wheel notches.
     *
     * @param notches notches to scroll, positive scrolls down
     */
    public void scroll(float notches) {
        scrollBy(notches * ROW_HEIGHT);
    }

    @Override
    public void update(float delta) {
        sinceClick += delta;
    }

    @Override
    public void render(SpriteBatch batch, float mouseX, float mouseY) {
        if (!isVisible()) {
            return;
        }
        for (int index = 0; index < entries.size(); index++) {
            float rowTop = y() + height() - (index * ROW_HEIGHT - scroll) - ROW_HEIGHT;
            if (rowTop + ROW_HEIGHT <= y() || rowTop >= y() + height()) {
                // The row lies outside the box of the list.
                continue;
            }
            boolean hovered = isHovered(mouseX, mouseY) && rowAt(mouseX, mouseY) == index;
            boolean isSelected = index == selected;
            if (isSelected || hovered) {
                drawFrame(batch, rowTop, isSelected ? WidgetStyle.SELECTION_FRAME_COLOR
                        : WidgetStyle.HOVER_FRAME_COLOR);
            }
            entries.get(index).render(batch, font, x() + FRAME, rowTop + FRAME,
                    width() - FRAME * 2.0f, ROW_HEIGHT - FRAME * 2.0f, hovered, isSelected);
        }
    }

    /**
     * Draws the frame around a row.
     *
     * @param batch batch to draw into
     * @param rowTop upper edge of the row
     * @param color colour of the frame
     */
    private void drawFrame(SpriteBatch batch, float rowTop, Color color) {
        TextureRegion pixel = style.whitePixel();
        if (pixel == null) {
            return;
        }
        batch.setColor(color);
        float bottom = rowTop + ROW_HEIGHT - FRAME;
        batch.draw(pixel, x(), rowTop, width(), FRAME);
        batch.draw(pixel, x(), bottom, width(), FRAME);
        batch.draw(pixel, x(), rowTop, FRAME, ROW_HEIGHT);
        batch.draw(pixel, x() + width() - FRAME, rowTop, FRAME, ROW_HEIGHT);
        batch.setColor(Color.WHITE);
    }

    @Override
    public boolean touchDown(float mouseX, float mouseY, int button) {
        if (!accepts(mouseX, mouseY)) {
            return false;
        }
        int row = rowAt(mouseX, mouseY);
        if (row < 0) {
            return true;
        }
        doubleClick = sinceClick <= DOUBLE_CLICK_TIME && row == selected;
        sinceClick = 0.0f;
        setSelectedIndex(row);
        if (doubleClick && onActivate != null) {
            onActivate.run();
        }
        return true;
    }

    /** {@code true} when the last press was the second click of a double click. */
    public boolean wasDoubleClick() {
        return doubleClick;
    }

    /**
     * Row under a point.
     *
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     * @return the row index, or {@code -1} when the point lies beside the rows
     */
    public int rowAt(float mouseX, float mouseY) {
        if (!contains(mouseX, mouseY)) {
            return -1;
        }
        float offset = (y() + height()) - mouseY + scroll;
        int row = (int) (offset / ROW_HEIGHT);
        return row >= 0 && row < entries.size() ? row : -1;
    }

    /** Keeps the scrolled part inside the rows that exist. */
    private void clampScroll() {
        float maximum = Math.max(0.0f, contentHeight() - height());
        scroll = Math.max(0.0f, Math.min(scroll, maximum));
    }

    @Override
    public String toString() {
        return "ScrollListWidget(" + entries.size() + " rows, selected " + selected + ")";
    }
}
