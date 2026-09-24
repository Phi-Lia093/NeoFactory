package com.philia093.neofactory.fluid;

import com.badlogic.gdx.graphics.Color;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every fluid of the game.
 * <p>
 * The table is small on purpose: water and lava, plus whatever the industry adds later. A fluid is a
 * name, the colour a tank and a cell are painted in and whether a bucket may carry it, see
 * {@link Fluid}, and nothing else - the game has no fluid block, so a fluid is never met in the world
 * and is only ever carried by a tank, a bucket or a cell.
 */
public final class Fluids {

    /**
     * Colour water is painted in.
     * <p>
     * The colour serves the tank of a machine and the cell of an item: drawn as {@code art * colour}
     * the water of a bucket and the water of a tank look the way the art of the pack intends, and a
     * new fluid is one colour and one line in {@link #registerAll()}.
     */
    public static final Color WATER_COLOR = new Color(0.19f, 0.28f, 1.0f, 0.70f);

    /** Colour lava is painted in, the hue of the orange art it is cut from. */
    public static final Color LAVA_COLOR = new Color(1.0f, 0.38f, 0.06f, 1.0f);

    /** Water, the fluid of the sea of a landscape and of the recipes of the industry. */
    public static Fluid WATER;

    /** Lava, the hot fluid of the deep layers of the world. */
    public static Fluid LAVA;

    private static final Map<String, Fluid> BY_NAME = new LinkedHashMap<>();

    private Fluids() {
        // Utility class: never instantiated.
    }

    /**
     * Creates every fluid of the game.
     * <p>
     * Called once during startup. A fluid is handed out to a tank, a bucket, a cell and a recipe, so
     * this table has to be written before the first of them is used. It touches no registry of its own
     * and may therefore be filled at any point during startup.
     */
    public static void registerAll() {
        if (!BY_NAME.isEmpty()) {
            return;
        }
        WATER = register("water", WATER_COLOR, true);
        LAVA = register("lava", LAVA_COLOR, true);
    }

    /**
     * Keeps a fluid by name.
     *
     * @param name name of the fluid, the name a save file stores
     * @param color colour the fluid is painted in
     * @param bucketable {@code true} when a bucket may carry the fluid
     * @return the fluid, ready to be used by an item or a machine
     */
    private static Fluid register(String name, Color color, boolean bucketable) {
        Fluid fluid = new Fluid(name, color, bucketable);
        BY_NAME.put(name, fluid);
        return fluid;
    }

    /**
     * Looks a fluid up by its name.
     *
     * @param name name such as {@code "water"}
     * @return the fluid, or {@code null} when no fluid uses that name
     */
    public static Fluid byName(String name) {
        return name == null ? null : BY_NAME.get(name);
    }

    /** Every fluid the game knows, water first. */
    public static Collection<Fluid> all() {
        return BY_NAME.values();
    }
}
