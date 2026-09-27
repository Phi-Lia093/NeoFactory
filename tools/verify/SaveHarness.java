import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.save.ChunkCodec;
import com.philia093.neofactory.world.save.LevelData;
import com.philia093.neofactory.world.save.SaveException;
import com.philia093.neofactory.world.save.SaveFormat;
import com.philia093.neofactory.world.save.SaveNames;
import com.philia093.neofactory.world.save.SaveSummary;
import com.philia093.neofactory.world.save.SaveTags;
import com.philia093.neofactory.world.save.WorldLoader;
import com.philia093.neofactory.world.save.WorldSaver;
import com.philia093.neofactory.world.save.WorldStorage;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtIo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Temporary harness for the save layer, no window needed. */
public final class SaveHarness {

    private static final int OBJECT = Chunk.LAYER_OBJECT;
    private static final int FLOOR = Chunk.LAYER_FLOOR;
    private static final int SEED = 20260918;

    private static int checks;
    private static int failures;
    private static final List<String> FAILED = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        Blocks.registerAll();
        Items.registerAll();

        Path root = Path.of("build", "verify", "saves");
        deleteTree(root);

        WorldStorage storage = new WorldStorage(root.toFile());
        checkNames();
        checkChunkRoundTrip();
        checkLevelDataRoundTrip();
        checkStorageOperations(storage);
        checkWorldRoundTrip(storage);

