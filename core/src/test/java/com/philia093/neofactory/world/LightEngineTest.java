package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the light of a world that is played in.
 * <p>
 * A flat world is used, because every case here is about a cell a player made: the sky that stands on the
 * surface, the shadow of a roof, the hole that lets the sky in again and the light of a torch that is put
 * down and taken up. Everything is asked the way a machine asks - no window, no mesh - and what is read is
 * the light map a mesh is drawn from, see {@code Section}.
 */
class LightEngineTest {

    /** Seed the worlds of these cases are built with, any fixed value does. */
    private static final int SEED = 20240924;

    /** Height of the bedrock of a flat column, the block the world ends at. */
    private static final int BEDROCK_Y = Constants.MIN_Y;

    /** Height of the grass of a flat column, the block a body walks on. */
    private static final int GRASS_Y = Constants.MIN_Y + 2;

    /** Height of the cell a body stands in, right above the grass. */
    private static final int AIR_Y = Constants.MIN_Y + 3;

    /** Height of a roof laid over the surface of a flat column. */
    private static final int ROOF_Y = AIR_Y + 6;

    /** Cell of the flat land every case works at. */
    private static final int CELL_X = 2;

    /** Second coordinate of the cell every case works at. */
    private static final int CELL_Z = 3;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theSkyLightsTheCellsThatSeeIt() {
        World world = flatWorld();

        assertEquals(Block.MAX_LIGHT, world.peekSkyLight(CELL_X, AIR_Y, CELL_Z),
                "the cell a body stands in sees the whole sky");
        assertEquals(Block.MAX_LIGHT, world.peekSkyLight(CELL_X, ROOF_Y, CELL_Z),
                "and so does the air above it, wherever it is");
        assertEquals(0, world.peekSkyLight(CELL_X, BEDROCK_Y, CELL_Z),
                "no sky reaches the bedrock under the grass");
        assertTrue(world.chunkIfLoaded(0, 0).isLighted(), "the chunk was lit once it was complete");
    }

    @Test
    void aRoofCastsAShadowAndItsRemovalLetsTheSkyInAgain() {
        World world = flatWorld();
        int x = CELL_X + 6;
        int z = CELL_Z + 6;

        world.setBlock(x, ROOF_Y, z, Blocks.STONE);

        assertTrue(world.peekSkyLight(x, ROOF_Y - 1, z) < Block.MAX_LIGHT,
                "the cell under a roof is in the shadow of it");

        world.setBlock(x, ROOF_Y, z, Blocks.AIR);

        assertEquals(Block.MAX_LIGHT, world.peekSkyLight(x, ROOF_Y - 1, z),
                "taking the roof away lets the sky in again");
    }

    @Test
    void aTorchLightsItsNeighbourhoodAndGoesOutAgain() {
        World world = flatWorld();
        int x = CELL_X + 12;
        int z = CELL_Z + 12;
        int floor = GRASS_Y + 1;

        world.setBlock(x, floor, z, Blocks.TORCH);

        assertEquals(Blocks.TORCH_LIGHT, world.peekBlockLight(x, floor, z), "the torch itself");
        assertEquals(Blocks.TORCH_LIGHT - 1, world.peekBlockLight(x + 1, floor, z),
                "one step away it is one level darker");
        assertEquals(2, world.peekBlockLight(x + Blocks.TORCH_LIGHT - 2, floor, z),
                "twelve steps away two levels of it are left");
        assertEquals(0, world.peekBlockLight(x + Blocks.TORCH_LIGHT, floor, z),
                "further than the torch reaches there is nothing of it left");

        world.setBlock(x, floor, z, Blocks.AIR);

        assertEquals(0, world.peekBlockLight(x, floor, z), "the light went out with the torch");
        assertEquals(0, world.peekBlockLight(x + 1, floor, z), "and it took its light away with it");
    }

    @Test
    void aSecondTorchKeepsTheCellLitWhenTheFirstOneIsTakenDown() {
        World world = flatWorld();
        int x = CELL_X - 12;
        int z = CELL_Z - 12;
        int floor = GRASS_Y + 1;

        world.setBlock(x, floor, z, Blocks.TORCH);
        world.setBlock(x + 4, floor, z, Blocks.TORCH);
        assertTrue(world.peekBlockLight(x + 2, floor, z) > 0, "the cell between the two torches is lit");

        world.setBlock(x, floor, z, Blocks.AIR);

        assertEquals(Blocks.TORCH_LIGHT - 2, world.peekBlockLight(x + 2, floor, z),
                "the light of the second torch stays, and it is the only one that is left");
    }

    @Test
    void aTorchLightsTheChunkBesideTheOneItStandsIn() {
        World world = flatWorld();
        int floor = GRASS_Y + 1;
        int x = Constants.CHUNK_SIZE - 1;
        int z = CELL_Z;

        world.setBlock(x, floor, z, Blocks.TORCH);

        assertEquals(Blocks.TORCH_LIGHT - 1, world.peekBlockLight(x + 1, floor, z),
                "the torch lights the chunk next to the one it stands in");
    }

    @Test
    void aSectionWhoseLightChangedIsMeshedAgain() {
        World world = flatWorld();
        int x = CELL_X + 9;
        int z = CELL_Z + 9;
        Chunk chunk = world.chunkIfLoaded(Chunk.chunkOf(x), Chunk.chunkOf(z));
        Section section = chunk.section(ROOF_Y / Section.SIZE);
        section.clearDirty();

        world.setBlock(x, ROOF_Y, z, Blocks.STONE);

        assertTrue(section.isDirty(), "a shadow is a reason to mesh the section again");
    }

    /** A flat world, the land every case of this test works in. */
    private static World flatWorld() {
        return new World(SEED, 0, 0, null, WorldType.FLAT);
    }
}
