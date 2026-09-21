package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.blockentity.BlockEntity;
import com.philia093.neofactory.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.philia093.neofactory.util.Constants.CHUNK_SIZE;

/**
 * A square tile of the world, seen from above.
 * <p>
 * A chunk stores {@link Constants#CHUNK_SIZE} times
 * {@link Constants#CHUNK_SIZE} cells. Both axes are horizontal, there is no
 * vertical axis: the camera looks straight down on the world. Two independent
 * layers are kept per cell:
 * <ul>
 *     <li>{@link #LAYER_FLOOR} - the ground the player walks on, for example
 *         grass, sand or stone, produced by the biome driven generator</li>
 *     <li>{@link #LAYER_OBJECT} - the layer the player stands in, empty by
 *         default and used for trees, plants and everything the player places
 *         later on</li>
 * </ul>
 * A cell stores a block id and a state, both as full 32 bit numbers: the id is
 * resolved into the actual {@link Block} through the {@link BlockRegistry}, which
 * keeps the memory footprint small, and the state is what a block carries beyond
 * its id, see {@link #meta(int, int, int)}.
 */
public final class Chunk {

    /** Number of block layers stored per cell. */
    public static final int LAYERS = 2;

    /** Index of the generated ground layer. */
    public static final int LAYER_FLOOR = Constants.LAYER_FLOOR;

    /** Index of the object layer holding trees, plants and player blocks. */
    public static final int LAYER_OBJECT = Constants.LAYER_OBJECT;

    /** Amount of cells stored by a single chunk. */
    public static final int CELL_COUNT = CHUNK_SIZE * CHUNK_SIZE;

    private final int chunkX;
    private final int chunkY;

    /** Block id of every cell, one number per cell and layer. */
    private final int[] blocks;

    /**
     * State of every cell, one number per cell and layer.
     * <p>
     * The state is what a block carries beyond its id: the direction a machine
     * faces, the shape a pipe has to be drawn with, whether a machine is running.
     * Nothing interprets it yet, but it travels with the chunk, so a later system
     * finds the state of a cell exactly where it left it. A cell whose block
     * changes loses its state, see {@link #setRawId(int, int, int, int)}.
     */
    private final int[] meta;

    /**
     * Block entity of every cell, {@code null} for a cell that carries none.
     * <p>
     * The array is the lookup: a machine standing somewhere is found without walking
     * anything. {@link #active} is what a tick walks instead, because the list holds the
     * entities that are really there - a chunk with a single machine does not make the
     * world look at {@link #CELL_COUNT} cells every tick.
     */
    private final BlockEntity[] blockEntities = new BlockEntity[CELL_COUNT * LAYERS];

    /** Block entities of this chunk, in the order they were put in. */
    private final List<BlockEntity> active = new ArrayList<>();

    /** Cells whose floor layer was already written by the generator. */
    private final boolean[] generatedCells = new boolean[CELL_COUNT];

    /** Amount of cells whose floor layer is already generated. */
    private int generatedCellCount;

    /** Set once the decorations of this chunk were planted. */
    private boolean decorated;

    /**
     * Set when the player changed the content of this chunk, which means the
     * chunk must reach the save game. Generation and decoration never set it,
     * because generated chunks can be rebuilt from the seed alone.
     */
    private boolean modified;

    /** Set when the content changed and render caches have to be rebuilt. */
    private boolean dirty = true;

