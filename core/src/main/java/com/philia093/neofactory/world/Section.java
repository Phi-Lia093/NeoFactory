package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.util.Constants;

/**
 * A cube of sixteen blocks on every side, the unit a chunk is stored and drawn in.
 * <p>
 * The flat world stored two layers per cell and a chunk was a square of tiles. A world of cubes
 * needs a third axis, and a column of two hundred and fifty six blocks per cell would cost every
 * chunk the same whether it holds a mountain or a sky: a section is that column cut into pieces of
 * sixteen, so a chunk pays for the air above the trees exactly nothing. A section that holds no
 * block at all keeps no array, see {@link #isEmpty()}.
 * <p>
 * <b>The order of the cells never changes.</b> A cell is found by
 * {@code index = (y << 8) | (z << 4) | x}, which is the order a stored chunk, a meshed section and a
 * light map read it in. Changing it would turn every saved world inside out.
 * <p>
 * <b>A block is an id, not an object.</b> A cell stores the number of a block and resolves it
 * through the {@link BlockRegistry}, which is what keeps a section at eight kilobytes for its
 * blocks: an id fits in a short, because {@link BlockRegistry#MAX_BLOCKS} is exactly the amount of
 * values an unsigned short holds.
 * <p>
 * <b>The state of a cell is free for a system to interpret.</b> It travels with the section and is
 * cleared whenever the block of the cell changes, the way the flat engine already stored it. The
 * direction a machine faces and the shape a pipe has to be drawn with live there.
 */
public final class Section {

    /** Side length of a section in blocks. */
    public static final int SIZE = Constants.SECTION_SIZE;

    /** Amount of blocks a section holds. */
    public static final int VOLUME = SIZE * SIZE * SIZE;

    /** Amount of bytes one light map of a section needs, one nibble per block. */
    public static final int LIGHT_BYTES = VOLUME / 2;

    /** Highest level a block may be lit with, the value of a cell that sees the whole sky. */
    public static final int MAX_LIGHT = 15;

    /**
     * Highest state a cell may carry.
     * <p>
     * A state is a wide number and not a byte: the fluid of a cell packs its level and whether it is
     * a source into nine bits, and the shapes a machine may take will need more of them. A state that
     * does not fit is refused where it is written, see {@link #setState(int, int, int, int)}, so a
     * system that outgrows this number hears about it there instead of finding its states cut down
     * somewhere later.
     */
    public static final int MAX_STATE = 0xFFFF;

    private static final int X_SHIFT = 0;
    private static final int Z_SHIFT = 4;
    private static final int Y_SHIFT = 8;

    /** Index of this section along the vertical axis of its chunk, never negative. */
    private final int sectionY;

    /** Block id of every cell, {@code null} while the section holds nothing but air. */
    private short[] blocks;

    /** State of every cell, {@code null} while no cell carries one. */
    private short[] states;

    /** Light of the sky falling into every cell, {@code null} until the sky was computed. */
    private byte[] skyLight;

    /** Light of the sources in and around this section, {@code null} until they were computed. */
    private byte[] blockLight;

    /** Amount of cells that hold a block, which is what makes {@link #isEmpty()} cheap. */
    private int blockCount;

    /** Set while the mesh of this section is behind the blocks it holds. */
    private boolean dirty;

    /**
     * Creates an empty section.
     *
     * @param sectionY index of the section along the vertical axis, {@code 0} for the lowest one
     * @throws IllegalArgumentException when the index is outside the world
     */
    public Section(int sectionY) {
        if (sectionY < 0 || sectionY >= Constants.SECTION_COUNT) {
            throw new IllegalArgumentException("Section outside the world: " + sectionY);
        }
        this.sectionY = sectionY;
    }

    /** Index of this section along the vertical axis of its chunk. */
    public int sectionY() {
        return sectionY;
    }

    /** Height of the lowest block this section holds. */
    public int originY() {
        return Constants.MIN_Y + sectionY * SIZE;
    }

    /** {@code true} while this section holds nothing but air and pays for no array. */
    public boolean isEmpty() {
        return blockCount == 0;
    }

