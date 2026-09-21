package com.philia093.neofactory.machine;

import com.badlogic.gdx.graphics.Color;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A kind of fluid a machine may hold.
 * <p>
 * Nothing in the world flows yet: fluids are the second half of the machine contracts,
 * the one a pump, a pipe or a tank will use. Keeping the kinds in a registry means a
 * machine, a recipe file and a tank can name the same fluid by name, exactly the way
 * items and blocks are named.
 */
public final class FluidType {

    /** Still water, the fluid of every lake and ocean. */
    public static final FluidType WATER = new FluidType("water",
            new Color(0.25f, 0.45f, 0.90f, 1.0f));

    /** Lava, the fluid of the deep layers of the world. */
    public static final FluidType LAVA = new FluidType("lava",
            new Color(0.95f, 0.45f, 0.10f, 1.0f));

    private static final Map<String, FluidType> BY_NAME = new LinkedHashMap<>();

    static {
        BY_NAME.put(WATER.name, WATER);
        BY_NAME.put(LAVA.name, LAVA);
    }

    private final String name;

    /**
     * Colour a screen fills a tank of this fluid with.
     * <p>
     * The game has no picture per fluid yet, so a tank shows its content as a coloured
     * level, see {@code MachineGui}. The colour has to be readable on the grey slot the tank
     * is drawn in.
     */
    private final Color color;

    private FluidType(String name, Color color) {
        this.name = Objects.requireNonNull(name, "name");
        this.color = new Color(color);
    }

    /** Name of this fluid, used by files and by the log. */
    public String name() {
        return name;
    }

    /**
     * Colour a screen fills a tank of this fluid with.
     *
     * @return the colour, do not modify it
     */
    public Color color() {
        return color;
    }

    /**
     * Looks a fluid up by its name.
     *
     * @param name name such as {@code "water"}
     * @return the fluid, or {@code null} when no fluid uses that name
     */
    public static FluidType byName(String name) {
        return name == null ? null : BY_NAME.get(name);
    }

    /** Every fluid the game knows, water first. */
    public static Collection<FluidType> all() {
        return BY_NAME.values();
    }

    @Override
    public String toString() {
        return "FluidType(" + name + ")";
    }
}
