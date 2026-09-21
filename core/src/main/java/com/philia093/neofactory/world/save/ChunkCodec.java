package com.philia093.neofactory.world.save;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.blockentity.BlockEntity;
import com.philia093.neofactory.blockentity.BlockEntityRegistry;
import com.philia093.neofactory.blockentity.BlockEntityType;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.util.nbt.NbtByteArray;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtIntArray;
import com.philia093.neofactory.util.nbt.NbtList;
import com.philia093.neofactory.world.Chunk;
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
 * Three things have to travel with the blocks:
 * <ul>
 *     <li>the block data of both layers, in the fixed order
 *         {@link Chunk#LAYER_FLOOR} then {@link Chunk#LAYER_OBJECT}</li>
 *     <li>the state of both layers, see {@link Chunk#meta(int, int, int)}</li>
 *     <li>the generated-cell bitmap and the decorated flag</li>
 * </ul>
 * The last point is the one that is easy to forget: the generator only creates
 * the ground of cells that are already generated, so without that bitmap a chunk
 * would look half empty after loading and the generator would fill it in again,
 * possibly burying what the player built there.
 * <p>
 * A block id and a state are full 32 bit numbers, so the game never runs out of
 * room for the blocks a factory needs. The version of the format travels with every
 * file and is checked while reading: a chunk written by another version is refused
 * instead of guessed at.
 */
public final class ChunkCodec {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Name of the tag holding the chunk X coordinate. */
    static final String TAG_X = "X";

    /** Name of the tag holding the chunk Y coordinate. */
    static final String TAG_Y = "Y";

    /** Name of the tag holding the version of the format the file was written in. */
    static final String TAG_VERSION = SaveTags.DATA_VERSION;

    /** Name of the tag holding the block id of every cell of both layers. */
    static final String TAG_BLOCKS = "Blocks";

    /** Name of the tag holding the state of every cell of both layers. */
    static final String TAG_META = "Meta";

    /** Name of the tag holding the generated-cell bitmap. */
    static final String TAG_GENERATED = "Generated";

    /** Name of the tag holding whether the decorations were planted. */
    static final String TAG_DECORATED = "Decorated";

    /** Amount of numbers the block data and the state of a chunk need. */
    static final int CELL_VALUES = Chunk.CELL_COUNT * Chunk.LAYERS;

    /** Amount of bytes the generated-cell bitmap needs, one per cell. */
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
        int[] blocks = new int[CELL_VALUES];
        int[] meta = new int[CELL_VALUES];
        byte[] generated = new byte[GENERATED_BYTES];
        int index = 0;
        for (int layer = 0; layer < Chunk.LAYERS; layer++) {
            for (int localY = 0; localY < Constants.CHUNK_SIZE; localY++) {
                for (int localX = 0; localX < Constants.CHUNK_SIZE; localX++) {
                    blocks[index] = chunk.rawId(localX, localY, layer);
                    meta[index] = chunk.meta(localX, localY, layer);
                    index++;
                    if (layer == Chunk.LAYER_FLOOR) {
                        int cell = localY * Constants.CHUNK_SIZE + localX;
                        generated[cell] = (byte) (chunk.isCellGenerated(localX, localY) ? 1 : 0);
                    }
                }
            }
        }

        NbtCompound compound = new NbtCompound("");
        compound.putInt(TAG_VERSION, SaveFormat.DATA_VERSION);
        compound.putInt(TAG_X, chunk.chunkX());
        compound.putInt(TAG_Y, chunk.chunkY());
        compound.put(new NbtIntArray(TAG_BLOCKS, blocks));
        compound.put(new NbtIntArray(TAG_META, meta));
        compound.put(new NbtByteArray(TAG_GENERATED, generated));
        compound.put(writeBlockEntities(chunk));
        compound.putBoolean(TAG_DECORATED, chunk.isDecorated());
        return compound;
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
            entry.putInt(SaveTags.BLOCK_ENTITY_Y, Chunk.localOf(entity.y()));
            entry.putInt(SaveTags.BLOCK_ENTITY_LAYER, entity.layer());
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
                        chunk.chunkX(), chunk.chunkY(), typeName);
                continue;
            }
            int localX = entry.getInt(SaveTags.BLOCK_ENTITY_X, -1);
            int localY = entry.getInt(SaveTags.BLOCK_ENTITY_Y, -1);
            int layer = entry.getInt(SaveTags.BLOCK_ENTITY_LAYER, Chunk.LAYER_OBJECT);
            if (!Chunk.contains(localX, localY) || layer < 0 || layer >= Chunk.LAYERS) {
                LOGGER.warn("Chunk ({}, {}) holds a block entity outside its cells at ({}, {}, {})",
                        chunk.chunkX(), chunk.chunkY(), localX, localY, layer);
                continue;
            }
            Block block = chunk.getBlock(localX, localY, layer);
            if (!type.name().equals(block.blockEntityTypeName())) {
                LOGGER.warn("Chunk ({}, {}) holds a '{}' at ({}, {}, {}) but the block there is"
                        + " '{}', the entity is dropped", chunk.chunkX(), chunk.chunkY(), typeName,
                        localX, localY, layer, block.name());
                continue;
            }
            BlockEntity entity = type.create();
            entity.setPosition(chunk.originX() + localX, chunk.originY() + localY, layer);
            NbtCompound data = entry.getCompound(SaveTags.DATA);
            entity.readData(data == null ? new NbtCompound(SaveTags.DATA) : data);
            chunk.setBlockEntity(entity);
        }
    }

    /**
     * Fills a chunk from a compound written by {@link #write(Chunk)}.
     * <p>
     * The chunk has to be empty, because the reader replaces every cell instead of
     * merging into it. Cell coordinates and the "generated" flags are restored as
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
            throw new SaveException("Chunk (" + chunk.chunkX() + ", " + chunk.chunkY()
                    + ") was written by save format " + version + ", this build reads format "
                    + SaveFormat.DATA_VERSION);
        }
        NbtIntArray blocks = compound.getIntArray(TAG_BLOCKS);
        NbtIntArray meta = compound.getIntArray(TAG_META);
        NbtByteArray generated = compound.getByteArray(TAG_GENERATED);
        if (blocks == null || blocks.length() != CELL_VALUES) {
            throw new SaveException("Chunk (" + chunk.chunkX() + ", " + chunk.chunkY()
                    + ") holds " + (blocks == null ? "no" : blocks.length()) + " block ids instead of "
                    + CELL_VALUES);
        }
        if (meta == null || meta.length() != CELL_VALUES) {
            throw new SaveException("Chunk (" + chunk.chunkX() + ", " + chunk.chunkY()
                    + ") holds " + (meta == null ? "no" : meta.length()) + " states instead of "
                    + CELL_VALUES);
        }
        if (generated == null || generated.length() != GENERATED_BYTES) {
            throw new SaveException("Chunk (" + chunk.chunkX() + ", " + chunk.chunkY()
                    + ") holds " + (generated == null ? "no" : generated.length())
                    + " generated flags instead of " + GENERATED_BYTES);
        }

        int index = 0;
        for (int layer = 0; layer < Chunk.LAYERS; layer++) {
            for (int localY = 0; localY < Constants.CHUNK_SIZE; localY++) {
                for (int localX = 0; localX < Constants.CHUNK_SIZE; localX++) {
                    // An id the game does not know becomes air, the same way a block of a
                    // game version that is gone would.
                    Block block = BlockRegistry.byId(blocks.get(index));
                    // The id goes first: writing it clears the state of the cell, so the
                    // stored state has to follow.
                    chunk.setRawId(localX, localY, layer, block.id());
                    chunk.setMeta(localX, localY, layer, meta.get(index));
                    index++;
                    if (layer == Chunk.LAYER_FLOOR
                            && generated.get(localY * Constants.CHUNK_SIZE + localX) != 0) {
                        chunk.markCellGenerated(localX, localY);
                    }
                }
            }
        }
        readBlockEntities(chunk, compound.getList(SaveTags.BLOCK_ENTITIES));
        if (compound.getBoolean(TAG_DECORATED, false)) {
            chunk.markDecorated();
        }
        // The chunk was just written, nothing has to be redrawn because of it, and
        // the file is now the authority, so the player-change flag is cleared too.
        chunk.clearDirty();
        chunk.clearModified();
        return true;
    }
}
