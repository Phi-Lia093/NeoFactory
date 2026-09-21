package com.philia093.neofactory.world.save;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtIo;
import com.philia093.neofactory.util.nbt.NbtList;
import com.philia093.neofactory.world.Chunk;
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
 * Checks that a world written by the first save format still opens.
 * <p>
 * Format 1 kept every chunk inside the level file. Converting such a world moves
 * those chunks into their own files, which has to keep what the player built and
 * has to leave a file behind that the new reader can handle.
 */
class LegacyMigrationTest {

    /** Seed of the world the test converts. */
    private static final int SEED = 77;

    /** Cell holding the change of the player in the old save game. */
    private static final int BUILT_X = 7;

    /** Second coordinate of that cell. */
    private static final int BUILT_Y = 7;

    @TempDir
    Path tempFolder;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aFormatOneWorldIsConvertedWhileOpening() throws Exception {
        WorldStorage storage = new WorldStorage(tempFolder.toFile());
        SaveSummary summary = storage.create("Legacy", SEED, 0, 0);
        writeLegacySaveGame(summary);

        SaveSummary legacy = storage.list().get(0);
        assertEquals(1, legacy.dataVersion());

        WorldLoader loader = WorldLoader.open(storage, legacy, 0, 0);

        assertEquals(1, loader.storedChunks());
        assertTrue(new File(summary.chunksFolder(), "c.0.0.dat").isFile());
        assertEquals(Blocks.BEDROCK,
                loader.world().getBlock(BUILT_X, BUILT_Y, Chunk.LAYER_OBJECT));

        NbtCompound rewritten = (NbtCompound) NbtIo.readGzip(summary.levelFile());
        assertEquals(SaveFormat.DATA_VERSION, rewritten.getInt(SaveTags.DATA_VERSION, 0));
        assertFalse(rewritten.contains(SaveTags.CHUNKS),
                "the old chunk list survived the conversion");
    }

    @Test
    void convertingTwiceKeepsTheChunkFiles() throws Exception {
        WorldStorage storage = new WorldStorage(tempFolder.toFile());
        SaveSummary summary = storage.create("Legacy", SEED, 0, 0);
        writeLegacySaveGame(summary);
        WorldLoader.open(storage, storage.list().get(0), 0, 0);

        // A world whose level file was rewritten holds no list, so opening it again
        // is a plain open and the migrated chunks stay where they are.
        WorldLoader second = WorldLoader.open(storage, storage.list().get(0), 0, 0);

        assertEquals(1, second.storedChunks());
        assertEquals(Blocks.BEDROCK,
                second.world().getBlock(BUILT_X, BUILT_Y, Chunk.LAYER_OBJECT));
    }

    /**
     * Writes a save game in the layout of format 1.
     * <p>
     * The level file is built by the current writer and then turned back into a
     * version 1 file: the version is lowered and the chunk list that the old format
     * carried is added. That is exactly what the old build produced.
     *
     * @param summary save game to fill
     */
    private static void writeLegacySaveGame(SaveSummary summary) {
        LevelData data = new LevelData();
        data.setWorldName("Legacy");
        data.setSeed(SEED);
        data.setCreated(1L);
        data.setLastPlayed(1L);
        data.setSpawn(0, 0);
        NbtCompound root = data.write(new PlayerInventory());
        root.putInt(SaveTags.DATA_VERSION, 1);

        World source = new World(SEED, 0, 0);
        Chunk chunk = source.loadChunk(0, 0);
        chunk.setRawId(BUILT_X, BUILT_Y, Chunk.LAYER_OBJECT, Blocks.BEDROCK.id());

        NbtList chunks = new NbtList(SaveTags.CHUNKS);
        chunks.add(ChunkCodec.write(chunk));
        root.put(chunks);

        NbtIo.writeGzip(root, summary.levelFile());
    }
}
