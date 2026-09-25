package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.blockentity.BlockEntity;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemDrops;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.ItemWear;
import com.philia093.neofactory.loot.LootTable;
import com.philia093.neofactory.loot.LootTableSource;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.World;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Breaks the targeted block while the mouse button is held.
 * <p>
 * The controller counts the time the player keeps breaking the same cell and asks
 * the {@link MiningRule} how long that block needs. Aiming somewhere else or
 * letting the button go starts the count again, which is what the original game
 * does as well.
 * <p>
 * Any cell the line of sight reaches can be broken, whatever face the line entered it through and
 * whatever stands on or beside it: a wall comes apart from the side as well as from above, and the
 * ground under a column is carried away after the blocks over it. The view from above could ask for
 * the cell over the target to be empty first, because there the top of a column was the only cell a
 * player could ever name; in a world of cubes that rule would leave nothing but the surface
 * breakable. What a break removes is only ever the cell itself - a block that loses its support
 * stays where it is, because nothing here falls yet.
 * <p>
 * How long a break may take at the least is decided here as well: a rule that
 * breaks instantly would otherwise remove a whole row of blocks in one frame, so
 * the blocks of one press wait {@link Constants#MINIMUM_BREAK_TIME} between each
 * other, while the first block of a press is not held back by it: a click breaks the
 * block it names right away, whatever the rule says about time. A rule that needs
 * more time for a block is never sped up by either value.
 * <p>
 * Because the game has no dropped item entities yet the items a block hands back
 * are passed to an {@link ItemDrops}, see {@link com.philia093.neofactory.item.InventoryDrops}.
 * <p>
 * What a block hands over is asked of its loot table, and a block that names none hands over itself, see
 * {@link com.philia093.neofactory.loot.LootTable}. Whether anything is handed over at all is decided by
 * the rule: a tool that is too weak for a block still breaks it, it only leaves nothing behind.
 * <p>
 * <b>A block costs its tool one use.</b> The stack a harvest was done with is worn down by one, and a
 * piece whose life is gone is reported to an {@link ItemWear} sink, which is how a tool of the player
 * disappears instead of staying in the hand. A block that could not be harvested costs nothing, which
 * keeps a wrong tool from being worn away by stone, see {@code HardnessMining}.
 */
public class MiningController {

    private final MiningRule rule;
    private final ItemDrops drops;
    private final LootTableSource loot;
    private final ItemWear wear;

    /** Source of the amounts and of the chances of a loot table, used for every break. */
    private final Random random = new Random();

    /** Cell the collected time belongs to, {@code null} while nothing is mined. */
    private BlockTarget target;

    /** Seconds the player has spent on the current cell. */
    private float elapsed;

    /** Seconds the current cell needs, used to report the progress. */
    private float required;

    /** {@code true} while the button is held down, which is what a press lasts from frame to frame. */
    private boolean pressed;

    /**
     * Creates a controller whose blocks hand over the item of the block itself.
     *
     * @param rule rule deciding how long a block takes and whether it may be harvested
     * @param drops sink receiving the items of a broken block
     */
    public MiningController(MiningRule rule, ItemDrops drops) {
        this(rule, drops, block -> null);
    }

    /**
     * Creates a controller.
     *
     * @param rule rule deciding how long a block takes and whether it may be harvested
     * @param drops sink receiving the items of a broken block
     * @param loot tables saying what a block leaves behind, {@code null} for a block that names none
     */
    public MiningController(MiningRule rule, ItemDrops drops, LootTableSource loot) {
        this(rule, drops, loot, stack -> {
            // A piece that is used up is forgotten: without a sink there is no slot to clear, see ItemWear.
        });
    }

    /**
     * Creates a controller that wears the tool of the player down.
     *
     * @param rule rule deciding how long a block takes and whether it may be harvested
     * @param drops sink receiving the items of a broken block
     * @param loot tables saying what a block leaves behind, {@code null} for a block that names none
     * @param wear sink told about a tool that is used up, see {@link ItemWear}
     */
    public MiningController(MiningRule rule, ItemDrops drops, LootTableSource loot, ItemWear wear) {
        this.rule = Objects.requireNonNull(rule, "rule");
        this.drops = Objects.requireNonNull(drops, "drops");
        this.loot = Objects.requireNonNull(loot, "loot");
        this.wear = Objects.requireNonNull(wear, "wear");
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

        Block block = world.getBlock(target.x(), target.y(), target.z());
        if (block.isAir()) {
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

        // The first block of a press goes as soon as the rule allows it, so a click in creative mode breaks
        // the block it names on the spot. Every block after it waits at least MINIMUM_BREAK_TIME, because a
        // rule that needs no time at all would otherwise eat a whole row of blocks in one frame.
        float floor = pressed ? Constants.MINIMUM_BREAK_TIME : 0.0f;
        required = Math.max(needed, floor);
        pressed = true;
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
        // The press is over with the break, so the next one starts over at the first block, see #update.
        pressed = false;
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
        // What a machine holds is handed over before the block goes: the entity is
        // removed with the block, see World#setBlock(int, int, int, Block), so its
        // content has to leave first or it would be lost with it.
        BlockEntity entity = world.blockEntity(target.x(), target.y(), target.z());
        if (entity != null) {
            entity.onBroken(drops, target.centerX(), target.centerZ());
        }
        world.setBlock(target.x(), target.y(), target.z(), Blocks.AIR);
        if (!rule.canHarvest(block, tool)) {
            return;
        }
        for (ItemStack drop : dropsOf(block)) {
            drops.drop(drop, target.centerX(), target.centerZ());
        }
        wearDown(tool);
    }

    /**
     * Takes one use from the tool a block was harvested with.
     * <p>
     * The stack is the very one the player carries, so the damage lands in the inventory on its own.
     * A piece that reaches the end of its life is reported to the {@link ItemWear} sink and leaves
     * the hand of the player, see {@link com.philia093.neofactory.item.Damageable#isBroken()}. A
     * block that was not harvested costs nothing - not the item of the block, which is lost anyway,
     * and not the tool either, which is what the original game does as well; a piece that never
     * wears out, such as a bare hand, is not touched by this.
     *
     * @param tool stack the player holds
     */
    private void wearDown(ItemStack tool) {
        if (tool.isEmpty() || !tool.isDamageable()) {
            return;
        }
        if (tool.applyDamage(1) > 0 && tool.isBroken()) {
            wear.wornOut(tool);
        }
    }

    /**
     * What a broken block leaves behind.
     * <p>
     * A block that names a loot table hands over what that table rolls, so a stone can hand over
     * cobblestone, an ore its material and glass nothing at all, see {@link LootTable}. Every other block
     * hands over itself, which is the item of its own block, and a block the game has no item for - air, a
     * fluid - leaves nothing behind.
     *
     * @param block block that was broken
     * @return the stacks the block hands over, empty for a block that leaves nothing
     */
    private List<ItemStack> dropsOf(Block block) {
        LootTable table = loot.of(block);
        if (table != null) {
            return table.roll(random);
        }
        Item item = ItemRegistry.byBlock(block);
        return item == null ? List.of() : List.of(ItemStack.of(item, 1));
    }

    @Override
    public String toString() {
        return "MiningController(" + rule + ", progress " + progress() + ")";
    }
}
