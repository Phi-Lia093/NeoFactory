package com.philia093.neofactory.entity;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a body falling through the world stops on the ground it falls towards.
 * <p>
 * The ground of a world of cubes is one block thick, and a body is moved by the length of a frame: a frame
 * that took long enough - the first ones of a world, or a stall - used to make a player step from above
 * the ground to below it in one go, where the test of the step found nothing to stop it. The player then
 * fell through a floor that was right under their feet, out of the world, and the world refused to answer
 * for a cell below itself and stopped the game. Both are checked here, because both are pure arithmetic:
 * no window and no graphics card is involved in a body and the ground beneath it.
 */
class PlayerFallTest {

    /** Seed the cases are built with; any world has ground under its spawn. */
    private static final int SEED = 777;

    /** Length of a frame that took far too long, in seconds. */
    private static final float SLOW_FRAME = 0.5f;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aBodyThatFallsInSlowFramesStillLandsOnTheGround() {
        World world = new World(SEED, 0, 0);
        Player player = Player.spawnOnGround(world, world.spawnX(), world.spawnZ());
        int surface = world.surfaceY(player.blockX(), player.blockZ());
        player.position().y = surface + 40.0f;

        for (int frame = 0; frame < 20 && !player.isOnGround(); frame++) {
            player.update(world, SLOW_FRAME);
        }

        assertTrue(player.isOnGround(), "the body came to rest, it is at " + player.position());
        assertEquals(surface, player.blockY(), "the feet stand on the ground of the column, not in it");
        assertTrue(world.isSolid(player.blockX(), player.blockY() - 1, player.blockZ()),
                "the block under the feet is what stopped it");
    }

    @Test
    void aBodyFallsToTheBottomOfTheWorldWithoutLeavingIt() {
        World world = new World(SEED, 0, 0);
        Player player = Player.spawnOnGround(world, world.spawnX(), world.spawnZ());
        int x = player.blockX();
        int z = player.blockZ();
        // The ground is taken away, so there is nothing to land on between the body and the bottom.
        for (int y = player.blockY() - 1; y >= Constants.MIN_Y; y--) {
            world.setBlock(x, y, z, Blocks.AIR);
        }

        for (int frame = 0; frame < 200; frame++) {
            player.update(world, SLOW_FRAME);
        }

        assertTrue(player.position().y >= Constants.MIN_Y,
                "the body stopped at the bottom of the world, it is at " + player.position());
        assertTrue(player.isOnGround(), "the bottom of the world is a floor, not a fall that never ends");
    }
}
