package com.philia093.neofactory.item;

import com.philia093.neofactory.entity.ItemEntity;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.WorldType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a thrown stack does.
 * <p>
 * A stack a player drops is thrown, not put down: it leaves the hand in front of the eyes, flies along the
 * line of sight with the speed of a throw and is left to the world from there - gravity pulls it down, a block
 * stops it and it comes to rest on the shape it lands on, see {@link ItemEntity}. The line of sight is the
 * only direction a player names with the mouse, so it is what decides where a dropped stack ends up.
 */
class WorldDropsTest {

    /** Seed the cases are built with. */
    private static final int SEED = 4242;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aThrownStackLeavesTheHandAlongTheLineOfSight() {
        World world = flatWorld();
        Player player = Player.spawnOnGround(world, world.spawnX(), world.spawnZ());
        player.setView(90.0f, 0.0f); // a quarter turn to the left: the view looks west
        WorldDrops drops = new WorldDrops(world);

        drops.throwFrom(player, ItemStack.of(Items.STONE, 1));

        ItemEntity thrown = thrownItem(world);
        assertEquals(Items.STONE, thrown.stack().item(), "the thrown stack carries what was dropped");
        assertTrue(thrown.velocity().x < 0.0f, "the throw carries the speed the view points in");
        assertEquals(0.0f, thrown.velocity().z, 1.0e-4f, "and nothing to the side");
        assertTrue(thrown.position().x < player.position().x, "the stack left the hand towards the view");
        assertTrue(thrown.position().y > player.position().y,
                "the stack leaves the hand at the height of the eyes, not on the ground");
    }

    @Test
    void aThrownStackLandsACoupleOfBlocksAway() {
        World world = flatWorld();
        Player player = Player.spawnOnGround(world, world.spawnX(), world.spawnZ());
        player.setView(90.0f, 0.0f);
        WorldDrops drops = new WorldDrops(world);
        drops.throwFrom(player, ItemStack.of(Items.STONE, 1));
        ItemEntity thrown = thrownItem(world);
        float ground = world.surfaceY(player.blockX(), player.blockZ()) * Constants.BLOCK_SIZE;

        for (int frame = 0; frame < 60; frame++) {
            thrown.update(world, 1.0f / 20.0f);
        }

        float flown = player.position().x - thrown.position().x;
        assertTrue(flown > 1.0f && flown < 10.0f, "the stack flew a few blocks: " + flown);
        assertEquals(ground, thrown.position().y, 1.0e-3f, "and it came to rest on the ground");
    }

    @Test
    void nothingIsThrownForAnEmptyStack() {
        World world = flatWorld();
        Player player = Player.spawnOnGround(world, world.spawnX(), world.spawnZ());
        WorldDrops drops = new WorldDrops(world);

        drops.throwFrom(player, ItemStack.EMPTY);

        assertEquals(0, world.entities().all().size(), "an empty stack leaves nothing behind");
    }

    /** Flat world with the table of blocks a flat column is, so a throw lands on a known height. */
    private static World flatWorld() {
        return new World(SEED, 0, 0, null, WorldType.FLAT);
    }

    /** The item the test world holds, which is the thrown one. */
    private static ItemEntity thrownItem(World world) {
        for (var entity : world.entities().all()) {
            if (entity instanceof ItemEntity) {
                return (ItemEntity) entity;
            }
        }
        throw new AssertionError("no item was thrown");
    }
}
