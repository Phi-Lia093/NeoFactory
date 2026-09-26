package com.philia093.neofactory.pipe;

import com.badlogic.gdx.graphics.Color;

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
 * <b>The numbers of the table are placeholders.</b> Whether a bronze pipe carries more than a copper one
 * and by how much is a question of the balance of the game and is decided later; the flow rates written
 * in {@link PipeMaterials} are a first guess that keeps the sizes and the materials in a sane order - a
 * larger size carries more, a better material takes more heat - and nothing else depends on the exact
 * value.
 */
public final class PipeMaterial {

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
     *             {@link PipeSize}, in the order the sizes are declared
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
        for (int rate : flow) {
            if (rate <= 0) {
                throw new IllegalArgumentException("A pipe of " + name
                        + " moves nothing at all, a flow rate has to be above zero");
            }
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
     * @return the flow rate in millibuckets per second
     */
    public int flow(PipeSize size) {
        return flow[size.ordinal()];
    }

    @Override
    public String toString() {
        return "PipeMaterial(" + name + ", up to " + maxTemperature + "K)";
    }
}
