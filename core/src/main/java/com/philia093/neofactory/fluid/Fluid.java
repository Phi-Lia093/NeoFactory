package com.philia093.neofactory.fluid;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.block.Block;

import java.util.Objects;

/**
 * A kind of fluid, the water of a lake and the lava of the deep layers.
 * <p>
 * A fluid is the single source of truth for everything that belongs to it: the colour the
 * world and the interface paint it in, the grey scale picture its animation is cut from, how
 * far it spreads and whether a bucket may carry it. Nothing else in the game knows the
 * difference between water and lava, so a block, a tank, a bucket and a cell all ask the
 * fluid instead of spelling a name out.
 * <p>
 * <b>Grey scale pictures:</b> the sheet of a fluid holds brightness only and never a colour
 * of its own. The colour is {@link #color()} and is multiplied with the picture while it is
 * drawn, which is why a new fluid needs one new line in {@link Fluids} and not a new picture
 * per place it shows up: world, tank, bucket and cell all receive their colour from here.
 * <p>
 * <b>Still and spread:</b> a fluid has no faces, the view is from above, so one sheet of
 * {@link #frames} frames is all it needs. {@link #range()} says how many cells it reaches
 * away from its source and {@link #tickInterval()} how long it takes to reach the next ring,
 * see {@code FluidSpread} and {@code FluidFlow}.
 *
 * @param name name used by files and by the log, never blank
 * @param color colour the fluid is painted in, the tint of its block and of its cell
 * @param stillTexture name of the grey scale sheet, relative to the {@code blocks/} folder and
 *                     without extension, for example {@code generic_fluid}
 * @param frames amount of cells in the sheet, at least one
 * @param frameTicks amount of ticks one cell is shown, at least one
 * @param range amount of cells the fluid spreads away from its source, at least one
 * @param tickInterval amount of ticks between two rings of the spread, at least one
 * @param bucketable {@code true} when a bucket may carry this fluid
 * @param block block this fluid stands in the world with
 */
public record Fluid(String name, Color color, String stillTexture, int frames, int frameTicks,
        int range, int tickInterval, boolean bucketable, Block block) {

    /** Checks the fields, so a broken fluid fails while it is registered. */
    public Fluid {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(stillTexture, "stillTexture");
        Objects.requireNonNull(block, "block");
        if (name.isBlank()) {
            throw new IllegalArgumentException("The name of a fluid must not be blank");
        }
        if (stillTexture.isBlank()) {
            throw new IllegalArgumentException("A fluid needs a picture: " + name);
        }
        if (frames < 1) {
            throw new IllegalArgumentException("A fluid needs at least one frame: " + frames);
        }
        if (frameTicks < 1) {
            throw new IllegalArgumentException("A frame lasts at least one tick: " + frameTicks);
        }
        if (range < 1) {
            throw new IllegalArgumentException("A fluid spreads at least one cell: " + range);
        }
        if (tickInterval < 1) {
            throw new IllegalArgumentException("A spread takes at least one tick: " + tickInterval);
        }
        // The colour is handed out to renderers, so the copy keeps the one that was given
        // to the builder from being changed behind the back of the fluid.
        color = new Color(color);
    }

    /** {@code true} when the sheet of this fluid holds more than a single frame. */
    public boolean hasAnimation() {
        return frames > 1;
    }

    /**
     * Ticks one round through the sheet takes.
     *
     * @return {@code frames * frameTicks}, never zero
     */
    public int cycleTicks() {
        return frames * frameTicks;
    }

    /**
     * {@code true} when this fluid spreads from its source at all.
     *
     * @return always {@code true}, a fluid with a range of one cell stays where it is poured
     */
    public boolean spreads() {
        return range > 1;
    }

    @Override
    public String toString() {
        return "Fluid(" + name + ")";
    }
}
