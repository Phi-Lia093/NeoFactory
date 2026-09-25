package com.philia093.neofactory.world.save;

import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtList;
import com.philia093.neofactory.world.GameMode;
import com.philia093.neofactory.world.WorldType;

/**
 * Everything about a world that is not a block.
 * <p>
 * The tag tree mirrors the structure of the original game, which keeps a stored
 * world familiar and makes it easy to add entries later:
 * <pre>
 * NeoFactory
 *  +- DataVersion, WorldName, GameMode, WorldType, Seed, SpawnX, SpawnZ, Created, LastPlayed,
 *     PlayedMillis
 *  +- Player
 *  |   +- PosX, PosY, RotationX, RotationY, SelectedSlot
 *  |   +- Inventory: [ {Slot, id, Count}, ... ]
 *  +- Entities: []
 *  +- GameRules: { doDaylightCycle: 1, doWeatherCycle: 1, keepInventory: 0 }
 * </pre>
 * The chunks are not part of this file: one file per changed chunk lives below
 * {@link SaveFormat#CHUNK_FOLDER} instead, see {@link ChunkStorage}.
 * <p>
 * Groups that belong to systems which do not exist yet are written as well, so a
 * world stored today keeps its shape once they do. Reading never fails because an
 * entry is missing: every getter falls back to a default, see
 * {@link NbtCompound#getInt(String, int)}. A file of another save format is refused
 * instead, see {@link #read(NbtCompound)}.
 */
public final class LevelData {

    private String worldName = SaveFormat.DEFAULT_NAME;

    /** Mode the world is played in, {@link GameMode#SURVIVAL} until a player says otherwise. */
    private GameMode gameMode = GameMode.SURVIVAL;

    /** Terrain the world is made of, {@link WorldType#NORMAL} until a player says otherwise. */
    private WorldType worldType = WorldType.NORMAL;

    private int seed;
    private int spawnX;
    private int spawnZ;
    private long created;
    private long lastPlayed;
    private long playedMillis;

    private float playerX;
    private float playerZ;
    private float rotationX;
    private float rotationY;
    private int selectedSlot;

    /**
     * Items the world was stored with.
     * <p>
     * The level data does not own an inventory while the game runs, the player does,
     * so reading fills this array and {@link #applyInventory(PlayerInventory)} copies
     * it into the live inventory once the world is open.
     */
    private final ItemStack[] storedInventory = new ItemStack[PlayerInventory.SLOT_COUNT];

    /** Reads empty level data, used for a world that is about to be created. */
    public LevelData() {
        java.util.Arrays.fill(storedInventory, ItemStack.EMPTY);
    }

    /**
     * Reads level data from a stored world.
     *
     * @param root root tag of the stored world
     * @return the level data
     * @throws SaveException when the file was written by another save format
     */
    public static LevelData read(NbtCompound root) {
        int version = root.getInt(SaveTags.DATA_VERSION, 0);
        if (version != SaveFormat.DATA_VERSION) {
            throw new SaveException("World was written by save format " + version
                    + ", this build reads format " + SaveFormat.DATA_VERSION
                    + " and converts no other one");
        }

        LevelData data = new LevelData();
        data.worldName = root.getString(SaveTags.WORLD_NAME, SaveFormat.DEFAULT_NAME);
        data.gameMode = readGameMode(root.getString(SaveTags.GAME_MODE, ""));
        data.worldType = readWorldType(root.getString(SaveTags.WORLD_TYPE, ""));
        data.seed = root.getInt(SaveTags.SEED, 0);
        data.spawnX = root.getInt(SaveTags.SPAWN_X, 0);
        data.spawnZ = root.getInt(SaveTags.SPAWN_Z, 0);
        data.created = root.getLong(SaveTags.CREATED, System.currentTimeMillis());
        data.lastPlayed = root.getLong(SaveTags.LAST_PLAYED, data.created);
        data.playedMillis = root.getLong(SaveTags.PLAYED_MILLIS, 0L);

        NbtCompound player = root.getCompound(SaveTags.PLAYER);
        if (player != null) {
            data.playerX = player.getFloat(SaveTags.POS_X, 0.0f);
            data.playerZ = player.getFloat(SaveTags.POS_Z, 0.0f);
            data.rotationX = player.getFloat(SaveTags.ROTATION_X, 0.0f);
            data.rotationY = player.getFloat(SaveTags.ROTATION_Y, 0.0f);
            data.selectedSlot = player.getInt(SaveTags.SELECTED_SLOT, 0);
            data.readStoredInventory(player.getList(SaveTags.INVENTORY));
        }
        return data;
    }

