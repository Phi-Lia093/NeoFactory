package com.philia093.neofactory.item;

import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.fluid.FluidStorage;

import java.util.List;

/**
 * Trades a cell with the tanks of a machine.
 * <p>
 * The class is the whole rule of the fluid slot of a machine and it is plain item arithmetic: a full cell
 * is poured into a tank the machine takes fluid from, and an empty one is filled from a tank the machine
 * makes fluid in. A machine only has to call {@link #exchange(ItemStack, List, List)} once per tick and
 * put the stack it hands back into the slot, see
 * {@link com.philia093.neofactory.machine.Machine#tick(float)}.
 * <p>
 * <b>Nothing is ever lost.</b> A cell is only emptied when a single tank can take the whole amount it
 * holds, and a cell is only filled when a single tank holds a whole cell worth of fluid. Both are asked
 * with a simulated call first, so a tank that is nearly full neither swallows a part of a cell nor hands
 * out a part of one. A cell that is only half poured is not a thing of this game: the item of a cell is
 * the full one, see {@link FluidContainer}.
 * <p>
 * <b>One cell at a time, and a cell that lies in the slot alone.</b> The slot of a machine holds one
 * stack, so the stack has to be a single cell: a stack of empty cells would have nowhere to keep the
 * full one it earns. A cell that carries a fluid stacks to one piece anyway, see {@link Item#maxStackSize()}.
 */
public final class CellExchange {

    private CellExchange() {
        // Utility class: never instantiated.
    }

    /**
     * Trades the stack of a slot with the tanks of a machine.
     *
     * @param carried stack the slot holds, may be empty
     * @param inputs tanks a full cell may be poured into, in the order they are offered
     * @param outputs tanks an empty cell may be filled from, in the order they are offered
     * @return the stack the slot carries afterwards, or the very stack that was given when nothing
     *         happened - a caller may therefore compare the two with {@code ==}
     */
    public static ItemStack exchange(ItemStack carried, List<FluidStorage> inputs,
            List<FluidStorage> outputs) {
        if (carried == null || carried.isEmpty() || carried.count() != 1) {
            // Nothing to trade, or a stack the slot could not hand back: an empty cell is only filled
            // when it lies in the slot alone.
            return carried;
        }
        FluidContainer container = carried.item().container();
        if (container == null) {
            return carried;
        }
        return container.isEmpty() ? fillAnEmptyCell(carried, container, outputs)
                : pourAFullCell(carried, container, inputs);
    }

    /** Pours a full cell into the first tank that takes the whole amount. */
    private static ItemStack pourAFullCell(ItemStack carried, FluidContainer container,
            List<FluidStorage> inputs) {
        Fluid fluid = container.content();
        for (FluidStorage tank : inputs) {
            if (tank.fill(fluid, container.capacity(), true) < container.capacity()) {
                continue;
            }
            tank.fill(fluid, container.capacity(), false);
            return ItemStack.of(FluidCells.empty(), 1);
        }
        return carried;
    }

    /** Fills an empty cell from the first tank that holds a whole cell worth of a fluid. */
    private static ItemStack fillAnEmptyCell(ItemStack carried, FluidContainer container,
            List<FluidStorage> outputs) {
        for (FluidStorage tank : outputs) {
            Fluid fluid = tank.fluid();
            if (fluid == null || tank.amount() < container.capacity()) {
                continue;
            }
            Item filled = FluidCells.filled(fluid);
            if (filled == null) {
                // A fluid the game has no cell of yet, so the tank keeps it.
                continue;
            }
            if (tank.drain(container.capacity(), true) < container.capacity()) {
                continue;
            }
            tank.drain(container.capacity(), false);
            return ItemStack.of(filled, 1);
        }
        return carried;
    }

    @Override
    public String toString() {
        return "CellExchange";
    }
}
