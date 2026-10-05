package com.philia093.neofactory.fluid;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.chemistry.Phase;
import com.philia093.neofactory.chemistry.Substance;
import com.philia093.neofactory.chemistry.Substances;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every fluid of the game.
 * <p>
 * The table is small on purpose: water, lava and the steam of the first machines, plus whatever the
 * industry adds later. A fluid is a name, the colour a tank and a cell are painted in and the
 * temperature it carries, see {@link Fluid}, and nothing else - the game has no fluid block, so a fluid
 * is never met in the world and is only ever carried by a tank or a cell.
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

    /**
     * Temperature water arrives at, in kelvin.
     * <p>
     * The fluids of the game keep the heat of the place they come from: the water of a river and of a
     * boiler that is still cold is three hundred kelvin, which is what a wooden pipe takes and what a
     * copper one has no trouble with, see {@link com.philia093.neofactory.pipe.PipeMaterial}.
     */
    public static final float WATER_TEMPERATURE = 300.0f;

    /**
     * Temperature lava arrives at, in kelvin.
     * <p>
     * Hotter than every pipe of the first materials: lava moves in bronze and in steel and bursts the
     * pipes of copper and of wood, see {@link com.philia093.neofactory.pipe.PipeMaterials}.
     */
    public static final float LAVA_TEMPERATURE = 1300.0f;

    /**
     * Temperature steam arrives at, in kelvin, the boiling point of water.
     * <p>
     * A steam pipe has to take this and no less: a wooden pipe of three hundred and fifty kelvin carries
     * the water of a river and bursts the moment the steam of a boiler reaches it.
     */
    public static final float STEAM_TEMPERATURE = 373.0f;

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
        Substances catalog = Substances.starter();
        Substance water = catalog.byName("water");
        WATER = register(new Fluid("water", WATER_COLOR, WATER_TEMPERATURE,
                water.formula(), Phase.LIQUID, java.util.List.of(water)));
        // Steam is the very same water with more heat in it: the two fluids answer with one substance
        // and only their state tells them apart, which is what keeps a boiler from making something
        // out of nothing.
        STEAM = register(new Fluid("steam", STEAM_COLOR, STEAM_TEMPERATURE,
                water.formula(), Phase.GAS, java.util.List.of(water)));
        // Lava is a mixture of a mountain and no substance: it carries no formula and the chemistry of
        // the industry can make nothing out of it.
        LAVA = register(new Fluid("lava", LAVA_COLOR, LAVA_TEMPERATURE));
        for (Substance substance : catalog.all()) {
            if (substance.phase() == Phase.SOLID || BY_NAME.containsKey(substance.name())) {
                continue;
            }
            // Every liquid, gas and solution of the catalog is a fluid of its own, so it can be poured
            // into a tank, carried in a cell and painted in the colour of what it is made of.
            register(new Fluid(substance.name(), colorOf(substance), substance.formula(),
                    substance.phase(), substance));
        }
    }

    /**
     * The colour a substance is painted in, which is the colour of its fluid and of its cell.
     * <p>
     * The colour of an element stands in the table below; a substance of several elements takes the
     * colour of the one it holds most of, and one the table does not know is drawn pale, so a substance
     * is always a picture and never a blank window. <b>Nothing here is a fact of the chemistry</b> - a
     * colour is a thing a player reads - and no reaction ever asks for it.
     *
     * @param substance substance to paint
     * @return the colour of it
     */
    public static Color colorOf(Substance substance) {
        if (substance == null) {
            return PALE;
        }
        String heaviest = null;
        int most = 0;
        for (Map.Entry<String, Integer> entry : substance.chemical().composition().byElement()
                .entrySet()) {
            if (entry.getValue() > most || (entry.getValue() == most && heaviest != null
                    && entry.getKey().compareTo(heaviest) < 0)) {
                most = entry.getValue();
                heaviest = entry.getKey();
            }
        }
        return heaviest == null ? PALE : COLORS.getOrDefault(heaviest, PALE);
    }

    /** Colour a substance of an element the table does not know is drawn in. */
    private static final Color PALE = new Color(0.72f, 0.72f, 0.76f, 1f);

    /** The colours of the elements the industry of the game is written with. */
    private static final Map<String, Color> COLORS = new LinkedHashMap<>();

    static {
        put("H", 0.86f, 0.90f, 0.96f);
        put("C", 0.22f, 0.22f, 0.24f);
        put("N", 0.45f, 0.55f, 0.85f);
        put("O", 0.85f, 0.35f, 0.35f);
        put("Na", 0.90f, 0.85f, 0.60f);
        put("Mg", 0.76f, 0.78f, 0.80f);
        put("Al", 0.86f, 0.88f, 0.92f);
        put("Si", 0.55f, 0.55f, 0.60f);
        put("S", 0.90f, 0.80f, 0.25f);
        put("Cl", 0.55f, 0.85f, 0.35f);
        put("K", 0.80f, 0.70f, 0.85f);
        put("Ca", 0.90f, 0.90f, 0.85f);
        put("Ti", 0.70f, 0.72f, 0.75f);
        put("Cr", 0.60f, 0.65f, 0.70f);
        put("Mn", 0.72f, 0.62f, 0.55f);
        put("Fe", 0.78f, 0.76f, 0.74f);
        put("Ni", 0.70f, 0.75f, 0.70f);
        put("Cu", 0.85f, 0.55f, 0.35f);
        put("Zn", 0.75f, 0.78f, 0.82f);
        put("Sn", 0.82f, 0.84f, 0.86f);
        put("Pb", 0.55f, 0.58f, 0.65f);
        put("Ag", 0.90f, 0.92f, 0.95f);
        put("Au", 0.95f, 0.82f, 0.30f);
        put("W", 0.45f, 0.50f, 0.55f);
    }

    /** Names the colour of one element. */
    private static void put(String symbol, float red, float green, float blue) {
        COLORS.put(symbol, new Color(red, green, blue, 1f));
    }

    /**
     * Keeps a fluid by name.
     *
     * @param fluid the fluid, whose name is the name a save file stores
     * @return the fluid, ready to be used by an item or a machine
     */
    private static Fluid register(Fluid fluid) {
        BY_NAME.put(fluid.name(), fluid);
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
