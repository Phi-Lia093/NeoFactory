package com.philia093.neofactory.material;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.chemistry.Blend;
import com.philia093.neofactory.chemistry.Chemical;
import com.philia093.neofactory.chemistry.Composition;
import com.philia093.neofactory.chemistry.Fraction;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Declaration of every material of the game, and the one place its items are registered.
 * <p>
 * <b>Where the items come from.</b> A material declares what it is - a colour, a formula, a kind
 * - and this class turns that into items, one per shape: the id, the name {@code iron_plate}, the
 * picture of the shape and the colour of the material, see {@link #registerItems()}. Nothing is
 * written twice, and a material that arrives later needs a single line below and no art at all,
 * because the picture of a shape is grey scale and the colour is multiplied over it.
 * <p>
 * <b>When it runs.</b> {@link #registerAll()} is called at the end of {@link Items#registerAll()},
 * right before the item table is frozen: the materials append their items behind the ones the
 * game spells out by hand, so none of those ids moves, and every item of a material is a real
 * item of the game from then on - it lies in an inventory, it burns in a furnace, it is found by
 * the search box.
 * <p>
 * <b>An id never moves.</b> A shape may declare the id it is registered with,
 * {@link Material.Builder#item(MaterialForm, int)}, which is how the iron ingot kept the number it
 * was stored with while its definition moved out of {@link Items}: the number belongs to the save
 * format, the definition does not. Every other shape takes the next free id in the order the
 * materials and the shapes are declared, so a material has to be appended at the end of
 * {@link #declare()} and never inserted in the middle.
 */
public final class Materials {

    private static final Logger LOGGER = LogManager.getLogger();

    // ------------------------------------------------------------------
    // The metals. Append new ones at the end, never in the middle.
    // ------------------------------------------------------------------

    /** Iron, the metal every machine is built from. */
    public static Material IRON;

    /** Gold, the soft and precious metal. */
    public static Material GOLD;

    /** Copper, the metal that carries a current. */
    public static Material COPPER;

    /**
     * Colour of copper, shared with the pipes that are made of it.
     * <p>
     * The colour of a material is written down here as a constant as well, because a block needs it while
     * the blocks are registered - the pipes of copper are painted with it, see {@code PipeMaterials} - and
     * the materials themselves are declared later, while the items are registered.
     */
    public static final Color COPPER_COLOR = new Color(0.85f, 0.55f, 0.35f, 1f);

    /** Colour of bronze, shared with the pipes that are made of it. */
    public static final Color BRONZE_COLOR = new Color(0.72f, 0.45f, 0.22f, 1f);

    /** Colour of steel, shared with the pipes that are made of it. */
    public static final Color STEEL_COLOR = new Color(0.58f, 0.60f, 0.64f, 1f);

    /**
     * Colour of iron, shared with the cables that are made of it.
     * <p>
     * A cable is a block of the industry and a block is painted while the blocks are registered, which
     * happens long before the materials are declared. The colours of every metal a cable is made of
     * therefore stand here, next to the colour of copper, and the materials themselves use them as well
     * so that a cable is the colour of its ingot, see {@code CableMaterials}.
     */
    public static final Color IRON_COLOR = new Color(0.86f, 0.86f, 0.88f, 1f);

    /** Colour of gold, shared with the cables that are made of it. */
    public static final Color GOLD_COLOR = new Color(1.00f, 0.85f, 0.30f, 1f);

    /** Colour of tin, shared with the cables that are made of it. */
    public static final Color TIN_COLOR = new Color(0.80f, 0.82f, 0.85f, 1f);

    /** Colour of silver, shared with the cables that are made of it. */
    public static final Color SILVER_COLOR = new Color(0.94f, 0.95f, 0.97f, 1f);

    /** Colour of nickel, shared with the cables that are made of it. */
    public static final Color NICKEL_COLOR = new Color(0.78f, 0.83f, 0.72f, 1f);

    /** Colour of aluminium, shared with the cables that are made of it. */
    public static final Color ALUMINIUM_COLOR = new Color(0.86f, 0.88f, 0.92f, 1f);

    /** Colour of platinum, shared with the cables that are made of it. */
    public static final Color PLATINUM_COLOR = new Color(0.82f, 0.92f, 0.94f, 1f);

    /** Tin, used to solder and to alloy. */
    public static Material TIN;

    /** Lead, heavy and soft. */
    public static Material LEAD;

    /** Silver, the best conductor of the metals. */
    public static Material SILVER;

    /** Nickel, tough and resistant. */
    public static Material NICKEL;

    /** Aluminium, light and common. */
    public static Material ALUMINIUM;

    /** Platinum, rare and noble. */
    public static Material PLATINUM;

    /** Tungsten, the metal that takes the most heat. */
    public static Material TUNGSTEN;

    /**
     * Bronze, the alloy of the bronze age.
     * <p>
     * It is the metal of the first machines of the industry: the boiler that makes the steam is built of
     * bronze, the pipes that carry it are, and the machines of the age that follow are. It is declared
     * behind every other metal because the ids of the items a material brings must never move, see the
     * class comment.
     */
    public static Material BRONZE;

    /**
     * Steel, the metal of the age behind bronze.
     * <p>
     * Steel takes more heat than bronze and more pressure, which is what the steam of a larger boiler and
     * the machines of a workshop ask for.
     */
    public static Material STEEL;

    // ------------------------------------------------------------------
    // The colours of the materials that carry a pipe of their own, see PipeMaterials: a block is painted
    // while the blocks are registered and the materials themselves are declared later, while the items are.
    // ------------------------------------------------------------------

    /** Colour of wood, the one material of the pipes that is not a metal. */
    public static final Color WOOD_COLOR = new Color(0.62f, 0.45f, 0.26f, 1f);

    /** Colour of clay, which is dug, shaped and baked. */
    public static final Color CLAY_COLOR = new Color(0.66f, 0.62f, 0.57f, 1f);

    /** Colour of wrought iron, the soft iron of a bloomery. */
    public static final Color WROUGHT_IRON_COLOR = new Color(0.80f, 0.78f, 0.75f, 1f);

    /** Colour of lead, the heavy metal that flows in a line that takes little heat. */
    public static final Color LEAD_COLOR = new Color(0.50f, 0.52f, 0.62f, 1f);

    /** Colour of tungsten, the metal with the highest melting point of them all. */
    public static final Color TUNGSTEN_COLOR = new Color(0.44f, 0.44f, 0.48f, 1f);

    /** Colour of polyethylene, the plastic every first line is drawn from. */
    public static final Color POLYETHYLENE_COLOR = new Color(0.90f, 0.90f, 0.87f, 1f);

    /** Colour of stainless steel, the steel that does not rust. */
    public static final Color STAINLESS_STEEL_COLOR = new Color(0.73f, 0.76f, 0.79f, 1f);

    /** Colour of titanium, the light and strong metal. */
    public static final Color TITANIUM_COLOR = new Color(0.78f, 0.78f, 0.81f, 1f);

    /** Colour of polytetrafluoroethylene, the plastic that nothing sticks to. */
    public static final Color PTFE_COLOR = new Color(0.92f, 0.93f, 0.94f, 1f);

    /** Colour of tungsten steel, the steel of a tool that keeps its edge white hot. */
    public static final Color TUNGSTEN_STEEL_COLOR = new Color(0.37f, 0.37f, 0.43f, 1f);

    /** Colour of polybenzimidazole, the amber plastic that takes the heat of a flame. */
    public static final Color PBI_COLOR = new Color(0.85f, 0.79f, 0.52f, 1f);

    /** Colour of niobium titanium, the alloy of a superconducting wire. */
    public static final Color NIOBIUM_TITANIUM_COLOR = new Color(0.62f, 0.62f, 0.68f, 1f);

    /** Colour of the tantalum tungsten alloy of grade sixty. */
    public static final Color TANTALUM_TUNGSTEN_60_COLOR = new Color(0.55f, 0.52f, 0.50f, 1f);

    /** Colour of the tantalum tungsten alloy of grade sixty one. */
    public static final Color TANTALUM_TUNGSTEN_61_COLOR = new Color(0.58f, 0.55f, 0.52f, 1f);

    /** Colour of europium, the rare earth of a screen that glows. */
    public static final Color EUROPIUM_COLOR = new Color(0.90f, 0.88f, 0.79f, 1f);

    /** Colour of depleted uranium, the heavy metal of a shield. */
    public static final Color DEPLETED_URANIUM_COLOR = new Color(0.46f, 0.50f, 0.43f, 1f);

    /** Colour of maraging steel of grade three hundred. */
    public static final Color MARAGING_STEEL_300_COLOR = new Color(0.63f, 0.65f, 0.71f, 1f);

    /** Colour of the nickel chromium alloy of grade six ninety. */
    public static final Color INCONEL_690_COLOR = new Color(0.70f, 0.72f, 0.68f, 1f);

    /** Colour of the nickel chromium alloy of grade seven ninety two. */
    public static final Color INCONEL_792_COLOR = new Color(0.72f, 0.74f, 0.70f, 1f);

    /** Colour of maraging steel of grade three hundred and fifty. */
    public static final Color MARAGING_STEEL_350_COLOR = new Color(0.65f, 0.67f, 0.73f, 1f);

    /** Colour of the superalloy known as Hastelloy X. */
    public static final Color HASTELLOY_X_COLOR = new Color(0.67f, 0.69f, 0.65f, 1f);

    /** Colour of the heat resistant chromium iron alloy nine hundred and three. */
    public static final Color INCOLOY_903_COLOR = new Color(0.69f, 0.71f, 0.67f, 1f);

    // ------------------------------------------------------------------
    // The colours of the materials that carry a cable of their own, see CableMaterials. A cable is a block
    // of the industry like a pipe and the same rule holds: the blocks are painted while they are
    // registered, and the materials themselves are declared long after that. A metal that already has its
    // colour written down above keeps it - iron, gold, tin, silver, nickel, aluminium, platinum, lead,
    // copper, steel, titanium, tungsten, tungsten steel and niobium titanium - so a cable of silver is the
    // colour of a silver ingot and no second colour has to be kept in step with the first.
    // ------------------------------------------------------------------

    /** Colour of the red alloy, the first line of a workshop. */
    public static final Color RED_ALLOY_COLOR = new Color(0.75f, 0.32f, 0.28f, 1f);

    /** Colour of the redstone alloy, the cheap line of the age of steam. */
    public static final Color REDSTONE_ALLOY_COLOR = new Color(0.62f, 0.20f, 0.20f, 1f);

    /** Colour of cobalt, the blue grey metal of a line that takes heat. */
    public static final Color COBALT_COLOR = new Color(0.40f, 0.45f, 0.62f, 1f);

    /** Colour of zinc, the pale metal that protects a line. */
    public static final Color ZINC_COLOR = new Color(0.62f, 0.70f, 0.76f, 1f);

    /** Colour of solder, the dull alloy of a join. */
    public static final Color SOLDER_COLOR = new Color(0.72f, 0.72f, 0.70f, 1f);

    /** Colour of cupronickel, the silvery alloy of a wide line. */
    public static final Color CUPRONICKEL_COLOR = new Color(0.72f, 0.74f, 0.78f, 1f);

    /** Colour of annealed copper, the bright copper that lost its hardness. */
    public static final Color ANNEALED_COPPER_COLOR = new Color(0.90f, 0.58f, 0.36f, 1f);

    /** Colour of magnetic steel, the dark steel a line is wound around. */
    public static final Color MAGNETIC_STEEL_COLOR = new Color(0.45f, 0.47f, 0.52f, 1f);

    /** Colour of kanthal, the warm grey alloy of a heating line. */
    public static final Color KANTHAL_COLOR = new Color(0.52f, 0.50f, 0.48f, 1f);

    /** Colour of electrum, the pale gold of an alloy of gold and silver. */
    public static final Color ELECTRUM_COLOR = new Color(0.92f, 0.84f, 0.45f, 1f);

    /** Colour of nichrome, the grey alloy of a heating element. */
    public static final Color NICHROME_COLOR = new Color(0.58f, 0.58f, 0.60f, 1f);

    /** Colour of black steel, the darkest metal of the table. */
    public static final Color BLACK_STEEL_COLOR = new Color(0.22f, 0.23f, 0.26f, 1f);

    /** Colour of graphene, the black sheet a cheap line is cut from. */
    public static final Color GRAPHENE_COLOR = new Color(0.16f, 0.17f, 0.19f, 1f);

    /** Colour of osmium, the densest metal of a line. */
    public static final Color OSMIUM_COLOR = new Color(0.68f, 0.72f, 0.80f, 1f);

    /** Colour of the high speed steel of grade G. */
    public static final Color HSS_G_COLOR = new Color(0.42f, 0.44f, 0.48f, 1f);

    /** Colour of the high speed steel of grade E. */
    public static final Color HSS_E_COLOR = new Color(0.38f, 0.40f, 0.45f, 1f);

    /** Colour of the high speed steel of grade S, the last steel of the table. */
    public static final Color HSS_S_COLOR = new Color(0.34f, 0.36f, 0.42f, 1f);

    /** Colour of vanadium gallium, the compound a superconducting line is wound from. */
    public static final Color VANADIUM_GALLIUM_COLOR = new Color(0.55f, 0.58f, 0.66f, 1f);

    /** Colour of the ceramic of yttrium, barium and copper, the oxide of a warm superconductor. */
    public static final Color YTTRIUM_BARIUM_COPPER_OXIDE_COLOR = new Color(0.30f, 0.32f, 0.36f, 1f);

    /** Colour of osmiridium, the white alloy of osmium and iridium. */
    public static final Color OSMIRIDIUM_COLOR = new Color(0.74f, 0.78f, 0.84f, 1f);

    // The superconductors, from the middle voltage up. Every one of them is a colder blue than the metal
    // line of its age, and the ones of the last ages turn pale, because a line of that size is drawn on the
    // panel of a machine the way the picture of a superconductor reads: a bright wire that loses nothing.

    /** Colour of the superconductor of the middle voltage, the first one the industry draws. */
    public static final Color MV_SUPERCONDUCTOR_COLOR = new Color(0.55f, 0.75f, 0.80f, 1f);

    /** Colour of the superconductor of the high voltage. */
    public static final Color HV_SUPERCONDUCTOR_COLOR = new Color(0.50f, 0.72f, 0.85f, 1f);

    /** Colour of the superconductor of the extreme voltage. */
    public static final Color EV_SUPERCONDUCTOR_COLOR = new Color(0.45f, 0.70f, 0.90f, 1f);

    /** Colour of the superconductor of the fine age. */
    public static final Color IV_SUPERCONDUCTOR_COLOR = new Color(0.60f, 0.65f, 0.92f, 1f);

    /** Colour of the superconductor of the large age. */
    public static final Color LUV_SUPERCONDUCTOR_COLOR = new Color(0.62f, 0.80f, 0.70f, 1f);

    /** Colour of the superconductor of the zero point module. */
    public static final Color ZPM_SUPERCONDUCTOR_COLOR = new Color(0.75f, 0.72f, 0.95f, 1f);

    /** Colour of the superconductor of the ultimate voltage. */
    public static final Color UV_SUPERCONDUCTOR_COLOR = new Color(0.80f, 0.60f, 0.95f, 1f);

    /** Colour of the superconductor of the ultra high voltage. */
    public static final Color UHV_SUPERCONDUCTOR_COLOR = new Color(0.90f, 0.65f, 0.85f, 1f);

    /** Colour of the superconductor of the ultra excessive voltage. */
    public static final Color UEV_SUPERCONDUCTOR_COLOR = new Color(0.95f, 0.80f, 0.70f, 1f);

    /** Colour of the superconductor of the ultra immense voltage. */
    public static final Color UIV_SUPERCONDUCTOR_COLOR = new Color(0.95f, 0.90f, 0.65f, 1f);

    /** Colour of the superconductor of the ultra massive voltage, the palest of them all. */
    public static final Color UMV_SUPERCONDUCTOR_COLOR = new Color(0.95f, 0.95f, 0.90f, 1f);

    private static final List<Material> DECLARED = new ArrayList<>();
    private static boolean registered;

    private Materials() {
        // Utility class: never instantiated.
    }

    /**
     * Declares every material and registers its items.
     * <p>
     * Called once during startup, see {@link Items#registerAll()}. The second call does nothing,
     * which keeps the tests free to ask for a world without watching the order of the setup.
     */
    public static void registerAll() {
        if (registered) {
            return;
        }
        declare();
        registerItems();
        MaterialRegistry.freeze();
        registered = true;
    }

    /** Every material of the game, in the order it was declared in. */
    public static List<Material> all() {
        return MaterialRegistry.all();
    }

    /** Builds the materials and writes them into the registry. */
    private static void declare() {
        IRON = register(Material.builder("iron", "Iron").color(IRON_COLOR)
                .formula("Fe").kind(MaterialKind.METAL)
                // The ingot keeps the id it had in Items, see the class comment.
                .item(MaterialForm.INGOT, Items.IRON_INGOT_ID).build());
        GOLD = register(Material.builder("gold", "Gold").color(GOLD_COLOR)
                .formula("Au").kind(MaterialKind.METAL)
                .item(MaterialForm.INGOT, Items.GOLD_INGOT_ID).build());
        COPPER = register(Material.builder("copper", "Copper")
                .color(COPPER_COLOR).formula("Cu").build());
        TIN = register(Material.builder("tin", "Tin").color(TIN_COLOR)
                .formula("Sn").build());
        LEAD = register(Material.builder("lead", "Lead").color(LEAD_COLOR)
                .formula("Pb").build());
        SILVER = register(Material.builder("silver", "Silver")
                .color(SILVER_COLOR).formula("Ag").build());
        NICKEL = register(Material.builder("nickel", "Nickel")
                .color(NICKEL_COLOR).formula("Ni").build());
        ALUMINIUM = register(Material.builder("aluminium", "Aluminium")
                .color(ALUMINIUM_COLOR).formula("Al").build());
        PLATINUM = register(Material.builder("platinum", "Platinum")
                .color(PLATINUM_COLOR).formula("Pt").build());
        TUNGSTEN = register(Material.builder("tungsten", "Tungsten")
                .color(TUNGSTEN_COLOR).formula("W").build());
        // The two metals of the bronze age. They stand at the end of the list on purpose: a material
        // declares its items in the order it is written down in, so a metal that is added later has to be
        // appended here and never inserted between two others, see the class comment.
        BRONZE = register(Material.builder("bronze", "Bronze")
                .color(BRONZE_COLOR).formula("Cu3Sn").kind(MaterialKind.METAL).build());
        STEEL = register(Material.builder("steel", "Steel")
                .color(STEEL_COLOR).formula("FeC").kind(MaterialKind.METAL).build());

        // ------------------------------------------------------------------
        // The materials of the lines of the industry, see PipeMaterials. They are written after the twelve
        // above so that no id of an item that a world already holds moves, and they are reached by their name
        // instead of by a field, because nothing but a pipe needs them, see MaterialRegistry#byName.
        //
        // A formula is written down where the material really has one: a metal that is one element, a salt,
        // a polymer. An alloy whose name does not spell out what is in it - the steels, the superalloys, wood
        // and clay - is left without one, so nothing here is a guess.
        // ------------------------------------------------------------------

        /** Wood, which carries the water of a river and is no airtight line at all. */
        register(Material.builder("wood", "Wood").color(WOOD_COLOR).formula("")
                .onlyForms(MaterialForm.PLATE).build());

        /** Clay, which is dug, shaped and baked before it carries anything. */
        register(Material.builder("clay", "Clay").color(CLAY_COLOR).formula("")
                .onlyForms(MaterialForm.DUST, MaterialForm.PLATE).build());

        register(Material.builder("wrought_iron", "Wrought Iron").color(WROUGHT_IRON_COLOR)
                .formula("Fe").build());
        register(Material.builder("polyethylene", "Polyethylene").color(POLYETHYLENE_COLOR)
                .formula("(C2H4)n").build());
        register(Material.builder("stainless_steel", "Stainless Steel").color(STAINLESS_STEEL_COLOR)
                .formula("").build());
        register(Material.builder("titanium", "Titanium").color(TITANIUM_COLOR)
                .formula("Ti").build());
        register(Material.builder("ptfe", "Polytetrafluoroethylene").color(PTFE_COLOR)
                .formula("(C2F4)n").build());
        register(Material.builder("tungsten_steel", "Tungsten Steel").color(TUNGSTEN_STEEL_COLOR)
                .formula("").build());
        register(Material.builder("pbi", "Polybenzimidazole").color(PBI_COLOR)
                .formula("C20H12N4").build());
        register(Material.builder("niobium_titanium", "Niobium Titanium")
                .color(NIOBIUM_TITANIUM_COLOR).formula("NbTi").build());
        register(Material.builder("tantalum_tungsten_60", "Tantalum Tungsten 60")
                .color(TANTALUM_TUNGSTEN_60_COLOR).formula("TaW").build());
        register(Material.builder("tantalum_tungsten_61", "Tantalum Tungsten 61")
                .color(TANTALUM_TUNGSTEN_61_COLOR).formula("TaW").build());
        register(Material.builder("europium", "Europium").color(EUROPIUM_COLOR)
                .formula("Eu").build());
        register(Material.builder("depleted_uranium", "Depleted Uranium")
                .color(DEPLETED_URANIUM_COLOR).formula("U").build());
        register(Material.builder("maraging_steel_300", "Maraging Steel 300")
                .color(MARAGING_STEEL_300_COLOR).formula("").build());
        register(Material.builder("inconel_690", "Inconel 690").color(INCONEL_690_COLOR)
                .formula("").build());
        register(Material.builder("inconel_792", "Inconel 792").color(INCONEL_792_COLOR)
                .formula("").build());
        register(Material.builder("maraging_steel_350", "Maraging Steel 350")
                .color(MARAGING_STEEL_350_COLOR).formula("").build());
        register(Material.builder("hastelloy_x", "Hastelloy X").color(HASTELLOY_X_COLOR)
                .formula("").build());
        register(Material.builder("incoloy_903", "Incoloy 903").color(INCOLOY_903_COLOR)
                .formula("").build());

        // ------------------------------------------------------------------
        // The materials of the cable line, see CableMaterials. They are written after the lines of the
        // pipes for the same reason the pipes were written after the twelve metals: a material declares its
        // items in the order it stands in, so a material that arrives later is appended here and never
        // inserted between two others, see the class comment.
        //
        // A metal of the cable line comes in the shapes of a metal, and the materials that are no plain
        // metal bring their own list: graphene is a sheet of carbon and not an alloy, so it is cut, rolled
        // and drawn but never cast into a gear, and a superconductor comes as the powder it is mixed from,
        // the blank it is cast into, the bar it is drawn out of and the wire it ends up as. A formula is
        // written down where the material really has one and the name of it says so - an element, a well
        // known alloy, a compound - and an alloy whose name hides its composition is left without one, so
        // nothing here is a guess.
        // ------------------------------------------------------------------

        /** The red alloy, the cheapest line of a workshop, made of copper and redstone. */
        register(Material.builder("red_alloy", "Red Alloy").color(RED_ALLOY_COLOR)
                .formula("").kind(MaterialKind.METAL).build());

        /** The redstone alloy, the cheap line of the age of steam. */
        register(Material.builder("redstone_alloy", "Redstone Alloy").color(REDSTONE_ALLOY_COLOR)
                .formula("").kind(MaterialKind.METAL).build());

        /** Cobalt, the tough metal of a line that takes heat. */
        register(Material.builder("cobalt", "Cobalt").color(COBALT_COLOR)
                .formula("Co").kind(MaterialKind.METAL).build());

        /** Zinc, the metal that protects what it is drawn around. */
        register(Material.builder("zinc", "Zinc").color(ZINC_COLOR)
                .formula("Zn").kind(MaterialKind.METAL).build());

        /** Solder, the soft alloy of tin and lead that holds a join together. */
        register(Material.builder("solder", "Solder").color(SOLDER_COLOR)
                .formula("SnPb").kind(MaterialKind.METAL).build());

        /** Cupronickel, the alloy of copper and nickel that carries a wide line. */
        register(Material.builder("cupronickel", "Cupronickel").color(CUPRONICKEL_COLOR)
                .formula("CuNi").kind(MaterialKind.METAL).build());

        /** Annealed copper, the same metal softened, which loses less over the same length. */
        register(Material.builder("annealed_copper", "Annealed Copper").color(ANNEALED_COPPER_COLOR)
                .formula("Cu").kind(MaterialKind.METAL).build());

        /** Magnetic steel, the steel a line of the age is wound around. */
        register(Material.builder("magnetic_steel", "Magnetic Steel").color(MAGNETIC_STEEL_COLOR)
                .formula("").kind(MaterialKind.METAL).build());

        /** Kanthal, the iron chromium aluminium alloy of a heating line. */
        register(Material.builder("kanthal", "Kanthal").color(KANTHAL_COLOR)
                .formula("FeCrAl").kind(MaterialKind.METAL).build());

        /** Electrum, the alloy of gold and silver. */
        register(Material.builder("electrum", "Electrum").color(ELECTRUM_COLOR)
                .formula("AgAu").kind(MaterialKind.METAL).build());

        /** Nichrome, the alloy of nickel and chromium of a heating element. */
        register(Material.builder("nichrome", "Nichrome").color(NICHROME_COLOR)
                .formula("NiCr").kind(MaterialKind.METAL).build());

        /** Black steel, the dark steel of a line that carries four amperes. */
        register(Material.builder("black_steel", "Black Steel").color(BLACK_STEEL_COLOR)
                .formula("").kind(MaterialKind.METAL).build());

        /** Graphene, the sheet of carbon a cheap line of the fine age is cut from. */
        register(Material.builder("graphene", "Graphene").color(GRAPHENE_COLOR)
                .formula("C").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.PLATE,
                        MaterialForm.FOIL, MaterialForm.ROD, MaterialForm.FINE_WIRE)
                .build());

        /** Osmium, the densest metal of the table. */
        register(Material.builder("osmium", "Osmium").color(OSMIUM_COLOR)
                .formula("Os").kind(MaterialKind.METAL).build());

        /** High speed steel of grade G, the steel of a tool that keeps its edge white hot. */
        register(Material.builder("hss_g", "HSS-G").color(HSS_G_COLOR)
                .formula("").kind(MaterialKind.METAL).build());

        /** High speed steel of grade E, the steel of the last metal line of the large age. */
        register(Material.builder("hss_e", "HSS-E").color(HSS_E_COLOR)
                .formula("").kind(MaterialKind.METAL).build());

        /** High speed steel of grade S, the steel of the last line of the table that is no superconductor. */
        register(Material.builder("hss_s", "HSS-S").color(HSS_S_COLOR)
                .formula("").kind(MaterialKind.METAL).build());

        /** Vanadium gallium, the intermetallic compound a superconducting wire is wound from. */
        register(Material.builder("vanadium_gallium", "Vanadium Gallium")
                .color(VANADIUM_GALLIUM_COLOR).formula("V3Ga").kind(MaterialKind.METAL).build());

        /** The ceramic superconductor of yttrium, barium and copper. */
        register(Material.builder("yttrium_barium_copper_oxide", "Yttrium Barium Copper Oxide")
                .color(YTTRIUM_BARIUM_COPPER_OXIDE_COLOR).formula("YBa2Cu3O7")
                .onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.ROD,
                        MaterialForm.FINE_WIRE)
                .build());

        /** Osmiridium, the white alloy of osmium and iridium: the highest current at the lowest loss. */
        register(Material.builder("osmiridium", "Osmiridium").color(OSMIRIDIUM_COLOR)
                .formula("OsIr").kind(MaterialKind.METAL).build());

        // The superconductors, from the middle voltage up, see CableMaterials. Every one of them is the
        // wire of its own age and the blank that wire is drawn out of: the bar a player draws and the fine
        // wire that comes out of it are two shapes of the same material, and the cable of the table is the
        // wire in the world.

        /** The superconductor of the middle voltage, the first line that loses nothing. */
        register(Material.builder("mv_superconductor", "MV Superconductor")
                .color(MV_SUPERCONDUCTOR_COLOR).formula("")
                .onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.ROD,
                        MaterialForm.FINE_WIRE)
                .build());

        /** The superconductor of the high voltage. */
        register(Material.builder("hv_superconductor", "HV Superconductor")
                .color(HV_SUPERCONDUCTOR_COLOR).formula("")
                .onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.ROD,
                        MaterialForm.FINE_WIRE)
                .build());

        /** The superconductor of the extreme voltage. */
        register(Material.builder("ev_superconductor", "EV Superconductor")
                .color(EV_SUPERCONDUCTOR_COLOR).formula("")
                .onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.ROD,
                        MaterialForm.FINE_WIRE)
                .build());

        /** The superconductor of the fine age. */
        register(Material.builder("iv_superconductor", "IV Superconductor")
                .color(IV_SUPERCONDUCTOR_COLOR).formula("")
                .onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.ROD,
                        MaterialForm.FINE_WIRE)
                .build());

        /** The superconductor of the large age. */
        register(Material.builder("luv_superconductor", "LuV Superconductor")
                .color(LUV_SUPERCONDUCTOR_COLOR).formula("")
                .onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.ROD,
                        MaterialForm.FINE_WIRE)
                .build());

        /** The superconductor of the zero point module. */
        register(Material.builder("zpm_superconductor", "ZPM Superconductor")
                .color(ZPM_SUPERCONDUCTOR_COLOR).formula("")
                .onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.ROD,
                        MaterialForm.FINE_WIRE)
                .build());

        /** The superconductor of the ultimate voltage. */
        register(Material.builder("uv_superconductor", "UV Superconductor")
                .color(UV_SUPERCONDUCTOR_COLOR).formula("")
                .onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.ROD,
                        MaterialForm.FINE_WIRE)
                .build());

        /** The superconductor of the ultra high voltage. */
        register(Material.builder("uhv_superconductor", "UHV Superconductor")
                .color(UHV_SUPERCONDUCTOR_COLOR).formula("")
                .onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.ROD,
                        MaterialForm.FINE_WIRE)
                .build());

        /** The superconductor of the ultra excessive voltage. */
        register(Material.builder("uev_superconductor", "UEV Superconductor")
                .color(UEV_SUPERCONDUCTOR_COLOR).formula("")
                .onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.ROD,
                        MaterialForm.FINE_WIRE)
                .build());

        /** The superconductor of the ultra immense voltage. */
        register(Material.builder("uiv_superconductor", "UIV Superconductor")
                .color(UIV_SUPERCONDUCTOR_COLOR).formula("")
                .onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.ROD,
                        MaterialForm.FINE_WIRE)
                .build());

        /** The superconductor of the ultra massive voltage, the widest line of the table. */
        register(Material.builder("umv_superconductor", "UMV Superconductor")
                .color(UMV_SUPERCONDUCTOR_COLOR).formula("")
                .onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.ROD,
                        MaterialForm.FINE_WIRE)
                .build());

        // The metals of the periodic table the game did not have, each as the material of the element it is:
        // a dust, an ingot, a nugget and a plate, the formula of the element under the name, and the SMILES
        // the catalog knows the same substance by. A material is a colour and a list of shapes and owns no
        // art, see {@link Material}, so a metal of the table costs a few lines here and no picture at all -
        // and the shapes it does not name are the ones a metal of the trade is not worked into.
        register(Material.builder("lithium", "Lithium").color(new Color(0.85f, 0.87f, 0.90f, 1f))
                .formula("Li").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("beryllium", "Beryllium").color(new Color(0.88f, 0.90f, 0.88f, 1f))
                .formula("Be").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("sodium", "Sodium").color(new Color(0.90f, 0.90f, 0.92f, 1f))
                .formula("Na").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("magnesium", "Magnesium").color(new Color(0.86f, 0.87f, 0.88f, 1f))
                .formula("Mg").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("potassium", "Potassium").color(new Color(0.88f, 0.88f, 0.90f, 1f))
                .formula("K").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("calcium", "Calcium").color(new Color(0.90f, 0.90f, 0.88f, 1f))
                .formula("Ca").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("scandium", "Scandium").color(new Color(0.90f, 0.90f, 0.92f, 1f))
                .formula("Sc").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        // Titanium already stands above with the shapes a metal of the shop comes in.
        register(Material.builder("vanadium", "Vanadium").color(new Color(0.78f, 0.80f, 0.82f, 1f))
                .formula("V").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("chromium", "Chromium").color(new Color(0.72f, 0.75f, 0.78f, 1f))
                .formula("Cr").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("manganese", "Manganese").color(new Color(0.80f, 0.78f, 0.76f, 1f))
                .formula("Mn").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        // Cobalt already stands above with the shapes a metal of the shop comes in.
        register(Material.builder("gallium", "Gallium").color(new Color(0.80f, 0.82f, 0.88f, 1f))
                .formula("Ga").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("germanium", "Germanium").color(new Color(0.78f, 0.80f, 0.82f, 1f))
                .formula("Ge").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("rubidium", "Rubidium").color(new Color(0.86f, 0.84f, 0.86f, 1f))
                .formula("Rb").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("zirconium", "Zirconium").color(new Color(0.80f, 0.82f, 0.85f, 1f))
                .formula("Zr").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("strontium", "Strontium").color(new Color(0.88f, 0.88f, 0.86f, 1f))
                .formula("Sr").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("yttrium", "Yttrium").color(new Color(0.82f, 0.84f, 0.86f, 1f))
                .formula("Y").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("niobium", "Niobium").color(new Color(0.76f, 0.78f, 0.82f, 1f))
                .formula("Nb").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("molybdenum", "Molybdenum").color(new Color(0.70f, 0.72f, 0.76f, 1f))
                .formula("Mo").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("technetium", "Technetium").color(new Color(0.72f, 0.74f, 0.78f, 1f))
                .formula("Tc").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("ruthenium", "Ruthenium").color(new Color(0.66f, 0.68f, 0.72f, 1f))
                .formula("Ru").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("rhodium", "Rhodium").color(new Color(0.75f, 0.78f, 0.82f, 1f))
                .formula("Rh").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("palladium", "Palladium").color(new Color(0.80f, 0.82f, 0.86f, 1f))
                .formula("Pd").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("cadmium", "Cadmium").color(new Color(0.76f, 0.78f, 0.80f, 1f))
                .formula("Cd").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("indium", "Indium").color(new Color(0.82f, 0.84f, 0.88f, 1f))
                .formula("In").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("antimony", "Antimony").color(new Color(0.80f, 0.78f, 0.80f, 1f))
                .formula("Sb").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("caesium", "Caesium").color(new Color(0.85f, 0.82f, 0.80f, 1f))
                .formula("Cs").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("barium", "Barium").color(new Color(0.88f, 0.90f, 0.88f, 1f))
                .formula("Ba").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("hafnium", "Hafnium").color(new Color(0.74f, 0.76f, 0.80f, 1f))
                .formula("Hf").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("tantalum", "Tantalum").color(new Color(0.72f, 0.74f, 0.78f, 1f))
                .formula("Ta").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("iridium", "Iridium").color(new Color(0.72f, 0.75f, 0.78f, 1f))
                .formula("Ir").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("rhenium", "Rhenium").color(new Color(0.72f, 0.74f, 0.78f, 1f))
                .formula("Re").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        // Osmium already stands above with the shapes a metal of the shop comes in.
        register(Material.builder("mercury", "Mercury").color(new Color(0.85f, 0.87f, 0.90f, 1f))
                .formula("Hg").onlyForms(MaterialForm.DUST, MaterialForm.NUGGET,
                        MaterialForm.FLUID_CELL).build());
        register(Material.builder("thallium", "Thallium").color(new Color(0.78f, 0.80f, 0.82f, 1f))
                .formula("Tl").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("bismuth", "Bismuth").color(new Color(0.82f, 0.78f, 0.86f, 1f))
                .formula("Bi").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("polonium", "Polonium").color(new Color(0.84f, 0.82f, 0.80f, 1f))
                .formula("Po").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("francium", "Francium").color(new Color(0.86f, 0.84f, 0.82f, 1f))
                .formula("Fr").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        register(Material.builder("radium", "Radium").color(new Color(0.88f, 0.90f, 0.84f, 1f))
                .formula("Ra").onlyForms(MaterialForm.DUST, MaterialForm.INGOT, MaterialForm.NUGGET,
                        MaterialForm.PLATE).build());
        // The non-metals of the table, which are ground and never cast: a stone of boron or of iodine is a
        // dust of it, and the smallest piles of that dust are what a recipe of the trade asks for.
        register(Material.builder("boron", "Boron").color(new Color(0.70f, 0.68f, 0.62f, 1f))
                .formula("B").onlyForms(MaterialForm.DUST, MaterialForm.SMALL_DUST,
                        MaterialForm.TINY_DUST).build());
        register(Material.builder("arsenic", "Arsenic").color(new Color(0.72f, 0.74f, 0.70f, 1f))
                .formula("As").onlyForms(MaterialForm.DUST, MaterialForm.SMALL_DUST,
                        MaterialForm.TINY_DUST).build());
        register(Material.builder("selenium", "Selenium").color(new Color(0.70f, 0.72f, 0.68f, 1f))
                .formula("Se").onlyForms(MaterialForm.DUST, MaterialForm.SMALL_DUST,
                        MaterialForm.TINY_DUST).build());
        register(Material.builder("tellurium", "Tellurium").color(new Color(0.76f, 0.76f, 0.72f, 1f))
                .formula("Te").onlyForms(MaterialForm.DUST, MaterialForm.SMALL_DUST,
                        MaterialForm.TINY_DUST).build());
        register(Material.builder("iodine", "Iodine").color(new Color(0.62f, 0.52f, 0.72f, 1f))
                .formula("I").onlyForms(MaterialForm.DUST, MaterialForm.SMALL_DUST,
                        MaterialForm.TINY_DUST).build());

        // The minerals of the ground, which are the stones a player meets: crushed, ground to a dust and, when
        // they are worth it, cut. A mineral is never worked into a machine, which is what its kind says, and
        // the formulas are the real ones of the stone - written without brackets, because a balance of the
        // industry is read in that shape.
        register(Material.builder("calcite", "Calcite").color(new Color(0.94f, 0.94f, 0.90f, 1f))
                .kind(MaterialKind.MINERAL).formula("CaCO3").build());
        register(Material.builder("gypsum", "Gypsum").color(new Color(0.92f, 0.92f, 0.88f, 1f))
                .kind(MaterialKind.MINERAL).formula("CaSO4").build());
        register(Material.builder("talc", "Talc").color(new Color(0.88f, 0.90f, 0.86f, 1f))
                .kind(MaterialKind.MINERAL).formula("Mg3Si4O10").build());
        register(Material.builder("mica", "Mica").color(new Color(0.82f, 0.80f, 0.76f, 1f))
                .kind(MaterialKind.MINERAL).formula("KAl3Si3O10").build());
        register(Material.builder("asbestos", "Asbestos").color(new Color(0.80f, 0.82f, 0.78f, 1f))
                .kind(MaterialKind.MINERAL).formula("Mg3Si2O9H4").build());
        register(Material.builder("borax", "Borax").color(new Color(0.90f, 0.90f, 0.94f, 1f))
                .kind(MaterialKind.MINERAL).formula("Na2B4O7").build());
        register(Material.builder("cryolite", "Cryolite").color(new Color(0.84f, 0.88f, 0.92f, 1f))
                .kind(MaterialKind.MINERAL).formula("Na3AlF6").build());
        register(Material.builder("fluorite", "Fluorite").color(new Color(0.76f, 0.88f, 0.86f, 1f))
                .kind(MaterialKind.MINERAL).formula("CaF2").build());
        register(Material.builder("rock_salt", "Rock Salt").color(new Color(0.92f, 0.90f, 0.88f, 1f))
                .kind(MaterialKind.MINERAL).formula("KCl").build());
        register(Material.builder("salt", "Salt").color(new Color(0.96f, 0.96f, 0.96f, 1f))
                .kind(MaterialKind.MINERAL).formula("NaCl").build());
        register(Material.builder("quartz", "Quartz").color(new Color(0.92f, 0.92f, 0.94f, 1f))
                .kind(MaterialKind.MINERAL).formula("SiO2").build());
        register(Material.builder("sodalite", "Sodalite").color(new Color(0.72f, 0.76f, 0.88f, 1f))
                .kind(MaterialKind.MINERAL).formula("Na4Al3Si3O12Cl").build());
        register(Material.builder("apatite", "Apatite").color(new Color(0.86f, 0.90f, 0.82f, 1f))
                .kind(MaterialKind.MINERAL).formula("Ca5P3O12F").build());

        // The stones cut into jewels: a gem is held and not built with, so it comes as the stone, the crushed
        // ore of it and the dust of that, see the kind of it.
        register(Material.builder("diamond", "Diamond").color(new Color(0.86f, 0.96f, 0.98f, 1f))
                .kind(MaterialKind.GEM).formula("C").build());
        register(Material.builder("amethyst", "Amethyst").color(new Color(0.68f, 0.48f, 0.86f, 1f))
                .kind(MaterialKind.GEM).formula("SiO2").build());
        register(Material.builder("ruby", "Ruby").color(new Color(0.86f, 0.20f, 0.24f, 1f))
                .kind(MaterialKind.GEM).formula("Al2O3").build());
        register(Material.builder("sapphire", "Sapphire").color(new Color(0.24f, 0.36f, 0.86f, 1f))
                .kind(MaterialKind.GEM).formula("Al2O3").build());
        register(Material.builder("spinel", "Spinel").color(new Color(0.78f, 0.34f, 0.44f, 1f))
                .kind(MaterialKind.GEM).formula("MgAl2O4").build());
        register(Material.builder("olivine", "Olivine").color(new Color(0.62f, 0.78f, 0.36f, 1f))
                .kind(MaterialKind.GEM).formula("Mg2SiO4").build());
        register(Material.builder("zircon", "Zircon").color(new Color(0.84f, 0.72f, 0.52f, 1f))
                .kind(MaterialKind.GEM).formula("ZrSiO4").build());
        register(Material.builder("garnet", "Garnet").color(new Color(0.70f, 0.20f, 0.26f, 1f))
                .kind(MaterialKind.GEM).formula("Fe3Al2Si3O12").build());
        register(Material.builder("emerald", "Emerald").color(new Color(0.20f, 0.82f, 0.44f, 1f))
                .kind(MaterialKind.GEM).formula("Be3Al2Si6O18").build());
        register(Material.builder("topaz", "Topaz").color(new Color(0.94f, 0.86f, 0.46f, 1f))
                .kind(MaterialKind.GEM).formula("Al2SiO4F2").build());
        register(Material.builder("malachite", "Malachite").color(new Color(0.22f, 0.72f, 0.48f, 1f))
                .kind(MaterialKind.GEM).formula("Cu2CH2O5").build());
        register(Material.builder("lazurite", "Lazurite").color(new Color(0.24f, 0.34f, 0.80f, 1f))
                .kind(MaterialKind.GEM).formula("Na8Al6Si6O24S2").build());

        // The polymers of a workshop of plastic: drawn out of a vessel as a bar or a long rod and rolled from
        // there, and never cast into an ingot, see the kind of it.
        register(Material.builder("polypropylene", "Polypropylene").color(new Color(0.90f, 0.90f, 0.88f, 1f))
                .kind(MaterialKind.POLYMER).formula("C3H6").build());
        register(Material.builder("polyvinyl_chloride", "Polyvinyl Chloride")
                .color(new Color(0.86f, 0.86f, 0.82f, 1f)).kind(MaterialKind.POLYMER)
                .formula("C2H3Cl").build());
        register(Material.builder("polystyrene", "Polystyrene").color(new Color(0.92f, 0.92f, 0.94f, 1f))
                .kind(MaterialKind.POLYMER).formula("C8H8").build());
        register(Material.builder("nylon", "Nylon").color(new Color(0.92f, 0.90f, 0.86f, 1f))
                .kind(MaterialKind.POLYMER).formula("C6H11NO").build());
        register(Material.builder("kevlar", "Kevlar").color(new Color(0.94f, 0.88f, 0.60f, 1f))
                .kind(MaterialKind.POLYMER).formula("C14H10N2O2").build());
        register(Material.builder("rubber", "Rubber").color(new Color(0.30f, 0.30f, 0.32f, 1f))
                .kind(MaterialKind.POLYMER).formula("C5H8").build());
        register(Material.builder("silicone_rubber", "Silicone Rubber")
                .color(new Color(0.86f, 0.80f, 0.84f, 1f)).kind(MaterialKind.POLYMER)
                .formula("C2H6OSi").build());
        register(Material.builder("epoxy", "Epoxy Resin").color(new Color(0.88f, 0.86f, 0.78f, 1f))
                .kind(MaterialKind.POLYMER).formula("C21H24O4").build());
    }

    /** Writes a material into the registry and hands it back. */
    private static Material register(Material material) {
        MaterialRegistry.register(material);
        DECLARED.add(material);
        return material;
    }

    /**
     * Registers one item per shape of every material.
     * <p>
     * A shape with a declared id keeps it, every other shape takes the next free one, handed out
     * in the order the materials and the shapes are declared. {@link MaterialForm} is an enum, so
     * that order is written down in a file and never depends on the runtime, which is what makes
     * the ids of the materials reproducible from one start to the next - and a save game that
     * spells them out readable.
     */
    private static void registerItems() {
        int next = Items.NEXT_FREE_ID;
        for (Material material : MaterialRegistry.all()) {
            for (MaterialForm form : MaterialForm.values()) {
                if (!material.has(form)) {
                    continue;
                }
                Integer declared = declaredId(material, form);
                int id = declared == null ? next++ : declared;
                Item item = buildItem(material, form, id);
                ItemRegistry.register(item);
                material.put(form, item);
            }
        }
        LOGGER.info("Registered {} items for {} materials, next free id is {}",
                MaterialRegistry.itemCount(), MaterialRegistry.count(), next);
    }

    /**
     * Id a shape declared, checked against the run of new ids.
     *
     * @param material material that owns the shape
     * @param form shape the item stands for
     * @return the declared id, or {@code null} when the shape declared none
     * @throws IllegalStateException when the declared id would collide with a new item
     */
    private static Integer declaredId(Material material, MaterialForm form) {
        Integer declared = material.declaredId(form);
        if (declared != null && declared >= Items.NEXT_FREE_ID) {
            throw new IllegalStateException("The " + form + " of " + material.name()
                    + " declares the id " + declared + ", which is not below the first free id ("
                    + Items.NEXT_FREE_ID + ") and collides with the items registered here");
        }
        return declared;
    }

    /**
     * Builds the item of one shape of a material.
     * <p>
     * The technical name of the item is the name of the material and the shape - {@code
     * "iron_plate"} - and the name for the player is the two of them spelled out, {@code
     * "Iron Plate"}. The colour of the material lands on the item as its tint, which is what
     * multiplies the grey scale picture of the shape, see {@link MaterialForm}.
     *
     * @param material material that owns the shape
     * @param form shape the item stands for
     * @param id id the item is registered with
     * @return the item
     */
    private static Item buildItem(Material material, MaterialForm form, int id) {
        Item.Builder builder = Item.builder(id, material.itemName(form))
                .displayName(material.displayName() + " " + form.displayName())
                .texture(form.texture())
                .tint(material.color())
                .maxStackSize(form.maxStackSize())
                .formula(material.chemicalFormula())
                .chemicals(blendOf(material, form));
        if (form.hasOverlay()) {
            builder.overlayTexture(form.overlayTexture());
        }
        return builder.build();
    }

    /**
     * The pile of substances one piece of a shape of a material is, in millibuckets.
     * <p>
     * The amount of the piece is the amount its shape carries - a hundred millibuckets of a metal for an
     * ingot, a hundred for a dust, ten for a nugget - and it is split over the elements of the material in
     * the ratio its formula names: an ingot of iron is a hundred millibuckets of iron, an ingot of bronze is
     * seventy five of copper and twenty five of tin, and both are exact and not rounded, see {@link Blend}.
     * <b>An alloy is its elements side by side</b>, which is what an alloy is and what keeps bronze from
     * being read as one compound, see {@code Mixture}.
     * <p>
     * A material that names no formula, a shape that carries no measured amount and a formula the plain
     * reading cannot follow all answer with the empty pile: an item a reaction cannot use is one such an
     * item, and the pile of it is empty rather than wrong.
     *
     * @param material the material the shape is made of
     * @param form the shape
     * @return the pile one piece of it is
     */
    private static Blend blendOf(Material material, MaterialForm form) {
        Fraction amount = form.millibuckets();
        if (amount.isZero() || !material.hasChemicalFormula()) {
            return Blend.empty();
        }
        Composition composition;
        try {
            composition = Composition.parse(material.chemicalFormula());
        } catch (IllegalArgumentException notAPlainFormula) {
            return Blend.empty();
        }
        int atoms = 0;
        for (int count : composition.byElement().values()) {
            atoms += count;
        }
        if (atoms == 0) {
            return Blend.empty();
        }
        return Blend.of(elementPile(composition, amount, Fraction.of(atoms)));
    }

    /** One element of a material as a substance of its own, in the share its count is worth. */
    private static java.util.Map<Chemical, Fraction> elementPile(Composition composition,
            Fraction amount, Fraction atoms) {
        java.util.Map<Chemical, Fraction> components = new java.util.LinkedHashMap<>();
        composition.byElement().forEach((element, count) -> components.put(
                Chemical.parse("[" + element + "]"),
                amount.times(Fraction.of(count)).dividedBy(atoms)));
        return components;
    }
}
