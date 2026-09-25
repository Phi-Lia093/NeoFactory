package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.ToolType;

/**
 * Mining rule that takes the hardness of a block, the mining level and the kind of the held tool into
 * account.
 * <p>
 * The times follow the original game: a block needs {@code hardness * 1.5 / speed} seconds when the
 * held tool may harvest it and five times as long when it may not, so a wrong tool is slow and leaves
 * nothing behind. A block whose hardness is negative can never be broken, and a block with hardness
 * zero breaks right away.
 * <p>
 * Two questions are asked of the tool, and both are answered by the kind of it, see {@link ToolType}.
 * The <b>kind</b> decides the speed: a pickaxe mines stone with its own speed while an axe mines it no
 * faster than a bare hand, and the same pair decides the other way round what happens under a trunk.
 * The <b>level</b> decides what a block hands over: a block that asks for a mining level hands its
 * item only to the kind of tool it names and only when that tool reaches the level, so a stone of
 * level {@code 1} wants an iron pickaxe and refuses an axe, a bare hand and every weaker tool. Either
 * way the block comes apart - what is lost is the item and never the break.
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
        float factor = canHarvest(block, tool) ? MATCHING_TOOL_FACTOR : WRONG_TOOL_FACTOR;
        return hardness * factor / speedOf(block, tool);
    }

    @Override
    public boolean canHarvest(Block block, ItemStack tool) {
        if (block.harvestLevel() <= 0) {
            // A block that asks for no level hands its item to a bare hand and to every kind of tool:
            // the kind only decides how fast it goes, see speedOf.
            return true;
        }
        if (tool.isEmpty()) {
            return false;
        }
        ToolType wanted = block.toolType();
        Item item = tool.item();
        if (wanted != null && item.toolType() != wanted) {
            return false;
        }
        return item.toolLevel() >= block.harvestLevel();
    }

    /**
     * Speed the held stack breaks blocks with.
     * <p>
     * The speed of a tool counts for the blocks of its own kind, whatever its level: an iron pickaxe
     * on obsidian is slow, because the block refuses its item, but it is still a pickaxe and not a
     * hand. Every other stack - a bare hand, the wrong kind of tool, a block held in the hand - reaches
     * {@link Item#HAND_MINING_SPEED}.
     *
     * @param block block that is mined
     * @param tool stack the player holds
     * @return the speed of the stack, or the speed of a bare hand
     */
    private static float speedOf(Block block, ItemStack tool) {
        ToolType wanted = block.toolType();
        if (tool.isEmpty() || wanted == null || tool.item().toolType() != wanted) {
            return Item.HAND_MINING_SPEED;
        }
        return tool.item().miningSpeed();
    }

    @Override
    public String toString() {
        return "HardnessMining";
    }
}
