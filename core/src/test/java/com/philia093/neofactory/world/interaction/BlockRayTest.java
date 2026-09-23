package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Checks the walk a line of sight takes through the cells of a world.
 * <p>
 * Four things about the walk are easy to get wrong and are pinned down here: the face a ray enters a
 * cell through, which is what a block is built against; the reach, which stops a player from editing
 * the landscape from afar; the height, because a world of cubes is walked in three axes and not only
 * in the plane the game draws; and the cell the ray starts in, which it did not enter through
 * anything.
 */
class BlockRayTest {

    /** Height the object layer of the flat view stands at, where the tests build their walls. */
    private static final int OBJECT_Y = Chunk.flatY(Chunk.LAYER_OBJECT);

    /** Height of the ground layer, where the tests look down onto. */
    private static final int FLOOR_Y = Chunk.flatY(Chunk.LAYER_FLOOR);

    /** Reach of the player, the distance a ray may walk. */
    private static final float REACH = 4.5f;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aRayEntersTheCellItMeetsThroughTheFaceItComesFrom() {
        World world = wall(5);

        BlockRay.Hit hit = cast(world, 0.5f, OBJECT_Y + 0.5f, 0.5f, 1.0f, 0.0f, 0.0f);

        assertNotNull(hit, "the wall was not met");
        assertEquals(5, hit.x());
        assertEquals(OBJECT_Y, hit.y());
        assertEquals(0, hit.z());
        assertEquals(BlockFace.WEST, hit.face(), "a ray walking east enters through the west face");
        assertEquals(4.5f, hit.distance(), 1.0e-4f, "the distance is measured in blocks");
    }

    @Test
    void aRayWalkingBackwardsEntersThroughTheOppositeFace() {
        World world = wall(0);

        BlockRay.Hit hit = cast(world, 5.5f, OBJECT_Y + 0.5f, 0.5f, -1.0f, 0.0f, 0.0f);

        assertNotNull(hit);
        assertEquals(0, hit.x());
        assertEquals(BlockFace.EAST, hit.face());
    }

    @Test
    void aRayStopsAtItsReach() {
        World world = wall(20);

        assertNull(cast(world, 0.5f, OBJECT_Y + 0.5f, 0.5f, 1.0f, 0.0f, 0.0f),
                "a ray reached a wall far beyond the reach of the player");
    }

    @Test
    void aRayLookingDownMeetsTheTopOfTheGround() {
        World world = new World(7, 0, 0);

        BlockRay.Hit hit = cast(world, 0.5f, OBJECT_Y + 0.5f, 0.5f, 0.0f, -1.0f, 0.0f);

        assertNotNull(hit, "the ground was not met");
        assertEquals(FLOOR_Y, hit.y(), "the floor of a flat world stands at the ground layer");
        assertEquals(BlockFace.TOP, hit.face(), "a ray looking down enters through the top face");
    }

    @Test
    void aRayWalksTheCellsOfASlantedDirection() {
        World world = wall(3, 3);

        BlockRay.Hit hit = cast(world, 0.5f, OBJECT_Y + 0.5f, 0.5f, 1.0f, 0.0f, 1.0f);

        assertNotNull(hit, "the wall across the diagonal was not met");
        assertEquals(3, hit.x());
        assertEquals(3, hit.z());
        // A perfect diagonal crosses the corner of four cells at once, so the face is whatever the walk
        // picks first: a tie between the boundary of X and the boundary of Z steps along X, which is
        // why the ray came from the west one step before it enters through the north face.
        assertEquals(BlockFace.NORTH, hit.face(), "a tie between two boundaries steps along X first");
    }

    /** A world with a wall of planks in the object layer of one column. */
    private static World wall(int x) {
        World world = new World(7, 0, 0);
        world.setBlock(x, OBJECT_Y, 0, Blocks.PLANKS_OAK);
        return world;
    }

    /** A world with a wall of planks across one column of a diagonal. */
    private static World wall(int x, int z) {
        World world = new World(7, 0, 0);
        world.setBlock(x, OBJECT_Y, z, Blocks.PLANKS_OAK);
        return world;
    }

    /** Casts a ray that stops at the first cell holding a block. */
    private static BlockRay.Hit cast(World world, float startX, float startY, float startZ,
            float dirX, float dirY, float dirZ) {
        return BlockRay.cast(world, startX, startY, startZ, dirX, dirY, dirZ, REACH,
                (access, x, y, z) -> !access.getBlock(x, y, z).isAir());
    }
}
