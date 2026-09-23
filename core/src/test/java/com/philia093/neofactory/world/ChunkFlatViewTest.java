package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the flat view of a chunk: the two layers the game still plays in, and the column of
 * sections they really live in.
 * <p>
 * Both views address the very same cells, so a write through one has to be seen by the other - the
 * ground layer is the block at {@link Chunk#LAYER_BASE_Y} and the layer the player stands in is the
 * one above it. A fluid keeps its level in the state of its cell, which is why this class watches a
 * state as closely as a block.
 */
class ChunkFlatViewTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theFlatLayersSitAtTheBaseHeightOfTheWorld() {
        assertEquals(64, Chunk.LAYER_BASE_Y);
        assertEquals(Chunk.LAYER_BASE_Y, Chunk.flatY(Chunk.LAYER_FLOOR));
        assertEquals(Chunk.LAYER_BASE_Y + 1, Chunk.flatY(Chunk.LAYER_OBJECT));
        assertThrows(IndexOutOfBoundsException.class, () -> Chunk.flatY(9));
    }

    @Test
    void aBlockWrittenOnALayerIsTheBlockOfTheColumnThere() {
        Chunk chunk = new Chunk(0, 0);

        chunk.setBlock(3, 5, Chunk.LAYER_FLOOR, Blocks.STONE);
        chunk.setBlock(3, 5, Chunk.LAYER_OBJECT, Blocks.TALL_GRASS);

        assertEquals(Blocks.STONE, chunk.getBlock(3, 5, Chunk.LAYER_FLOOR));
        assertEquals(Blocks.TALL_GRASS, chunk.getBlock(3, 5, Chunk.LAYER_OBJECT));

        assertEquals(Blocks.STONE, chunk.getBlockAt(3, Chunk.LAYER_BASE_Y, 5),
                "the ground layer is the ground");
        assertEquals(Blocks.TALL_GRASS, chunk.getBlockAt(3, Chunk.LAYER_BASE_Y + 1, 5),
                "and the other one is above it");
        assertEquals(Blocks.AIR, chunk.getBlockAt(3, Chunk.LAYER_BASE_Y - 1, 5));
        assertEquals(Blocks.AIR, chunk.getBlockAt(3, Chunk.LAYER_BASE_Y + 2, 5));
    }

    @Test
    void aBlockWrittenInTheColumnIsTheBlockOfItsLayer() {
        Chunk chunk = new Chunk(0, 0);

        chunk.setBlockAt(7, Chunk.LAYER_BASE_Y, 9, Blocks.SAND);

        assertEquals(Blocks.SAND, chunk.getBlock(7, 9, Chunk.LAYER_FLOOR));
    }

    @Test
    void aStateWrittenOnALayerIsTheStateOfTheColumnThere() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(1, 2, Chunk.LAYER_OBJECT, Blocks.WATER);

        chunk.setMeta(1, 2, Chunk.LAYER_OBJECT, 42);

        assertEquals(42, chunk.meta(1, 2, Chunk.LAYER_OBJECT), "the flat view read it back");
        assertEquals(42, chunk.stateAt(1, Chunk.LAYER_BASE_Y + 1, 2), "and so did the column");
        assertEquals(0, chunk.stateAt(1, Chunk.LAYER_BASE_Y, 2), "the layer below is untouched");
    }

    @Test
    void aStateWrittenInTheColumnIsTheStateOfItsLayer() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlockAt(4, Chunk.LAYER_BASE_Y, 6, Blocks.WATER);

        chunk.setStateAt(4, Chunk.LAYER_BASE_Y, 6, 7);

        assertEquals(7, chunk.meta(4, 6, Chunk.LAYER_FLOOR));
    }

    @Test
    void aStateIsWrittenAfterItsBlockAndSurvivesTheOtherLayer() {
        Chunk chunk = new Chunk(0, 0);

        // The order a fluid writes in: the block first, which clears the state of the cell, then
        // the state that belongs to it.
        chunk.setBlock(0, 0, Chunk.LAYER_OBJECT, Blocks.WATER);
        chunk.setMeta(0, 0, Chunk.LAYER_OBJECT, 1);
        assertEquals(1, chunk.meta(0, 0, Chunk.LAYER_OBJECT));

        chunk.setBlock(0, 0, Chunk.LAYER_FLOOR, Blocks.STONE);
        assertEquals(1, chunk.meta(0, 0, Chunk.LAYER_OBJECT), "the state of the layer above stuck");

        // Replacing the block does clear it, because the state belonged to the block that was there.
        chunk.setBlock(0, 0, Chunk.LAYER_OBJECT, Blocks.LAVA);
        assertEquals(0, chunk.meta(0, 0, Chunk.LAYER_OBJECT));
    }

    @Test
    void aStateSurvivesTheWritePathOfAWorld() {
        World world = new World(21);

        world.setBlock(4, 4, Chunk.LAYER_OBJECT, Blocks.WATER);
        world.setMeta(4, 4, Chunk.LAYER_OBJECT, 9);

        assertEquals(Blocks.WATER, world.peekBlock(4, 4, Chunk.LAYER_OBJECT),
                "the block of the layer came back");
        assertEquals(9, world.peekMeta(4, 4, Chunk.LAYER_OBJECT), "and so did its state");
        assertEquals(Blocks.WATER, world.getBlock(4, 4, Chunk.LAYER_OBJECT));
        assertEquals(9, world.getMeta(4, 4, Chunk.LAYER_OBJECT));

        // A fluid writes the same cell twice: the block, then the state that belongs to it.
        world.setBlock(5, 5, Chunk.LAYER_OBJECT, Blocks.LAVA);
        world.setMeta(5, 5, Chunk.LAYER_OBJECT, 3);
        assertEquals(3, world.getMeta(5, 5, Chunk.LAYER_OBJECT));
    }

    @Test
    void theHeightOfAColumnFollowsItsHighestBlock() {
        Chunk chunk = new Chunk(2, 3);

        assertEquals(Chunk.NO_BLOCK, chunk.highestBlockY(0, 0), "an empty column has no height");
        assertFalse(chunk.hasBlock(0, 0));

        chunk.setBlock(0, 0, Chunk.LAYER_FLOOR, Blocks.STONE);
        assertEquals(Chunk.LAYER_BASE_Y, chunk.highestBlockY(0, 0));
        assertTrue(chunk.hasBlock(0, 0));

        chunk.setBlock(0, 0, Chunk.LAYER_OBJECT, Blocks.TALL_GRASS);
        assertEquals(Chunk.LAYER_BASE_Y + 1, chunk.highestBlockY(0, 0));

        // A block far above the surface moves the height as well.
        chunk.setBlockAt(0, 200, 0, Blocks.STONE);
        assertEquals(200, chunk.highestBlockY(0, 0));

        // Clearing the highest block makes the chunk look for the next one below it.
        chunk.setBlockAt(0, 200, 0, Blocks.AIR);
        assertEquals(Chunk.LAYER_BASE_Y + 1, chunk.highestBlockY(0, 0));

        chunk.setBlock(0, 0, Chunk.LAYER_OBJECT, Blocks.AIR);
        chunk.setBlock(0, 0, Chunk.LAYER_FLOOR, Blocks.AIR);
        assertEquals(Chunk.NO_BLOCK, chunk.highestBlockY(0, 0), "the column is empty again");
        assertFalse(chunk.hasBlock(0, 0));
    }

    @Test
    void aSectionPaysForItsBlocksOnly() {
        Chunk chunk = new Chunk(0, 0);
        int groundSection = Constants.SEA_LEVEL / Section.SIZE;

        assertEquals(0, chunk.sectionCount(), "an empty chunk holds no section");
        assertTrue(chunk.isEmptySection(groundSection));

        chunk.setBlock(1, 1, Chunk.LAYER_FLOOR, Blocks.STONE);
        assertEquals(1, chunk.sectionCount());
        assertEquals(Chunk.LAYER_BASE_Y / Section.SIZE, groundSection,
                "the flat world sits in one section");
        assertFalse(chunk.isEmptySection(groundSection));

        // A block at the very top of the world costs one more section, and nothing in between.
        chunk.setBlockAt(1, Constants.MAX_Y, 1, Blocks.STONE);
        assertEquals(2, chunk.sectionCount());
    }

    @Test
    void theSectionsOfAChunkAreFoundByTheirIndex() {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlockAt(0, 40, 0, Blocks.STONE);

        assertEquals(2, 40 / Section.SIZE);
        assertEquals(32, chunk.section(2).originY(), "section two covers 32 to 47");
        assertEquals(Blocks.STONE.id(), chunk.section(2).rawId(0, 8, 0));
        assertNull(chunk.section(3), "a section of nothing but air is not there");
    }

    @Test
    void aHeightOutsideTheWorldIsRefused() {
        Chunk chunk = new Chunk(0, 0);

        assertThrows(IndexOutOfBoundsException.class,
                () -> chunk.setBlockAt(0, Constants.MAX_Y + 1, 0, Blocks.STONE));
        assertThrows(IndexOutOfBoundsException.class,
                () -> chunk.getBlockAt(0, Constants.MIN_Y - 1, 0));
    }
}
