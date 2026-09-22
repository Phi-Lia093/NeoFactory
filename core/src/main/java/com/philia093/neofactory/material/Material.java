package com.philia093.neofactory.material;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * One material of the game, the iron every machine is built from.
 * <p>
 * A material is what the player means when they say "iron": the thing that has a colour, a
 * chemical formula and a list of shapes. It owns no picture of its own - the shapes carry the
 * pictures and the colour is multiplied over them while they are drawn, see {@link MaterialForm}
 * - so a new material is a handful of values and nothing else, no matter how many of the shapes
 * it comes in.
 * <p>
 * <b>The items belong to the material.</b> They are built and registered by
 * {@link Materials#registerAll()}, which is why a material is written before any of its items
 * exist and why this class hands them out only afterwards, see {@link #form(MaterialForm)}. A
 * material that does not come in a shape simply leaves it out, and {@link #form(MaterialForm)}
 * answers {@code null} for it.
 * <p>
 * <b>An id never moves.</b> The id of an item is permanent - a stored inventory keeps it - so a
 * material may claim the id an item already had, see {@link Builder#item(MaterialForm, int)},
 * which is how the iron ingot kept the number it was saved with while its definition moved here.
 */
public final class Material {

    private final String name;
    private final String displayName;
    private final Color color;
    private final String chemicalFormula;
    private final MaterialKind kind;
    private final Set<MaterialForm> forms;
    private final Map<MaterialForm, Integer> declaredIds;

    /** Items of this material, filled by the registry while the game starts. */
    private final Map<MaterialForm, Item> items = new EnumMap<>(MaterialForm.class);

    private Material(Builder builder) {
        this.name = builder.name;
        this.displayName = builder.displayName;
        this.color = new Color(builder.color);
        this.chemicalFormula = builder.chemicalFormula;
        this.kind = builder.kind;
        this.forms = Collections.unmodifiableSet(builder.forms());
        this.declaredIds = Collections.unmodifiableMap(builder.declaredIds());
    }

    /** Technical name of this material, for example {@code "iron"}. */
    public String name() {
        return name;
    }

    /** Name shown to the player, for example {@code "Iron"}. */
    public String displayName() {
        return displayName;
    }

    /** Colour every shape of this material is painted in. */
    public Color color() {
        return color;
    }

    /**
     * Chemical formula of this material, the second line of its tooltip.
     *
     * @return a formula such as {@code "Fe"}, or an empty string for a material that has none
     */
    public String chemicalFormula() {
        return chemicalFormula;
    }

    /** {@code true} when this material carries a formula worth showing. */
    public boolean hasChemicalFormula() {
        return !chemicalFormula.isEmpty();
    }

    /** Kind of material, which is what its default shapes come from. */
    public MaterialKind kind() {
        return kind;
    }

    /** Every shape this material comes in, in the order of {@link MaterialForm}. */
    public Set<MaterialForm> forms() {
        return forms;
    }

    /**
     * {@code true} when this material comes in that shape.
     *
     * @param form shape to test, may be {@code null}
     * @return {@code true} when the material owns an item of it
     */
    public boolean has(MaterialForm form) {
        return form != null && forms.contains(form);
    }

    /**
     * Name the item of a shape carries.
     *
     * @param form shape of this material
     * @return the technical name, for example {@code "iron_dust"}
     */
    public String itemName(MaterialForm form) {
        Objects.requireNonNull(form, "form");
        return name + form.itemSuffix();
    }

    /**
     * Id a shape was declared with, or {@code null} when it takes the next free one.
     * <p>
     * Read while the items are registered, see {@link Materials#registerAll()}. A declared id is
     * how the iron ingot kept the number {@code Items.IRON_INGOT_ID} it was saved with.
     *
     * @param form shape of this material
     * @return the declared id, or {@code null}
     */
    Integer declaredId(MaterialForm form) {
        return declaredIds.get(form);
    }

    /**
     * Remembers the item of a shape, called once while the game starts.
     *
     * @param form shape the item stands for
     * @param item item that was registered for it
     */
    void put(MaterialForm form, Item item) {
        items.put(form, item);
    }

    /**
     * The item of a shape, the way the player carries it.
     *
     * @param form shape to look up
     * @return the item, or {@code null} when this material does not come in that shape
     * @throws IllegalStateException when the shape is declared but its item was never registered
     */
    public Item form(MaterialForm form) {
        if (form == null || !forms.contains(form)) {
            return null;
        }
        Item item = items.get(form);
        if (item == null) {
            throw new IllegalStateException("The " + form + " of " + name
                    + " is declared but was never registered, Materials.registerAll() has to run");
        }
        return item;
    }

    /**
     * Every item of this material, in the order of {@link MaterialForm}.
     *
     * @return a fresh list, empty while the registry has not run yet
     */
    public List<Item> items() {
        List<Item> result = new ArrayList<>(items.size());
        for (MaterialForm form : MaterialForm.values()) {
            Item item = items.get(form);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // The shapes, one reader each, so a caller spells the shape out.
    // ------------------------------------------------------------------

    /** The dust of this material, {@code null} when it comes in none. */
    public Item dust() {
        return form(MaterialForm.DUST);
    }

    /** The small pile of dust of this material, {@code null} when it comes in none. */
    public Item smallDust() {
        return form(MaterialForm.SMALL_DUST);
    }

    /** The tiny pile of dust of this material, {@code null} when it comes in none. */
    public Item tinyDust() {
        return form(MaterialForm.TINY_DUST);
    }

    /** The ingot of this material, {@code null} when it comes in none. */
    public Item ingot() {
        return form(MaterialForm.INGOT);
    }

    /** The nugget of this material, {@code null} when it comes in none. */
    public Item nugget() {
        return form(MaterialForm.NUGGET);
    }

    /** The plate of this material, {@code null} when it comes in none. */
    public Item plate() {
        return form(MaterialForm.PLATE);
    }

    /** The foil of this material, {@code null} when it comes in none. */
    public Item foil() {
        return form(MaterialForm.FOIL);
    }

    /** The rod of this material, {@code null} when it comes in none. */
    public Item rod() {
        return form(MaterialForm.ROD);
    }

    /** The long rod of this material, {@code null} when it comes in none. */
    public Item longRod() {
        return form(MaterialForm.LONG_ROD);
    }

    /** The bolt of this material, {@code null} when it comes in none. */
    public Item bolt() {
        return form(MaterialForm.BOLT);
    }

    /** The screw of this material, {@code null} when it comes in none. */
    public Item screw() {
        return form(MaterialForm.SCREW);
    }

    /** The ring of this material, {@code null} when it comes in none. */
    public Item ring() {
        return form(MaterialForm.RING);
    }

    /** The round of this material, {@code null} when it comes in none. */
    public Item round() {
        return form(MaterialForm.ROUND);
    }

    /** The fine wire of this material, {@code null} when it comes in none. */
    public Item fineWire() {
        return form(MaterialForm.FINE_WIRE);
    }

    /** The spring of this material, {@code null} when it comes in none. */
    public Item spring() {
        return form(MaterialForm.SPRING);
    }

    /** The small spring of this material, {@code null} when it comes in none. */
    public Item smallSpring() {
        return form(MaterialForm.SMALL_SPRING);
    }

    /** The gear of this material, {@code null} when it comes in none. */
    public Item gear() {
        return form(MaterialForm.GEAR);
    }

    /** The small gear of this material, {@code null} when it comes in none. */
    public Item smallGear() {
        return form(MaterialForm.SMALL_GEAR);
    }

    /** The rotor of this material, {@code null} when it comes in none. */
    public Item rotor() {
        return form(MaterialForm.ROTOR);
    }

    /** The cell of this material, {@code null} when it comes in none. */
    public Item cell() {
        return form(MaterialForm.FLUID_CELL);
    }

    /**
     * Starts building a material.
     *
     * @param name technical name, also the head of the name of every one of its items
     * @param displayName name shown to the player
     * @return the builder, see {@link Builder}
     */
    public static Builder builder(String name, String displayName) {
        return new Builder(name, displayName);
    }

    /**
     * Fluent builder for {@link Material} instances.
     * <p>
     * A material needs a colour, everything else has a sensible default: the shapes of its kind
     * and no chemical formula. A formula that is not set simply keeps the second line of the
     * tooltip away, see {@link Item#tooltipLines()}.
     */
    public static final class Builder {

        private final String name;
        private final String displayName;
        private Color color;
        private String chemicalFormula = "";
        private MaterialKind kind = MaterialKind.METAL;
        private final Set<MaterialForm> extraForms = EnumSet.noneOf(MaterialForm.class);
        private EnumSet<MaterialForm> onlyForms;
        private final Map<MaterialForm, Integer> declaredIds = new EnumMap<>(MaterialForm.class);

        private Builder(String name, String displayName) {
            this.name = Objects.requireNonNull(name, "name");
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            if (name.isBlank()) {
                throw new IllegalArgumentException("The name of a material must not be blank");
            }
        }

        /**
         * Sets the colour every shape of this material is painted in.
         *
         * @param color colour the items take as their tint
         */
        public Builder color(Color color) {
            this.color = new Color(Objects.requireNonNull(color, "color"));
            return this;
        }

        /**
         * Sets the chemical formula shown under the name of every item.
         *
         * @param chemicalFormula formula such as {@code "Fe"} or {@code "Al2O3"}
         */
        public Builder formula(String chemicalFormula) {
            this.chemicalFormula = Objects.requireNonNull(chemicalFormula, "chemicalFormula");
            return this;
        }

        /**
         * Sets what the material is made of, which is where its shapes come from.
         *
         * @param kind kind of material, {@link MaterialKind#METAL} by default
         */
        public Builder kind(MaterialKind kind) {
            this.kind = Objects.requireNonNull(kind, "kind");
            return this;
        }

        /**
         * Adds shapes on top of the ones the kind brings.
         *
         * @param forms shapes this material comes in as well
         */
        public Builder forms(MaterialForm... forms) {
            Objects.requireNonNull(forms, "forms");
            for (MaterialForm form : forms) {
                extraForms.add(Objects.requireNonNull(form, "form"));
            }
            return this;
        }

        /**
         * Declares the shapes of this material instead of taking the ones of its kind.
         * <p>
         * A metal comes in the shapes of a metal, see {@link MaterialKind}, and a material of
         * another kind brings its own list. The shapes named here are the whole list, which is
         * what lets a material own fewer shapes than its kind: a metal that is only ground into a
         * dust declares {@code onlyForms(DUST)} and owns nothing else.
         *
         * @param forms every shape this material comes in, at least one
         * @throws IllegalArgumentException when the list is empty
         */
        public Builder onlyForms(MaterialForm... forms) {
            Objects.requireNonNull(forms, "forms");
            EnumSet<MaterialForm> declared = EnumSet.noneOf(MaterialForm.class);
            for (MaterialForm form : forms) {
                declared.add(Objects.requireNonNull(form, "form"));
            }
            if (declared.isEmpty()) {
                throw new IllegalArgumentException("A material comes in at least one shape: " + name);
            }
            this.onlyForms = declared;
            return this;
        }

        /**
         * Declares the id the item of a shape is registered with.
         * <p>
         * An id is permanent, so an item that lived in {@code Items} before it moved here keeps
         * the number it was saved with: the iron ingot is registered with {@code 19} because a
         * stored inventory that holds it spells that number out. A shape without a declared id
         * takes the next free one, see {@link Materials#registerAll()}.
         *
         * @param form shape whose item gets the id
         * @param id numeric item id, lower than the first free one
         */
        public Builder item(MaterialForm form, int id) {
            Objects.requireNonNull(form, "form");
            if (id < 0) {
                throw new IllegalArgumentException("Item id must not be negative: " + id);
            }
            if (declaredIds.put(form, id) != null) {
                throw new IllegalArgumentException("Two ids declared for the " + form + " of "
                        + name);
            }
            extraForms.add(form);
            return this;
        }

        /**
         * Builds the material.
         *
         * @return the material, ready to be registered
         * @throws IllegalArgumentException when no colour was set
         */
        public Material build() {
            if (color == null) {
                throw new IllegalArgumentException("A material needs a colour: " + name);
            }
            return new Material(this);
        }

        /** Every shape the material comes in, the ones of its kind and its own. */
        private EnumSet<MaterialForm> forms() {
            EnumSet<MaterialForm> all = onlyForms != null ? EnumSet.copyOf(onlyForms)
                    : EnumSet.copyOf(kind.defaultForms());
            all.addAll(extraForms);
            return all;
        }

        /** Ids the material declared, read while its items are registered. */
        private Map<MaterialForm, Integer> declaredIds() {
            return declaredIds;
        }
    }

    @Override
    public String toString() {
        return "Material(" + name + ", " + forms.size() + " forms)";
    }
}
