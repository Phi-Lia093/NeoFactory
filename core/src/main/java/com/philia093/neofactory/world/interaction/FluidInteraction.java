package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.fluid.FluidFlow;
import com.philia093.neofactory.fluid.FluidState;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.item.Buckets;
import com.philia093.neofactory.item.FluidContainer;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Fills a bucket or a cell out of the world and pours it back in.
 * <p>
 * The rules are the ones a player expects. An empty container takes the fluid of the targeted
 * cell, a full one pours its fluid into an empty cell, and a cell that is taken does nothing,
 * exactly like a block is only built into an empty cell, see {@link BlockPlacer}.
 * <p>
 * Only a source is taken, never a cell the fluid merely ran through: a running spill belongs to
 * the source it came from, and taking it away would leave water behind that nothing feeds. The
 * fluid of a cell is a block of the object layer, the one the player stands in, and taking it out
 * is what makes the spill it fed run out by itself, see {@link FluidFlow}.
 * <p>
 * What a container is allowed to carry is decided by the item, not here: a bucket takes water and
 * lava and nothing else, a cell takes any fluid of the industry, see
 * {@link FluidContainer#accepts(Fluid)}.
 */
public final class FluidInteraction {

    private static final Logger LOGGER = LogManager.getLogger();

    private FluidInteraction() {
        // Utility class: never instantiated.
    }

    /**
     * Uses the held container on the targeted cell.
     *
     * @param world world to change
     * @param target cell the player points at, may be {@code null}
     * @param inventory inventory holding the stack the player carries
     * @return {@code true} when the world or the held stack changed
     */
    public static boolean use(World world, BlockTarget target, PlayerInventory inventory) {
        if (target == null) {
            return false;
        }
        ItemStack held = inventory.heldStack();
        if (held.isEmpty()) {
            return false;
        }
        FluidContainer container = held.item().container();
        if (container == null) {
            return false;
        }
        if (container.isEmpty()) {
            return fill(world, target, inventory, container);
        }
        return pour(world, target, inventory, container);
    }

    /**
     * Takes the source of a fluid out of the world and into the held container.
     *
     * @param world world to change
     * @param target cell to take the fluid from
     * @param inventory inventory holding the stack the player carries
     * @param container container that is filled
     * @return {@code true} when a source was taken
     */
    private static boolean fill(World world, BlockTarget target, PlayerInventory inventory,
            FluidContainer container) {
        int x = target.x();
        int y = target.y();
        int layer = target.layer();
        Fluid fluid = Fluids.byBlock(world.getFlatBlock(x, y, layer));
        if (fluid == null || !container.accepts(fluid)) {
            // No fluid there, or one this container never carries, such as oil in a bucket.
            return false;
        }
        if (!FluidState.unpack(world.getFlatState(x, y, layer)).isSource()) {
            // The fluid only ran through this cell, the source that feeds it lies elsewhere.
            return false;
        }
        Item filled = Buckets.filled(container.kind(), fluid);
        if (filled == null) {
            LOGGER.warn("The game has no {} for the fluid {}", container.kind(), fluid.name());
            return false;
        }
        ItemStack taken = ItemStack.of(filled, 1);
        if (inventory.heldStack().count() > 1 && !inventory.hasRoomFor(taken)) {
            // A stack of empty containers was in the hand: the full one needs a slot of its own,
            // because a container with a fluid in it does not stack. Without one the source stays
            // where it is instead of being swallowed.
            LOGGER.info("No room for the {} the player just filled", filled.name());
            return false;
        }

        world.setFlatBlock(x, y, layer, Blocks.AIR);
        world.setFlatState(x, y, layer, 0);
        handOver(inventory, taken);
        LOGGER.info("Filled a {} at ({}, {}) of layer {}", filled.name(), x, y, layer);
        return true;
    }

    /**
     * Takes one empty container out of the hand and hands the full one to the player.
     * <p>
     * A stack of empty buckets is what a player carries to a lake, so filling one of them leaves
     * the rest of the stack in the hand and puts the full container into the inventory - a full
     * bucket takes a slot of its own, because it does not stack, see {@code Items}.
     *
     * @param inventory inventory of the player, its held stack is what is left of the empty ones
     * @param filled stack of one that is given back
     */
    private static void handOver(PlayerInventory inventory, ItemStack filled) {
        ItemStack held = inventory.heldStack();
        if (held.count() > 1) {
            held.setCount(held.count() - 1);
            inventory.add(filled);
            return;
        }
        inventory.set(inventory.selectedSlot(), filled);
    }

    /**
     * Pours the held fluid into an empty cell as a source.
     * <p>
     * The rules are the ones a block follows, see {@link BlockPlacer}: nothing is poured into a
     * taken cell, and a build never skips a layer - the ground below the feet can only be filled
     * while the layer the player stands in is empty, and the layer the player stands in can only
     * be filled while it has ground below it. A fluid in a cell without ground would float in the
     * air, which is refused.
     *
     * @param world world to change
     * @param target cell to pour into
     * @param inventory inventory holding the stack the player carries
     * @param container container that is emptied
     * @return {@code true} when a source was poured
     */
    private static boolean pour(World world, BlockTarget target, PlayerInventory inventory,
            FluidContainer container) {
        int x = target.x();
        int y = target.y();
        int layer = target.layer();
        if (!world.getFlatBlock(x, y, layer).isAir()) {
            // The cell is taken, by a wall, a plant or another fluid.
            return false;
        }
        if (!mayStand(world, x, y, layer)) {
            return false;
        }

        Fluid fluid = container.content();
        world.setFlatBlock(x, y, layer, fluid.block());
        world.setFlatState(x, y, layer, FluidState.SOURCE.pack());
        inventory.set(inventory.selectedSlot(), ItemStack.of(Buckets.empty(container.kind()), 1));
        LOGGER.info("Poured {} into ({}, {}) of layer {}", fluid.name(), x, y, layer);
        return true;
    }

    /**
     * {@code true} when a fluid may stand in an empty cell of a layer.
     * <p>
     * The two layers answer differently, exactly like a block does: the ground below the feet is
     * out of reach while the layer the player stands in holds something, and the layer the player
     * stands in needs ground below it.
     *
     * @param world world to read
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @param layer layer the fluid would stand in
     * @return {@code true} when the fluid may be poured there
     */
    private static boolean mayStand(World world, int x, int y, int layer) {
        if (layer == Chunk.LAYER_FLOOR) {
            return !world.hasFlatObjectBlock(x, y);
        }
        return world.hasFlatGround(x, y);
    }
}
