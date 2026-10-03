package com.philia093.neofactory.recipe;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The kind of a recipe, which is also the folder its file lives in.
 * <p>
 * Recipes are stored as {@code assets/recipes/<type>/<name>.json}, so the name of a
 * type is the name of a folder: a new kind of crafting only needs a constant here and
 * a folder next to the existing ones, see {@link RecipeLoader}.
 * <p>
 * The list holds what the game can use today. The crafting grid of the player reads the two crafting types,
 * and every machine that works on an item reads one of {@link #processingKinds()} - one folder per group of
 * machines, so a furnace of bronze and a furnace of the high voltage read the very same file.
 */
public final class RecipeType {

    /** A recipe whose ingredients stand in a pattern, such as a pickaxe. */
    public static final RecipeType CRAFTING_SHAPED = new RecipeType("crafting_shaped");

    /** A recipe whose ingredients only have to be present, such as planks from a log. */
    public static final RecipeType CRAFTING_SHAPELESS = new RecipeType("crafting_shapeless");

    /** A recipe a machine performs over time, such as smelting an ore. */
    public static final RecipeType SMELTING = new RecipeType("smelting");

    /** A recipe the alloy furnace mixes out of two items. */
    public static final RecipeType ALLOY_SMELTING = new RecipeType("alloy_smelting");

    /** A recipe the grinder turns an ore into dust with. */
    public static final RecipeType GRINDING = new RecipeType("grinding");

    /** A recipe the compressor presses an item into a plate with. */
    public static final RecipeType COMPRESSING = new RecipeType("compressing");

    /** A recipe the extractor squeezes an item with. */
    public static final RecipeType EXTRACTING = new RecipeType("extracting");

    /** A recipe the forge hammer beats an ingot into shape with. */
    public static final RecipeType FORGING = new RecipeType("forging");

    private static final Map<String, RecipeType> BY_NAME = new LinkedHashMap<>();

    /**
     * The kinds of recipe a machine works through over time, the ones of {@link #SMELTING} through
     * {@link #FORGING}.
     */
    private static final List<RecipeType> PROCESSING;

    static {
        BY_NAME.put(CRAFTING_SHAPED.name, CRAFTING_SHAPED);
        BY_NAME.put(CRAFTING_SHAPELESS.name, CRAFTING_SHAPELESS);
        List<RecipeType> processing = List.of(SMELTING, ALLOY_SMELTING, GRINDING, COMPRESSING,
                EXTRACTING, FORGING);
        for (RecipeType type : processing) {
            BY_NAME.put(type.name, type);
        }
        PROCESSING = processing;
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

    /**
     * {@code true} when this kind of recipe is worked through by a machine over time.
     * <p>
     * A kind of that sort is read from its own folder and is one the furnace of every age may read: the file
     * says what it makes and what a craft costs, and each machine decides how to pay for it, see
     * {@link ProcessingRecipe}.
     */
    public boolean isProcessing() {
        return PROCESSING.contains(this);
    }

    /** The kinds of recipe a machine works through over time, in the order they are declared. */
    public static List<RecipeType> processingKinds() {
        return PROCESSING;
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
