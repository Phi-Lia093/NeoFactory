package com.philia093.neofactory.gui.recipe;

import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.recipe.Ingredient;
import com.philia093.neofactory.recipe.Recipe;
import com.philia093.neofactory.recipe.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One recipe, laid out for a screen.
 * <p>
 * A recipe knows what it takes and what it makes, but not how it looks on a page: a pattern has a shape
 * of its own - three planks over two sticks - while a bag of ingredients and the recipe of a machine only
 * have an order. This class turns both into the same thing: a small grid of cells, one item in each, and
 * the product beside it, so a screen only has to draw cells and never asks what kind of recipe it shows.
 * <p>
 * <b>A cell holds one item</b> and not a stack: a page shows what a recipe is made of, and how many of
 * it the recipe takes is the business of the recipe itself, see {@link Recipe#consume}.
 */
public final class RecipePage {

    /** Amount of columns a recipe without a shape is laid out in, at most. */
    public static final int BAG_COLUMNS = 3;

    /**
     * Amount of columns a recipe of a machine is laid out in, at most.
     * <p>
     * A machine takes its ingredients beside each other and not above each other: the input slots of a
     * machine of this workshop lie in a row, the second one right of the first, so a recipe of a machine is
     * read the way the machine is filled. The width of the page is what the number is, which is why the
     * layout of the panel is asked for it; a recipe that asks for more than that many ingredients is the
     * only one that wraps, and it wraps onto a second row.
     */
    public static final int MACHINE_COLUMNS = RecipeBrowserLayout.PAGE_COLUMNS;

    private final Recipe recipe;
    private final int columns;
    private final int rows;
    private final List<ItemStack> cells;

    private RecipePage(Recipe recipe, int columns, int rows, List<ItemStack> cells) {
        this.recipe = recipe;
        this.columns = columns;
        this.rows = rows;
        this.cells = List.copyOf(cells);
    }

    /**
     * Lays a recipe out for a screen.
     *
     * @param recipe recipe to show, never {@code null}
     * @return the page, its grid is at least one cell large
     */
    public static RecipePage of(Recipe recipe) {
        Objects.requireNonNull(recipe, "recipe");
        if (recipe instanceof ShapedRecipe shaped) {
            List<ItemStack> cells = new ArrayList<>();
            for (int row = 0; row < shaped.height(); row++) {
                for (int column = 0; column < shaped.width(); column++) {
                    cells.add(sample(shaped.ingredient(column, row)));
                }
            }
            return new RecipePage(recipe, shaped.width(), shaped.height(), cells);
        }
        List<Ingredient> ingredients = recipe.ingredients();
        // A bag of ingredients is filled row by row and wraps for the sake of the page; a machine takes its
        // ingredients in one row, because its slots lie in one, see #MACHINE_COLUMNS.
        boolean craft = recipe.type().name().startsWith("crafting_");
        int columns = Math.max(1,
                Math.min(craft ? BAG_COLUMNS : MACHINE_COLUMNS, ingredients.size()));
        int rows = Math.max(1, (ingredients.size() + columns - 1) / columns);
        List<ItemStack> cells = new ArrayList<>();
        for (int cell = 0; cell < columns * rows; cell++) {
            cells.add(cell < ingredients.size() ? sample(ingredients.get(cell)) : ItemStack.EMPTY);
        }
        return new RecipePage(recipe, columns, rows, cells);
    }

    /** Recipe this page shows. */
    public Recipe recipe() {
        return recipe;
    }

    /** Amount of columns of the grid of this page. */
    public int columns() {
        return columns;
    }

    /** Amount of rows of the grid of this page. */
    public int rows() {
        return rows;
    }

    /**
     * What stands in one cell of the page.
     *
     * @param x column, counted from the left
     * @param y row, counted from the top
     * @return one item of the ingredient, {@link ItemStack#EMPTY} for a cell the recipe leaves empty
     */
    public ItemStack cell(int x, int y) {
        if (x < 0 || y < 0 || x >= columns || y >= rows) {
            return ItemStack.EMPTY;
        }
        return cells.get(y * columns + x);
    }

    /** What this recipe makes. */
    public ItemStack result() {
        return recipe.result();
    }

    /** Name of the kind of recipe, the name of its folder. */
    public String kind() {
        return recipe.type().name();
    }

    /** One item of an ingredient, {@code null} for an ingredient that asks for nothing. */
    private static ItemStack sample(Ingredient ingredient) {
        if (ingredient.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(ingredient.items().iterator().next(), 1);
    }

    @Override
    public String toString() {
        return "RecipePage(" + recipe.name() + ", " + columns + " x " + rows + ")";
    }
}
