package com.philia093.neofactory.item;

import java.util.Objects;

/**
 * A number of items of a single type, the unit an inventory slot stores.
 * <p>
 * The item type never changes, only the amount may be grown, shrunk or split.
 * The amount is always clamped to the stack size of the item, so a slot can
 * never hold more than {@link Item#maxStackSize()} of it.
 * <p>
 * {@link #EMPTY} stands for an empty slot and is shared by the whole game. That
 * instance is read only: growing it or writing a positive amount to it is
 * refused, because every empty slot would change at once otherwise.
 */
public final class ItemStack {

    /** The empty stack, shared by every empty inventory slot. */
    public static final ItemStack EMPTY = new ItemStack(null, 0);

    private Item item;
    private int count;

    private ItemStack(Item item, int count) {
        this.item = item;
        this.count = count;
    }

    /**
     * Creates a stack of the given size.
     *
     * @param item item type to store
     * @param count requested amount, clamped to the stack size of the item
     * @return the new stack, or {@link #EMPTY} when the item is {@code null} or
     *         the amount is not positive
     */
    public static ItemStack of(Item item, int count) {
        if (item == null || count <= 0) {
            return EMPTY;
        }
        return new ItemStack(item, Math.min(count, item.maxStackSize()));
    }

    /** {@code true} when this stack holds no item. */
    public boolean isEmpty() {
        return item == null || count <= 0;
    }

    /** Item type of this stack, {@link Items#AIR} when the stack is empty. */
    public Item item() {
        return item == null ? Items.AIR : item;
    }

    /** Amount of items held by this stack. */
    public int count() {
        return count;
    }

    /** Amount of this item that fits into a single slot. */
    public int maxStackSize() {
        return item == null ? 0 : item.maxStackSize();
    }

    /** {@code true} when no more item of this type fits into this stack. */
    public boolean isFull() {
        return count >= maxStackSize();
    }

    /** Space left in this stack, used to merge two stacks of the same item. */
    public int room() {
        return Math.max(0, maxStackSize() - count);
    }

    /** {@code true} when both stacks hold the same item type. */
    public boolean sameItem(ItemStack other) {
        return other != null && !isEmpty() && item == other.item;
    }

    /** {@code true} when the other stack may be merged into this one. */
    public boolean isStackableWith(ItemStack other) {
        return sameItem(other) && item.isStackable();
    }

    /**
     * Adds items to this stack, filling it up to its stack size.
     *
     * @param amount requested amount, ignored when not positive
     * @return the amount that did not fit, {@code 0} when everything fit
     */
    public int grow(int amount) {
        if (item == null || amount <= 0) {
            return amount;
        }
        int fitting = Math.min(amount, room());
        count += fitting;
        return amount - fitting;
    }

    /**
     * Moves items of this stack into a new stack.
     *
     * @param amount requested amount
     * @return a new stack holding the taken items, {@link #EMPTY} when this stack
     *         is empty
     */
    public ItemStack split(int amount) {
        if (item == null || amount <= 0) {
            return EMPTY;
        }
        int taken = Math.min(amount, count);
        count -= taken;
        return new ItemStack(item, taken);
    }

    /**
     * Writes a new amount into this stack.
     *
     * @param newCount requested amount, clamped between {@code 0} and the stack
     *                 size of the item
     * @throws IllegalArgumentException when the shared {@link #EMPTY} instance is
     *         asked to hold items
     */
    public void setCount(int newCount) {
        if (item == null) {
            if (newCount > 0) {
                throw new IllegalArgumentException("The shared empty stack cannot hold items, use ItemStack.of");
            }
            return;
        }
        count = Math.max(0, Math.min(newCount, item.maxStackSize()));
    }

    /** Returns a detached copy of this stack, see {@link #EMPTY}. */
    public ItemStack copy() {
        if (isEmpty()) {
            return EMPTY;
        }
        return new ItemStack(item, count);
    }

    @Override
    public String toString() {
        return isEmpty() ? "ItemStack(empty)" : "ItemStack(" + count + " x " + item.name() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemStack)) {
            return false;
        }
        ItemStack other = (ItemStack) o;
        return item == other.item && count == other.count;
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, count);
    }
}
