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
     * material of the industry now, carried by a tank or a cell, and the ground of a world is
     * dry everywhere. A chunk of version 3 names block ids that no longer exist - the water and the lava
     * of the landscape - so it is refused instead of loading a world with holes where its sea was.
     * <p>
     * <b>What version 5 changed.</b> A stack carries the damage a tool has taken, so a pickaxe wears out
     * over a save game as well, see {@code SaveTags#DAMAGE}. At the same time the eight pieces of armour
     * left the game: the numbers they held are free now, and a stored inventory that names one of them is
     * read as an empty slot, see {@code Items#ARMOUR_ID_FROM}.
     * <p>
     * <b>What version 6 changed.</b> The game has one container and no bucket. The empty bucket, the
     * bucket of water and the bucket of lava left the game, so the item ids 67, 68 and 69 name nothing
     * any more and a stored inventory that holds one of them is read as an empty slot, see
     * {@code Items#FLUID_CELL_ID}. Every fluid travels in a cell now, and the cells kept their numbers:
     * 70, 71 and 72 are still the cell, the water cell and the lava cell.
     * <p>
     * <b>What version 7 changed.</b> The industry arrived: steam is a fluid of the game and travels in a
     * cell like water and lava, see {@code Fluids#STEAM}. Its cell is the first item written down after
     * the cells of the two fluids of the world, so it takes the first free number, see
     * {@code Items#NEXT_FREE_ID}.
     * <p>
     * <b>What version 8 changed.</b> The items of the industry take the numbers 86 to 99 - the cell of
     * steam, the bronze boiler and the machines, pipes and tools that follow - and the materials of the
     * game stand behind that run, so every item of every material moved fourteen numbers up. A stored
     * inventory of an older version therefore names the wrong items and is refused instead of being read,
     * see {@code Items#BRONZE_BOILER_ID}.
     * <p>
     * <b>What version 10 changed.</b> The twenty eight pipes of the game took the numbers 100 to 127, which
     * is more than the window of the industry had kept free, and bronze and steel joined the materials as
     * well, so every item of every material moved another twenty eight numbers up: what version 8 said
     * about a stored inventory holds here word for word, see {@code Items#PIPE_ID_FROM}.
     * <p>
     * <b>What version 11 changed.</b> Not every material is made in every size any more: wood comes as a
     * small, a medium and a large pipe and as no bundle, so the run of the pipes holds twenty four of them
     * instead of twenty eight and the pipes behind wood moved four numbers down - what stood for a wooden
     * tiny pipe in an older save game names a copper one there. The items of the materials stand behind the
     * window the pipes are given and kept their numbers, see {@code PipeMaterials} and
     * {@code Items#PIPE_ID_FROM}.
     * <b>What version 12 changed.</b> The line of the industry grew from four materials to twenty five: the
     * pipes of wood, copper, bronze and steel kept their numbers, every other material was appended after
     * them, and the materials themselves came with their items - a hundred and twenty one pipes more than
     * version 11 held, so the window the pipes are given reaches from 100 to 244 and every item of every
     * material stands a hundred and seventeen numbers higher than before, see {@code PipeMaterials} and
     * {@code Materials}.
     * <p>
     * <b>What version 13 changed.</b> The game has containers. A chest is a block that keeps what a player
     * puts in it - block id 187, item id 258 - and it names a block entity, {@code chest}, that no older
     * build knows: such a build would report and skip the entry and open a chest empty while the items in it
     * were still written down. <b>Its item took the number the run of the materials used to start at</b>, so
     * that run begins one number higher and every stored inventory of version 12 names the wrong items for
     * every material - which is the second reason this version refuses it. The block id cost nothing: block
     * ids are written down one by one and no run of them grows in the middle, see {@code Blocks#CHEST_ID}.
     * <p>
     * <b>What versions 14 and 15 changed.</b> The line of the power arrived: the cables of the industry took
     * the block numbers 188 to 727 and the item numbers 304 to 843, which is the run the items of the
     * materials of the game used to start at. Every item of every material therefore stands five hundred and
     * forty numbers higher than it did and a stored inventory of an older version names the wrong items for
     * every material - what version 8 said about a stored inventory holds here word for word, see
     * {@code Cables}. The first of the two steps held the single line of every material, the second added the
     * five wider widths and the two kinds, a line with a skin and one without.
     * <p>
     * <b>What version 16 changed.</b> The three steam turbines - the first machines of the game that make
     * power - took the block numbers 728 to 730 and the item numbers 844 to 846. The blocks cost nothing: they
     * stand behind the cables and no block of the game moved. <b>The items are what refuses this version</b>,
     * because those three numbers are where the items of the materials of the game began: every item of every
     * material stands three numbers higher than it did and a stored inventory of version 15 names the wrong
     * items for every material - what version 8 said about a stored inventory holds here word for word, see
     * {@code Items#STEAM_TURBINE_LV_ID}.
     * <p>
     * <b>What version 17 changed.</b> The sides of a machine are words a player reads at the machine now - the
     * right flank, the left one, the back, the ceiling and the floor - and no longer sides of the world: the
     * group a machine stores its sides in holds {@code RIGHT} where it held {@code WEST}, so the sides of a
     * machine travel with it when it is turned and its front can never carry a job, see
     * {@code FaceConfig#save(NbtCompound)}. Nothing else of the format moved: the words stand under the very
     * keys they always did, and a world of version 16 is refused for the one reason that its words name sides
     * of the world, which no machine of this game reads.
     * <p>
     * <b>What version 18 changed.</b> The age of the battery arrived: the fifteen cells of {@code Batteries} and
     * the twelve boxes of {@code BatteryBoxes} took the numbers the items of the materials of the game used to
     * start at, so <b>every item of every material stands forty two numbers higher than it did</b>. That is the
     * same reason version 16 refuses a world of version 15 - what version 8 said about a stored inventory holds
     * here word for word - and it is the only thing this version moved: the blocks of the boxes went behind the
     * eighteen machines of the line, so no block of the game moved at all, see {@code Items#BATTERY_BOX_FIRST_ID}
     * and {@code Blocks#BATTERY_BOX_FIRST_ID}. A world of version 17 is therefore refused for the stored items
     * of its materials and not for its blocks.
     */
    public static final int DATA_VERSION = 18;

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
