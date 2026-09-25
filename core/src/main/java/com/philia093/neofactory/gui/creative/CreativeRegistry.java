package com.philia093.neofactory.gui.creative;

import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.Items;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The tabs of the creative inventory, in the order they are drawn.
 * <p>
 * The original game groups its items into a dozen tabs; this game holds a single
 * group of every item it owns, so the tabs follow the groups {@link Items} is written
 * in: the blocks, the machines, the materials, the food and the tools. Two
 * more tabs follow: the search, which lists what the player typed into the box, and the
 * inventory, which shows the very screen the player inventory shows.
 * <p>
 * A category is recognised by what an item <i>is</i> and not by its id, so a new item
 * lands in its group by itself: an item that places a block is a block, an item that
 * carries a tool kind is a tool and the food is listed by name, because the game has no
 * marker for it yet. Everything that is none of those is a material, which is also why
 * the categories together hold every item of the game exactly once.
 */
public final class CreativeRegistry {

    /** Names of the food of the game, see {@link Items}. */
    private static final Set<String> FOOD = Set.of("apple", "bread", "cookie", "carrot",
            "potato", "baked_potato", "cooked_beef", "cooked_porkchop", "cooked_chicken",
            "melon");

    private static final List<CreativeTab> TABS = List.of(
            CreativeTab.items("blocks", "Blocks", Items.STONE, CreativeRegistry::isBlock),
            CreativeTab.items("machines", "Machines", Items.FURNACE, CreativeRegistry::isMachine),
            CreativeTab.items("fluids", "Fluids", Items.WATER_BUCKET, CreativeRegistry::isFluid),
            CreativeTab.items("materials", "Materials", Items.STICK, CreativeRegistry::isMaterial),
            CreativeTab.items("food", "Food", Items.APPLE, CreativeRegistry::isFood),
            CreativeTab.items("tools", "Tools", Items.IRON_PICKAXE, CreativeRegistry::isTool),
            CreativeTab.search("search", "Search", Items.DIAMOND),
            CreativeTab.inventory("inventory", "Inventory", Items.PAPER));

    private CreativeRegistry() {
        // Utility class: never instantiated.
    }

    /** The tabs of the creative inventory, in the order they are drawn. */
    public static List<CreativeTab> tabs() {
        return TABS;
    }

    /**
     * The tab the creative inventory opens with.
     *
     * @return the first tab, which holds the blocks
     */
    public static CreativeTab firstTab() {
        return TABS.get(0);
    }

    /** {@code true} when an item places a plain block, the machines have a tab of their own. */
    private static boolean isBlock(Item item) {
        return item.isBlockItem() && !isMachine(item);
    }

    /**
     * {@code true} when the block of an item carries a machine inside it.
     * <p>
     * A machine is recognised by its block and not by its name: a block that asks for a
     * block entity holds a machine, which is what the furnace does today and what every
     * machine of the game will do, see
     * {@link com.philia093.neofactory.blockentity.BlockEntityTypes}. The tab of the blocks
     * hands its machines over instead of listing them twice, so every item lands in
     * exactly one tab.
     */
    private static boolean isMachine(Item item) {
        return item.isBlockItem() && item.block() != null && item.block().hasBlockEntity();
    }

    /** {@code true} when an item is a raw material the game has no other group for. */
    private static boolean isMaterial(Item item) {
        return item != Items.AIR && !item.isBlockItem() && !isTool(item) && !isFood(item)
                && !isFluid(item);
    }

    /**
     * {@code true} when an item carries a fluid, a bucket or a cell.
     * <p>
     * The containers have a tab of their own because they belong to the fluids and not to the
     * materials: a bucket of water is the water of a lake the player carries around, and the cell
     * is the shape every fluid of the industry travels in.
     */
    private static boolean isFluid(Item item) {
        return item.isFluidContainer();
    }

    /**
     * {@code true} when an item is a tool the player works with.
     * <p>
     * The kind of a tool is what marks it, see {@link com.philia093.neofactory.item.ToolType}: a
     * pickaxe, an axe, a shovel, a hoe and a sword are tools, while a material that happens to cause
     * damage one day is not.
     */
    private static boolean isTool(Item item) {
        return item.isTool();
    }

    /** {@code true} when an item is food, which the game lists by name. */
    private static boolean isFood(Item item) {
        return FOOD.contains(item.name().toLowerCase(Locale.ROOT));
    }
}
