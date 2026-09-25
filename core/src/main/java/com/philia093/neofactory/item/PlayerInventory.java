package com.philia093.neofactory.item;

/**
 * The inventory of the player.
 * <p>
 * Slot {@code 0} to {@code 8} form the hotbar, the row that stays visible at the
 * bottom of the screen, and slot {@code 9} to {@code 35} are the storage rows
 * shown by the inventory screen. That order is the same order the slots are
 * drawn in, which keeps the mapping between a clicked slot and its index simple.
 * <p>
 * The two by two crafting grid of the inventory screen is not part of this inventory:
 * it is drawn and it may hold something while the screen is open, but it stores
 * nothing when the screen closes, see {@link com.philia093.neofactory.gui.InventoryLayout}.
 */
public final class PlayerInventory extends Inventory {

    /** Amount of slots of the hotbar row. */
    public static final int HOTBAR_SLOTS = 9;

    /** Amount of slots of the three storage rows. */
    public static final int MAIN_SLOTS = 27;

    /** Amount of storage rows the inventory screen shows. */
    public static final int STORAGE_ROWS = 3;

    /** Amount of slots of a player inventory. */
    public static final int SLOT_COUNT = HOTBAR_SLOTS + MAIN_SLOTS;

    /** Slot the player selected, from {@code 0} to {@code HOTBAR_SLOTS - 1}. */
    private int selectedSlot;

    /** Creates an empty inventory with the hotbar on slot zero. */
    public PlayerInventory() {
        super(SLOT_COUNT);
    }

    /** Slot that is currently used, for example by a future block placing action. */
    public int selectedSlot() {
        return selectedSlot;
    }

    /**
     * Selects a hotbar slot.
     *
     * @param slot requested slot, wrapped into the hotbar range so that walking
     *             past the last slot continues at the first one
     */
    public void setSelectedSlot(int slot) {
        selectedSlot = Math.floorMod(slot, HOTBAR_SLOTS);
    }

    /**
     * Moves the selection by a number of slots.
     *
     * @param steps amount of slots to move, may be negative
     */
    public void scrollSelection(int steps) {
        setSelectedSlot(selectedSlot + steps);
    }

    /**
     * {@code true} when a slot belongs to the hotbar row.
     *
     * @param slot slot index, {@code 0 <= slot < size()}
     */
    public boolean isHotbarSlot(int slot) {
        return slot >= 0 && slot < HOTBAR_SLOTS;
    }

    /** Stack the player currently holds, {@link ItemStack#EMPTY} when the slot is empty. */
    public ItemStack heldStack() {
        return get(selectedSlot);
    }

    @Override
    public String toString() {
        return "PlayerInventory(selected=" + selectedSlot + ", items=" + storedItemCount() + ")";
    }
}
