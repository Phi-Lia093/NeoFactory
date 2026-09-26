package com.philia093.neofactory.item;

import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.fluid.FluidStorage;

/**
 * Trades the cell a player carries with one tank of a machine.
 * <p>
 * <b>A tank holds a fluid the way a number grows, a cell holds it in whole thousands.</b> What a machine
 * has in its tank is continuous - a boiler boils twenty units of water a second, a recipe drains whatever
 * amount it asks for - while a cell only ever carries a full thousand or nothing at all, see
 * {@link FluidContainer#CAPACITY}. The two therefore meet in one place: a click that moves a whole cell's
 * worth of fluid or moves nothing.
 * <p>
 * <b>Pouring</b> takes a full cell out of the hand and puts it into a tank a machine takes fluid from. It
 * only happens when the tank is empty or holds the very fluid of the cell and still has room for all of
 * it, so a tank is never mixed and a cell is never poured in part. <b>Filling</b> does the other
 * direction: an empty cell in the hand is filled from a tank a machine makes fluid in, and only when that
 * tank holds a whole cell's worth. A cell that is only half poured, or half filled, is not a thing of this
 * game, which is why every move is asked with a simulated call first.
 * <p>
 * The class is plain item arithmetic and knows nothing about a screen, a mouse or a machine: a container
 * hands it the stack the mouse carries and the tank that was clicked, see
 * {@code ContainerMenu#setTankFinder}.
 */
public final class CellTransfer {

    /** Amount of fluid one cell carries, the step every transfer is made of, in millibuckets. */
    public static final int CELL_AMOUNT = FluidContainer.CAPACITY;

    /**
     * What one click did.
     *
     * @param carried stack the hand carries afterwards, the very stack that was given when nothing moved
     * @param moved {@code true} when fluid went from the cell into the tank, or the other way round
     */
    public record Result(ItemStack carried, boolean moved) {
    }

    private CellTransfer() {
        // Utility class: never instantiated.
    }

    /**
     * Pours the cell of a hand into a tank a machine takes fluid from.
     * <p>
     * The tank has to be empty or to hold the same fluid with room for a whole cell, and the hand has to
     * carry exactly one full cell.
     *
     * @param carried stack the hand holds, may be {@code null} or empty
     * @param tank tank that was clicked, may be {@code null}
     * @return what the hand carries afterwards, {@code moved} tells whether anything happened
     */
    public static Result pour(ItemStack carried, FluidStorage tank) {
        FluidContainer container = fullCell(carried);
        if (container == null || tank == null) {
            return nothing(carried);
        }
        Fluid fluid = container.content();
        if (tank.fill(fluid, CELL_AMOUNT, true) < CELL_AMOUNT) {
            // The tank holds another fluid or has less room than a cell needs, so the metal of the cell
            // stays closed and the player keeps what they came with.
            return nothing(carried);
        }
        tank.fill(fluid, CELL_AMOUNT, false);
        return new Result(ItemStack.of(FluidCells.empty(), 1), true);
    }

    /**
     * Fills the empty cell of a hand from a tank a machine makes fluid in.
     * <p>
     * The tank has to hold a whole cell's worth of a fluid the game has a cell for, and the hand has to
     * carry exactly one empty cell.
     *
     * @param carried stack the hand holds, may be {@code null} or empty
     * @param tank tank that was clicked, may be {@code null}
     * @return what the hand carries afterwards, {@code moved} tells whether anything happened
     */
    public static Result fill(ItemStack carried, FluidStorage tank) {
        if (emptyCell(carried) == null || tank == null) {
            return nothing(carried);
        }
        Fluid fluid = tank.fluid();
        if (fluid == null || tank.amount() < CELL_AMOUNT) {
            return nothing(carried);
        }
        Item filled = FluidCells.filled(fluid);
        if (filled == null) {
            // A fluid the game has no cell of yet: the tank keeps it rather than losing it.
            return nothing(carried);
        }
        if (tank.drain(CELL_AMOUNT, true) < CELL_AMOUNT) {
            return nothing(carried);
        }
        tank.drain(CELL_AMOUNT, false);
        return new Result(ItemStack.of(filled, 1), true);
    }

    /**
     * The container of a full cell a hand carries alone.
     *
     * @param carried stack the hand holds, may be {@code null}
     * @return the container, or {@code null} when the hand carries no single full cell
     */
    private static FluidContainer fullCell(ItemStack carried) {
        FluidContainer container = carriedCell(carried);
        return container == null || container.isEmpty() ? null : container;
    }

    /**
     * The container of an empty cell a hand carries alone.
     *
     * @param carried stack the hand holds, may be {@code null}
     * @return the container, or {@code null} when the hand carries no single empty cell
     */
    private static FluidContainer emptyCell(ItemStack carried) {
        FluidContainer container = carriedCell(carried);
        return container == null || !container.isEmpty() ? null : container;
    }

    /**
     * The container of the cell a hand carries.
     * <p>
     * The hand has to carry one single cell: a filled cell stacks to one piece anyway, and a stack of empty
     * ones would have nowhere to put the full cell it earns.
     *
     * @param carried stack the hand holds, may be {@code null}
     * @return the container, or {@code null} when the hand carries no single cell
     */
    private static FluidContainer carriedCell(ItemStack carried) {
        if (carried == null || carried.isEmpty() || carried.count() != 1) {
            return null;
        }
        return carried.item().container();
    }

    /** What a hand keeps when a click moved nothing. */
    private static Result nothing(ItemStack carried) {
        return new Result(carried == null ? ItemStack.EMPTY : carried, false);
    }

    @Override
    public String toString() {
        return "CellTransfer(" + CELL_AMOUNT + " mB per cell)";
    }
}
