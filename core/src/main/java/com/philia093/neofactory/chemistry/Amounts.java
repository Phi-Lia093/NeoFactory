package com.philia093.neofactory.chemistry;

/**
 * How much of a substance a piece of the industry carries, in millibuckets.
 * <p>
 * A workshop moves a substance around in pieces and not in grams, and a piece says how much of it there is:
 * a block of a metal is a thousand millibuckets, an ingot is a hundred, a nugget is ten, and a cell is a
 * thousand whatever it is filled with. Those numbers are the one place the size of a piece is written down,
 * so that a reaction which spends an ingot and one which spends a nugget are told apart by how much they
 * spent and not by two different words for metal, see {@link Blend}.
 * <p>
 * <b>A millibucket is the amount a balance is written in.</b> Every substance a machine holds is held as
 * some number of millibuckets, and the atoms of that substance follow from how much of it there is, so a
 * reaction conserves millibuckets of each element and never the pieces a player put in. The numbers here
 * are not the balance itself: they only say how many millibuckets of a thing one piece of it is.
 */
public final class Amounts {

    /** Millibuckets of a substance one block of it holds. */
    public static final Fraction BLOCK = Fraction.of(1000);

    /** Millibuckets of a substance one ingot of it holds. */
    public static final Fraction INGOT = Fraction.of(100);

    /** Millibuckets of a substance one nugget of it holds. */
    public static final Fraction NUGGET = Fraction.of(10);

    /** Millibuckets a full cell of fluid carries. */
    public static final Fraction FLUID_CELL = Fraction.of(1000);

    private Amounts() {
        // Utility class: never instantiated.
    }
}
