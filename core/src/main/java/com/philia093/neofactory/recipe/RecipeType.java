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

    /**
     * A recipe the furnace of the age of steam smelts.
     * <p>
     * It reads like {@link #SMELTING} and spends steam besides, see {@link #isSteam()}: a machine of the age
     * of steam pays for its work with the steam of the boiler and not with a flame of its own, so the two ages
     * cannot share one folder - an ore that is smelted for free would not be a machine of the steam age.
     */
    public static final RecipeType STEAM_SMELTING = new RecipeType("steam_smelting");

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
     * The kinds of recipe a machine of the age of steam reads, the ones of {@link #STEAM_SMELTING} through
     * {@link #FORGING}.
     */
    private static final List<RecipeType> STEAM;

    static {
        BY_NAME.put(CRAFTING_SHAPED.name, CRAFTING_SHAPED);
        BY_NAME.put(CRAFTING_SHAPELESS.name, CRAFTING_SHAPELESS);
        BY_NAME.put(SMELTING.name, SMELTING);
        List<RecipeType> steam = List.of(STEAM_SMELTING, ALLOY_SMELTING, GRINDING, COMPRESSING,
                EXTRACTING, FORGING);
        for (RecipeType type : steam) {
            BY_NAME.put(type.name, type);
        }
        STEAM = steam;
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
     * {@code true} when this kind of recipe is performed by a machine of the age of steam.
     * <p>
     * A kind of that age is read from its own folder and spends steam, see
     * {@link com.philia093.neofactory.recipe.SteamRecipe} and
     * {@link com.philia093.neofactory.machine.SteamMachine}.
     */
    public boolean isSteam() {
        return STEAM.contains(this);
    }

    /** The kinds of recipe a machine of the age of steam reads, in the order they are declared. */
    public static List<RecipeType> steamKinds() {
        return STEAM;
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
