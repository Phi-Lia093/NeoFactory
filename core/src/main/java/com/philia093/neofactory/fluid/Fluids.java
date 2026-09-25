package com.philia093.neofactory.fluid;

import com.badlogic.gdx.graphics.Color;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every fluid of the game.
 * <p>
 * The table is small on purpose: water, lava and the steam of the first machines, plus whatever the
 * industry adds later. A fluid is a name and the colour a tank and a cell are painted in, see
 * {@link Fluid}, and nothing else - the game has no fluid block, so a fluid is never met in the world
 * and is only ever carried by a tank or a cell.
 */
public final class Fluids {

    /**
     * Colour water is painted in.
     * <p>
     * The colour serves the tank of a machine and the cell of an item: drawn as {@code art * colour}
     * the water of a cell and the water of a tank look the way the art of the pack intends, and a
     * new fluid is one colour and one line in {@link #registerAll()}.
     */
    public static final Color WATER_COLOR = new Color(0.19f, 0.28f, 1.0f, 0.70f);

    /** Colour lava is painted in, the hue of the orange art it is cut from. */
    public static final Color LAVA_COLOR = new Color(1.0f, 0.38f, 0.06f, 1.0f);

    /**
     * Colour steam is painted in.
     * <p>
     * A pale grey that is barely opaque, which is what makes a tank of steam read as vapour next to the
     * blue of water: the colour is all the art a fluid has, see {@link #WATER_COLOR}.
     */
    public static final Color STEAM_COLOR = new Color(0.86f, 0.89f, 0.93f, 0.55f);

    /** Water, the fluid of the sea of a landscape and of the recipes of the industry. */
    public static Fluid WATER;

    /** Lava, the hot fluid of the deep layers of the world. */
    public static Fluid LAVA;

    /** Steam, what a boiler makes out of water and what the first machines run on. */
    public static Fluid STEAM;

    private static final Map<String, Fluid> BY_NAME = new LinkedHashMap<>();

    private Fluids() {
        // Utility class: never instantiated.
    }

    /**
     * Creates every fluid of the game.
     * <p>
     * Called once during startup. A fluid is handed out to a tank, a cell and a recipe, so
     * this table has to be written before the first of them is used. It touches no registry of its own
     * and may therefore be filled at any point during startup.
     */
    public static void registerAll() {
        if (!BY_NAME.isEmpty()) {
            return;
        }
        WATER = register("water", WATER_COLOR);
        LAVA = register("lava", LAVA_COLOR);
        STEAM = register("steam", STEAM_COLOR);
    }

    /**
     * Keeps a fluid by name.
     *
     * @param name name of the fluid, the name a save file stores
     * @param color colour the fluid is painted in
     * @return the fluid, ready to be used by an item or a machine
     */
    private static Fluid register(String name, Color color) {
        Fluid fluid = new Fluid(name, color);
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
