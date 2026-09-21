package com.philia093.neofactory.gui;

import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.render.BlockTextureCache;

/**
 * Geometry of the inventory screen and of the hotbar.
 * <p>
 * The container picture is no longer one piece: the panel, the slots and the arrow come
 * from {@code gui/inventory_icons.png} and are stretched to whatever size the slots ask
 * for, see {@link com.philia093.neofactory.gui.panel.PanelTextures}. This class only
 * says where the slots of the player lie, and {@link #of} hands the result to a
 * {@link ContainerLayout}, which grows the panel around them.
 * <p>
 * The screen shows what the player owns and what may be made of it: a two by two
 * crafting field with its result, the three storage rows and the hotbar. The armour
 * slots and the preview of the player of the original game are gone, the game has
 * neither armour nor a player to show yet.
 * <p>
 * <b>Storage slots:</b> the hotbar row comes first and the three storage rows follow,
 * which is exactly the order of {@link PlayerInventory}: slot {@code 0} to {@code 8} is
 * the hotbar and slot {@code 9} to {@code 35} is the storage. On screen the storage is
 * drawn above the hotbar, so the two blocks are added to the layout separately.
 * <p>
 * All coordinates are pixels of the panel, measured from its upper left corner
 * downwards.
 */
public final class InventoryLayout {

    /** X coordinate of the two by two crafting field. */
    public static final int CRAFT_X = 32;

    /** Side length of the clickable part of a slot in pixels, the size of an icon. */
    public static final int SLOT_SIZE = ContainerLayout.SLOT_SIZE;

    /** Y coordinate of the upper row of the crafting field. */
    public static final int CRAFT_Y = 22;

    /** Columns between the crafting field and its result, the arrow fills them. */
    public static final int RESULT_COLUMNS = 4;

    /** Y coordinate of the result slot, centred on the crafting field. */
    public static final int RESULT_Y = CRAFT_Y + ContainerLayout.SLOT_PITCH / 2;

    /** X coordinate of the crafting arrow. */
    public static final int ARROW_X = CRAFT_X + 2 * ContainerLayout.SLOT_PITCH + 2;

    /** Y coordinate of the crafting arrow, centred on the crafting field. */
    public static final int ARROW_Y = CRAFT_Y + ContainerLayout.SLOT_PITCH / 2;

    /** Y coordinate of the upper storage row of the three by nine grid. */
    public static final int STORAGE_Y = 74;

    /** Y coordinate of the hotbar row. */
    public static final int HOTBAR_Y = 132;

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
     * Builds the layout of the inventory screen.
     *
     * @param player inventory of the player, both the storage and the hotbar
     * @param crafting the two by two field of the crafting grid
     * @param result inventory that holds the current result of the crafting field
     * @return the layout, its panel is as large as the slots ask for
     */
    public static ContainerLayout of(PlayerInventory player, Inventory crafting, Inventory result) {
        ContainerLayout layout = new ContainerLayout();
        layout.addGrid(CRAFT_X, CRAFT_Y, 2, 2, crafting, 0, Slot.Rule.WORK);
        layout.add(CRAFT_X + RESULT_COLUMNS * ContainerLayout.SLOT_PITCH, RESULT_Y, result, 0,
                Slot.Rule.OUTPUT);
        layout.addGrid(ContainerLayout.PADDING, STORAGE_Y, 9, 3, player,
                PlayerInventory.HOTBAR_SLOTS, Slot.Rule.NORMAL);
        layout.addGrid(ContainerLayout.PADDING, HOTBAR_Y, 9, 1, player, 0, Slot.Rule.NORMAL);
        return layout;
    }

    /**
     * {@code true} when a slot of the player belongs to the hotbar row.
     *
     * @param slot slot index, {@code 0} to {@code 35}
     * @return {@code true} for the first nine slots
     */
    public static boolean isHotbarSlot(int slot) {
        return slot >= 0 && slot < PlayerInventory.HOTBAR_SLOTS;
    }
}
