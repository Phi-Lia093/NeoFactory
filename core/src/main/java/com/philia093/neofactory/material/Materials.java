package com.philia093.neofactory.material;

import com.badlogic.gdx.graphics.Color;
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
        IRON = register(Material.builder("iron", "Iron").color(new Color(0.86f, 0.86f, 0.88f, 1f))
                .formula("Fe").kind(MaterialKind.METAL)
                // The ingot keeps the id it had in Items, see the class comment.
                .item(MaterialForm.INGOT, Items.IRON_INGOT_ID).build());
        GOLD = register(Material.builder("gold", "Gold").color(new Color(1.00f, 0.85f, 0.30f, 1f))
                .formula("Au").kind(MaterialKind.METAL)
                .item(MaterialForm.INGOT, Items.GOLD_INGOT_ID).build());
        COPPER = register(Material.builder("copper", "Copper")
                .color(COPPER_COLOR).formula("Cu").build());
        TIN = register(Material.builder("tin", "Tin").color(new Color(0.80f, 0.82f, 0.85f, 1f))
                .formula("Sn").build());
        LEAD = register(Material.builder("lead", "Lead").color(LEAD_COLOR)
                .formula("Pb").build());
        SILVER = register(Material.builder("silver", "Silver")
                .color(new Color(0.94f, 0.95f, 0.97f, 1f)).formula("Ag").build());
        NICKEL = register(Material.builder("nickel", "Nickel")
                .color(new Color(0.78f, 0.83f, 0.72f, 1f)).formula("Ni").build());
        ALUMINIUM = register(Material.builder("aluminium", "Aluminium")
                .color(new Color(0.86f, 0.88f, 0.92f, 1f)).formula("Al").build());
        PLATINUM = register(Material.builder("platinum", "Platinum")
                .color(new Color(0.82f, 0.92f, 0.94f, 1f)).formula("Pt").build());
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
                .formula(material.chemicalFormula());
        if (form.hasOverlay()) {
            builder.overlayTexture(form.overlayTexture());
        }
        return builder.build();
    }
}
