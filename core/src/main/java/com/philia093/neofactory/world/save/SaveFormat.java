package com.philia093.neofactory.world.save;

/**
 * Constants shared by everything that reads or writes a stored world.
 * <p>
 * The version is written into every file and checked while reading. It is the
 * handle for later changes of the format: a reader that finds a version it does
 * not know can either convert the data or refuse the file, instead of guessing.
 */
public final class SaveFormat {

    /**
     * Version of the save format written by this build.
     * <p>
     * The game is still being built, so a save game is not carried over between
     * versions: the layout grows as the factory systems arrive and a file of another
     * version is refused instead of guessed at, see
     * {@link LevelData} and {@link ChunkCodec}. Every change of the layout bumps
     * this number.
     */
    public static final int DATA_VERSION = 1;

    /** Name of the root tag of a stored world. */
    public static final String ROOT_TAG = "NeoFactory";

    /** Name of the file holding a stored world. */
    public static final String LEVEL_FILE = "level.dat";

    /** Folder below a save game holding one file per changed chunk. */
    public static final String CHUNK_FOLDER = "chunks";

    /** Folder holding every save game, relative to the working directory. */
    public static final String SAVES_FOLDER = "saves";

    /** Prefix of the folder of a single save game. */
    public static final String SAVE_FOLDER_PREFIX = "save_";

    /** Highest amount of characters a world name may hold. */
    public static final int MAX_NAME_LENGTH = 32;

    /** Name used when the player did not enter one. */
    public static final String DEFAULT_NAME = "New World";

    private SaveFormat() {
        // Utility class: never instantiated.
    }
}
