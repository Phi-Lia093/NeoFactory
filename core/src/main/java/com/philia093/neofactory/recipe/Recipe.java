package com.philia093.neofactory.recipe;

import com.philia093.neofactory.item.ItemStack;

/**
 * A recipe the game can perform on a grid of items.
 * <p>
 * A recipe only knows how to recognise its ingredients and what it makes: where the
 * grid lives - the two by two field of the inventory, the three by three field of a
 * workbench or the input of a machine - is the business of {@link RecipeGrid}. That is
 * what lets the same file serve a crafting screen and a machine.
 */
public interface Recipe {

    /** Name of the recipe, the name of its file without the extension. */
    String name();

    /** Kind of this recipe, also the folder its file was read from. */
    RecipeType type();

    /**
     * What the recipe makes.
     * <p>
     * Every call hands out a fresh stack, so the caller may change it without
     * touching the recipe.
     *
     * @return the result, never empty
     */
    ItemStack result();

    /**
     * {@code true} when the grid holds what this recipe needs.
     *
     * @param grid items that are offered to the recipe
     * @return {@code true} when {@link #consume(RecipeGrid)} may be called
     */
    boolean matches(RecipeGrid grid);

    /**
     * Takes one craft worth of ingredients out of the grid.
     * <p>
     * Called only after {@link #matches(RecipeGrid)} returned {@code true}.
     *
     * @param grid grid the ingredients are taken from
     */
    void consume(RecipeGrid grid);
}
