package com.philia093.neofactory.fluid;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.chemistry.Phase;
import com.philia093.neofactory.chemistry.Substance;

import java.util.List;
import java.util.Objects;

/**
 * A kind of fluid, the water of a cell and the steam of a boiler.
 * <p>
 * A fluid is the single source of truth for everything that belongs to it: the colour a tank and the
 * cell of an item paint it in and the name a save file stores. Nothing else in the game knows the
 * difference between water and steam, so a tank, a pipe and a cell all ask the fluid instead of
 * spelling a name out.
 * <p>
 * <b>A fluid has no place in the world.</b> It is a material of the industry: a machine holds it in a
 * tank, a cell carries it and a recipe asks for it. A world of cubes is made of blocks that do not run,
 * so the game has no fluid block and no spread, and nothing here describes a block, a picture or a
 * distance a fluid reaches. Water of a landscape therefore arrives as an item or in a tank and never as
 * a cell of the land.
 * <p>
 * <b>One container and no bucket.</b> Every fluid of the game travels in the same cell, see
 * {@link com.philia093.neofactory.item.FluidContainer}: a body of steel with a window the colour of the
 * fluid shows through. The buckets went with the fluids that used to stand in the world as a block, so
 * a fluid no longer has to say whether one may carry it.
 *
 * <p>
 * <b>A fluid has a temperature.</b> {@link #temperature()} is how hot it is, in kelvin, and it is the
 * whole of what a pipe has to know about it: a pipe carries a fluid while the fluid is no hotter than
 * the heat its {@link com.philia093.neofactory.pipe.PipeMaterial material} takes and bursts when it is,
 * see {@code PipeTransport}. The temperature belongs to the fluid and not to the tank it lies in, so
 * water of three hundred kelvin stays water of three hundred kelvin wherever it runs.
 *
 * @param name name used by files and by the log, never blank
 * @param color colour the fluid is painted in, the tint of its tank and of its cell
 * @param temperature how hot the fluid is, in kelvin, always above zero
 */
public record Fluid(String name, Color color, float temperature, String formula, Phase state,
        List<Substance> substances) {

    /**
     * Temperature of a fluid that is written without one, the temperature of a room.
     * <p>
     * The fluids of the game name their heat, see {@link Fluids}; the constant is for the fluids a test
     * or a later age writes down in one line, where the colour is the whole of what the fluid is about.
     */
    public static final float ROOM_TEMPERATURE = 300.0f;

    /**
     * Creates a fluid of room temperature that carries nothing.
     *
     * @param name name used by files and by the log, never blank
     * @param color colour the fluid is painted in
     */
    public Fluid(String name, Color color) {
        this(name, color, ROOM_TEMPERATURE, "", null, List.of());
    }

    /**
     * Creates a fluid that carries nothing.
     *
     * @param name name used by files and by the log, never blank
     * @param color colour the fluid is painted in
     * @param temperature how hot the fluid is, in kelvin
     */
    public Fluid(String name, Color color, float temperature) {
        this(name, color, temperature, "", null, List.of());
    }

    /**
     * Creates a fluid that stands for one substance.
     *
     * @param name name used by files and by the log, never blank
     * @param color colour the fluid is painted in
     * @param formula the chemical formula a player reads, empty for a fluid that has none
     * @param state whether the substance is a gas or a liquid
     * @param substance the substance the fluid is
     */
    public Fluid(String name, Color color, String formula, Phase state, Substance substance) {
        this(name, color, ROOM_TEMPERATURE, formula, state, List.of(substance));
    }

    /** Checks the fields, so a broken fluid fails while it is registered. */
    public Fluid {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(color, "color");
        formula = formula == null ? "" : formula;
        substances = List.copyOf(substances == null ? List.of() : substances);
        if (name.isBlank()) {
            throw new IllegalArgumentException("The name of a fluid must not be blank");
        }
        if (!(temperature > 0.0f)) {
            throw new IllegalArgumentException("The temperature of " + name
                    + " is not above zero, and a fluid that is not there is no fluid");
        }
        // The colour is handed out to renderers, so the copy keeps the one that was given
        // to the builder from being changed behind the back of the fluid.
        color = new Color(color);
    }

    /**
     * {@code true} when this fluid is hotter than something that may hold it.
     *
     * @param kelvin temperature of the thing, in kelvin
     * @return {@code true} when the fluid is above it
     */
    public boolean hotterThan(float kelvin) {
        return temperature > kelvin;
    }

    /**
     * The chemical formula of this fluid, the way it is printed under the name of its cell.
     *
     * @return the formula, empty when the fluid has none - lava is a mixture and not a substance
     */
    public String formula() {
        return formula;
    }

    /** {@code true} when this fluid names a formula a player reads. */
    public boolean hasFormula() {
        return !formula.isEmpty();
    }

    /**
     * The substances this fluid is, in millibuckets of a full cell each.
     * <p>
     * A fluid is a substance and not a name of its own: water is H2O and steam is the very same water
     * with more heat in it, so the two of them answer with the same substance and only their state
     * differs. A fluid of the world that is no substance - the lava of a mountain - carries none, which
     * is what tells the chemistry of the industry that nothing can be made out of it.
     *
     * @return the substances, empty for a fluid that is a mixture and not a substance
     */
    public List<Substance> substances() {
        return substances;
    }

    /** {@code true} when this fluid is a substance the chemistry of the industry can work with. */
    public boolean isSubstance() {
        return !substances.isEmpty();
    }

    @Override
    public String toString() {
        return "Fluid(" + name + ")";
    }
}
