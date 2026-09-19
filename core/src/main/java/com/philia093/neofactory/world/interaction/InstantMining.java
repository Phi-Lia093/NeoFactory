package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.item.ItemStack;

/**
 * Mining rule that is active while the game is being built.
 * <p>
 * Every block disappears the moment it is hit, which keeps testing the world,
 * the inventory and the drops simple. Only a block that cannot be broken is
 * respected: an unbreakable block uses a negative hardness, which is how bedrock
 * and water are declared, see {@code Blocks}.
 * <p>
 * {@link HardnessMining} is the rule that replaces this one once mining should
 * take time; nothing else has to change for that.
 */
public final class InstantMining implements MiningRule {

    /** Value reported for a block that cannot be broken. */
    private static final float UNBREAKABLE = -1.0f;

    /** Value reported for a block that breaks right away. */
    private static final float INSTANT = 0.0f;

    @Override
    public float breakSeconds(Block block, ItemStack tool) {
        return block.hardness() < 0.0f ? UNBREAKABLE : INSTANT;
    }

    @Override
    public boolean canHarvest(Block block, ItemStack tool) {
        return block.hardness() >= 0.0f;
    }

    @Override
    public String toString() {
        return "InstantMining";
    }
}
