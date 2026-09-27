package com.philia093.neofactory.gui;

import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.recipe.CraftingField;

/**
 * Geometry of the screen of the table of the workshop.
 * <p>
 * The field of nine cells stands in the upper left corner of the panel, the arrow and the result it
 * makes lie beside it, and the rows of the player follow below them - the very rows the screen of a
 * container uses, so the two screens read as one family and a player always knows where their own
 * inventory is. The panel is drawn with the picture of the interface stretched in nine cells, exactly
 * like the screen of a chest, see {@link ChestLayout}.
 * <p>
 * <b>Every cell of the field is a plain slot of the block.</b> What lies on the table belongs to the
 * block and not to the moment the screen is open, so a pattern is still lying there when the player
 * comes back - which is the one thing that tells this field from the two by two field of the player,
 * where what is left is thrown on the ground when the screen closes, see {@link Slot.Rule#WORK}.
 * Coordinates are measured from the upper left corner of the panel with the Y axis pointing down.
 */
public final class CraftingLayout {

    /** Height of the band at the top of the panel that holds the name of the table. */
    public static final int TITLE_HEIGHT = ChestLayout.TITLE_HEIGHT;

    /** X coordinate of the first column of the field, the left edge of an icon. */
    public static final int FIELD_X = ContainerLayout.PADDING;

    /** Y coordinate of the first row of the field, the upper edge of an icon. */
    public static final int FIELD_Y = TITLE_HEIGHT;

    /** Distance between the arrow and the result slot. */
    public static final int RESULT_GAP = 6;

    /**
     * X coordinate of the arrow that points from the field to its result.
     * <p>
     * The field ends at {@link #FIELD_X} plus three columns, and the arrow and the result together are
     * centred in the paper that is left between the field and the frame of the panel.
     */
    public static final int ARROW_X = 91;

    /** X coordinate of the result slot, right of the arrow. */
    public static final int RESULT_X = ARROW_X + PanelTextures.ARROW_WIDTH + RESULT_GAP;

    /** Y coordinate of the arrow, centred on the middle row of the field. */
    public static final int ARROW_Y = FIELD_Y + ContainerLayout.SLOT_PITCH;

    /** Y coordinate of the result slot, centred on the arrow. */
    public static final int RESULT_Y = ARROW_Y;

    /** Distance between the last row of the field and the first row of the player. */
    public static final int GAP = ChestLayout.GAP;

    /** Y coordinate of the upper storage row of the player. */
    public static final int STORAGE_Y = FIELD_Y + CraftingField.ROWS * ContainerLayout.SLOT_PITCH + GAP;

    /** Y coordinate of the hotbar row of the player. */
    public static final int HOTBAR_Y = STORAGE_Y + 3 * ContainerLayout.SLOT_PITCH + ChestLayout.HOTBAR_GAP;

    private CraftingLayout() {
        // Utility class: never instantiated.
    }

    /**
     * Builds the layout of the screen of a table.
     *
     * @param result one slot holding what the field makes
     * @param contents the nine cells of the table, owned by its block
     * @param player inventory of the player, both the storage and the hotbar
     * @return the layout, its panel is as large as the slots ask for
     */
    public static ContainerLayout of(Inventory result, Inventory contents, PlayerInventory player) {
        ContainerLayout layout = new ContainerLayout();
        layout.addGrid(FIELD_X, FIELD_Y, CraftingField.COLUMNS, CraftingField.ROWS, contents, 0,
                Slot.Rule.NORMAL);
        layout.add(RESULT_X, RESULT_Y, result, 0, Slot.Rule.OUTPUT);
        layout.addGrid(ContainerLayout.PADDING, STORAGE_Y, ChestLayout.PLAYER_COLUMNS, 3, player,
                PlayerInventory.HOTBAR_SLOTS, Slot.Rule.NORMAL);
        layout.addGrid(ContainerLayout.PADDING, HOTBAR_Y, ChestLayout.PLAYER_COLUMNS, 1, player, 0,
                Slot.Rule.NORMAL);
        return layout;
    }
}
