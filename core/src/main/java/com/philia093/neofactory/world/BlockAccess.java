package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Block;

/**
 * Read and write access to the blocks of a world.
 * <p>
 * Consumers such as renderers, world generation or entity logic talk to this
 * interface instead of {@link World} so that the storage layout (chunks,
 * snapshots, network views, ...) can change without touching them.
 * <p>
 * Both coordinates are horizontal because the game is viewed from above. Every
 * access has to name the layer that is meant, see {@link Chunk#LAYER_FLOOR} and
 * {@link Chunk#LAYER_OBJECT}.
 */
public interface BlockAccess {

    /**
     * Returns a block of the world.
     *
     * @param x block coordinate along the first horizontal axis, may be negative
     * @param y block coordinate along the second horizontal axis, may be negative
     * @param layer layer index, see {@link Chunk#LAYER_FLOOR} and
     *              {@link Chunk#LAYER_OBJECT}
     * @return the block at that position, never {@code null}
     */
    Block getBlock(int x, int y, int layer);

    /**
     * Stores a block in the world.
     *
     * @param x block coordinate along the first horizontal axis, may be negative
     * @param y block coordinate along the second horizontal axis, may be negative
     * @param layer layer index, see {@link Chunk#LAYER_FLOOR} and
     *              {@link Chunk#LAYER_OBJECT}
     * @param block block to store, must not be {@code null}
     */
    void setBlock(int x, int y, int layer, Block block);

    /**
     * Returns {@code true} when a position stops player movement.
     * <p>
     * The object layer wins over the floor layer, an empty object layer cell is
     * therefore walkable as long as the floor below it is walkable too. Ground
     * surfaces are not solid, so a plain grass, sand or stone cell lets the player
     * pass: only an obstacle in the object layer or impassable ground such as
     * bedrock blocks the way, see {@code Blocks}.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return {@code true} when the player cannot enter that cell
     */
    default boolean isSolid(int x, int y) {
        Block object = getBlock(x, y, Chunk.LAYER_OBJECT);
        if (!object.isAir()) {
            return object.isSolid();
        }
        return getBlock(x, y, Chunk.LAYER_FLOOR).isSolid();
    }
}
