package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the chunk table follows the player instead of the walk.
 * <p>
 * The interesting part is not that chunks are loaded, it is what happens to a chunk
 * the player changed when it leaves memory: it has to be written before it is
 * dropped, and reading it later has to bring the change back. The tests use an
 * {@link InMemoryChunkStore} so they can look at what was written.
 */
class ChunkStreamerTest {

    /** Frames a test runs to give the streamer time to finish its work. */
    private static final int FRAMES = 400;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theViewDistanceIsClamped() {
        ChunkStreamer streamer = new ChunkStreamer();
        streamer.setViewDistance(Constants.CHUNK_VIEW_DISTANCE_MIN - 5);
        assertEquals(Constants.CHUNK_VIEW_DISTANCE_MIN, streamer.viewDistance());

        streamer.setViewDistance(Constants.CHUNK_VIEW_DISTANCE_MAX + 5);
        assertEquals(Constants.CHUNK_VIEW_DISTANCE_MAX, streamer.viewDistance());
    }

    @Test
    void theLoadRadiusFollowsTheViewDistanceAndTheZoom() {
        ChunkStreamer streamer = new ChunkStreamer();
        streamer.setViewDistance(4);

        assertEquals(4, streamer.loadRadius(0.0f));
        // A zoomed out camera shows more terrain, so more chunks are needed.
        assertTrue(streamer.loadRadius(Constants.CHUNK_SIZE * 10.0f) > 4);
    }

    @Test
    void walkingAwayDropsTheChunksBehindAndKeepsTheChanges() {
        InMemoryChunkStore store = new InMemoryChunkStore();
        World world = new World(2024, 0, 0, store);
        ChunkStreamer streamer = new ChunkStreamer();
        streamer.setViewDistance(Constants.CHUNK_VIEW_DISTANCE_MIN);

        world.setBlock(2, Chunk.flatY(Chunk.LAYER_OBJECT), 2, Blocks.STONE);
        for (int frame = 0; frame < FRAMES; frame++) {
            streamer.update(world, 2.0f, 2.0f, 16.0f);
        }
        assertNotNull(world.chunkIfLoaded(0, 0));
        int keepRadius = Constants.CHUNK_VIEW_DISTANCE_MIN + Constants.CHUNK_UNLOAD_MARGIN;
        assertTrue(world.chunkCount() <= keptArea(keepRadius));

        // Walk far enough that the first chunk falls out of the kept area.
        for (int frame = 0; frame < FRAMES; frame++) {
            streamer.update(world, 20_000.0f, 20_000.0f, 16.0f);
        }

        assertNull(world.chunkIfLoaded(0, 0), "the chunk the player left is still in memory");
        assertTrue(world.chunkCount() <= keptArea(keepRadius));
        assertEquals(1, store.storedChunkCount());
        assertEquals(0, world.modifiedChunkCount());

        // Coming back shows the change, because the store was asked first.
        for (int frame = 0; frame < FRAMES; frame++) {
            streamer.update(world, 2.0f, 2.0f, 16.0f);
        }
        assertEquals(Blocks.STONE, world.getBlock(2, Chunk.flatY(Chunk.LAYER_OBJECT), 2));
    }

    @Test
    void aChangeStaysInMemoryWhenThereIsNowhereToWriteIt() {
        World world = new World(88, 0, 0, null);
        ChunkStreamer streamer = new ChunkStreamer();
        streamer.setViewDistance(Constants.CHUNK_VIEW_DISTANCE_MIN);

        world.setBlock(2, Chunk.flatY(Chunk.LAYER_OBJECT), 2, Blocks.STONE);
        for (int frame = 0; frame < FRAMES; frame++) {
            streamer.update(world, 20_000.0f, 20_000.0f, 16.0f);
        }

        // Without a store the chunk is the only copy, so it is never dropped.
        assertNotNull(world.chunkIfLoaded(0, 0));
        assertEquals(1, world.modifiedChunkCount());
        assertEquals(Blocks.STONE, world.getBlock(2, Chunk.flatY(Chunk.LAYER_OBJECT), 2));
    }

    @Test
    void terrainNobodyChangedIsRebuiltFromTheSeed() {
        World first = new World(555, 0, 0);
        World second = new World(555, 0, 0);

        for (int x = -30; x <= 30; x++) {
            assertEquals(first.getBlock(x, 3, Chunk.LAYER_FLOOR),
                    second.getBlock(x, 3, Chunk.LAYER_FLOOR));
            assertEquals(first.getBlock(x, 3, Chunk.LAYER_OBJECT),
                    second.getBlock(x, 3, Chunk.LAYER_OBJECT));
        }
    }

    /** Amount of chunks a square of that radius holds. */
    private static int keptArea(int keepRadius) {
        int side = 2 * keepRadius + 1;
        return side * side;
    }
}
