package com.philia093.neofactory.recipe;

import com.philia093.neofactory.item.ItemStack;

import java.util.Objects;

/**
 * A recipe whose ingredients stand in a pattern.
 * <p>
 * The pattern is a small grid of {@link Ingredient ingredients}, for example two
 * planks above each other for a stick. Where the pattern sits inside the grid does not
 * matter: it is looked for at every offset, which is what makes a two by two recipe
 * work in the two by two field of the player and in a larger one alike. Places of the
 * grid that the pattern does not cover have to be empty, so a recipe never matches a
 * field that has something in the way.
 */
public final class ShapedRecipe implements Recipe {

    private final String name;
    private final Ingredient[][] pattern;
    private final ItemStack result;

    /**
     * Creates a recipe.
     *
     * @param name name of the recipe, the name of its file
     * @param pattern ingredients, counted row by row from the upper left one
     * @param result what the recipe makes
     */
    public ShapedRecipe(String name, Ingredient[][] pattern, ItemStack result) {
        this.name = Objects.requireNonNull(name, "name");
        this.pattern = Objects.requireNonNull(pattern, "pattern");
        this.result = Objects.requireNonNull(result, "result");
        if (pattern.length == 0 || pattern[0].length == 0) {
            throw new IllegalArgumentException("A pattern needs at least one ingredient: " + name);
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public RecipeType type() {
        return RecipeType.CRAFTING_SHAPED;
    }

    @Override
    public ItemStack result() {
        return ItemStack.of(result.item(), result.count());
    }

    /** Amount of columns the pattern covers. */
    public int width() {
        return pattern[0].length;
    }

    /** Amount of rows the pattern covers. */
    public int height() {
        return pattern.length;
    }

    /** Ingredient that has to stand in one place of the pattern. */
    public Ingredient ingredient(int x, int y) {
        return pattern[y][x];
    }

    @Override
    public boolean matches(RecipeGrid grid) {
        return offsetOf(grid) >= 0;
    }

    /**
     * Where the pattern fits into the grid.
     *
     * @param grid grid to look at
     * @return the offset of the first fitting place, or {@code -1} when the pattern
     *         does not fit anywhere
     */
    public int offsetOf(RecipeGrid grid) {
        for (int y = 0; y + height() <= grid.height(); y++) {
            for (int x = 0; x + width() <= grid.width(); x++) {
                if (matchesAt(grid, x, y)) {
                    return x + y * grid.width();
                }
            }
        }
        return -1;
    }

    @Override
    public void consume(RecipeGrid grid) {
        int offset = offsetOf(grid);
        if (offset < 0) {
            return;
        }
        int offsetX = offset % grid.width();
        int offsetY = offset / grid.width();
        for (int y = 0; y < height(); y++) {
            for (int x = 0; x < width(); x++) {
                if (pattern[y][x].isEmpty()) {
                    continue;
                }
                ItemStack stack = grid.get(offsetX + x, offsetY + y);
                stack.setCount(stack.count() - 1);
            }
        }
    }

    /** {@code true} when the pattern matches at one place and the rest is empty. */
    private boolean matchesAt(RecipeGrid grid, int offsetX, int offsetY) {
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                boolean inside = x >= offsetX && x < offsetX + width()
                        && y >= offsetY && y < offsetY + height();
                if (inside) {
                    if (!pattern[y - offsetY][x - offsetX].matches(grid.get(x, y))) {
                        return false;
                    }
                } else if (!grid.get(x, y).isEmpty()) {
                    // Everything the pattern does not cover has to stay empty.
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "ShapedRecipe(" + name + ", " + width() + " x " + height() + " -> "
                + result + ")";
    }
}
