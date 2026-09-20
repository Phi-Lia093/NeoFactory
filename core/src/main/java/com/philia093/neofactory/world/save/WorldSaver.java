package com.philia093.neofactory.world.save;

import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtIo;
import com.philia093.neofactory.util.nbt.NbtList;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Collection;

/**
 * Writes a world into its save game.
 * <p>
 * The file is written to a temporary name first and moved over the old file once it
 * is complete. A crash while saving therefore never leaves a half written file
 * behind: either the old save game is still there or the new one is.
 * <p>
 * Every chunk the world currently holds in memory is stored whole, see
 * {@link ChunkCodec}. Chunks that were never loaded are not part of the file and
 * are generated from the seed again when the player reaches them, which is what
 * keeps a save game small.
 */
public final class WorldSaver {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Name of the file written before the complete one replaces it. */
    private static final String TEMPORARY_SUFFIX = ".tmp";

    private WorldSaver() {
        // Utility class: never instantiated.
    }

    /**
     * Stores a world.
     *
     * @param storage storage providing the folder
     * @param summary save game to write into
     * @param data level data of the world
     * @param world world holding the chunks
     * @param inventory inventory of the player
     * @return amount of chunks that were stored
     * @throws SaveException when the file cannot be written
     */
    public static int save(WorldStorage storage, SaveSummary summary, LevelData data, World world,
            PlayerInventory inventory) {
        storage.prepareFolder(summary);

        NbtCompound root = data.write(inventory);
        Collection<Chunk> chunks = world.chunks();
        NbtList stored = new NbtList(SaveTags.CHUNKS);
        for (Chunk chunk : chunks) {
            stored.add(ChunkCodec.write(chunk));
        }
        root.put(stored);

        File target = summary.levelFile();
        File temporary = new File(target.getParentFile(), target.getName() + TEMPORARY_SUFFIX);
        try {
            NbtIo.writeGzip(root, temporary);
        } catch (RuntimeException e) {
            LOGGER.error("Unable to write the save game {}", temporary, e);
            throw e instanceof SaveException ? (SaveException) e
                    : new SaveException("Unable to write " + temporary, e);
        }

        // Replacing the file in one step is what keeps a crash from destroying the
        // save game that was already there.
        if (!temporary.renameTo(target) && !replaceByCopy(temporary, target)) {
            throw new SaveException("Unable to replace the save game " + target);
        }

        LOGGER.info("Saved world '{}' with {} chunks into {}", summary.displayName(),
                stored.size(), target);
        return stored.size();
    }

    /**
     * Copies the temporary file over the old one when a rename is refused.
     * <p>
     * Windows refuses to rename a file onto an existing one, so the old file is
     * removed first. The window in which neither file exists is a single system call
     * wide, which is the best that can be done without a real rename.
     *
     * @param temporary file holding the new data
     * @param target file to replace
     * @return {@code true} when the target holds the new data
     */
    private static boolean replaceByCopy(File temporary, File target) {
        if (target.exists() && !target.delete()) {
            LOGGER.error("Unable to remove the old save game {}", target);
            return false;
        }
        if (temporary.renameTo(target)) {
            return true;
        }
        LOGGER.error("Unable to move {} onto {}", temporary, target);
        return false;
    }
}
