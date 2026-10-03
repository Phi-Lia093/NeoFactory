package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.machine.ElectricMaceratorMachine;
import com.philia093.neofactory.machine.SteamBoilerMachine;
import com.philia093.neofactory.recipe.RecipeLoader;
import com.philia093.neofactory.recipe.RecipeRegistry;
import com.philia093.neofactory.recipe.RecipeType;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a machine says in the state of its cell whether it works.
 * <p>
 * A boiler that burns is drawn with the glowing mouth of its front, see {@link MachineBlockEntity#LIT}, and
 * the state of a cell is what the mesh of a chunk reads: the block entity writes it while it ticks, so the
 * light of a burning boiler belongs to the world and not to the screen that happened to draw it.
 */
class MachineLitStateTest {

    private static final int SEED = 909;
    private static final int X = 3;
    private static final int Y = 200;
    private static final int Z = 5;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aBurningBoilerLightsTheFrontOfItsBlock() {
        World world = new World(SEED, 0, 0);
        world.setBlock(X, Y, Z, Blocks.BRONZE_BOILER);
        SteamBoilerMachine boiler = new SteamBoilerMachine();
        MachineBlockEntity entity = new MachineBlockEntity(BlockEntityTypes.BRONZE_BOILER, boiler);
        entity.setPosition(X, Y, Z);
        world.addBlockEntity(entity);

        assertEquals("false", litOf(world), "a boiler that stands still is not lit");

        boiler.water().fill(Fluids.WATER, 1000, false);
        boiler.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.STICK, 1));
        entity.tick(world, 1.0f);

        assertTrue(boiler.isRunning());
        assertEquals("true", litOf(world), "the mouth of a burning boiler glows");

        // A stick burns four and a half times as long in the boiler as in a furnace, see
        // SteamBoilerMachine#BRONZE_FUEL_SHARE, and this boiler holds water, so the fire does not take it past
        // the boiling point. The world ticks the machine a frame at a time, see TickClock, which is the rate a
        // boiler reaches its boiling point at before its heat is carried away.
        int ticks = Math.round(5.0f * SteamBoilerMachine.BRONZE_FUEL_SHARE / TickClock.TICK_SECONDS) + 2;
        for (int tick = 0; tick < ticks; tick++) {
            entity.tick(world, TickClock.TICK_SECONDS);
        }

        assertFalse(boiler.isExploded(), "the boiler was not pushed past its limit");
        assertFalse(boiler.isRunning(), "the stick burned down");

        // The light stands a moment past the last frame of work, so that a machine which hands one craft over
        // and takes the next one does not flicker, see MachineBlockEntity#LIT_HOLD_SECONDS.
        entity.tick(world, 0.5f);

        assertEquals("false", litOf(world), "and the light goes out with the flame");
    }

    @Test
    void aFurnaceIsNotAffectedByTheLightOfAMachine() {
        World world = new World(SEED, 0, 0);
        world.setBlock(X, Y, Z, Blocks.FURNACE);
        MachineBlockEntity entity = new MachineBlockEntity(BlockEntityTypes.FURNACE,
                new com.philia093.neofactory.machine.SmeltingMachine());
        entity.setPosition(X, Y, Z);
        world.addBlockEntity(entity);

        entity.tick(world, 1.0f);

        // The furnace carries no property for the light of a machine, so its cell is left alone.
        assertEquals(0, world.getState(X, Y, Z));
        assertFalse(Blocks.FURNACE.states().hasProperty(MachineBlockEntity.LIT));
    }

    @Test
    void aMachineOfTheLineLightsTheFrontOfItsBlock() {
        World world = new World(SEED, 0, 0);
        Block macerator = BlockRegistry.byName("macerator_lv");

        assertNotNull(macerator, "the game holds a macerator of the low voltage");
        assertTrue(macerator.states().hasProperty(MachineBlockEntity.LIT),
                "a machine of the line says in its cell whether it works");
        assertTrue(macerator.states().hasProperty(MachineBlockEntity.FACING), "and which way it looks");
        assertEquals(8, macerator.states().stateCount(),
                "four directions and the two faces of a machine that runs");
        world.setBlock(X, Y, Z, macerator);

        // A machine of the line works through the very recipe the grinder of bronze reads and pays for it out
        // of the buffer the block entity fills, see MachineBlockEntity and MachineFamilies.
        ElectricMaceratorMachine machine = new ElectricMaceratorMachine(Voltage.LOW);
        MachineBlockEntity entity = new MachineBlockEntity(
                new BlockEntityType("macerator_lv", type -> new MachineBlockEntity(type, machine)), machine);
        entity.setPosition(X, Y, Z);
        world.addBlockEntity(entity);

        RecipeRegistry.register(RecipeLoader.parse(RecipeType.GRINDING, "test_dust",
                "{ \"ingredient\": \"iron_ore\", \"result\": { \"item\": \"iron_dust\" },"
                        + " \"time\": 8.0, \"power\": 1, \"voltage\": 8 }"));
        machine.inventory().set(ElectricMaceratorMachine.INPUT, ItemStack.of(Items.IRON_ORE, 1));
        machine.buffer().setAmount(machine.bufferCapacity());

        entity.tick(world, TickClock.TICK_SECONDS);

        assertTrue(machine.isRunning(), "the machine started the work");
        assertEquals("true", litOf(world, macerator), "the front of a working machine glows");
    }

    /** Value of the property of the light in the cell of the test. */
    private static String litOf(World world) {
        return Blocks.BRONZE_BOILER.states().decode(world.getState(X, Y, Z))
                .get(MachineBlockEntity.LIT);
    }

    /** Value of the property of the light in the cell of the test, read through the states of a block. */
    private static String litOf(World world, Block block) {
        return block.states().decode(world.getState(X, Y, Z)).get(MachineBlockEntity.LIT);
    }
}
