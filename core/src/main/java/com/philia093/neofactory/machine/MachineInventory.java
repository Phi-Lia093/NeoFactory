package com.philia093.neofactory.machine;

import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.recipe.RecipeGrid;
import com.philia093.neofactory.recipe.SlotGrid;

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

        /**
         * Holds a module the machine itself reads while it works.
         * <p>
         * An upgrade slot is filled and emptied like an input slot, it is simply not
         * part of what a recipe is offered: a speed module or a filter belongs to the
         * machine and not to its recipe. Which modules the game will own is not decided
         * yet, so this role only says where such a stack belongs.
         */
        UPGRADE,

        /**
         * The slot that will configure a machine later on.
         * <p>
         * It sits in the lower right corner of the panel and belongs to a machine that
         * serves one input from many: which of its inputs a recipe takes is a question of
         * the configuration the player puts here. Nothing reads it yet, so the role only
         * says where such a stack belongs.
         */
        CONFIGURE,

        /**
         * Holds a cell the machine trades with its tanks.
         * <p>
         * A full cell is poured into the tanks the machine takes fluid from, and an empty one is filled
         * from the tanks it makes fluid in, see
         * {@link com.philia093.neofactory.item.CellExchange}. The slot is therefore not part of what a
         * recipe is offered - a recipe never eats a cell - and it is the one place where a player hands a
         * fluid to a machine, or takes one away, without a pipe.
         */
        CELL,

        /** A plain slot without a job. */
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

    /**
     * The slots of one role, offered to a recipe as a grid.
     * <p>
     * The slots of a machine need not lie next to each other - an input may sit between
     * a fuel slot and an output - so the grid is built from the list of indexes instead
     * of a rectangle of the inventory, see {@link SlotGrid}. A machine that has no slot
     * of that role gets a grid without a place, which a recipe sees as an empty input.
     *
     * @param role role to collect
     * @return the grid, never {@code null}
     */
    public RecipeGrid gridOf(Role role) {
        List<Integer> slots = slotsOf(role);
        return slots.isEmpty() ? NO_SLOTS : new SlotGrid(this, slots);
    }

    /** Grid a machine uses for a role it has no slot of. */
    private static final RecipeGrid NO_SLOTS = new RecipeGrid() {

        @Override
        public int width() {
            return 0;
        }

        @Override
        public int height() {
            return 0;
        }

        @Override
        public ItemStack get(int x, int y) {
            return ItemStack.EMPTY;
        }

        @Override
        public String toString() {
            return "RecipeGrid(no slots)";
        }
    };

    @Override
    public String toString() {
        return "MachineInventory(" + size() + " slots, " + storedItemCount() + " items)";
    }
}
