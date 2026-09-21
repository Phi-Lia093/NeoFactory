package com.philia093.neofactory.world.save;

import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtException;
import com.philia093.neofactory.util.nbt.NbtIo;
import com.philia093.neofactory.util.nbt.NbtList;
import com.philia093.neofactory.world.Chunk;
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
 * <p>
 * A world written by format 1 kept all its chunks inside the level file. Those
 * chunks are converted into chunk files while opening, see
 * {@link #migrateLegacyChunks}, and the level file is rewritten without them.
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
     * @param spawnY block Y coordinate of the spawn to use when the file holds none
     * @return the loaded world
     * @throws SaveException when the file cannot be read
     */
    public static WorldLoader open(WorldStorage storage, SaveSummary summary, int spawnX,
            int spawnY) {
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
        int spawnBlockY = root.contains(SaveTags.SPAWN_Y) ? data.spawnY() : spawnY;

        FileChunkStore store = new FileChunkStore(summary.folder());
        int migrated = migrateLegacyChunks(summary, root, store);

        World world = new World(data.seed(), spawnBlockX, spawnBlockY, store);
        int entities = world.entities().load(root.getList(SaveTags.ENTITIES), world);
        world.loadChunksAround(data.playerX() / Constants.TILE_SIZE,
                data.playerY() / Constants.TILE_SIZE, LOAD_CHUNK_RADIUS, Integer.MAX_VALUE);

        int stored = store.storedChunkCount();
        LOGGER.info("Opened world '{}' (seed {}) with {} stored chunks and {} entities{}",
                data.worldName(), data.seed(), stored, entities,
                migrated > 0 ? ", " + migrated + " converted from format 1" : "");
        return new WorldLoader(summary, data, world, stored);
    }

    /**
     * Converts the chunks of a format 1 world into chunk files.
     * <p>
     * Format 1 stored every chunk the player had seen inside the level file. Those
     * chunks are read back, written into their own files and the list is dropped from
     * the level file, which turns the world into the format this build writes.
     * <p>
     * The conversion is guarded against running twice: a world that already has chunk
     * files keeps them, because they are newer than the list the level file still
     * holds. That can only happen when a conversion wrote its chunks and then failed
     * to rewrite the level file, and in that case the files win.
     *
     * @param summary save game being opened
     * @param root root tag of that save game, rewritten by this method
     * @param store store receiving the chunks
     * @return amount of chunks that were converted, {@code 0} for a current world
     */
    private static int migrateLegacyChunks(SaveSummary summary, NbtCompound root,
            FileChunkStore store) {
        NbtList legacy = root.getList(SaveTags.CHUNKS);
        if (legacy == null) {
            return 0;
        }
        boolean converted = store.storedChunkCount() > 0;
        int migrated = 0;
        for (int index = 0; index < legacy.size(); index++) {
            NbtCompound entry = legacy.getCompound(index);
            int chunkX = entry.getInt(ChunkCodec.TAG_X, Integer.MIN_VALUE);
            int chunkY = entry.getInt(ChunkCodec.TAG_Y, Integer.MIN_VALUE);
            if (chunkX == Integer.MIN_VALUE || chunkY == Integer.MIN_VALUE) {
                LOGGER.warn("Chunk {} of the old save game holds no coordinates and is skipped",
                        index);
                continue;
            }
            if (converted) {
                continue;
            }
            // An empty chunk is enough: the reader replaces every cell, and the file
            // written here is what the world reads from now on.
            Chunk chunk = new Chunk(chunkX, chunkY);
            ChunkCodec.read(chunk, entry);
            store.persist(chunk);
            migrated++;
        }

        // The list is gone from the level file. Dropping it is what stops the next
        // open from converting the same chunks again, which would write their old
        // content over the files that were written since.
        root.remove(SaveTags.CHUNKS);
        root.putInt(SaveTags.DATA_VERSION, SaveFormat.DATA_VERSION);
        AtomicNbtFile.write(root, summary.levelFile());
        LOGGER.info("Converted world '{}' to format {}: {} chunks moved out of the level file",
                summary.displayName(), SaveFormat.DATA_VERSION, migrated);
        return migrated;
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
