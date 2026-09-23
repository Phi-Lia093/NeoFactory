package com.philia093.neofactory.world.interaction;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.fluid.FluidFlow;
import com.philia093.neofactory.fluid.FluidState;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a bucket and a cell do with the world.
 * <p>
 * The rules are checked on a real world, because filling and pouring touch three things at once:
 * the fluid of the cell, the block of the world and the stack the player holds. The tests work on
 * a patch of empty ground at the spawn of a fresh world, exactly like {@code FluidFlowTest}.
 */
class FluidInteractionTest {

    /** Layer the fluids of the tests stand in: the one the player stands in. */
    private static final int LAYER = Chunk.LAYER_OBJECT;

    /** Seed of the test world, a fixed one keeps a failure reproducible. */
    private static final int SEED = 777;

    /** Half the side of the empty patch. */
    private static final int PATCH = 12;

    /** A fluid of the industry, one the game has no item for yet. */
    private static final Fluid OIL = oilOfTheIndustry();

    /**
     * Builds the fluid of the industry.
     * <p>
     * The tables of the game are filled first: a fluid is built with the block it stands in the
     * world with, and the blocks of the game only exist once they are registered.
     *
     * @return the fluid
     */
    private static Fluid oilOfTheIndustry() {
        TestRegistries.ensure();
        return new Fluid("oil", new Color(0.1f, 0.1f, 0.1f, 1.0f), "generic_fluid", 1, 4, 3, 10,
                false, Blocks.STONE);
    }

    private World world;
    private PlayerInventory inventory;

    @BeforeAll
    static void register() {
        TestRegistries.ensure();
    }

    @BeforeEach
    void freshWorld() {
        world = new World(SEED);
        clearPatch();
        inventory = new PlayerInventory();
    }

    @Test
    void anEmptyBucketTakesTheWaterOfASource() {
        pourFluid(Fluids.WATER, 0, 0);
        hold(Items.BUCKET);

        assertTrue(use(0, 0), "the bucket was filled");

        assertEquals(Items.WATER_BUCKET, inventory.heldStack().item());
        assertEquals(Blocks.AIR, blockAt(0, 0), "the source is gone");
        assertEquals(0, world.getState(0, Chunk.flatY(LAYER), 0), "and its state went with it");
    }

    @Test
    void aFilledBucketPoursItsWaterBack() {
        hold(Items.WATER_BUCKET);

        assertTrue(use(0, 0), "the water was poured");

        assertEquals(Items.BUCKET, inventory.heldStack().item(), "the player holds an empty bucket");
        assertEquals(Fluids.WATER.block(), blockAt(0, 0));
        assertTrue(FluidState.unpack(world.getState(0, Chunk.flatY(LAYER), 0)).isSource(),
                "what a bucket pours is a source");
    }

    @Test
    void aRunningSpillIsNotTaken() {
        pourFluid(Fluids.WATER, 0, 0);
        settle();
        hold(Items.BUCKET);

        assertFalse(use(3, 0), "a cell the water only ran through cannot be taken");
        assertEquals(Fluids.WATER.block(), blockAt(3, 0), "the water stayed");
        assertEquals(Items.BUCKET, inventory.heldStack().item(), "and the bucket stayed empty");
    }

    @Test
    void aBucketRefusesAFluidOfTheIndustry() {
        pourFluid(OIL, 0, 0);
        hold(Items.BUCKET);

        assertFalse(use(0, 0), "a bucket never takes oil");
        assertEquals(OIL.block(), blockAt(0, 0), "the fluid stayed where it was");
        assertEquals(Items.BUCKET, inventory.heldStack().item());
    }

    @Test
    void aCellTakesTheFluidOfASource() {
        pourFluid(Fluids.LAVA, 0, 0);
        hold(Items.FLUID_CELL);

        assertTrue(use(0, 0), "the cell was filled");

        assertEquals(Items.LAVA_CELL, inventory.heldStack().item());
        assertEquals(Blocks.AIR, blockAt(0, 0));
    }

    @Test
    void aFluidTheGameHasNoItemForIsNotTaken() {
        pourFluid(OIL, 0, 0);
        hold(Items.FLUID_CELL);

        assertFalse(use(0, 0), "the game has no cell of oil yet");
        assertEquals(OIL.block(), blockAt(0, 0), "so the fluid stays in the world");
        assertEquals(Items.FLUID_CELL, inventory.heldStack().item());
    }

