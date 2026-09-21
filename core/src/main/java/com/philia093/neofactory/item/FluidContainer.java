package com.philia093.neofactory.item;

import com.philia093.neofactory.fluid.Fluid;

import java.util.Objects;

/**
 * What an item does with a fluid: carry some, pour it into the world and take it out again.
 * <p>
 * A container is either a bucket or a cell, and the difference is what it may carry. A bucket is
 * the tool for the fluids that stand in the world as a block and pour into it, see
 * {@link Fluid#bucketable()}: water and lava, and nothing else, which
 * {@link #bucket(Fluid)} refuses to build and {@link #accepts(Fluid)} refuses to fill. A cell is
 * the container of the industry, it takes any fluid a later machine, pipe or refinery brings.
 * <p>
 * The value is immutable, so an item is built with the container it will ever have: the empty
 * bucket and the water bucket are two items with two pictures, exactly like the game has no
 * bucket that is half full. What an item carries is therefore the item itself and not a field of
 * a stack, see {@link Buckets} for the table that maps a fluid back to its item.
 */
public final class FluidContainer {

    /** Amount of fluid a full container holds, in units of the game. */
    public static final int CAPACITY = 1000;

    /** Kind of a container, which is what decides what it may carry. */
    public enum Kind {

        /** A bucket, hard wired to water and lava. */
        BUCKET,

        /** A cell, the container that takes any fluid of the industry. */
        CELL
    }

    private final Kind kind;
    private final Fluid content;

    private FluidContainer(Kind kind, Fluid content) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.content = content;
    }

    /** An empty bucket, ready for water or lava. */
    public static FluidContainer bucket() {
        return new FluidContainer(Kind.BUCKET, null);
    }

    /**
     * A bucket that carries a fluid.
     *
     * @param fluid fluid inside, it must be one a bucket is allowed to carry
     * @return the container
     */
    public static FluidContainer bucket(Fluid fluid) {
        Fluid checked = Objects.requireNonNull(fluid, "fluid");
        if (!checked.bucketable()) {
            throw new IllegalArgumentException("A bucket never carries "
                    + checked.name() + ", only water and lava");
        }
        return new FluidContainer(Kind.BUCKET, checked);
    }

    /** An empty cell, ready for any fluid. */
    public static FluidContainer cell() {
        return new FluidContainer(Kind.CELL, null);
    }

    /**
     * A cell that carries a fluid.
     *
     * @param fluid fluid inside
     * @return the container
     */
    public static FluidContainer cell(Fluid fluid) {
        return new FluidContainer(Kind.CELL, Objects.requireNonNull(fluid, "fluid"));
    }

    /** Kind of this container. */
    public Kind kind() {
        return kind;
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
     * Only an empty container takes anything, and a bucket takes no more than the two fluids that
     * pour into the world: a bucket of oil is not a thing of this game, the cell of the fluid is.
     *
     * @param fluid fluid that is offered, may be {@code null}
     * @return {@code true} when the fluid goes into this container
     */
    public boolean accepts(Fluid fluid) {
        if (fluid == null || !isEmpty()) {
            return false;
        }
        return kind == Kind.CELL || fluid.bucketable();
    }

    /**
     * {@code true} when the picture of this container shows the fluid in a window.
     * <p>
     * A cell is drawn as one grey scale picture whose window is painted in the colour of the
     * fluid, see {@link com.philia093.neofactory.render.CellIconFactory}. A bucket is not: the art
     * pack draws a bucket of water and a bucket of lava as pictures of their own, so their colour
     * is already in the file and nothing is painted over it. The flag is asked by the icon of an
     * item and by the preview of the fluids, which keeps the two ways of showing a fluid apart.
     *
     * @return {@code true} when the window of this container takes the colour of its fluid
     */
    public boolean paintsItsWindow() {
        return kind == Kind.CELL && !isEmpty();
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
        return "FluidContainer(" + kind + ", " + (content == null ? "empty" : content.name()) + ")";
    }
}
