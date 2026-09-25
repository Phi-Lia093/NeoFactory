package com.philia093.neofactory.world;

import com.badlogic.gdx.math.MathUtils;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.util.Aabb;
import com.philia093.neofactory.util.Constants;

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
     * The part of a cell a body cannot enter, at the place the cell stands in the world.
     * <p>
     * A block is not always a whole cube. A slab fills the lower or the upper half of its cell, an anvil a
     * body of its own, so what a body runs into is the shape of
     * the model the state of the block is drawn with, see
     * {@link Block#shape(int, com.philia093.neofactory.util.Aabb)}, turned the way that state turns it
     * and moved to the place of the cell.
     * <p>
     * <b>A cell nothing holds is empty.</b> Air, a plant, a torch and a ladder are entered by a body
     * whatever shape they are drawn with, which is what {@link Block#isSolid()} decides: only a solid
     * block answers with a shape. The box a caller hands in is written to and never kept, so a frame
     * that walks the cells around a body allocates nothing.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @param into box to write the shape into, in the units of the world
     * @return the box, empty when a body walks through the cell, see {@link Aabb#isEmpty()}
     */
    default Aabb shape(int x, int y, int z, Aabb into) {
        Block block = getBlock(x, y, z);
        if (!block.isSolid()) {
            return into.clear();
        }
        return block.shape(getState(x, y, z), into).offset(x, y, z);
    }

    /**
     * {@code true} when a box reaches into the shape of a cell.
     * <p>
     * A body of the world is a box - the box of the player, the little box of a dropped item - and what it
     * runs into is the shape of every cell it reaches, see {@link #shape(int, int, int, Aabb)}: a block fills
     * its cell, a slab the lower or the upper half of it and a ladder nothing at all. The box a caller hands
     * in for the shapes is written to and never kept, so a frame that walks a body through the world
     * allocates nothing. A box that only touches a shape is not inside it, which is what lets a body rest on
     * the very top of what carries it, see {@link Aabb#intersects(Aabb)}.
     *
     * @param box box of the body, in the units of the world
     * @param into box the shape of one cell is read into
     * @return {@code true} when the box overlaps the shape of at least one cell
     */
    default boolean overlaps(Aabb box, Aabb into) {
        if (box.isEmpty()) {
            return false;
        }
        int minX = MathUtils.floor(box.minX());
        int maxX = MathUtils.floor(box.maxX());
        int minZ = MathUtils.floor(box.minZ());
        int maxZ = MathUtils.floor(box.maxZ());
        int minY = Math.max(Constants.MIN_Y, MathUtils.floor(box.minY()));
        int maxY = Math.min(Constants.MAX_Y, MathUtils.floor(box.maxY()));
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Aabb shape = shape(x, y, z, into);
                    if (!shape.isEmpty() && box.intersects(shape)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Height a box comes to rest at on top of the shapes of one cell.
     * <p>
     * The shape of a cell is not always a whole cube: a slab ends halfway up its own cell and an anvil at the
     * height of its plate. The top of the highest shape a body can rest on is what its feet are put on - a
     * shape that lies above the height the body still stood at does not carry it, it is what the body fell
     * past. A height that is read this way is exact, so a body comes to rest on the very top of what stopped
     * it and stands there without moving again, see {@code Player#stepVertically}.
     *
     * @param box box of the body, in the units of the world
     * @param cell cell the fall reached, the one the shape that stopped it stands in
     * @param above height the body still stood at before the step
     * @param into box the shape of one cell is read into
     * @return the height the body is put on, at least the floor of that cell
     */
    default float landingHeight(Aabb box, int cell, float above, Aabb into) {
        // The world has a bottom and it is a floor: a body that reached it stands on it instead of asking
        // about cells below the world, which are not there.
        if (cell < Constants.MIN_Y) {
            return Constants.MIN_Y;
        }
        float highest = cell;
        int minX = MathUtils.floor(box.minX());
        int maxX = MathUtils.floor(box.maxX());
        int minZ = MathUtils.floor(box.minZ());
        int maxZ = MathUtils.floor(box.maxZ());
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Aabb shape = shape(x, cell, z, into);
                if (shape.isEmpty() || shape.maxY() > above) {
                    continue;
                }
                highest = Math.max(highest, shape.maxY());
            }
        }
        return highest;
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
