package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.World;

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

        useOneItem(inventory, held);
        return true;
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
