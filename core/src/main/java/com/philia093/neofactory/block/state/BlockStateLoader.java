package com.philia093.neofactory.block.state;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.philia093.neofactory.block.state.BlockStateTable.Variant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the states of the blocks from {@code assets/blockstates}.
 * <p>
 * One file per block, named like the block itself, so {@code Blocks.FURNACE} is described by
 * {@code assets/blockstates/furnace.json}:
 *
 * <pre>
 * { "properties": { "facing": ["north", "east", "south", "west"] },
 *   "variants": { "facing=north": { "model": "furnace", "y": 0 },
 *                 "facing=west":  { "model": "furnace", "y": 270 } } }
 * { "model": "grass" }
 * </pre>
 *
 * The second file is the short form: a block that is always drawn the same way names one model and
 * nothing else, which is what most of the blocks of the game are. A file that cannot be read is
 * reported and skipped, the way a broken recipe is, and the block falls back to the whole cube its
 * picture is.
 */
public final class BlockStateLoader {

    /** Folder below the asset root that holds the blockstate files. */
    public static final String FOLDER = "blockstates";

    /** Extension of a blockstate file. */
    public static final String EXTENSION = ".json";

    /** List of every asset path, written by the build next to the assets. */
    public static final String ASSET_LIST = "assets.txt";

    /** Key that stands for every state of a block. */
    private static final String ANY_STATE = "*";

    private static final Logger LOGGER = LogManager.getLogger();

    private BlockStateLoader() {
        // Utility class: never instantiated.
    }

    /**
     * Reads every blockstate file below {@code assets/blockstates} and registers it.
     *
     * @return amount of tables that were read
     */
    public static int loadAll() {
        FileHandle list = Gdx.files.internal(ASSET_LIST);
        if (!list.exists()) {
            LOGGER.error("The asset list '{}' is missing, no state of a block can be read",
                    ASSET_LIST);
            return 0;
        }
        Map<String, String> files = new LinkedHashMap<>();
        for (String path : assetPaths(list.readString("UTF-8"))) {
            files.put(nameOf(path), Gdx.files.internal(path).readString("UTF-8"));
        }
        LOGGER.info("Read {} blockstate files from {}", files.size(), ASSET_LIST);
        return BlockStateRegistry.registerAll(read(files));
    }

    /**
     * Reads every blockstate file below a checked out asset folder, used by the tests.
     *
     * @param assetsRoot root of the assets, the folder holding {@code assets.txt}
     * @return amount of tables that were read
     */
    public static int loadAllFrom(Path assetsRoot) {
        Map<String, String> files = new LinkedHashMap<>();
        try {
            for (String path : assetPaths(Files.readString(assetsRoot.resolve(ASSET_LIST),
                    StandardCharsets.UTF_8))) {
                files.put(nameOf(path), Files.readString(assetsRoot.resolve(path),
                        StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            LOGGER.error("Unable to read the blockstates below {}", assetsRoot, e);
            return 0;
        }
        return BlockStateRegistry.registerAll(read(files));
    }

    /** Paths of the blockstate files a list of asset paths holds, in the order they are listed. */
    private static List<String> assetPaths(String assetList) {
        List<String> paths = new ArrayList<>();
        for (String line : assetList.split("\\R")) {
            String path = line.trim();
            if (path.startsWith(FOLDER + "/") && path.endsWith(EXTENSION)) {
                paths.add(path);
            }
        }
        return paths;
    }

    /** Name of a blockstate path, which is the name of its file without the extension. */
    private static String nameOf(String path) {
        String file = path.substring(path.lastIndexOf('/') + 1);
        return file.substring(0, file.length() - EXTENSION.length());
    }

    /** Turns the text of every blockstate file into a table. */
    private static Map<String, BlockStateTable> read(Map<String, String> files) {
        Map<String, BlockStateTable> tables = new LinkedHashMap<>();
        for (Map.Entry<String, String> file : files.entrySet()) {
            try {
                tables.put(file.getKey(), parse(file.getKey(), file.getValue()));
            } catch (RuntimeException e) {
                LOGGER.error("The states of the block '{}' cannot be read and are skipped",
                        file.getKey(), e);
            }
        }
        return tables;
    }

    /**
     * Turns the text of one blockstate file into a table.
     *
     * @param blockName name of the block the file belongs to
     * @param json text of the file
     * @return the table
     * @throws IllegalArgumentException when the file cannot be read
     */
    public static BlockStateTable parse(String blockName, String json) {
        JsonValue root = new JsonReader().parse(json);
        Map<String, List<String>> values = readProperties(blockName, root.get("properties"));
        int states = 1;
        for (List<String> propertyValues : values.values()) {
            states *= propertyValues.size();
        }
        Variant[] variants = new Variant[states];
        JsonValue named = root.get("variants");
        if (named != null) {
            for (JsonValue variant = named.child; variant != null; variant = variant.next) {
                int state = stateOf(blockName, variant.name, values);
                variants[state] = new Variant(variant.getString("model", null),
                        variant.getInt("y", 0));
            }
        } else {
            // The short form: one model, every state the same.
            variants[0] = new Variant(root.getString("model", null), 0);
        }
        Variant fallback = Variant.NONE;
        for (Variant variant : variants) {
            if (variant != null) {
                fallback = variant;
                break;
            }
        }
        for (int state = 0; state < states; state++) {
            if (variants[state] == null) {
                // A file that names only some combinations leaves the rest at the first one it names,
                // so a table never answers with nothing.
                variants[state] = fallback;
            }
        }
        return new BlockStateTable(blockName, values, Arrays.asList(variants));
    }

    /** Reads the properties of a file, in the order they are written down. */
    private static Map<String, List<String>> readProperties(String blockName, JsonValue properties) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        if (properties == null) {
            return values;
        }
        for (JsonValue property = properties.child; property != null; property = property.next) {
            if (!property.isArray() || property.size == 0) {
                throw new IllegalArgumentException("The property '" + property.name + "' of '"
                        + blockName + "' is not a list of values");
            }
            List<String> names = new ArrayList<>();
            for (JsonValue value : property) {
                names.add(value.asString());
            }
            if (values.put(property.name, names) != null) {
                throw new IllegalArgumentException("The block '" + blockName + "' names the "
                        + "property '" + property.name + "' twice");
            }
        }
        return values;
    }

    /** The number of the state one key of a {@code variants} object stands for. */
    private static int stateOf(String blockName, String key, Map<String, List<String>> values) {
        if (key.isEmpty() || ANY_STATE.equals(key)) {
            return 0;
        }
        Map<String, String> named = new HashMap<>();
        for (String part : key.split(",")) {
            int separator = part.indexOf('=');
            if (separator <= 0) {
                throw new IllegalArgumentException("'" + part + "' is no value of a state, the "
                        + "block '" + blockName + "' names it");
            }
            named.put(part.substring(0, separator).trim(), part.substring(separator + 1).trim());
        }
        for (String property : named.keySet()) {
            if (!values.containsKey(property)) {
                throw new IllegalArgumentException("The block '" + blockName + "' carries no "
                        + "property '" + property + "'");
            }
        }
        int number = 0;
        for (Map.Entry<String, List<String>> property : values.entrySet()) {
            String wanted = named.get(property.getKey());
            int index = wanted == null ? 0 : property.getValue().indexOf(wanted);
            if (index < 0) {
                throw new IllegalArgumentException("The property '" + property.getKey() + "' of '"
                        + blockName + "' does not take the value '" + wanted + "'");
            }
            number = number * property.getValue().size() + index;
        }
        return number;
    }
}
