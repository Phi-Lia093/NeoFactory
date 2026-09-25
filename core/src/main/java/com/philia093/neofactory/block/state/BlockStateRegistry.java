package com.philia093.neofactory.block.state;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.model.BlockModel;
import com.philia093.neofactory.block.model.ModelRegistry;
import com.philia093.neofactory.block.state.BlockStateTable.Variant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The states of every block of the game, looked up by the name of the block.
 * <p>
 * The tables are read once while the game starts, see {@link BlockStateLoader}, and are frozen
 * right after that: a cell of a stored world carries the number of a state, so a table that changed
 * after the world was read would draw an old world with a new shape.
 * <p>
 * <b>A block without a table is always drawn the same way.</b> {@link #shown(Block, int)} answers
 * with the model {@link ModelRegistry#of(Block)} picks and no turn, which is what the state of a
 * block that carries none means.
 */
public final class BlockStateRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Table of every block that has one, keyed by the name of the block. */
    private static final Map<String, BlockStateTable> BY_BLOCK = new LinkedHashMap<>();

    /** Models a missing name was reported for, so the log holds one line per name. */
    private static final Set<String> reported = new HashSet<>();

    /** What one state of a block shows. */
    public record Shown(BlockModel model, int rotateY) {

        /** {@code true} when the model is turned by a quarter turn around its vertical axis. */
        public boolean isTurned() {
            return rotateY != 0;
        }
    }

    private static boolean frozen;

    private BlockStateRegistry() {
        // Utility class: never instantiated.
    }

    /** Throws everything away, used when the game starts over. */
    public static void clear() {
        BY_BLOCK.clear();
        reported.clear();
        frozen = false;
    }

    /**
     * Registers every table of a list.
     *
     * @param tables tables to register, keyed by the name of their block
     * @return amount of tables that were registered
     */
    public static int registerAll(Map<String, BlockStateTable> tables) {
        int count = 0;
        for (Map.Entry<String, BlockStateTable> entry : tables.entrySet()) {
            register(entry.getKey(), entry.getValue());
            count++;
        }
        return count;
    }

    /**
     * Adds one table.
     *
     * @param blockName name of the block the table describes
     * @param table table to register
     * @throws IllegalStateException when the table is frozen
     * @throws IllegalArgumentException when the block is written down twice
     */
    public static void register(String blockName, BlockStateTable table) {
        if (frozen) {
            throw new IllegalStateException("The blockstate table is frozen, cannot add "
                    + blockName);
        }
        if (BY_BLOCK.put(blockName, table) != null) {
            throw new IllegalArgumentException("Two blockstate files describe the block '"
                    + blockName + "'");
        }
    }

    /** Prevents any further registration, called once the tables were read. */
    public static void freeze() {
        frozen = true;
        LOGGER.info("Blockstate table frozen with {} blocks that carry a state", BY_BLOCK.size());
    }

    /**
     * The table of a block.
     *
     * @param block block to ask for
     * @return the table, or {@link BlockStateTable#NONE} when the block carries no state
     */
    public static BlockStateTable of(Block block) {
        return tableOf(block.name());
    }

    /**
     * The table of a block, by name.
     *
     * @param blockName name of the block
     * @return the table, or {@link BlockStateTable#NONE} when no file describes that block
     */
    public static BlockStateTable tableOf(String blockName) {
        BlockStateTable table = blockName == null ? null : BY_BLOCK.get(blockName);
        return table == null ? BlockStateTable.NONE : table;
    }

    /**
     * The model a state of a block is drawn with.
     *
     * @param block block to draw
     * @param state number of the state, {@code 0} for a block that carries none
     * @return the model and the quarter turns around the vertical axis of the block
     */
    public static Shown shown(Block block, int state) {
        Variant variant = of(block).variant(state);
        if (variant.model() == null) {
            return new Shown(ModelRegistry.of(block), variant.rotateY());
        }
        BlockModel model = ModelRegistry.byName(variant.model());
        if (model == null) {
            if (reported.add(variant.model())) {
                LOGGER.warn("The state of '{}' names the model '{}', which no file holds: the "
                        + "block is drawn as the cube of its picture", block.name(),
                        variant.model());
            }
            return new Shown(ModelRegistry.of(block), variant.rotateY());
        }
        return new Shown(model, variant.rotateY());
    }

    /** Amount of blocks that carry a state. */
    public static int count() {
        return BY_BLOCK.size();
    }

    /** Every table, in the order the files were read. */
    public static List<BlockStateTable> all() {
        return List.copyOf(BY_BLOCK.values());
    }
}
