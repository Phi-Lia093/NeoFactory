package com.philia093.neofactory.recipe;

/**
 * The recipes a work field of nine cells makes.
 * <p>
 * The game holds two kinds of hand crafting - a pattern that has to be laid out and a bag of
 * ingredients that only have to be present - and both are looked up in one place, so a new field
 * never asks the two tables in a different order than the one before it: <b>a pattern wins over a
 * bag</b>. Without that rule a field that holds a pattern could also satisfy a shapeless recipe of
 * the same items and would make whichever came first in the table.
 * <p>
 * A field is offered as a {@link RecipeGrid}, so the two by two field of the player and the three
 * by three field of the table of the workshop use the very same lookup: {@link ShapedRecipe} looks
 * for its pattern at every offset, which is what makes a recipe of two by two work in a field of
 * three by three, see {@link ShapedRecipe#offsetOf(RecipeGrid)}.
 */
public final class CraftingRecipes {

    private CraftingRecipes() {
        // Utility class: never instantiated.
    }

    /**
     * The recipe a field makes, the shaped ones first.
     *
     * @param grid items the player laid out, see {@link InventoryGrid}
     * @return the recipe, or {@code null} when nothing can be made of them
     */
    public static Recipe find(RecipeGrid grid) {
        Recipe shaped = RecipeRegistry.find(RecipeType.CRAFTING_SHAPED, grid);
        return shaped != null ? shaped : RecipeRegistry.find(RecipeType.CRAFTING_SHAPELESS, grid);
    }
}
