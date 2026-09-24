package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.fluid.FluidFlow;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.decoration.TerrainSampler;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the terrain the generator draws.
 * <p>
 * Two kinds of question are asked. The first is about the fields: a river has to wind and to stay a
 * band, a lake has to be a ragged lobe and never a circle, and neither may take over the world. The
 * second is about what the decorators make of them and what the fluids do with them: the water of a
 * river and of a lake is a source, the ring around it is solid, and a lake laid out by the generator
 * behaves like terrain instead of like a spill that runs.
 * <p>
 * The fields are sampled into a plain grid of booleans, so a shape can be read without a set, a
 * coordinate object or a hash in the way.
 */
class WorldGenTest {

    /** Seed the samples are taken from. */
    private static final int SEED = 20260921;

    /** Side of the patch the field tests sample, in blocks. */
    private static final int SAMPLE = 320;

    /** Radius the fluid tests look at around the spawn, in blocks. */
    private static final int NEARBY = 64;

    /** Ticks the fluid tests may spend on letting the world settle. */
    private static final int SETTLE_LIMIT = 200;

    /** Biomes that carry water of their own, see {@link Biome}. */
    private static final Set<Biome> WATER_BIOMES = Set.of(Biome.RIVER, Biome.LAKE);

    private static WorldGen generator;
    private static boolean[][] river;
    private static boolean[][] lake;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
        generator = new WorldGen(SEED);
        river = sample(true);
        lake = sample(false);
    }

    /**
     * Samples a body of water of a patch.
     *
     * @param wantRiver {@code true} for the river field, {@code false} for the lake field
     * @return a grid of the patch, indexed by {@code [y][x]}
     */
    private static boolean[][] sample(boolean wantRiver) {
        boolean[][] grid = new boolean[SAMPLE][SAMPLE];
        for (int y = 0; y < SAMPLE; y++) {
            for (int x = 0; x < SAMPLE; x++) {
                grid[y][x] = wantRiver ? generator.isRiverAt(x, y) : generator.isLakeAt(x, y);
            }
        }
        return grid;
    }

    /** Amount of cells of a grid that carry water. */
    private static int area(boolean[][] grid) {
        int found = 0;
        for (boolean[] row : grid) {
            for (boolean cell : row) {
                if (cell) {
                    found += 1;
                }
            }
        }
        return found;
    }

    /** {@code true} when a cell of a grid carries water, the patch ending counts as no water. */
    private static boolean waterAt(boolean[][] grid, int x, int y) {
        return x >= 0 && x < SAMPLE && y >= 0 && y < SAMPLE && grid[y][x];
    }

    /** {@code true} when all four neighbours of a cell carry water as well. */
    private static boolean inside(boolean[][] grid, int x, int y) {
        return waterAt(grid, x + 1, y) && waterAt(grid, x - 1, y)
                && waterAt(grid, x, y + 1) && waterAt(grid, x, y - 1);
    }

    /** {@code true} when one of the four neighbours of a cell carries water as well. */
    private static boolean nearWater(boolean[][] grid, int x, int y) {
        return waterAt(grid, x + 1, y) || waterAt(grid, x - 1, y)
                || waterAt(grid, x, y + 1) || waterAt(grid, x, y - 1);
    }

    @Test
    void aRiverWindsAndStaysABand() {
        int cells = area(river);
        assertTrue(cells > 0, "the seed has to produce a river");
        assertShare(cells, "river");

        // A band touches itself almost everywhere: a cell whose four neighbours carry no river as
        // well is an end of one, and a patch this size holds only a handful of those.
        int ends = 0;
        for (int y = 0; y < SAMPLE; y++) {
            for (int x = 0; x < SAMPLE; x++) {
                if (river[y][x] && !nearWater(river, x, y)) {
                    ends++;
                }
            }
        }
        assertTrue(ends * 20 < cells,
                "a river is a band, not a spray of cells: " + ends + " of " + cells
                        + " cells stand alone");

        // A slice of the biome field would be a region hundreds of cells across. A contour line is
        // a band, so it is nowhere near a quarter of a patch this wide.
        int widest = widestRun(river);
        assertTrue(widest >= 1 && widest < SAMPLE / 4,
                "a river is a band, so it is neither broken nor that wide: " + widest + " cells");
    }

    @Test
    void aLakeIsARaggedLobeAndNeverACircle() {
        int cells = area(lake);
        assertTrue(cells > 0, "the seed has to produce a lake");
        assertShare(cells, "lake");

        // A bay costs border without adding area, so the isoperimetric quotient of a shape - its
        // border squared over its area - tells a lobe of a noise field from a drawn circle: a
        // circle of any size stays near four pi, which is about 12.6.
        int border = 0;
        for (int y = 0; y < SAMPLE; y++) {
            for (int x = 0; x < SAMPLE; x++) {
                if (lake[y][x] && !inside(lake, x, y)) {
                    border++;
                }
            }
        }
        float quotient = (float) border * border / cells;
        assertTrue(quotient > 20.0f,
                "a lake has bays and peninsulas, so its border is far longer than that of a circle:"
                        + " quotient " + quotient + " of " + cells + " cells and " + border
                        + " of border");
    }

    @Test
    void aBedOfABodyOfWaterIsSandClayOrGravel() {
        for (int y = 0; y < SAMPLE; y++) {
            for (int x = 0; x < SAMPLE; x++) {
                if (river[y][x] || lake[y][x]) {
                    assertBed(x, y);
                }
            }
        }
    }

    @Test
    void theBiomeFieldNeverProducesARiverOrALake() {
        for (int step = 0; step <= 1000; step++) {
            Biome biome = Biome.fromNormalized(step / 1000.0f);
            assertFalse(WATER_BIOMES.contains(biome),
                    "water comes from its own field and never from the biome field: " + biome);
        }
    }

    @Test
    void theBiomeOfACellIsTheWaterThatCoversIt() {
        // A decorator sees the terrain only through the sampler, so the two answers have to agree
        // wherever either of them is asked.
        TerrainSampler terrain = generator;
        for (int y = -200; y <= 200; y += 7) {
            for (int x = -200; x <= 200; x += 7) {
                boolean water = generator.isRiverAt(x, y) || generator.isLakeAt(x, y);
                assertEquals(water, WATER_BIOMES.contains(terrain.biomeAt(x, y)),
                        "the biome of a cell is the water that covers it, at (" + x + ", " + y + ")");
            }
        }
    }

    @Test
    void theSameSeedAlwaysDrawsTheSameWater() {
        WorldGen other = new WorldGen(SEED);
        for (int index = 0; index < 200; index++) {
            int x = index * 37 - 3000;
            int y = index * 53 - 4000;
            assertEquals(generator.isRiverAt(x, y), other.isRiverAt(x, y));
            assertEquals(generator.isLakeAt(x, y), other.isLakeAt(x, y));
            assertEquals(generator.floorAt(x, y), other.floorAt(x, y));
        }
    }

    /** The widest horizontal run of water in a grid, in cells. */
    private static int widestRun(boolean[][] grid) {
        int widest = 0;
        for (int y = 0; y < SAMPLE; y++) {
            int run = 0;
            for (int x = 0; x < SAMPLE; x++) {
                run = grid[y][x] ? run + 1 : 0;
                if (run > widest) {
                    widest = run;
                }
            }
        }
        return widest;
    }

    /** Asserts that the ground under a cell of a body of water is one of the bed materials. */
    private static void assertBed(int x, int y) {
        Block bed = generator.floorAt(x, y);
        assertTrue(isBedBlock(bed), "the bed under a body of water is sand, clay or gravel: " + bed
                + " at (" + x + ", " + y + ")");
    }

    /**
     * {@code true} when a block is one of the three materials a bed of a body of water is made of.
     * <p>
     * The blocks are compared one by one and not through a set built in a field: the tables of the
     * game are filled after the classes are loaded, so a field would hold nothing but nulls.
     *
     * @param block block to look at
     * @return {@code true} for sand, clay and gravel
     */
    private static boolean isBedBlock(Block block) {
        return block == Blocks.SAND || block == Blocks.CLAY || block == Blocks.GRAVEL;
    }

    /** Asserts that a body of water takes a plausible share of the patch. */
    private static void assertShare(int cells, String what) {
        float share = (float) cells / (SAMPLE * SAMPLE);
        assertTrue(share > 0.002f, "the patch has to hold some " + what + ": " + share);
        assertTrue(share < 0.35f, what + " may not take over the world: " + share);
    }

    @Test
    void nothingGrowsOnAPoolOfLava() {
        // A pool lies somewhere in the patch, so the test looks for one instead of hoping that the
        // spawn happens to have it.
        int poolX = 0;
        int poolZ = 0;
        boolean found = false;
        for (int z = 0; z < SAMPLE && !found; z++) {
            for (int x = 0; x < SAMPLE && !found; x++) {
                if (generator.isLavaPoolAt(x, z) && !WATER_BIOMES.contains(generator.biomeAt(x, z))
                        && generator.groundY(x, z) > Constants.SEA_LEVEL) {
                    poolX = x;
                    poolZ = z;
                    found = true;
                }
            }
        }
        Assumptions.assumeTrue(found, "the seed has to drop a pool somewhere in the patch");

        World world = new World(SEED);
        int centerChunkX = Chunk.chunkOf(poolX);
        int centerChunkZ = Chunk.chunkOf(poolZ);
        for (int chunkX = centerChunkX - 1; chunkX <= centerChunkX + 1; chunkX++) {
            for (int chunkZ = centerChunkZ - 1; chunkZ <= centerChunkZ + 1; chunkZ++) {
                world.loadChunk(chunkX, chunkZ);
            }
        }

        int ground = generator.groundY(poolX, poolZ);
        assertEquals(Blocks.LAVA, world.getBlock(poolX, ground + 1, poolZ),
                "the terrain carried lava into the cell of a pool");
        Block floor = world.getBlock(poolX, ground, poolZ);
        assertTrue(floor == Blocks.STONE || floor == Blocks.GRAVEL,
                "and the ground under it is scorched: " + floor);

        // Nothing that plants on the ground may stand on a pool. A tree did exactly that while the
        // field of the pools lived in the decoration alone: the decoration had burnt the ground, but
        // the generator did not know, and a tree reads the ground of the generator.
        for (int dz = -4; dz <= 4; dz++) {
            for (int dx = -4; dx <= 4; dx++) {
                int cellX = poolX + dx;
                int cellZ = poolZ + dz;
                if (!generator.isLavaPoolAt(cellX, cellZ)
                        || WATER_BIOMES.contains(generator.biomeAt(cellX, cellZ))) {
                    // A cell that lies under a river and under the field of the pools at the same time
                    // belongs to the water, so water there is right.
                    continue;
                }
                int cellGround = generator.groundY(cellX, cellZ);
                if (cellGround <= Constants.SEA_LEVEL) {
                    // The field of the pools reaches cells below the water line, but a pool is only
                    // carried where the land stands above the sea: the water of a sea or a lake is right.
                    continue;
                }
                assertEquals(Blocks.LAVA, world.getBlock(cellX, cellGround + 1, cellZ),
                        "a pool of lava carries lava at (" + cellX + ", " + cellZ + ")");
                Block above = world.getBlock(cellX, cellGround + 2, cellZ);
                assertTrue(above.isAir() || above == Blocks.LEAVES_OAK,
                        "nothing but the crown of a tree beside it may reach over a pool: " + above
                                + " at (" + cellX + ", " + cellZ + ")");
            }
        }
    }

    @Test
    void theRingAroundABodyOfWaterIsSolid() {
        World world = new World(SEED);
        Set<BlockPos> water = waterAround(world);
        Assumptions.assumeFalse(water.isEmpty(), "the seed has no water near its spawn");

        for (BlockPos cell : water) {
            for (BlockPos neighbour : neighbours(cell)) {
                if (waterOf(world, neighbour) == Fluids.WATER.block()) {
                    // A cell of the water itself: a body reaches its own cells, of course.
                    continue;
                }
                assertFalse(
                        FluidFlow.mayStand(world, Fluids.WATER, neighbour.x(), waterY(neighbour.x(), neighbour.y()), neighbour.y()),
                        "the ring around a body of water holds it in, so (" + neighbour.x() + ", "
                                + neighbour.y() + ") beside (" + cell.x() + ", " + cell.y()
                                + ") may not take water");
            }
        }
    }

    @Test
    void theFluidSystemLeavesALakeAlone() {
        World world = new World(SEED);
        Set<BlockPos> before = waterAround(world);
        Assumptions.assumeFalse(before.isEmpty(), "the seed has no water near its spawn");

        for (int tick = 0; tick < SETTLE_LIMIT; tick++) {
            world.tick(TickClock.TICK_SECONDS);
        }
        // Asked before the world is read again: reading asks every chunk to tell the fluids where
        // they stand, see World#loadChunk, which marks them all over again.
        assertEquals(0, world.fluids().pendingCellCount(),
                "nothing about a lake waits for another look once it settled");

        Set<BlockPos> after = waterAround(world);
        for (BlockPos cell : after) {
            assertTrue(before.contains(cell),
                    "no water may appear outside the body: (" + cell.x() + ", " + cell.y() + ")");
        }
        assertEquals(before, after,
                "the water of the generator is terrain: none of it runs and none of it dries up");
    }

    /** The block a cell carries in the layer of the player. */
    private static int waterY(int x, int y) {
        return generator.groundY(x, y) + 1;
    }

    private static Block waterOf(World world, BlockPos cell) {
        return world.getBlock(cell.x(), waterY(cell.x(), cell.y()), cell.y());
    }

    /** The four cells around one. */
    private static Set<BlockPos> neighbours(BlockPos cell) {
        return Set.of(cell.up(), cell.down(), cell.east(), cell.west());
    }

    /** The water of the object layer around the spawn of a world. */
    private static Set<BlockPos> waterAround(World world) {
        int centerX = world.spawnX();
        int centerY = world.spawnZ();
        int radius = NEARBY / Constants.CHUNK_SIZE + 1;
        int centerChunkX = Chunk.chunkOf(centerX);
        int centerChunkY = Chunk.chunkOf(centerY);
        for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; chunkX++) {
            for (int chunkY = centerChunkY - radius; chunkY <= centerChunkY + radius; chunkY++) {
                world.loadChunk(chunkX, chunkY);
            }
        }

        Set<BlockPos> found = new HashSet<>();
        for (int y = centerY - NEARBY; y <= centerY + NEARBY; y++) {
            for (int x = centerX - NEARBY; x <= centerX + NEARBY; x++) {
                if (world.getBlock(x, waterY(x, y), y) == Fluids.WATER.block()) {
                    found.add(BlockPos.of(x, y));
                }
            }
        }
        return found;
    }
}
