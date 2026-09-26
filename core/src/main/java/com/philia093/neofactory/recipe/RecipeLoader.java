package com.philia093.neofactory.recipe;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the recipes of the game from {@code assets/recipes}.
 * <p>
 * A recipe is a small JSON file and the folder it lies in names its type, so the layout
 * of the asset folder is the table of contents:
 *
 * <pre>
 * recipes/crafting_shapeless/oak_planks.json   { "ingredients": ["log_oak"],
 *                                                "result": { "item": "planks_oak", "count": 4 } }
 * recipes/crafting_shaped/stick.json           { "pattern": ["P", "P"],
 *                                                "key": { "P": "planks_oak" },
 *                                                "result": { "item": "stick", "count": 4 } }
 * recipes/smelting/iron_ingot.json             { "ingredient": "iron_ore",
 *                                                "result": { "item": "iron_ingot" },
 *                                                "time": 10.0 }
 * </pre>
 *
 * An ingredient names a single item or a list of alternatives, for example
 * {@code "ingredient": ["log_oak", "log_birch"]}. Items are named the way
 * {@link ItemRegistry#byName(String)} knows them, so a recipe file never mentions a
 * number that only the code could understand.
 * <p>
 * {@link #parse(RecipeType, String, String)} is plain text to recipe work and can be
 * checked without a window, see {@code RecipeLoaderTest}; {@link #loadAll()} reads the
 * list the build writes next to the assets and registers everything it names. A file
 * that cannot be read is logged and skipped, so one broken recipe never keeps the game
 * from starting.
 */
public final class RecipeLoader {

    /** Folder below the asset root that holds the recipes. */
    public static final String FOLDER = "recipes";

    /** Extension of a recipe file. */
    public static final String EXTENSION = ".json";

    /**
     * Name of the file the build writes with every asset path.
     * <p>
     * The list is read instead of walking the folder of the running game: the assets live
     * in a jar once the game is shipped, where listing a folder depends on the platform,
     * while the list is a plain text file next to them.
     */
    public static final String ASSET_LIST = "assets.txt";

    private static final Logger LOGGER = LogManager.getLogger();

    private RecipeLoader() {
        // Utility class: never instantiated.
    }

    /**
     * Reads every recipe below {@code assets/recipes} and registers it.
     *
     * @return amount of recipes that were read
     */
    public static int loadAll() {
        RecipeRegistry.clear();
        FileHandle list = Gdx.files.internal(ASSET_LIST);
        if (!list.exists()) {
            LOGGER.error("The asset list '{}' is missing, no recipe can be read", ASSET_LIST);
            return 0;
        }
        int loaded = 0;
        int skipped = 0;
        for (String line : list.readString("UTF-8").split("\\R")) {
            String path = line.trim();
            if (!path.startsWith(FOLDER + "/") || !path.endsWith(EXTENSION)) {
                continue;
            }
            RecipeType type = typeOf(path);
            if (type == null) {
                skipped++;
                continue;
            }
            try {
                RecipeRegistry.register(
                        parse(type, nameOf(path), Gdx.files.internal(path).readString("UTF-8")));
                loaded++;
            } catch (RuntimeException e) {
                LOGGER.error("Unable to read the recipe '{}'", path, e);
            }
        }
        if (skipped > 0) {
            LOGGER.warn("Skipped {} recipe files below an unknown folder", skipped);
        }
        LOGGER.info("Read {} recipes from {}", loaded, ASSET_LIST);
        return loaded;
    }

    /**
     * Type of a recipe path, named by the folder the file lies in.
     *
     * @param path path such as {@code recipes/crafting_shapeless/oak_planks.json}
     * @return the type, or {@code null} when no type uses that folder
     */
    private static RecipeType typeOf(String path) {
        String rest = path.substring(FOLDER.length() + 1);
        int slash = rest.indexOf('/');
        return slash <= 0 ? null : RecipeType.byName(rest.substring(0, slash));
    }

    /**
     * Name of the recipe a path points at, which is the name of its file.
     *
     * @param path path such as {@code recipes/smelting/iron_ingot.json}
     * @return the name, without the folder and without the extension
     */
    private static String nameOf(String path) {
        String file = path.substring(path.lastIndexOf('/') + 1);
        return file.substring(0, file.length() - EXTENSION.length());
    }

    /**
     * Turns the text of a recipe file into a recipe.
     *
     * @param type type of the recipe, the folder the file lies in
     * @param name name of the recipe, the name of the file without its extension
     * @param json contents of the file
     * @return the recipe
     * @throws IllegalArgumentException when the file does not describe the type
     */
    public static Recipe parse(RecipeType type, String name, String json) {
        JsonValue root = new JsonReader().parse(json);
        ItemStack result = readResult(name, root);
        if (type == RecipeType.CRAFTING_SHAPED) {
            return new ShapedRecipe(name, readPattern(name, root), result);
        }
        if (type == RecipeType.CRAFTING_SHAPELESS) {
            return new ShapelessRecipe(name, readIngredients(name, root.get("ingredients")), result);
        }
        if (type == RecipeType.SMELTING) {
            return new SmeltingRecipe(name, readIngredient(name, root.get("ingredient")), result,
                    root.getFloat("time", SmeltingRecipe.DEFAULT_SECONDS));
        }
        if (type.isSteam()) {
            // A recipe of the age of steam names its ingredients as a list, the way a shapeless recipe does,
            // and says how many millibuckets of steam one craft spends, see SteamRecipe.
            return new SteamRecipe(name, type, readIngredients(name, root.get("ingredients")), result,
                    root.getFloat("time", SteamRecipe.DEFAULT_SECONDS), root.getInt("steam", 0));
        }
        throw new IllegalArgumentException("No loader for the recipe type '" + type.name() + "'");
    }

    /** Reads the stack a recipe makes. */
    private static ItemStack readResult(String name, JsonValue root) {
        JsonValue result = root.get("result");
        if (result == null) {
            throw new IllegalArgumentException("The recipe '" + name + "' has no result");
        }
        JsonValue item = result.get("item");
        if (item == null) {
            throw new IllegalArgumentException("The result of '" + name + "' names no item");
        }
        return ItemStack.of(readItem(name, item.asString()), result.getInt("count", 1));
    }

    /** Reads the rows of a shaped pattern and maps them through the key. */
    private static Ingredient[][] readPattern(String name, JsonValue root) {
        JsonValue pattern = root.get("pattern");
        JsonValue key = root.get("key");
        if (pattern == null || !pattern.isArray() || key == null) {
            throw new IllegalArgumentException("The shaped recipe '" + name
                    + "' needs a pattern and a key");
        }
        String[] rows = pattern.asStringArray();
        Ingredient[][] ingredients = new Ingredient[rows.length][];
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            if (line.isEmpty()) {
                throw new IllegalArgumentException("Row " + row + " of '" + name + "' is empty");
            }
            ingredients[row] = new Ingredient[line.length()];
            for (int column = 0; column < line.length(); column++) {
                ingredients[row][column] = readSymbol(name, key, line.charAt(column));
            }
        }
        return ingredients;
    }

    /** Reads one symbol of a pattern, a blank symbol asks for an empty place. */
    private static Ingredient readSymbol(String name, JsonValue key, char symbol) {
        if (symbol == ' ' || symbol == '.') {
            return Ingredient.empty();
        }
        JsonValue entry = key.get(String.valueOf(symbol));
        if (entry == null) {
            throw new IllegalArgumentException("The recipe '" + name + "' uses '" + symbol
                    + "' without naming it in its key");
        }
        return readIngredient(name, entry);
    }

    /** Reads a list of ingredients, one per place of a shapeless grid. */
    private static List<Ingredient> readIngredients(String name, JsonValue array) {
        if (array == null || !array.isArray() || array.size == 0) {
            throw new IllegalArgumentException("The recipe '" + name + "' needs ingredients");
        }
        List<Ingredient> ingredients = new ArrayList<>(array.size);
        for (JsonValue element : array) {
            ingredients.add(readIngredient(name, element));
        }
        return ingredients;
    }

    /** Reads one ingredient, which is an item name or a list of them. */
    private static Ingredient readIngredient(String name, JsonValue value) {
        if (value == null) {
            throw new IllegalArgumentException("The recipe '" + name + "' names no item");
        }
        List<Item> items = new ArrayList<>();
        if (value.isArray()) {
            for (JsonValue element : value) {
                items.add(readItem(name, element.asString()));
            }
        } else {
            items.add(readItem(name, value.asString()));
        }
        return Ingredient.of(items.toArray(new Item[0]));
    }

    /** Looks an item up by the name a recipe file uses. */
    private static Item readItem(String name, String itemName) {
        Item item = ItemRegistry.byName(itemName);
        if (item == null) {
            throw new IllegalArgumentException("The recipe '" + name
                    + "' names the unknown item '" + itemName + "'");
        }
        return item;
    }
}
