package com.philia093.neofactory.entity;

import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtList;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Checks that entities survive a save game.
 * <p>
 * The player is the first entity, so it is also the test case: a world has to
 * remember where the player stood, where it looked and what it carried, and a type
 * that this build does not know must not stop a world from opening.
 */
class EntityPersistenceTest {

    /** Seed used by the tests. */
    private static final int SEED = 4321;

    @TempDir
    Path tempFolder;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void thePlayerComesBackOutOfTheSaveGame() throws Exception {
        WorldStorage storage = new WorldStorage(tempFolder.toFile());
        SaveSummary summary = storage.create("Entities", SEED, 0, 0);

        World world = new World(SEED, 0, 0);
        Player player = new Player(120.0f, 65.0f, 96.0f);
        player.facing().set(0.0f, 1.0f);
        player.inventory().set(3, ItemStack.of(Items.DIAMOND, 5));
        player.inventory().setSelectedSlot(3);
        world.entities().spawn(player);

        WorldSaver.save(storage, summary, levelData(), world, new PlayerInventory());

        WorldLoader loader = WorldLoader.open(storage, storage.list().get(0), 0, 0);
        Player restored = loader.world().entities().player();

        assertNotNull(restored);
        assertEquals(1, loader.world().entities().count());
        assertEquals(120.0f, restored.position().x, 1.0e-4f);
        assertEquals(96.0f, restored.position().z, 1.0e-4f);
        assertEquals(0.0f, restored.facing().x, 1.0e-4f);
        assertEquals(1.0f, restored.facing().y, 1.0e-4f);
        assertEquals(3, restored.inventory().selectedSlot());
        assertEquals(5, restored.inventory().get(3).count());
        assertEquals(Items.DIAMOND, restored.inventory().get(3).item());
    }

    @Test
    void anUnknownEntityTypeIsSkipped() {
        EntityManager manager = new EntityManager();
        NbtList list = new NbtList(SaveTags.ENTITIES);
        NbtCompound entry = new NbtCompound("");
        entry.putString(SaveTags.ENTITY_ID, "not-in-this-build");
        list.add(entry);

        assertEquals(0, manager.load(list, new World(SEED, 0, 0)));
        assertNull(manager.player());
        assertEquals(0, manager.count());
    }

    @Test
    void aDiscardedEntityLeavesTheWorld() {
        EntityManager manager = new EntityManager();
        World world = new World(SEED, 0, 0);
        Player player = new Player(10.0f, 65.0f, 10.0f);
        manager.spawn(player);

        player.discard();
        manager.update(world, 0.016f, 1, 1);

        assertEquals(0, manager.count());
    }

    /** Level data of a world that only holds entities. */
    private static LevelData levelData() {
        LevelData data = new LevelData();
        data.setWorldName("Entities");
        data.setSeed(SEED);
        data.setCreated(1L);
        data.setLastPlayed(1L);
        data.setSpawn(0, 0);
        return data;
    }
}
