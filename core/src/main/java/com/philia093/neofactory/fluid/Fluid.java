package com.philia093.neofactory.fluid;

import com.badlogic.gdx.graphics.Color;

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
public record Fluid(String name, Color color, float temperature) {

    /**
     * Temperature of a fluid that is written without one, the temperature of a room.
     * <p>
     * The fluids of the game name their heat, see {@link Fluids}; the constant is for the fluids a test
     * or a later age writes down in one line, where the colour is the whole of what the fluid is about.
     */
    public static final float ROOM_TEMPERATURE = 300.0f;

    /**
     * Creates a fluid of room temperature.
     *
     * @param name name used by files and by the log, never blank
     * @param color colour the fluid is painted in
     */
    public Fluid(String name, Color color) {
        this(name, color, ROOM_TEMPERATURE);
    }

    /** Checks the fields, so a broken fluid fails while it is registered. */
    public Fluid {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(color, "color");
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

    @Override
    public String toString() {
        return "Fluid(" + name + ")";
    }
}
