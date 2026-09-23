package com.philia093.neofactory.world.save;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtIo;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.ChunkStreamer;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a world really comes back out of its save game.
 * <p>
 * Everything here works on the file system, because that is where the interesting
 * details live: a chunk the player changed ends up in its own file, the level file
 * stays small, and a world that is opened again shows what was built in it.
 */
class WorldPersistenceTest {

    /** Seed used by the tests, any fixed value does. */
    private static final int SEED = 1234;

    /** Cell the tests build in, close enough to the origin to be loaded on open. */
    private static final int BUILT_X = 3;

    /** Second coordinate of the cell the tests build in. */
    private static final int BUILT_Y = 3;

    @TempDir
    Path tempFolder;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aChangedChunkGoesIntoItsOwnFile() throws Exception {
        WorldStorage storage = new WorldStorage(tempFolder.toFile());
        SaveSummary summary = createSaveGame(storage);

        World world = new World(SEED, 0, 0);
        world.setBlock(BUILT_X, Chunk.flatY(Chunk.LAYER_OBJECT), BUILT_Y, Blocks.STONE);

        int stored = WorldSaver.save(storage, summary, levelData(), world, new PlayerInventory());

        assertEquals(1, stored);
        assertTrue(new File(summary.chunksFolder(), "c.0.0.dat").isFile(),
                "the changed chunk has no file");
        assertEquals(0, world.modifiedChunkCount(), "a saved chunk is still marked as changed");

        NbtCompound root = (NbtCompound) NbtIo.readGzip(summary.levelFile());
        assertEquals(SaveFormat.DATA_VERSION, root.getInt(SaveTags.DATA_VERSION, 0));
    }

    @Test
    void aWorldWithoutChangesStoresNoChunkAtAll() throws Exception {
        WorldStorage storage = new WorldStorage(tempFolder.toFile());
        SaveSummary summary = createSaveGame(storage);

        World world = new World(SEED, 0, 0);
        int stored = WorldSaver.save(storage, summary, levelData(), world, new PlayerInventory());

        assertEquals(0, stored);
        assertEquals(0, summary.chunks().count());
        assertEquals(0L, summary.chunks().sizeBytes());
    }

    @Test
    void aStoredChangeIsThereAfterOpeningTheWorldAgain() throws Exception {
        WorldStorage storage = new WorldStorage(tempFolder.toFile());
        SaveSummary summary = createSaveGame(storage);

        World world = new World(SEED, 0, 0);
        world.setBlock(BUILT_X, Chunk.flatY(Chunk.LAYER_OBJECT), BUILT_Y, Blocks.STONE);
        WorldSaver.save(storage, summary, levelData(), world, new PlayerInventory());

        SaveSummary reopened = storage.list().get(0);
        assertEquals(SaveFormat.DATA_VERSION, reopened.dataVersion());
        assertTrue(reopened.sizeBytes() > 0L);

        WorldLoader loader = WorldLoader.open(storage, reopened, 0, 0);

        assertEquals(SEED, loader.data().seed());
        assertEquals(1, loader.storedChunks());
        assertEquals(Blocks.STONE,
                loader.world().getBlock(BUILT_X, Chunk.flatY(Chunk.LAYER_OBJECT), BUILT_Y));
    }

    @Test
    void aChangeSurvivesLeavingTheAreaAndComingBack() throws Exception {
        WorldStorage storage = new WorldStorage(tempFolder.toFile());
        SaveSummary summary = createSaveGame(storage);
        FileChunkStore store = new FileChunkStore(summary.folder());

        World world = new World(SEED, 0, 0, store);
        ChunkStreamer streamer = new ChunkStreamer();
        world.setBlock(BUILT_X, Chunk.flatY(Chunk.LAYER_OBJECT), BUILT_Y, Blocks.STONE);

        for (int frame = 0; frame < 200; frame++) {
            streamer.update(world, 20_000.0f, 20_000.0f, 16.0f);
        }
        // Leaving the area wrote the chunk, so it was allowed to leave memory.
        assertTrue(store.hasChunk(0, 0), "the changed chunk was dropped without being written");

        for (int frame = 0; frame < 200; frame++) {
            streamer.update(world, BUILT_X + 0.5f, BUILT_Y + 0.5f, 16.0f);
        }
        assertEquals(Blocks.STONE, world.getBlock(BUILT_X, Chunk.flatY(Chunk.LAYER_OBJECT), BUILT_Y));
    }

    @Test
    void aStateSurvivesItsSaveGame() throws Exception {
        WorldStorage storage = new WorldStorage(tempFolder.toFile());
        SaveSummary summary = createSaveGame(storage);

        World world = new World(SEED, 0, 0);
        world.setBlock(BUILT_X, Chunk.flatY(Chunk.LAYER_OBJECT), BUILT_Y, Blocks.STONE);
        // The state is what a machine would face or a pipe would be drawn with, so it
        // has to reach the chunk file exactly like the block does.
        world.setState(BUILT_X, Chunk.flatY(Chunk.LAYER_OBJECT), BUILT_Y, 42);
        WorldSaver.save(storage, summary, levelData(), world, new PlayerInventory());

        WorldLoader loader = WorldLoader.open(storage, storage.list().get(0), 0, 0);

        assertEquals(Blocks.STONE,
                loader.world().getBlock(BUILT_X, Chunk.flatY(Chunk.LAYER_OBJECT), BUILT_Y));
        assertEquals(42, loader.world().getState(BUILT_X, Chunk.flatY(Chunk.LAYER_OBJECT), BUILT_Y));
    }

    @Test
    void deletingAWorldRemovesItsChunkFiles() throws Exception {
        WorldStorage storage = new WorldStorage(tempFolder.toFile());
        SaveSummary summary = createSaveGame(storage);

        World world = new World(SEED, 0, 0);
        world.setBlock(BUILT_X, Chunk.flatY(Chunk.LAYER_OBJECT), BUILT_Y, Blocks.STONE);
        WorldSaver.save(storage, summary, levelData(), world, new PlayerInventory());

        assertTrue(storage.delete(summary));
        assertFalse(summary.folder().exists(), "the folder of the world is still there");
        assertTrue(storage.list().isEmpty());
    }

    /** Creates an empty save game. */
    private SaveSummary createSaveGame(WorldStorage storage) {
        return storage.create("Persisted", SEED, 0, 0);
    }

    /** Level data pointing at the origin, where the tests build. */
    private static LevelData levelData() {
        LevelData data = new LevelData();
        data.setWorldName("Persisted");
        data.setSeed(SEED);
        data.setCreated(1L);
        data.setLastPlayed(1L);
        data.setSpawn(0, 0);
        return data;
    }
}
