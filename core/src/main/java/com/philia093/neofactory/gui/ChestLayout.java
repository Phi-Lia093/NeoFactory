package com.philia093.neofactory.gui;

import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.render.PixelFont;

/**
 * Geometry of the screen of a container: what lies where and how large its panel becomes.
 * <p>
 * The panel picture is not drawn for this screen: it is the very picture the inventory of the
 * player is drawn with, stretched in nine cells to whatever the slots ask for, see
 * {@link com.philia093.neofactory.gui.panel.PanelTextures}. Only the places of the slots live
 * here, which is what keeps the drawn panel and the rectangle a click can hit the same one - the
 * screen paints from these numbers and asks them what the mouse points at.
 * <p>
 * Coordinates are measured from the upper left corner of the panel with the Y axis pointing down,
 * the way the art is stored.
 * <p>
 * <b>The name of the container stands above its slots.</b> A machine writes its name into a corner
 * of a panel that was drawn with room for it; a container panel has none, so this layout keeps
 * {@link #TITLE_HEIGHT} pixels free at the top and the screen writes the name there, see
 * {@link ContainerGui#TITLE_TOP}.
 */
public final class ChestLayout {

    /** Amount of columns of the slots the container itself holds. */
    public static final int COLUMNS = 9;

    /** Amount of rows of the slots the container itself holds. */
    public static final int ROWS = 3;

    /**
     * Height of the band at the top of the panel that holds the name of the container.
     * <p>
     * The band has to hold the line of the font with the air above and below it and nothing of it may reach
     * the first row of slots: the name begins {@link ContainerGui#TITLE_TOP} pixels below the upper edge of
     * the panel and the line of the font is as tall as its cell, so the band is that much plus the air under
     * the name, see {@link #CONTAINER_Y}.
     */
    public static final int TITLE_HEIGHT = ContainerGui.TITLE_TOP + PixelFont.ASCII_CELL_SIZE + 2;

    /** Y coordinate of the first row of the container, the upper edge of an icon. */
    public static final int CONTAINER_Y = TITLE_HEIGHT;

    /** Distance between the last row of the container and the first row of the player. */
    public static final int GAP = 6;

    /** Distance between the last row of the storage of the player and the hotbar row. */
    public static final int HOTBAR_GAP = 4;

    /** Y coordinate of the upper storage row of the player, as {@link InventoryLayout} places it. */
    public static final int STORAGE_Y = CONTAINER_Y + ROWS * ContainerLayout.SLOT_PITCH + GAP;

    /** Y coordinate of the hotbar row of the player. */
    public static final int HOTBAR_Y = STORAGE_Y + 3 * ContainerLayout.SLOT_PITCH + HOTBAR_GAP;

    /** Amount of columns of the rows the player brings to the screen. */
    public static final int PLAYER_COLUMNS = 9;

    private ChestLayout() {
        // Utility class: never instantiated.
    }

    /**
     * Builds the layout of the screen of a container.
     * <p>
     * The slots of the container come first and the two blocks of the player follow, exactly the
     * way the inventory screen places them: the three storage rows above the hotbar row. The
     * paper the two of them lie on is the panel that grows around them, so the same layout serves
     * a chest of three rows and the taller screen of a later container.
     *
     * @param container slots the block holds
     * @param player inventory of the player, both the storage and the hotbar
     * @return the layout, its panel is as large as the slots ask for
     */
    public static ContainerLayout of(Inventory container, PlayerInventory player) {
        ContainerLayout layout = new ContainerLayout();
        layout.addGrid(ContainerLayout.PADDING, CONTAINER_Y, COLUMNS, ROWS, container, 0,
                Slot.Rule.NORMAL);
        layout.addGrid(ContainerLayout.PADDING, STORAGE_Y, PLAYER_COLUMNS, 3, player,
                PlayerInventory.HOTBAR_SLOTS, Slot.Rule.NORMAL);
        layout.addGrid(ContainerLayout.PADDING, HOTBAR_Y, PLAYER_COLUMNS, 1, player, 0,
                Slot.Rule.NORMAL);
        return layout;
    }
}