    /** Amount of cells of this section that hold a block. */
    public int blockCount() {
        return blockCount;
    }

    /**
     * Block id of one cell of this section.
     *
     * @param x local X, {@code 0} to {@code 15}
     * @param y local Y, {@code 0} to {@code 15}
     * @param z local Z, {@code 0} to {@code 15}
     * @return the id, or {@link Block#AIR_ID} for a cell that holds nothing
     */
    public int rawId(int x, int y, int z) {
        return blocks == null ? Block.AIR_ID : blocks[index(x, y, z)] & 0xFFFF;
    }

    /**
     * Puts a block into one cell of this section.
     * <p>
     * The state of the cell is cleared whenever the block really changes, because a state that
     * belonged to the block that was there would describe the new one wrongly. Writing air into a
     * cell that already holds air changes nothing, so a generator may fill a column without paying
     * for the air above it - and a section that is never given a block never allocates.
     *
     * @param x local X, {@code 0} to {@code 15}
     * @param y local Y, {@code 0} to {@code 15}
     * @param z local Z, {@code 0} to {@code 15}
     * @param id block id
     * @throws IllegalArgumentException when the id is not a block of this game
     */
    public void setRawId(int x, int y, int z, int id) {
        if (id < 0 || id >= BlockRegistry.MAX_BLOCKS) {
            throw new IllegalArgumentException("Block id out of range: " + id);
        }
        int cell = index(x, y, z);
        int previous = blocks == null ? Block.AIR_ID : blocks[cell] & 0xFFFF;
        if (previous == id) {
            return;
        }
        if (id == Block.AIR_ID) {
            // The array exists: a cell that held air would have returned above.
            blocks[cell] = 0;
            states[cell] = 0;
            blockCount--;
            markDirty();
            return;
        }
        if (blocks == null) {
            blocks = new short[VOLUME];
            states = new short[VOLUME];
        }
        if (previous == Block.AIR_ID) {
            blockCount++;
        }
        blocks[cell] = (short) id;
        states[cell] = 0;
        markDirty();
    }

    /**
     * State of one cell, what its block carries beyond its id.
     *
     * @param x local X, {@code 0} to {@code 15}
     * @param y local Y, {@code 0} to {@code 15}
     * @param z local Z, {@code 0} to {@code 15}
     * @return the state, {@code 0} for a cell that carries none
     */
    public int state(int x, int y, int z) {
        return states == null ? 0 : states[index(x, y, z)] & 0xFFFF;
    }

    /**
     * Sets the state of one cell.
     * <p>
     * A state of zero is the state of a cell that carries nothing beyond its block, so it never
     * allocates. A number that does not fit into {@link #MAX_STATE} is refused here, where it is
     * written, because the storage would otherwise cut it down without a word and the system that
     * wrote it would find a state it never chose, far away from its own code.
     *
     * @param x local X, {@code 0} to {@code 15}
     * @param y local Y, {@code 0} to {@code 15}
     * @param z local Z, {@code 0} to {@code 15}
     * @param state state to store, {@code 0} to {@link #MAX_STATE}
     * @throws IllegalArgumentException when the state does not fit
     */
    public void setState(int x, int y, int z, int state) {
        if (state < 0 || state > MAX_STATE) {
            throw new IllegalArgumentException("State out of range: " + state);
        }
        if (state == 0 && states == null) {
            return;
        }
        if (states == null) {
            states = new short[VOLUME];
        }
        states[index(x, y, z)] = (short) state;
        // A state is what a pipe is drawn from and what a machine turns by, so a section that was given
        // one has to be meshed again: without this the cell would keep the shape of the state it had.
        markDirty();
    }

    /**
     * Level of sky light in one cell.
     *
     * @param x local X, {@code 0} to {@code 15}
     * @param y local Y, {@code 0} to {@code 15}
     * @param z local Z, {@code 0} to {@code 15}
     * @return the level, {@code 0} for a dark cell and for a section the sky was not computed in
     */
    public int skyLight(int x, int y, int z) {
        return skyLight == null ? 0 : nibble(skyLight, index(x, y, z));
    }

