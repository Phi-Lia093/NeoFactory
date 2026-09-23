package com.philia093.neofactory.world.save;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.blockentity.BlockEntity;
import com.philia093.neofactory.blockentity.BlockEntityRegistry;
import com.philia093.neofactory.blockentity.BlockEntityType;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.util.nbt.NbtByteArray;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtList;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.Section;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Converts a {@link Chunk} to tag data and back.
 * <p>
 * A chunk is stored whole instead of storing only the blocks the player changed.
 * That keeps the file readable: a chunk that was written, read and written again
 * holds exactly the same cells. Only chunks the player changed are written at all,
 * and each of them goes into its own file, see {@link ChunkStorage}.
 * <p>
 * <b>A chunk is stored as the sections it is built from.</b> The flat engine wrote two layers of
 * sixteen by sixteen cells and the file knew nothing about a height at all. A world of cubes is a
 * column of two hundred and fifty six blocks, and only the sections that really hold something are
 * written, so the sky above a landscape costs the file nothing - the same rule an empty
 * {@link Section} follows in memory.
 * <p>
 * Three things have to travel with the blocks:
 * <ul>
 *     <li>the block id of every cell of every section that carries one, packed into a palette, see
 *         {@link PackedValues}</li>
 *     <li>the state of those cells, see {@link Section#state(int, int, int)}</li>
 *     <li>the generated-cell bitmap and the decorated flag</li>
 * </ul>
 * The last point is the one that is easy to forget: the generator only creates
 * the ground of cells that are already generated, so without that bitmap a chunk
 * would look half empty after loading and the generator would fill it in again,
 * possibly burying what the player built there.
 * <p>
 * <b>Light is not stored.</b> The two light maps of a section are what a light engine computes from
 * the blocks around them, so a chunk arrives dark and is lit again; the maps are left out of the file
 * on purpose, see {@link Section#clearLight()}.
 * <p>
 * A block id and a state are full 32 bit numbers, so the game never runs out of
 * room for the blocks a factory needs, and the palette of a section is what keeps
 * the file small. The version of the format travels with every
 * file and is checked while reading: a chunk written by another version is refused
 * instead of guessed at.
 */
public final class ChunkCodec {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Name of the tag holding the chunk X coordinate. */
    static final String TAG_X = "X";

    /** Name of the tag holding the chunk Z coordinate, the second horizontal axis of the world. */
    static final String TAG_Z = "Z";

    /** Name of the tag holding the version of the format the file was written in. */
    static final String TAG_VERSION = SaveTags.DATA_VERSION;

    /** Name of the list holding one entry per section of the column that carries something. */
    static final String TAG_SECTIONS = "Sections";

    /** Name of the tag holding the index of a section along the vertical axis of its chunk. */
    static final String TAG_SECTION_Y = "Y";

    /** Name of the compound holding the block id of every cell of a section. */
    static final String TAG_BLOCKS = "Blocks";

    /** Name of the compound holding the state of every cell of a section. */
    static final String TAG_STATES = "States";

    /** Name of the tag holding the generated-cell bitmap. */
    static final String TAG_GENERATED = "Generated";

    /** Name of the tag holding whether the decorations were planted. */
    static final String TAG_DECORATED = "Decorated";

    /** Amount of bytes the generated-cell bitmap needs, one per column. */
    static final int GENERATED_BYTES = Chunk.CELL_COUNT;

    private ChunkCodec() {
        // Utility class: never instantiated.
    }

    /**
     * Packs a chunk into a compound.
     *
     * @param chunk chunk to store
     * @return the tag data of that chunk
     */
    public static NbtCompound write(Chunk chunk) {
        NbtCompound compound = new NbtCompound("");
        compound.putInt(TAG_VERSION, SaveFormat.DATA_VERSION);
        compound.putInt(TAG_X, chunk.chunkX());
        compound.putInt(TAG_Z, chunk.chunkZ());
        compound.put(writeSections(chunk));
        compound.put(new NbtByteArray(TAG_GENERATED, generatedColumns(chunk)));
        compound.put(writeBlockEntities(chunk));
        compound.putBoolean(TAG_DECORATED, chunk.isDecorated());
        return compound;
    }

    /**
     * Packs the sections of a chunk that carry a block into a list.
     * <p>
     * A section that holds nothing but air is not written at all: it has no array in memory either,
     * see {@link Chunk#isEmptySection(int)}, and a file that listed the sky above a landscape would
     * grow with the height of the world instead of with what stands in it.
     *
     * @param chunk chunk to store
     * @return the list of sections, ordered the way they stand in the world
     */
    private static NbtList writeSections(Chunk chunk) {
        NbtList list = new NbtList(TAG_SECTIONS);
        int[] blocks = new int[Section.VOLUME];
        int[] states = new int[Section.VOLUME];
        for (int sectionY = 0; sectionY < Constants.SECTION_COUNT; sectionY++) {
            if (chunk.isEmptySection(sectionY)) {
                continue;
            }
            fillArrays(chunk.section(sectionY), blocks, states);

            NbtCompound entry = new NbtCompound("");
            entry.putInt(TAG_SECTION_Y, sectionY);
            entry.put(PackedValues.write(TAG_BLOCKS, blocks));
            entry.put(PackedValues.write(TAG_STATES, states));
            list.add(entry);
        }
        return list;
    }

    /**
     * Reads the blocks and the states of one section into two arrays.
     * <p>
     * The cells are walked in the order {@link Section#index(int, int, int)} names them, which is the
     * order a stored array is read back in as well: the two have to agree, or a section would come
     * back with its corners swapped.
     *
     * @param section section to read
     * @param blocks array receiving the block id of every cell
     * @param states array receiving the state of every cell
     */
    private static void fillArrays(Section section, int[] blocks, int[] states) {
        int cell = 0;
        for (int y = 0; y < Section.SIZE; y++) {
            for (int z = 0; z < Section.SIZE; z++) {
                for (int x = 0; x < Section.SIZE; x++) {
                    blocks[cell] = section.rawId(x, y, z);
                    states[cell] = section.state(x, y, z);
                    cell++;
                }
            }
        }
    }

    /**
     * Packs the columns of a chunk the generator already filled.
     *
     * @param chunk chunk to store
     * @return one flag per column, {@code 1} for a column the generator wrote
     */
    private static byte[] generatedColumns(Chunk chunk) {
        byte[] generated = new byte[GENERATED_BYTES];
        for (int localZ = 0; localZ < Constants.CHUNK_SIZE; localZ++) {
            for (int localX = 0; localX < Constants.CHUNK_SIZE; localX++) {
                generated[localZ * Constants.CHUNK_SIZE + localX] =
                        (byte) (chunk.isCellGenerated(localX, localZ) ? 1 : 0);
            }
        }
        return generated;
    }

    /**
     * Packs the block entities of a chunk into a list.
     * <p>
     * Every entry names its type and its cell, so a reader knows where to put the entity
     * back and what to create for it. The two numbers a cell holds - the id of the block
     * and its state - stay in the arrays above; this list is what a machine adds to them.
     *
     * @param chunk chunk to store
     * @return the list of block entities, empty when the chunk holds none
     */
    private static NbtList writeBlockEntities(Chunk chunk) {
        NbtList list = new NbtList(SaveTags.BLOCK_ENTITIES);
        for (BlockEntity entity : chunk.blockEntities()) {
            NbtCompound entry = new NbtCompound("");
            entry.putInt(SaveTags.BLOCK_ENTITY_X, Chunk.localOf(entity.x()));
            entry.putInt(SaveTags.BLOCK_ENTITY_Y, entity.y());
            entry.putInt(SaveTags.BLOCK_ENTITY_Z, Chunk.localOf(entity.z()));
            entry.putString(SaveTags.BLOCK_ENTITY_ID, entity.type().name());
            NbtCompound data = new NbtCompound(SaveTags.DATA);
            entity.writeData(data);
            entry.put(data);
            list.add(entry);
        }
        return list;
    }

    /**
     * Fills the block entities of a chunk.
     * <p>
     * An entry whose type is unknown, whose cell lies outside the chunk or whose block
     * does not carry that kind of entity is reported and skipped: a chunk that holds a
     * machine the game no longer knows still opens, without a machine that would run
     * where no block is.
     *
     * @param chunk chunk to fill, its blocks are already read
     * @param list list written by {@link #writeBlockEntities(Chunk)}, may be {@code null}
     */
    private static void readBlockEntities(Chunk chunk, NbtList list) {
        if (list == null) {
            return;
        }
        for (int index = 0; index < list.size(); index++) {
            NbtCompound entry = list.getCompound(index);
            String typeName = entry.getString(SaveTags.BLOCK_ENTITY_ID, "");
            BlockEntityType type = BlockEntityRegistry.byName(typeName);
            if (type == null) {
                LOGGER.warn("Chunk ({}, {}) holds an unknown block entity '{}' and it is skipped",
                        chunk.chunkX(), chunk.chunkZ(), typeName);
                continue;
            }
            int localX = entry.getInt(SaveTags.BLOCK_ENTITY_X, -1);
            int blockY = entry.getInt(SaveTags.BLOCK_ENTITY_Y, -1);
            int localZ = entry.getInt(SaveTags.BLOCK_ENTITY_Z, -1);
            // A block entity that is stored outside the cells of its chunk names nothing it could
            // stand on: the entry is reported and skipped instead of being placed somewhere else.
            if (!Chunk.contains(localX, localZ)
                    || blockY < Constants.MIN_Y || blockY > Constants.MAX_Y) {
                LOGGER.warn("Chunk ({}, {}) holds a block entity outside its cells at ({}, {}, {})",
                        chunk.chunkX(), chunk.chunkZ(), localX, blockY, localZ);
                continue;
            }
            Block block = chunk.getBlock(localX, blockY, localZ);
            if (!type.name().equals(block.blockEntityTypeName())) {
                LOGGER.warn("Chunk ({}, {}) holds a '{}' at ({}, {}, {}) but the block there is"
                        + " '{}', the entity is dropped", chunk.chunkX(), chunk.chunkZ(), typeName,
                        localX, blockY, localZ, block.name());
                continue;
            }
            BlockEntity entity = type.create();
            entity.setPosition(chunk.originX() + localX, blockY, chunk.originZ() + localZ);
            NbtCompound data = entry.getCompound(SaveTags.DATA);
            entity.readData(data == null ? new NbtCompound(SaveTags.DATA) : data);
            chunk.setBlockEntity(entity);
        }
    }

    /**
     * Fills a chunk from a compound written by {@link #write(Chunk)}.
     * <p>
     * The chunk has to be empty, because the reader replaces every cell instead of
     * merging into it. Every section of the file lands in the section of the column it was taken
     * from, so a cell keeps the height it was stored at, and the "generated" flags are restored as
     * they were, so the generator never touches the chunk again.
     *
     * @param chunk chunk to fill, must be empty
     * @param compound tag data of that chunk
     * @return {@code true} when the chunk was filled
     * @throws SaveException when the tag data does not describe a chunk of this format
     */
    public static boolean read(Chunk chunk, NbtCompound compound) {
        int version = compound.getInt(TAG_VERSION, 0);
        if (version != SaveFormat.DATA_VERSION) {
            throw new SaveException("Chunk (" + chunk.chunkX() + ", " + chunk.chunkZ()
                    + ") was written by save format " + version + ", this build reads format "
                    + SaveFormat.DATA_VERSION);
        }
        NbtByteArray generated = compound.getByteArray(TAG_GENERATED);
        if (generated == null || generated.length() != GENERATED_BYTES) {
            throw new SaveException("Chunk (" + chunk.chunkX() + ", " + chunk.chunkZ()
                    + ") holds " + (generated == null ? "no" : generated.length())
                    + " generated flags instead of " + GENERATED_BYTES);
        }

        readSections(chunk, compound.getList(TAG_SECTIONS));
        readBlockEntities(chunk, compound.getList(SaveTags.BLOCK_ENTITIES));
        if (compound.getBoolean(TAG_DECORATED, false)) {
            chunk.markDecorated();
        }
        for (int localZ = 0; localZ < Constants.CHUNK_SIZE; localZ++) {
            for (int localX = 0; localX < Constants.CHUNK_SIZE; localX++) {
                if (generated.get(localZ * Constants.CHUNK_SIZE + localX) != 0) {
                    chunk.markCellGenerated(localX, localZ);
                }
            }
        }
        // The chunk was just written, nothing has to be redrawn because of it, and
        // the file is now the authority, so the player-change flag is cleared too.
        chunk.clearDirty();
        chunk.clearModified();
        return true;
    }

    /**
     * Fills the sections of a chunk from a list written by {@link #writeSections(Chunk)}.
     * <p>
     * The arrays of a section are read back into the very cells they were taken from, and the height
     * of the section is added to the local Y of a cell, so a section that stands high up in the world
     * lands where it belongs. An id the game does not know becomes air, the same way a block of a game
     * version that is gone would, and a state is written after its block, because writing a block
     * clears the state of the cell, see {@link Chunk#setRawId(int, int, int, int)}.
     *
     * @param chunk chunk to fill
     * @param list list of sections, {@code null} when the file holds none
     * @throws SaveException when an entry does not describe a section of this format
     */
    private static void readSections(Chunk chunk, NbtList list) {
        if (list == null) {
            throw new SaveException("Chunk (" + chunk.chunkX() + ", " + chunk.chunkZ()
                    + ") holds no sections");
        }
        for (int index = 0; index < list.size(); index++) {
            NbtCompound entry = list.getCompound(index);
            int sectionY = entry.getInt(TAG_SECTION_Y, -1);
            if (sectionY < 0 || sectionY >= Constants.SECTION_COUNT) {
                throw new SaveException("Chunk (" + chunk.chunkX() + ", " + chunk.chunkZ()
                        + ") holds a section at " + sectionY + ", the world carries 0 to "
                        + (Constants.SECTION_COUNT - 1));
            }
            int[] blocks = PackedValues.read(entry.getCompound(TAG_BLOCKS), Section.VOLUME);
            int[] states = PackedValues.read(entry.getCompound(TAG_STATES), Section.VOLUME);

            int originY = Constants.MIN_Y + sectionY * Section.SIZE;
            int cell = 0;
            for (int y = 0; y < Section.SIZE; y++) {
                for (int z = 0; z < Section.SIZE; z++) {
                    for (int x = 0; x < Section.SIZE; x++) {
                        Block block = BlockRegistry.byId(blocks[cell]);
                        if (block.id() != Blocks.AIR_ID) {
                            chunk.setRawId(x, originY + y, z, block.id());
                        }
                        if (states[cell] != 0) {
                            chunk.setState(x, originY + y, z, states[cell]);
                        }
                        cell++;
                    }
                }
            }
        }
    }
}
