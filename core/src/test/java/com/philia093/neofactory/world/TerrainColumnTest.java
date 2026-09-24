package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a world is a column of blocks from the bottom of the world up to its surface.
 * <p>
 * A world of cubes is made of columns, and every one of them is filled the same way: bedrock at the very
 * bottom, stone above it, a layer of soil, the block of the biome on top and the water of the sea - or the
 * lava of a pool - up to the level that liquid stands at. A column that misses a block is a hole a player
 * can fall through, and a surface that does not agree with what the terrain sampler predicts is what makes
 * a decoration hang in the air or a spawn sit inside a hill, so both are checked here.
 */
class TerrainColumnTest {

    /** Seed the cases are built with. */
    private static final int SEED = 20_260_924;

    /** Columns per axis the cases walk over. */
    private static final int SAMPLE = 24;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void everyColumnIsFilledFromTheBottomOfTheWorldToItsSurface() {
        WorldGen generator = new WorldGen(SEED);
        World world = new World(SEED);

        for (int x = 0; x < SAMPLE; x++) {
            for (int z = 0; z < SAMPLE; z++) {
                int ground = generator.groundY(x, z);
                assertEquals(Blocks.BEDROCK, world.getBlock(x, Constants.MIN_Y, z),
                        "the block at the bottom of the world at (" + x + ", " + z + ")");
                for (int y = Constants.MIN_Y + 1; y <= ground; y++) {
                    assertFalse(world.getBlock(x, y, z).isAir(),
                            "no hole in the ground at (" + x + ", " + y + ", " + z + ")");
                }
                assertEquals(generator.floorAt(x, z), world.getBlock(x, ground, z),
                        "the surface block of the column at (" + x + ", " + z + ")");
                assertTrue(world.surfaceY(x, z) >= generator.surfaceY(x, z),
                        "a decoration stands on top of a column, it never takes its ground away at ("
                                + x + ", " + z + ")");
            }
        }
    }

    @Test
    void theWaterOfTheWorldStandsUpToTheLevelOfTheSea() {
        WorldGen generator = new WorldGen(SEED);
        World world = new World(SEED);
        int columns = 0;
        int wet = 0;

        for (int x = -SAMPLE * 8; x < SAMPLE * 8; x += 7) {
            for (int z = -SAMPLE * 8; z < SAMPLE * 8; z += 7) {
                columns++;
                int ground = generator.groundY(x, z);
                if (ground >= Constants.SEA_LEVEL) {
                    Block aboveGround = world.getBlock(x, ground + 1, z);
                    assertFalse(aboveGround == Fluids.WATER.block(),
                            "a column that stands above the sea carries no water, only what grows on it: "
                                    + aboveGround + " at (" + x + ", " + z + ")");
                    continue;
                }
                // A column below the sea is filled with water up to the level the sea stands at, except
                // where the land is burnt: a pool lies one block above its scorched ground and never in
                // the water, see the rule in WorldGen#fillColumn.
                Block liquid = world.getBlock(x, ground + 1, z);
                if (liquid == Blocks.LAVA) {
                    continue;
                }
                for (int y = ground + 1; y <= Constants.SEA_LEVEL; y++) {
                    assertEquals(Fluids.WATER.block(), world.getBlock(x, y, z),
                            "water up to the level of the sea at (" + x + ", " + y + ", " + z + ")");
                }
                wet++;
            }
        }

        float share = wet / (float) columns;
        assertTrue(share > 0.05f, "the world has to hold water of the sea: " + share);
        assertTrue(share < 0.45f, "most of a world has to be land, not water: " + share);
    }

    @Test
    void theLandRisesAndFallsAroundTheSea() {
        WorldGen generator = new WorldGen(SEED);
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;

        for (int x = -SAMPLE * 8; x < SAMPLE * 8; x += 3) {
            for (int z = -SAMPLE * 8; z < SAMPLE * 8; z += 3) {
                int ground = generator.groundY(x, z);
                lowest = Math.min(lowest, ground);
                highest = Math.max(highest, ground);
            }
        }

        assertTrue(highest - lowest >= 12,
                "the land has to have shape, not be a table: " + lowest + " to " + highest);
        assertTrue(lowest <= Constants.SEA_LEVEL && highest >= Constants.SEA_LEVEL,
                "the sea has to meet the land: " + lowest + " to " + highest);
    }

    @Test
    void theSpawnOfANewWorldStandsOnSolidGround() {
        World world = new World(SEED);
        Player player = Player.spawnOnGround(world, world.spawnX(), world.spawnZ());

        int x = player.blockX();
        int z = player.blockZ();
        Block ground = world.getBlock(x, player.blockY() - 1, z);
        assertTrue(ground.isSolid() && !ground.isLiquid(),
                "a new world has to start a player on solid ground: " + ground);
        assertEquals(world.surfaceY(x, z), player.blockY(), "with the feet on top of that ground");
    }
}
