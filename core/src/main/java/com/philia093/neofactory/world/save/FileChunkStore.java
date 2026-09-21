package com.philia093.neofactory.world.save;

import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.ChunkStore;

/**
 * A {@link ChunkStore} that keeps every chunk in its own file.
 * <p>
 * This is the bridge between the world, which only speaks {@link ChunkStore}, and
 * {@link ChunkStorage}, which knows about files, and it is where the format of a
 * chunk is applied: reading fills a chunk from its file, writing converts it with
 * {@link ChunkCodec} and clears the changed flag of the chunk.
 */
public final class FileChunkStore implements ChunkStore {

    private final ChunkStorage storage;

    /**
     * Creates a store for a save game.
     *
     * @param worldFolder folder of the save game
     */
    public FileChunkStore(java.io.File worldFolder) {
        this(new ChunkStorage(worldFolder));
    }

    /**
     * Creates a store on top of chunk files.
     *
     * @param storage storage holding the files
     */
    public FileChunkStore(ChunkStorage storage) {
        this.storage = storage;
    }

    /** Chunk files this store works on. */
    public ChunkStorage storage() {
        return storage;
    }

    @Override
    public boolean hasChunk(int chunkX, int chunkY) {
        return storage.hasChunk(chunkX, chunkY);
    }

    @Override
    public void loadInto(Chunk chunk) {
        NbtCompound data = storage.read(chunk.chunkX(), chunk.chunkY());
        if (data == null) {
            // The file vanished between the check and the read, the chunk stays
            // empty and the seed fills it. Nothing the player built is at risk,
            // because a missing file can only be a chunk nobody changed.
            return;
        }
        ChunkCodec.read(chunk, data);
    }

    @Override
    public void persist(Chunk chunk) {
        storage.write(chunk.chunkX(), chunk.chunkY(), ChunkCodec.write(chunk));
        // The file holds the content now, so the chunk is no longer the only copy
        // of the change and may be dropped from memory.
        chunk.clearModified();
    }

    @Override
    public int storedChunkCount() {
        return storage.count();
    }
}
