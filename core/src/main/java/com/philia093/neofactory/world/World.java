package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.blockentity.BlockEntity;
import com.philia093.neofactory.entity.EntityManager;
import com.philia093.neofactory.fluid.FluidFlow;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.philia093.neofactory.util.Constants.SPAWN_CHUNK_RADIUS;

/**
 * The block storage of the game.
 * <p>
 * A world is an unbounded plane of {@link Chunk chunks} addressed by two
 * horizontal chunk coordinates. Chunks are created on demand while the player
 * walks around, see {@link #loadChunksAround(float, float, int, int)}, and dropped
 * again once the player walked away, see {@link #unloadChunksOutside(int, int, int)}.
 * <p>
 * Dropping a chunk is only safe when its content can be produced again, so the
 * world keeps two kinds of chunks apart:
 * <ul>
 *     <li>a chunk the player never touched is rebuilt from the seed by the
 *         generator, which is deterministic for every cell</li>
 *     <li>a chunk the player changed is written into the {@link ChunkStore} before
 *         it leaves memory, and read back from there when the player returns</li>
 * </ul>
 * Memory use therefore follows the view distance instead of the distance walked,
 * and a chunk on disk is never the only copy of anything the player built.
 * <p>
 * Generation is intentionally split into a pure noise phase followed by a
 * decoration phase, see {@link WorldGen}. Because no generation step ever reads a
 * block of a chunk, loading a chunk can never trigger the loading of another one
 * and the recursive generation that used to blow the stack cannot happen. The
 * {@code generating} guard is kept nonetheless as a cheap safety net for future
 * generators that do look at their neighbours.
 */
public final class World implements BlockAccess {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Radius in blocks searched for a safe spawn position. */
    private static final int SPAWN_SEARCH_RADIUS = 32;

    private final int seed;

    /** Land this world is made of, see {@link WorldType}. */
    private final WorldType type;

    private final WorldGen generator;
    private final Map<Long, Chunk> chunks = new LinkedHashMap<>();

    /**
     * Where changed chunks are written when they leave memory, {@code null} for a
     * world that was not saved yet.
     * <p>
     * Without a store a changed chunk is never dropped: throwing it away would
     * lose the only copy of what the player built. The store is attached when a
     * world is opened or saved for the first time, see
     * {@link com.philia093.neofactory.world.save.WorldSaver}.
     */
    private ChunkStore store;

    /** Everything in this world that is not a block. */
    private final EntityManager entities = new EntityManager();

    /** Chunks currently being generated, used to detect re-entrant loading. */
    private final Set<Long> generating = new HashSet<>();

    private final int spawnX;
    private final int spawnZ;

    /** Fluids that run through this world, see {@link FluidFlow}. */
    private final FluidFlow fluids = new FluidFlow();

    /** Ticks this world has run, the clock of everything that works on a schedule. */
    private long tickCount;

    /**
     * Creates a world and prepares the chunks around the spawn point.
     *
     * @param seed world seed, selects the generated terrain
     */
    public World(int seed) {
        this(seed, 0, 0, null, WorldType.NORMAL);
    }

    /**
     * Creates a world and prepares the chunks around the spawn point.
     *
     * @param seed world seed, selects the generated terrain
     * @param spawnX block X coordinate the player would like to start near
     * @param spawnZ block Y coordinate the player would like to start near
     */
    public World(int seed, int spawnX, int spawnZ) {
        this(seed, spawnX, spawnZ, null);
    }

    /**
     * Creates a world and prepares the chunks around the spawn point.
     * <p>
     * The store is passed into the constructor instead of being set afterwards,
     * because the spawn area is prepared right here: a chunk that exists in the
     * store has to be read from it before anything generates over it, or the
     * changes of the player would be replaced by terrain the seed produces.
     *
     * @param seed world seed, selects the generated terrain
     * @param spawnX block X coordinate the player would like to start near
     * @param spawnZ block Y coordinate the player would like to start near
     * @param store store holding the chunks of this world, may be {@code null}
     */
    public World(int seed, int spawnX, int spawnZ, ChunkStore store) {
        this(seed, spawnX, spawnZ, store, WorldType.NORMAL);
    }

