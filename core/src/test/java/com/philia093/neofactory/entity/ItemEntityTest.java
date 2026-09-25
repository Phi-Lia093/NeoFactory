package com.philia093.neofactory.entity;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.WorldType;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what an item on the ground does.
 * <p>
 * An item is a body of the world like a player is one: it falls, a block stops it and it comes to rest on top
 * of the shape it lands on, a block built over it pushes it out, and it turns around its own axis so a player
 * can tell it from a block. On top of that it has to wait a moment before it may be taken, walk into the
 * inventory when the player comes close, and it must not be thrown away when there is no room for it.
 */
class ItemEntityTest {

    /** Seed used by the tests. */
    private static final int SEED = 909;

    /** Position the tests drop items at, in world units. */
    private static final float DROP_X = 100.0f;

    /** Second coordinate of the position the tests drop items at. */
    private static final float DROP_Z = 100.0f;

    /**
     * Height a case that does not care about falling works at.
     * <p>
     * The cases of the pickup rules measure the distance between an item and a player across the ground, so
     * both stand on it: an item that falls away from the player would be picked up by the world and not by the
     * rules a case is about.
     */
    private static final float DROP_HEIGHT = 65.0f * Constants.BLOCK_SIZE;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void anItemWalksIntoTheInventoryOfThePlayer() {
        World world = new World(SEED, 0, 0, null, WorldType.FLAT);
        float ground = groundOf(world);
        Player player = new Player(DROP_X, ground, DROP_Z);
        world.entities().spawn(player);
        ItemEntity item = new ItemEntity(DROP_X, ground, DROP_Z, ItemStack.of(Items.DIAMOND, 5));
        world.entities().spawn(item);

        // The pickup delay has to pass before anything is picked up.
        item.update(world, 0.1f);
        assertFalse(item.isRemoved(), "the item was picked up although it just appeared");
        assertEquals(0, player.inventory().countOf(Items.DIAMOND));

        item.update(world, 1.0f);
        assertTrue(item.isRemoved());
        assertEquals(5, player.inventory().countOf(Items.DIAMOND));
    }

    @Test
    void whatDoesNotFitStaysOnTheGround() {
        World world = new World(SEED, 0, 0, null, WorldType.FLAT);
        float ground = groundOf(world);
        Player player = new Player(DROP_X, ground, DROP_Z);
        fill(player.inventory());
        world.entities().spawn(player);
        ItemEntity item = new ItemEntity(DROP_X, ground, DROP_Z, ItemStack.of(Items.DIAMOND, 5));
        world.entities().spawn(item);

        item.update(world, 1.0f);

        assertFalse(item.isRemoved(), "a full inventory swallowed the items");
        assertEquals(5, item.stack().count());
    }

    @Test
    void anItemIsGoneAfterAWhile() {
        World world = new World(SEED, 0, 0, null, WorldType.FLAT);
        ItemEntity item = new ItemEntity(DROP_X, groundOf(world), DROP_Z, ItemStack.of(Items.DIAMOND, 1));
        world.entities().spawn(item);

        item.update(world, 10.0f);
        assertFalse(item.isRemoved());
        item.update(world, 400.0f);
        assertTrue(item.isRemoved());
    }

    @Test
    void theStoredItemComesBack() {
        EntityManager manager = new EntityManager();
        manager.spawn(new ItemEntity(DROP_X, DROP_HEIGHT, DROP_Z, ItemStack.of(Items.DIAMOND, 7)));

        EntityManager restored = new EntityManager();
        assertEquals(1, restored.load(manager.save(), new World(SEED, 0, 0, null, WorldType.FLAT)));

        Entity entity = restored.all().get(0);
        assertTrue(entity instanceof ItemEntity);
        assertEquals(7, ((ItemEntity) entity).stack().count());
        assertEquals(Items.DIAMOND, ((ItemEntity) entity).stack().item());
    }

