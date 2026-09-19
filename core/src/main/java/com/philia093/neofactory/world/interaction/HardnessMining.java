package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;

/**
 * Mining rule that takes hardness, mining level and tool speed into account.
 * <p>
 * The rule is written and tested, but the game runs {@link InstantMining} for now:
 * pass an instance of this class to the {@link MiningController} to make breaks
 * take time.
 * <p>
 * The times follow the original game: a block needs
 * {@code hardness * 1.5 / speed} seconds when the tool may harvest it and five
 * times as long when it may not, so a wrong tool is slow and leaves nothing
 * behind. A block whose hardness is negative can never be broken, and a block
 * with hardness zero breaks right away.
 */
public final class HardnessMining implements MiningRule {

    /** Factor applied when the held tool is good enough. */
    private static final float MATCHING_TOOL_FACTOR = 1.5f;

    /** Factor applied when the held tool is too weak or missing. */
    private static final float WRONG_TOOL_FACTOR = 5.0f;

    /** Value reported for a block that cannot be broken. */
    private static final float UNBREAKABLE = -1.0f;

    @Override
    public float breakSeconds(Block block, ItemStack tool) {
        float hardness = block.hardness();
        if (hardness < 0.0f) {
            return UNBREAKABLE;
        }
        if (hardness == 0.0f) {
            return 0.0f;
        }
        boolean harvesting = canHarvest(block, tool);
        float factor = harvesting ? MATCHING_TOOL_FACTOR : WRONG_TOOL_FACTOR;
        return hardness * factor / speedOf(tool, harvesting);
    }

    @Override
    public boolean canHarvest(Block block, ItemStack tool) {
        if (block.harvestLevel() <= 0) {
            return true;
        }
        return !tool.isEmpty() && tool.item().toolLevel() >= block.harvestLevel();
    }

    /**
     * Speed the held stack breaks blocks with.
     *
     * @param tool stack the player holds
     * @param harvesting {@code true} when the tool is good enough for the block
     * @return the speed of the item, or the speed of a bare hand
     */
    private static float speedOf(ItemStack tool, boolean harvesting) {
        if (tool.isEmpty() || !harvesting) {
            return Item.HAND_MINING_SPEED;
        }
        return tool.item().miningSpeed();
    }

    @Override
    public String toString() {
        return "HardnessMining";
    }
}