    /**
     * Creates a world of a chosen kind of land and prepares the chunks around the spawn point.
     * <p>
     * The type decides what the generator makes of every column, see {@link WorldGen}: the landscape
     * of the seed or the table of blocks a flat world is. It belongs to the world from the first
     * chunk on, because a chunk is generated exactly once - a world that is opened again is built
     * from the type it was stored with and never from another one.
     *
     * @param seed world seed, selects the generated terrain
     * @param spawnX block X coordinate the player would like to start near
     * @param spawnZ block Y coordinate the player would like to start near
     * @param store store holding the chunks of this world, may be {@code null}
     * @param type land to generate, {@code null} generates the landscape of the seed
     */
    public World(int seed, int spawnX, int spawnZ, ChunkStore store, WorldType type) {
        this.seed = seed;
        this.type = type == null ? WorldType.NORMAL : type;
        this.generator = new WorldGen(seed, this.type);
        this.store = store;

        // Biomes cover large areas, so the requested point is only a hint: the
        // nearest grassy cell is used instead, which keeps the start of the game
        // inside a landscape that can carry trees and grass.
        int[] spawn = generator.findSpawnBiome(spawnX, spawnZ);
        this.spawnX = spawn[0];
        this.spawnZ = spawn[1];
        if (this.spawnX != spawnX || this.spawnZ != spawnZ) {
            LOGGER.info("Spawn moved from ({}, {}) to ({}, {}) to reach a grassy biome",
                    spawnX, spawnZ, this.spawnX, this.spawnZ);
        }

        // The area is finished before anything looks for a free cell inside it:
        // an unfinished chunk would plant its trees later and could bury whatever
        // already stands there, the player included.
        ensureChunksAround(this.spawnX, this.spawnZ, SPAWN_CHUNK_RADIUS);
        LOGGER.info("World {} created with {} chunks around spawn ({}, {})",
                seed, chunks.size(), this.spawnX, this.spawnZ);
    }

    /** Seed this world was generated from. */
    public int seed() {
        return seed;
    }

    /** Land this world is made of, see {@link WorldType}. */
    public WorldType worldType() {
        return type;
    }

    /** Block X coordinate the player starts near. */
    public int spawnX() {
        return spawnX;
    }

    /** Block Y coordinate the player starts near. */
    public int spawnZ() {
        return spawnZ;
    }

    /** Generator producing the terrain of this world. */
    public WorldGen generator() {
        return generator;
    }

    /** Amount of chunks currently held in memory. */
    public int chunkCount() {
        return chunks.size();
    }

    /** Every loaded chunk, in insertion order. */
    public Collection<Chunk> chunks() {
        return Collections.unmodifiableCollection(chunks.values());
    }

    /**
     * Entities of this world.
     * <p>
     * The list belongs to the world instead of a chunk, so nothing an entity does
     * depends on which chunks happen to be in memory. Save games store it together
     * with the level file, see
     * {@link com.philia093.neofactory.world.save.WorldSaver}.
     */
    public EntityManager entities() {
        return entities;
    }

    /** Amount of chunks in memory whose content the player changed. */
    public int modifiedChunkCount() {
        int count = 0;
        for (Chunk chunk : chunks.values()) {
            if (chunk.isModified()) {
                count++;
            }
        }
        return count;
    }

    /** Amount of chunks waiting in the store, {@code 0} without a store. */
    public int storedChunkCount() {
        return store == null ? 0 : store.storedChunkCount();
    }

    /** Store this world writes changed chunks into, may be {@code null}. */
    public ChunkStore chunkStore() {
        return store;
    }

    /**
     * Points this world at the store of its save game.
     * <p>
     * Called while opening a world and before saving one. Attaching a store in the
     * middle of a session is safe: chunks that are already in memory stay there and
     * are written the next time they are dropped or saved.
     *
     * @param store store to use, {@code null} keeps every changed chunk in memory
     */
    public void attachChunkStore(ChunkStore store) {
        this.store = store;
    }

