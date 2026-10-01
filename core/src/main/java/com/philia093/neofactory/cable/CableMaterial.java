package com.philia093.neofactory.cable;

import com.badlogic.gdx.graphics.Color;

import java.util.Objects;

/**
 * What a cable is made of: the highest voltage it may carry, how much current it takes and what it
 * loses on the way.
 * <p>
 * A cable is a line of the industry like a pipe, see {@code PipeMaterial}, and it is described the same
 * way: a table of the game names one row per material and the blocks are built from that table. What a
 * cable decides is <b>the power of a line</b>:
 * <ul>
 *     <li>{@link #voltage()} the tier of the line. A line of a higher voltage than the material of a
 *         cable is a line that cable may not carry, which is what the table of the industry means by
 *         "copper is a line of medium voltage";</li>
 *     <li>{@link #amperage()} how much current runs through one cable. A line of one ampere of medium
 *         voltage carries a hundred and twenty eight units a tick, and a line of four amperes of the
 *         same voltage carries four times that much, see {@link #throughput()};</li>
 *     <li>{@link #loss()} what one cable of the line takes away for every block an energy travels, so a
 *         long line loses more than a short one. A material whose loss is {@link #NO_LOSS} is a
 *         superconductor: it carries what it is given from one end to the other and loses nothing.</li>
 * </ul>
 * <p>
 * <b>The heat of the table is a level and not a temperature.</b> The table of the industry names a small
 * number for every cable that melts above it - one for the metals that give way first, four for the ones
 * that take the most - and leaves it empty for a material the table does not name one for, which is
 * {@link #NO_HEAT}. It is the heat of a line and no property of the energy, so it is compared with what
 * a line is asked to carry and never with the heat of a fluid, see {@code Fluids}.
 * <p>
 * <b>A material of a cable is a material of the game.</b> The cable table reaches the colour of its
 * material through the constants of {@code Materials}, exactly like the table of the pipes, because a
 * cable is a block and a block is painted long before the materials are declared.
 */
public final class CableMaterial {

    /** Loss of a cable that carries the energy of a line without losing any of it, a superconductor. */
    public static final int NO_LOSS = 0;

    /** Heat of a cable the table of the industry names no heat for. */
    public static final int NO_HEAT = 0;

    private final String name;
    private final String displayName;
    private final Color color;
    private final Voltage voltage;
    private final int amperage;
    private final int loss;
    private final int heat;

    /**
     * Creates a cable material.
     *
     * @param name technical name, such as {@code annealed_copper}
     * @param displayName name a player reads, such as {@code Annealed Copper}
     * @param color colour the grey scale art of a cable is painted in
     * @param voltage highest voltage a line of this material may carry
     * @param amperage current one cable of this material takes, at least one
     * @param loss energy one cable takes away per block an energy travels, {@link #NO_LOSS} for a
     *             superconductor
     * @param heat level of the heat a cable of this material takes, {@link #NO_HEAT} when the table
     *             names none
     * @throws IllegalArgumentException when an amount is out of its range
     */
    CableMaterial(String name, String displayName, Color color, Voltage voltage, int amperage,
            int loss, int heat) {
        this.name = Objects.requireNonNull(name, "name");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.color = new Color(Objects.requireNonNull(color, "color"));
        this.voltage = Objects.requireNonNull(voltage, "voltage");
        if (name.isBlank() || displayName.isBlank()) {
            throw new IllegalArgumentException("A cable material needs a name");
        }
        if (amperage < 1) {
            throw new IllegalArgumentException("A cable of " + name
                    + " carries no current at all, an amperage has to be at least one");
        }
        if (loss < 0) {
            throw new IllegalArgumentException("A cable of " + name
                    + " loses less than nothing, a material without a loss is a superconductor and says "
                    + NO_LOSS);
        }
        if (heat < 0) {
            throw new IllegalArgumentException("A cable of " + name
                    + " has a heat below the first level, and a material the table names none for says "
                    + NO_HEAT);
        }
        this.amperage = amperage;
        this.loss = loss;
        this.heat = heat;
    }

    /** Technical name of this material, also the name of its cables. */
    public String name() {
        return name;
    }

    /** Name of this material as a word, used where an item names what it is made of. */
    public String displayName() {
        return displayName;
    }

    /** Colour the grey scale art of a cable is painted in. */
    public Color color() {
        return color;
    }

    /** Highest voltage a line of this material may carry. */
    public Voltage voltage() {
        return voltage;
    }

    /** Current one cable of this material takes. */
    public int amperage() {
        return amperage;
    }

    /** Energy one cable of this material takes away per block an energy travels. */
    public int loss() {
        return loss;
    }

    /**
     * Heat of this material, the level of the table of the industry.
     *
     * @return the level, {@link #NO_HEAT} when the table names none
     */
    public int heat() {
        return heat;
    }

    /** {@code true} when the table of the industry names a heat for this material. */
    public boolean hasHeat() {
        return heat != NO_HEAT;
    }

    /** {@code true} when a cable of this material loses nothing, a superconductor. */
    public boolean isSuperconductor() {
        return loss == NO_LOSS;
    }

    /**
     * Energy a line of one cable of this material carries a tick.
     * <p>
     * The power of a line is what its voltage and its current come to: a line of medium voltage of one
     * ampere carries a hundred and twenty eight units a tick, and a cable of the same voltage that takes
     * four amperes carries four times as much.
     *
     * @return the amount in units of the game
     */
    public int throughput() {
        return voltage.euPerTick() * amperage;
    }

    @Override
    public String toString() {
        return "CableMaterial(" + name + ", " + voltage.fileName() + ", " + amperage + "A)";
    }
}