    /**
     * Creates an empty chunk.
     *
     * @param chunkX chunk coordinate along the X axis, covers the block columns
     *               {@code chunkX * CHUNK_SIZE} to {@code chunkX * CHUNK_SIZE + CHUNK_SIZE - 1}
     * @param chunkY chunk coordinate along the Y axis, same rule as {@code chunkX}
     */
    public Chunk(int chunkX, int chunkY) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.blocks = new int[CELL_COUNT * LAYERS];
        this.meta = new int[CELL_COUNT * LAYERS];
    }

    /** Chunk coordinate along the X axis. */
    public int chunkX() {
        return chunkX;
    }

    /** Chunk coordinate along the Y axis. */
    public int chunkY() {
        return chunkY;
    }

    /** Block X coordinate of the first column of this chunk. */
    public int originX() {
        return chunkX * CHUNK_SIZE;
    }

    /** Block Y coordinate of the first row of this chunk. */
    public int originY() {
        return chunkY * CHUNK_SIZE;
    }

    /**
     * Returns the block stored in a specific layer.
     *
     * @param localX local X coordinate, {@code 0 <= localX < CHUNK_SIZE}
     * @param localY local Y coordinate, {@code 0 <= localY < CHUNK_SIZE}
     * @param layer layer index, see {@link #LAYER_FLOOR} and {@link #LAYER_OBJECT}
     * @return the stored block, {@link Blocks#AIR} for empty space
     */
    public Block getBlock(int localX, int localY, int layer) {
        return BlockRegistry.byId(blocks[index(localX, localY, layer)]);
    }

    /**
     * Stores a block in a specific layer and marks the chunk for redrawing.
     *
     * @param localX local X coordinate, {@code 0 <= localX < CHUNK_SIZE}
     * @param localY local Y coordinate, {@code 0 <= localY < CHUNK_SIZE}
     * @param layer layer index, see {@link #LAYER_FLOOR} and {@link #LAYER_OBJECT}
     * @param block block to store, passing {@link Blocks#AIR} clears the position
     */
    public void setBlock(int localX, int localY, int layer, Block block) {
        setRawId(localX, localY, layer, block.id());
    }

    /**
     * Returns the raw id stored at a position, used by serialization.
     *
     * @param localX local X coordinate
     * @param localY local Y coordinate
     * @param layer layer index
     * @return the numeric block id
     */
    public int rawId(int localX, int localY, int layer) {
        return blocks[index(localX, localY, layer)];
    }

    /**
     * Writes a raw block id, used by serialization and world generation.
     * <p>
     * A cell whose block changes loses its state: the state of the block that was
     * there belongs to that block and means nothing for the one that takes its
     * place.
     *
     * @param localX local X coordinate
     * @param localY local Y coordinate
     * @param layer layer index
     * @param id numeric block id
     */
    public void setRawId(int localX, int localY, int layer, int id) {
        int slot = index(localX, localY, layer);
        if (blocks[slot] == id) {
            return;
        }
        blocks[slot] = id;
        meta[slot] = 0;
        dirty = true;
    }

    /**
     * Returns the state stored for a cell.
     *
     * @param localX local X coordinate
     * @param localY local Y coordinate
     * @param layer layer index
     * @return the state, {@code 0} for a cell nothing wrote a state to
     */
    public int meta(int localX, int localY, int layer) {
        return meta[index(localX, localY, layer)];
    }

    /**
     * Writes the state of a cell.
     * <p>
     * The state is what a block carries beyond its id, see {@link #meta}. Writing
     * it marks the chunk for redrawing, because a state is usually what a renderer
     * draws differently.
     *
     * @param localX local X coordinate
     * @param localY local Y coordinate
     * @param layer layer index
     * @param state state to store
     */
    public void setMeta(int localX, int localY, int layer, int state) {
        int slot = index(localX, localY, layer);
        if (meta[slot] == state) {
            return;
        }
        meta[slot] = state;
        dirty = true;
    }

    /**
     * Returns the block entity of a cell.
     *
     * @param localX local X coordinate
     * @param localY local Y coordinate
     * @param layer layer index
     * @return the entity, or {@code null} when the cell carries none
     */
    public BlockEntity blockEntity(int localX, int localY, int layer) {
        return blockEntities[index(localX, localY, layer)];
    }

    /**
     * Puts a block entity at the cell it belongs to.
     *
     * @param entity entity to store, its position has to lie inside this chunk
     * @return the entity that was stored there before, {@code null} when the cell was free
     * @throws IllegalArgumentException when the position lies outside this chunk
     */
    public BlockEntity setBlockEntity(BlockEntity entity) {
        Objects.requireNonNull(entity, "entity");
        if (chunkOf(entity.x()) != chunkX || chunkOf(entity.y()) != chunkY) {
            throw new IllegalArgumentException("The block entity " + entity
                    + " does not belong to chunk (" + chunkX + ", " + chunkY + ")");
        }
        int slot = index(localOf(entity.x()), localOf(entity.y()), entity.layer());
        BlockEntity previous = blockEntities[slot];
        if (previous != null) {
            active.remove(previous);
        }
        blockEntities[slot] = entity;
        active.add(entity);
        dirty = true;
        return previous;
    }

    /**
     * Takes the block entity of a cell out of this chunk.
     *
     * @param localX local X coordinate
     * @param localY local Y coordinate
     * @param layer layer index
     * @return the entity that was there, {@code null} when the cell carried none
     */
    public BlockEntity removeBlockEntity(int localX, int localY, int layer) {
        int slot = index(localX, localY, layer);
        BlockEntity previous = blockEntities[slot];
        if (previous == null) {
            return null;
        }
        blockEntities[slot] = null;
        active.remove(previous);
        dirty = true;
        return previous;
    }

    /** Every block entity of this chunk, in the order they were put in. */
    public List<BlockEntity> blockEntities() {
        return Collections.unmodifiableList(active);
    }

    /** Amount of block entities this chunk holds. */
    public int blockEntityCount() {
        return active.size();
    }

    /** {@code true} when this chunk holds at least one block entity. */
    public boolean hasBlockEntities() {
        return !active.isEmpty();
    }

    /** {@code true} when the chunk changed since the last call to {@link #clearDirty()}. */
    public boolean isDirty() {
        return dirty;
    }

    /** Marks the chunk as unchanged, called once the renderer picked it up. */
    public void clearDirty() {
        dirty = false;
    }

    /**
     * {@code true} when the player changed this chunk, which means it has to be
     * written to the save game and may never be dropped from memory silently.
     */
    public boolean isModified() {
        return modified;
    }

    /** Remembers that the player changed this chunk. */
    public void markModified() {
        modified = true;
    }

    /**
     * Clears the player-change flag, called once the chunk reached the save game
     * or was refilled from it.
     */
    public void clearModified() {
        modified = false;
    }

    /**
     * {@code true} when the floor of a cell was already generated.
     *
     * @param localX local X coordinate
     * @param localY local Y coordinate
     */
    public boolean isCellGenerated(int localX, int localY) {
        return generatedCells[cellIndex(localX, localY)];
    }

    /**
     * Marks the floor of a cell as generated.
     *
     * @param localX local X coordinate
     * @param localY local Y coordinate
     */
    public void markCellGenerated(int localX, int localY) {
        int index = cellIndex(localX, localY);
        if (!generatedCells[index]) {
            generatedCells[index] = true;
            generatedCellCount++;
        }
    }

    /** {@code true} when the floor layer of every cell of this chunk exists. */
    public boolean isFullyGenerated() {
        return generatedCellCount == CELL_COUNT;
    }

    /** {@code true} when the decorations of this chunk were already planted. */
    public boolean isDecorated() {
        return decorated;
    }

    /**
     * {@code true} when the chunk is finished.
     * <p>
     * A chunk may exist without being finished, because a single block read
     * generates only the cell it touched. Such a chunk still misses floors and has
     * no decorations, so it has to be completed before the player sees it.
     */
    public boolean isComplete() {
        return isFullyGenerated() && decorated;
    }

    /** Remembers that the decorations of this chunk were planted. */
    public void markDecorated() {
        decorated = true;
    }

    /**
     * Verifies that a local coordinate is inside this chunk.
     *
     * @param localX local X coordinate
     * @param localY local Y coordinate
     * @return {@code true} when the coordinate can be used on this chunk
     */
    public static boolean contains(int localX, int localY) {
        return localX >= 0 && localX < CHUNK_SIZE && localY >= 0 && localY < CHUNK_SIZE;
    }

    /**
     * Converts a block coordinate into the local coordinate of its chunk.
     *
     * @param blockCoordinate block coordinate along any horizontal axis, may be
     *                        negative
     * @return a value between {@code 0} and {@code CHUNK_SIZE - 1}
     */
    public static int localOf(int blockCoordinate) {
        return Math.floorMod(blockCoordinate, CHUNK_SIZE);
    }

    /**
     * Converts a block coordinate into the coordinate of its chunk.
     *
     * @param blockCoordinate block coordinate along any horizontal axis, may be
     *                        negative
     * @return the chunk coordinate owning that block
     */
    public static int chunkOf(int blockCoordinate) {
        return Math.floorDiv(blockCoordinate, CHUNK_SIZE);
    }

    private static int cellIndex(int localX, int localY) {
        return localY * CHUNK_SIZE + localX;
    }

    private static int index(int localX, int localY, int layer) {
        if (layer < 0 || layer >= LAYERS) {
            throw new IndexOutOfBoundsException("Layer out of range: " + layer);
        }
        if (!contains(localX, localY)) {
            throw new IndexOutOfBoundsException(
                    "Block out of range: localX=" + localX + ", localY=" + localY);
        }
        return layer * CELL_COUNT + cellIndex(localX, localY);
    }
}

