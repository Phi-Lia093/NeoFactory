package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.material.Materials;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.save.LevelData;
import com.philia093.neofactory.world.save.SaveSummary;
import com.philia093.neofactory.world.save.SaveTags;
import com.philia093.neofactory.world.save.WorldLoader;
import com.philia093.neofactory.world.save.WorldSaver;
import com.philia093.neofactory.world.save.WorldStorage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a container keeps and what it hands over.
 * <p>
 * A container is the first block whose content is what a player put in and not what the block did with
 * it, so two things have to hold: what lies in it travels with the chunk, and a broken container gives
 * every item back instead of swallowing it. Both are asked without a window, the way the machines are.
 */
class ContainerBlockEntityTest {

    /** Block Y coordinate the tests build at. */
    private static final int BUILT_Y = 65;

    /** Block X coordinate the tests build at. */
    private static final int BUILT_X = 2;

    /** Block Z coordinate the tests build at. */
    private static final int BUILT_Z = 9;

    @TempDir
    Path tempFolder;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aChestKeepsTheItemsAndTheWearOfATool() {
        ChestBlockEntity chest = chest(BUILT_X, BUILT_Z);
        assertEquals(ChestBlockEntity.SLOTS, chest.slotCount(), "the slots of a chest");
        chest.contents().set(0, ItemStack.of(Items.STONE, 64));
        ItemStack used = ItemStack.of(Items.DIAMOND_PICKAXE, 1);
        used.setDamage(31);
        chest.contents().set(ChestBlockEntity.SLOTS - 1, used);

        NbtCompound data = new NbtCompound(SaveTags.DATA);
        chest.writeData(data);

        ChestBlockEntity reopened = chest(BUILT_X, BUILT_Z);
        reopened.readData(data);

        assertEquals(Items.STONE, reopened.contents().get(0).item());
        assertEquals(64, reopened.contents().get(0).count());
        assertEquals(31, reopened.contents().get(ChestBlockEntity.SLOTS - 1).damage(),
                "the wear of a piece belongs to the slot it lies in");
        assertTrue(reopened.contents().get(1).isEmpty(), "an empty slot stays empty");
        assertEquals(ChestBlockEntity.SLOTS, reopened.slotCount(), "a stored chest keeps its size");
    }

    @Test
    void anEmptyChestIsStoredAndComesBackEmpty() {
        ChestBlockEntity chest = chest(BUILT_X, BUILT_Z);
        assertTrue(chest.isEmpty());

        NbtCompound data = new NbtCompound(SaveTags.DATA);
        chest.writeData(data);

        ChestBlockEntity reopened = chest(BUILT_X, BUILT_Z);
        reopened.readData(data);

        assertTrue(reopened.isEmpty());
    }

    @Test
    void aBrokenChestHandsEveryItemToTheWorld() {
        ChestBlockEntity chest = chest(BUILT_X, BUILT_Z);
        chest.contents().set(0, ItemStack.of(Items.STONE, 5));
        chest.contents().set(14, ItemStack.of(Items.COAL, 3));
        List<ItemStack> dropped = new ArrayList<>();

        chest.onBroken((stack, worldX, worldY) -> dropped.add(stack), 4.5f, 6.5f);

        assertEquals(2, dropped.size(), "only the filled slots are handed over");
        assertEquals(Items.STONE, dropped.get(0).item());
        assertEquals(3, dropped.get(1).count());
        assertTrue(chest.isEmpty(), "the chest is empty after the break");
    }

    @Test
    void aChestInTheWorldKeepsItsItemsAcrossASaveGame() throws Exception {
        int seed = 5150;
        WorldStorage storage = new WorldStorage(tempFolder.toFile());
        SaveSummary summary = storage.create("Chests", seed, 0, 0);

        World world = new World(seed, 0, 0);
        world.setBlock(BUILT_X, BUILT_Y, BUILT_Z, Blocks.CHEST);
        BlockEntity placed = world.ensureBlockEntity(BUILT_X, BUILT_Y, BUILT_Z);
        assertInstanceOf(ChestBlockEntity.class, placed, "a built chest carries a container");
        ChestBlockEntity chest = (ChestBlockEntity) placed;
        chest.contents().set(3, ItemStack.of(Materials.IRON.ingot(), 7));

        // A container does no work: forty ticks of the world may not change what lies in it.
        for (int tick = 0; tick < 40; tick++) {
            world.tick(TickClock.TICK_SECONDS);
        }
        assertEquals(7, chest.contents().get(3).count(), "a container works on nothing");

        WorldSaver.save(storage, summary, levelData(seed), world, new PlayerInventory());

        WorldLoader loader = WorldLoader.open(storage, storage.list().get(0), 0, 0);
        BlockEntity reopened = loader.world().blockEntity(BUILT_X, BUILT_Y, BUILT_Z);

        assertNotNull(reopened, "the chest was lost with the save game");
        assertInstanceOf(ChestBlockEntity.class, reopened);
        assertEquals(7, ((ChestBlockEntity) reopened).contents().get(3).count(),
                "the items of a chest are gone after the world was stored");
    }

    @Test
    void replacingTheChestTakesTheContainerWithIt() {
        World world = new World(7331, 0, 0);
        world.setBlock(BUILT_X, BUILT_Y, BUILT_Z, Blocks.CHEST);
        world.ensureBlockEntity(BUILT_X, BUILT_Y, BUILT_Z);
        assertEquals(1, world.blockEntityCount());

        world.setBlock(BUILT_X, BUILT_Y, BUILT_Z, Blocks.AIR);

        assertEquals(0, world.blockEntityCount(), "a chest stayed behind where nothing stands");
        assertNull(world.blockEntity(BUILT_X, BUILT_Y, BUILT_Z));
    }

    /** A chest placed at a cell of a chunk, its position already set. */
    private static ChestBlockEntity chest(int x, int z) {
        ChestBlockEntity entity = (ChestBlockEntity) BlockEntityTypes.CHEST.create();
        entity.setPosition(x, BUILT_Y, z);
        return entity;
    }

    /** Level data pointing at the origin, where the tests build. */
    private static LevelData levelData(int seed) {
        LevelData data = new LevelData();
        data.setWorldName("Chests");
        data.setSeed(seed);
        data.setCreated(1L);
        data.setLastPlayed(1L);
        data.setSpawn(0, 0);
        return data;
    }
}
