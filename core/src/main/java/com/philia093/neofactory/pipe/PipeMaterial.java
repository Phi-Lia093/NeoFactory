package com.philia093.neofactory.pipe;

import com.badlogic.gdx.graphics.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * What a pipe is made of: how it looks, how hot it may get and how much it carries.
 * <p>
 * A pipe exists in two of these at once - a {@link PipeMaterial material} and a {@link PipeSize size} - and
 * every material carries the very same seven sizes, so the two tables meet in the blocks of
 * {@link Pipes} and nowhere else.
 * <p>
 * <b>The material decides three things.</b>
 * <ul>
 *     <li>the colour the grey scale art of its {@link PipeTexture family} is painted in, which is what
 *         makes a copper pipe copper without a picture of its own;</li>
 *     <li>{@link #maxTemperature()}, the temperature a fluid may have inside it - a wooden pipe carries
 *         the water of a river and bursts with the steam of a boiler, a steel pipe carries the steam of
 *         the first machines;</li>
 *     <li>{@link #flow(PipeSize)} what it moves a second, which grows with its size.</li>
 * </ul>
 * <b>Not every material comes in every size.</b> A flow rate of {@link #NOT_MADE} says that the game has
 * no such pipe - wood is made of three tubes and no bundle, and a later material may stop at the huge
 * pipe - and those cells of the table are simply left empty, see {@link #hasSize(PipeSize)}. It is the
 * table of the game and no rule of arithmetic: every cell and every temperature is a decision of the
 * design and nothing but the order of the table depends on the numbers, see {@link PipeMaterials}.
 */
public final class PipeMaterial {

    /**
     * Flow rate of a size this material is not made in.
     * <p>
     * A cell of the table left empty, see {@link PipeMaterials}: the game holds no pipe of that material
     * and that size, so {@link #hasSize(PipeSize)} answers {@code false} for it.
     */
    public static final int NOT_MADE = 0;

    private final String name;
    private final String displayName;
    private final Color color;
    private final PipeTexture texture;
    private final float maxTemperature;
    private final int[] flow;

    /**
     * Creates a pipe material.
     *
     * @param name technical name, such as {@code bronze}
     * @param displayName name a player reads, such as {@code Bronze}
     * @param color colour the grey scale art is painted in, ignored by a family with its own colours
     * @param texture family of art this material is drawn from
     * @param maxTemperature hottest fluid this material carries, in kelvin
     * @param flow what it moves a second in one pipe of each size, in millibuckets, one per
     *             {@link PipeSize}, in the order the sizes are declared; a cell left at
     *             {@link #NOT_MADE} says the material is not made in that size
     */
    PipeMaterial(String name, String displayName, Color color, PipeTexture texture,
            float maxTemperature, int... flow) {
        this.name = Objects.requireNonNull(name, "name");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.color = new Color(Objects.requireNonNull(color, "color"));
        this.texture = Objects.requireNonNull(texture, "texture");
        if (name.isBlank() || displayName.isBlank()) {
            throw new IllegalArgumentException("A pipe material needs a name");
        }
        if (maxTemperature <= 0.0f) {
            throw new IllegalArgumentException("The pipes of " + name
                    + " carry no fluid at all, a temperature has to be above zero");
        }
        if (flow.length != PipeSize.values().length) {
            throw new IllegalArgumentException("The pipes of " + name + " name " + flow.length
                    + " flow rates, but the game holds " + PipeSize.values().length + " sizes");
        }
        int made = 0;
        for (int rate : flow) {
            if (rate < 0) {
                throw new IllegalArgumentException("A pipe of " + name
                        + " moves less than nothing, and a size it is not made in is left at "
                        + NOT_MADE);
            }
            if (rate > 0) {
                made++;
            }
        }
        if (made == 0) {
            throw new IllegalArgumentException("The pipes of " + name
                    + " are made in no size at all, which is no material");
        }
        this.maxTemperature = maxTemperature;
        this.flow = flow.clone();
    }

    /** Technical name of this material, also the name of its blocks. */
    public String name() {
        return name;
    }

    /** Name of this material as a word, used where an item names what it is made of. */
    public String displayName() {
        return displayName;
    }

    /** Colour the grey scale art is painted in. */
    public Color color() {
        return color;
    }

    /** Family of art this material is drawn from. */
    public PipeTexture texture() {
        return texture;
    }

    /**
     * Hotter fluid than this bursts a pipe of this material.
     *
     * @return the temperature in kelvin
     */
    public float maxTemperature() {
        return maxTemperature;
    }

    /**
     * What one pipe of a size moves a second.
     *
     * @param size size of the pipe
     * @return the flow rate in millibuckets per second, {@link #NOT_MADE} for a size this material is
     *         not made in
     */
    public int flow(PipeSize size) {
        return flow[size.ordinal()];
    }

    /**
     * {@code true} when this material is made in a size.
     *
     * @param size size to ask about
     * @return {@code true} when the game holds a pipe of that size
     */
    public boolean hasSize(PipeSize size) {
        return flow[size.ordinal()] > NOT_MADE;
    }

    /**
     * The sizes this material is made in, in the order the sizes are declared.
     *
     * @return the sizes, never empty
     */
    public List<PipeSize> sizes() {
        List<PipeSize> made = new ArrayList<>();
        for (PipeSize size : PipeSize.values()) {
            if (hasSize(size)) {
                made.add(size);
            }
        }
        return List.copyOf(made);
    }

    @Override
    public String toString() {
        return "PipeMaterial(" + name + ", up to " + maxTemperature + "K)";
    }
}
