package com.philia093.neofactory.world;

import java.util.Objects;

/**
 * Immutable integer block coordinate inside a {@link World}.
 * <p>
 * Both axes are horizontal because the world is viewed from above. On screen
 * {@code x} grows to the right and {@code y} grows upwards, so increasing
 * {@code y} moves towards the top of the screen. Coordinates may be negative in
 * every direction, the world is not limited to a single quadrant.
 */
public final class BlockPos {

    private final int x;
    private final int y;

    private BlockPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** Creates a position from raw block coordinates. */
    public static BlockPos of(int x, int y) {
        return new BlockPos(x, y);
    }

    /** Creates a mutable copy of the given position. */
    public static BlockPos of(BlockPos other) {
        return new BlockPos(other.x, other.y);
    }

    /**
     * Packs a block coordinate pair into a single {@code long}.
     * <p>
     * Both coordinates are limited to 32 bit each, which yields a stable key
     * that can be used inside hash maps.
     */
    public static long pack(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    /** Unpacks the X component previously written by {@link #pack(int, int)}. */
    public static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    /** Unpacks the Y component previously written by {@link #pack(int, int)}. */
    public static int unpackY(long packed) {
        return (int) packed;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    /** Returns this position shifted by the given amount. */
    public BlockPos offset(int dx, int dy) {
        return new BlockPos(x + dx, y + dy);
    }

    /** Returns this position shifted one block upwards. */
    public BlockPos up() {
        return new BlockPos(x, y + 1);
    }

    /** Returns this position shifted one block downwards. */
    public BlockPos down() {
        return new BlockPos(x, y - 1);
    }

    /** Returns this position shifted one block towards positive X. */
    public BlockPos east() {
        return new BlockPos(x + 1, y);
    }

    /** Returns this position shifted one block towards negative X. */
    public BlockPos west() {
        return new BlockPos(x - 1, y);
    }

    /** Packed representation of this position, see {@link #pack(int, int)}. */
    public long packed() {
        return pack(x, y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BlockPos)) {
            return false;
        }
        BlockPos other = (BlockPos) o;
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "BlockPos(" + x + ", " + y + ")";
    }
}