    /**
     * Returns a chunk only when it is already loaded.
     * <p>
     * The renderer uses this instead of {@link #loadChunk(int, int)} so that
     * drawing the frame never generates terrain.
     *
     * @param chunkX chunk coordinate along the first horizontal axis
     * @param chunkY chunk coordinate along the second horizontal axis
     * @return the loaded chunk or {@code null} when it is not in memory
     */
    public Chunk chunkIfLoaded(int chunkX, int chunkY) {
        return chunks.get(chunkKey(chunkX, chunkY));
    }

    /**
     * Loads a chunk, generating it when it is not ready yet.
     *
     * @param chunkX chunk coordinate along the first horizontal axis
     * @param chunkY chunk coordinate along the second horizontal axis
     * @return the loaded chunk, never {@code null}
     */
    public Chunk loadChunk(int chunkX, int chunkY) {
        long key = chunkKey(chunkX, chunkY);
        Chunk chunk = chunkForWrite(chunkX, chunkY);
        if (!generating.add(key)) {
            // Re-entrant call: the outer invocation is still filling this chunk,
            // so the caller gets the handle early instead of a second instance.
            return chunk;
        }
        try {
            generator.generateFloor(chunk);
            completeGeneration(chunk);
        } finally {
            generating.remove(key);
        }
        // A chunk that comes back has to tell the fluids where they stand: while it was out of
        // memory nothing ran, so a lake that was left half spilled has to finish.
        fluids.noticeLoaded(this, chunk);
        return chunk;
    }

    /**
     * Makes sure every chunk inside a square around a block position is finished.
     * <p>
     * Called by the game loop after the player moved, so that walking keeps
     * revealing new terrain. A chunk that already exists is only skipped when it is
     * {@link Chunk#isComplete() complete}: a chunk that was created by reading a
     * single block still misses floors, and if it were skipped it would be filled
     * in later, block by block, which would plant its trees right next to or even
     * on top of the player.
     *
     * @param blockX block X coordinate to center the square on
     * @param blockY block Y coordinate to center the square on
     * @param chunkRadius amount of chunks loaded in every direction
     */
    public void ensureChunksAround(float blockX, float blockY, int chunkRadius) {
        loadChunksAround(blockX, blockY, chunkRadius, Integer.MAX_VALUE);
    }

    /**
     * Loads the chunks inside a square around a block position.
     * <p>
     * The chunks are visited in rings that grow outwards, so the terrain closest to
     * the player appears first and a limited {@code maxChunks} budget spends itself
     * on what the player is about to see. The budget is what keeps walking from
     * costing one long frame instead of a few short ones.
     *
     * @param blockX block X coordinate to center the square on
     * @param blockY block Y coordinate to center the square on
     * @param chunkRadius amount of chunks loaded in every direction
     * @param maxChunks maximum amount of chunks to load in this call
     * @return amount of chunks that were loaded
     */
    public int loadChunksAround(float blockX, float blockY, int chunkRadius, int maxChunks) {
        int centerChunkX = Chunk.chunkOf((int) Math.floor(blockX));
        int centerChunkY = Chunk.chunkOf((int) Math.floor(blockY));
        int loaded = 0;
        for (int ring = 0; ring <= chunkRadius; ring++) {
            for (int chunkY = centerChunkY - ring; chunkY <= centerChunkY + ring; chunkY++) {
                for (int chunkX = centerChunkX - ring; chunkX <= centerChunkX + ring;
                        chunkX++) {
                    // Only the border of the ring is new, the inside was loaded by
                    // the rings before it.
                    if (Math.max(Math.abs(chunkX - centerChunkX),
                            Math.abs(chunkY - centerChunkY)) != ring) {
                        continue;
                    }
                    Chunk chunk = chunks.get(chunkKey(chunkX, chunkY));
                    if (chunk != null && chunk.isComplete()) {
                        continue;
                    }
                    if (loaded >= maxChunks) {
                        return loaded;
                    }
                    loadChunk(chunkX, chunkY);
                    loaded++;
                }
            }
        }
        return loaded;
    }

