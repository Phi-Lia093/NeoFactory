package com.philia093.neofactory.world.save;

import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtList;

/** Names of the tags a stored world uses, kept in one place. */
public final class SaveTags {

    /** Version of the save format. */
    public static final String DATA_VERSION = "DataVersion";

    /** Name of the world. */
    public static final String WORLD_NAME = "WorldName";

    /** Seed of the terrain. */
    public static final String SEED = "Seed";

    /** Block X coordinate of the spawn. */
    public static final String SPAWN_X = "SpawnX";

    /** Block Y coordinate of the spawn. */
    public static final String SPAWN_Y = "SpawnY";

    /** Creation timestamp. */
    public static final String CREATED = "Created";

    /** Timestamp of the last visit. */
    public static final String LAST_PLAYED = "LastPlayed";

    /** Time played in this world. */
    public static final String PLAYED_MILLIS = "PlayedMillis";

    /** Group holding everything about the player. */
    public static final String PLAYER = "Player";

    /** World X coordinate of the player. */
    public static final String POS_X = "PosX";

    /** World Y coordinate of the player. */
    public static final String POS_Y = "PosY";

    /** Facing X component of the player. */
    public static final String ROTATION_X = "RotationX";

    /** Facing Y component of the player. */
    public static final String ROTATION_Y = "RotationY";

    /** Selected hotbar slot. */
    public static final String SELECTED_SLOT = "SelectedSlot";

    /** List of the items the player carries. */
    public static final String INVENTORY = "Inventory";

    /** Slot of a single stack. */
    public static final String SLOT = "Slot";

    /** Item name of a single stack. */
    public static final String ITEM_ID = "id";

    /** Amount of items of a single stack. */
    public static final String COUNT = "Count";

    /**
     * List of the chunks, written by format 1 and read back only to convert a
     * world to format 2, where every changed chunk lives in its own file.
     */
    public static final String CHUNKS = "Chunks";

    /**
     * Group of the stored entities.
     * <p>
     * Every entry holds the shared fields of an entity plus one nested group with
     * whatever the type adds, see
     * {@link com.philia093.neofactory.entity.Entity#writeTo(NbtCompound)}.
     */
    public static final String ENTITIES = "Entities";

    /** Type name of an entity, for example {@code "player"} or {@code "item"}. */
    public static final String ENTITY_ID = "id";

    /** Group holding the type specific data of an entity. */
    public static final String ENTITY_DATA = "Data";

    /** First half of the identifier of an entity. */
    public static final String UUID_MOST = "UUIDMost";

    /** Second half of the identifier of an entity. */
    public static final String UUID_LEAST = "UUIDLeast";

    /** Velocity X component of an entity, world units per second. */
    public static final String VEL_X = "VelX";

    /** Velocity Y component of an entity, world units per second. */
    public static final String VEL_Y = "VelY";

    /** State of the world in which the entity was stored, used when it comes back. */
    public static final String ENTITY_AGE = "Age";

    /** Seconds left before an item on the ground may be picked up. */
    public static final String PICKUP_DELAY = "PickupDelay";

    /** Group of the world rules. */
    public static final String GAME_RULES = "GameRules";

    /** Whether the day cycle runs, unused so far. */
    public static final String RULE_DAYLIGHT = "doDaylightCycle";

    /** Whether the weather changes, unused so far. */
    public static final String RULE_WEATHER = "doWeatherCycle";

    /** Whether the inventory survives death, unused so far. */
    public static final String RULE_KEEP_INVENTORY = "keepInventory";

    private SaveTags() {
        // Utility class: never instantiated.
    }

    /**
     * Looks up an item by the name stored in a file.
     *
     * @param name item name, may be empty
     * @return the item, or {@code null} when the name is empty or unknown
     */
    public static com.philia093.neofactory.item.Item itemByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return ItemRegistry.byName(name);
    }

    /**
     * Writes an inventory as a list of stacks.
     * <p>
     * An empty slot is written with a count of zero instead of being skipped, which
     * keeps the slot number of a stack implicit: the index inside the list is the
     * slot, so a stack always lands where it was.
     *
     * @param inventory inventory to write
     * @return the list tag
     */
    public static NbtList writeInventory(PlayerInventory inventory) {
        NbtList list = new NbtList(INVENTORY);
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.get(slot);
            NbtCompound entry = new NbtCompound("");
            entry.putInt(SLOT, slot);
            entry.putString(ITEM_ID, stack.isEmpty() ? "" : stack.item().name());
            entry.putInt(COUNT, stack.count());
            list.add(entry);
        }
        return list;
    }

    /**
     * Reads an inventory from a list written by
     * {@link #writeInventory(PlayerInventory)}.
     *
     * @param inventory inventory to fill
     * @param list list to read, may be {@code null}
     */
    public static void readInventory(PlayerInventory inventory, NbtList list) {
        if (list == null) {
            return;
        }
        for (int index = 0; index < list.size(); index++) {
            NbtCompound entry = list.getCompound(index);
            int slot = entry.getInt(SLOT, index);
            if (slot < 0 || slot >= inventory.size()) {
                continue;
            }
            com.philia093.neofactory.item.Item item =
                    itemByName(entry.getString(ITEM_ID, ""));
            int count = entry.getInt(COUNT, 0);
            inventory.set(slot, item == null ? ItemStack.EMPTY : ItemStack.of(item, count));
        }
    }
}
