package com.philia093.neofactory.machine;

import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.recipe.Recipe;

import java.util.List;

/**
 * A recipe a machine performs over time.
 * <p>
 * One contract is enough for every machine of the game: a furnace turns a single item
 * into another one and only adds {@link #seconds()}, while a chemical reactor takes
 * several items and a fluid in and hands several items and a fluid out. Neither of
 * them knows anything about the machine that runs it, because everything a recipe looks
 * at lives in {@link MachineInput} and everything it hands out goes through
 * {@link MachineOutput}.
 * <p>
 * The interface extends {@link Recipe}, so a recipe that only works on items - a
 * smelting recipe, for instance - is a machine recipe the moment it says how long it
 * takes. A recipe that also works on fluids adds
 * {@link #fluidIngredients()} and {@link #fluidProducts()}, and a recipe that takes
 * energy adds {@link #energy()}.
 * <p>
 * A machine asks a recipe many times while it looks for work, so
 * {@link #matches(MachineInput)} and {@link #fits(MachineOutput)} may never change
 * anything.
 */
public interface MachineRecipe extends Recipe {

    /** Seconds one craft takes. */
    float seconds();

    /** Items the recipe makes, one entry per output slot it fills. */
    default List<ItemStack> products() {
        return List.of(result());
    }

    /** Fluid the recipe takes, empty for a recipe that only works on items. */
    default List<FluidIngredient> fluidIngredients() {
        return List.of();
    }

    /** Fluid the recipe makes, empty for a recipe that only makes items. */
    default List<FluidIngredient> fluidProducts() {
        return List.of();
    }

    /** Energy one craft costs, {@code 0} for a machine that runs on fuel or nothing. */
    default int energy() {
        return 0;
    }

    /**
     * {@code true} when the machine holds what this recipe needs.
     * <p>
     * The default combines the items of the input with the fluids a recipe asks for, so
     * a recipe that only works on items never has to think about tanks.
     *
     * @param input what the machine offers
     * @return {@code true} when {@link #consume(MachineInput)} may be called
     */
    default boolean matches(MachineInput input) {
        if (!matches(input.items())) {
            return false;
        }
        for (FluidIngredient need : fluidIngredients()) {
            if (!input.hasFluid(need.fluid(), need.amount())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Takes one craft worth of input out of the machine.
     *
     * @param input what the machine offers
     */
    default void consume(MachineInput input) {
        consume(input.items());
        for (FluidIngredient need : fluidIngredients()) {
            input.drain(need.fluid(), need.amount());
        }
    }

    /**
     * {@code true} when every product has a place to go.
     *
     * @param output where the products would land
     * @return {@code true} when {@link #produce(MachineOutput)} may be called
     */
    default boolean fits(MachineOutput output) {
        if (!output.hasRoomFor(products())) {
            return false;
        }
        for (FluidIngredient product : fluidProducts()) {
            if (!output.hasRoomFor(product.fluid(), product.amount())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Hands the products of one craft over.
     *
     * @param output where the products land
     */
    default void produce(MachineOutput output) {
        output.addAll(products());
        for (FluidIngredient product : fluidProducts()) {
            output.fill(product.fluid(), product.amount());
        }
    }
}
