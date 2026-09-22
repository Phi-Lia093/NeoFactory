package com.philia093.neofactory.material;

import com.philia093.neofactory.item.Item;

import java.util.Objects;

/**
 * A shape a material comes in, the plate of a rolling mill and the gear of a press.
 * <p>
 * The form owns everything that is the same for every material: the name its item carries, the
 * picture that item is drawn from and how many of them fit into a slot. Writing that down once
 * per shape is what makes a new material a single line instead of one line per item, see
 * {@link Materials}.
 * <p>
 * <b>Grey scale pictures:</b> the picture of a form holds brightness only and never a colour -
 * an ingot is an ingot, no matter what it is made of. The colour lands on the item as its
 * {@link Item#tint()} and is multiplied with the picture while it is drawn, which is why twenty
 * shapes serve every material the game will ever have and no material needs art of its own. The
 * same rule as for a fluid, see {@code Fluids}, and the files come from the same place: they were
 * cut out of the art pack by {@code build/verify/material_forms.ps1}.
 * <p>
 * <b>A form may carry a second layer.</b> {@link #overlayTexture()} is drawn over the shape
 * without the tint of the material, which is how a fine wire keeps its darker core inside the
 * bright metal that was drawn around it: the shape is the metal, the overlay is what the metal
 * covers. Only a form that really needs it names one, the rest answer {@code null} and are drawn
 * as a single picture.
 */
public enum MaterialForm {

    /** Loose dust, the result of grinding anything. */
    DUST("dust", "Dust", Item.DEFAULT_MAX_STACK, "generic_dust", null),

    /** A small pile of dust, a third of a dust in each direction. */
    SMALL_DUST("small_dust", "Small Pile of Dust", Item.DEFAULT_MAX_STACK, "generic_dust_small",
            null),

    /** A tiny pile of dust, the smallest pile a grinder leaves. */
    TINY_DUST("tiny_dust", "Tiny Pile of Dust", Item.DEFAULT_MAX_STACK, "generic_dust_tiny", null),

    /** A cast ingot, the shape a metal leaves the smelter in. */
    INGOT("ingot", "Ingot", Item.DEFAULT_MAX_STACK, "generic_ingot", null),

    /** A nugget, a small piece of metal. */
    NUGGET("nugget", "Nugget", Item.DEFAULT_MAX_STACK, "generic_nugget", null),

    /** A rolled plate. */
    PLATE("plate", "Plate", Item.DEFAULT_MAX_STACK, "generic_plate", null),

    /** A rolled foil, thinner than a plate. */
    FOIL("foil", "Foil", Item.DEFAULT_MAX_STACK, "generic_foil", null),

    /** A short rod. */
    ROD("rod", "Rod", Item.DEFAULT_MAX_STACK, "generic_rod", null),

    /** A long rod. */
    LONG_ROD("long_rod", "Long Rod", Item.DEFAULT_MAX_STACK, "generic_long_rod", null),

    /** A bolt, a rod with a head. */
    BOLT("bolt", "Bolt", Item.DEFAULT_MAX_STACK, "generic_bolt", null),

    /** A screw, a bolt with a thread. */
    SCREW("screw", "Screw", Item.DEFAULT_MAX_STACK, "generic_screw", null),

    /** A ring, a plate with a hole. */
    RING("ring", "Ring", Item.DEFAULT_MAX_STACK, "generic_ring", null),

    /** A round, a small disc of metal. */
    ROUND("round", "Round", Item.DEFAULT_MAX_STACK, "generic_round", null),

    /** A fine wire, the thinnest thing a metal is drawn into. */
    FINE_WIRE("fine_wire", "Fine Wire", Item.DEFAULT_MAX_STACK, "generic_fine_wire",
            "generic_fine_wire_overlay"),

    /** A spring. */
    SPRING("spring", "Spring", Item.DEFAULT_MAX_STACK, "generic_spring", null),

    /** A small spring. */
    SMALL_SPRING("small_spring", "Small Spring", Item.DEFAULT_MAX_STACK, "generic_small_spring",
            null),

    /** A gear, the shape that carries a rotation. */
    GEAR("gear", "Gear", Item.DEFAULT_MAX_STACK, "generic_gear", null),

    /** A small gear. */
    SMALL_GEAR("small_gear", "Small Gear", Item.DEFAULT_MAX_STACK, "generic_small_gear", null),

    /** A rotor, a gear on a shaft. */
    ROTOR("rotor", "Rotor", Item.DEFAULT_MAX_STACK, "generic_rotor", null),

    /**
     * A cell full of the material, the way the industry carries it around.
     * <p>
     * A cell is not drawn from a grey scale shape of its own: the game already has the picture of
     * a cell, and its window is painted in the colour of whatever is inside, see
     * {@code CellIconFactory}. This form names that very picture, so a cell of a material looks
     * exactly like a cell of a fluid - and it is the shape the material travels in, not a stack
     * of it, which is why a single one fills a slot.
     */
    FLUID_CELL("cell", "Cell", Item.SINGLE_ITEM_STACK, "fluid_cell", null);

    private final String formName;
    private final String displayName;
    private final int maxStackSize;
    private final String textureName;
    private final String overlayName;

    MaterialForm(String formName, String displayName, int maxStackSize, String textureName,
            String overlayName) {
        this.formName = Objects.requireNonNull(formName, "formName");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.textureName = Objects.requireNonNull(textureName, "textureName");
        this.overlayName = overlayName;
        this.maxStackSize = maxStackSize;
        if (maxStackSize < 1) {
            throw new IllegalArgumentException("A form stacks at least one item: " + formName);
        }
    }

    /**
     * Name of this form, the tail of the name of its item.
     * <p>
     * A form may not call itself {@code name()}, that one belongs to the enum, so the field is
     * spelled out. It is the lower case word the item of a material carries, see
     * {@link #itemSuffix()}.
     */
    public String formName() {
        return formName;
    }

    /** Name shown to the player, for example inside a tooltip. */
    public String displayName() {
        return displayName;
    }

    /**
     * Tail an item of this form carries in its name.
     *
     * @return an underscore and the name of the form, for example {@code "_dust"}
     */
    public String itemSuffix() {
        return "_" + formName;
    }

    /** Amount of this form that fits into a single inventory slot. */
    public int maxStackSize() {
        return maxStackSize;
    }

    /**
     * Picture this form is drawn from, relative to the asset root.
     *
     * @return a path such as {@code items/generic_ingot}, without extension
     */
    public String texture() {
        return Item.ITEM_FOLDER + textureName;
    }

    /**
     * Picture drawn over the shape in its own colours, or {@code null} for a single layer form.
     *
     * @return a path such as {@code items/generic_fine_wire_overlay}, without extension
     */
    public String overlayTexture() {
        return overlayName == null ? null : Item.ITEM_FOLDER + overlayName;
    }

    /** {@code true} when this form is drawn from two layers, see {@link #overlayTexture()}. */
    public boolean hasOverlay() {
        return overlayName != null;
    }

    @Override
    public String toString() {
        return "MaterialForm(" + formName + ")";
    }
}
