package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;

/**
 * Read and write access to the blocks of a world.
 * <p>
 * Consumers such as renderers, world generation or entity logic talk to this
 * interface instead of {@link World} so that the storage layout (chunks,
 * snapshots, network views, ...) can change without touching them.
 * <p>
 * <b>A cell is named by three coordinates.</b> X points east, Y points up and Z points south, the
 * axes of the original game, so a block is addressed by {@code (x, y, z)} and the height of a cell
 * is its Y coordinate. The flat engine had no height: it named a layer, and its second horizontal
 * axis was called Y. Nothing of that is left in the names here - a caller that still thinks in
 * layers asks {@link Chunk#flatY(int)} for the height it means, which is the only part of the flat
 * view this interface still carries, see {@link #FLAT_FLOOR_Y}.
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
    /**
     * {@code true} when a cell may hold a block of its own.
     * <p>
     * A block is attached to whatever it is built against, and a line of sight brings the face it entered
     * the cell through with it: the cell behind that face is what the block hangs on, so a wall is built
     * against and a bridge grows sideways. A cell the mouse named knows no side, and the rule of the flat
     * view answers instead - the ground below it - because there is nothing else to ask.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @param face face the cell was entered through, {@code null} when no side is known
     * @return {@code true} when the cell has something to hold on to
     */
    default boolean hasSupport(int x, int y, int z, BlockFace face) {
        if (face == null) {
            return hasSupport(x, y, z);
        }
        return hasBlock(x - face.x(), y - face.y(), z - face.z());
    }

    /**
     * {@code true} when a cell holds a block, the ground of the flat view included.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @return {@code true} when that cell is not empty
     */
    default boolean hasSupport(int x, int y, int z) {
        return y == FLAT_FLOOR_Y || hasBlock(x, y - 1, z);
    }







}
