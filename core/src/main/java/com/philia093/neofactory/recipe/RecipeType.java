package com.philia093.neofactory.recipe;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The kind of a recipe, which is also the folder its file lives in.
 * <p>
 * Recipes are stored as {@code assets/recipes/<type>/<name>.json}, so the name of a
 * type is the name of a folder: a new kind of crafting only needs a constant here and
 * a folder next to the existing ones, see {@link RecipeLoader}.
 * <p>
 * The list holds what the game can use today. A furnace and a machine read
 * {@link #SMELTING}, the crafting grid of the player reads the two crafting types.
 */
public final class RecipeType {

    /** A recipe whose ingredients stand in a pattern, such as a pickaxe. */
    public static final RecipeType CRAFTING_SHAPED = new RecipeType("crafting_shaped");

    /** A recipe whose ingredients only have to be present, such as planks from a log. */
    public static final RecipeType CRAFTING_SHAPELESS = new RecipeType("crafting_shapeless");

    /** A recipe a machine performs over time, such as smelting an ore. */
    public static final RecipeType SMELTING = new RecipeType("smelting");

    private static final Map<String, RecipeType> BY_NAME = new LinkedHashMap<>();

    static {
        BY_NAME.put(CRAFTING_SHAPED.name, CRAFTING_SHAPED);
        BY_NAME.put(CRAFTING_SHAPELESS.name, CRAFTING_SHAPELESS);
        BY_NAME.put(SMELTING.name, SMELTING);
    }

    private final String name;

    private RecipeType(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    /** Name of this type, also the name of its folder below {@code assets/recipes}. */
    public String name() {
        return name;
    }

    /**
     * Looks a type up by its name.
     *
     * @param name name of a folder below {@code assets/recipes}
     * @return the type, or {@code null} when no type uses that name
     */
    public static RecipeType byName(String name) {
        return name == null ? null : BY_NAME.get(name);
    }

    /** Every type the game knows, in the order they are declared. */
    public static java.util.Collection<RecipeType> all() {
        return BY_NAME.values();
    }

    @Override
    public String toString() {
        return "RecipeType(" + name + ")";
    }
}
