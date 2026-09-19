package com.philia093.neofactory.gui;

import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.render.BlockTextureCache;

/**
 * Geometry of the player inventory, measured pixel by pixel from the pictures of
 * the art pack.
 * <p>
 * Everything is in interface pixels, which are drawn one to one: a slot is 16
 * pixels wide and the next one follows 18 pixels later, so the two pixels between
 * two slots are the bevel the picture already brings along.
 * <p>
 * The container picture {@code gui/container/inventory.png} holds the frame, the
 * slot bevels, the player preview and the crafting arrow, so the screen only has
 * to draw the items on top of it. Every coordinate below is relative to the top
 * left corner of that picture.
 * <p>
 * <b>Storage slots:</b> the hotbar row comes first and the three storage rows
 * follow, which is exactly the order of {@link PlayerInventory}: slot {@code 0} to
 * {@code 8} is the hotbar and slot {@code 9} to {@code 35} is the storage.
 * <p>
 * The armour slots and the crafting group belong to the picture, but they neither
 * store items nor own a slot of the player inventory yet, so this class only
 * reports where they are.
 */
public final class InventoryLayout {

    /** Texture of the container frame, without extension. */
    public static final String CONTAINER_TEXTURE = BlockTextureCache.GUI_FOLDER + "container/inventory";

    /** Width of the container frame in pixels. */
    public static final int CONTAINER_WIDTH = 176;

    /** Height of the container frame in pixels. */
    public static final int CONTAINER_HEIGHT = 166;

    /** Side length of the clickable part of a slot in pixels. */
    public static final int SLOT_SIZE = 16;

    /** Distance between the left edges of two neighbouring slots in pixels. */
    public static final int SLOT_PITCH = 18;

    /** X coordinate of the single armour column. */
    public static final int ARMOR_COLUMN_X = 8;

    /** Y coordinate of the first armour slot, the helmet is on top. */
    public static final int FIRST_ARMOR_SLOT_Y = 8;

    /** Amount of armour slots, from the helmet down to the boots. */
    public static final int ARMOR_SLOT_COUNT = 4;

    /** X coordinate of the left column of the two by two crafting grid. */
    public static final int CRAFT_GRID_X = 88;

    /** Y coordinate of the upper row of the crafting grid. */
    public static final int CRAFT_GRID_Y = 26;

    /** X coordinate of the crafting result slot. */
    public static final int CRAFT_RESULT_X = 144;

    /** Y coordinate of the crafting result slot. */
    public static final int CRAFT_RESULT_Y = 36;

    /** X coordinate of the left column of the storage grid. */
    public static final int GRID_X = 8;

    /** Y coordinate of the upper storage row of the three by nine grid. */
    public static final int FIRST_GRID_Y = 84;

    /** Amount of columns of the storage grid and of the hotbar. */
    public static final int GRID_COLUMNS = 9;

    /** Amount of storage rows, the hotbar not counted. */
    public static final int GRID_ROWS = 3;

    /** Y coordinate of the hotbar row inside the container. */
    public static final int HOTBAR_ROW_Y = 142;

    /** Y coordinate of the label above the crafting grid. */
    public static final int CRAFTING_LABEL_Y = 6;

    /** Y coordinate of the label above the storage grid. */
    public static final int INVENTORY_LABEL_Y = 72;

    /** Texture of the hotbar widget, without extension. */
    public static final String HOTBAR_TEXTURE = BlockTextureCache.GUI_FOLDER + "widgets";

    /** Width of the hotbar background in pixels, it is {@code 2 + 9 * 20}. */
    public static final int HOTBAR_WIDTH = 182;

    /** Height of the hotbar background in pixels. */
    public static final int HOTBAR_HEIGHT = 22;

    /** Distance between the left edges of two hotbar slots in pixels. */
    public static final int HOTBAR_PITCH = 20;

    /** Offset of a hotbar icon inside the hotbar background in pixels. */
    public static final int HOTBAR_INSET = 3;

