package com.philia093.neofactory.pipe;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.material.Materials;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the table of the pipes: the materials, the sizes each of them is made in, their ids and their masks.
 * <p>
 * The numbers of the table - the heat a material takes and what it moves - are the table of the industry,
 * so what is checked here is their order and their shape: a single tube carries more the larger it is, a
 * bundle moves no more than the single pipe of its cell, a better material takes more heat than a worse
 * one, and a handful of cells is pinned down as they are written down, see
 * {@link #theNumbersOfTheTableAreTheNumbersOfTheIndustry()}.
 */
class PipeTableTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theTableHoldsOnePipeOfEveryMaterialAndSize() {
        assertEquals(25, PipeMaterials.all().size(), "the materials of the line of the industry");
        assertEquals(7, PipeSize.values().length, "tiny to nonuple");
        // Wood is made of three tubes and of no bundle, so the game holds twenty four pipes and not the
        // twenty eight a material of every size would come to, see PipeMaterial#hasSize.
        assertEquals(List.of(PipeSize.SMALL, PipeSize.MEDIUM, PipeSize.LARGE),
                PipeMaterials.WOOD.sizes(), "the three sizes of wood");
        assertEquals(145, Pipes.all().size());
        assertEquals(Pipes.all().size(), Pipes.COUNT);
        for (PipeMaterial material : PipeMaterials.all()) {
            for (PipeSize size : PipeSize.values()) {
                Pipes.Pipe pipe = Pipes.of(material, size);
                if (!material.hasSize(size)) {
                    assertNull(pipe, material + " is not made in " + size);
                    continue;
                }
                assertNotNull(pipe, material + " " + size);
                assertEquals(material, pipe.material());
                assertEquals(size, pipe.size());
                assertEquals(material.flow(size), pipe.flow(), "the rate of the table");
                assertEquals(material.maxTemperature(), pipe.maxTemperature());
                assertEquals(material.name() + "_pipe_" + size.fileName(), pipe.name());
                assertEquals(material.displayName() + " " + size.displayName() + " Pipe",
                        pipe.displayName());
                assertEquals(pipe, Pipes.of(pipe.block()), "the block leads back to its pipe");
            }
        }
        assertNull(Pipes.of(Blocks.STONE), "a stone is no pipe");
        assertNull(Pipes.of((Block) null));
    }

    @Test
    void onlyThePipesOfTheTableArePipes() {
        for (Block block : new Block[] {Blocks.STONE, Blocks.DIRT, Blocks.FURNACE, Blocks.AIR}) {
            assertFalse(Pipes.connects(block), block.name() + " joins a pipe");
        }
        // A machine names the tanks behind its sides, so a line may be built towards it as well, see
        // Block#carriesFluid.
        assertTrue(Pipes.connects(Blocks.BRONZE_BOILER), "a machine joins a pipe");
        for (Pipes.Pipe pipe : Pipes.all()) {
            assertTrue(Pipes.connects(pipe.block()), pipe + " is no pipe");
        }
        assertFalse(Pipes.connects(null));
    }

    @Test
    void theIdsOfThePipesAreARunOfTheirOwn() {
        Set<Integer> blockIds = new HashSet<>();
        Set<Integer> itemIds = new HashSet<>();
        for (Pipes.Pipe pipe : Pipes.all()) {
            Block block = pipe.block();
            assertTrue(blockIds.add(block.id()), "two pipes share a block id");
            assertTrue(itemIds.add(pipe.item().id()), "two pipes share an item id");
            assertTrue(block.id() >= Pipes.FIRST_BLOCK_ID && block.id() < Pipes.FIRST_BLOCK_ID + Pipes.COUNT,
                    pipe + " stands outside the run of the pipe blocks");
            assertTrue(pipe.item().id() >= Pipes.FIRST_ITEM_ID
                    && pipe.item().id() < Pipes.FIRST_ITEM_ID + Pipes.COUNT,
                    pipe + " stands outside the run of the pipe items");
            assertEquals(block, pipe.item().block());
            assertEquals(pipe.item(), ItemRegistry.byName(pipe.name()));
            assertEquals(pipe.item(), ItemRegistry.byId(pipe.item().id()));
        }
        assertEquals(Pipes.COUNT, blockIds.size());
        assertEquals(Pipes.COUNT, itemIds.size());
        // The runs of the pipes are given a little more room than the pipes of today need - a material that
        // gains a size is appended without moving a block behind it - so the run is checked against the ids
        // of the blocks and items behind it and not against their exact value.
        assertEquals(Pipes.FIRST_BLOCK_ID + Pipes.COUNT - 1,
                Pipes.all().get(Pipes.COUNT - 1).block().id(), "the last pipe block ends the run");
        assertTrue(Pipes.FIRST_BLOCK_ID + Pipes.COUNT <= Blocks.NEXT_FREE_ID,
                "the run of the pipe blocks reaches into the blocks behind it");
        assertTrue(Pipes.FIRST_ITEM_ID + Pipes.COUNT <= Items.NEXT_FREE_ID,
                "the run of the pipe items reaches into the items behind it");
    }

    @Test
    void aPipeIsWalkedThroughAndCarriesAnEntity() {
        for (Pipes.Pipe pipe : Pipes.all()) {
            Block block = pipe.block();
            assertTrue(block.isDrawable(), pipe + " has no picture and would never be drawn");
            assertFalse(block.isSolid(), pipe + " would hold the player back");
            assertTrue(block.isGround(), pipe + " could not be walked through");
            assertFalse(block.isTransparent(),
                    pipe + " would keep the flat plate of its picture in a slot: a pipe that is see-through "
                            + "is never drawn as itself, see BlockIconRenderer");
            assertEquals(Pipes.STRAIGHT_MASK, block.itemState(),
                    pipe + " would be meshed as the bare stub of state zero in a hand, on the ground and in "
                            + "a slot instead of a straight length of itself");
            assertEquals(Pipes.BLOCK_ENTITY, block.blockEntityTypeName(), pipe + " without its entity");
            assertEquals(64, block.states().stateCount(), pipe + " has to know every way to be joined");
            for (BlockFace face : Pipes.DIRECTIONS) {
                assertEquals(2, block.states().valuesOf(face.name().toLowerCase(Locale.ROOT)).size(),
                        "a direction is a property of two values");
            }
        }
    }

    @Test
    void thePipesOfAMetalCarryItsColourAndTheWoodenOnesTheirOwnArt() {
        assertEquals(Materials.COPPER.color(), PipeMaterials.COPPER.color());
        assertEquals(Materials.BRONZE.color(), PipeMaterials.BRONZE.color());
        assertEquals(Materials.STEEL.color(), PipeMaterials.STEEL.color());
        assertTrue(PipeMaterials.BRONZE.texture().isTinted(), "the grey art of a metal takes its colour");
        assertFalse(PipeMaterials.WOOD.texture().isTinted(), "the art of wood is drawn as it is");
        assertEquals(PipeMaterials.BRONZE.color(),
                Pipes.of(PipeMaterials.BRONZE, PipeSize.MEDIUM).block().tint());
        assertEquals(PipeMaterials.WOOD.texture().end(PipeSize.MEDIUM),
                Pipes.of(PipeMaterials.WOOD, PipeSize.MEDIUM).block().texture(),
                "a pipe names the picture of its own size");
    }

    @Test
    void everyPipeCarriesMoreTheBiggerItIsAndEveryMaterialTakesSomeHeat() {
        for (PipeMaterial material : PipeMaterials.all()) {
            assertTrue(material.maxTemperature() > 273.0f, material + " freezes");
            int smaller = 0;
            for (PipeSize size : PipeSize.values()) {
                if (!material.hasSize(size) || size.isBundle()) {
                    continue;
                }
                int flow = material.flow(size);
                assertTrue(flow > smaller, material + " " + size + " carries no more than the size below it");
                smaller = flow;
            }
            assertTrue(material.flow(PipeSize.QUADRUPLE) <= material.flow(PipeSize.MEDIUM),
                    material + " bundle moves more than the single pipe of its cell");
            assertTrue(material.flow(PipeSize.NONUPLE) <= material.flow(PipeSize.QUADRUPLE),
                    material + " nine tubes move more than its four");
        }
        assertTrue(PipeMaterials.WOOD.maxTemperature() < PipeMaterials.COPPER.maxTemperature(),
                "wood takes less heat than copper");
        assertTrue(PipeMaterials.COPPER.maxTemperature() < PipeMaterials.BRONZE.maxTemperature(),
                "copper takes less heat than bronze");
        assertTrue(PipeMaterials.BRONZE.maxTemperature() < PipeMaterials.STEEL.maxTemperature(),
                "bronze takes less heat than steel");
    }

    /**
     * Checks the numbers of the table itself.
     * <p>
     * The rates and the temperatures are the table of the industry and no placeholder any more, so a handful
     * of cells of it is pinned down here: a line that changes one of them changes this test as well, which
     * is the point.
     */
    @Test
    void theNumbersOfTheTableAreTheNumbersOfTheIndustry() {
        assertEquals(350.0f, PipeMaterials.WOOD.maxTemperature());
        assertEquals(200, PipeMaterials.WOOD.flow(PipeSize.SMALL));
        assertEquals(600, PipeMaterials.WOOD.flow(PipeSize.MEDIUM));
        assertEquals(1200, PipeMaterials.WOOD.flow(PipeSize.LARGE));

        assertEquals(1000.0f, PipeMaterials.COPPER.maxTemperature());
        assertEquals(60, PipeMaterials.COPPER.flow(PipeSize.TINY));
        assertEquals(400, PipeMaterials.COPPER.flow(PipeSize.MEDIUM));
        assertEquals(1600, PipeMaterials.COPPER.flow(PipeSize.HUGE));
        assertEquals(120, PipeMaterials.COPPER.flow(PipeSize.NONUPLE));

        assertEquals(2000.0f, PipeMaterials.BRONZE.maxTemperature());
        assertEquals(9600, PipeMaterials.BRONZE.flow(PipeSize.HUGE));

        assertEquals(2500.0f, PipeMaterials.STEEL.maxTemperature());
        assertEquals(800, PipeMaterials.STEEL.flow(PipeSize.TINY));
        assertEquals(19200, PipeMaterials.STEEL.flow(PipeSize.HUGE));

        // A fluid of the game and the heat every one of them arrives with.
        assertEquals(300.0f, Fluids.WATER.temperature());
        assertEquals(373.0f, Fluids.STEAM.temperature());
        assertEquals(1300.0f, Fluids.LAVA.temperature());
        assertTrue(Fluids.STEAM.hotterThan(PipeMaterials.WOOD.maxTemperature()),
                "the steam of a boiler bursts a wooden pipe");
        assertFalse(Fluids.STEAM.hotterThan(PipeMaterials.COPPER.maxTemperature()),
                "and runs in a copper one");
        assertTrue(Fluids.LAVA.hotterThan(PipeMaterials.COPPER.maxTemperature()),
                "lava bursts a copper pipe");
        assertFalse(Fluids.LAVA.hotterThan(PipeMaterials.BRONZE.maxTemperature()),
                "and runs in a bronze one");
    }

    @Test
    void theMaskOfAPipeIsWrittenInTheBitsOfItsDirections() {
        assertEquals(32, Pipes.bit(BlockFace.NORTH));
        assertEquals(16, Pipes.bit(BlockFace.EAST));
        assertEquals(8, Pipes.bit(BlockFace.SOUTH));
        assertEquals(4, Pipes.bit(BlockFace.WEST));
        assertEquals(2, Pipes.bit(BlockFace.TOP));
        assertEquals(1, Pipes.bit(BlockFace.BOTTOM));
        assertEquals(63, Pipes.ALL_MASK);
        assertEquals(Pipes.ALL_MASK, Pipes.stateOf(Pipes.ALL_MASK));
        assertEquals(Pipes.STRAIGHT_MASK, Pipes.maskOf(Pipes.stateOf(Pipes.STRAIGHT_MASK)));
        assertTrue(Pipes.isConnected(Pipes.STRAIGHT_MASK, BlockFace.NORTH));
        assertTrue(Pipes.isConnected(Pipes.STRAIGHT_MASK, BlockFace.SOUTH));
        assertFalse(Pipes.isConnected(Pipes.STRAIGHT_MASK, BlockFace.EAST));
        assertEquals(Pipes.STRAIGHT_MASK, Pipes.mask(BlockFace.NORTH, BlockFace.SOUTH));
    }

    @Test
    void aTurnOfAPipeIsTheSamePipe() {
        assertEquals(Pipes.mask(BlockFace.WEST, BlockFace.EAST), Pipes.turned(Pipes.STRAIGHT_MASK, 1));
        assertEquals(Pipes.STRAIGHT_MASK, Pipes.turned(Pipes.STRAIGHT_MASK, 2));
        assertEquals(BlockFace.WEST, Pipes.turned(BlockFace.NORTH));
        assertEquals(BlockFace.TOP, Pipes.turned(BlockFace.TOP), "a vertical side stays vertical");
        Set<Integer> canonicals = new HashSet<>();
        for (int mask = 0; mask <= Pipes.ALL_MASK; mask++) {
            assertEquals(mask, Pipes.turned(mask, 4), "four turns come back to where they started");
            assertEquals(mask, Pipes.turned(Pipes.canonical(mask), Pipes.turnsToDraw(mask)),
                    "the turn of a state has to show the mask it stands for");
            assertTrue(Pipes.canonical(mask) <= mask, "the canonical mask is the smallest of the four");
            assertEquals(Pipes.canonical(mask), Pipes.canonical(Pipes.turned(mask, 1)),
                    "every turn of a pipe is the same pipe");
            canonicals.add(Pipes.canonical(mask));
        }
        assertEquals(24, canonicals.size(), "the number of models one size of one family needs");
    }
}
