package com.philia093.neofactory.entity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global lookup table for every {@link EntityType} known to the game.
 * <p>
 * Types are registered once during startup through {@link EntityTypes#registerAll()},
 * exactly like blocks and items are. A save game only stores the name of a type, so
 * this table is what turns a stored name back into an entity while reading a world.
 * A name that is not known is reported and skipped instead of taking the whole file
 * down with it, which keeps one broken entity from making a world unreadable.
 */
public final class EntityRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Maximum amount of entity types, only a sanity bound on the lookup table. */
    public static final int MAX_ENTITY_TYPES = 256;

    private static final List<EntityType> BY_ID = new ArrayList<>();
    private static final Map<String, EntityType> BY_NAME = new HashMap<>();
    private static boolean frozen;

    private EntityRegistry() {
        // Utility class: never instantiated.
    }

    /**
     * Adds an entity type to the registry.
     *
     * @param type type to register
     * @throws IllegalStateException when the registry was already frozen
     * @throws IllegalArgumentException when the id is out of range or the id or the
     *         name is used twice
     */
    public static void register(EntityType type) {
        if (frozen) {
            throw new IllegalStateException("Entity registry is frozen, cannot add "
                    + type.name());
        }
        if (type.id() >= MAX_ENTITY_TYPES) {
            throw new IllegalArgumentException("Entity id out of range: " + type.id());
        }
        if (BY_NAME.containsKey(type.name())) {
            throw new IllegalArgumentException("Duplicate entity name: " + type.name());
        }
        while (BY_ID.size() <= type.id()) {
            BY_ID.add(null);
        }
        if (BY_ID.get(type.id()) != null) {
            throw new IllegalArgumentException("Duplicate entity id: " + type.id());
        }
        BY_ID.set(type.id(), type);
        BY_NAME.put(type.name(), type);
    }

    /** Prevents any further registration, called once startup is finished. */
    public static void freeze() {
        frozen = true;
        LOGGER.info("Entity registry frozen with {} entity types", BY_NAME.size());
    }

    /**
     * Returns the type with the given id.
     *
     * @param id numeric entity id
     * @return the matching type, or {@code null} when the id is unused
     */
    public static EntityType byId(int id) {
        if (id < 0 || id >= BY_ID.size()) {
            return null;
        }
        return BY_ID.get(id);
    }

    /**
     * Returns the type with the given name.
     *
     * @param name name such as {@code "player"}
     * @return the matching type, or {@code null} when no type uses that name
     */
    public static EntityType byName(String name) {
        return name == null ? null : BY_NAME.get(name);
    }

    /** Returns every registered type, in id order. */
    public static List<EntityType> all() {
        List<EntityType> result = new ArrayList<>(BY_NAME.size());
        for (EntityType type : BY_ID) {
            if (type != null) {
                result.add(type);
            }
        }
        return result;
    }

    /** Amount of registered entity types. */
    public static int count() {
        return BY_NAME.size();
    }
}
