package com.philia093.neofactory.recipe;

import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.ItemStack;

import java.util.Objects;

/**
 * A grid that shows a rectangular block of slots of an {@link Inventory}.
 * <p>
 * The class is the bridge between the inventories of the game and the recipes: the two
 * by two field of the player is one of these, and so is the input of a machine. It
 * counts its slots row by row from the upper left one, the same order
 * {@link com.philia093.neofactory.gui.container.ContainerLayout#addGrid} places them
 * in.
 */
public final class InventoryGrid implements RecipeGrid {

    private final Inventory inventory;
    private final int firstIndex;
    private final int width;
    private final int height;

    /**
     * Creates a view onto a block of slots.
     *
     * @param inventory inventory that holds the slots
     * @param firstIndex index of the slot in the upper left corner
     * @param width amount of columns
     * @param height amount of rows
     */
    public InventoryGrid(Inventory inventory, int firstIndex, int width, int height) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.firstIndex = firstIndex;
        this.width = width;
        this.height = height;
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
        return inventory.get(indexOf(x, y));
    }

    /**
     * Index of one place inside the inventory this grid shows.
     *
     * @param x column, counted from the left
     * @param y row, counted from the top
     * @return the slot index inside {@link #inventory()}
     */
    public int indexOf(int x, int y) {
        return firstIndex + y * width + x;
    }

    /** Inventory this grid shows. */
    public Inventory inventory() {
        return inventory;
    }

    /**
     * Takes a single item out of one place of the grid.
     *
     * @param x column, counted from the left
     * @param y row, counted from the top
     */
    public void takeOne(int x, int y) {
        ItemStack stack = get(x, y);
        if (stack.isEmpty()) {
            return;
        }
        stack.setCount(stack.count() - 1);
    }

    @Override
    public String toString() {
        return "InventoryGrid(" + width + " x " + height + ", " + inventory + ")";
    }
}
