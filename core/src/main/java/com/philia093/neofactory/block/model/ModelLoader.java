package com.philia093.neofactory.block.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.philia093.neofactory.block.BlockFace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads the models of the blocks from {@code assets/models/block}.
 * <p>
 * A block is not one picture: it is a shape, and every face of that shape names the picture it
 * shows. The shape of a block is therefore a file of its own, which is what lets the grass show a
 * bright top over a side the colour of its biome is painted through and the furnace a plain top
 * over the mouth of its front:
 *
 * <pre>
 * models/block/grass.json        { "parent": "template_cube_bottom_top",
 *                                  "textures": { "side": "grass_side", "top": "grass_top",
 *                                                "bottom": "dirt",
 *                                                "side_overlay": "grass_side_overlay" },
 *                                  "tint": true }
 * models/block/oak_log.json      { "parent": "template_cube_column",
 *                                  "textures": { "side": "log_oak", "end": "log_oak_top" } }
 * models/block/tall_grass.json   { "parent": "template_cross",
 *                                  "textures": { "cross": "tallgrass" }, "tint": true }
 * </pre>
 *
 * <b>A model inherits from its parent.</b> {@code parent} names another file of the same folder; the
 * child takes its boxes and may name its own pictures, which is why a template is written once and a
 * block is a handful of lines. A template is a model like every other one: the loader makes no
 * difference between the two.
 * <p>
 * <b>A file that cannot be read is reported and skipped</b>, the way
 * {@link com.philia093.neofactory.recipe.RecipeLoader} does it with a recipe: one broken model never
 * keeps the game from starting, and the block it belonged to falls back to the whole cube
 * {@link com.philia093.neofactory.block.Block#texture()} names.
 * <p>
 * {@link #read(Map)} is plain text to models and can be checked without a window, see
 * {@code ModelLoaderTest}; {@link #loadAll()} reads the list the build writes next to the assets.
 */
public final class ModelLoader {

    /** Folder below the asset root that holds the models of the blocks. */
    public static final String FOLDER = "models/block";

    /** Extension of a model file. */
    public static final String EXTENSION = ".json";

    /** List of every asset path, written by the build next to the assets. */
    public static final String ASSET_LIST = "assets.txt";

    /** How deep a chain of parents may be before a file is called broken. */
    private static final int MAX_DEPTH = 16;

    private static final Logger LOGGER = LogManager.getLogger();

    private ModelLoader() {
        // Utility class: never instantiated.
    }

    /**
     * Reads every model below {@code assets/models/block} and registers it.
     *
     * @return amount of models that were read
     */
    public static int loadAll() {
        FileHandle list = Gdx.files.internal(ASSET_LIST);
        if (!list.exists()) {
            LOGGER.error("The asset list '{}' is missing, no model can be read", ASSET_LIST);
            return 0;
        }
        Map<String, String> files = new LinkedHashMap<>();
        for (String path : assetPaths(list.readString("UTF-8"))) {
            files.put(nameOf(path), Gdx.files.internal(path).readString("UTF-8"));
        }
        LOGGER.info("Read {} model files from {}", files.size(), ASSET_LIST);
        return ModelRegistry.registerAll(read(files));
    }

    /**
     * Reads every model below a checked out asset folder, used by the tests.
     *
     * @param assetsRoot root of the assets, the folder holding {@code assets.txt}
     * @return amount of models that were read
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
            LOGGER.error("Unable to read the models below {}", assetsRoot, e);
            return 0;
        }
        return ModelRegistry.registerAll(read(files));
    }

    /** Paths of the model files a list of asset paths holds, in the order they are listed. */
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

    /** Name of a model path, which is the name of its file without the extension. */
    private static String nameOf(String path) {
        String file = path.substring(path.lastIndexOf('/') + 1);
        return file.substring(0, file.length() - EXTENSION.length());
    }

    /**
     * Turns the text of every model file into a model.
     * <p>
     * The files are read twice: once for the shape a file inherits from its parent, and once for the
     * pictures its own faces name. That is what lets a child override the pictures of its parent -
     * the boxes of the parent are written down with the pictures of the child, which is the only way
     * a template can be shared by a stone and a grass block.
     *
     * @param files text of every model file, keyed by the name of the file without the extension
     * @return the models that could be read, keyed by their name
     */
    public static Map<String, BlockModel> read(Map<String, String> files) {
        Map<String, JsonValue> raw = new LinkedHashMap<>();
        for (Map.Entry<String, String> file : files.entrySet()) {
            try {
                raw.put(file.getKey(), new JsonReader().parse(file.getValue()));
            } catch (RuntimeException e) {
                LOGGER.error("The model '{}' is not valid JSON and is skipped", file.getKey(), e);
            }
        }
        Map<String, Shape> shapes = new LinkedHashMap<>();
        for (String name : raw.keySet()) {
            try {
                shapeOf(name, raw, shapes, new LinkedHashSet<>());
            } catch (RuntimeException e) {
                LOGGER.error("The model '{}' cannot be read and is skipped", name, e);
            }
        }
        Map<String, BlockModel> models = new LinkedHashMap<>();
        for (Map.Entry<String, Shape> shape : shapes.entrySet()) {
            if (shape.getValue().root().getBoolean("template", false)) {
                // A template is a shape other models inherit from; it names no pictures of its own, so
                // it is not a model a block could be drawn with, see the class comment.
                continue;
            }
            JsonValue elements = shape.getValue().elements();
            if (elements == null) {
                LOGGER.error("The model '{}' names no boxes at all and is skipped", shape.getKey());
                continue;
            }
            if (elements.size == 0) {
                // A model that names an empty list of boxes draws nothing, which is a shape of its own: a
                // pipe that is joined on every side is covered by its neighbours and has no face left to
                // draw, see Pipes. A file that forgot its boxes names no list at all and is a mistake,
                // which is what the error above is for.
                models.put(shape.getKey(), new BlockModel(shape.getKey(), List.of()));
                continue;
            }
            try {
                models.put(shape.getKey(), build(shape.getKey(), shape.getValue().root(),
                        elements, shape.getValue().textures()));
            } catch (RuntimeException e) {
                LOGGER.error("The boxes of the model '{}' cannot be read and are skipped",
                        shape.getKey(), e);
            }
        }
        return models;
    }

    /**
     * The shape a model file describes: the boxes it draws with and the pictures they name.
     * <p>
     * A file that names no boxes of its own takes the boxes of its parent, and a file that names no
     * pictures takes the pictures of its parent, so a template is written once and inherited.
     */
    private record Shape(JsonValue root, JsonValue elements, Map<String, String> textures) {
    }

    /** Resolves the shape of a model file, walking up its chain of parents. */
    private static Shape shapeOf(String name, Map<String, JsonValue> raw, Map<String, Shape> shapes,
            Set<String> visiting) {
        Shape known = shapes.get(name);
        if (known != null) {
            return known;
        }
        JsonValue json = raw.get(name);
        if (json == null) {
            throw new IllegalArgumentException("The model '" + name + "' does not exist");
        }
        if (!visiting.add(name)) {
            throw new IllegalArgumentException("The model '" + name + "' inherits from itself");
        }
        if (visiting.size() > MAX_DEPTH) {
            throw new IllegalArgumentException("The model '" + name + "' inherits from more than "
                    + MAX_DEPTH + " models");
        }
        String parentName = json.getString("parent", null);
        Shape parent = parentName == null ? null : shapeOf(parentName, raw, shapes, visiting);
        Map<String, String> textures = new LinkedHashMap<>();
        if (parent != null) {
            textures.putAll(parent.textures());
        }
        readTextures(json.get("textures"), textures);
        resolveTextures(textures);
        JsonValue elements = json.get("elements") != null ? json.get("elements")
                : (parent == null ? null : parent.elements());
        Shape shape = new Shape(json, elements, textures);
        shapes.put(name, shape);
        visiting.remove(name);
        return shape;
    }

    /** Reads the picture names of a model file into the map its parent started. */
    private static void readTextures(JsonValue textures, Map<String, String> into) {
        if (textures == null) {
            return;
        }
        for (JsonValue entry = textures.child; entry != null; entry = entry.next) {
            into.put(entry.name, entry.asString());
        }
    }

    /**
     * Replaces every name of a picture that points at another one.
     * <p>
     * A file writes {@code "side": "#all"} to hand a picture down to the faces of its parent, which
     * is how a child renames the one picture of a template without writing a box again.
     */
    private static void resolveTextures(Map<String, String> textures) {
        for (String key : List.copyOf(textures.keySet())) {
            String value = textures.get(key);
            int guard = 0;
            while (value != null && value.startsWith("#")) {
                value = textures.get(value.substring(1));
                if (++guard > MAX_DEPTH) {
                    throw new IllegalArgumentException("The picture '" + key + "' points at itself");
                }
            }
            if (value == null || value.isEmpty()) {
                throw new IllegalArgumentException("The picture '" + key + "' names no file");
            }
            textures.put(key, value);
        }
    }

    /** Reads the boxes of a model file. */
    private static BlockModel build(String name, JsonValue root, JsonValue elements,
            Map<String, String> textures) {
        if (!elements.isArray()) {
            throw new IllegalArgumentException("The boxes of '" + name + "' are not a list");
        }
        // The tint of a model is what the colour of the block is multiplied over: the sheets of the
        // grass and of the leaves hold brightness only, see Block#tint(). A face may name its own.
        boolean tinted = root.getBoolean("tint", false);
        List<ModelBox> boxes = new ArrayList<>();
        for (JsonValue element : elements) {
            boxes.add(readBox(name, element, textures, tinted));
        }
        return new BlockModel(name, boxes);
    }

    /** Reads one box of a model file. */
    private static ModelBox readBox(String name, JsonValue element, Map<String, String> textures,
            boolean tinted) {
        float[] from = readCorner(name, element.get("from"), "from");
        float[] to = readCorner(name, element.get("to"), "to");
        ModelBox box = new ModelBox(from[0], from[1], from[2], to[0], to[1], to[2],
                readTurn(name, element.get("rotation")), element.getBoolean("shade", true));
        JsonValue faces = element.get("faces");
        if (faces == null || faces.size == 0) {
            throw new IllegalArgumentException("A box of '" + name + "' names no face");
        }
        for (JsonValue face = faces.child; face != null; face = face.next) {
            BlockFace direction = BlockFace.byName(face.name);
            if (direction == null) {
                throw new IllegalArgumentException("'" + face.name + "' is no face of a box, the "
                        + "model '" + name + "' names it");
            }
            box.setFace(direction, readFace(name, face, textures, tinted));
        }
        return box;
    }

    /** Reads the picture and the window of one face. */
    private static ModelFace readFace(String name, JsonValue face, Map<String, String> textures,
            boolean tinted) {
        String picture = readPicture(name, face.getString("texture", null), textures);
        String overlayName = face.getString("overlay", null);
        String overlay = overlayName == null ? "" : readPicture(name, overlayName, textures);
        BlockFace cullface = null;
        String culled = face.getString("cullface", null);
        if (culled != null) {
            cullface = BlockFace.byName(culled);
            if (cullface == null) {
                throw new IllegalArgumentException("'" + culled + "' is no direction of a face, "
                        + "the model '" + name + "' culls with it");
            }
        }
        float[] window = readWindow(name, face.get("uv"));
        return new ModelFace(picture, overlay, window[0], window[1], window[2], window[3], cullface,
                face.getInt("rotation", 0), face.getBoolean("tint", tinted));
    }

    /**
     * Reads a picture name of a face.
     * <p>
     * A name behind a {@code #} points at the pictures the file named, which is how a template hands
     * the picture it was built for down to its boxes; every other name is the name of a file, either
     * relative to {@code blocks/} or carrying its own folder.
     */
    private static String readPicture(String name, String value, Map<String, String> textures) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("A face of '" + name + "' names no picture");
        }
        if (!value.startsWith("#")) {
            return value;
        }
        String picture = textures.get(value.substring(1));
        if (picture == null) {
            throw new IllegalArgumentException("The face of '" + name + "' names the picture '"
                    + value + "', which the file does not hold");
        }
        return picture;
    }

    /** Reads a window of a face, which is four numbers in sixteenths of a picture. */
    private static float[] readWindow(String name, JsonValue uv) {
        if (uv == null) {
            return new float[] {0.0f, 0.0f, 1.0f, 1.0f};
        }
        if (!uv.isArray() || uv.size != 4) {
            throw new IllegalArgumentException("The window of a face of '" + name
                    + "' is not four numbers");
        }
        float[] window = new float[4];
        for (int index = 0; index < 4; index++) {
            window[index] = uv.get(index).asFloat() / ModelBox.UNITS;
        }
        return window;
    }

    /** Reads the corner of a box, which is three numbers in sixteenths of a block. */
    private static float[] readCorner(String name, JsonValue corner, String what) {
        if (corner == null || !corner.isArray() || corner.size != 3) {
            throw new IllegalArgumentException("The '" + what + "' of a box of '" + name
                    + "' is not three numbers");
        }
        float[] point = new float[3];
        for (int axis = 0; axis < 3; axis++) {
            point[axis] = corner.get(axis).asFloat();
        }
        return point;
    }

    /** Reads the turn of a box, {@code null} when the box stands straight. */
    private static ModelBox.Rotation readTurn(String name, JsonValue rotation) {
        if (rotation == null) {
            return null;
        }
        float[] origin = readCorner(name, rotation.get("origin"), "origin of the turn");
        String axis = rotation.getString("axis", "y");
        if (axis.length() != 1) {
            throw new IllegalArgumentException("A box of '" + name + "' is turned around '" + axis
                    + "', which is no axis");
        }
        return new ModelBox.Rotation(origin[0], origin[1], origin[2], axis.charAt(0),
                rotation.getFloat("angle", 0.0f));
    }
}
