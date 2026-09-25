package com.philia093.neofactory.item;

/**
 * Something that wears out with use.
 * <p>
 * Durability is not a property of a tool alone: a mortar, a screwdriver or any other piece of the
 * workshop wears out the same way, and so will a part inside a machine one day. What a worn thing
 * has to answer is therefore asked through this interface and never read from a field of one kind
 * of item: how much a fresh piece takes ({@link #maxDamage()}), how much of it is gone
 * ({@link #damage()}) and what one more use does to it ({@link #applyDamage(int)}).
 * <p>
 * {@link ItemStack} is the implementation the game carries: the wear belongs to the stack and not
 * to the type, so the pickaxe in the hand and the one in a chest are worn differently.
 * {@link Item#maxDamage()} is what a fresh piece of a kind takes - zero for an item that never
 * wears out - and it is also the number the bar of an inventory slot is measured against, see
 * {@code com.philia093.neofactory.gui.DurabilityBar}.
 */
public interface Damageable {

    /**
     * Amount of damage a fresh piece takes before it is used up.
     *
     * @return the life of a new piece, {@code 0} for a thing that never wears out
     */
    int maxDamage();

    /** Amount of damage taken so far, always between {@code 0} and {@link #maxDamage()}. */
    int damage();

    /** {@code true} when this thing takes damage at all, which every tool and every worn part does. */
    default boolean isDamageable() {
        return maxDamage() > 0;
    }

    /** Damage left before this thing is used up, {@link #maxDamage()} while it is untouched. */
    default int remainingDamage() {
        return Math.max(0, maxDamage() - damage());
    }

    /** {@code true} when everything a piece could take is gone and it is used up. */
    default boolean isBroken() {
        return isDamageable() && damage() >= maxDamage();
    }

    /**
     * Part of the life that is gone.
     *
     * @return {@code 0} for a fresh piece, {@code 1} for a used up one, {@code 0} for a thing that
     *         never wears out
     */
    default float wear() {
        int max = maxDamage();
        if (max <= 0) {
            return 0.0f;
        }
        return Math.min(1.0f, (float) damage() / (float) max);
    }

    /**
     * Uses this thing one more time.
     * <p>
     * Damage is clamped to the life of the piece, so a piece that is used up stays used up instead
     * of counting past its end.
     *
     * @param amount amount of damage to take, ignored when it is not positive
     * @return the amount that was really taken, which is less than asked for at the end of the life
     */
    int applyDamage(int amount);
}
