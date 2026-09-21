package com.philia093.neofactory.recipe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The recipes the game knows, sorted by their type.
 * <p>
 * The table is filled once during startup by {@link RecipeLoader} and only read after
 * that. A caller asks for the recipes of one type and lets them decide whether the
 * grid it holds is theirs, so a new recipe never has to be linked into the code of a
 * screen or of a machine.
 */
public final class RecipeRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<RecipeType, List<Recipe>> BY_TYPE = new LinkedHashMap<>();

    private RecipeRegistry() {
        // Utility class: never instantiated.
    }

    /**
     * Adds a recipe to the table.
     *
     * @param recipe recipe to add
     */
    public static void register(Recipe recipe) {
        BY_TYPE.computeIfAbsent(recipe.type(), type -> new ArrayList<>()).add(recipe);
    }

    /**
     * Every recipe of one type, in the order they were read.
     *
     * @param type type to look up
     * @return the recipes, an empty list when the type has none
     */
    public static List<Recipe> recipes(RecipeType type) {
        List<Recipe> recipes = BY_TYPE.get(type);
        return recipes == null ? List.of() : Collections.unmodifiableList(recipes);
    }

    /**
     * Finds the recipe that fits a grid.
     * <p>
     * The first match wins, which is the order the files were read in. A machine that
     * needs the exact recipe instead of the first one asks for the list and picks its
     * own, see {@link #recipes(RecipeType)}.
     *
     * @param type type of the recipe
     * @param grid items that are offered
     * @return the recipe, or {@code null} when none fits
     */
    public static Recipe find(RecipeType type, RecipeGrid grid) {
        for (Recipe recipe : recipes(type)) {
            if (recipe.matches(grid)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * Amount of recipes of one type.
     *
     * @param type type to count
     * @return the amount
     */
    public static int count(RecipeType type) {
        return recipes(type).size();
    }

    /** Amount of recipes of every type together. */
    public static int count() {
        int total = 0;
        for (List<Recipe> recipes : BY_TYPE.values()) {
            total += recipes.size();
        }
        return total;
    }

    /** Logs how many recipes were read, called once startup is finished. */
    public static void logStatistics() {
        for (RecipeType type : RecipeType.all()) {
            LOGGER.info("{} recipes of type '{}'", count(type), type.name());
        }
    }

    /** Empties the table, used when a game is thrown away or a test starts over. */
    public static void clear() {
        BY_TYPE.clear();
    }
}
