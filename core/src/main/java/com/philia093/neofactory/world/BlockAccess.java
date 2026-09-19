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
     * therefore walkable as long as the ground below it is walkable too.
     * <p>
     * The object layer is the layer the player stands in, so a block there blocks
     * the way whenever it is solid: a wall the player built, a tree trunk or the
     * leaves of a canopy. Tall grass and water are not solid and are walked through.
     * <p>
     * The floor layer is the ground the player walks over, so only a block that is
     * not a ground surface blocks there, for example bedrock. Stone, sand and every
     * other ground surface never do, no matter how hard the material is, and neither
     * does air: a hole the player dug stays passable, see {@link Block#isGround()}.
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
        return !getBlock(x, y, Chunk.LAYER_FLOOR).isGround();
    }

    /**
     * {@code true} when the layer the player stands in holds a block.
     * <p>
     * Such a column carries something in front of the player: the block has to be
     * removed before the ground below it can be touched at all, see
     * {@link #hasGround(int, int)}.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return {@code true} when the object layer of that cell is not empty
     */
    default boolean hasObjectBlock(int x, int y) {
        return !getBlock(x, y, Chunk.LAYER_OBJECT).isAir();
    }

    /**
     * {@code true} when the cell has ground below.
     * <p>
     * A cell without ground is a hole the player dug. Nothing may be built into the
     * layer the player stands in above such a cell, because the block would float in
     * the air.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return {@code true} when the floor layer of that cell is not empty
     */
    default boolean hasGround(int x, int y) {
        return !getBlock(x, y, Chunk.LAYER_FLOOR).isAir();
    }
}
