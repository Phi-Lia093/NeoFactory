package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.render.MeshData;
import com.philia093.neofactory.render.SectionMesher;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void theValleyOfARiverOrALakeStaysDry() {
        // The game has no fluid block: a river and a lake carve their valley into the land and the
        // ground of it is sand, clay or gravel, but nothing is poured over it. This is what keeps a
        // world dry everywhere and why water has to be carried as an item or in a tank now.
        WorldGen generator = new WorldGen(SEED);
        World world = new World(SEED);
        int columns = 0;
        int valleys = 0;

        for (int x = -SAMPLE * 4; x < SAMPLE * 4; x += 5) {
            for (int z = -SAMPLE * 4; z < SAMPLE * 4; z += 5) {
                if (!generator.isRiverValleyAt(x, z) && !generator.isLakeValleyAt(x, z)) {
                    continue;
                }
                columns++;
                int ground = generator.groundY(x, z);
                assertTrue(ground < Constants.SEA_LEVEL,
                        "the valley of a river or a lake lies below the sea at (" + x + ", " + z + ")");
                for (int y = ground + 1; y <= Constants.SEA_LEVEL; y++) {
                    assertEquals(Blocks.AIR, world.getBlock(x, y, z),
                            "nothing is poured into the valley at (" + x + ", " + y + ", " + z + ")");
                }
                valleys++;
            }
        }

        assertTrue(valleys > 0, "the sampled area has to hold a valley at all");
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
        assertTrue(ground.isSolid(),
                "a new world has to start a player on solid ground: " + ground);
        assertEquals(world.surfaceY(x, z), player.blockY(), "with the feet on top of that ground");
    }

    @Test
    void theLowestSectionOfAWorldCanBeMeshed() {
        World world = new World(SEED);
        Chunk chunk = world.loadChunk(0, 0);
        Section section = chunk.section(0);
        int originY = Constants.MIN_Y;
        SectionMesher.Blocks blocks = (x, y, z) -> world.peekBlock(chunk.originX() + x, originY + y,
                chunk.originZ() + z);

        List<MeshData> meshes = SectionMesher.build(section, chunk.originX(), originY, chunk.originZ(),
                blocks, picture -> 0);

        // The bottom section holds the bedrock and the stone of the columns, so it has faces to draw - and
        // building it reads the cell under its lowest blocks, which lies outside the world. That read used
        // to stop the game the moment a world was drawn from the bottom up, see World#outsideTheWorld.
        assertFalse(meshes.isEmpty(), "the lowest section of the world has faces to draw");
    }
}
