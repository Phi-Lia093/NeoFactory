package com.philia093.neofactory.recipe;

import com.philia093.neofactory.item.ItemStack;

/**
 * The items a recipe is offered, arranged in a grid.
 * <p>
 * The interface keeps a recipe free of any knowledge about inventories: the crafting
 * field of the player passes a view onto its own slots, a machine passes its input
 * slots, and a test may pass a plain array. Every place holds a stack, an unused place
 * holds {@link ItemStack#EMPTY}.
 */
public interface RecipeGrid {

    /** Amount of columns of the grid. */
    int width();

    /** Amount of rows of the grid. */
    int height();

    /**
     * Stack that stands in one place of the grid.
     *
     * @param x column, counted from the left
     * @param y row, counted from the top
     * @return the stack, {@link ItemStack#EMPTY} when the place is unused
     */
    ItemStack get(int x, int y);

    /** Amount of places that hold at least one item. */
    default int filledPlaces() {
        int filled = 0;
        for (int y = 0; y < height(); y++) {
            for (int x = 0; x < width(); x++) {
                if (!get(x, y).isEmpty()) {
                    filled++;
                }
            }
        }
        return filled;
    }

    /** Number of items in the whole grid, a stack of five counts as five. */
    default int itemCount() {
        int total = 0;
        for (int y = 0; y < height(); y++) {
            for (int x = 0; x < width(); x++) {
                total += get(x, y).count();
            }
        }
        return total;
    }
}
