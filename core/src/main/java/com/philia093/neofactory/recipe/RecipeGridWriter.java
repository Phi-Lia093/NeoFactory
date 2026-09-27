package com.philia093.neofactory.recipe;

import com.philia093.neofactory.item.ItemStack;

/**
 * A grid a recipe may be laid into.
 * <p>
 * {@link RecipeGrid} is what a recipe looks at, and looking is all a recipe does: what it asks for and
 * what it makes. Laying a recipe out - which is what a player does with the cells of a table, and what
 * a button beside a recipe does for them - needs a grid that may be written to, see
 * {@link RecipeArranger}.
 * <p>
 * The two grids of the game implement this as well, so a field of a table, the input of a machine and
 * the field of the player are all arranged by the very same code: {@link InventoryGrid} writes into the
 * slot its place points at, {@link SlotGrid} into the slot of the machine.
 */
public interface RecipeGridWriter extends RecipeGrid {

    /**
     * Puts a stack into one place of the grid.
     * <p>
     * The stack that stood there before is replaced, the way a player who takes something out of their
     * hand and puts it into a cell does it. An empty stack clears the place.
     *
     * @param x column, counted from the left
     * @param y row, counted from the top
     * @param stack stack to store
     */
    void place(int x, int y, ItemStack stack);
}
