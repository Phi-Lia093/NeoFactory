package com.philia093.neofactory.machine;

import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.fluid.FluidIngredient;
import com.philia093.neofactory.fluid.FluidStorage;
import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.ItemStack;

import java.util.List;
import java.util.Objects;

/**
 * The simple {@link MachineOutput} of a machine: its output slots and the tanks a
 * recipe may fill.
 * <p>
 * A product takes one slot, which is what keeps the answer of
 * {@link #hasRoomFor(List)} and the effect of {@link #addAll(List)} the same: a stack
 * that was promised a place finds it again. A stack that may be merged into a slot that
 * already holds the same item counts as that slot, so a furnace fills its output up to
 * a full stack instead of asking for a second slot.
 */
public final class MachineOutputs implements MachineOutput {

    private final Inventory inventory;
    private final int[] slots;
    private final FluidStorage[] tanks;

    /**
     * Creates an output view.
     *
     * @param inventory inventory holding the slots
     * @param slots indexes of the output slots, in the order they are written
     * @param tanks tanks a recipe may fill
     */
    public MachineOutputs(Inventory inventory, int[] slots, FluidStorage... tanks) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.slots = slots == null ? new int[0] : slots.clone();
        this.tanks = tanks == null ? new FluidStorage[0] : tanks.clone();
    }

    @Override
    public boolean hasRoomFor(List<ItemStack> products) {
        if (products == null) {
            return true;
        }
        boolean[] used = new boolean[slots.length];
        for (ItemStack product : products) {
            if (product == null || product.isEmpty()) {
                continue;
            }
            if (claimSlot(product, used) < 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void addAll(List<ItemStack> products) {
        if (products == null) {
            return;
        }
        for (ItemStack product : products) {
            if (product == null || product.isEmpty()) {
                continue;
            }
            int slot = claimSlot(product, new boolean[slots.length]);
            if (slot < 0) {
                // hasRoomFor said yes, so this can only happen when a recipe produces more
                // than it announced; the stack is reported by the caller's log instead of
                // being written over something.
                continue;
            }
            store(slot, product);
        }
    }

    @Override
    public boolean hasRoomFor(Fluid fluid, int amount) {
        if (amount <= 0) {
            return true;
        }
        for (FluidStorage tank : tanks) {
            if (FluidIngredient.of(fluid, amount).fits(tank)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int fill(Fluid fluid, int amount) {
        if (amount <= 0) {
            return 0;
        }
        for (FluidStorage tank : tanks) {
            int stored = tank.fill(fluid, amount, false);
            if (stored > 0) {
                return stored;
            }
        }
        return 0;
    }

    /** Index of the slot a product would take, marking it as taken. */
    private int claimSlot(ItemStack product, boolean[] used) {
        for (int index = 0; index < slots.length; index++) {
            if (used[index]) {
                continue;
            }
            ItemStack stored = inventory.get(slots[index]);
            if (stored.isEmpty()) {
                used[index] = true;
                return index;
            }
            if (stored.isStackableWith(product) && stored.room() >= product.count()) {
                used[index] = true;
                return index;
            }
        }
        return -1;
    }

    /** Writes a product into a slot, topping up a stack of the same item. */
    private void store(int position, ItemStack product) {
        ItemStack stored = inventory.get(slots[position]);
        if (stored.isEmpty()) {
            inventory.set(slots[position], ItemStack.of(product.item(), product.count()));
            return;
        }
        stored.grow(product.count());
    }

    @Override
    public String toString() {
        return "MachineOutputs(" + slots.length + " slots, " + tanks.length + " tanks)";
    }
}
