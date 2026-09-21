package com.philia093.neofactory.blockentity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Global lookup table for every {@link BlockEntityType} the game knows.
 * <p>
 * Types are registered once during startup, exactly like blocks, items and entities are.
 * A chunk only stores the name of a type, so this table is what turns a stored name back
 * into an entity while a chunk is read. A name that is not known is reported and skipped
 * instead of taking the chunk down with it.
 * <p>
 * The table is frozen after registration, so a type of a later system is registered in
 * {@link BlockEntityTypes} and not while the game runs.
 */
public final class BlockEntityRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, BlockEntityType> BY_NAME = new LinkedHashMap<>();
    private static boolean frozen;

    private BlockEntityRegistry() {
        // Utility class: never instantiated.
    }

    /**
     * Adds a type to the registry.
     *
     * @param type type to register
     * @throws IllegalStateException when the registry was already frozen
     * @throws IllegalArgumentException when the name is used twice
     */
    public static void register(BlockEntityType type) {
        if (frozen) {
            throw new IllegalStateException("Block entity registry is frozen, cannot add "
                    + type.name());
        }
        if (BY_NAME.containsKey(type.name())) {
            throw new IllegalArgumentException("Duplicate block entity name: " + type.name());
        }
        BY_NAME.put(type.name(), type);
    }

    /** Prevents any further registration, called once startup is finished. */
    public static void freeze() {
        frozen = true;
        LOGGER.info("Block entity registry frozen with {} types", BY_NAME.size());
    }

    /**
     * Returns the type with the given name.
     *
     * @param name name such as {@code "furnace"}
     * @return the matching type, or {@code null} when no type uses that name
     */
    public static BlockEntityType byName(String name) {
        return name == null || name.isEmpty() ? null : BY_NAME.get(name);
    }

    /** Returns every registered type, in the order they were registered. */
    public static List<BlockEntityType> all() {
        return new ArrayList<>(BY_NAME.values());
    }

    /** Amount of registered types. */
    public static int count() {
        return BY_NAME.size();
    }
}
