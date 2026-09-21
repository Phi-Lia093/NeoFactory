package com.philia093.neofactory.machine;

/**
 * An {@link EnergyStorage} that counts in a single number.
 * <p>
 * The class is the simple case a machine starts with: a buffer with a capacity and two
 * limits that say how fast it may be filled and emptied. It is also what a test uses to
 * check a machine without a power network, see {@code SmeltingMachineTest}.
 */
public final class SimpleEnergyStorage implements EnergyStorage {

    private final int capacity;
    private final int maxReceive;
    private final int maxExtract;

    private int amount;

    /**
     * Creates a buffer that takes and gives energy as fast as it fits.
     *
     * @param capacity largest amount the buffer holds
     */
    public SimpleEnergyStorage(int capacity) {
        this(capacity, capacity, capacity);
    }

    /**
     * Creates a buffer.
     *
     * @param capacity largest amount the buffer holds
     * @param maxReceive largest amount one call may add
     * @param maxExtract largest amount one call may take
     */
    public SimpleEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        if (capacity < 0) {
            throw new IllegalArgumentException("A buffer with a negative capacity does not exist");
        }
        this.capacity = capacity;
        this.maxReceive = Math.max(0, maxReceive);
        this.maxExtract = Math.max(0, maxExtract);
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
    public int receive(int maxReceive, boolean simulate) {
        int taken = Math.max(0, Math.min(Math.min(maxReceive, this.maxReceive), capacity - amount));
        if (!simulate) {
            amount += taken;
        }
        return taken;
    }

    @Override
    public int extract(int maxExtract, boolean simulate) {
        int given = Math.max(0, Math.min(Math.min(maxExtract, this.maxExtract), amount));
        if (!simulate) {
            amount -= given;
        }
        return given;
    }

    @Override
    public boolean canReceive() {
        return maxReceive > 0;
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }

    /**
     * Writes an amount into the buffer without going through the limits.
     * <p>
     * Used when a machine is loaded or when a test sets up a state; the network of the
     * game uses {@link #receive(int, boolean)} instead.
     *
     * @param amount requested amount, clamped between zero and the capacity
     */
    public void setAmount(int amount) {
        this.amount = Math.max(0, Math.min(amount, capacity));
    }

    @Override
    public String toString() {
        return "SimpleEnergyStorage(" + amount + "/" + capacity + ")";
    }
}