    /**
     * Reads the mode of a world.
     * <p>
     * A world stored before the creative inventory existed carries no mode at all and
     * an unknown name is not from this build either, so both fall back to survival
     * instead of failing to open the world.
     *
     * @param name stored name of the mode, may be empty
     * @return the mode, {@link GameMode#SURVIVAL} when the name is unknown
     */
    private static GameMode readGameMode(String name) {
        GameMode mode = GameMode.byName(name);
        return mode == null ? GameMode.SURVIVAL : mode;
    }

    /**
     * Reads the terrain type of a world.
     * <p>
     * A world stored before flat worlds existed carries no type at all and an unknown name is not
     * from this build either, so both fall back to the generated landscape instead of failing to
     * open the world. The chunks of such a world were generated by it, and no other type would match
     * them.
     *
     * @param name stored name of the type, may be empty
     * @return the type, {@link WorldType#NORMAL} when the name is unknown
     */
    private static WorldType readWorldType(String name) {
        WorldType type = WorldType.byName(name);
        return type == null ? WorldType.NORMAL : type;
    }

    /**
     * Copies the stored items into an array.
     *
     * @param list list written by {@link SaveTags#writeInventory(PlayerInventory)},
     *             may be {@code null}
     */
    private void readStoredInventory(NbtList list) {
        if (list == null) {
            return;
        }
        for (int index = 0; index < list.size(); index++) {
            NbtCompound entry = list.getCompound(index);
            int slot = entry.getInt(SaveTags.SLOT, index);
            if (slot < 0 || slot >= storedInventory.length) {
                continue;
            }
            com.philia093.neofactory.item.Item item =
                    SaveTags.itemByName(entry.getString(SaveTags.ITEM_ID, ""));
            int count = entry.getInt(SaveTags.COUNT, 0);
            if (item == null) {
                storedInventory[slot] = ItemStack.EMPTY;
                continue;
            }
            ItemStack stack = ItemStack.of(item, count);
            // A tool carries what it has dug, so it is as worn after a save game as it was before it,
            // see SaveTags#DAMAGE and Damageable.
            if (!stack.isEmpty()) {
                stack.setDamage(entry.getInt(SaveTags.DAMAGE, 0));
            }
            storedInventory[slot] = stack;
        }
    }

    /**
     * Fills a live inventory with the stored items.
     *
     * @param inventory inventory of the player, replaced slot by slot
     */
    public void applyInventory(PlayerInventory inventory) {
        for (int slot = 0; slot < storedInventory.length && slot < inventory.size(); slot++) {
            inventory.set(slot, storedInventory[slot].copy());
        }
        inventory.setSelectedSlot(selectedSlot);
    }

    /**
     * Writes the level data into a compound, the chunks not included.
     *
     * @param inventory inventory of the player
     * @return the tag data of this world
     */
    public NbtCompound write(PlayerInventory inventory) {
        NbtCompound root = new NbtCompound(SaveFormat.ROOT_TAG);
        root.putInt(SaveTags.DATA_VERSION, SaveFormat.DATA_VERSION);
        root.putString(SaveTags.WORLD_NAME, worldName);
        root.putString(SaveTags.GAME_MODE, gameMode.modeName());
        root.putString(SaveTags.WORLD_TYPE, worldType.typeName());
        root.putInt(SaveTags.SEED, seed);
        root.putInt(SaveTags.SPAWN_X, spawnX);
        root.putInt(SaveTags.SPAWN_Z, spawnZ);
        root.putLong(SaveTags.CREATED, created);
        root.putLong(SaveTags.LAST_PLAYED, lastPlayed);
        root.putLong(SaveTags.PLAYED_MILLIS, playedMillis);

        NbtCompound player = new NbtCompound(SaveTags.PLAYER);
        player.putFloat(SaveTags.POS_X, playerX);
        player.putFloat(SaveTags.POS_Z, playerZ);
        player.putFloat(SaveTags.ROTATION_X, rotationX);
        player.putFloat(SaveTags.ROTATION_Y, rotationY);
        player.putInt(SaveTags.SELECTED_SLOT, selectedSlot);
        player.put(SaveTags.writeInventory(inventory));
        root.put(player);

        // Groups of systems that do not exist yet, so the shape of a file does not
        // change the day they arrive.
        root.put(new NbtList(SaveTags.ENTITIES));
        NbtCompound rules = new NbtCompound(SaveTags.GAME_RULES);
        rules.putBoolean(SaveTags.RULE_DAYLIGHT, true);
        rules.putBoolean(SaveTags.RULE_WEATHER, true);
        rules.putBoolean(SaveTags.RULE_KEEP_INVENTORY, false);
        root.put(rules);
        return root;
    }

