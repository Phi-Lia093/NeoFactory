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
 * <p>
 * <b>Wear:</b> a stack is what a tool of the game is worn in, see {@link Damageable}: the amount of
 * damage belongs to the stack and not to the type of the item, so the pickaxe in the hand and the
 * one in a chest are worn differently. Every stack starts new, and the damage is written to a
 * stored inventory as well, see {@code SaveTags}. Because the damage belongs to the stack, two
 * stacks only merge while they are worn the same: a used axe and a new one are two pieces, not one
 * pile.
 */
public final class ItemStack implements Damageable {

    /** The empty stack, shared by every empty inventory slot. */
    public static final ItemStack EMPTY = new ItemStack(null, 0);

    private Item item;
    private int count;

    /** Damage taken from the life of the item, {@code 0} for a fresh piece. */
    private int damage;

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
        return sameItem(other) && item.isStackable() && damage == other.damage;
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
     * <p>
     * The new stack is worn the same way this one is: the damage belongs to the stack, so every
     * piece a stack holds is as worn as the stack itself.
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
        ItemStack split = new ItemStack(item, taken);
        split.damage = damage;
        return split;
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

    @Override
    public int maxDamage() {
        return item == null ? 0 : item.maxDamage();
    }

    @Override
    public int damage() {
        return damage;
    }

    @Override
    public int applyDamage(int amount) {
        if (item == null || amount <= 0) {
            return 0;
        }
        int taken = Math.min(amount, remainingDamage());
        damage += taken;
        return taken;
    }

    /**
     * Writes a new amount of damage into this stack.
     * <p>
     * The value is clamped to the life of the item, which is what keeps a piece that is used up at
     * the end of its life instead of counting past it, see {@link Damageable#isBroken()}.
     *
     * @param newDamage requested damage, clamped between {@code 0} and the life of the item
     * @throws IllegalArgumentException when the shared {@link #EMPTY} instance is asked to wear
     */
    public void setDamage(int newDamage) {
        if (item == null) {
            if (newDamage > 0) {
                throw new IllegalArgumentException("The shared empty stack cannot wear, use ItemStack.of");
            }
            return;
        }
        if (newDamage < 0) {
            throw new IllegalArgumentException("Damage must not be negative: " + newDamage);
        }
        damage = Math.min(newDamage, item.maxDamage());
    }

    /** Returns a detached copy of this stack, see {@link #EMPTY}. */
    public ItemStack copy() {
        if (isEmpty()) {
            return EMPTY;
        }
        ItemStack copy = new ItemStack(item, count);
        copy.damage = damage;
        return copy;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "ItemStack(empty)";
        }
        String wear = isDamageable() ? ", " + remainingDamage() + "/" + maxDamage() + " left" : "";
        return "ItemStack(" + count + " x " + item.name() + wear + ")";
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
        return item == other.item && count == other.count && damage == other.damage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, count, damage);
    }
}
