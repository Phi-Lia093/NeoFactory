package com.philia093.neofactory.machine;

import java.util.Objects;

/**
 * One tank of a machine, together with the job it has.
 * <p>
 * A machine never fills "tank two": a reactor takes its water out of one tank and
 * pours its product into another one. Naming the role is what lets a recipe, a pipe
 * and a screen find the tank they mean, exactly like the roles of
 * {@link MachineInventory} name the slots.
 */
public final class MachineTank {

    /** What a tank of a machine is for. */
    public enum Role {

        /** Fluid a recipe takes out, the raw material of the machine. */
        INPUT,

        /** Fluid the machine made, for a pipe or a bucket to take out. */
        OUTPUT
    }

    private final FluidStorage storage;
    private final Role role;

    /**
     * Creates a tank.
     *
     * @param storage storage behind the tank
     * @param role job of the tank
     */
    public MachineTank(FluidStorage storage, Role role) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.role = Objects.requireNonNull(role, "role");
    }

    /**
     * Creates an empty tank of a size.
     *
     * @param capacity amount the tank holds
     * @param role job of the tank
     * @return the tank
     */
    public static MachineTank of(int capacity, Role role) {
        return new MachineTank(new SimpleFluidStorage(capacity), role);
    }

    /** Storage behind this tank. */
    public FluidStorage storage() {
        return storage;
    }

    /** Job of this tank. */
    public Role role() {
        return role;
    }

    /** {@code true} when a recipe may drain this tank. */
    public boolean isInput() {
        return role == Role.INPUT;
    }

    /** {@code true} when this tank holds what the machine made. */
    public boolean isOutput() {
        return role == Role.OUTPUT;
    }

    @Override
    public String toString() {
        return "MachineTank(" + role + ", " + storage + ")";
    }
}