        System.out.println("checks: " + checks + ", failures: " + failures);
        for (String failure : FAILED) {
            System.out.println("FAILED: " + failure);
        }
    }

    private static void checkNames() {
        check("a blank name falls back", SaveFormat.DEFAULT_NAME.equals(SaveNames.sanitize("   ")));
        check("a null name falls back", SaveFormat.DEFAULT_NAME.equals(SaveNames.sanitize(null)));
        check("a name is trimmed", "World".equals(SaveNames.sanitize("  World  ")));
        check("control characters are removed", "World".equals(SaveNames.sanitize("Wo\nrld")));
        check("a chinese name is kept", "新的工厂".equals(SaveNames.sanitize("新的工厂")));
        check("a long name is cut",
                SaveNames.sanitize("x".repeat(100)).length() == SaveFormat.MAX_NAME_LENGTH);
        // The readable name is stored inside the file, the folder uses an id, so a
        // name may hold characters that a file system would refuse.
        check("a name may hold any visible character",
                "a/b:c*d?e\"f<g>h|i".equals(SaveNames.sanitize("a/b:c*d?e\"f<g>h|i")));
        check("an id is safe as a folder name",
                SaveNames.nextId(List.of()).matches("[a-z0-9_]+"));

        List<String> taken = new ArrayList<>();
        taken.add("World");
        check("a duplicate name gets a number", "World 2".equals(SaveNames.uniqueName("World", taken)));
        check("a free name is kept", "Other".equals(SaveNames.uniqueName("Other", taken)));
        check("an unused id is produced", !SaveNames.nextId(taken).isEmpty());
    }

    private static void checkChunkRoundTrip() {
        World world = new World(SEED, 0, 0);
        Chunk original = world.loadChunk(1, 1);

        // Something the player did: a hole in the ground and a wall above it.
        original.setRawId(3, 4, FLOOR, Blocks.AIR.id());
        original.setRawId(5, 6, OBJECT, Blocks.PLANKS_OAK.id());
        original.setRawId(7, 8, FLOOR, Blocks.STONE.id());

        NbtCompound tag = ChunkCodec.write(original);
        Chunk restored = new Chunk(1, 1);
        ChunkCodec.read(restored, tag);

        check("the chunk keeps its coordinate", restored.chunkX() == 1 && restored.chunkY() == 1);
        check("the dug hole survives", restored.getBlock(3, 4, FLOOR) == Blocks.AIR);
        check("the built wall survives", restored.getBlock(5, 6, OBJECT) == Blocks.PLANKS_OAK);
        check("the new ground survives", restored.getBlock(7, 8, FLOOR) == Blocks.STONE);
        check("the chunk is complete after loading", restored.isComplete());
        check("the chunk is not dirty after loading", !restored.isDirty());

        int mismatches = 0;
        for (int localY = 0; localY < Constants.CHUNK_SIZE; localY++) {
            for (int localX = 0; localX < Constants.CHUNK_SIZE; localX++) {
                if (original.rawId(localX, localY, FLOOR) != restored.rawId(localX, localY, FLOOR)
                        || original.rawId(localX, localY, OBJECT) != restored.rawId(localX, localY, OBJECT)
                        || original.isCellGenerated(localX, localY)
                                != restored.isCellGenerated(localX, localY)) {
                    mismatches++;
                }
            }
        }
        check("every cell and flag matches (" + mismatches + " mismatches)", mismatches == 0);
    }

    private static void checkLevelDataRoundTrip() {
        PlayerInventory inventory = new PlayerInventory();
        inventory.set(0, ItemStack.of(Items.DIAMOND_PICKAXE, 1));
        inventory.set(4, ItemStack.of(Items.STONE, 64));
        inventory.set(35, ItemStack.of(Items.SUGAR, 17));
        inventory.setSelectedSlot(4);

        LevelData data = new LevelData();
        data.setWorldName("测试世界");
        data.setSeed(SEED);
        data.setSpawn(12, -3);
        data.setCreated(1_700_000_000_000L);
        data.setLastPlayed(1_700_000_500_000L);
        data.addPlayedMillis(90_000L);
        data.capturePlayer(320.5f, 880.25f, 0.6f, -0.8f, inventory);

        NbtCompound root = data.write(inventory);
        check("the root tag carries the format version",
                root.getInt("DataVersion", -1) == SaveFormat.DATA_VERSION);
        check("the root tag carries the name", "测试世界".equals(root.getString("WorldName", "")));
        check("the root tag carries an entities list", root.getList("Entities") != null);
        check("the root tag carries the game rules", root.getCompound("GameRules") != null);

        LevelData read = LevelData.read(root);
        check("the name survives", "测试世界".equals(read.worldName()));
        check("the seed survives", read.seed() == SEED);
        check("the spawn survives", read.spawnX() == 12 && read.spawnY() == -3);
        check("the creation time survives", read.created() == 1_700_000_000_000L);
        check("the last played time survives", read.lastPlayed() == 1_700_000_500_000L);
        check("the played time survives", read.playedMillis() == 90_000L);
        check("the player position survives",
                read.playerX() == 320.5f && read.playerY() == 880.25f);
        check("the facing survives", read.rotationX() == 0.6f && read.rotationY() == -0.8f);
        check("the selected slot survives", read.selectedSlot() == 4);

        PlayerInventory loaded = new PlayerInventory();
        SaveTags.readInventory(loaded, root.getCompound("Player").getList("Inventory"));
        check("the tool survives", loaded.get(0).item() == Items.DIAMOND_PICKAXE);
        check("the stack size survives", loaded.get(4).count() == 64);
        check("the last slot survives", loaded.get(35).item() == Items.SUGAR);
        check("an untouched slot stays empty", loaded.get(20).isEmpty());

        // A file written by a newer build is refused instead of being guessed at.
        NbtCompound newer = data.write(inventory);
        newer.putInt("DataVersion", SaveFormat.DATA_VERSION + 1);
        check("a newer format is refused", throwsSave(() -> LevelData.read(newer)));
    }

    private static void checkStorageOperations(WorldStorage storage) {
        SaveSummary first = storage.create("First World", SEED, 0, 0);
        check("the folder was created", first.folder().isDirectory());
        check("the folder is named after the id",
                first.folder().getName().equals(SaveFormat.SAVE_FOLDER_PREFIX + first.id()));
        check("a folder without data is not a world yet", storage.readSummary(first.folder()) == null);
        saveSmallWorld(storage, first);

        // The name of an existing world is known from its file, which is what keeps a
        // second world from being created under the same name.
        SaveSummary second = storage.create("First World", SEED, 0, 0);
        check("a second world gets another folder", !second.id().equals(first.id()));
        check("a second world gets another name",
                "First World 2".equals(second.displayName()));
        saveSmallWorld(storage, second);

        List<SaveSummary> afterCreate = storage.list();
        check("the store lists both worlds", afterCreate.size() == 2);
        check("a duplicate name is made unique", afterCreate.stream()
                .anyMatch(summary -> "First World 2".equals(summary.displayName())));
        check("a stored name is read back", afterCreate.stream()
                .anyMatch(summary -> "First World".equals(summary.displayName())));
        check("the summary carries the seed", afterCreate.stream()
                .allMatch(summary -> summary.seed() == SEED));
        check("the summary carries the file size", afterCreate.stream()
                .allMatch(summary -> summary.sizeBytes() > 0L));

        // Without data a folder is not a world and stays out of the list.
        File empty = storage.folderOf("empty");
        check("an empty folder can be created", empty.mkdirs());
        check("an empty folder is not listed", storage.list().size() == 2);
        check("an empty folder can be removed", empty.delete());

        SaveSummary removed = storage.list().get(0);
        check("deleting removes the folder", storage.delete(removed) && !removed.folder().exists());
        check("the list shrinks after deleting", storage.list().size() == 1);
    }

    /** Writes a small world into a save game, so that it becomes readable. */
    private static void saveSmallWorld(WorldStorage storage, SaveSummary summary) {
        World world = new World(SEED, 0, 0);
        PlayerInventory inventory = new PlayerInventory();
        LevelData data = new LevelData();
        data.setWorldName(summary.displayName());
        data.setSeed(SEED);
        data.setSpawn(world.spawnX(), world.spawnY());
        data.capturePlayer(Constants.TILE_SIZE * 0.5f, Constants.TILE_SIZE * 0.5f, 1.0f, 0.0f,
                inventory);
        WorldSaver.save(storage, summary, data, world, inventory);
    }

    private static void checkWorldRoundTrip(WorldStorage storage) {
        SaveSummary summary = storage.create("Survival", SEED, 0, 0);

        World original = new World(SEED, 0, 0);
        PlayerInventory inventory = new PlayerInventory();
        inventory.set(3, ItemStack.of(Items.PLANKS_OAK, 12));
        inventory.setSelectedSlot(3);
        int holeX = 20;
        int holeY = 20;
        original.setBlock(holeX, holeY, FLOOR, Blocks.AIR);
        original.setBlock(holeX + 1, holeY, OBJECT, Blocks.STONE);
        original.setBlock(holeX + 2, holeY, FLOOR, Blocks.SAND);
        int chunkCount = original.chunkCount();
        check("the world holds chunks before saving", chunkCount > 0);

        LevelData data = new LevelData();
        data.setWorldName("Survival");
        data.setSeed(SEED);
        data.setSpawn(original.spawnX(), original.spawnY());
        data.capturePlayer((holeX + 0.5f) * Constants.TILE_SIZE,
                (holeY + 0.5f) * Constants.TILE_SIZE, 1.0f, 0.0f, inventory);

        int stored = WorldSaver.save(storage, summary, data, original, inventory);
        check("every chunk was stored", stored == chunkCount);
        check("the file exists", summary.levelFile().isFile());
        check("no temporary file is left behind",
                !new File(summary.folder(), SaveFormat.LEVEL_FILE + ".tmp").exists());

        WorldLoader loader = WorldLoader.open(storage, summary, 0, 0);
        World loaded = loader.world();
        check("the seed survives", loaded.seed() == SEED);
        check("no stored chunk was lost", loaded.chunkCount() >= chunkCount);
        check("the stored chunk count is reported", loader.storedChunks() == chunkCount);
        check("the hole is still a hole", loaded.getBlock(holeX, holeY, FLOOR).isAir());
        check("the built block is still there",
                loaded.getBlock(holeX + 1, holeY, OBJECT) == Blocks.STONE);
        check("the new ground is still there",
                loaded.getBlock(holeX + 2, holeY, FLOOR) == Blocks.SAND);
        check("the name survives", "Survival".equals(loader.data().worldName()));
        check("the player position survives",
                loader.data().playerX() == (holeX + 0.5f) * Constants.TILE_SIZE);

        PlayerInventory loadedInventory = new PlayerInventory();
        loader.data().applyInventory(loadedInventory);
        check("the inventory survives a real save",
                loadedInventory.get(3).item() == Items.PLANKS_OAK
                        && loadedInventory.get(3).count() == 12);
        check("the selected slot survives a real save", loadedInventory.selectedSlot() == 3);

        // Walking around after loading must not generate over the stored chunks.
        loaded.ensureChunksAround(holeX, holeY, 3);
        check("the hole survives a second generation pass",
                loaded.getBlock(holeX, holeY, FLOOR).isAir());
        check("the built block survives a second generation pass",
                loaded.getBlock(holeX + 1, holeY, OBJECT) == Blocks.STONE);
        check("a loaded chunk is complete",
                loaded.chunkIfLoaded(Chunk.chunkOf(holeX), Chunk.chunkOf(holeY)).isComplete());

        // A damaged file is moved aside and reported, never overwritten.
        File level = summary.levelFile();
        try {
            Files.write(level.toPath(), new byte[24]);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        check("a damaged world is refused",
                throwsSave(() -> WorldLoader.open(storage, summary, 0, 0)));
        check("the damaged file was moved aside", !level.exists());
        File[] backups = summary.folder().listFiles((dir, name) -> name.contains("corrupt"));
        check("a backup of the damaged file exists", backups != null && backups.length == 1);
    }

    private static boolean throwsSave(SaveAction action) {
        try {
            action.run();
            return false;
        } catch (SaveException expected) {
            return true;
        } catch (RuntimeException other) {
            FAILED.add("unexpected exception: " + other);
            failures++;
            checks++;
            return false;
        }
    }

    /** An action that may fail with a {@link SaveException}. */
    private interface SaveAction {
        void run();
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static void check(String label, boolean ok) {
        checks++;
        if (!ok) {
            failures++;
            FAILED.add(label);
        }
    }
}