    /**
     * Drops every chunk outside a square around a chunk position.
     * <p>
     * The order inside this method is the whole point of it: a chunk the player
     * changed is written into the store <em>before</em> it leaves memory, so a
     * crash right after an unload cannot lose what the player built. A chunk that
     * was never changed is simply dropped and generated again from the seed when
     * the player comes back, which the generator reproduces exactly because every
     * cell, decorations included, only depends on the seed and its coordinates.
     * <p>
     * Without a store a changed chunk is kept: it is the only copy of the change
     * until the world is saved, and a missing store only happens before the first
     * save of a new world.
     *
     * @param centerChunkX chunk coordinate along the first horizontal axis to keep
     * @param centerChunkY chunk coordinate along the second horizontal axis to keep
     * @param keepRadius amount of chunks kept around the center
     * @return amount of chunks that were dropped
     */
    public int unloadChunksOutside(int centerChunkX, int centerChunkY, int keepRadius) {
        int unloaded = 0;
        Iterator<Map.Entry<Long, Chunk>> iterator = chunks.entrySet().iterator();
        while (iterator.hasNext()) {
            Chunk chunk = iterator.next().getValue();
            if (Math.abs(chunk.chunkX() - centerChunkX) <= keepRadius
                    && Math.abs(chunk.chunkZ() - centerChunkY) <= keepRadius) {
                continue;
            }
            if (chunk.isModified()) {
                if (store == null) {
                    // Nowhere to write the change to, so keeping it is the only way
                    // not to lose it. The next save attaches the store.
                    continue;
                }
                store.persist(chunk);
            }
            iterator.remove();
            unloaded++;
        }
        if (unloaded > 0) {
            LOGGER.debug("Dropped {} chunks outside ({}, {}) +- {}", unloaded, centerChunkX,
                    centerChunkY, keepRadius);
        }
        return unloaded;
    }

    /**
     * Writes every changed chunk into the store.
     * <p>
     * Called while saving the world. The level file no longer holds any chunk, so a
     * save costs what the player changed since the last one instead of what is
     * loaded.
     *
     * @return amount of chunks that were written
     */
    public int persistModifiedChunks() {
        if (store == null) {
            int pending = modifiedChunkCount();
            if (pending > 0) {
                LOGGER.warn("No chunk store is attached, {} changed chunks stay in memory",
                        pending);
            }
            return 0;
        }
        int written = 0;
        for (Chunk chunk : chunks.values()) {
            if (chunk.isModified()) {
                store.persist(chunk);
                written++;
            }
        }
        return written;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        if (outsideTheWorld(y)) {
            // There is nothing outside the world, and a reader that asks anyway - a mesh looking under
            // the lowest block of a section, a body at the very bottom - is told so instead of stopped.
            return Blocks.AIR;
        }
        Chunk chunk = preparedChunk(x, z);
        return chunk.getBlock(Chunk.localOf(x), y, Chunk.localOf(z));
    }

    /**
     * {@code true} when a height lies outside the world.
     * <p>
     * The world is a column of sections from {@link Constants#MIN_Y} to {@link Constants#MAX_Y}; every
     * reader of a block asks about a neighbour of one it already has, so one cell beyond either end is a
     * question that has to be answered rather than refused.
     *
     * @param y block Y coordinate, the height
     * @return {@code true} when nothing can stand at that height
     */
    public static boolean outsideTheWorld(int y) {
        return y < Constants.MIN_Y || y > Constants.MAX_Y;
    }

    @Override
    public void setBlock(int x, int y, int z, Block block) {
        Objects.requireNonNull(block, "block");
        Chunk chunk = preparedChunk(x, z);
        int localX = Chunk.localOf(x);
        int localZ = Chunk.localOf(z);
        BlockEntity previous = chunk.blockEntity(localX, y, localZ);
        if (previous != null && !previous.type().name().equals(block.blockEntityTypeName())) {
            // The block that carried the entity is being replaced, so the entity goes with
            // it: a machine that stayed behind would keep running where nothing stands.
            chunk.removeBlockEntity(localX, y, localZ);
        }
        chunk.setBlock(localX, y, localZ, block);
        // This is the public write path of the world: everything reaching it is a
        // player change and has to survive unloading and saving.
        chunk.markModified();
        // Water and lava are looked at again where something happened: a wall built through a lake holds
        // the water back, a hole in that wall lets it through, and a source that was taken away stops
        // feeding the water that lived from it.
        fluids.mark(this, x, y, z);
    }

