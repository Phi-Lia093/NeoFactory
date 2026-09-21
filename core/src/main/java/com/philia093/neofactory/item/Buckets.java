package com.philia093.neofactory.item;

import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.fluid.Fluids;

/**
 * The items that carry a fluid: the buckets and the cells of the game.
 * <p>
 * The table is spelled out instead of being built from the fluids, because the id of an item is
 * permanent - a stored inventory keeps it - so a fluid that is added later must not shift the
 * item of the one before it. A bucket is listed by hand for a second reason: the game has exactly
 * three of them, the empty one, water and lava, and a fluid of the industry never gets one, see
 * {@link FluidContainer.Kind#BUCKET}.
 * <p>
 * A cell is listed as well, even though every fluid of the industry will get one: adding a fluid
 * means adding its cell here, which is the one place that knows which item belongs to which fluid.
 */
public final class Buckets {

    private Buckets() {
        // Utility class: never instantiated.
    }

    /**
     * The empty form of a container.
     *
     * @param kind kind of the container
     * @return the item that carries nothing, for example the empty bucket
     */
    public static Item empty(FluidContainer.Kind kind) {
        return kind == FluidContainer.Kind.BUCKET ? Items.BUCKET : Items.FLUID_CELL;
    }

    /**
     * The form of a container that carries a fluid.
     *
     * @param kind kind of the container
     * @param fluid fluid that goes inside
     * @return the item that carries it, or {@code null} when the game has none
     */
    public static Item filled(FluidContainer.Kind kind, Fluid fluid) {
        if (kind == FluidContainer.Kind.BUCKET) {
            return bucketOf(fluid);
        }
        return cellOf(fluid);
    }

    /** The bucket of water or lava, {@code null} for any other fluid. */
    private static Item bucketOf(Fluid fluid) {
        if (fluid == Fluids.WATER) {
            return Items.WATER_BUCKET;
        }
        if (fluid == Fluids.LAVA) {
            return Items.LAVA_BUCKET;
        }
        return null;
    }

    /** The cell of a fluid, {@code null} while the game has no cell for it yet. */
    private static Item cellOf(Fluid fluid) {
        if (fluid == Fluids.WATER) {
            return Items.WATER_CELL;
        }
        if (fluid == Fluids.LAVA) {
            return Items.LAVA_CELL;
        }
        return null;
    }
}
