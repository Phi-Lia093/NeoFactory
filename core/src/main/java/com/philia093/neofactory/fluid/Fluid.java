package com.philia093.neofactory.fluid;

import com.badlogic.gdx.graphics.Color;

import java.util.Objects;

/**
 * A kind of fluid, the water of a bucket and the lava of a cell.
 * <p>
 * A fluid is the single source of truth for everything that belongs to it: the colour a tank and the
 * cell of an item paint it in, the name a save file stores and whether a bucket may carry it. Nothing
 * else in the game knows the difference between water and lava, so a tank, a bucket and a cell all ask
 * the fluid instead of spelling a name out.
 * <p>
 * <b>A fluid has no place in the world.</b> It is a material of the industry: a machine holds it in a
 * tank, an item carries it in a bucket or a cell, and a recipe asks for it. A world of cubes is made of
 * blocks that do not run, so the game has no fluid block and no spread, and nothing here describes a
 * block, a picture or a distance a fluid reaches. Water of a landscape therefore arrives as an item or
 * in a tank and never as a cell of the land.
 *
 * @param name name used by files and by the log, never blank
 * @param color colour the fluid is painted in, the tint of its tank and of its cell
 * @param bucketable {@code true} when a bucket may carry this fluid
 */
public record Fluid(String name, Color color, boolean bucketable) {

    /** Checks the fields, so a broken fluid fails while it is registered. */
    public Fluid {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(color, "color");
        if (name.isBlank()) {
            throw new IllegalArgumentException("The name of a fluid must not be blank");
        }
        // The colour is handed out to renderers, so the copy keeps the one that was given
        // to the builder from being changed behind the back of the fluid.
        color = new Color(color);
    }

    @Override
    public String toString() {
        return "Fluid(" + name + ")";
    }
}
