package com.philia093.neofactory.gui.container;

import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Where the slots of a container lie and how large its panel therefore has to be.
 * <p>
 * A container describes itself: the caller places its slots - a grid of the player
 * inventory, the input of a machine, the result of a recipe - and the panel grows
 * around them. That is what lets one panel picture serve every screen and what keeps
 * the drawing code free of arithmetic, see
 * {@link com.philia093.neofactory.gui.panel.PanelTextures#drawPanel}.
 * <p>
 * Coordinates are measured from the upper left corner of the panel with the Y axis
 * pointing down, so a layout reads like the picture it describes.
 */
public final class ContainerLayout {

    /** Distance between the frame of the panel and the outermost slot in pixels. */
    public static final int PADDING = 8;

    /** Distance between the left edges of two neighbouring slots in pixels. */
    public static final int SLOT_PITCH = 18;

    /** Side length of the clickable part of a slot, the size of an item icon. */
    public static final int SLOT_SIZE = Constants.ITEM_ICON_SIZE;

    private final List<Slot> slots = new ArrayList<>();

    /**
     * Adds a single slot.
     *
     * @param x left edge of the cell, relative to the panel
     * @param y upper edge of the cell, relative to the panel
     * @param inventory inventory the slot shows
     * @param index index of the shown slot inside that inventory
     * @param rule what may happen to the items in this slot
     * @return the added slot
     */
    public Slot add(int x, int y, Inventory inventory, int index, Slot.Rule rule) {
        Slot slot = new Slot(x, y, inventory, index, rule);
        slots.add(slot);
        return slot;
    }

    /**
     * Adds a rectangular block of slots, counted row by row from the upper left one.
     *
     * @param x left edge of the first column, relative to the panel
     * @param y upper edge of the first row, relative to the panel
     * @param columns amount of columns
     * @param rows amount of rows
     * @param inventory inventory the slots show
     * @param firstIndex index of the slot in the upper left corner
     * @param rule what may happen to the items in these slots
     */
    public void addGrid(int x, int y, int columns, int rows, Inventory inventory, int firstIndex,
            Slot.Rule rule) {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                add(x + column * SLOT_PITCH, y + row * SLOT_PITCH, inventory,
                        firstIndex + row * columns + column, rule);
            }
        }
    }

    /** Every slot of this container, in the order they were added. */
    public List<Slot> slots() {
        return Collections.unmodifiableList(slots);
    }

    /**
     * Width the panel needs for the slots that were added.
     * <p>
     * The value is the right edge of the rightmost slot plus {@link #PADDING}, or two
     * paddings for a container without a slot, which keeps an empty panel visible.
     *
     * @return the width in pixels
     */
    public int panelWidth() {
        int width = 2 * PADDING;
        for (Slot slot : slots) {
            width = Math.max(width, slot.x() + SLOT_SIZE + PADDING);
        }
        return width;
    }

    /**
     * Height the panel needs for the slots that were added.
     *
     * @return the height in pixels
     */
    public int panelHeight() {
        int height = 2 * PADDING;
        for (Slot slot : slots) {
            height = Math.max(height, slot.y() + SLOT_SIZE + PADDING);
        }
        return height;
    }

    /**
     * {@code true} when a point lies on the panel of this container.
     * <p>
     * A click that lands outside keeps the container from seeing it, which is what turns
     * such a click into the action of throwing the carried stack away, see
     * {@link ContainerMenu#dropCursor()}.
     *
     * @param localX X coordinate relative to the panel
     * @param localY Y coordinate relative to the panel, measured downwards
     * @return {@code true} when the point is inside the frame of the panel
     */
    public boolean contains(int localX, int localY) {
        return localX >= 0 && localX < panelWidth() && localY >= 0 && localY < panelHeight();
    }

    /**
     * Slot under a point of the panel.
     * <p>
     * The two pixels of bevel between two cells belong to neither of them, which is
     * why a click right between two slots does nothing.
     *
     * @param localX X coordinate relative to the panel
     * @param localY Y coordinate relative to the panel, measured downwards
     * @return the slot, or {@code null} when the point is beside every slot
     */
    public Slot slotAt(int localX, int localY) {
        for (Slot slot : slots) {
            if (slot.contains(localX, localY)) {
                return slot;
            }
        }
        return null;
    }

    /** Amount of slots of this container. */
    public int size() {
        return slots.size();
    }

    @Override
    public String toString() {
        return "ContainerLayout(" + size() + " slots, " + panelWidth() + " x " + panelHeight() + ")";
    }
}