    /**
     * Copies the state of a player into this data.
     *
     * @param playerX world X coordinate of the player
     * @param playerZ world Y coordinate of the player
     * @param rotationX facing X component of the player
     * @param rotationY facing Y component of the player
     * @param inventory inventory of the player
     */
    public void capturePlayer(float playerX, float playerZ, float rotationX, float rotationY,
            PlayerInventory inventory) {
        this.playerX = playerX;
        this.playerZ = playerZ;
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.selectedSlot = inventory.selectedSlot();
    }

    /** Name shown to the player. */
    public String worldName() {
        return worldName;
    }

    /** Mode the world is played in, see {@link GameMode}. */
    public GameMode gameMode() {
        return gameMode;
    }

    /**
     * Writes the mode of the world.
     *
     * @param gameMode mode to store, {@code null} keeps survival
     */
    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode == null ? GameMode.SURVIVAL : gameMode;
    }

    /** Terrain this world is made of, see {@link WorldType}. */
    public WorldType worldType() {
        return worldType;
    }

    /**
     * Writes the terrain of the world.
     * <p>
     * The type only belongs to a world that has no chunks yet: the land of a world is generated
     * once, so a world that is opened again is built from the type it was stored with.
     *
     * @param worldType terrain to store, {@code null} keeps the generated landscape
     */
    public void setWorldType(WorldType worldType) {
        this.worldType = worldType == null ? WorldType.NORMAL : worldType;
    }

    /**
     * Sets the name shown to the player.
     *
     * @param worldName requested name, blank falls back to the default name
     */
    public void setWorldName(String worldName) {
        this.worldName = SaveNames.sanitize(worldName);
    }

    /** Seed the world was generated from. */
    public int seed() {
        return seed;
    }

    /** Sets the seed of the world. */
    public void setSeed(int seed) {
        this.seed = seed;
    }

    /** Block X coordinate the player starts near. */
    public int spawnX() {
        return spawnX;
    }

    /** Block Y coordinate the player starts near. */
    public int spawnZ() {
        return spawnZ;
    }

    /** Sets the spawn point of the world. */
    public void setSpawn(int spawnX, int spawnZ) {
        this.spawnX = spawnX;
        this.spawnZ = spawnZ;
    }

    /** Time the world was created at, in milliseconds since 1970. */
    public long created() {
        return created;
    }

    /** Sets the creation time. */
    public void setCreated(long created) {
        this.created = created;
    }

    /** Time the world was played at last, in milliseconds since 1970. */
    public long lastPlayed() {
        return lastPlayed;
    }

    /** Sets the time the world was played at last. */
    public void setLastPlayed(long lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    /** Time played in this world, in milliseconds. */
    public long playedMillis() {
        return playedMillis;
    }

    /**
     * Adds to the played time.
     *
     * @param millis milliseconds to add, ignored when not positive
     */
    public void addPlayedMillis(long millis) {
        if (millis > 0) {
            playedMillis += millis;
        }
    }

    /** World X coordinate of the player. */
    public float playerX() {
        return playerX;
    }

    /** World Y coordinate of the player. */
    public float playerZ() {
        return playerZ;
    }

    /** Facing X component of the player. */
    public float rotationX() {
        return rotationX;
    }

    /** Facing Y component of the player. */
    public float rotationY() {
        return rotationY;
    }

    /** Slot the player selected in the hotbar. */
    public int selectedSlot() {
        return selectedSlot;
    }

    @Override
    public String toString() {
        return "LevelData(" + worldName + ", seed " + seed + ", spawn (" + spawnX + ", " + spawnZ
                + "), played " + playedMillis / 1000L + "s)";
    }
}
