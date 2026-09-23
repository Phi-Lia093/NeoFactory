package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Block;

/**
 * Read and write access to the blocks of a world.
 * <p>
 * Consumers such as renderers, world generation or entity logic talk to this
 * interface instead of {@link World} so that the storage layout (chunks,
 * snapshots, network views, ...) can change without touching them.
 * <p>
 * <b>A cell is named by three coordinates.</b> X points east, Y points up and Z points south, the
 * axes of the original game, so a block is addressed by {@code (x, y, z)} and the height of a cell
 * is its Y coordinate. The flat engine had no height: it named a layer and its second horizontal
 * axis was called Y, which is why this interface carries a second set of methods for that view.
 * Those are named for what they are - {@link #getFlatBlock(int, int, int)} and the methods around it
 * name a column and a layer instead of a cell - and they go away once the game is played standing in
 * the world instead of looking down on it.
 */
public interface BlockAccess {

    /** Height of the ground layer of the flat view, the layer the player walks on. */
    int FLAT_FLOOR_Y = Chunk.flatY(Chunk.LAYER_FLOOR);

    /** Height of the object layer of the flat view, the layer the player walks in. */
    int FLAT_OBJECT_Y = Chunk.flatY(Chunk.LAYER_OBJECT);

    /**
     * Returns a block of the world.
     *
     * @param x block X coordinate, may be negative
     * @param y block Y coordinate, the height, {@code MIN_Y} to {@code MAX_Y}
     * @param z block Z coordinate, may be negative
     * @return the block at that cell, never {@code null}
     */
    Block getBlock(int x, int y, int z);

    /**
     * Stores a block in the world.
     *
     * @param x block X coordinate, may be negative
     * @param y block Y coordinate, the height, {@code MIN_Y} to {@code MAX_Y}
     * @param z block Z coordinate, may be negative
     * @param block block to store, must not be {@code null}
     */
    void setBlock(int x, int y, int z, Block block);

    /**
     * Returns the state of a cell.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @return the stored state, {@code 0} for a cell nothing wrote one to
     */
    int getState(int x, int y, int z);

    /**
     * Writes the state of a cell.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @param state state to store
     */
    void setState(int x, int y, int z, int state);

    /**
     * Returns {@code true} when a cell stops a body.
     * <p>
     * A cell holds a body back when it carries a block that fills it as an obstacle - a wall of
     * planks, a trunk, a machine, bedrock - or when it carries something that is neither air, nor a
     * surface to walk on, nor a fluid. Everything else is a cell a body may enter: the empty air
     * everywhere, the ground a body stands on and the substance it wades through, see
     * {@link Block#isSolid()}, {@link Block#isGround()} and {@link Block#isLiquid()}.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @return {@code true} when the cell cannot be entered
     */
    default boolean isSolid(int x, int y, int z) {
        Block block = getBlock(x, y, z);
        if (block.isAir() || block.isLiquid()) {
            return false;
        }
        return block.isSolid() || !block.isGround();
    }

    /**
     * {@code true} when a cell holds a block.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @return {@code true} when the cell is not empty
     */
    default boolean hasBlock(int x, int y, int z) {
        return !getBlock(x, y, z).isAir();
    }

    /**
     * {@code true} when a block or a fluid may stand in an empty cell.
     * <p>
     * Something to stand on is what keeps a block from floating: the cell below has to hold one. The
     * ground layer of the flat view is the exception, because the world the game draws today has no
     * terrain below it: the floor of a column is the bottom of that world, and a hole dug into it
     * could never be filled again without this.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @return {@code true} when a block or a fluid may be placed here
     */
    default boolean hasSupport(int x, int y, int z) {
        return y == FLAT_FLOOR_Y || hasBlock(x, y - 1, z);
    }

    /**
     * Returns a block of the flat view: a layer of the surface of a column.
     *
     * @param x block X coordinate along the first horizontal axis, may be negative
     * @param z block Z coordinate along the second horizontal axis, may be negative
     * @param layer layer index, see {@link Chunk#LAYER_FLOOR} and {@link Chunk#LAYER_OBJECT}
     * @return the block at that cell, never {@code null}
     * @deprecated the flat view: a world of cubes names the height of a cell, see
     *         {@link #getBlock(int, int, int)}
     */
    @Deprecated
    default Block getFlatBlock(int x, int z, int layer) {
        return getBlock(x, Chunk.flatY(layer), z);
    }

