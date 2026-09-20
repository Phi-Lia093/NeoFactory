package com.philia093.neofactory.world.save;

import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtException;
import com.philia093.neofactory.util.nbt.NbtIo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Finds, creates and removes the folders a save game lives in.
 * <p>
 * Every world owns one folder below {@link SaveFormat#SAVES_FOLDER}, named after
 * its identifier, holding a single {@link SaveFormat#LEVEL_FILE}. The readable
 * name of a world is stored inside that file, so renaming never has to move a
 * folder around.
 * <p>
 * Listing a world reads only the header of its file. A world whose file cannot be
 * read is reported and skipped instead of taking the whole list down with it, which
 * is what keeps one damaged save game from making the game unplayable.
 */
public class WorldStorage {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Pattern used to name the backup of a file that cannot be read. */
    private static final String BACKUP_PATTERN = "yyyyMMdd-HHmmss";

    /** Folder holding every save game. */
    private final File savesFolder;

    /** Creates storage below the working directory. */
    public WorldStorage() {
        this(new File(SaveFormat.SAVES_FOLDER));
    }

    /**
     * Creates storage below a folder.
     *
     * @param savesFolder folder holding the save games
     */
    public WorldStorage(File savesFolder) {
        this.savesFolder = savesFolder;
    }

    /** Folder holding every save game. */
    public File savesFolder() {
        return savesFolder;
    }

    /**
     * Lists every readable save game, most recently played first.
     *
     * @return the save games, never {@code null}
     */
    public List<SaveSummary> list() {
        List<SaveSummary> summaries = new ArrayList<>();
        File[] folders = savesFolder.listFiles(File::isDirectory);
        if (folders == null) {
            return summaries;
        }
        for (File folder : folders) {
            SaveSummary summary = readSummary(folder);
            if (summary != null) {
                summaries.add(summary);
            }
        }
        summaries.sort(Comparator.comparingLong(SaveSummary::lastPlayed).reversed());
        return summaries;
    }

    /**
     * Reads the header of a save game.
     *
     * @param folder folder of the save game
     * @return the summary, or {@code null} when the file cannot be read
     */
    public SaveSummary readSummary(File folder) {
        File level = new File(folder, SaveFormat.LEVEL_FILE);
        if (!level.isFile()) {
            // A folder without data is not a save game, only leftover of a copy.
            return null;
        }
        try {
            NbtCompound root = (NbtCompound) NbtIo.readGzip(level);
            return new SaveSummary(folder.getName(), folder,
                    root.getString(SaveTags.WORLD_NAME, folder.getName()),
                    root.getInt(SaveTags.SEED, 0),
                    root.getLong(SaveTags.LAST_PLAYED, level.lastModified()),
                    root.getLong(SaveTags.CREATED, level.lastModified()),
                    root.getLong(SaveTags.PLAYED_MILLIS, 0L),
                    root.getInt(SaveTags.DATA_VERSION, 0),
                    level.length());
        } catch (NbtException | ClassCastException e) {
            LOGGER.error("Save game {} cannot be read and is hidden from the list", folder, e);
            return null;
        }
    }

    /**
     * Creates the folder of a new save game.
     *
     * @param displayName requested name of the world
     * @param seed seed of the world
     * @param spawnX block X coordinate of the spawn
     * @param spawnY block Y coordinate of the spawn
     * @return the created save game
     * @throws SaveException when the folder cannot be created
     */
    public SaveSummary create(String displayName, int seed, int spawnX, int spawnY) {
        Set<String> takenIds = new HashSet<>();
        Set<String> takenNames = new HashSet<>();
        for (SaveSummary summary : list()) {
            takenIds.add(summary.id());
            takenNames.add(summary.displayName());
        }

        String id = SaveNames.nextId(takenIds);
        File folder = folderOf(id);
        if (!folder.mkdirs()) {
            throw new SaveException("Unable to create the folder of a new world: " + folder);
        }
        SaveSummary summary = SaveSummary.created(id, folder,
                SaveNames.uniqueName(displayName, takenNames), seed);
        LOGGER.info("Created folder {} for the new world '{}'", folder, summary.displayName());
        return summary;
    }

    /**
     * Removes a save game, its file and its folder.
     *
     * @param summary save game to remove
     * @return {@code true} when everything was removed
     */
    public boolean delete(SaveSummary summary) {
        File[] files = summary.folder().listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.delete()) {
                    LOGGER.warn("Unable to delete {}", file);
                    return false;
                }
            }
        }
        boolean removed = summary.folder().delete();
        LOGGER.info("Deleted world '{}': {}", summary.displayName(),
                removed ? "done" : "the folder is still there");
        return removed;
    }

    /**
     * Folder of a save game.
     *
     * @param id identifier of the save game
     * @return the folder, whether it exists or not
     */
    public File folderOf(String id) {
        return new File(savesFolder, SaveFormat.SAVE_FOLDER_PREFIX + id);
    }

    /**
     * Moves a file that cannot be read out of the way.
     * <p>
     * A damaged file is never deleted and never overwritten: the next save would
     * destroy whatever is still inside it. It is renamed instead, so the player keeps
     * it and the world can be started over.
     *
     * @param file file to move away
     * @return the new location, or {@code null} when moving failed
     */
    public File backupCorrupt(File file) {
        String stamp = new SimpleDateFormat(BACKUP_PATTERN, Locale.ROOT).format(new Date());
        File backup = new File(file.getParentFile(), file.getName() + ".corrupt-" + stamp);
        try {
            Path moved = Files.move(file.toPath(), backup.toPath());
            LOGGER.warn("Damaged save game moved to {}", moved);
            return moved.toFile();
        } catch (IOException e) {
            LOGGER.error("Unable to move the damaged save game {} away", file, e);
            return null;
        }
    }

    /**
     * Returns the folder of a save game, creating it when it is missing.
     *
     * @param summary save game to prepare
     * @return the folder
     * @throws SaveException when the folder cannot be created
     */
    public File prepareFolder(SaveSummary summary) {
        File folder = summary.folder();
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new SaveException("Unable to create the folder of the world: " + folder);
        }
        return folder;
    }

    /**
     * Writes a new name into a stored world.
     * <p>
     * Only the header of the file is touched, the chunks are copied unchanged. That
     * is what makes a rename instant even for a large world, and it is why the name
     * lives inside the file instead of being the name of the folder.
     *
     * @param summary world to rename
     * @param name requested name, cleaned by {@link SaveNames#sanitize(String)}
     * @return a summary carrying the new name
     * @throws SaveException when the file cannot be read or written
     */
    public SaveSummary rename(SaveSummary summary, String name) {
        File level = summary.levelFile();
        try {
            NbtCompound root = (NbtCompound) NbtIo.readGzip(level);
            String cleaned = SaveNames.sanitize(name);
            root.putString(SaveTags.WORLD_NAME, cleaned);
            NbtIo.writeGzip(root, level);
            LOGGER.info("Renamed world '{}' to '{}'", summary.displayName(), cleaned);
            return new SaveSummary(summary.id(), summary.folder(), cleaned, summary.seed(),
                    summary.lastPlayed(), summary.created(), summary.playedMillis(),
                    summary.dataVersion(), level.length());
        } catch (NbtException | ClassCastException e) {
            backupCorrupt(level);
            throw new SaveException("World '" + summary.displayName() + "' cannot be renamed, "
                    + "the damaged file was moved aside", e);
        }
    }
}