    /**
     * Reads a block without generating anything.
     * <p>
     * Returns {@link Blocks#AIR} for unloaded chunks. Used by the decorators,
     * which must be able to look at a possibly missing neighbour without pulling
     * the whole neighbourhood into memory.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @return the stored block, {@link Blocks#AIR} when the chunk is not loaded
     */
    public Block peekBlock(int x, int y, int z) {
        if (outsideTheWorld(y)) {
            // The mesh of the lowest section looks under its own blocks, see SectionMeshCache: outside the
            // world there is nothing, which is air and not an error.
            return Blocks.AIR;
        }
        Chunk chunk = chunks.get(chunkKey(Chunk.chunkOf(x), Chunk.chunkOf(z)));
        if (chunk == null) {
            return Blocks.AIR;
        }
        return chunk.getBlock(Chunk.localOf(x), y, Chunk.localOf(z));
    }

    @Override
    public int getState(int x, int y, int z) {
        if (outsideTheWorld(y)) {
            // Nothing stands outside the world, so nothing carries a state there either.
            return 0;
        }
        Chunk chunk = preparedChunk(x, z);
        return chunk.state(Chunk.localOf(x), y, Chunk.localOf(z));
    }

    @Override
    public void setState(int x, int y, int z, int state) {
        Chunk chunk = preparedChunk(x, z);
        chunk.setState(Chunk.localOf(x), y, Chunk.localOf(z), state);
    }

    /**
     * Height the surface of a column stands at, the cell a body stands in.
     * <p>
     * This is what a spawn, a teleport and a drop ask for: the cell above the highest block of the
     * column, which is the ground a body walks on and not the block that ground is made of.
     *
     * @param x block X coordinate
     * @param z block Z coordinate
     * @return the height of the cell above the highest block, {@code MIN_Y} for an empty column
     */
    public int surfaceY(int x, int z) {
        Chunk chunk = preparedChunk(x, z);
        int highest = chunk.highestBlockY(Chunk.localOf(x), Chunk.localOf(z));
        return highest == Chunk.NO_BLOCK ? Constants.MIN_Y : highest + 1;
    }

    /**
     * Reads the state of a cell without generating anything.
     * <p>
     * Returns {@code 0} for unloaded chunks, so a renderer or a network may ask a
     * cell next to the player what it holds without pulling a whole chunk into
     * memory, see {@link #peekBlock(int, int, int)}.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @return the stored state, {@code 0} when the chunk is not loaded
     */
    public int peekState(int x, int y, int z) {
        if (outsideTheWorld(y)) {
            return 0;
        }
        Chunk chunk = chunks.get(chunkKey(Chunk.chunkOf(x), Chunk.chunkOf(z)));
        if (chunk == null) {
            return 0;
        }
        return chunk.state(Chunk.localOf(x), y, Chunk.localOf(z));
    }

    /**
     * Returns the block entity of a cell without generating anything.
     * <p>
     * This is what asks a machine what it holds, and what the interaction code asks
     * before a block is broken, so it never pulls a chunk into memory: a cell whose chunk
     * is not loaded has no entity to report.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @return the entity, or {@code null} when the cell carries none
     */
    public BlockEntity blockEntity(int x, int y, int z) {
        Chunk chunk = chunks.get(chunkKey(Chunk.chunkOf(x), Chunk.chunkOf(z)));
        if (chunk == null) {
            return null;
        }
        return chunk.blockEntity(Chunk.localOf(x), y, Chunk.localOf(z));
    }

    /**
     * Puts a block entity into the world at the cell it was placed at.
     * <p>
     * The cell counts as a player change, because a machine carries what somebody put
     * into it, so it has to survive unloading and saving like a block does.
     *
     * @param entity entity to add, its position is already set
     * @return the entity that was stored there before, {@code null} when the cell was free
     */
    public BlockEntity addBlockEntity(BlockEntity entity) {
        Objects.requireNonNull(entity, "entity");
        Chunk chunk = preparedChunk(entity.x(), entity.z());
        BlockEntity previous = chunk.setBlockEntity(entity);
        chunk.markModified();
        return previous;
    }

