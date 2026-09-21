package com.philia093.neofactory.machine;

import com.philia093.neofactory.item.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * The inventory of a machine, whose slots have a job.
 * <p>
 * A machine never works on "slot two": it works on its input, its fuel and its output.
 * Naming the roles keeps the code of a machine readable and lets a recipe, a screen and
 * a later automation each find the slot they mean without knowing the layout.
 */
public final class MachineInventory extends Inventory {

    /** What a slot of a machine is for. */
    public enum Role {

        /** Holds what the machine works on, an ore for a furnace. */
        INPUT,

        /** Holds what keeps the machine running, a piece of coal for a furnace. */
        FUEL,

        /** Holds what the machine made, a screen only lets a player take it out. */
        OUTPUT,

        /** A plain slot without a job, for example the upgrade of a machine. */
        NORMAL
    }

    private final Role[] roles;

    /**
     * Creates a machine inventory.
     *
     * @param roles one role per slot, in the order the slots are drawn
     */
    public MachineInventory(Role... roles) {
        super(roles.length);
        this.roles = roles.clone();
    }

    /**
     * Role of one slot.
     *
     * @param slot slot index
     * @return the role of that slot
     */
    public Role role(int slot) {
        return roles[slot];
    }

    /**
     * First slot of a role.
     *
     * @param role role to look for
     * @return the slot index, or {@code -1} when the machine has no such slot
     */
    public int slotOf(Role role) {
        for (int slot = 0; slot < roles.length; slot++) {
            if (roles[slot] == role) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * Every slot of a role, in the order they were declared.
     *
     * @param role role to look for
     * @return the slot indexes, an empty list when the machine has none
     */
    public List<Integer> slotsOf(Role role) {
        List<Integer> found = new ArrayList<>();
        for (int slot = 0; slot < roles.length; slot++) {
            if (roles[slot] == role) {
                found.add(slot);
            }
        }
        return found;
    }

    @Override
    public String toString() {
        return "MachineInventory(" + size() + " slots, " + storedItemCount() + " items)";
    }
}
