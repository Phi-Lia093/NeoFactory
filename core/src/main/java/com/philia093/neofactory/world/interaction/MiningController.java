package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemDrops;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.World;

import java.util.Objects;

/**
 * Breaks the targeted block while the mouse button is held.
 * <p>
 * The controller counts the time the player keeps breaking the same cell and asks
 * the {@link MiningRule} how long that block needs. Aiming somewhere else or
 * letting the button go starts the count again, which is what the original game
 * does as well.
 * <p>
 * How long a break may take at the least is decided here as well: a rule that
 * breaks instantly would otherwise remove a whole row of blocks in one frame, so
 * a held button waits {@link Constants#MINIMUM_BREAK_TIME} between two blocks. A
 * rule that needs more time for a block is never sped up by that value.
 * <p>
 * Because the game has no dropped item entities yet the items a block hands back
 * are passed to an {@link ItemDrops}, see {@link com.philia093.neofactory.item.InventoryDrops}.
 */
public class MiningController {

    private final MiningRule rule;
    private final ItemDrops drops;

    /** Cell the collected time belongs to, {@code null} while nothing is mined. */
    private BlockTarget target;

    /** Seconds the player has spent on the current cell. */
    private float elapsed;

    /** Seconds the current cell needs, used to report the progress. */
    private float required;

    /**
     * Creates a controller.
     *
     * @param rule rule deciding how long a block takes and what it drops
     * @param drops sink receiving the items of a broken block
     */
    public MiningController(MiningRule rule, ItemDrops drops) {
        this.rule = Objects.requireNonNull(rule, "rule");
        this.drops = Objects.requireNonNull(drops, "drops");
    }

    /**
     * Advances the break of one frame.
     *
     * @param delta time since the last frame in seconds
     * @param world world holding the block
     * @param target cell the player aims at, {@code null} while nothing is targeted
     * @param tool stack the player holds, {@link ItemStack#EMPTY} for a bare hand
     * @param breaking {@code true} while the mouse button is held
     * @return {@code true} when this frame broke a block
     */
    public boolean update(float delta, World world, BlockTarget target, ItemStack tool,
            boolean breaking) {
        if (!breaking || target == null) {
            reset();
            return false;
        }
        if (!target.equals(this.target)) {
            // A new cell starts from zero, no matter how far the last one was.
            this.target = target;
            elapsed = 0.0f;
            required = 0.0f;
        }

        Block block = world.getBlock(target.x(), target.y(), target.layer());
        if (block.isAir()) {
            reset();
            return false;
        }
        if (target.layer() == Chunk.LAYER_FLOOR && world.hasObjectBlock(target.x(), target.y())) {
            // The layer the player stands in is still occupied: the ground below it is
            // out of reach until that block is gone, a break may never skip the layer
            // the player is in.
            reset();
            return false;
        }

        float needed = rule.breakSeconds(block, tool);
        if (needed < 0.0f) {
            // Unbreakable blocks collect no progress at all.
            elapsed = 0.0f;
            required = 0.0f;
            return false;
        }

        required = Math.max(needed, Constants.MINIMUM_BREAK_TIME);
        elapsed += delta;
        if (elapsed < required) {
            return false;
        }

        breakBlock(world, target, block, tool);
        elapsed = 0.0f;
        return true;
    }

    /** Progress of the current break, {@code 0} to {@code 1}. */
    public float progress() {
        if (required <= 0.0f) {
            return 0.0f;
        }
        return Math.min(1.0f, elapsed / required);
    }

    /** Rule this controller asks for the time a block needs. */
    public MiningRule rule() {
        return rule;
    }

    /** Forgets the cell and its progress, called when mining stops. */
    public void reset() {
        target = null;
        elapsed = 0.0f;
        required = 0.0f;
    }

    /**
     * Removes a block from the world and hands its item to the drop sink.
     *
     * @param world world to change
     * @param target cell that is broken
     * @param block block that was stored there
     * @param tool stack the player holds
     */
    private void breakBlock(World world, BlockTarget target, Block block, ItemStack tool) {
        world.setBlock(target.x(), target.y(), target.layer(), Blocks.AIR);
        if (!rule.canHarvest(block, tool)) {
            return;
        }
        Item item = ItemRegistry.byBlock(block);
        if (item != null) {
            drops.drop(ItemStack.of(item, 1), target.centerX(), target.centerY());
        }
    }

    @Override
    public String toString() {
        return "MiningController(" + rule + ", progress " + progress() + ")";
    }
}
