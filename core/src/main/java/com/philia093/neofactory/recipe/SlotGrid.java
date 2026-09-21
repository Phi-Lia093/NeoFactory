package com.philia093.neofactory.recipe;

import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.ItemStack;

import java.util.List;
import java.util.Objects;

/**
 * A grid that shows any list of slots of an {@link Inventory}.
 * <p>
 * {@link InventoryGrid} covers the common case, a rectangular block of slots that lies
 * next to each other. A machine is not that tidy: its input slots may sit between a
 * fuel slot and the outputs, and it still wants to offer them to a recipe as one grid.
 * This class does exactly that - it maps the places of the grid onto a list of slot
 * indexes, in the order the machine declared them.
 * <p>
 * A grid of a single row is what a machine uses, because the input of a machine is a
 * bag of items rather than a pattern; {@link #SlotGrid(Inventory, List, int)} builds a
 * wider one for a machine that really does work on a pattern.
 */
public final class SlotGrid implements RecipeGrid {

    private final Inventory inventory;
    private final int[] slots;
    private final int width;
    private final int height;

    /**
     * Creates a grid of one row that shows the given slots.
     *
     * @param inventory inventory holding the slots
     * @param slots slot indexes, in the order they are offered to a recipe
     */
    public SlotGrid(Inventory inventory, List<Integer> slots) {
        this(inventory, slots, Math.max(1, slots.size()));
    }

    /**
     * Creates a grid of a width.
     *
     * @param inventory inventory holding the slots
     * @param slots slot indexes, in the order they are offered to a recipe
     * @param width amount of columns, has to divide the amount of slots
     * @throws IllegalArgumentException when the width does not fit the slots
     */
    public SlotGrid(Inventory inventory, List<Integer> slots, int width) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        Objects.requireNonNull(slots, "slots");
        if (slots.isEmpty()) {
            throw new IllegalArgumentException("A grid without a slot does not exist");
        }
        if (width <= 0 || slots.size() % width != 0) {
            throw new IllegalArgumentException("The width " + width + " does not fit "
                    + slots.size() + " slots");
        }
        this.slots = new int[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            this.slots[i] = slots.get(i);
        }
        this.width = width;
        this.height = slots.size() / width;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public ItemStack get(int x, int y) {
        return inventory.get(slotIndex(x, y));
    }

    /**
     * Slot index inside {@link #inventory()} that one place of the grid points at.
     *
     * @param x column, counted from the left
     * @param y row, counted from the top
     * @return the slot index
     */
    public int slotIndex(int x, int y) {
        int place = y * width + x;
        if (place < 0 || place >= slots.length) {
            throw new IndexOutOfBoundsException("Place out of range: x=" + x + ", y=" + y);
        }
        return slots[place];
    }

    /** Inventory this grid shows. */
    public Inventory inventory() {
        return inventory;
    }

    @Override
    public String toString() {
        return "SlotGrid(" + width + " x " + height + ", " + slots.length + " slots of "
                + inventory + ")";
    }
}
