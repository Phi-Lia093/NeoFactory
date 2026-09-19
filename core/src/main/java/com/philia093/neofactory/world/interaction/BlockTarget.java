package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.util.Constants;

import java.util.Objects;

/**
 * The cell a break or a build action would touch.
 * <p>
 * Beside the block coordinate and the layer the target remembers how it was
 * found. The mouse decides the cell as long as it is close enough to the player,
 * otherwise the line of sight does, see
 * {@link BlockTargeting#select(com.philia093.neofactory.world.World,
 * com.philia093.neofactory.entity.Player, float, float, int)}. The interface uses
 * that difference to draw the frame in two shades, so the player can tell which
 * one is in charge.
 * <p>
 * Instances are immutable and compared by their content, which is what lets the
 * {@link MiningController} notice that the player aimed somewhere else.
 */
public final class BlockTarget {

    private final int x;
    private final int y;
    private final int layer;
    private final boolean fromRay;

    private BlockTarget(int x, int y, int layer, boolean fromRay) {
        this.x = x;
        this.y = y;
        this.layer = layer;
        this.fromRay = fromRay;
    }

    /**
     * Creates a target the mouse picked.
     *
     * @param x block X coordinate
     * @param y block Y coordinate
     * @param layer layer index, see {@link com.philia093.neofactory.world.Chunk#LAYER_FLOOR}
     *              and {@link com.philia093.neofactory.world.Chunk#LAYER_OBJECT}
     */
    public static BlockTarget of(int x, int y, int layer) {
        return new BlockTarget(x, y, layer, false);
    }

    /**
     * Creates a target the line of sight picked, used when the mouse aims further
     * away than the player can reach.
     *
     * @param x block X coordinate
     * @param y block Y coordinate
     * @param layer layer index, see {@link com.philia093.neofactory.world.Chunk#LAYER_FLOOR}
     *              and {@link com.philia093.neofactory.world.Chunk#LAYER_OBJECT}
     */
    public static BlockTarget fromRay(int x, int y, int layer) {
        return new BlockTarget(x, y, layer, true);
    }

    /** Block X coordinate of the targeted cell. */
    public int x() {
        return x;
    }

    /** Block Y coordinate of the targeted cell. */
    public int y() {
        return y;
    }

    /** Layer the action applies to. */
    public int layer() {
        return layer;
    }

    /** {@code true} when the line of sight picked this cell instead of the mouse. */
    public boolean fromRay() {
        return fromRay;
    }

    /** World X coordinate of the middle of the targeted cell. */
    public float centerX() {
        return (x + 0.5f) * Constants.TILE_SIZE;
    }

    /** World Y coordinate of the middle of the targeted cell. */
    public float centerY() {
        return (y + 0.5f) * Constants.TILE_SIZE;
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
        return x == other.x && y == other.y && layer == other.layer && fromRay == other.fromRay;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, layer, fromRay);
    }

    @Override
    public String toString() {
        return "BlockTarget(" + x + ", " + y + ", layer " + layer
                + (fromRay ? ", line of sight" : ", mouse") + ")";
    }
}
