package com.philia093.neofactory.item;

/**
 * Sink for the items a broken block hands back.
 * <p>
 * Breaking a block never touches an inventory directly, it only reports what
 * appeared in the world. The game uses {@link WorldDrops}, which leaves the items
 * lying on the ground as entities that the player picks up by walking over them.
 * {@link InventoryDrops} is the simpler alternative that moves the items straight
 * into the inventory, which is what a "fast pickup" rule or a creative mode would
 * use without touching the mining code.
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
