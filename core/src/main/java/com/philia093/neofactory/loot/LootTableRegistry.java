package com.philia093.neofactory.loot;

import com.badlogic.gdx.utils.IntMap;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

/**
 * The loot tables the game knows, one per block that has one.
 * <p>
 * The table is filled once during startup by {@link LootTableLoader} and only read after that, from the
 * break of a block, see {@code MiningController}. A table is filed under the block it belongs to and
 * looked up by that block, so neither a file nor a caller ever handles a number that only the code
 * could understand.
 * <p>
 * <b>Most blocks have no table.</b> The registry only holds the blocks that name a file, and a break of
 * any other block hands over the item of that block, which is what makes the fallback of the game the
 * absence of a table rather than a table that says so.
 */
public final class LootTableRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Table of every block that names one, keyed by block id. */
    private static final IntMap<LootTable> BY_BLOCK = new IntMap<>();

    private LootTableRegistry() {
        // Utility class: never instantiated.
    }

    /**
     * Adds the table of a block.
     *
     * @param blockName name of the block the table belongs to, the name its file carries
     * @param table table of that block
     * @throws IllegalArgumentException when the game has no block of that name, which is what a broken
     *                                  file names and what {@link LootTableLoader} reports
     */
    public static void register(String blockName, LootTable table) {
        Block block = BlockRegistry.byName(blockName);
        if (block == null) {
            throw new IllegalArgumentException("The loot table of '" + blockName
                    + "' names no block of the game");
        }
        BY_BLOCK.put(block.id(), Objects.requireNonNull(table, "table"));
    }

    /**
     * Table of a block.
     *
     * @param block block that was broken
     * @return its table, or {@code null} when the block has none and hands over itself
     */
    public static LootTable byBlock(Block block) {
        return block == null ? null : BY_BLOCK.get(block.id());
    }

    /** Amount of blocks that name a table. */
    public static int count() {
        return BY_BLOCK.size;
    }

    /** Logs how many tables were read, called once startup is finished. */
    public static void logStatistics() {
        LOGGER.info("{} blocks name a loot table, every other block drops itself", count());
    }

    /** Empties the table, used when a game is thrown away or a test starts over. */
    public static void clear() {
        BY_BLOCK.clear();
    }
}