    /**
     * Sets the sky light of one cell.
     *
     * @param x local X, {@code 0} to {@code 15}
     * @param y local Y, {@code 0} to {@code 15}
     * @param z local Z, {@code 0} to {@code 15}
     * @param level level between {@code 0} and {@link #MAX_LIGHT}
     */
    public void setSkyLight(int x, int y, int z, int level) {
        skyLight = put(skyLight, index(x, y, z), level);
    }

    /**
     * Level of block light in one cell, the light of the torches and machines around it.
     *
     * @param x local X, {@code 0} to {@code 15}
     * @param y local Y, {@code 0} to {@code 15}
     * @param z local Z, {@code 0} to {@code 15}
     * @return the level, {@code 0} for a cell no source reaches
     */
    public int blockLight(int x, int y, int z) {
        return blockLight == null ? 0 : nibble(blockLight, index(x, y, z));
    }

    /**
     * Sets the block light of one cell.
     *
     * @param x local X, {@code 0} to {@code 15}
     * @param y local Y, {@code 0} to {@code 15}
     * @param z local Z, {@code 0} to {@code 15}
     * @param level level between {@code 0} and {@link #MAX_LIGHT}
     */
    public void setBlockLight(int x, int y, int z, int level) {
        blockLight = put(blockLight, index(x, y, z), level);
    }

    /** {@code true} once a light of this section was computed, either one. */
    public boolean hasLight() {
        return skyLight != null || blockLight != null;
    }

    /** Forgets both light maps, so the next computation starts from the dark. */
    public void clearLight() {
        skyLight = null;
        blockLight = null;
    }

    /** {@code true} while the mesh of this section is behind the blocks it holds. */
    public boolean isDirty() {
        return dirty;
    }

    /** Remembers that the blocks of this section changed and its mesh has to follow. */
    public void markDirty() {
        dirty = true;
    }

    /** Remembers that the mesh of this section was built from the blocks it holds now. */
    public void clearDirty() {
        dirty = false;
    }

    /**
     * Cell an index stands for.
     *
     * @param x local X, {@code 0} to {@code 15}
     * @param y local Y, {@code 0} to {@code 15}
     * @param z local Z, {@code 0} to {@code 15}
     * @return the index of that cell, {@code 0} to {@link #VOLUME} minus one
     * @throws IndexOutOfBoundsException when a coordinate is outside the section
     */
    public static int index(int x, int y, int z) {
        if (!contains(x) || !contains(y) || !contains(z)) {
            throw new IndexOutOfBoundsException(
                    "Cell outside the section: x=" + x + ", y=" + y + ", z=" + z);
        }
        return (y << Y_SHIFT) | (z << Z_SHIFT) | (x << X_SHIFT);
    }

    /**
     * Verifies that a local coordinate is inside a section.
     *
     * @param localCoordinate coordinate along any axis, may be negative
     * @return {@code true} when the coordinate can be used on this section
     */
    public static boolean contains(int localCoordinate) {
        return localCoordinate >= 0 && localCoordinate < SIZE;
    }

    /** Writes a level into a light map, allocating it on the first level that is not dark. */
    private static byte[] put(byte[] map, int index, int level) {
        if (level <= 0 && map == null) {
            return null;
        }
        byte[] target = map == null ? new byte[LIGHT_BYTES] : map;
        putNibble(target, index, level);
        return target;
    }

    /** Reads one nibble of a light map, the low nibble of a byte before the high one. */
    private static int nibble(byte[] map, int index) {
        int packed = map[index >> 1] & 0xFF;
        return (index & 1) == 0 ? packed & 0x0F : packed >>> 4;
    }

    /** Writes one nibble of a light map. */
    private static void putNibble(byte[] map, int index, int level) {
        int clamped = Math.min(Math.max(level, 0), MAX_LIGHT);
        int slot = index >> 1;
        int packed = map[slot] & 0xFF;
        map[slot] = (byte) ((index & 1) == 0
                ? (packed & 0xF0) | clamped
                : (packed & 0x0F) | (clamped << 4));
    }

    @Override
    public String toString() {
        return "Section(y=" + sectionY + " from " + originY() + ", " + blockCount + " blocks)";
    }
}
