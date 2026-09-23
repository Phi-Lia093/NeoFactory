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
 * A build goes into the cell the player aims at, and when that cell is taken it goes into the cell
 * behind the face the line of sight entered through, see {@link BlockTarget#neighbour()}: that is how a
 * wall is built against and how a bridge grows from the block it is laid on. A cell the mouse named
 * carries no face, and then the taken cell simply refuses the build.
 * <p>
 * A block needs something to stand on, see
 * {@link com.philia093.neofactory.world.BlockAccess#hasSupport(int, int, int)}: a cell without a block
 * below it is a hole in the air, and a block there would float. A block that would end up inside the
 * player is refused as well: the check runs on the changed world and undoes the write when the player
 * no longer fits.
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
        if (world.getBlock(target.x(), target.y(), target.z()).isAir()) {
            return buildInto(world, player, target, block, inventory, held);
        }
        // The cell is taken. A ray knows the face it entered through, so the block goes into the cell
        // behind that face: that is how a wall is built against and how a bridge grows from the block
        // it is laid on. A cell the mouse named has no face, and then there is nothing to build
        // against.
        BlockTarget behind = target.neighbour();
        return behind != null && buildInto(world, player, behind, block, inventory, held);
    }

    /**
     * Builds the held block into one empty cell.
     * <p>
     * A block needs something to stand on: a cell without a block below it is a hole in the air, and
     * the block would float. A build that would end up inside the player is refused as well - the
     * block is written first and taken back when the body no longer fits, which is the only way to ask
     * whether the body fits at all.
     *
     * @param world world to change
     * @param player player that must stay able to move
     * @param cell cell to build into
     * @param block block to store
     * @param inventory inventory holding the stack the player carries
     * @param held stack the player holds
     * @return {@code true} when the world changed and one item was used up
     */
    private static boolean buildInto(World world, Player player, BlockTarget cell, Block block,
            PlayerInventory inventory, ItemStack held) {
        if (!world.getBlock(cell.x(), cell.y(), cell.z()).isAir()
                || !world.hasSupport(cell.x(), cell.y(), cell.z())) {
            return false;
        }

        world.setBlock(cell.x(), cell.y(), cell.z(), block);
        if (player.collides(world, player.position().x, player.position().y)) {
            world.setBlock(cell.x(), cell.y(), cell.z(), Blocks.AIR);
            return false;
        }

        placeBlockEntity(world, cell, block);
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
    private static void placeBlockEntity(World world, BlockTarget cell, Block block) {
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
        entity.setPosition(cell.x(), cell.y(), cell.z());
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