    /**
     * Takes the block entity of a cell out of the world.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @return the entity that was removed, {@code null} when the cell carried none
     */
    public BlockEntity removeBlockEntity(int x, int y, int z) {
        Chunk chunk = chunks.get(chunkKey(Chunk.chunkOf(x), Chunk.chunkOf(z)));
        if (chunk == null) {
            return null;
        }
        BlockEntity removed = chunk.removeBlockEntity(Chunk.localOf(x), y, Chunk.localOf(z));
        if (removed != null) {
            chunk.markModified();
        }
        return removed;
    }




    /** Amount of block entities in the chunks that are loaded right now. */
    public int blockEntityCount() {
        int total = 0;
        for (Chunk chunk : chunks.values()) {
            total += chunk.blockEntityCount();
        }
        return total;
    }

    /** Chunks that hold block entities, reused by every tick. */
    private final List<Chunk> ticking = new ArrayList<>();

    /**
     * Advances every block entity of the loaded chunks by one tick.
     * <p>
     * Only loaded chunks are ticked, so the work of the world follows the view distance
     * instead of the distance walked, exactly like the terrain does. A chunk that is
     * dropped from memory takes its machines with it: they are written into their file
     * first, see {@link #unloadChunksOutside(int, int, int)}, and start working again
     * where they left off once the player returns.
     * <p>
     * The chunks are collected before anything is ticked and the entities are walked by
     * index, because a machine is allowed to change the world while it works: it may start
     * a machine next to it or load a chunk, and neither may break the loop that runs it.
     *
     * @param tickDelta length of one tick in seconds, see {@link TickClock#TICK_SECONDS}
     * @return amount of block entities that were advanced
     */
    public int tick(float tickDelta) {
        if (tickDelta <= 0.0f) {
            return 0;
        }
        tickCount++;
        ticking.clear();
        for (Chunk chunk : chunks.values()) {
            if (chunk.hasBlockEntities()) {
                ticking.add(chunk);
            }
        }
        int ticked = 0;
        for (Chunk chunk : ticking) {
            List<BlockEntity> entities = chunk.blockEntities();
            for (int index = 0; index < entities.size(); index++) {
                entities.get(index).tick(this, tickDelta);
                ticked++;
            }
        }
        // Fluids take their turn after the machines: a machine that pours water into the world
        // changes a block, and that change has to be seen by the fluid in the same tick.
        fluids.tick(this, tickCount);
        return ticked;
    }

    /**
     * Ticks this world has run.
     * <p>
     * The count advances once per {@link #tick(float)}, so everything that works on a schedule -
     * the frames of an animated block, the ring a fluid is about to fill - reads the same clock
     * and stays in step.
     *
     * @return amount of ticks since the world was created or opened
     */
    public long tickCount() {
        return tickCount;
    }

    /** The fluids of this world, the side a bucket talks to, see {@link FluidFlow}. */
    public FluidFlow fluids() {
        return fluids;
    }


    /**
     * Writes into the object layer, allocating a missing chunk without generating
     * it.
     * <p>
     * Used while planting decorations: a canopy may reach into a neighbouring
     * chunk that was not generated yet. Such a chunk only lacks its floor, which
     * the generator fills later without touching the object layer.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @param block block to store
     */
    public void setObjectBlock(int x, int y, Block block) {
        Chunk chunk = chunkForWrite(Chunk.chunkOf(x), Chunk.chunkOf(y));
        // Raw ids instead of setBlock: this is the decoration path, a chunk grown
        // from the seed alone must not count as a player change.
        chunk.setRawId(Chunk.localOf(x), surfaceY(x, y), Chunk.localOf(y), block.id());
    }

    /**
     * Writes a block of the object layer into one cell of the world.
     * <p>
     * This is what a decoration that is taller than one block writes with, such as the trunk of a tree.
     * The cell is named by its height and not by the ground of its column, see
     * {@link #placeObjectIfAirAt(int, int, int, Block)}.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @param block block to store
     */
    public void setObjectBlockAt(int x, int y, int z, Block block) {
        Chunk chunk = chunkForWrite(Chunk.chunkOf(x), Chunk.chunkOf(z));
        // Decoration path, see setObjectBlock: never marks the chunk as modified.
        chunk.setRawId(Chunk.localOf(x), y, Chunk.localOf(z), block.id());
    }

