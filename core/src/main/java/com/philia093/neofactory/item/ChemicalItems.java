package com.philia093.neofactory.item;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.chemistry.Amounts;
import com.philia093.neofactory.chemistry.Blend;
import com.philia093.neofactory.chemistry.Chemical;
import com.philia093.neofactory.chemistry.Composition;
import com.philia093.neofactory.chemistry.Fraction;
import com.philia093.neofactory.chemistry.Phase;
import com.philia093.neofactory.chemistry.Substance;
import com.philia093.neofactory.chemistry.Substances;
import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.material.MaterialForm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The items the substances of the catalog travel as, one item per substance.
 * <p>
 * <b>A substance is an item and not a label on a stack.</b> The industry of the game has to be able to
 * name what it makes: a player holds a dust of carbon and a cell of water, a chest is filled with a
 * hundred of them, a recipe file spells the name out and a search box finds it. One plain item whose
 * contents only a saved stack could name works for a machine that never says anything to anybody, but it
 * cannot be seen, searched, listed or traded, so every substance the catalog knows gets an item of its
 * own here, the way every material got one in
 * {@link com.philia093.neofactory.material.Materials}.
 * <p>
 * <b>A shape follows the state of the substance.</b> A substance that is a solid travels as a dust, which
 * is a hundred millibuckets of it and the shape a grinder already leaves; everything else - a liquid, a
 * gas, a solution - travels in a cell of a thousand, which is what a cell of water holds, see
 * {@link Amounts}. The picture is the grey scale sheet of the shape and the colour is the colour of the
 * substance, so nothing here needs art: the tint is worked out of the elements the substance is made of,
 * see {@link #colorOf(Substance)}.
 * <p>
 * <b>An item that already stands for a substance is borrowed and not made twice.</b> A dust of iron is
 * the dust of the metal and a cell of water is the cell of the fluid; the industry would be lying if it
 * held a second one of either, so a substance that a material or a fluid already carries an item for
 * keeps that item and only the rest are made here. What is borrowed is listed by the tab of the chemistry
 * as well, so a player finds every dust of every substance in one place, see
 * {@link com.philia093.neofactory.gui.creative.CreativeRegistry}.
 * <p>
 * <b>When it runs.</b> {@link #registerAll()} is called at the end of {@link Items#registerAll()}, right
 * before the item table is frozen, and every item it makes takes the next free id - so a substance that
 * is added later moves no number that a stored inventory already names, see {@link ItemRegistry#nextId()}.
 */
public final class ChemicalItems {

    /** The substances the game knows, which is the list of the chemistry of the industry. */
    private static final Substances CATALOG = Substances.starter();

    /** The item of every substance this table has an item for. */
    private static final Map<Chemical, Item> BY_CHEMICAL = new LinkedHashMap<>();

    /** The substance every item of this table stands for. */
    private static final Map<Item, Substance> BY_ITEM = new LinkedHashMap<>();

    private ChemicalItems() {
        // Utility class: never instantiated.
    }

    /**
     * Gives every substance of the catalog an item, borrowing the ones the game already has.
     * <p>
     * A substance a material already carries - iron, copper, gold, the metals of the alloys - keeps the
     * dust of it, and a substance a cell already carries keeps that cell: the two of them are the items
     * the industry has been handing around since before the catalog existed.
     */
    public static void registerAll() {
        for (Substance substance : CATALOG.all()) {
            Item item = borrowed(substance);
            if (item == null) {
                item = make(substance);
            }
            BY_ITEM.put(item, substance);
            BY_CHEMICAL.putIfAbsent(substance.chemical(), item);
        }
    }

    /**
     * The item a substance travels as.
     *
     * @param substance substance of the catalog
     * @return the item, or {@code null} when the game has none for it
     */
    public static Item item(Substance substance) {
        return substance == null ? null : BY_CHEMICAL.get(substance.chemical());
    }

    /**
     * The item a substance travels as.
     *
     * @param chemical a substance of the catalog
     * @return the item, or {@code null} when no item stands for it
     */
    public static Item itemOf(Chemical chemical) {
        return chemical == null ? null : BY_CHEMICAL.get(chemical);
    }

    /**
     * The substance an item stands for.
     *
     * @param item item to look up
     * @return the substance, or {@code null} when the item carries none of its own
     */
    public static Substance substanceOf(Item item) {
        return item == null ? null : BY_ITEM.get(item);
    }

    /**
     * {@code true} when an item is the item of a substance of the catalog.
     * <p>
     * This is what the tab of the chemistry lists and what keeps every other tab from listing the same
     * item a second time, see {@link com.philia093.neofactory.gui.creative.CreativeRegistry}.
     *
     * @param item item to test
     * @return {@code true} when the item stands for a substance
     */
    public static boolean isChemical(Item item) {
        return item != null && BY_ITEM.containsKey(item);
    }

    /** Every item of this table, in the order the substances are declared. */
    public static List<Item> all() {
        return List.copyOf(BY_ITEM.keySet());
    }

    /** Amount of substances the game has an item for. */
    public static int count() {
        return BY_ITEM.size();
    }

    /**
     * The amount of a substance one piece of its item carries, in millibuckets.
     *
     * @param substance substance of the catalog
     * @return a hundred for a solid, which is a dust, a thousand for everything else, which is a cell
     */
    public static Fraction piece(Substance substance) {
        return substance.phase() == Phase.SOLID ? Amounts.DUST : Amounts.FLUID_CELL;
    }

    /** Every substance the catalog knows, for a caller that walks the whole table. */
    public static List<Substance> substances() {
        return new ArrayList<>(CATALOG.all());
    }

    /**
     * Finds an item the game already has for a substance.
     * <p>
     * The shape of the substance is looked for first - a dust for a solid, a cell for everything else -
     * and only then any item whose pile is the substance and nothing else, so the dust of iron is the
     * dust of the metal and never the ingot that happens to hold the same hundred millibuckets of it.
     */
    private static Item borrowed(Substance substance) {
        boolean solid = substance.phase() == Phase.SOLID;
        if (!solid) {
            // A cell of the fluid the substance is: the very container the industry has always carried a
            // fluid in, which is what makes a substance painted like a fluid and not like a plain item.
            Item cell = FluidCells.filled(Fluids.byName(substance.name()));
            if (cell != null && !BY_ITEM.containsKey(cell)) {
                return cell;
            }
        }
        String wanted = slug(substance.name()) + (solid ? "_dust" : "_cell");
        // The name first: a cell of water is the cell of the fluid and carries its fluid and not a pile,
        // so it is found by the name the industry has always written it with.
        Item byName = ItemRegistry.byName(wanted);
        if (byName != null && !BY_ITEM.containsKey(byName) && byName != Items.AIR) {
            return byName;
        }
        Item fallback = null;
        for (Item item : ItemRegistry.all()) {
            if (BY_ITEM.containsKey(item) || item == Items.AIR) {
                continue;
            }
            Blend pile = item.chemicals();
            if (pile.components().size() != 1 || !pile.components().containsKey(substance.chemical())) {
                continue;
            }
            if (item.name().endsWith(solid ? "_dust" : "_cell")) {
                return item;
            }
            if (fallback == null) {
                fallback = item;
            }
        }
        return fallback;
    }

    /** Makes the item of a substance the game has none for. */
    private static Item make(Substance substance) {
        boolean solid = substance.phase() == Phase.SOLID;
        String name = slug(substance.name()) + (solid ? "_dust" : "_cell");
        Item.Builder builder = Item.builder(ItemRegistry.nextId(), name)
                .displayName(title(substance.name()) + (solid ? " Dust" : " Cell"))
                .maxStackSize(Item.DEFAULT_MAX_STACK)
                .formula(substance.formula());
        if (solid) {
            builder.texture(MaterialForm.DUST.texture())
                    .tint(Fluids.colorOf(substance))
                    .chemicals(Blend.of(substance.chemical(), piece(substance)));
        } else {
            // Everything that flows is a cell of the fluid the substance is: a body of steel with a window
            // the colour of it, the very container water has always travelled in, so the picture of a
            // substance is drawn by the same rule as the picture of a fluid.
            builder.texture(Item.ITEM_FOLDER + "fluid_cell")
                    .maxStackSize(Item.SINGLE_ITEM_STACK)
                    .container(FluidContainer.cell(Fluids.byName(substance.name())))
                    .chemicals(Blend.of(substance.chemical(), piece(substance)));
        }
        return Items.registerChemical(builder.build());
    }

    /** The name of a substance as the tail of the name of an item. */
    private static String slug(String name) {
        StringBuilder slug = new StringBuilder(name.length());
        for (int index = 0; index < name.length(); index++) {
            char letter = name.charAt(index);
            if (Character.isLetterOrDigit(letter)) {
                slug.append(Character.toLowerCase(letter));
            } else if (slug.length() > 0 && slug.charAt(slug.length() - 1) != '_') {
                slug.append('_');
            }
        }
        while (slug.length() > 0 && slug.charAt(slug.length() - 1) == '_') {
            slug.setLength(slug.length() - 1);
        }
        return slug.toString();
    }

    /** The name of a substance the way a player reads it at an item. */
    private static String title(String name) {
        StringBuilder title = new StringBuilder(name.length());
        for (String word : name.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            if (title.length() > 0) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return title.toString();
    }

    /**
     * The colour a substance is drawn in.
     * <p>
     * The colour of an element stands in the table below; a substance of several elements takes the
     * colour of the one it holds most of, and one the table does not know is drawn pale, so a substance
     * is always a picture and never a blank square. Nothing here is a fact of the chemistry - a colour is
     * a thing a player reads - and no reaction ever asks for it.
     *
     * @param substance substance to paint
     * @return the colour of its item
     */
    public static Color colorOf(Substance substance) {
        if (substance == null) {
            return PALE;
        }
        String heaviest = null;
        int most = 0;
        for (Map.Entry<String, Integer> entry : substance.chemical().composition().byElement()
                .entrySet()) {
            if (entry.getValue() > most
                    || (entry.getValue() == most && heaviest != null
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

    @Override
    public String toString() {
        return "ChemicalItems(" + BY_ITEM.size() + " substances)";
    }
}
