package com.philia093.neofactory.world.save;

import java.io.File;

/**
 * Everything the world list needs to know about a stored world, without opening
 * it.
 * <p>
 * The name of a save game lives inside its file, the folder only carries an
 * identifier, which is why renaming a world never has to touch the file system.
 *
 * @param id identifier of the save game, also the name of its folder
 * @param folder folder holding the save game
 * @param displayName name shown to the player
 * @param seed seed the world was generated from
 * @param lastPlayed time the world was played at, in milliseconds since 1970
 * @param created time the world was created at, in milliseconds since 1970
 * @param playedMillis time played in this world, in milliseconds
 * @param dataVersion version of the save format
 * @param sizeBytes size of the stored file
 */
public record SaveSummary(String id, File folder, String displayName, int seed, long lastPlayed,
        long created, long playedMillis, int dataVersion, long sizeBytes) {

    /**
     * Creates a summary for a world that was just created.
     *
     * @param id identifier of the save game
     * @param folder folder of the save game
     * @param displayName name shown to the player
     * @param seed seed of the world
     * @return the summary, without a size yet
     */
    public static SaveSummary created(String id, File folder, String displayName, int seed) {
        long now = System.currentTimeMillis();
        return new SaveSummary(id, folder, displayName, seed, now, now, 0L,
                SaveFormat.DATA_VERSION, 0L);
    }

    /** File the world data is stored in. */
    public File levelFile() {
        return new File(folder, SaveFormat.LEVEL_FILE);
    }
}
