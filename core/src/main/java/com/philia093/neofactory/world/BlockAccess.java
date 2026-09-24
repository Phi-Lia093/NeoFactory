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
 * is its Y coordinate. Nothing here names a layer or a plane: the ground of a column is the block
 * below it, and every other cell of the column is reached by its own height.
 */
public interface BlockAccess {

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
     * planks, a trunk, a machine, bedrock - or when it carries something that is neither air nor a
     * surface to walk on. Everything else is a cell a body may enter: the empty air everywhere and the
     * ground a body stands on, see {@link Block#isSolid()} and {@link Block#isGround()}.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @return {@code true} when the cell cannot be entered
     */
    default boolean isSolid(int x, int y, int z) {
        Block block = getBlock(x, y, z);
        if (block.isAir()) {
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
            // A cell the mouse named knows no side, so the ground below it has to carry the block.
            return hasBlock(x, y - 1, z);
        }
        return hasBlock(x - face.x(), y - face.y(), z - face.z());
    }

}
