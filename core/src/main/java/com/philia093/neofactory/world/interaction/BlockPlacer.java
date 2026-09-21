package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.blockentity.BlockEntity;
import com.philia093.neofactory.blockentity.BlockEntityRegistry;
import com.philia093.neofactory.blockentity.BlockEntityType;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Builds the held block into the world.
 * <p>
 * Only a block item can be built, see
 * {@link com.philia093.neofactory.item.BlockItem}, and only into an empty cell:
 * an existing block must be broken first, exactly like the original game behaves.
 * <p>
 * A build never skips a layer. The ground below the feet can only be filled while
 * the layer the player stands in is empty, and the layer the player stands in can
 * only be built on while it has ground below: above a hole a block would float in
 * the air, which is refused.
 * <p>
 * A block that would end up inside the player is refused as well. The object layer
 * is the layer the player stands in, so building into the own cell would trap the
 * player inside a wall; the check runs on the changed world and undoes the write
 * when the player no longer fits.
 */
public final class BlockPlacer {

    private static final Logger LOGGER = LogManager.getLogger();

    private BlockPlacer() {
        // Utility class: never instantiated.
    }

    /**
     * Builds one item of the held stack into the targeted cell.
     *
     * @param world world to change
     * @param player player that must stay able to move
     * @param target cell to build into
     * @param inventory inventory holding the stack the player carries
     * @return {@code true} when the world changed and one item was used up
     */
    public static boolean place(World world, Player player, BlockTarget target,
            PlayerInventory inventory) {
        if (target == null) {
            return false;
        }
        ItemStack held = inventory.heldStack();
        if (held.isEmpty()) {
            return false;
        }
        Block block = held.item().block();
        if (block == null || !block.isDrawable()) {
            // A material, a tool or a block that is never drawn, such as water.
            return false;
        }
        if (!world.getBlock(target.x(), target.y(), target.layer()).isAir()) {
            return false;
        }
        if (target.layer() == Chunk.LAYER_FLOOR) {
            if (world.hasObjectBlock(target.x(), target.y())) {
                // The layer the player stands in is occupied: the ground below it is
                // out of reach until that block is removed, building may never skip
                // the layer the player is in.
                return false;
            }
        } else if (!world.hasGround(target.x(), target.y())) {
            // Above a hole there is nothing to build on, the block would float.
            return false;
        }

        world.setBlock(target.x(), target.y(), target.layer(), block);
        if (player.collides(world, player.position().x, player.position().y)) {
            world.setBlock(target.x(), target.y(), target.layer(), Blocks.AIR);
            return false;
        }

        placeBlockEntity(world, target, block);
        useOneItem(inventory, held);
        return true;
    }

    /**
     * Puts the block entity a block carries behind it.
     * <p>
     * A machine without its entity would stand there and do nothing, so the entity is
     * created while the block is built, see {@link Block#blockEntityTypeName()}. A block
     * that names a type the game does not know is reported and left without one, which
     * keeps a typo in the block table from stopping the game.
     *
     * @param world world that received the block
     * @param target cell that was built
     * @param block block that was stored there
     */
    private static void placeBlockEntity(World world, BlockTarget target, Block block) {
        if (!block.hasBlockEntity()) {
            return;
        }
        BlockEntityType type = BlockEntityRegistry.byName(block.blockEntityTypeName());
        if (type == null) {
            LOGGER.warn("The block '{}' names the unknown block entity '{}', it stays empty",
                    block.name(), block.blockEntityTypeName());
            return;
        }
        BlockEntity entity = type.create();
        entity.setPosition(target.x(), target.y(), target.layer());
        world.addBlockEntity(entity);
    }

    /**
     * Takes one item out of the held stack.
     *
     * @param inventory inventory owning the stack
     * @param held stack the player holds
     */
    private static void useOneItem(PlayerInventory inventory, ItemStack held) {
        held.setCount(held.count() - 1);
        if (held.isEmpty()) {
            inventory.set(inventory.selectedSlot(), ItemStack.EMPTY);
        }
    }
}