    /** X coordinate of the hotbar selection frame inside {@link #HOTBAR_TEXTURE}. */
    public static final int HOTBAR_SELECTION_X = 0;

    /** Y coordinate of the hotbar selection frame inside {@link #HOTBAR_TEXTURE}. */
    public static final int HOTBAR_SELECTION_Y = 22;

    /** Side length of the hotbar selection frame in pixels. */
    public static final int HOTBAR_SELECTION_SIZE = 24;

    private InventoryLayout() {
        // Utility class: never instantiated.
    }

    /**
     * X coordinate of a storage slot inside the container.
     *
     * @param slot slot index, {@code 0} to {@code 35}
     * @return the left edge in pixels
     */
    public static int slotX(int slot) {
        return GRID_X + columnOf(slot) * SLOT_PITCH;
    }

    /**
     * Y coordinate of a storage slot inside the container.
     *
     * @param slot slot index, {@code 0} to {@code 35}
     * @return the upper edge in pixels, the hotbar row lies below the storage grid
     */
    public static int slotY(int slot) {
        if (isHotbarSlot(slot)) {
            return HOTBAR_ROW_Y;
        }
        return FIRST_GRID_Y + (slot - GRID_COLUMNS) / GRID_COLUMNS * SLOT_PITCH;
    }

    /**
     * Column of a slot, counted from the left.
     *
     * @param slot slot index, {@code 0} to {@code 35}
     * @return the column, {@code 0} to {@code 8}
     */
    public static int columnOf(int slot) {
        return slot % GRID_COLUMNS;
    }

    /** {@code true} when a slot belongs to the hotbar row. */
    public static boolean isHotbarSlot(int slot) {
        return slot >= 0 && slot < GRID_COLUMNS;
    }

    /**
     * Y coordinate of an armour slot.
     *
     * @param index armour slot, {@code 0} is the helmet
     * @return the upper edge in pixels
     */
    public static int armorSlotY(int index) {
        return FIRST_ARMOR_SLOT_Y + index * SLOT_PITCH;
    }

    /**
     * X coordinate of a cell of the crafting grid.
     *
     * @param index cell, counted row by row from the top left
     * @return the left edge in pixels
     */
    public static int craftSlotX(int index) {
        return CRAFT_GRID_X + index % 2 * SLOT_PITCH;
    }

    /**
     * Y coordinate of a cell of the crafting grid.
     *
     * @param index cell, counted row by row from the top left
     * @return the upper edge in pixels
     */
    public static int craftSlotY(int index) {
        return CRAFT_GRID_Y + index / 2 * SLOT_PITCH;
    }

    /**
     * Finds the storage slot under a point of the container.
     * <p>
     * The two pixels of bevel between two slots belong to neither of them, which
     * is why a click right between two slots does nothing.
     *
     * @param localX X coordinate relative to the container
     * @param localY Y coordinate relative to the container
     * @return the slot index, or {@code -1} when no slot is hit
     */
    public static int slotAt(int localX, int localY) {
        int column = columnAt(localX);
        if (column < 0) {
            return -1;
        }
        if (contains(localY, HOTBAR_ROW_Y)) {
            return column;
        }
        for (int row = 0; row < GRID_ROWS; row++) {
            if (contains(localY, FIRST_GRID_Y + row * SLOT_PITCH)) {
                return GRID_COLUMNS * (row + 1) + column;
            }
        }
        return -1;
    }

    /** Column under a point, {@code -1} when the point is not on a slot column. */
    private static int columnAt(int localX) {
        int offset = localX - GRID_X;
        if (offset < 0) {
            return -1;
        }
        int column = offset / SLOT_PITCH;
        if (column >= GRID_COLUMNS) {
            return -1;
        }
        return offset < column * SLOT_PITCH + SLOT_SIZE ? column : -1;
    }

    /** {@code true} when a coordinate lies inside the slot that starts at {@code start}. */
    private static boolean contains(int value, int start) {
        return value >= start && value < start + SLOT_SIZE;
    }
}
