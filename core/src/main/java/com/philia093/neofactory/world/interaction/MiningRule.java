package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.item.ItemStack;

/**
 * Decides how long a block takes to break and whether it hands its item back.
 * <p>
 * The game starts with {@link InstantMining}, which breaks every block at once.
 * Hardness, mining level and the speed of the tool are already modelled, see
 * {@link HardnessMining}: that rule is written and tested, it only has to be
 * handed to the {@link MiningController} to take over.
 * <p>
 * Both rules are asked through this interface, so a rule can be replaced without
 * touching the controller, the screen or the game loop.
 */
public interface MiningRule {

    /**
     * Time a block needs to break.
     *
     * @param block block that is being broken
     * @param tool stack the player holds, {@link ItemStack#EMPTY} for a bare hand
     * @return the time in seconds, or a negative value when the block cannot be
     *         broken at all
     */
    float breakSeconds(Block block, ItemStack tool);

    /**
     * Whether breaking a block with this tool hands its item back.
     * <p>
     * A block that needs a tool the player does not carry still disappears, it
     * only leaves nothing behind.
     *
     * @param block block that is being broken
     * @param tool stack the player holds, {@link ItemStack#EMPTY} for a bare hand
     * @return {@code true} when the block drops its item
     */
    boolean canHarvest(Block block, ItemStack tool);
}
