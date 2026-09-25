package com.philia093.neofactory.item;

/**
 * Sink that is told when a piece of the workshop reached the end of its life.
 * <p>
 * A tool takes damage with every block it harvests, and a piece whose life is gone leaves the hand
 * of the player. What breaks a block knows nothing about an inventory - it is handed one stack and
 * hands nothing back - so the used up piece is reported here, the same way a broken block reports
 * its drops through {@link ItemDrops}. The game clears the slot the stack came from: a life of
 * zero is what makes a tool disappear, see {@link Damageable#isBroken()}.
 * <p>
 * The sink is asked for every kind of piece and not only for a tool: a mortar that is ground down
 * and a screwdriver that is twisted off are reported the same way, which is what keeps the mining
 * code free of a list of tools.
 */
public interface ItemWear {

    /**
     * Reports a piece that has no life left.
     *
     * @param stack the worn out stack, already marked as used up, it is no longer in the hand of
     *              the player after this call
     */
    void wornOut(ItemStack stack);
}
