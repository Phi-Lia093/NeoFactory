package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.util.Constants;

import java.util.Objects;

/**
 * The cell a break or a build action would touch.
 * <p>
 * Beside the cell itself the target remembers how it was found. The mouse decides the cell as long as
 * it is close enough to the player, otherwise the line of sight does, see
 * {@link BlockTargeting#select(com.philia093.neofactory.world.World,
 * com.philia093.neofactory.entity.Player, float, float, int)}; the interface uses that difference to
 * draw the frame in two shades, so the player can tell which one is in charge.
 * <p>
 * <b>The face is what a ray brings with it.</b> {@link #face()} names the face the line of sight
 * entered the cell through, and it is what a block is built against - a block goes into the cell
 * behind that face. A cell the mouse named has no face, because pointing at a cell says nothing about
 * the side it is looked at from, see {@link #of(int, int, int)}.
 * <p>
 * Instances are immutable and compared by their content, which is what lets the
 * {@link MiningController} notice that the player aimed somewhere else.
 */
public final class BlockTarget {

    private final int x;
    private final int y;
    private final int z;
    private final BlockFace face;
    private final boolean fromRay;

    private BlockTarget(int x, int y, int z, BlockFace face, boolean fromRay) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
        this.fromRay = fromRay;
    }

    /**
     * Creates a target the mouse picked, seen from no particular side.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @return the target, without a face
     */
    public static BlockTarget of(int x, int y, int z) {
        return new BlockTarget(x, y, z, null, false);
    }

    /**
     * Creates a target the mouse picked that knows the side it is looked at from.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @param face face the cell is looked at through
     * @return the target
     */
    public static BlockTarget of(int x, int y, int z, BlockFace face) {
        return new BlockTarget(x, y, z, face, false);
    }

    /**
     * Creates a target the line of sight picked, used when the mouse aims further away than the player
     * can reach.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @param face face the ray entered the cell through
     * @return the target, with the face the ray came in through
     */
    public static BlockTarget fromRay(int x, int y, int z, BlockFace face) {
        return new BlockTarget(x, y, z, face, true);
    }

    /**
     * Creates a target from a ray that stopped at a cell.
     *
     * @param hit cell the ray met, with the face it entered through
     * @return the target
     */
    public static BlockTarget of(BlockRay.Hit hit) {
        return new BlockTarget(hit.x(), hit.y(), hit.z(), hit.face(), true);
    }

    /** Block X coordinate of the targeted cell. */
    public int x() {
        return x;
    }

    /** Block Y coordinate of the targeted cell, its height in the world. */
    public int y() {
        return y;
    }

    /** Block Z coordinate of the targeted cell. */
    public int z() {
        return z;
    }

    /**
     * Face the line of sight entered the cell through.
     *
     * @return the face, or {@code null} when the mouse named the cell and no side is known
     */
    public BlockFace face() {
        return face;
    }

    /** {@code true} when the line of sight picked this cell instead of the mouse. */
    public boolean fromRay() {
        return fromRay;
    }

    /**
     * The cell this cell was entered from, one step back through {@link #face()}.
     *
     * @return the neighbouring cell, or {@code null} when no face is known
     */
    public BlockTarget neighbour() {
        if (face == null) {
            return null;
        }
        return of(x + face.x(), y + face.y(), z + face.z(), face);
    }

    /** World X coordinate of the middle of the targeted cell. */
    public float centerX() {
        return (x + 0.5f) * Constants.BLOCK_SIZE;
    }

    /**
     * World Z coordinate of the middle of the targeted cell.
     * <p>
     * The world of the flat view is the plane of X and Z, so this is the second of the two
     * coordinates a dropped item is placed with, see
     * {@link com.philia093.neofactory.item.ItemDrops#drop(com.philia093.neofactory.item.ItemStack,
     * float, float)}.
     */
    public float centerZ() {
        return (z + 0.5f) * Constants.BLOCK_SIZE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BlockTarget)) {
            return false;
        }
        BlockTarget other = (BlockTarget) o;
        return x == other.x && y == other.y && z == other.z && face == other.face
                && fromRay == other.fromRay;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, face, fromRay);
    }

    @Override
    public String toString() {
        return "BlockTarget(" + x + ", " + y + ", " + z
                + (face == null ? ", no face" : ", face " + face)
                + (fromRay ? ", line of sight" : ", mouse") + ")";
    }
}
