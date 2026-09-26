package com.philia093.neofactory.world.save;

import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtList;

/** Names of the tags a stored world uses, kept in one place. */
public final class SaveTags {

    /** Version of the save format. */
    public static final String DATA_VERSION = "DataVersion";

    /** Name of the world. */
    public static final String WORLD_NAME = "WorldName";

    /**
     * Mode the world is played in.
     * <p>
     * The entry is written since the creative inventory exists; a world stored before
     * that carries none and is read as {@code survival}, see
     * {@link com.philia093.neofactory.world.GameMode#byName(String)}.
     */
    public static final String GAME_MODE = "GameMode";

    /**
     * Terrain the world is made of.
     * <p>
     * The entry is written since flat worlds exist; a world stored before that carries none and is
     * read as {@code normal}, see {@link com.philia093.neofactory.world.WorldType#byName(String)}.
     */
    public static final String WORLD_TYPE = "WorldType";

    /** Seed of the terrain. */
    public static final String SEED = "Seed";

    /** Block X coordinate of the spawn. */
    public static final String SPAWN_X = "SpawnX";

    /** Block Z coordinate of the spawn. */
    public static final String SPAWN_Z = "SpawnZ";

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

    /** World Y coordinate of a body, its height in the world. */
    public static final String POS_Y = "PosY";

    /** World Z coordinate of a body. */
    public static final String POS_Z = "PosZ";

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
     * Damage a stack has taken.
     * <p>
     * Written for a piece that wears out and that has really been used, so a stack of stone or a new
     * pickaxe carries no tag at all and an inventory of an older world stays as short as it was. A
     * missing tag is read as a fresh piece, see {@code ItemStack#setDamage(int)} and
     * {@code Damageable}.
     */
    public static final String DAMAGE = "Damage";

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

    /** Group holding the type specific data of anything the game stores. */
    public static final String DATA = "Data";

    /** Group holding the type specific data of an entity. */
    public static final String ENTITY_DATA = DATA;

    /** First half of the identifier of an entity. */
    public static final String UUID_MOST = "UUIDMost";

    /** Second half of the identifier of an entity. */
    public static final String UUID_LEAST = "UUIDLeast";

    /** Velocity X component of an entity, world units per second. */
    public static final String VEL_X = "VelX";

    /** Velocity Y component of an entity, its speed towards the sky, world units per second. */
    public static final String VEL_Y = "VelY";

    /** Velocity Z component of an entity, world units per second. */
    public static final String VEL_Z = "VelZ";

    /** State of the world in which the entity was stored, used when it comes back. */
    public static final String ENTITY_AGE = "Age";

    /** Seconds left before an item on the ground may be picked up. */
    public static final String PICKUP_DELAY = "PickupDelay";

    /** Group holding what a machine adds to its shared state. */
    public static final String MACHINE_STATE = "State";

    /** Amount of energy a machine holds. */
    public static final String ENERGY = "Energy";

    /** List of the tanks of a machine. */
    public static final String TANKS = "Tanks";

    /** Kind of fluid inside one tank. */
    public static final String FLUID = "Fluid";

    /** Amount of fluid inside one tank. */
    public static final String FLUID_AMOUNT = "Amount";

    /** Seconds a machine has worked on its current craft. */
    public static final String CRAFT_SECONDS = "CraftSeconds";

    /** Seconds the current craft takes in total. */
    public static final String CRAFT_TOTAL = "CraftTotal";

    /** Energy a machine still owes for the craft it works on. */
    public static final String ENERGY_DEBT = "EnergyDebt";

    /** Seconds a furnace has left to burn. */
    public static final String BURN_SECONDS = "BurnSeconds";

    /**
     * Name of the flag with which a steam machine says that its exhaust was found blocked, see
     * {@code SteamMachine}: a machine that cannot blow its steam out waits with the next recipe.
     */
    public static final String EXHAUST_BLOCKED = "ExhaustBlocked";

    /** Name of the side a machine was turned to, see {@code MachineBlockEntity#facing()}. */
    public static final String MACHINE_FACING = "Facing";

    /** Name of the side a steam machine blows its steam out of, see {@code MachineBlockEntity}. */
    public static final String MACHINE_EXHAUST = "Exhaust";

    /** Seconds a whole piece of fuel burns. */
    public static final String BURN_TOTAL = "BurnTotal";

    /**
     * Name of the recipe a machine works on.
     * <p>
     * A machine swallows what a recipe needs before it starts, see
     * {@code RecipeMachine#startCraft(float)}, so the recipe it is working on has to travel with it: a
     * recipe that is looked for again after a world was opened would not find its input any more.
     */
    public static final String CRAFT_RECIPE = "CraftRecipe";

    /**
     * Water a boiler earned but has not turned into steam yet.
     * <p>
     * The amount is below one unit, so it is the part of the work of a frame that the arithmetic did not
     * reach and that the frames after it carry on with.
     */
    public static final String WATER_DEBT = "WaterDebt";

    /** Temperature a boiler has reached, in kelvin. */
    public static final String TEMPERATURE = "Temperature";

    /**
     * {@code true} once a boiler boiled dry while it was hot.
     * <p>
     * Such a boiler is ruined by the first water that reaches it, so the mark has to travel with it, see
     * {@code SteamBoilerMachine}.
     */
    public static final String SCORCHED = "Scorched";

    /** List of the block entities of a chunk. */
    public static final String BLOCK_ENTITIES = "BlockEntities";

    /** Name of the block entity type of one entry. */
    public static final String BLOCK_ENTITY_ID = "id";

    /** Local X coordinate of one block entity, inside its chunk. */
    public static final String BLOCK_ENTITY_X = "X";

    /** Block Y coordinate of one block entity, its height in the world. */
    public static final String BLOCK_ENTITY_Y = "Y";

    /** Local Z coordinate of one block entity, inside its chunk. */
    public static final String BLOCK_ENTITY_Z = "Z";

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
     * slot, so a stack always lands where it was. A piece that wears out carries how much damage it
     * has taken, see {@link #DAMAGE}.
     *
     * @param inventory inventory to write
     * @return the list tag
     */
    public static NbtList writeInventory(Inventory inventory) {
        NbtList list = new NbtList(INVENTORY);
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.get(slot);
            NbtCompound entry = new NbtCompound("");
            entry.putInt(SLOT, slot);
            entry.putString(ITEM_ID, stack.isEmpty() ? "" : stack.item().name());
            entry.putInt(COUNT, stack.count());
            if (stack.damage() > 0) {
                entry.putInt(DAMAGE, stack.damage());
            }
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
    public static void readInventory(Inventory inventory, NbtList list) {
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
            if (item == null) {
                inventory.set(slot, ItemStack.EMPTY);
                continue;
            }
            ItemStack stack = ItemStack.of(item, count);
            // A piece that an older world knew as new - or one that never wears out - carries no tag,
            // which reads as a fresh piece.
            if (!stack.isEmpty()) {
                stack.setDamage(entry.getInt(DAMAGE, 0));
            }
            inventory.set(slot, stack);
        }
    }
}