    @Test
    void anItemIsDrawnTowardsANearbyPlayer() {
        World world = new World(SEED, 0, 0, null, WorldType.FLAT);
        float ground = groundOf(world);
        Player player = new Player(DROP_X, ground, DROP_Z);
        world.entities().spawn(player);
        float twoBlocks = 2.0f * Constants.BLOCK_SIZE;
        ItemEntity item = new ItemEntity(DROP_X + twoBlocks, ground, DROP_Z, ItemStack.of(Items.DIAMOND, 1));
        world.entities().spawn(item);

        // Nothing happens while the pickup delay runs.
        item.update(world, 0.4f);
        float waiting = item.position().dst(player.position());

        item.update(world, 0.5f);
        assertTrue(item.position().dst(player.position()) < waiting,
                "the item was pulled towards the player");
    }

    @Test
    void anItemTooFarAwayIsLeftAlone() {
        World world = new World(SEED, 0, 0, null, WorldType.FLAT);
        float ground = groundOf(world);
        Player player = new Player(DROP_X, ground, DROP_Z);
        world.entities().spawn(player);
        float farAway = 6.0f * Constants.BLOCK_SIZE;
        ItemEntity item = new ItemEntity(DROP_X + farAway, ground, DROP_Z, ItemStack.of(Items.DIAMOND, 1));
        world.entities().spawn(item);

        item.update(world, 1.0f);

        assertEquals(farAway, item.position().dst(player.position()), 1.0e-3f,
                "an item outside the magnet range stays where it is: item " + item.position()
                        + ", player " + player.position());
    }

    @Test
    void aThrownItemSlidesAndComesToRest() {
        World world = new World(SEED, 0, 0, null, WorldType.FLAT);
        ItemEntity item = new ItemEntity(DROP_X, groundOf(world), DROP_Z, ItemStack.of(Items.DIAMOND, 1));
        item.velocity().set(Constants.BLOCK_SIZE * 3.0f, 0.0f, 0.0f);
        world.entities().spawn(item);

        item.update(world, 0.1f);
        assertTrue(item.position().x > DROP_X, "the item was thrown");

        for (int frame = 0; frame < 20; frame++) {
            item.update(world, 0.1f);
        }
        assertTrue(item.velocity().isZero(), "the slide came to an end");
        float travelled = item.position().x - DROP_X;
        assertTrue(travelled > 0.0f && travelled < 5.0f * Constants.BLOCK_SIZE,
                "the item slid a short distance: " + travelled);
    }

    @Test
    void aFullInventoryDoesNotAttractAnything() {
        World world = new World(SEED, 0, 0, null, WorldType.FLAT);
        float ground = groundOf(world);
        Player player = new Player(DROP_X, ground, DROP_Z);
        fill(player.inventory());
        world.entities().spawn(player);
        float twoBlocks = 2.0f * Constants.BLOCK_SIZE;
        ItemEntity item = new ItemEntity(DROP_X + twoBlocks, ground, DROP_Z, ItemStack.of(Items.DIAMOND, 1));
        world.entities().spawn(item);

        item.update(world, 1.0f);

        assertEquals(twoBlocks, item.position().dst(player.position()), 1.0e-3f,
                "nothing is pulled while no place is free");
        assertFalse(item.isRemoved(), "and nothing is picked up either");
    }

    @Test
    void anItemIsPulledInAgainWhenAPlaceIsFree() {
        World world = new World(SEED, 0, 0, null, WorldType.FLAT);
        float ground = groundOf(world);
        Player player = new Player(DROP_X, ground, DROP_Z);
        fill(player.inventory());
        world.entities().spawn(player);
        float twoBlocks = 2.0f * Constants.BLOCK_SIZE;
        ItemEntity item = new ItemEntity(DROP_X + twoBlocks, ground, DROP_Z, ItemStack.of(Items.DIAMOND, 1));
        world.entities().spawn(item);
        item.update(world, 1.0f);
        float waiting = item.position().dst(player.position());

        // Freeing a single slot is enough to attract the item again.
        player.inventory().set(0, ItemStack.EMPTY);
        item.update(world, 0.5f);

        assertTrue(item.position().dst(player.position()) < waiting,
                "the item came closer as soon as a place was free");
    }