    @Test
    void pouringIntoATakenCellDoesNothing() {
        world.setBlock(0, Chunk.flatY(LAYER), 0, Blocks.STONE);
        hold(Items.WATER_BUCKET);

        assertFalse(use(0, 0), "the cell is taken");
        assertEquals(Blocks.STONE, blockAt(0, 0));
        assertEquals(Items.WATER_BUCKET, inventory.heldStack().item(), "the bucket stayed full");
    }

    @Test
    void anItemThatCarriesNoFluidIsLeftAlone() {
        hold(Items.STONE);

        assertFalse(use(0, 0), "a block is not poured");
        assertEquals(Items.STONE, inventory.heldStack().item());
        assertEquals(Blocks.AIR, blockAt(0, 0));
    }

    @Test
    void takingASourceDrainsTheSpillAroundIt() {
        pourFluid(Fluids.WATER, 0, 0);
        settle();
        assertEquals(Fluids.WATER.block(), blockAt(5, 0), "the water ran");

        hold(Items.BUCKET);
        assertTrue(use(0, 0), "the source was taken");
        settle();

        assertEquals(Blocks.AIR, blockAt(5, 0), "and the spill went with it");
    }

    @Test
    void aStackOfEmptyBucketsLeavesTheRestInTheHand() {
        pourFluid(Fluids.WATER, 0, 0);
        inventory.set(inventory.selectedSlot(), ItemStack.of(Items.BUCKET, 64));

        assertTrue(use(0, 0), "one bucket of the stack was filled");

        assertEquals(Items.BUCKET, inventory.heldStack().item());
        assertEquals(63, inventory.heldStack().count(), "the rest of the stack stayed in the hand");
        assertEquals(1, inventory.countOf(Items.WATER_BUCKET),
                "and the full bucket went into the inventory, because it does not stack");
    }

    @Test
    void aFullInventoryRefusesToFillAStackOfBuckets() {
        // Every slot is taken; the one in the hand holds the stack of empty buckets itself.
        for (int slot = 0; slot < PlayerInventory.SLOT_COUNT; slot++) {
            if (slot == inventory.selectedSlot()) {
                continue;
            }
            inventory.set(slot, ItemStack.of(Items.STONE, Items.STONE.maxStackSize()));
        }
        pourFluid(Fluids.WATER, 0, 0);
        inventory.set(inventory.selectedSlot(), ItemStack.of(Items.BUCKET, 64));

        assertFalse(use(0, 0), "a full bucket needs a slot of its own and there is none");

        assertEquals(Fluids.WATER.block(), blockAt(0, 0), "so the source stayed in the world");
        assertEquals(64, inventory.heldStack().count(), "and the stack was not touched");
    }

    @Test
    void aFluidIsPouredIntoTheLayerThePlayerAimsAt() {
        // A hole in the ground: the player may aim at the ground layer and pour into it.
        world.setBlock(0, Chunk.flatY(FluidFlow.FLOOR_LAYER), 0, Blocks.AIR);
        hold(Items.WATER_BUCKET);

        assertTrue(use(0, 0, FluidFlow.FLOOR_LAYER), "water runs into the hole");

        assertEquals(Fluids.WATER.block(), blockAt(0, 0, FluidFlow.FLOOR_LAYER));
        assertEquals(Blocks.AIR, blockAt(0, 0, LAYER), "and not into the layer above");
    }

    @Test
    void groundBlocksTheWaterOfItsOwnLayer() {
        hold(Items.WATER_BUCKET);

        assertFalse(use(0, 0, FluidFlow.FLOOR_LAYER), "the ground is in the way");

        assertEquals(Blocks.STONE, blockAt(0, 0, FluidFlow.FLOOR_LAYER), "the ground stayed");
        assertEquals(Items.WATER_BUCKET, inventory.heldStack().item(), "and the bucket stayed full");
    }

