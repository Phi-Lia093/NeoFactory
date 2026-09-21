package com.philia093.neofactory.fluid;

/**
 * The fluid a machine holds, the interface a pipe or a tank will talk to.
 * <p>
 * A tank holds one kind of fluid at a time, which is what keeps the contract simple:
 * {@link #fill(Fluid, int, boolean)} only takes what fits and refuses a second kind, and
 * {@link #drain(int, boolean)} gives the stored one back. Like the energy contract every
 * change can be simulated, so a machine may ask before it acts.
 */
public interface FluidStorage {

    /**
     * Fluid that is stored.
     *
     * @return the fluid, or {@code null} while the tank is empty
     */
    Fluid fluid();

    /** Amount of fluid that is stored right now, in units of the game. */
    int amount();

    /** Largest amount this tank can hold. */
    int capacity();

    /**
     * Offers fluid to this tank.
     *
     * @param type kind of the offered fluid
     * @param amount largest amount that is offered
     * @param simulate {@code true} to only ask what would happen
     * @return the amount that was, or would be, taken
     */
    int fill(Fluid type, int amount, boolean simulate);

    /**
     * Takes fluid out of this tank.
     *
     * @param maxDrain largest amount that is wanted
     * @param simulate {@code true} to only ask what would happen
     * @return the amount that was, or would be, given
     */
    int drain(int maxDrain, boolean simulate);

    /** {@code true} when the tank holds nothing. */
    default boolean isEmpty() {
        return amount() <= 0 || fluid() == null;
    }

    /** {@code true} when no more fluid fits. */
    default boolean isFull() {
        return amount() >= capacity();
    }
}
