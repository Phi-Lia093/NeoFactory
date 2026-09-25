package com.philia093.neofactory.entity;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a ladder does with the body that holds on to it.
 * <p>
 * A ladder is the one block of the game a body climbs on: it does not stand in the way - its cell is walked
 * through like air - but it carries the body, which sinks slowly instead of falling and climbs while the
 * player walks into the wall the ladder hangs on or holds the jump key. All of that is plain arithmetic on
 * a body and the shapes around it, so it is checked without a window.
 */
class PlayerLadderTest {

    /** Seed the cases are built with. */
    private static final int SEED = 777;

    /** Length of one frame, the twentieth of a second the world advances in. */
    private static final float FRAME = 1.0f / 20.0f;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theLadderOfTheTestWorldIsWhereTheBodyStands() {
        World world = new World(SEED, 0, 0);
        Player player = Player.spawnOnGround(world, world.spawnX(), world.spawnZ());
        int floor = placeLadder(world, player);

        assertEquals(Blocks.LADDER, world.getBlock(player.blockX(), floor, player.blockZ()),
                "the ladder stands in the cell of the feet");
        assertTrue(world.getBlock(player.blockX(), floor, player.blockZ()).isClimbable(),
                "and it is a block a body climbs on");
    }

    @Test
    void aLadderIsNotAShapeThatStopsABody() {
        World world = new World(SEED, 0, 0);
        Player player = Player.spawnOnGround(world, world.spawnX(), world.spawnZ());
        int floor = placeLadder(world, player);

        assertFalse(player.collides(world, player.position().x, floor, player.position().z),
                "a body stands in the cell of a ladder instead of being stopped by it");
    }

    @Test
    void aBodyThatHoldsOnToLadderRungsSinksSlowly() {
        World world = new World(SEED, 0, 0);
        Player player = Player.spawnOnGround(world, world.spawnX(), world.spawnZ());
        int ladder = placeLadder(world, player);
        // The body hangs in the ladder with nothing under its feet: a fall of two fifths of a second covers
        // more than two blocks, so a body the ladder did not carry would have fallen right through it.
        player.position().y = ladder + 2.0f;

        for (int frame = 0; frame < 8; frame++) {
            player.update(world, FRAME);
        }

        assertTrue(player.position().y > ladder + 1.0f,
                "the rungs carry the body, it is at " + player.position());
    }

    @Test
    void aBodyClimbsALadderByWalkingIntoTheWall() {
        World world = new World(SEED, 0, 0);
        Player player = Player.spawnOnGround(world, world.spawnX(), world.spawnZ());
        placeLadder(world, player);
        // The view looks south, where the wall stands, so walking forward pushes the body against the rungs.
        player.setView(0.0f, 0.0f);
        player.setMoveInput(0.0f, 1.0f);
        float start = player.position().y;

        for (int frame = 0; frame < 8; frame++) {
            player.update(world, FRAME);
        }

        assertEquals(Constants.LADDER_CLIMB_SPEED * 8 * FRAME, player.position().y - start, 0.05f,
                "walking into the wall carries the body up the rungs");
    }

    @Test
    void aJumpClimbsALadderWithoutLeavingTheGroundFirst() {
        World world = new World(SEED, 0, 0);
        Player player = Player.spawnOnGround(world, world.spawnX(), world.spawnZ());
        int ladder = placeLadder(world, player);
        // A body in a ladder is never on the ground, so a jump that only the ground allowed would be refused.
        player.position().y = ladder + 1.0f;
        player.update(world, FRAME);

        player.jump();

        assertEquals(Constants.LADDER_CLIMB_SPEED, player.velocity.y, 1.0e-4f,
                "the jump key climbs a ladder");
    }

    /**
     * Builds a wall of three blocks and a ladder on its north side in the column of a body.
     *
     * @param world world to build in
     * @param player body the ladder is built around
     * @return height of the lowest cell of the ladder, the one the feet of the body are in
     */
    private static int placeLadder(World world, Player player) {
        int x = player.blockX();
        int z = player.blockZ();
        int floor = world.surfaceY(x, z);
        for (int step = 0; step < 3; step++) {
            world.setBlock(x, floor + step, z + 1, Blocks.STONE);
            world.setBlock(x, floor + step, z, Blocks.LADDER);
            world.setState(x, floor + step, z, Blocks.LADDER.states()
                    .stateOf(Map.of("facing", "north")));
        }
        return floor;
    }
}
