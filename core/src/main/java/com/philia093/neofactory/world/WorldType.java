package com.philia093.neofactory.world;

import java.util.Locale;

/**
 * How the terrain of a world is made.
 * <p>
 * The type is chosen when a world is created and stored with it, because it decides what the ground
 * of the world is built from: a chunk that was generated once is never generated again, so a world
 * cannot change its type afterwards without the land a player already saw turning into something
 * else. Everything above the ground - the blocks, the machines, the items, the save format - is the
 * same in every type.
 * <p>
 * The name is what a save file stores, so it may never change once a world was written with it, the
 * same rule {@link GameMode} follows.
 */
public enum WorldType {

    /** Hills, rivers, lakes, ores and trees, the landscape the game is played in. */
    NORMAL("normal", "Normal"),

    /**
     * A table of blocks: bedrock, one layer of dirt over it and one layer of grass on top.
     * <p>
     * A flat world exists for building and for trying a mechanic out. Every cell of its surface is
     * the same height, so a wall, a floor or a machine is built without a slope getting in the way, a
     * hole dug into the ground is level with the cell beside it, and the ground tells a body exactly
     * where the world ends. Nothing is planted on it and no water, lava or ore is in it - a
     * decoration that asks the land for a height would find the same one everywhere.
     */
    FLAT("flat", "Superflat");

    /** Name the save file stores. */
    private final String name;

    /** Name a form shows to the player. */
    private final String displayName;

    WorldType(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    /** Name the save file stores, never {@code null}. */
    public String typeName() {
        return name;
    }

    /** Name a form shows to the player, never {@code null}. */
    public String displayName() {
        return displayName;
    }

    /**
     * Looks a type up by the name a save file stores.
     *
     * @param name name of the type, upper and lower case mixed as the file likes
     * @return the type, or {@code null} when the game has none of that name
     */
    public static WorldType byName(String name) {
        if (name == null) {
            return null;
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        for (WorldType type : values()) {
            if (type.name.equals(key)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
