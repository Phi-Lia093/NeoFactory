package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.save.LevelData;
import com.philia093.neofactory.world.save.SaveFormat;
import com.philia093.neofactory.world.save.SaveTags;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the table of blocks a flat world is.
 * <p>
 * A flat world is the land the game is developed and tried out in, so what is checked here is exactly
 * what makes it useful: every column is the same three blocks - bedrock, soil, grass - and nothing
 * else, no hill and no hole is in the way, no tree or pool stands on it, a body starts on its surface
 * and the whole land of a chunk fits into the lowest section it can. The last cases make sure the
 * choice travels: a world carries its land while it is open and a stored world keeps it, while a world
 * from before flat worlds existed stays the landscape it was made of.
 */
class FlatWorldTest {

    /** Seed the cases are built with, any fixed value does. */
    private static final int SEED = 20240924;

    /** Height of the bedrock of a flat column, the block the world ends at. */
    private static final int BEDROCK_Y = Constants.MIN_Y;

    /** Height of the soil of a flat column, one block above the bedrock. */
    private static final int SOIL_Y = Constants.MIN_Y + 1;

    /** Height of the grass of a flat column, the block a body walks on. */
    private static final int GRASS_Y = Constants.MIN_Y + 2;

    /** Height of the cell a body stands in, right above the grass. */
    private static final int AIR_Y = Constants.MIN_Y + 3;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void everyColumnIsBedrockDirtGrassAndThenAir() {
        World world = flatWorld();
        int[][] cells = {{0, 0}, {5, -7}, {-40, 33}, {1234, -1234}};
        for (int[] cell : cells) {
            int x = cell[0];
            int z = cell[1];
            String at = " at (" + x + ", " + z + ")";
            assertEquals(Blocks.BEDROCK, world.getBlock(x, BEDROCK_Y, z), "the world ends at bedrock" + at);
            assertEquals(Blocks.DIRT, world.getBlock(x, SOIL_Y, z), "soil lies over the bedrock" + at);
            assertEquals(Blocks.GRASS, world.getBlock(x, GRASS_Y, z), "grass lies on top" + at);
            assertEquals(Blocks.AIR, world.getBlock(x, AIR_Y, z), "nothing stands on the grass" + at);
            assertEquals(Blocks.AIR, world.getBlock(x, Constants.SEA_LEVEL, z),
                    "and no sea is over it either" + at);
        }
    }

    @Test
    void theLandHasTheSameHeightEverywhere() {
        WorldGen generator = new WorldGen(SEED, WorldType.FLAT);
        for (int x = -64; x <= 64; x += 17) {
            for (int z = -64; z <= 64; z += 19) {
                String at = " at (" + x + ", " + z + ")";
                assertEquals(GRASS_Y, generator.groundY(x, z), "the ground is level" + at);
                assertEquals(AIR_Y, generator.surfaceY(x, z), "a body stands one block higher" + at);
                assertEquals(Biome.PLAINS, generator.biomeAt(x, z), "one biome covers it all" + at);
                assertEquals(Blocks.GRASS, generator.floorAt(x, z), "the floor is grass" + at);
                assertFalse(generator.isRiverValleyAt(x, z), "no river cuts through it" + at);
                assertFalse(generator.isLakeValleyAt(x, z), "no lake is carved into it" + at);
            }
        }
    }

    @Test
    void nothingGrowsOnAFlatWorld() {
        assertTrue(new WorldGen(SEED, WorldType.FLAT).decorations().isEmpty(),
                "a flat world carries no decoration");

        // The land itself has to agree: nothing may stand over the grass in a generated area.
        World world = flatWorld();
        for (int x = 0; x < 32; x += 3) {
            for (int z = 0; z < 32; z += 3) {
                for (int y = AIR_Y; y < AIR_Y + 16; y++) {
                    assertEquals(Blocks.AIR, world.getBlock(x, y, z),
                            "nothing stands at (" + x + ", " + y + ", " + z + ")");
                }
            }
        }
    }

    @Test
    void aBodyStartsOnTheGrass() {
        World world = flatWorld();
        Player player = Player.spawnOnGround(world, 0, 0);

        assertEquals(AIR_Y, player.blockY(), "the body stands right above the grass");
        assertEquals(Blocks.GRASS, world.getBlock(player.blockX(), GRASS_Y, player.blockZ()),
                "and it stands on grass");
    }

    @Test
    void theWholeLandOfAChunkFitsIntoOneSection() {
        WorldGen generator = new WorldGen(SEED, WorldType.FLAT);
        Chunk chunk = new Chunk(0, 0);
        generator.generateFloor(chunk);

        assertEquals(1, chunk.sectionCount(), "the whole land fits into the lowest section");
        assertFalse(chunk.isEmptySection(0), "that section really holds the land");
        assertTrue(chunk.isEmptySection(1), "and everything over it is air");
        assertEquals(GRASS_Y, chunk.highestBlockY(0, 0), "the highest block is the grass");
        assertEquals(GRASS_Y, chunk.highestBlockY(7, 9));
    }

    @Test
    void aWorldCarriesItsLandAndAStoredWorldKeepsIt() {
        assertEquals(WorldType.FLAT, flatWorld().worldType(), "a flat world knows what it is");
        assertEquals(WorldType.NORMAL, new World(SEED, 0, 0).worldType(),
                "a world that was not told otherwise is the landscape of its seed");

        LevelData data = new LevelData();
        data.setWorldType(WorldType.FLAT);
        NbtCompound root = data.write(new PlayerInventory());

        assertEquals(WorldType.FLAT, LevelData.read(root).worldType(), "the land travels in the file");
    }

    @Test
    void aStoredWorldWithoutATypeIsTheGeneratedLandscape() {
        NbtCompound root = new NbtCompound(SaveFormat.ROOT_TAG);
        root.putInt(SaveTags.DATA_VERSION, SaveFormat.DATA_VERSION);

        assertEquals(WorldType.NORMAL, LevelData.read(root).worldType(),
                "a world stored before flat worlds existed keeps its landscape");
        assertEquals(WorldType.FLAT, WorldType.byName("flat"));
        assertEquals(WorldType.NORMAL, WorldType.byName(" normal "));
        assertNull(WorldType.byName("nowhere"));
        assertNull(WorldType.byName(null));
    }

    /** A flat world around the origin, the land a case works in. */
    private static World flatWorld() {
        return new World(SEED, 0, 0, null, WorldType.FLAT);
    }
}
