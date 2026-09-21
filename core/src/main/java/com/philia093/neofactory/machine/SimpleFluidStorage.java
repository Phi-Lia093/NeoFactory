package com.philia093.neofactory.machine;

/**
 * A {@link FluidStorage} that holds one kind of fluid.
 * <p>
 * The class is the simple case a machine starts with: a tank of a fixed size that takes
 * a fluid as long as it is the one it already holds and gives it back on demand. An
 * empty tank accepts whatever is offered first.
 */
public final class SimpleFluidStorage implements FluidStorage {

    private final int capacity;

    private FluidType fluid;
    private int amount;

    /**
     * Creates an empty tank.
     *
     * @param capacity largest amount the tank holds
     */
    public SimpleFluidStorage(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("A tank with a negative capacity does not exist");
        }
        this.capacity = capacity;
    }

    @Override
    public FluidType fluid() {
        return fluid;
    }

    @Override
    public int amount() {
        return amount;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public int fill(FluidType type, int amount, boolean simulate) {
        if (type == null || amount <= 0) {
            return 0;
        }
        if (fluid != null && fluid != type) {
            // A tank never mixes two fluids.
            return 0;
        }
        int taken = Math.min(amount, capacity - this.amount);
        if (taken <= 0) {
            return 0;
        }
        if (!simulate) {
            fluid = type;
            this.amount += taken;
        }
        return taken;
    }

    @Override
    public int drain(int maxDrain, boolean simulate) {
        if (maxDrain <= 0 || amount <= 0) {
            return 0;
        }
        int given = Math.min(maxDrain, amount);
        if (!simulate) {
            amount -= given;
            if (amount <= 0) {
                fluid = null;
            }
        }
        return given;
    }

    /**
     * Writes a content into the tank without going through the limits.
     * <p>
     * Used when a machine is loaded or when a test sets up a state.
     *
     * @param type kind of the fluid, {@code null} to empty the tank
     * @param amount requested amount, clamped between zero and the capacity
     */
    public void set(FluidType type, int amount) {
        if (type == null || amount <= 0) {
            fluid = null;
            this.amount = 0;
            return;
        }
        fluid = type;
        this.amount = Math.min(amount, capacity);
    }

    @Override
    public String toString() {
        return "SimpleFluidStorage(" + (fluid == null ? "empty" : fluid.name()) + " "
                + amount + "/" + capacity + ")";
    }
}
