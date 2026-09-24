package com.philia093.neofactory.world.save;

import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtException;
import com.philia093.neofactory.util.nbt.NbtIo;
import com.philia093.neofactory.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Opens a stored world.
 * <p>
 * The steps of loading are, in this order:
 * <ol>
 *     <li>the level file is read, which gives the seed, the spawn and the player
 *         state</li>
 *     <li>a world is created from that seed, with its chunk store attached, so the
 *         spawn area comes from the chunk files wherever they hold something</li>
 *     <li>the chunks around the player are loaded, which pulls them from the store
 *         or generates them from the seed</li>
 * </ol>
 * There is no moment where the terrain is generated and then replaced by stored
 * data: the store is asked first, so a chunk the player changed is never produced
 * by the seed in the first place. That is what keeps a tree planted by the
 * generator from burying what the player built there.
 */
public final class WorldLoader {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Chunks loaded around the player while opening. */
    private static final int LOAD_CHUNK_RADIUS = 2;

    private final SaveSummary summary;
    private final LevelData data;
    private final World world;
    private final int storedChunks;

    private WorldLoader(SaveSummary summary, LevelData data, World world, int storedChunks) {
        this.summary = summary;
        this.data = data;
        this.world = world;
        this.storedChunks = storedChunks;
    }

    /**
     * Opens a save game.
     *
     * @param storage storage holding the file
     * @param summary save game to open
     * @param spawnX block X coordinate of the spawn to use when the file holds none
     * @param spawnZ block Y coordinate of the spawn to use when the file holds none
     * @return the loaded world
     * @throws SaveException when the file cannot be read
     */
    public static WorldLoader open(WorldStorage storage, SaveSummary summary, int spawnX,
            int spawnZ) {
        File file = summary.levelFile();
        NbtCompound root;
        try {
            root = (NbtCompound) NbtIo.readGzip(file);
        } catch (NbtException | ClassCastException e) {
            // The file is kept and moved away, saving over it would destroy it.
            storage.backupCorrupt(file);
            throw new SaveException("World '" + summary.displayName() + "' cannot be opened, "
                    + "the damaged file was moved aside", e);
        }

        LevelData data = LevelData.read(root);
        // A world created by an older build may have no spawn stored yet, the caller
        // then decides where to start.
        int spawnBlockX = root.contains(SaveTags.SPAWN_X) ? data.spawnX() : spawnX;
        int spawnBlockY = root.contains(SaveTags.SPAWN_Z) ? data.spawnZ() : spawnZ;

        FileChunkStore store = new FileChunkStore(summary.folder());

        World world = new World(data.seed(), spawnBlockX, spawnBlockY, store, data.worldType());
        int entities = world.entities().load(root.getList(SaveTags.ENTITIES), world);
        world.loadChunksAround(data.playerX() / Constants.BLOCK_SIZE,
                data.playerZ() / Constants.BLOCK_SIZE, LOAD_CHUNK_RADIUS, Integer.MAX_VALUE);

        int stored = store.storedChunkCount();
        LOGGER.info("Opened world '{}' (seed {}) with {} stored chunks and {} entities",
                data.worldName(), data.seed(), stored, entities);
        return new WorldLoader(summary, data, world, stored);
    }

    /** Save game that was opened. */
    public SaveSummary summary() {
        return summary;
    }

    /** Level data of the world. */
    public LevelData data() {
        return data;
    }

    /** World that was created from the save game. */
    public World world() {
        return world;
    }

    /** Amount of chunks this world has stored. */
    public int storedChunks() {
        return storedChunks;
    }
}
