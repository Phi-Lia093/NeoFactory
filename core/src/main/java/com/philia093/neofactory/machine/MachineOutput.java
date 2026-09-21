package com.philia093.neofactory.machine;

import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.item.ItemStack;

import java.util.List;

/**
 * Where the products of a recipe end up.
 * <p>
 * A recipe hands its products over instead of writing into a slot, so the machine
 * decides where they land and a recipe never depends on the order of the slots.
 * Whether the products fit is asked before the recipe runs, see
 * {@link MachineRecipe#fits(MachineOutput)}, which is what keeps a machine from
 * starting work whose result has nowhere to go.
 * <p>
 * Like the input view this one may be asked freely: a check never changes anything.
 */
public interface MachineOutput {

    /**
     * {@code true} when every product still fits into the output slots.
     *
     * @param products items a recipe wants to hand over, may be empty
     * @return {@code true} when {@link #addAll(List)} can store all of them
     */
    boolean hasRoomFor(List<ItemStack> products);

    /**
     * Stores the products in the output slots.
     *
     * @param products items to store, empty stacks are ignored
     */
    void addAll(List<ItemStack> products);

    /**
     * {@code true} when this much of a fluid fits into one of the output tanks.
     *
     * @param fluid kind of the fluid
     * @param amount amount in units of the game
     * @return {@code true} when the fluid would be taken
     */
    boolean hasRoomFor(Fluid fluid, int amount);

    /**
     * Pours a fluid into the first output tank that may take it.
     *
     * @param fluid kind of the fluid
     * @param amount amount that is offered
     * @return the amount that was stored
     */
    int fill(Fluid fluid, int amount);

    /** {@code true} when a single product fits. */
    default boolean hasRoomFor(ItemStack product) {
        return hasRoomFor(List.of(product));
    }

    /** Stores a single product. */
    default void add(ItemStack product) {
        addAll(List.of(product));
    }
}
