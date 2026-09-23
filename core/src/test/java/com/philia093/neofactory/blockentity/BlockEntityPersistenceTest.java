package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.machine.Machine;
import com.philia093.neofactory.machine.SmeltingMachine;
import com.philia093.neofactory.recipe.RecipeLoader;
import com.philia093.neofactory.recipe.RecipeRegistry;
import com.philia093.neofactory.recipe.RecipeType;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtList;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.save.ChunkCodec;
import com.philia093.neofactory.world.save.LevelData;
import com.philia093.neofactory.world.save.SaveSummary;
import com.philia093.neofactory.world.save.SaveTags;
import com.philia093.neofactory.world.save.WorldLoader;
import com.philia093.neofactory.world.save.WorldSaver;
import com.philia093.neofactory.world.save.WorldStorage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that what a block carries beyond its id survives the world.
 * <p>
 * A machine is the interesting case, because it holds everything a cell cannot: the
 * slots, the fuel and the work that was done. All of it has to come back out of a chunk
 * file, and a chunk that holds an entity the game no longer knows - or one whose block
 * is gone - has to open without it instead of failing or leaving a machine running where
 * nothing stands.
 */
class BlockEntityPersistenceTest {

    /** Seed of the worlds the tests build. */
    private static final int SEED = 4242;

    /** Cell the tests build their machines in. */
    private static final int BUILT_X = 5;

    /** Second coordinate of the cell the tests build in. */
    private static final int BUILT_Y = 6;

    /** Time the recipe of these tests takes. */
    private static final float CRAFT_SECONDS = 10.0f;

    @TempDir
    Path tempFolder;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @BeforeEach
    void setUp() {
        RecipeRegistry.clear();
        RecipeRegistry.register(RecipeLoader.parse(RecipeType.SMELTING, "iron_ingot",
                "{ \"ingredient\": \"iron_ore\", \"result\": { \"item\": \"iron_ingot\" },"
                        + " \"time\": " + CRAFT_SECONDS + " }"));
    }

    @Test
    void aChunkKeepsTheBlockEntitiesItWasGiven() {
        Chunk chunk = new Chunk(0, 0);
        MachineBlockEntity entity = furnace(BUILT_X, BUILT_Y);

        chunk.setBlockEntity(entity);

        assertEquals(entity, chunk.blockEntity(BUILT_X, Chunk.flatY(Chunk.LAYER_OBJECT), BUILT_Y));
        assertEquals(1, chunk.blockEntityCount());

        chunk.removeBlockEntity(BUILT_X, Chunk.flatY(Chunk.LAYER_OBJECT), BUILT_Y);

        assertEquals(0, chunk.blockEntityCount());
        assertNull(chunk.blockEntity(BUILT_X, Chunk.flatY(Chunk.LAYER_OBJECT), BUILT_Y));
    }

    @Test
    void aMachineTravelsThroughTheChunkFile() {
        Chunk written = new Chunk(0, 0);
        written.setRawId(BUILT_X, Chunk.flatY(Chunk.LAYER_OBJECT), BUILT_Y, Blocks.FURNACE.id());
        MachineBlockEntity furnace = furnace(BUILT_X, BUILT_Y);
        furnace.machine().inventory().set(SmeltingMachine.INPUT, ItemStack.of(Items.IRON_ORE, 3));
        furnace.machine().inventory().set(SmeltingMachine.FUEL, ItemStack.of(Items.COAL, 2));
        written.setBlockEntity(furnace);

        Chunk read = new Chunk(0, 0);
        ChunkCodec.read(read, ChunkCodec.write(written));

        BlockEntity restored = read.blockEntity(BUILT_X, Chunk.flatY(Chunk.LAYER_OBJECT), BUILT_Y);
        assertNotNull(restored, "the machine was lost while writing or reading");
        assertEquals("furnace", restored.type().name());
        assertEquals(BUILT_X, restored.x());
        assertEquals(BUILT_Y, restored.z(), "the second horizontal coordinate came back");
        assertEquals(Chunk.flatY(Chunk.LAYER_OBJECT), restored.y(), "and its height");
        Machine machine = ((MachineBlockEntity) restored).machine();
        assertEquals(3, machine.inventory().get(SmeltingMachine.INPUT).count());
        assertEquals(2, machine.inventory().get(SmeltingMachine.FUEL).count());
    }

    @Test
    void anEntityWithoutItsBlockIsDropped() {
        Chunk written = new Chunk(0, 0);
        // The cell is empty: the machine has nothing to stand on.
        written.setBlockEntity(furnace(BUILT_X, BUILT_Y));

        Chunk read = new Chunk(0, 0);
        ChunkCodec.read(read, ChunkCodec.write(written));

        assertEquals(0, read.blockEntityCount());
    }

