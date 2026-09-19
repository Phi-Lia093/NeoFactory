package com.philia093.neofactory.item;

/**
 * Sink for the items a broken block hands back.
 * <p>
 * Breaking a block never touches an inventory directly, it only reports what
 * appeared in the world. The game uses {@link InventoryDrops} today, which moves
 * the items straight into the inventory of the player. A dropped item system
 * replaces that implementation with one that spawns entities on the ground, so
 * neither the mining code nor the game loop have to change for it.
 */
public interface ItemDrops {

    /**
     * Reports items that appeared in the world.
     *
     * @param stack items to hand out, ignored when the stack is empty
     * @param worldX world X coordinate the items appeared at
     * @param worldY world Y coordinate the items appeared at
     */
    void drop(ItemStack stack, float worldX, float worldY);
}
