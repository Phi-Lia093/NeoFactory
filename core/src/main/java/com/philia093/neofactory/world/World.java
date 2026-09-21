package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.entity.EntityManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private final int spawnY;

    /**
     * Creates a world and prepares the chunks around the spawn point.
     *
     * @param seed world seed, selects the generated terrain
     */
    public World(int seed) {
        this(seed, 0, 0, null);
    }

    /**
     * Creates a world and prepares the chunks around the spawn point.
     *
     * @param seed world seed, selects the generated terrain
     * @param spawnX block X coordinate the player would like to start near
     * @param spawnY block Y coordinate the player would like to start near
     */
    public World(int seed, int spawnX, int spawnY) {
        this(seed, spawnX, spawnY, null);
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
     * @param spawnY block Y coordinate the player would like to start near
     * @param store store holding the chunks of this world, may be {@code null}
     */
    public World(int seed, int spawnX, int spawnY, ChunkStore store) {
        this.seed = seed;
        this.generator = new WorldGen(seed);
        this.store = store;

        // Biomes cover large areas, so the requested point is only a hint: the
        // nearest grassy cell is used instead, which keeps the start of the game
        // inside a landscape that can carry trees and grass.
        int[] spawn = generator.findSpawnBiome(spawnX, spawnY);
        this.spawnX = spawn[0];
        this.spawnY = spawn[1];
        if (this.spawnX != spawnX || this.spawnY != spawnY) {
            LOGGER.info("Spawn moved from ({}, {}) to ({}, {}) to reach a grassy biome",
                    spawnX, spawnY, this.spawnX, this.spawnY);
        }

        // The area is finished before anything looks for a free cell inside it:
        // an unfinished chunk would plant its trees later and could bury whatever
        // already stands there, the player included.
        ensureChunksAround(this.spawnX, this.spawnY, SPAWN_CHUNK_RADIUS);
        LOGGER.info("World {} created with {} chunks around spawn ({}, {})",
                seed, chunks.size(), this.spawnX, this.spawnY);
    }

    /** Seed this world was generated from. */
    public int seed() {
        return seed;
    }

    /** Block X coordinate the player starts near. */
    public int spawnX() {
        return spawnX;
    }

    /** Block Y coordinate the player starts near. */
    public int spawnY() {
        return spawnY;
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
                    && Math.abs(chunk.chunkY() - centerChunkY) <= keepRadius) {
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
    public Block getBlock(int x, int y, int layer) {
        Chunk chunk = preparedChunk(x, y);
        return chunk.getBlock(Chunk.localOf(x), Chunk.localOf(y), layer);
    }

    @Override
    public void setBlock(int x, int y, int layer, Block block) {
        Chunk chunk = preparedChunk(x, y);
        chunk.setBlock(Chunk.localOf(x), Chunk.localOf(y), layer, block);
        // This is the public write path of the world: everything reaching it is a
        // player change and has to survive unloading and saving.
        chunk.markModified();
    }

    /**
     * Reads a block without generating anything.
     * <p>
     * Returns {@link Blocks#AIR} for unloaded chunks. Used by the decorators,
     * which must be able to look at a possibly missing neighbour without pulling
     * the whole neighbourhood into memory.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @param layer layer index, see {@link Chunk#LAYER_FLOOR} and
     *              {@link Chunk#LAYER_OBJECT}
     * @return the stored block, {@link Blocks#AIR} when the chunk is not loaded
     */
    public Block peekBlock(int x, int y, int layer) {
        Chunk chunk = chunks.get(chunkKey(Chunk.chunkOf(x), Chunk.chunkOf(y)));
        if (chunk == null) {
            return Blocks.AIR;
        }
        return chunk.getBlock(Chunk.localOf(x), Chunk.localOf(y), layer);
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
        chunk.setRawId(Chunk.localOf(x), Chunk.localOf(y), Chunk.LAYER_OBJECT, block.id());
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
        Chunk chunk = chunkForWrite(Chunk.chunkOf(x), Chunk.chunkOf(y));
        int localX = Chunk.localOf(x);
        int localY = Chunk.localOf(y);
        if (!chunk.getBlock(localX, localY, Chunk.LAYER_OBJECT).isAir()) {
            return false;
        }
        // Decoration path, see setObjectBlock: never marks the chunk as modified.
        chunk.setRawId(localX, localY, Chunk.LAYER_OBJECT, block.id());
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
    public int[] findSpawnPosition(int x, int y) {
        for (int radius = 0; radius <= SPAWN_SEARCH_RADIUS; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) {
                        continue;
                    }
                    int candidateX = x + dx;
                    int candidateY = y + dy;
                    if (!getBlock(candidateX, candidateY, Chunk.LAYER_FLOOR).isAir()
                            && !isSolid(candidateX, candidateY)) {
                        return new int[] {candidateX, candidateY};
                    }
                }
            }
        }
        LOGGER.warn("No walkable spawn cell within {} blocks of ({}, {}), falling back",
                SPAWN_SEARCH_RADIUS, x, y);
        return new int[] {x, y};
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

