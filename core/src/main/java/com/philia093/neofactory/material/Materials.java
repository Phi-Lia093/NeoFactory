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
                .color(new Color(0.85f, 0.55f, 0.35f, 1f)).formula("Cu").build());
        TIN = register(Material.builder("tin", "Tin").color(new Color(0.80f, 0.82f, 0.85f, 1f))
                .formula("Sn").build());
        LEAD = register(Material.builder("lead", "Lead").color(new Color(0.50f, 0.52f, 0.62f, 1f))
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
                .color(new Color(0.44f, 0.44f, 0.48f, 1f)).formula("W").build());
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
