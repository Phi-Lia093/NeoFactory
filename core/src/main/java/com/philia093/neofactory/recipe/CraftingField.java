package com.philia093.neofactory.recipe;

import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.ItemStack;

import java.util.Objects;

/**
 * The work field of a table of the workshop: nine cells, the recipe they make and the result.
 * <p>
 * The class is the whole logic of a table and knows nothing about drawing, so it is tested without a
 * window, the way the machines are. The nine cells belong to the block and are therefore handed in:
 * what a player laid out is stored with the chunk, so a table that is walked away from and opened
 * again still holds the pattern that was lying on it, see
 * {@link com.philia093.neofactory.blockentity.CraftingTableBlockEntity}.
 * <p>
 * <b>The result is made, not stored.</b> It is recomputed from the cells, so a stored table can never
 * hold a result that its own cells do not make - which is what would happen if the product of a craft
 * were part of the file. Taking the result consumes the ingredients the recipe was matched with, which
 * is the very arithmetic {@link ShapedRecipe#consume(RecipeGrid)} and
 * {@link ShapelessRecipe#consume(RecipeGrid)} already carry.
 */
public final class CraftingField {

    /** Amount of columns of the field. */
    public static final int COLUMNS = 3;

    /** Amount of rows of the field. */
    public static final int ROWS = 3;

    /** Amount of cells of the field, nine. */
    public static final int CELLS = COLUMNS * ROWS;

    /** The nine cells, owned by the block that holds this field. */
    private final Inventory contents;

    /** The cells offered to a recipe, counted row by row from the upper left one. */
    private final InventoryGrid grid;

    /** What the recipe makes, one slot a screen shows. */
    private final Inventory result = new Inventory(1);

    /** Recipe the cells hold right now, {@code null} when nothing matches them. */
    private Recipe recipe;

    /**
     * Creates the work field of a table.
     *
     * @param contents the nine cells, owned by the block of the table, never {@code null}
     */
    public CraftingField(Inventory contents) {
        this.contents = Objects.requireNonNull(contents, "contents");
        if (contents.size() < CELLS) {
            throw new IllegalArgumentException(
                    "A work field needs " + CELLS + " cells, the block holds " + contents.size());
        }
        this.grid = new InventoryGrid(contents, 0, COLUMNS, ROWS);
        refresh();
    }

    /** The nine cells of this field, owned by the block. */
    public Inventory contents() {
        return contents;
    }

    /** The cells offered to a recipe. */
    public InventoryGrid grid() {
        return grid;
    }

    /** The slot a screen shows the result in, one cell. */
    public Inventory resultSlot() {
        return result;
    }

    /**
     * Recipe the cells hold right now.
     *
     * @return the recipe, or {@code null} when nothing can be made of them
     */
    public Recipe recipe() {
        return recipe;
    }

    /** {@code true} when the cells make something. */
    public boolean makesSomething() {
        return recipe != null;
    }

    /**
     * Looks for the recipe of the cells again and shows it.
     * <p>
     * Called when the field is created and after every click that changed it, so the result slot and
     * the cells can never disagree, see
     * {@link com.philia093.neofactory.gui.container.ContainerMenu#setChangeListener(ContainerMenu.ChangeListener)}.
     */
    public void refresh() {
        recipe = CraftingRecipes.find(grid);
        result.set(0, recipe == null ? ItemStack.EMPTY : recipe.result());
    }

    /**
     * Takes the result out of the field.
     * <p>
     * Called by the screen when a player takes the product: the ingredients the recipe was matched with
     * give up one item each, the next result is worked out and handed over. <b>Nothing is made when the
     * cells hold no recipe</b>, which is what an empty field answers.
     *
     * @return the stack that was made, {@link ItemStack#EMPTY} when nothing matches
     */
    public ItemStack take() {
        if (recipe == null) {
            return ItemStack.EMPTY;
        }
        ItemStack made = recipe.result();
        recipe.consume(grid);
        refresh();
        return made;
    }

    @Override
    public String toString() {
        return "CraftingField(" + (recipe == null ? "nothing to make" : recipe.name()) + ")";
    }
}