    @Test
    void anItemFallsAndRestsOnTheGround() {
        World world = new World(SEED, 0, 0, null, WorldType.FLAT);
        float ground = groundOf(world);
        ItemEntity item = new ItemEntity(DROP_X, ground + 4.0f, DROP_Z, ItemStack.of(Items.DIAMOND, 1));
        world.entities().spawn(item);

        for (int frame = 0; frame < 40; frame++) {
            item.update(world, 1.0f / 20.0f);
        }

        assertEquals(ground, item.position().y, 1.0e-3f, "the item came to rest on the ground");
        for (int frame = 0; frame < 20; frame++) {
            item.update(world, 1.0f / 20.0f);
        }
        assertEquals(ground, item.position().y, 1.0e-3f, "and it stays there");
    }

    @Test
    void anItemRestsOnTopOfTheShapeItLandsOn() {
        World world = new World(SEED, 0, 0, null, WorldType.FLAT);
        float ground = groundOf(world);
        int cell = (int) ground;
        world.setBlock((int) DROP_X, cell, (int) DROP_Z, Blocks.STONE_SLAB);
        world.setState((int) DROP_X, cell, (int) DROP_Z, Blocks.STONE_SLAB.states()
                .stateOf(Map.of("type", "bottom")));
        ItemEntity item = new ItemEntity(DROP_X, ground + 3.0f, DROP_Z, ItemStack.of(Items.DIAMOND, 1));
        world.entities().spawn(item);

        for (int frame = 0; frame < 40; frame++) {
            item.update(world, 1.0f / 20.0f);
        }

        assertEquals(ground + 0.5f, item.position().y, 1.0e-3f,
                "the item rests on the half a slab fills, not on the top of the cell");
    }

    @Test
    void anItemIsStoppedByAWall() {
        World world = new World(SEED, 0, 0, null, WorldType.FLAT);
        float ground = groundOf(world);
        int cell = (int) ground;
        world.setBlock((int) DROP_X + 1, cell, (int) DROP_Z, Blocks.STONE);
        ItemEntity item = new ItemEntity(DROP_X, ground, DROP_Z, ItemStack.of(Items.DIAMOND, 1));
        item.velocity().set(Constants.BLOCK_SIZE * 8.0f, 0.0f, 0.0f);
        world.entities().spawn(item);

        for (int frame = 0; frame < 40; frame++) {
            item.update(world, 1.0f / 20.0f);
        }

        assertTrue(item.position().x < DROP_X + 1.0f,
                "the wall stopped the item, it is at " + item.position());
        assertEquals(0.0f, item.velocity().x, 1.0e-4f, "and its speed is gone");
    }

    @Test
    void aBlockBuiltOverAnItemPushesItOnTop() {
        World world = new World(SEED, 0, 0, null, WorldType.FLAT);
        float ground = groundOf(world);
        int cell = (int) ground;
        ItemEntity item = new ItemEntity(DROP_X, ground, DROP_Z, ItemStack.of(Items.DIAMOND, 1));
        world.entities().spawn(item);
        // A block is built into the very cell the item lies in: the item is inside of it now.
        world.setBlock((int) DROP_X, cell, (int) DROP_Z, Blocks.STONE);

        item.update(world, 1.0f / 20.0f);

        assertEquals(cell + 1.0f, item.position().y, 1.0e-3f,
                "the item was squeezed on top of the block built over it");
    }

    /** Height the ground of the test world stands at, where an item comes to rest. */
    private static float groundOf(World world) {
        return world.surfaceY((int) DROP_X, (int) DROP_Z) * Constants.BLOCK_SIZE;
    }

    /** Fills every slot, so nothing can be picked up. */
    private static void fill(PlayerInventory inventory) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            inventory.set(slot, ItemStack.of(Items.STONE, 64));
        }
    }
}