    @Test
    void nothingIsPouredOverAHole() {
        world.setBlock(0, Chunk.flatY(FluidFlow.FLOOR_LAYER), 0, Blocks.AIR);
        hold(Items.WATER_BUCKET);

        assertFalse(use(0, 0, LAYER), "water over a hole would float in the air");

        assertEquals(Blocks.AIR, blockAt(0, 0, LAYER));
        assertEquals(Items.WATER_BUCKET, inventory.heldStack().item());
    }

    @Test
    void aFluidDoesNotCareWhatStandsAboveIt() {
        world.setBlock(0, Chunk.flatY(FluidFlow.FLOOR_LAYER), 0, Blocks.AIR);
        world.setBlock(0, Chunk.flatY(LAYER), 0, Blocks.TALL_GRASS);
        hold(Items.WATER_BUCKET);

        // A world of cubes has no layers to skip: a cell of the ground is filled when it is empty and
        // the world carries it, no matter what stands in the cell above - which is what a spill under
        // a plant looks like. The flat view knew only two cells per column, so it had to keep the
        // ground out of reach until the cell the player stands in was empty.
        assertTrue(use(0, 0, FluidFlow.FLOOR_LAYER), "the ground below a plant takes water");
        assertEquals(Fluids.WATER.block(), blockAt(0, 0, FluidFlow.FLOOR_LAYER));
        assertEquals(Blocks.TALL_GRASS, blockAt(0, 0, LAYER), "the plant above is untouched");
    }

    @Test
    void aBucketIsFilledFromTheLayerThePlayerAimsAt() {
        world.setBlock(0, Chunk.flatY(FluidFlow.FLOOR_LAYER), 0, Fluids.WATER.block());
        world.setState(0, Chunk.flatY(FluidFlow.FLOOR_LAYER), 0, FluidState.SOURCE.pack());
        hold(Items.BUCKET);

        assertFalse(use(0, 0, LAYER), "there is no water in the layer above");
        assertTrue(use(0, 0, FluidFlow.FLOOR_LAYER), "but there is water in the ground");

        assertEquals(Items.WATER_BUCKET, inventory.heldStack().item());
        assertEquals(Blocks.AIR, blockAt(0, 0, FluidFlow.FLOOR_LAYER), "the hole is dry again");
    }

    /** Uses the held stack on a cell of the fluid layer. */
    private boolean use(int x, int y) {
        return use(x, y, LAYER);
    }

    /** Uses the held stack on a cell of a layer, the way the flat view names it. */
    private boolean use(int x, int y, int layer) {
        return FluidInteraction.use(world, BlockTarget.of(x, Chunk.flatY(layer), y), inventory);
    }

    /** Puts an item into the hand of the player. */
    private void hold(Item item) {
        inventory.set(inventory.selectedSlot(), ItemStack.of(item, 1));
    }

    /** Puts the block of a fluid into a cell and marks it as a source. */
    private void pourFluid(Fluid fluid, int x, int y) {
        world.setBlock(x, Chunk.flatY(LAYER), y, fluid.block());
        world.setState(x, Chunk.flatY(LAYER), y, FluidState.SOURCE.pack());
    }

    /** Empties the object layer of a patch around the spawn, so a spill has room to run. */
    private void clearPatch() {
        for (int y = -PATCH; y <= PATCH; y++) {
            for (int x = -PATCH; x <= PATCH; x++) {
                world.setBlock(x, Chunk.flatY(FluidFlow.FLOOR_LAYER), y, Blocks.STONE);
                world.setState(x, Chunk.flatY(FluidFlow.FLOOR_LAYER), y, 0);
                world.setBlock(x, Chunk.flatY(LAYER), y, Blocks.AIR);
                world.setState(x, Chunk.flatY(LAYER), y, 0);
            }
        }
    }

    /** Ticks the world until no fluid waits for a look. */
    private void settle() {
        for (int tick = 0; tick < 400 && world.fluids().pendingCellCount() > 0; tick++) {
            world.tick(TickClock.TICK_SECONDS);
        }
    }

    /** Block of a cell of the layer the fluids live in. */
    private Block blockAt(int x, int y) {
        return blockAt(x, y, LAYER);
    }

    /** Block of a cell of a layer. */
    private Block blockAt(int x, int y, int layer) {
        return world.getBlock(x, Chunk.flatY(layer), y);
    }
}
