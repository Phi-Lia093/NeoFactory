package com.philia093.neofactory.world.save;

import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtIo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Replaces a file in one step.
 * <p>
 * Writing into the file itself would leave a half written file behind if the game
 * dies while saving, and the world would be gone. The data is written to a
 * temporary name next to the target instead and moved over it once it is complete,
 * so a reader either sees the old file or the new one.
 * <p>
 * The same two steps are needed for the level file and for a chunk file, which is
 * why they live here instead of in one of their callers.
 */
final class AtomicNbtFile {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Name of the file written before the complete one replaces it. */
    static final String TEMPORARY_SUFFIX = ".tmp";

    private AtomicNbtFile() {
        // Utility class: never instantiated.
    }

    /**
     * Writes a tag tree into a file, replacing whatever is there.
     *
     * @param root tag tree to store
     * @param target file to write
     * @throws SaveException when the file cannot be written
     */
    static void write(NbtCompound root, File target) {
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new SaveException("Unable to create the folder " + parent);
        }
        File temporary = new File(parent, target.getName() + TEMPORARY_SUFFIX);
        try {
            NbtIo.writeGzip(root, temporary);
        } catch (RuntimeException e) {
            LOGGER.error("Unable to write the save game {}", temporary, e);
            throw e instanceof SaveException ? (SaveException) e
                    : new SaveException("Unable to write " + temporary, e);
        }

        // Replacing the file in one step is what keeps a crash from destroying the
        // file that was already there.
        if (!temporary.renameTo(target) && !replaceByCopy(temporary, target)) {
            throw new SaveException("Unable to replace the save game " + target);
        }
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
