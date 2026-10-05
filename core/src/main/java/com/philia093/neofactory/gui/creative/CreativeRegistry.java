package com.philia093.neofactory.gui.creative;

import com.philia093.neofactory.item.ChemicalItems;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.pipe.PipeMaterials;
import com.philia093.neofactory.cable.CableKind;
import com.philia093.neofactory.cable.CableMaterials;
import com.philia093.neofactory.cable.CableSize;
import com.philia093.neofactory.cable.Cables;
import com.philia093.neofactory.pipe.PipeSize;
import com.philia093.neofactory.pipe.Pipes;

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
            CreativeTab.items("machines", "Machines", Items.FURNACE,
                item -> CreativeRegistry.isMachine(item) && !CreativeRegistry.isCable(item)),
        CreativeTab.items("wires", "Wires", Cables.of(CableMaterials.COPPER, CableSize.SINGLE,
                CableKind.WIRE).item(), CreativeRegistry::isCable),
            CreativeTab.items("pipes", "Pipes", Pipes.of(PipeMaterials.BRONZE, PipeSize.MEDIUM).item(),
                    CreativeRegistry::isPipe),
            CreativeTab.items("chemical", "Chemical", Items.WATER_CELL, CreativeRegistry::isChemical),
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

    /** {@code true} when an item places a plain block, the machines and the pipes have tabs of their own. */
    private static boolean isBlock(Item item) {
        return item.isBlockItem() && !isMachine(item) && !isPipe(item);
    }

    /**
     * {@code true} when an item places a pipe.
     * <p>
     * A pipe carries a block entity like a machine does, so it would land in the tab of the machines by
     * itself, see {@link #isMachine(Item)}: the pipes have a tab of their own instead, because a player who
     * is building a line of them looks for all of them at once, see {@link Pipes}.
     */
    private static boolean isPipe(Item item) {
        return item.isBlockItem() && item.block() != null && Pipes.of(item.block()) != null;
    }

    /**
     * {@code true} when an item places a cable of the line of the power.
     * <p>
     * A cable carries a block entity like a pipe does, so it would land in the tab of the machines by
     * itself: the wires have a tab of their own instead, because a player who is building a line of them
     * looks for all of them at once - every material, every width and both the bare line and the one with a
     * skin - see {@link Cables} and {@code CableKind}.
     */
    private static boolean isCable(Item item) {
        return item.isBlockItem() && item.block() != null && Cables.of(item.block()) != null;
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
     * <p>
     * <b>A container is no machine.</b> A chest carries a block entity as well, but it works on
     * nothing: it only keeps what somebody put in, so it is listed with the blocks it is built
     * from, see {@link com.philia093.neofactory.block.Block#isContainer()}.
     */
    private static boolean isMachine(Item item) {
        return item.isBlockItem() && item.block() != null && item.block().hasBlockEntity()
                && !item.block().isContainer() && !isPipe(item);
    }

    /** {@code true} when an item is a raw material the game has no other group for. */
    private static boolean isMaterial(Item item) {
        return item != Items.AIR && !item.isBlockItem() && !isTool(item) && !isFood(item)
                && !isChemical(item);
    }

    /**
     * {@code true} when an item is the item a substance of the catalog travels as.
     * <p>
     * A substance is a thing of its own to the player and not a raw material among many: a dust of
     * carbon, a cell of water and every other shape the chemistry of the industry is carried in have a
     * tab of their own, so the substances are found in one place and no other tab has to be read through
     * to find the one a recipe asks for, see {@link ChemicalItems}.
     * <p>
     * <b>An item that already stood for a substance moves here.</b> The dust of iron and the cell of
     * water are the items the industry has handed around since it began; they are listed with the other
     * substances rather than left behind, so a player who looks for a dust looks in one place and finds
     * every one of them.
     */
    private static boolean isChemical(Item item) {
        return ChemicalItems.isChemical(item);
    }

    /**
     * {@code true} when an item carries a fluid, which is a cell.
     * <p>
     * The containers have a tab of their own because they belong to the fluids and not to the
     * materials: a cell of water is the water of a lake the player carries around, and the cell is
     * the shape every fluid of the industry travels in. The game has one container and no bucket, see
     * {@link com.philia093.neofactory.item.FluidCells}.
     */
    /**
     * {@code true} when an item carries a fluid, which is a cell.
     * <p>
     * The containers belong to the chemistry: a cell of water is the water the chemistry of the industry
     * works with and it stands in the tab of the substances with every other one of them, so no tab is
     * needed for the fluids alone, see {@link #isChemical(Item)}.
     *
     * @param item item to test
     * @return {@code true} when the item is a cell that is not the cell of a substance
     */
    private static boolean isFluid(Item item) {
        return item.isFluidContainer() && !isChemical(item);
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