    /**
     * Writes into the object layer together with the state of the cell.
     * <p>
     * The state is what a block carries beyond its id, and a fluid cannot do without it: a cell of
     * water is either the source a lake grows from or a cell that source reached, and nothing in
     * the game could tell the two apart from the id alone, see {@code FluidState#pack()}.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @param block block to store
     * @param meta state that belongs to the block, {@code 0} when it carries none
     */
    public void setObjectBlock(int x, int y, Block block, int meta) {
        setObjectBlock(x, y, block);
        Chunk chunk = chunkForWrite(Chunk.chunkOf(x), Chunk.chunkOf(y));
        chunk.setState(Chunk.localOf(x), surfaceY(x, y), Chunk.localOf(y), meta);
    }

    /**
     * Writes into the ground layer together with the state of the cell.
     * <p>
     * Used by a decoration that has to replace the ground the generator laid out, such as the pool
     * of lava that burns it into stone, see {@code LavaLakeDecoration}. Like the object layer
     * helpers this is the decoration path: raw ids, so a chunk grown from the seed alone never
     * counts as a change of the player.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @param block block to store
     * @param meta state that belongs to the block, {@code 0} when it carries none
     */
    public void setFloorBlock(int x, int y, Block block, int meta) {
        Chunk chunk = chunkForWrite(Chunk.chunkOf(x), Chunk.chunkOf(y));
        int localX = Chunk.localOf(x);
        int localY = Chunk.localOf(y);
        int ground = surfaceY(x, y) - 1;
        chunk.setRawId(localX, ground, localY, block.id());
        chunk.setState(localX, ground, localY, meta);
    }

    /**
     * Writes into the object layer only when that cell is still empty.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @param block block to store
     * @return {@code true} when the block was stored
     */
    public boolean placeObjectIfAir(int x, int y, Block block) {
        return placeObjectIfAirAt(x, surfaceY(x, y), y, block);
    }

    /**
     * Writes into the object layer of one cell only when that cell is still empty.
     * <p>
     * This is what a decoration that reaches over more than one block writes with. The rule is the same as
     * for the ground of a column: whoever fills a cell first owns it, so a canopy never covers a trunk and
     * a decoration never covers a block the player put down.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @param block block to store
     * @return {@code true} when the block was stored
     */
    public boolean placeObjectIfAirAt(int x, int y, int z, Block block) {
        Chunk chunk = chunkForWrite(Chunk.chunkOf(x), Chunk.chunkOf(z));
        int localX = Chunk.localOf(x);
        int localZ = Chunk.localOf(z);
        if (!chunk.getBlock(localX, y, localZ).isAir()) {
            return false;
        }
        // Decoration path, see setObjectBlock: never marks the chunk as modified.
        chunk.setRawId(localX, y, localZ, block.id());
        return true;
    }

    /**
     * Writes into the object layer only when that cell is still empty, together with its state.
     * <p>
     * This is what a decorator uses, and it follows a rule that keeps decorations from fighting each
     * other: <b>whoever fills a cell first owns it.</b> A tree does not grow out of the lava of a
     * pool, the bank of a river does not cover a burn, and - the case that found this rule - a
     * decoration that runs while a chunk is completed never overwrites a block the player put down.
     * The alternative, writing over whatever is there, is what let a tree be planted in lava.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @param block block to store
     * @param meta state that belongs to the block, {@code 0} when it carries none
     * @return {@code true} when the block was stored
     */
    public boolean placeObjectIfAir(int x, int y, Block block, int meta) {
        if (!placeObjectIfAir(x, y, block)) {
            return false;
        }
        Chunk chunk = chunkForWrite(Chunk.chunkOf(x), Chunk.chunkOf(y));
        chunk.setState(Chunk.localOf(x), surfaceY(x, y), Chunk.localOf(y), meta);
        return true;
    }

