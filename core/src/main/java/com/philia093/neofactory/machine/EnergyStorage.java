package com.philia093.neofactory.machine;

/**
 * The energy a machine holds, the interface a power network will talk to.
 * <p>
 * The game has no cables and no generator yet, which is exactly why the contract is
 * here: a machine asks for its storage without knowing where the energy comes from, so
 * a later network only has to find the storages of the blocks next to it and move
 * energy through them. Every method that changes the amount is offered twice - once
 * for real and once with {@code simulate} set, which asks what would happen without
 * doing it. A machine uses the second form while it looks for a recipe.
 * <p>
 * Amounts are counted in units of the game, the way the original game counts its
 * energy. One unit is one tick of work at full power, and the limits a storage applies
 * are the limits of the machine behind it.
 */
public interface EnergyStorage {

    /** Amount of energy that is stored right now. */
    int amount();

    /** Largest amount this storage can hold. */
    int capacity();

    /**
     * Offers energy to this storage.
     *
     * @param maxReceive largest amount that is offered
     * @param simulate {@code true} to only ask what would happen
     * @return the amount that was, or would be, taken
     */
    int receive(int maxReceive, boolean simulate);

    /**
     * Takes energy out of this storage.
     *
     * @param maxExtract largest amount that is wanted
     * @param simulate {@code true} to only ask what would happen
     * @return the amount that was, or would be, given
     */
    int extract(int maxExtract, boolean simulate);

    /** {@code true} when this storage may take energy at all. */
    boolean canReceive();

    /** {@code true} when this storage may give energy away. */
    boolean canExtract();

    /** {@code true} when nothing is stored. */
    default boolean isEmpty() {
        return amount() <= 0;
    }

    /** {@code true} when no more energy fits. */
    default boolean isFull() {
        return amount() >= capacity();
    }
}
