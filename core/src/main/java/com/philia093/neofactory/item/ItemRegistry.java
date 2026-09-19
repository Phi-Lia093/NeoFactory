package com.philia093.neofactory.item;

import com.philia093.neofactory.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global lookup table for every {@link Item} type known to the game.
 * <p>
 * Items are registered once during startup through {@link Items#registerAll()},
 * exactly like blocks are registered through
 * {@link com.philia093.neofactory.block.Blocks#registerAll()}. Next to the id and
 * the name lookup the registry keeps the reverse link from a block to the item
 * that places it, which is what a future block breaking system asks for.
 */
public final class ItemRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Maximum number of item types.
     * <p>
     * Items are never stored inside a chunk, so the limit is only a sanity bound
     * that keeps the lookup table small. Item ids still have to stay stable
     * between save games.
     */
    public static final int MAX_ITEMS = 4096;

    private static final List<Item> BY_ID = new ArrayList<>();
    private static final Map<String, Item> BY_NAME = new HashMap<>();
    private static final Map<Block, Item> BY_BLOCK = new HashMap<>();
    private static boolean frozen = false;

    private ItemRegistry() {
        // Utility class: never instantiated.
    }

    /**
     * Adds an item to the registry.
     *
     * @param item item to register
     * @throws IllegalStateException when the registry was already frozen or when
     *         two items place the same block
     * @throws IllegalArgumentException when the id is out of range or when the id
     *         or the name is used twice
     */
    public static void register(Item item) {
        if (frozen) {
            throw new IllegalStateException("Item registry is frozen, cannot add " + item.name());
        }
        if (item.id() < 0 || item.id() >= MAX_ITEMS) {
            throw new IllegalArgumentException("Item id out of range: " + item.id());
        }
        if (BY_NAME.containsKey(item.name())) {
            throw new IllegalArgumentException("Duplicate item name: " + item.name());
        }
        if (item.id() < BY_ID.size() && BY_ID.get(item.id()) != null) {
            throw new IllegalArgumentException("Duplicate item id: " + item.id());
        }
        Block block = item.block();
        if (block != null && BY_BLOCK.containsKey(block)) {
            throw new IllegalStateException("Block " + block.name() + " is already placed by "
                    + BY_BLOCK.get(block).name() + ", cannot add " + item.name());
        }

        while (BY_ID.size() <= item.id()) {
            BY_ID.add(null);
        }
        BY_ID.set(item.id(), item);
        BY_NAME.put(item.name(), item);
        if (block != null) {
            BY_BLOCK.put(block, item);
        }
    }

    /** Prevents any further registration, called once startup is finished. */
    public static void freeze() {
        frozen = true;
        LOGGER.info("Item registry frozen with {} item types", BY_NAME.size());
    }

    /**
     * Returns the item with the given id.
     *
     * @param id numeric item id
     * @return the matching item or {@link Items#AIR} when the id is unused
     */
    public static Item byId(int id) {
        if (id < 0 || id >= BY_ID.size()) {
            return Items.AIR;
        }
        Item item = BY_ID.get(id);
        return item == null ? Items.AIR : item;
    }

    /**
     * Returns the item with the given name.
     *
     * @param name item name such as {@code "diamond"}
     * @return the matching item or {@code null} when no item uses that name
     */
    public static Item byName(String name) {
        return BY_NAME.get(name);
    }

    /**
     * Returns the item that places the given block.
     *
     * @param block block to look up
     * @return the matching block item, or {@code null} when the block has no item,
     *         for example air or water
     */
    public static Item byBlock(Block block) {
        return BY_BLOCK.get(block);
    }

    /** Returns every registered item, including air. */
    public static List<Item> all() {
        List<Item> result = new ArrayList<>(BY_NAME.size());
        for (Item item : BY_ID) {
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    /** Amount of registered item types. */
    public static int count() {
        return BY_NAME.size();
    }
}