    /**
     * Stores a block in a layer of the flat view.
     *
     * @param x block X coordinate along the first horizontal axis, may be negative
     * @param z block Z coordinate along the second horizontal axis, may be negative
     * @param layer layer index, see {@link Chunk#LAYER_FLOOR} and {@link Chunk#LAYER_OBJECT}
     * @param block block to store, must not be {@code null}
     * @deprecated the flat view, see {@link #getFlatBlock(int, int, int)}
     */
    @Deprecated
    default void setFlatBlock(int x, int z, int layer, Block block) {
        setBlock(x, Chunk.flatY(layer), z, block);
    }

    /**
     * Returns the state of a cell of the flat view.
     *
     * @param x block X coordinate along the first horizontal axis
     * @param z block Z coordinate along the second horizontal axis
     * @param layer layer index, see {@link Chunk#LAYER_FLOOR} and {@link Chunk#LAYER_OBJECT}
     * @return the stored state, {@code 0} for a cell nothing wrote one to
     * @deprecated the flat view, see {@link #getFlatBlock(int, int, int)}
     */
    @Deprecated
    default int getFlatState(int x, int z, int layer) {
        return getState(x, Chunk.flatY(layer), z);
    }

    /**
     * Writes the state of a cell of the flat view.
     *
     * @param x block X coordinate along the first horizontal axis
     * @param z block Z coordinate along the second horizontal axis
     * @param layer layer index, see {@link Chunk#LAYER_FLOOR} and {@link Chunk#LAYER_OBJECT}
     * @param state state to store
     * @deprecated the flat view, see {@link #getFlatBlock(int, int, int)}
     */
    @Deprecated
    default void setFlatState(int x, int z, int layer, int state) {
        setState(x, Chunk.flatY(layer), z, state);
    }

    /**
     * {@code true} when a column of the flat view stops a body.
     * <p>
     * The flat body of the game walks in a column instead of standing in a cell, so both layers
     * decide whether it may enter: a wall in the layer the player stands in holds them back, and so
     * does a block below their feet that is not a surface to walk on.
     *
     * @param x block X coordinate along the first horizontal axis
     * @param z block Z coordinate along the second horizontal axis
     * @return {@code true} when the player cannot enter that column
     * @deprecated the flat view: a body of a world of cubes stands in a cell, see
     *         {@link #isSolid(int, int, int)}
     */
    @Deprecated
    default boolean isFlatSolid(int x, int z) {
        return isSolid(x, FLAT_OBJECT_Y, z) || isSolid(x, FLAT_FLOOR_Y, z);
    }

    /**
     * {@code true} when the layer the player stands in holds a block.
     * <p>
     * Such a column carries something in front of the player: the block has to be removed before the
     * ground below it can be touched at all, see {@link #hasFlatGround(int, int)}.
     *
     * @param x block X coordinate along the first horizontal axis
     * @param z block Z coordinate along the second horizontal axis
     * @return {@code true} when the object layer of that column is not empty
     * @deprecated the flat view, see {@link #hasBlock(int, int, int)}
     */
    @Deprecated
    default boolean hasFlatObjectBlock(int x, int z) {
        return hasBlock(x, FLAT_OBJECT_Y, z);
    }

    /**
     * {@code true} when a column of the flat view has ground below.
     * <p>
     * A column without ground is a hole the player dug. Nothing may be built into the layer the
     * player stands in above such a column, because the block would float in the air.
     *
     * @param x block X coordinate along the first horizontal axis
     * @param z block Z coordinate along the second horizontal axis
     * @return {@code true} when the floor layer of that column is not empty
     * @deprecated the flat view, see {@link #hasBlock(int, int, int)}
     */
    @Deprecated
    default boolean hasFlatGround(int x, int z) {
        return hasBlock(x, FLAT_FLOOR_Y, z);
    }
}
