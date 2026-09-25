package com.philia093.neofactory.loot;

import com.philia093.neofactory.block.Block;

/**
 * Hands out the loot table of a block.
 * <p>
 * The game answers with the tables it read from {@code assets/loot_tables/blocks}, see
 * {@link LootTableRegistry}, while a test hands out a table of its own. A block that names no table is
 * answered with {@code null}, and the break then hands over the item of the block itself.
 */
@FunctionalInterface
public interface LootTableSource {

    /**
     * Loot table of a block.
     *
     * @param block block that was broken
     * @return its table, or {@code null} when the block has none and drops itself
     */
    LootTable of(Block block);
}
