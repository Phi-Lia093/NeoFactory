package com.philia093.neofactory.entity;

/**
 * Every entity type the game knows.
 * <p>
 * Ids and names are permanent: chunks and save games refer to them, so a type that
 * ships keeps its name forever. New types take the next free id, the same rule the
 * block table follows.
 */
public final class EntityTypes {

    /** The player, one per world. */
    public static final EntityType PLAYER = new EntityType(1, "player",
            () -> new Player(0.0f, 0.0f, 0.0f));

    /** Items lying on the ground after a block was broken. */
    public static final EntityType ITEM = new EntityType(2, "item",
            () -> new ItemEntity(0.0f, 0.0f, 0.0f, null));

    /** Id the next entity type will get. */
    public static final int NEXT_FREE_ID = 3;

    private static boolean registered;

    private EntityTypes() {
        // Utility class: never instantiated.
    }

    /** Registers every entity type, called once during startup. */
    public static void registerAll() {
        if (registered) {
            return;
        }
        EntityRegistry.register(PLAYER);
        EntityRegistry.register(ITEM);
        registered = true;
    }
}
