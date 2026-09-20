package com.philia093.neofactory.world.save;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.util.nbt.NbtByteArray;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.Chunk;

/**
 * Converts a {@link Chunk} to tag data and back.
 * <p>
 * A chunk is stored whole instead of storing only the blocks the player changed.
 * That keeps writing simple and it keeps the file readable: a chunk that was
 * loaded, saved and loaded again holds exactly the same cells, whatever happened
 * to them.
 * <p>
 * Two things have to travel with the blocks:
 * <ul>
 *     <li>the block data of both layers, in the fixed order
 *         {@link Chunk#LAYER_FLOOR} then {@link Chunk#LAYER_OBJECT}</li>
 *     <li>the generated-cell bitmap and the decorated flag</li>
 * </ul>
 * The second point is the one that is easy to forget: the generator only creates
 * the ground of cells that are already generated, so without that bitmap a chunk
 * would look half empty after loading and the generator would fill it in again,
 * possibly burying what the player built there.
 */
public final class ChunkCodec {

    /** Name of the tag holding the chunk X coordinate. */
    static final String TAG_X = "X";

    /** Name of the tag holding the chunk Y coordinate. */
    static final String TAG_Y = "Y";

    /** Name of the tag holding the block data of both layers. */
    static final String TAG_BLOCKS = "Blocks";

    /** Name of the tag holding the generated-cell bitmap. */
    static final String TAG_GENERATED = "Generated";

    /** Name of the tag holding whether the decorations were planted. */
    static final String TAG_DECORATED = "Decorated";

    /** Amount of bytes the block data of a chunk needs. */
    static final int BLOCK_BYTES = Chunk.CELL_COUNT * Chunk.LAYERS;

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
        byte[] blocks = new byte[BLOCK_BYTES];
        byte[] generated = new byte[GENERATED_BYTES];
        int index = 0;
        for (int layer = 0; layer < Chunk.LAYERS; layer++) {
            for (int localY = 0; localY < Constants.CHUNK_SIZE; localY++) {
                for (int localX = 0; localX < Constants.CHUNK_SIZE; localX++) {
                    blocks[index++] = (byte) chunk.rawId(localX, localY, layer);
                    if (layer == Chunk.LAYER_FLOOR) {
                        int cell = localY * Constants.CHUNK_SIZE + localX;
                        generated[cell] = (byte) (chunk.isCellGenerated(localX, localY) ? 1 : 0);
                    }
                }
            }
        }

        NbtCompound compound = new NbtCompound("");
        compound.putInt(TAG_X, chunk.chunkX());
        compound.putInt(TAG_Y, chunk.chunkY());
        compound.put(new NbtByteArray(TAG_BLOCKS, blocks));
        compound.put(new NbtByteArray(TAG_GENERATED, generated));
        compound.putBoolean(TAG_DECORATED, chunk.isDecorated());
        return compound;
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
     * @throws SaveException when the tag data does not describe this chunk
     */
    public static boolean read(Chunk chunk, NbtCompound compound) {
        NbtByteArray blocks = compound.getByteArray(TAG_BLOCKS);
        NbtByteArray generated = compound.getByteArray(TAG_GENERATED);
        if (blocks == null || blocks.length() != BLOCK_BYTES) {
            throw new SaveException("Chunk (" + chunk.chunkX() + ", " + chunk.chunkY()
                    + ") holds " + (blocks == null ? "no" : blocks.length()) + " block bytes instead of "
                    + BLOCK_BYTES);
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
                    int id = blocks.get(index++) & 0xFF;
                    Block block = BlockRegistry.byId(id);
                    chunk.setRawId(localX, localY, layer, block.id());
                    if (layer == Chunk.LAYER_FLOOR
                            && generated.get(localY * Constants.CHUNK_SIZE + localX) != 0) {
                        chunk.markCellGenerated(localX, localY);
                    }
                }
            }
        }
        if (compound.getBoolean(TAG_DECORATED, false)) {
            chunk.markDecorated();
        }
        // The chunk was just written, nothing has to be redrawn because of it.
        chunk.clearDirty();
        return true;
    }

    /** Root tag a block id is stored in, used by the harness. */
    static String blockTagName() {
        return TAG_BLOCKS;
    }
}
