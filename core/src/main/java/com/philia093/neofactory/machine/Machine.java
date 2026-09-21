package com.philia093.neofactory.machine;

import java.util.Objects;

/**
 * Base class of every machine of the game.
 * <p>
 * A machine owns the three things every machine needs and nothing else: an inventory
 * whose slots have a {@link MachineInventory.Role role}, an {@link EnergyStorage} and
 * the tanks of fluid it may hold. What it does with them is decided by
 * {@link #update(float)}, so a furnace, a mill and a machine of a later age only differ
 * in that one method.
 * <p>
 * Nothing in the world ticks a machine yet: a machine block, a block entity and the
 * storage of a save game are the next step. The class is written so that step only has
 * to find the machines of the loaded chunks and call {@link #tick(float)} on them, and
 * so that every behaviour can be checked without a world, see
 * {@code SmeltingMachineTest}.
 */
public abstract class Machine {

    private final MachineInventory inventory;
    private final EnergyStorage energy;
    private final FluidStorage[] tanks;

    /**
     * Creates a machine.
     *
     * @param inventory inventory whose slots carry the roles of the machine
     * @param energy storage the machine takes its energy from
     * @param tanks tanks of fluid the machine holds, may be empty
     */
    protected Machine(MachineInventory inventory, EnergyStorage energy, FluidStorage... tanks) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.energy = Objects.requireNonNull(energy, "energy");
        this.tanks = tanks == null ? new FluidStorage[0] : tanks.clone();
    }

    /** Inventory of this machine. */
    public MachineInventory inventory() {
        return inventory;
    }

    /** Storage this machine takes its energy from. */
    public EnergyStorage energy() {
        return energy;
    }

    /** Amount of tanks of this machine. */
    public int tankCount() {
        return tanks.length;
    }

    /**
     * One tank of this machine.
     *
     * @param index tank index, {@code 0 <= index < tankCount()}
     * @return the tank
     */
    public FluidStorage tank(int index) {
        return tanks[index];
    }

    /**
     * Advances the machine by one frame.
     *
     * @param delta time since the last frame in seconds, ignored when not positive
     */
    public final void tick(float delta) {
        if (delta > 0.0f) {
            update(delta);
        }
    }

    /** {@code true} while the machine is working on something. */
    public boolean isRunning() {
        return false;
    }

    /**
     * Does one step of work.
     *
     * @param delta time since the last frame in seconds, always positive
     */
    protected abstract void update(float delta);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + inventory + ", running " + isRunning() + ")";
    }
}
