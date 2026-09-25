package com.philia093.neofactory.item;

import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.fluid.Fluids;

/**
 * The items that carry a fluid: the cells of the game.
 * <p>
 * The table is spelled out instead of being built from the fluids, because the id of an item is
 * permanent - a stored inventory keeps it - so a fluid that is added later must not shift the item of
 * the one before it. Adding a fluid therefore means adding its cell here, which is the one place that
 * knows which item belongs to which fluid.
 * <p>
 * <b>There is one container and no bucket.</b> Water, lava and the steam of a boiler all travel in the
 * same cell, a body of steel with a window the colour of the fluid shows through, see
 * {@link FluidContainer}. A bucket belonged to the fluids that stood in the world as a block, and those
 * are gone.
 */
public final class FluidCells {

    private FluidCells() {
        // Utility class: never instantiated.
    }

    /**
     * The form a container has before anything is put into it.
     *
     * @return the item that carries nothing
     */
    public static Item empty() {
        return Items.FLUID_CELL;
    }

    /**
     * The cell that carries a fluid.
     *
     * @param fluid fluid that goes inside
     * @return the item that carries it, or {@code null} when the game has no cell for it yet
     */
    public static Item filled(Fluid fluid) {
        if (fluid == Fluids.WATER) {
            return Items.WATER_CELL;
        }
        if (fluid == Fluids.LAVA) {
            return Items.LAVA_CELL;
        }
        if (fluid == Fluids.STEAM) {
            return Items.STEAM_CELL;
        }
        return null;
    }
}
