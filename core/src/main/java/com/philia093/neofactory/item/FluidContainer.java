package com.philia093.neofactory.item;

import com.philia093.neofactory.fluid.Fluid;

import java.util.Objects;

/**
 * What an item does with a fluid: carry one in a cell.
 * <p>
 * The game has one container and no bucket, see {@link com.philia093.neofactory.fluid.Fluid}: the cell
 * of the industry, a body of steel with a window in it. A cell that is empty takes any fluid the game
 * knows and a cell that is full takes nothing more, so nothing about a fluid has to be spelled out
 * here - the colour it is painted in lives with the fluid itself.
 * <p>
 * The value is immutable, so an item is built with the container it will ever have: the empty cell and
 * the water cell are two items with two pictures, exactly like the game has no cell that is half full.
 * What an item carries is therefore the item itself and not a field of a stack, see {@link FluidCells}
 * for the table that maps a fluid back to its item.
 */
public final class FluidContainer {

    /** Amount of fluid a full cell holds, in units of the game. */
    public static final int CAPACITY = 1000;

    private final Fluid content;

    private FluidContainer(Fluid content) {
        this.content = content;
    }

    /** An empty cell, ready for any fluid. */
    public static FluidContainer cell() {
        return new FluidContainer(null);
    }

    /**
     * A cell that carries a fluid.
     *
     * @param fluid fluid inside
     * @return the container
     */
    public static FluidContainer cell(Fluid fluid) {
        return new FluidContainer(Objects.requireNonNull(fluid, "fluid"));
    }

    /**
     * Fluid this container carries.
     *
     * @return the fluid, or {@code null} while the container is empty
     */
    public Fluid content() {
        return content;
    }

    /** {@code true} while nothing is inside. */
    public boolean isEmpty() {
        return content == null;
    }

    /** Amount of fluid this container holds when it is full, see {@link #CAPACITY}. */
    public int capacity() {
        return CAPACITY;
    }

    /**
     * {@code true} when this container may take the fluid.
     * <p>
     * Only an empty container takes anything and a full one takes nothing more, so the game never mixes
     * two fluids in one cell. Which fluid it is does not matter: a cell takes every fluid the industry
     * brings, see {@link com.philia093.neofactory.fluid.Fluid}.
     *
     * @param fluid fluid that is offered, may be {@code null}
     * @return {@code true} when the fluid goes into this container
     */
    public boolean accepts(Fluid fluid) {
        return fluid != null && isEmpty();
    }

    /**
     * {@code true} when the picture of this container shows the fluid in a window.
     * <p>
     * A cell is drawn as one grey scale picture whose window is painted in the colour of the fluid, see
     * {@link com.philia093.neofactory.render.CellIconFactory}: the steel of the container stays grey
     * and only the window takes the colour. The flag is asked by the icon of an item, which is what
     * keeps the picture and the paint apart.
     *
     * @return {@code true} when the window of this container takes the colour of its fluid
     */
    public boolean paintsItsWindow() {
        return !isEmpty();
    }

    /**
     * {@code true} when this very fluid is inside.
     *
     * @param fluid fluid to compare with
     * @return {@code true} when the container carries it
     */
    public boolean holds(Fluid fluid) {
        return content != null && content == fluid;
    }

    @Override
    public String toString() {
        return "FluidContainer(" + (content == null ? "empty" : content.name()) + ")";
    }
}
