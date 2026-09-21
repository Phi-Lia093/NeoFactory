package com.philia093.neofactory.world.save;

import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Writes a world into its save game.
 * <p>
 * A world on disk consists of two parts:
 * <ul>
 *     <li>{@link SaveFormat#LEVEL_FILE} holds everything that is not a block, see
 *         {@link LevelData}. It is small and written on every save.</li>
 *     <li>one file per chunk below {@link SaveFormat#CHUNK_FOLDER} holds the chunks
 *         the player changed, see {@link ChunkStorage}</li>
 * </ul>
 * Only changed chunks are written, and each of them goes into its own file, which
 * is what makes a save cost what was built since the last one instead of what is
 * loaded. Chunks the player never touched are not stored at all: the generator
 * rebuilds them from the seed, cell by cell, exactly as they were.
 * <p>
 * Both files are replaced in one step, see {@link AtomicNbtFile}, so a crash while
 * saving never leaves a half written file behind.
 */
public final class WorldSaver {

    private static final Logger LOGGER = LogManager.getLogger();

    private WorldSaver() {
        // Utility class: never instantiated.
    }

    /**
     * Stores a world.
     * <p>
     * The chunks are written first and the level file last. A failed save then
     * leaves the old level file in place, which still describes the world the
     * player knows, instead of a new one pointing at chunks that may not be there.
     * A chunk that fails to write keeps its changed flag, see
     * {@link World#persistModifiedChunks()}, so nothing is forgotten by a save that
     * did not go through.
     *
     * @param storage storage providing the folder
     * @param summary save game to write into
     * @param data level data of the world
     * @param world world holding the chunks
     * @param inventory inventory of the player
     * @return amount of chunks that were stored
     * @throws SaveException when a file cannot be written
     */
    public static int save(WorldStorage storage, SaveSummary summary, LevelData data, World world,
            PlayerInventory inventory) {
        storage.prepareFolder(summary);
        // Attached before anything is written: from now on a changed chunk may be
        // dropped from memory, because dropping it writes it first.
        world.attachChunkStore(new FileChunkStore(summary.folder()));

        int stored = world.persistModifiedChunks();

        NbtCompound root = data.write(inventory);
        // The entities travel with the level file: they are few and they belong to
        // the world, not to a chunk that may be dropped from memory.
        root.put(world.entities().save());
        AtomicNbtFile.write(root, summary.levelFile());

        LOGGER.info("Saved world '{}' with {} changed chunks and {} entities into {}",
                summary.displayName(), stored, world.entities().count(), summary.levelFile());
        return stored;
    }
}

