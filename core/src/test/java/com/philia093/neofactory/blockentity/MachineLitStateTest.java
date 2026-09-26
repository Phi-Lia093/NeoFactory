package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.machine.SteamBoilerMachine;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        // A stick burns for five seconds, see Fuels, and five seconds of fire do not take this boiler past
        // the boiling point: it ends up cold instead of ruined.
        entity.tick(world, 5.0f);

        assertFalse(boiler.isExploded(), "the boiler was not pushed past its limit");
        assertFalse(boiler.isRunning(), "the stick burned down");
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

    /** Value of the property of the light in the cell of the test. */
    private static String litOf(World world) {
        return Blocks.BRONZE_BOILER.states().decode(world.getState(X, Y, Z))
                .get(MachineBlockEntity.LIT);
    }
}
