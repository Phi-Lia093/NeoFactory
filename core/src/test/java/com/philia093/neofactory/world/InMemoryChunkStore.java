package com.philia093.neofactory.world;

import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.save.ChunkCodec;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A {@link ChunkStore} that keeps the chunks in a map.
 * <p>
 * Used by the tests: storing a chunk in memory makes it easy to check what the
 * world writes when it drops a chunk, and how it reads it back, without touching a
 * file system. The format is the real one, so a test fails when the stored data
 * cannot be read again.
 */
public final class InMemoryChunkStore implements ChunkStore {

    private final Map<Long, NbtCompound> chunks = new LinkedHashMap<>();

    @Override
    public boolean hasChunk(int chunkX, int chunkY) {
        return chunks.containsKey(BlockPos.pack(chunkX, chunkY));
    }

    @Override
    public void loadInto(Chunk chunk) {
        NbtCompound data = chunks.get(BlockPos.pack(chunk.chunkX(), chunk.chunkZ()));
        if (data != null) {
            ChunkCodec.read(chunk, data);
        }
    }

    @Override
    public void persist(Chunk chunk) {
        chunks.put(BlockPos.pack(chunk.chunkX(), chunk.chunkZ()), ChunkCodec.write(chunk));
        chunk.clearModified();
    }

    @Override
    public int storedChunkCount() {
        return chunks.size();
    }
}