    /**
     * Finds a walkable cell near a position to place the player on.
     * <p>
     * The search walks outwards in growing rings and accepts the first cell that
     * has a floor and does not block movement, so the player never starts inside a
     * tree or over a hole.
     *
     * @param x preferred block X coordinate
     * @param y preferred block Y coordinate
     * @return a block coordinate pair, {@code {x, y}}, the caller can spawn in
     */
    public int[] findSpawnPosition(int x, int z) {
        for (int radius = 0; radius <= SPAWN_SEARCH_RADIUS; radius++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    int candidateX = x + dx;
                    int candidateZ = z + dz;
                    if (isWalkable(candidateX, candidateZ)) {
                        return new int[] {candidateX, candidateZ};
                    }
                }
            }
        }
        LOGGER.warn("No walkable spawn cell within {} blocks of ({}, {}), falling back",
                SPAWN_SEARCH_RADIUS, x, z);
        return new int[] {x, z};
    }

    /**
     * {@code true} when a body may be put down in a column.
     * <p>
     * The ground of the column has to be solid and not a fluid, the cell the body would stand in has to be
     * free of both, and so has the cell above it, because a body is taller than one block. A player who
     * starts in the water of a lake has to wade out of it again, and one who starts over a hole or on top
     * of lava falls or burns before the first frame is over - so both are refused.
     *
     * @param x block X coordinate
     * @param z block Z coordinate
     * @return {@code true} when that column is a place to stand
     */
    private boolean isWalkable(int x, int z) {
        int feet = surfaceY(x, z);
        if (feet <= Constants.MIN_Y) {
            // An empty column has no ground to stand on at all.
            return false;
        }
        Block ground = getBlock(x, feet - 1, z);
        return ground.isSolid() && !ground.isLiquid()
                && !getBlock(x, feet, z).isLiquid() && !isSolid(x, feet, z)
                && !isSolid(x, feet + 1, z);
    }

    /**
     * {@code true} when a cell carries a fluid in either layer.
     * <p>
     * The water of a river and of a lake lies in the layer of the player and a spill the player
     * poured into a hole lies in the ground layer, so both are asked.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return {@code true} when water, lava or another fluid stands in that cell
     */
    private boolean inFluid(int x, int y) {
        return Fluids.byBlock(peekBlock(x, Chunk.flatY(Chunk.LAYER_FLOOR), y)) != null
                || Fluids.byBlock(peekBlock(x, Chunk.flatY(Chunk.LAYER_OBJECT), y)) != null;
    }

    /**
     * Returns a chunk whose cell is ready to be read or written.
     * <p>
     * A missing chunk is allocated, the requested cell gets its floor and, as soon
     * as the whole chunk is complete, its decorations. Only the touched cell is
     * generated, so a single block read never pays for a whole chunk.
     */
    private Chunk preparedChunk(int x, int y) {
        Chunk chunk = chunkForWrite(Chunk.chunkOf(x), Chunk.chunkOf(y));
        generator.generateCell(chunk, Chunk.localOf(x), Chunk.localOf(y));
        completeGeneration(chunk);
        return chunk;
    }

    /**
     * Returns the chunk for a pair of chunk coordinates, allocating it if needed.
     * <p>
     * A chunk that exists in the store is refilled from it before anybody touches
     * it, and its cells are marked generated by the stored flags, so the generator
     * steps aside for it. That single place is what makes the store the authority
     * for every chunk that was ever saved: reading a block, writing one and
     * planting a decoration into a neighbour all end up here.
     */
    private Chunk chunkForWrite(int chunkX, int chunkY) {
        long key = chunkKey(chunkX, chunkY);
        Chunk chunk = chunks.get(key);
        if (chunk == null) {
            chunk = new Chunk(chunkX, chunkY);
            // Stored before it is filled: a decoration may already reach into it.
            chunks.put(key, chunk);
            if (store != null && store.hasChunk(chunkX, chunkY)) {
                store.loadInto(chunk);
            }
        }
        return chunk;
    }

    /**
     * Plants the decorations of a chunk once its floor is complete.
     *
     * @param chunk chunk that may have just finished generating
     */
    private void completeGeneration(Chunk chunk) {
        if (!chunk.isFullyGenerated() || chunk.isDecorated()) {
            return;
        }
        chunk.markDecorated();
        generator.decorate(this, chunk);
    }

    /** Packs two chunk coordinates into the map key of the chunk table. */
    private static long chunkKey(int chunkX, int chunkY) {
        return BlockPos.pack(chunkX, chunkY);
    }
}

