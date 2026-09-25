package com.philia093.neofactory.block.model;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every model of the game, looked up by the name of the block that shows it.
 * <p>
 * The models are read once while the game starts, see {@link ModelLoader}, and are frozen right
 * after that: a model is shared by every cell of the world that shows it, so a later registration
 * would leave the world with two shapes for one block.
 * <p>
 * <b>A block without a model file is a whole cube of its picture.</b> {@link #of(Block)} falls back
 * to {@link ModelTemplates#wholeCube(String)}, which is the shape the flat engine drew every block
 * with: a picture on each of the six faces. A block that names no picture at all - air - is the
 * {@link BlockModel#EMPTY empty model} and shows nothing.
 */
public final class ModelRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Model of every name the loader read. */
    private static final Map<String, BlockModel> BY_NAME = new LinkedHashMap<>();

    /** Model of every block that was asked for, so the fallback is built once. */
    private static final Map<String, BlockModel> BY_BLOCK = new HashMap<>();

    private static boolean frozen;

    private ModelRegistry() {
        // Utility class: never instantiated.
    }

    /** Throws everything away, used when a world is left and the game starts over. */
    public static void clear() {
        BY_NAME.clear();
        BY_BLOCK.clear();
        frozen = false;
    }

    /**
     * Registers every model of a table.
     *
     * @param models models to register, keyed by their name
     * @return amount of models that were registered
     */
    public static int registerAll(Map<String, BlockModel> models) {
        int count = 0;
        for (Map.Entry<String, BlockModel> entry : models.entrySet()) {
            register(entry.getKey(), entry.getValue());
            count++;
        }
        return count;
    }

    /**
     * Adds one model.
     *
     * @param name name of the model, the name of its file without the extension
     * @param model model to register
     * @throws IllegalStateException when the table is frozen
     * @throws IllegalArgumentException when the name is used twice
     */
    public static void register(String name, BlockModel model) {
        if (frozen) {
            throw new IllegalStateException("The model table is frozen, cannot add " + name);
        }
        if (BY_NAME.put(name, model) != null) {
            throw new IllegalArgumentException("Two models are called '" + name + "'");
        }
    }

    /** Prevents any further registration, called once the models were read. */
    public static void freeze() {
        frozen = true;
        if (BY_NAME.isEmpty()) {
            LOGGER.warn("No model was read, every block is drawn as the cube of its picture");
        } else {
            LOGGER.info("Model table frozen with {} models", BY_NAME.size());
        }
    }

    /** {@code true} while the table refuses new models. */
    public static boolean isFrozen() {
        return frozen;
    }

    /**
     * Returns a model by name.
     *
     * @param name name of the model
     * @return the model, or {@code null} when no model uses that name
     */
    public static BlockModel byName(String name) {
        return name == null ? null : BY_NAME.get(name);
    }

    /**
     * The model a block is drawn with.
     * <p>
     * A block names its model by the name of the block itself, so {@code Blocks.FURNACE} is drawn
     * with {@code models/block/furnace.json}. A block without such a file is a whole cube of the
     * picture {@link Block#texture()} names, which is what most of the art pack is.
     *
     * @param block block to ask for
     * @return the model of that block, never {@code null}
     */
    public static BlockModel of(Block block) {
        BlockModel known = BY_BLOCK.get(block.name());
        if (known != null) {
            return known;
        }
        BlockModel model = BY_NAME.get(block.name());
        if (model == null) {
            model = ModelTemplates.wholeCube(block.texture(), hasTint(block));
            if (!model.isEmpty()) {
                LOGGER.debug("Block '{}' has no model file, it is drawn as the cube of its picture",
                        block.name());
            }
        }
        BY_BLOCK.put(block.name(), model);
        return model;
    }

    /** {@code true} when a block carries a colour of its own that its pictures are painted with. */
    private static boolean hasTint(Block block) {
        Color tint = block.tint();
        return tint.r != 1.0f || tint.g != 1.0f || tint.b != 1.0f;
    }

    /** Amount of models the table holds. */
    public static int count() {
        return BY_NAME.size();
    }

    /** Every model name, in the order the files were read. */
    public static List<String> names() {
        return List.copyOf(new ArrayList<>(BY_NAME.keySet()));
    }

    /** Every model the table holds, in the order the files were read. */
    public static List<BlockModel> all() {
        return List.copyOf(BY_NAME.values());
    }
}
