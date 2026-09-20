package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.philia093.neofactory.util.Constants.SPAWN_CHUNK_RADIUS;

/**
 * The block storage of the game.
 * <p>
 * A world is an unbounded plane of {@link Chunk chunks} addressed by two
 * horizontal chunk coordinates. Chunks are created on demand while the player
 * walks around, see {@link #ensureChunksAround(float, float, int)}.
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
        this(seed, 0, 0);
    }

    /**
     * Creates a world and prepares the chunks around the spawn point.
     *
     * @param seed world seed, selects the generated terrain
     * @param spawnX block X coordinate the player would like to start near
     * @param spawnY block Y coordinate the player would like to start near
     */
    public World(int seed, int spawnX, int spawnY) {
        this.seed = seed;
        this.generator = new WorldGen(seed);

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
        int centerChunkX = Chunk.chunkOf((int) Math.floor(blockX));
        int centerChunkY = Chunk.chunkOf((int) Math.floor(blockY));
        for (int chunkY = centerChunkY - chunkRadius; chunkY <= centerChunkY + chunkRadius;
                chunkY++) {
            for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius;
                    chunkX++) {
                Chunk chunk = chunks.get(chunkKey(chunkX, chunkY));
                if (chunk == null || !chunk.isComplete()) {
                    loadChunk(chunkX, chunkY);
                }
            }
        }
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
    }

    /**
     * Returns a chunk that is ready to be filled from a save game.
     * <p>
     * The chunk is allocated but nothing is generated inside it, because the caller
     * writes every cell together with the flags that tell the generator the cell is
     * done. Generating first would only be undone by the stored data.
     *
     * @param chunkX chunk coordinate along the first horizontal axis
     * @param chunkY chunk coordinate along the second horizontal axis
     * @return the chunk, empty until the caller fills it
     */
    public Chunk chunkForLoading(int chunkX, int chunkY) {
        return chunkForWrite(chunkX, chunkY);
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
        chunk.setBlock(Chunk.localOf(x), Chunk.localOf(y), Chunk.LAYER_OBJECT, block);
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
        chunk.setBlock(localX, localY, Chunk.LAYER_OBJECT, block);
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

    /** Returns the chunk for a pair of chunk coordinates, allocating it if needed. */
    private Chunk chunkForWrite(int chunkX, int chunkY) {
        long key = chunkKey(chunkX, chunkY);
        Chunk chunk = chunks.get(key);
        if (chunk == null) {
            chunk = new Chunk(chunkX, chunkY);
            // Stored before it is filled: a decoration may already reach into it.
            chunks.put(key, chunk);
        }
        return chunk;
    }

    /** Packs two chunk coordinates into the map key of the chunk table. */
    private static long chunkKey(int chunkX, int chunkY) {
        return BlockPos.pack(chunkX, chunkY);
    }
}

