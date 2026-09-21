package com.philia093.neofactory.item;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

/**
 * Hands dropped items straight to the player.
 * <p>
 * This is the simple implementation of {@link ItemDrops}: whatever a broken block
 * hands back is put into the inventory right away, as if the player had picked it
 * up. Items that do not fit are reported and lost.
 * <p>
 * The game uses {@link WorldDrops} instead, which leaves the items on the ground;
 * this class stays as the alternative for rules and modes where digging should not
 * leave anything behind.
 */
public class InventoryDrops implements ItemDrops {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Inventory that receives everything that is dropped. */
    private final PlayerInventory inventory;

    /**
     * Creates a sink that fills an inventory.
     *
     * @param inventory inventory receiving the drops
     */
    public InventoryDrops(PlayerInventory inventory) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    @Override
    public void drop(ItemStack stack, float worldX, float worldY) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        int remaining = inventory.add(stack);
        if (remaining > 0) {
            // The dropped item implementation leaves a stack on the ground instead,
            // see WorldDrops.
            LOGGER.info("Inventory is full, {} x {} could not be picked up",
                    remaining, stack.item().name());
        }
    }

    @Override
    public String toString() {
        return "InventoryDrops(" + inventory + ")";
    }
}
