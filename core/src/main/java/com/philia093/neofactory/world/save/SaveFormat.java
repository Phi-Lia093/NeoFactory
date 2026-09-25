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
     * <p>
     * <b>What version 2 changed.</b> A chunk is no longer two flat layers of sixteen by sixteen
     * cells: it is a column of {@link com.philia093.neofactory.world.Section sections} of sixteen
     * blocks on every side, stored one entry per section that carries something, each with its block
     * ids and its states packed into a palette. A world of version 1 therefore cannot be read any
     * more - its cells live in two layers that a world of cubes knows nothing about - and the game
     * says so instead of filling the world with blocks that were never there.
     * <p>
     * <b>What version 3 changed.</b> The level file names the spawn and the player by X and Z, so a
     * tag that still carries the second horizontal axis under the name of a height is refused instead of
     * being read as one. A height is not stored at all: a body of the flat view stands on the surface of
     * its column, which the world reports, see {@code World#surfaceY(int, int)}.
     * <p>
     * <b>What version 4 changed.</b> The game has no fluid block any more. Water and lava used to be
     * blocks a chunk could hold and a fluid ran through the world as a body of its own; a fluid is a
     * material of the industry now, carried by a tank, a bucket or a cell, and the ground of a world is
     * dry everywhere. A chunk of version 3 names block ids that no longer exist - the water and the lava
     * of the landscape - so it is refused instead of loading a world with holes where its sea was.
     * <p>
     * <b>What version 5 changed.</b> A stack carries the damage a tool has taken, so a pickaxe wears out
     * over a save game as well, see {@code SaveTags#DAMAGE}. At the same time the eight pieces of armour
     * left the game: the numbers they held are free now, and a stored inventory that names one of them is
     * read as an empty slot, see {@code Items#ARMOUR_ID_FROM}.
     */
    public static final int DATA_VERSION = 5;

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
