package com.philia093.neofactory.material;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global lookup table for every {@link Material} the game knows.
 * <p>
 * The table is written once during startup through {@link Materials#registerAll()}, exactly like
 * the blocks and the items are, and is frozen right after: a material that arrives later would
 * shift the ids of the items of the ones before it, and an id of an item is permanent - a stored
 * inventory keeps it, see {@link com.philia093.neofactory.item.Items}.
 */
public final class MaterialRegistry {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<Material> BY_ORDER = new ArrayList<>();
    private static final Map<String, Material> BY_NAME = new HashMap<>();
    private static boolean frozen;

    private MaterialRegistry() {
        // Utility class: never instantiated.
    }

    /**
     * Adds a material to the registry.
     *
     * @param material material to register
     * @throws IllegalStateException when the registry was already frozen
     * @throws IllegalArgumentException when the name is used twice
     */
    public static void register(Material material) {
        if (frozen) {
            throw new IllegalStateException("Material registry is frozen, cannot add "
                    + material.name());
        }
        if (BY_NAME.containsKey(material.name())) {
            throw new IllegalArgumentException("Duplicate material: " + material.name());
        }
        BY_ORDER.add(material);
        BY_NAME.put(material.name(), material);
    }

    /** Prevents any further registration, called once startup is finished. */
    public static void freeze() {
        frozen = true;
        LOGGER.info("Material registry frozen with {} materials and {} items", BY_ORDER.size(),
                itemCount());
    }

    /**
     * Looks a material up by its name.
     *
     * @param name name such as {@code "iron"}
     * @return the material, or {@code null} when no material uses that name
     */
    public static Material byName(String name) {
        return name == null ? null : BY_NAME.get(name);
    }

    /** Every material, in the order it was declared in. */
    public static List<Material> all() {
        return List.copyOf(BY_ORDER);
    }

    /** Amount of registered materials. */
    public static int count() {
        return BY_ORDER.size();
    }

    /** Amount of items the materials brought with them. */
    public static int itemCount() {
        int total = 0;
        for (Material material : BY_ORDER) {
            total += material.items().size();
        }
        return total;
    }
}
