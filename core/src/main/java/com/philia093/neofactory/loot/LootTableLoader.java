package com.philia093.neofactory.loot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the loot tables of the game from {@code assets/loot_tables/blocks}.
 * <p>
 * A table is a small JSON file named after the block it belongs to, so the file itself is the whole link
 * between a block and what it leaves behind:
 *
 * <pre>
 * loot_tables/blocks/stone.json       { "drops": [ { "item": "cobblestone" } ] }
 * loot_tables/blocks/clay.json        { "drops": [ { "item": "clay_ball", "count": 4 } ] }
 * loot_tables/blocks/gravel.json      { "drops": [ { "item": "gravel" },
 *                                                  { "item": "flint", "chance": 0.1 } ] }
 * loot_tables/blocks/glass.json       { "drops": [] }
 * </pre>
 *
 * <b>A block without a file drops itself.</b> Only the blocks that need something else - the stone that
 * hands over cobblestone, glass that breaks into nothing - carry a file, and every other block is left out
 * of the folder entirely, see {@link LootTableRegistry}.
 * <p>
 * {@link #parse(String, String)} is plain text to break work and can be checked without a window, see
 * {@code LootTableLoaderTest}; {@link #loadAll()} reads the list the build writes next to the assets and
 * registers every file it names. A file that cannot be read is logged and skipped, so one broken table
 * never keeps the game from starting.
 */
public final class LootTableLoader {

    /** Folder below the asset root that holds the tables, one file per block. */
    public static final String FOLDER = "loot_tables/blocks";

    /** Extension of a table file. */
    public static final String EXTENSION = ".json";

    /** List of every asset path, written by the build next to the assets. */
    public static final String ASSET_LIST = "assets.txt";

    private static final Logger LOGGER = LogManager.getLogger();

    private LootTableLoader() {
        // Utility class: never instantiated.
    }

    /**
     * Reads every table below {@code assets/loot_tables/blocks} and registers it.
     *
     * @return amount of tables that were read
     */
    public static int loadAll() {
        LootTableRegistry.clear();
        FileHandle list = Gdx.files.internal(ASSET_LIST);
        if (!list.exists()) {
            LOGGER.error("The asset list '{}' is missing, no loot table can be read", ASSET_LIST);
            return 0;
        }
        int loaded = 0;
        for (String path : tablePaths(list.readString("UTF-8"))) {
            try {
                register(nameOf(path),
                        parse(nameOf(path), Gdx.files.internal(path).readString("UTF-8")));
                loaded++;
            } catch (RuntimeException e) {
                LOGGER.error("The loot table '{}' cannot be read and is left out", path, e);
            }
        }
        LOGGER.info("Read {} loot tables from {}", loaded, FOLDER);
        return loaded;
    }

    /**
     * Reads every table below a checked out asset folder, used by the tests.
     *
     * @param assetsRoot root of the assets, the folder holding {@code assets.txt}
     * @return amount of tables that were read
     */
    public static int loadAllFrom(Path assetsRoot) {
        LootTableRegistry.clear();
        int loaded = 0;
        try {
            for (String path : tablePaths(Files.readString(assetsRoot.resolve(ASSET_LIST),
                    StandardCharsets.UTF_8))) {
                try {
                    register(nameOf(path), parse(nameOf(path),
                            Files.readString(assetsRoot.resolve(path), StandardCharsets.UTF_8)));
                    loaded++;
                } catch (RuntimeException e) {
                    LOGGER.error("The loot table '{}' cannot be read and is left out", path, e);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Unable to read the loot tables below {}", assetsRoot, e);
            return 0;
        }
        return loaded;
    }

    /**
     * Turns the text of one table file into a table.
     *
     * @param name name of the table, which is the name of the block it belongs to
     * @param json text of the file
     * @return the table
     * @throws IllegalArgumentException when the file names no item or numbers that make no sense
     */
    public static LootTable parse(String name, String json) {
        JsonValue root = new JsonReader().parse(json);
        JsonValue drops = root == null ? null : root.get("drops");
        if (drops == null) {
            throw new IllegalArgumentException("The loot table '" + name + "' names no drops");
        }
        if (!drops.isArray()) {
            throw new IllegalArgumentException("The drops of '" + name + "' have to be a list");
        }
        List<LootTable.Drop> entries = new ArrayList<>(drops.size);
        for (JsonValue drop : drops) {
            entries.add(readDrop(name, drop));
        }
        return new LootTable(entries);
    }

    /** Reads one line of a table: an item, how many of it and how often. */
    private static LootTable.Drop readDrop(String name, JsonValue drop) {
        JsonValue named = drop.get("item");
        if (named == null) {
            throw new IllegalArgumentException("A drop of '" + name + "' names no item");
        }
        Item item = ItemRegistry.byName(named.asString());
        if (item == null) {
            throw new IllegalArgumentException("The loot table '" + name + "' names the unknown item '"
                    + named.asString() + "'");
        }
        int min = 1;
        int max = 1;
        JsonValue count = drop.get("count");
        if (count != null && count.isArray()) {
            if (count.size != 2) {
                throw new IllegalArgumentException("The count of a drop of '" + name
                        + "' is one number or a range of two, not " + count.size + " numbers");
            }
            min = count.get(0).asInt();
            max = count.get(1).asInt();
        } else if (count != null) {
            min = count.asInt();
            max = min;
        }
        return new LootTable.Drop(item, min, max, drop.getFloat("chance", 1.0f));
    }

    /** Registers a table under the name of its block. */
    private static void register(String blockName, LootTable table) {
        LootTableRegistry.register(blockName, table);
    }

    /** Paths of the table files a list of asset paths holds, in the order they are listed. */
    private static List<String> tablePaths(String assetList) {
        List<String> paths = new ArrayList<>();
        for (String line : assetList.split("\\R")) {
            String path = line.trim();
            if (path.startsWith(FOLDER + "/") && path.endsWith(EXTENSION)) {
                paths.add(path);
            }
        }
        return paths;
    }

    /** Name of a table path, which is the name of its file without the extension. */
    private static String nameOf(String path) {
        String file = path.substring(path.lastIndexOf('/') + 1);
        return file.substring(0, file.length() - EXTENSION.length());
    }
}
