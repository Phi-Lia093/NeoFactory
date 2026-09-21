package com.philia093.neofactory.world;

/**
 * Storage a world can hand its chunks to.
 * <p>
 * The world itself does not know whether a chunk is written into a file, into a
 * region file or simply kept in memory: it only asks whether a chunk is stored,
 * loads the stored content into a chunk it owns, and hands a chunk over when its
 * content has to survive being dropped from memory. Everything that knows about
 * folders and files lives in the save package behind this interface.
 * <p>
 * The contract matters for correctness:
 * <ul>
 *     <li>{@link #hasChunk(int, int)} tells the world to read a chunk from the
 *         store instead of generating it. The store is the authority for such a
 *         chunk, the seed is not.</li>
 *     <li>{@link #persist(Chunk)} has to complete before the world drops the chunk,
 *         so a chunk that disappears from memory is never the only copy of a
 *         player change.</li>
 *     <li>Chunk files are never deleted: a chunk that was once stored either stays
 *         stored or is generated again from the seed, which only works for chunks
 *         nobody changed.</li>
 * </ul>
 */
public interface ChunkStore {

    /**
     * {@code true} when a chunk of this store exists.
     *
     * @param chunkX chunk coordinate along the first horizontal axis
     * @param chunkY chunk coordinate along the second horizontal axis
     */
    boolean hasChunk(int chunkX, int chunkY);

    /**
     * Fills a chunk with the stored content.
     * <p>
     * The chunk is empty when this is called and belongs to the coordinates that
     * were asked for. When the store cannot produce the chunk the chunk stays
     * empty and the generator fills it, so a missing file never breaks loading.
     *
     * @param chunk chunk to fill, already allocated for the right coordinates
     */
    void loadInto(Chunk chunk);

    /**
     * Writes the content of a chunk into the store and clears its changed flag.
     *
     * @param chunk chunk to store
     */
    void persist(Chunk chunk);

    /** Amount of chunks currently stored. */
    int storedChunkCount();
}
