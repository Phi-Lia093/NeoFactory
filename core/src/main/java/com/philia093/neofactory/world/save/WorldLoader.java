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
 * Loading a chunk happens in three steps, and the order matters:
 * <ol>
 *     <li>the level data is read, which gives the seed and the player state</li>
 *     <li>a fresh world is created from that seed, generating the terrain around the
 *         player</li>
 *     <li>every stored chunk replaces the generated one</li>
 * </ol>
 * Step three has to see a chunk that is fully generated, which is why the world
 * finishes the area around the player before the file is applied. Writing into a
 * chunk that is still being generated would let the generator fill its cells later
 * and bury what the player built. A chunk from the file carries its own
 * "generated" flags, so once it is applied the generator never touches it again.
 */
public final class WorldLoader {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Chunks loaded around the player before the stored ones are applied. */
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

        World world = new World(data.seed(), spawnBlockX, spawnBlockY);
        world.ensureChunksAround(data.playerX() / Constants.TILE_SIZE,
                data.playerY() / Constants.TILE_SIZE, LOAD_CHUNK_RADIUS);

        NbtList chunks = root.getList(SaveTags.CHUNKS);
        int applied = applyChunks(world, chunks);
        LOGGER.info("Opened world '{}' (seed {}) with {} of {} stored chunks", data.worldName(),
                data.seed(), applied, chunks == null ? 0 : chunks.size());
        return new WorldLoader(summary, data, world, applied);
    }

    /**
     * Applies the stored chunks to a world.
     * <p>
     * A stored chunk is filled directly instead of being generated first: it carries
     * its own cells and the flags that mark them as done, so generating it would only
     * be thrown away again. A chunk that is missing from the file is generated from
     * the seed as usual when the player walks into it.
     *
     * @param world world to fill
     * @param chunks stored chunks, may be {@code null}
     * @return amount of chunks that were applied
     */
    private static int applyChunks(World world, NbtList chunks) {
        if (chunks == null) {
            return 0;
        }
        int applied = 0;
        for (int index = 0; index < chunks.size(); index++) {
            NbtCompound entry = chunks.getCompound(index);
            int chunkX = entry.getInt(ChunkCodec.TAG_X, Integer.MIN_VALUE);
            int chunkY = entry.getInt(ChunkCodec.TAG_Y, Integer.MIN_VALUE);
            if (chunkX == Integer.MIN_VALUE || chunkY == Integer.MIN_VALUE) {
                LOGGER.warn("Chunk {} of the save game holds no coordinates and is skipped", index);
                continue;
            }
            Chunk chunk = world.chunkForLoading(chunkX, chunkY);
            ChunkCodec.read(chunk, entry);
            applied++;
        }
        return applied;
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

    /** Amount of chunks that came out of the file. */
    public int storedChunks() {
        return storedChunks;
    }
}
