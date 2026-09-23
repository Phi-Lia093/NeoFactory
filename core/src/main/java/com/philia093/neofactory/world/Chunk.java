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
 * A column of the world: sixteen blocks across, and as tall as the world is.
 * <p>
 * The flat engine kept two layers per cell and a chunk was a square of tiles, because the camera
 * looked straight down and a block was one picture. A world of cubes needs a height, and a column
 * of two hundred and fifty six blocks per cell would cost every chunk the same whether it holds a
 * mountain or a sky. A chunk is therefore a stack of {@link Section sections}, sixteen blocks tall
 * each, and a section that holds nothing but air keeps no array at all: the sky above the trees is
 * free, see {@link #section(int)}.
 * <p>
 * The chunk also remembers the highest block of every column. That height is what a generator
 * asks for when it wants to know where the ground is, what the sky light starts from and what a
 * spawn search walks along - and it is cheap to keep, because only a block that is placed higher
 * than the current height moves it.
 * <p>
 * <b>The flat view.</b> The game still <i>is</i> flat: it draws the world from above, the player
 * walks on a plane and a block is one picture. While that is true, the two cells of the old world
 * stand at {@link #flatY(int)} - the ground of a column and the cell the player walks in - and code
 * that still thinks in layers asks that method for the height it means.
 * <p>
 * <b>A cell is addressed by its height.</b> The storage of a column is the storage a world of cubes
 * needs: {@link #getBlock(int, int, int)} names the height of a cell, so the flat view is a layer of
 * the very same cells and needs no methods of its own. Only three parts of it are left -
 * {@link #flatY(int)}, {@link #flatLayer(int)} and {@link #isFlatHeight(int)} - and they go away
 * with the view, once the game is played standing in the world instead of looking down on it.
 * <p>
 * A cell stores the id of a block and its state; the id is resolved into the actual {@link Block}
 * through the {@link BlockRegistry}, which is what keeps a section at eight kilobytes.
 */
public final class Chunk {

    /** Number of layers of the flat view of this chunk. */
    public static final int LAYERS = 2;

    /** Index of the generated ground layer of the flat view. */
    public static final int LAYER_FLOOR = Constants.LAYER_FLOOR;

    /** Index of the object layer of the flat view, the layer the player stands in. */
    public static final int LAYER_OBJECT = Constants.LAYER_OBJECT;

    /**
     * Height the flat view of the world stands on.
     * <p>
     * The ground of a column is at this height and the layer the player stands in is one above it.
     * A world that is filled from the bottom up puts its surface where the noise says it belongs and
     * forgets this number altogether.
     */
    public static final int LAYER_BASE_Y = Constants.SEA_LEVEL;

    /** Amount of columns stored by a single chunk. */
    public static final int CELL_COUNT = CHUNK_SIZE * CHUNK_SIZE;

    /** Value of the height map for a column that holds no block at all. */
    public static final int NO_BLOCK = -1;

    private final int chunkX;
    private final int chunkZ;

    /** Sections of this column, {@code null} where one holds nothing but air. */
    private final Section[] sections = new Section[Constants.SECTION_COUNT];

    /**
     * Height of the highest block of every column, {@link #NO_BLOCK} for an empty one.
     * <p>
     * A short rather than a byte, because the top of the world does not fit into a signed one, see
     * {@link Constants#MAX_Y}.
     */
    private final short[] heightMap = new short[CELL_COUNT];

    /**
     * Block entity of every cell of this column, {@code null} for a cell that carries none.
     * <p>
     * The array is the lookup: a machine standing somewhere is found without walking anything.
     * {@link #active} is what a tick walks instead, because the list holds the entities that are
     * really there - a chunk with a single machine does not make the world look at every cell of
     * every section every tick.
     */
    private final BlockEntity[] blockEntities =
            new BlockEntity[CELL_COUNT * Constants.SECTION_COUNT];

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
     * @param chunkZ chunk coordinate along the Z axis, same rule as {@code chunkX}
     */
    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        for (int column = 0; column < CELL_COUNT; column++) {
            heightMap[column] = NO_BLOCK;
        }
    }

    /** Chunk coordinate along the X axis. */
    public int chunkX() {
        return chunkX;
    }

    /** Chunk coordinate along the Z axis, the second horizontal axis of the world. */
    public int chunkZ() {
        return chunkZ;
    }

    /** Block X coordinate of the first column of this chunk. */
    public int originX() {
        return chunkX * CHUNK_SIZE;
    }

    /** Block Z coordinate of the first column of this chunk. */
    public int originZ() {
        return chunkZ * CHUNK_SIZE;
    }

    /**
     * The section of this chunk that covers a height.
     *
     * @param sectionY index of the section along the vertical axis
     * @return the section, or {@code null} while it holds nothing but air
     */
    public Section section(int sectionY) {
        return sections[sectionY];
    }

    /** Amount of sections of this chunk that hold at least one block. */
    public int sectionCount() {
        int count = 0;
        for (Section section : sections) {
            if (section != null && !section.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * {@code true} while a section of this chunk holds nothing but air.
     * <p>
     * A section that holds nothing keeps no array, which is what makes the air above a landscape
     * free. A caller that walks the column of a chunk asks this before it looks at the section, and
     * a chunk that is stored writes only the sections that really hold something.
     *
     * @param sectionY index of the section along the vertical axis
     * @return {@code true} when the section carries no block
     */
    public boolean isEmptySection(int sectionY) {
        Section section = sections[sectionY];
        return section == null || section.isEmpty();
    }

    /**
     * Height of the highest block of a column.
     *
     * @param localX local X coordinate
     * @param localZ local Z coordinate
     * @return the block Y coordinate of the highest block, or {@link #NO_BLOCK} for a column that
     *         holds nothing at all
     */
    public int highestBlockY(int localX, int localZ) {
        return heightMap[cellIndex(localX, localZ)];
    }

    /** {@code true} when a column of this chunk holds at least one block. */
    public boolean hasBlock(int localX, int localZ) {
        return highestBlockY(localX, localZ) != NO_BLOCK;
    }

    /**
     * Block of one cell of this column.
     *
     * @param localX local X coordinate, {@code 0 <= localX < CHUNK_SIZE}
     * @param localY local Y coordinate, {@code MIN_Y} to {@code MAX_Y}
     * @param localZ local Z coordinate, {@code 0 <= localZ < CHUNK_SIZE}
     * @return the stored block, {@link Blocks#AIR} for empty space
     */
    public Block getBlock(int localX, int localY, int localZ) {
        return BlockRegistry.byId(rawId(localX, localY, localZ));
    }

    /**
     * Stores a block in one cell of this column and marks the chunk for redrawing.
     *
     * @param localX local X coordinate, {@code 0 <= localX < CHUNK_SIZE}
     * @param localY local Y coordinate, {@code MIN_Y} to {@code MAX_Y}
     * @param localZ local Z coordinate, {@code 0 <= localZ < CHUNK_SIZE}
     * @param block block to store, passing {@link Blocks#AIR} clears the cell
     */
    public void setBlock(int localX, int localY, int localZ, Block block) {
        setRawId(localX, localY, localZ, block.id());
    }

    /**
     * Raw id of one cell of this column, used by serialization and world generation.
     *
     * @param localX local X coordinate
     * @param localY local Y coordinate
     * @param localZ local Z coordinate
     * @return the numeric block id, {@link Blocks#AIR_ID} for an empty cell
     * @throws IndexOutOfBoundsException when the height is outside the world
     */
    public int rawId(int localX, int localY, int localZ) {
        checkY(localY);
        Section section = sections[localY / Section.SIZE];
        return section == null ? Blocks.AIR_ID
                : section.rawId(localX, localY % Section.SIZE, localZ);
    }

    /**
     * Writes a raw block id into one cell of this column.
     * <p>
     * A cell whose block changes loses its state: the state of the block that was there belongs to
     * that block and means nothing for the one that takes its place. A block that is placed above
     * the highest one of its column becomes the new highest, and clearing the highest one makes the
     * chunk look for the next block below it, see {@link #highestBlockY(int, int)}.
     *
     * @param localX local X coordinate
     * @param localY local Y coordinate
     * @param localZ local Z coordinate
     * @param id numeric block id
     * @throws IndexOutOfBoundsException when the height is outside the world
     */
    public void setRawId(int localX, int localY, int localZ, int id) {
        checkY(localY);
        int sectionY = localY / Section.SIZE;
        Section section = sections[sectionY];
        if (section == null) {
            if (id == Blocks.AIR_ID) {
                return;
            }
            section = new Section(sectionY);
            sections[sectionY] = section;
        }
        int insideY = localY % Section.SIZE;
        boolean wasFilled = section.rawId(localX, insideY, localZ) != Blocks.AIR_ID;
        section.setRawId(localX, insideY, localZ, id);
        boolean filled = id != Blocks.AIR_ID;
        if (wasFilled != filled) {
            updateHeight(localX, localZ, localY, filled);
        }
        dirty = true;
    }

    /**
     * State of one cell of this column.
     *
     * @param localX local X coordinate
     * @param localY local Y coordinate
     * @param localZ local Z coordinate
     * @return the state, {@code 0} for a cell nothing wrote a state to
     */
    public int state(int localX, int localY, int localZ) {
        checkY(localY);
        Section section = sections[localY / Section.SIZE];
        return section == null ? 0 : section.state(localX, localY % Section.SIZE, localZ);
    }

    /**
     * Writes the state of one cell of this column.
     *
     * @param localX local X coordinate
     * @param localY local Y coordinate
     * @param localZ local Z coordinate
     * @param state state to store
     */
    public void setState(int localX, int localY, int localZ, int state) {
        checkY(localY);
        int sectionY = localY / Section.SIZE;
        Section section = sections[sectionY];
        if (section == null) {
            if (state == 0) {
                return;
            }
            section = new Section(sectionY);
            sections[sectionY] = section;
        }
        section.setState(localX, localY % Section.SIZE, localZ, state);
        dirty = true;
    }

    /**
     * Block entity of one cell of this column.
     *
     * @param localX local X coordinate
     * @param localY local Y coordinate
     * @param localZ local Z coordinate
     * @return the entity, or {@code null} when the cell carries none
     */
    public BlockEntity blockEntity(int localX, int localY, int localZ) {
        return blockEntities[blockEntitySlot(localX, localY, localZ)];
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
        if (chunkOf(entity.x()) != chunkX || chunkOf(entity.z()) != chunkZ) {
            throw new IllegalArgumentException("The block entity " + entity
                    + " does not belong to chunk (" + chunkX + ", " + chunkZ + ")");
        }
        int slot = blockEntitySlot(localOf(entity.x()), entity.y(), localOf(entity.z()));
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
     * Takes the block entity of one cell of this column out of this chunk.
     *
     * @param localX local X coordinate
     * @param localY local Y coordinate
     * @param localZ local Z coordinate
     * @return the entity that was there, {@code null} when the cell carried none
     */
    public BlockEntity removeBlockEntity(int localX, int localY, int localZ) {
        int slot = blockEntitySlot(localX, localY, localZ);
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

    /**
     * Height the flat view of a layer stands at.
     *
     * @param layer layer index, see {@link #LAYER_FLOOR} and {@link #LAYER_OBJECT}
     * @return the block Y coordinate of that layer
     * @throws IndexOutOfBoundsException when no layer carries that index
     */
    public static int flatY(int layer) {
        if (layer < 0 || layer >= LAYERS) {
            throw new IndexOutOfBoundsException("Layer out of range: " + layer);
        }
        return LAYER_BASE_Y + layer;
    }

    /**
     * {@code true} when a height is one of the layers of the flat view.
     * <p>
     * The game is drawn from above and only ever writes the base height and the cell above it, see
     * {@link #flatY(int)}, so every other height is not part of the flat view yet. A reader that
     * stumbles over a cell outside those two layers reports it instead of mapping it to a layer that
     * does not exist.
     *
     * @param blockY block Y coordinate to test
     * @return {@code true} when a layer of the flat view stands at that height
     */
    public static boolean isFlatHeight(int blockY) {
        return blockY >= LAYER_BASE_Y && blockY < LAYER_BASE_Y + LAYERS;
    }

    /**
     * Layer the flat view of the world stands at a height, the inverse of {@link #flatY(int)}.
     *
     * @param blockY block Y coordinate, see {@link #flatY(int)}
     * @return the layer index that stands at that height
     * @throws IndexOutOfBoundsException when no layer of the flat view stands there
     */
    public static int flatLayer(int blockY) {
        if (!isFlatHeight(blockY)) {
            throw new IndexOutOfBoundsException("No layer of the flat view at height " + blockY);
        }
        return blockY - LAYER_BASE_Y;
    }

    /**
     * Keeps the highest block of a column up to date.
     * <p>
     * A block that is placed above the current height becomes the new one. Clearing the highest
     * block makes the chunk walk down the column until it finds the next block, which is what keeps
     * the height honest when the player digs the surface away: the ground of the column is then the
     * block that was below it.
     *
     * @param localX local X coordinate
     * @param localZ local Z coordinate
     * @param localY height that changed
     * @param filled {@code true} when a block took the cell, {@code false} when it was cleared
     */
    private void updateHeight(int localX, int localZ, int localY, boolean filled) {
        int column = cellIndex(localX, localZ);
        int highest = heightMap[column];
        if (filled) {
            if (localY > highest) {
                heightMap[column] = (short) localY;
            }
            return;
        }
        if (localY != highest) {
            return;
        }
        for (int y = localY - 1; y >= Constants.MIN_Y; y--) {
            Section section = sections[y / Section.SIZE];
            if (section != null && section.rawId(localX, y % Section.SIZE, localZ) != Blocks.AIR_ID) {
                heightMap[column] = (short) y;
                return;
            }
        }
        heightMap[column] = (short) NO_BLOCK;
    }

    /**
     * Verifies that a height lies inside the world.
     *
     * @param localY local Y coordinate to check
     * @throws IndexOutOfBoundsException when the height is outside the world
     */
    private static void checkY(int localY) {
        if (localY < Constants.MIN_Y || localY > Constants.MAX_Y) {
            throw new IndexOutOfBoundsException("Height outside the world: " + localY);
        }
    }

    /**
     * Slot of a block entity inside the lookup array of this chunk.
     *
     * @param localX local X coordinate
     * @param localY local Y coordinate
     * @param localZ local Z coordinate
     * @return the slot, one per cell of the section the height falls into
     */
    private static int blockEntitySlot(int localX, int localY, int localZ) {
        checkY(localY);
        return (localY / Section.SIZE) * CELL_COUNT + cellIndex(localX, localZ);
    }

    private static int cellIndex(int localX, int localZ) {
        if (!contains(localX, localZ)) {
            throw new IndexOutOfBoundsException(
                    "Column out of range: localX=" + localX + ", localZ=" + localZ);
        }
        return localZ * CHUNK_SIZE + localX;
    }
}

