package com.philia093.neofactory.blockentity;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.machine.SteamBoilerMachine;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what happens to the block of a machine that ruined itself.
 * <p>
 * A boiler that was pushed past its limit is destroyed where it stands, and so is everything it held: the
 * block goes, the entity that carried its slots and tanks goes with it, and nothing is handed back to the
 * player. Only the block itself is destroyed - the world around it is untouched, which is the part of an
 * explosion that is not written yet.
 */
class MachineRuinTest {

    private static final int SEED = 77;
    private static final int X = 6;
    private static final int Y = 200;
    private static final int Z = -3;

    /** Ticks the boiler of the test is given to ruin itself, twice what it needs. */
    private static final int TICKS = 400;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aRuinedBoilerLeavesNothingBehind() {
        World world = new World(SEED, 0, 0);
        world.setBlock(X, Y, Z, Blocks.BRONZE_BOILER);
        SteamBoilerMachine boiler = new SteamBoilerMachine();
        MachineBlockEntity entity = new MachineBlockEntity(BlockEntityTypes.BRONZE_BOILER, boiler);
        entity.setPosition(X, Y, Z);
        world.addBlockEntity(entity);

        // A fire under a boiler that has no water: the temperature climbs until the boiler is ruined.
        boiler.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.BLAZE_ROD, 1));
        for (int tick = 0; tick < TICKS && !boiler.isExploded(); tick++) {
            entity.tick(world, TickClock.TICK_SECONDS);
        }

        assertTrue(boiler.isExploded(), "the boiler got too hot");
        assertEquals(Blocks.AIR, world.getBlock(X, Y, Z), "and the block is gone");
        assertNull(world.blockEntity(X, Y, Z), "with the entity that held its slots and tanks");
    }
}
