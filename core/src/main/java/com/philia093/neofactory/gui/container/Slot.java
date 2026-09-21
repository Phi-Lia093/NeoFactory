package com.philia093.neofactory.gui.container;

import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.util.Constants;

import java.util.Objects;

/**
 * One cell of a container: where it is drawn, which inventory it shows and what may
 * happen to the items in it.
 * <p>
 * Coordinates are measured from the upper left corner of the panel with the Y axis
 * pointing down, the direction the art of the game is stored in;
 * {@link ContainerView} turns them into the upwards axis of the interface. A cell is
 * {@link ContainerLayout#SLOT_SIZE} pixels wide, exactly the icon of an item, and the
 * bevel around it comes from the slot pictures, which two neighbours share.
 * <p>
 * A slot never stores an item itself: it is a window onto one index of an
 * {@link Inventory}, so the same inventory can be shown by two containers and a
 * machine keeps its contents without knowing how they are drawn.
 */
public final class Slot {

    /** What may happen to the items of a slot. */
    public enum Rule {

        /** Takes and hands out items, the normal case. */
        NORMAL,

        /**
         * Only hands items out, the result of a recipe.
         * <p>
         * A player cannot drop anything here: what appears is made by the container
         * itself and is taken out again, see
         * {@link ContainerMenu#touchDown(float, float, int, boolean)}.
         */
        OUTPUT,

        /**
         * A work field such as the crafting grid of the player.
         * <p>
         * What lies here belongs to the moment the container is open: when it closes, the
         * container hands the field to the world instead of hiding it, see
         * {@link ContainerMenu#setDropper(ContainerMenu.StackDropper)}.
         */
        WORK
    }

    private final int x;
    private final int y;
    private final Inventory inventory;
    private final int index;
    private final Rule rule;

    /**
     * Creates a slot.
     *
     * @param x left edge of the cell, relative to the panel
     * @param y upper edge of the cell, relative to the panel
     * @param inventory inventory this slot shows
     * @param index index of the shown slot inside that inventory
     * @param rule what may happen to the items in this slot
     */
    public Slot(int x, int y, Inventory inventory, int index, Rule rule) {
        this.x = x;
        this.y = y;
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.index = index;
        this.rule = Objects.requireNonNull(rule, "rule");
    }

    /** Left edge of the cell, relative to the panel. */
    public int x() {
        return x;
    }

    /** Upper edge of the cell, relative to the panel. */
    public int y() {
        return y;
    }

    /** Inventory this slot shows. */
    public Inventory inventory() {
        return inventory;
    }

    /** Index of the shown slot inside {@link #inventory()}. */
    public int index() {
        return index;
    }

    /** What may happen to the items in this slot. */
    public Rule rule() {
        return rule;
    }

    /** {@code true} when this slot only hands items out. */
    public boolean isOutput() {
        return rule == Rule.OUTPUT;
    }

    /**
     * {@code true} when a point of the panel lies on this cell.
     *
     * @param localX X coordinate relative to the panel
     * @param localY Y coordinate relative to the panel, measured downwards
     * @return {@code true} when the point is inside the cell
     */
    public boolean contains(int localX, int localY) {
        return localX >= x && localX < x + ContainerLayout.SLOT_SIZE
                && localY >= y && localY < y + ContainerLayout.SLOT_SIZE;
    }

    /** Stack this slot shows, {@link ItemStack#EMPTY} when it is empty. */
    public ItemStack stack() {
        return inventory.get(index);
    }

    /**
     * Writes a stack into the inventory behind this slot.
     *
     * @param stack stack to store, an empty stack clears the slot
     */
    public void set(ItemStack stack) {
        inventory.set(index, stack);
    }

    /** Takes the stack out of the inventory behind this slot and clears it. */
    public ItemStack remove() {
        return inventory.remove(index);
    }

    /** Side length of a cell in pixels, the size of an item icon. */
    public static int size() {
        return Constants.ITEM_ICON_SIZE;
    }

    @Override
    public String toString() {
        return "Slot(" + x + ", " + y + " -> " + inventory + "[" + index + "], " + rule + ")";
    }
}
