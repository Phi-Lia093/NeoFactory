package com.philia093.neofactory.item;

import java.util.Arrays;

/**
 * A fixed number of slots that hold {@link ItemStack} instances.
 * <p>
 * The class only stores items, it does not know how a slot looks on screen.
 * Empty slots hold {@link ItemStack#EMPTY}, so a caller never has to test for
 * {@code null}. The player inventory is the only inventory of the game so far,
 * see {@link PlayerInventory}; the class is kept open for chests and machines
 * that will be added later.
 */
public class Inventory {

    private final ItemStack[] slots;

    /**
     * Creates an empty inventory.
     *
     * @param size amount of slots, must be positive
     */
    public Inventory(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Inventory size must be positive: " + size);
        }
        this.slots = new ItemStack[size];
        Arrays.fill(slots, ItemStack.EMPTY);
    }

    /** Amount of slots of this inventory. */
    public int size() {
        return slots.length;
    }

    /**
     * Returns the stack stored in a slot.
     *
     * @param slot slot index, {@code 0 <= slot < size()}
     * @return the stored stack, {@link ItemStack#EMPTY} when the slot is unused
     */
    public ItemStack get(int slot) {
        checkSlot(slot);
        return slots[slot];
    }

    /**
     * Writes a stack into a slot.
     *
     * @param slot slot index, {@code 0 <= slot < size()}
     * @param stack stack to store, {@code null} and empty stacks clear the slot
     */
    public void set(int slot, ItemStack stack) {
        checkSlot(slot);
        slots[slot] = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    /**
     * Takes the stack out of a slot and clears it.
     *
     * @param slot slot index, {@code 0 <= slot < size()}
     * @return the removed stack, {@link ItemStack#EMPTY} when the slot was empty
     */
    public ItemStack remove(int slot) {
        ItemStack stack = get(slot);
        slots[slot] = ItemStack.EMPTY;
        return stack;
    }

    /** {@code true} when no slot holds an item. */
    public boolean isEmpty() {
        for (ItemStack stack : slots) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** {@code true} when no slot can take another item stack. */
    public boolean isFull() {
        return firstEmptySlot() < 0;
    }

    /**
     * {@code true} when a stack of the given kind still fits somewhere.
     * <p>
     * A stack may be topped up on one that is not full even when every slot is taken, which
     * is what tells a rule apart from {@link #isFull()}: a full inventory still takes the
     * item it already holds a half stack of.
     *
     * @param stack stack that wants to be placed, may be {@code null}
     * @return {@code true} when at least one item of it fits
     */
    public boolean hasRoomFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return true;
        }
        if (stack.item().isStackable()) {
            for (ItemStack stored : slots) {
                if (stored.isStackableWith(stack) && !stored.isFull()) {
                    return true;
                }
            }
        }
        return firstEmptySlot() >= 0;
    }

    /**
     * Finds the first slot that can take a new stack.
     *
     * @return the slot index, or {@code -1} when every slot holds something
     */
    public int firstEmptySlot() {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i].isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Counts how many items of a type are stored.
     *
     * @param item item type to count
     * @return the total amount over every slot
     */
    public int countOf(Item item) {
        int total = 0;
        for (ItemStack stack : slots) {
            if (!stack.isEmpty() && stack.item() == item) {
                total += stack.count();
            }
        }
        return total;
    }

    /**
     * Adds a stack to the inventory.
     * <p>
     * Half filled stacks of the same item are topped up first, so a nearly full
     * inventory does not waste a slot on a single item. Remaining items are then
     * put into empty slots, starting at the first one.
     *
     * @param stack stack to add, its amount is left untouched
     * @return the amount that did not fit, {@code 0} when everything was added
     */
    public int add(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        Item item = stack.item();
        int remaining = stack.count();

        if (item.isStackable()) {
            for (int i = 0; i < slots.length && remaining > 0; i++) {
                ItemStack stored = slots[i];
                if (!stored.isStackableWith(stack)) {
                    continue;
                }
                remaining = stored.grow(remaining);
            }
        }

        for (int i = 0; i < slots.length && remaining > 0; i++) {
            if (!slots[i].isEmpty()) {
                continue;
            }
            int fitting = Math.min(remaining, item.maxStackSize());
            slots[i] = ItemStack.of(item, fitting);
            remaining -= fitting;
        }
        return remaining;
    }

    /** Empties every slot. */
    public void clear() {
        Arrays.fill(slots, ItemStack.EMPTY);
    }

    @Override
    public String toString() {
        return "Inventory(slots=" + slots.length + ", items=" + storedItemCount() + ")";
    }

    /** Verifies that a slot index belongs to this inventory. */
    private void checkSlot(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IndexOutOfBoundsException("Slot out of range: " + slot);
        }
    }

    /** Amount of items stored in total, used by the {@code toString()} methods. */
    protected int storedItemCount() {
        int total = 0;
        for (ItemStack stack : slots) {
            total += stack.count();
        }
        return total;
    }
}