    @Test
    void anUnknownTypeIsSkipped() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setRawId(BUILT_X, BUILT_Y, Chunk.LAYER_OBJECT, Blocks.FURNACE.id());
        NbtCompound data = ChunkCodec.write(chunk);
        NbtList entities = data.getList(SaveTags.BLOCK_ENTITIES);
        NbtCompound entry = new NbtCompound("");
        entry.putInt(SaveTags.BLOCK_ENTITY_X, BUILT_X);
        entry.putInt(SaveTags.BLOCK_ENTITY_Y, Chunk.flatY(Chunk.LAYER_OBJECT));
        entry.putInt(SaveTags.BLOCK_ENTITY_Z, BUILT_Y);
        entry.putString(SaveTags.BLOCK_ENTITY_ID, "a_machine_that_never_shipped");
        entities.add(entry);

        Chunk read = new Chunk(0, 0);
        ChunkCodec.read(read, data);

        assertEquals(0, read.blockEntityCount());
    }

    @Test
    void aMachineWorksInTheWorldAndKeepsItAcrossASaveGame() throws Exception {
        WorldStorage storage = new WorldStorage(tempFolder.toFile());
        SaveSummary summary = storage.create("Machines", SEED, 0, 0);

        // A player builds a furnace and fills it.
        World world = new World(SEED, 0, 0);
        world.setFlatBlock(BUILT_X, BUILT_Y, Chunk.LAYER_OBJECT, Blocks.FURNACE);
        MachineBlockEntity placed = furnace(BUILT_X, BUILT_Y);
        world.addBlockEntity(placed);
        placed.machine().inventory().set(SmeltingMachine.INPUT, ItemStack.of(Items.IRON_ORE, 2));
        placed.machine().inventory().set(SmeltingMachine.FUEL, ItemStack.of(Items.COAL, 1));

        // Twelve seconds of the world: the machine works while the game runs.
        tick(world, 240);

        assertEquals(1, placed.machine().inventory().get(SmeltingMachine.OUTPUT).count(),
                "the furnace did not smelt while the world ran");

        WorldSaver.save(storage, summary, levelData(), world, new PlayerInventory());

        // The player comes back to the same spot.
        WorldLoader loader = WorldLoader.open(storage, storage.list().get(0), 0, 0);
        BlockEntity reopened = loader.world().flatBlockEntity(BUILT_X, BUILT_Y, Chunk.LAYER_OBJECT);

        assertNotNull(reopened, "the furnace was lost with the save game");
        Machine machine = ((MachineBlockEntity) reopened).machine();
        assertEquals(1, machine.inventory().get(SmeltingMachine.OUTPUT).count(),
                "the ingot the furnace made is gone");
        assertEquals(1, machine.inventory().get(SmeltingMachine.INPUT).count(),
                "the ore that was left is gone");
        assertTrue(machine.isRunning(), "the coal is still burning");
    }

    @Test
    void replacingTheBlockTakesItsEntityWithIt() {
        World world = new World(SEED, 0, 0);
        world.setFlatBlock(BUILT_X, BUILT_Y, Chunk.LAYER_OBJECT, Blocks.FURNACE);
        world.addBlockEntity(furnace(BUILT_X, BUILT_Y));
        assertEquals(1, world.blockEntityCount());

        world.setFlatBlock(BUILT_X, BUILT_Y, Chunk.LAYER_OBJECT, Blocks.AIR);

        assertEquals(0, world.blockEntityCount(),
                "a machine stayed behind where nothing stands");
        assertNull(world.flatBlockEntity(BUILT_X, BUILT_Y, Chunk.LAYER_OBJECT));
    }

    /** Runs the world for a number of ticks, the way the frames of the game would. */
    private static void tick(World world, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            world.tick(TickClock.TICK_SECONDS);
        }
    }

    /** An empty furnace placed at a cell. */
    private static MachineBlockEntity furnace(int x, int y) {
        MachineBlockEntity entity = (MachineBlockEntity) BlockEntityTypes.FURNACE.create();
        entity.setPosition(x, Chunk.flatY(Chunk.LAYER_OBJECT), y);
        return entity;
    }

    /** Level data pointing at the origin, where the tests build. */
    private static LevelData levelData() {
        LevelData data = new LevelData();
        data.setWorldName("Machines");
        data.setSeed(SEED);
        data.setCreated(1L);
        data.setLastPlayed(1L);
        data.setSpawn(0, 0);
        return data;
    }
}
