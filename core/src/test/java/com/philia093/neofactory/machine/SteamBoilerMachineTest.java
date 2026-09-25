package com.philia093.neofactory.machine;

import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.nbt.NbtCompound;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the bronze boiler, the machine that turns water into steam.
 * <p>
 * The boiler is arithmetic on two tanks and a flame, so it is checked here without a world and without a
 * window: one unit of water becomes {@code STEAM_PER_WATER} units of steam, only whole units are moved, and
 * a boiler that has nothing to boil does not waste the fuel it was given.
 */
class SteamBoilerMachineTest {

    /** Water one second of burning boils, which the tests measure against. */
    private static final int WATER_PER_SECOND = (int) SteamBoilerMachine.WATER_PER_SECOND;

    /** Steam one second of burning makes. */
    private static final int STEAM_PER_SECOND = WATER_PER_SECOND * SteamBoilerMachine.STEAM_PER_WATER;

    private SteamBoilerMachine boiler;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @BeforeEach
    void setUp() {
        boiler = new SteamBoilerMachine();
    }

    @Test
    void aBoilerTurnsWaterIntoSteamWhileItBurns() {
        boiler.water().fill(Fluids.WATER, 1000, false);
        boiler.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.COAL, 1));

        boiler.tick(1.0f);

        assertTrue(boiler.isRunning(), "the coal is burning");
        assertEquals(1000 - WATER_PER_SECOND, boiler.water().amount(), "a second of water is gone");
        assertEquals(STEAM_PER_SECOND, boiler.steam().amount(), "and became steam");
        assertEquals(Fluids.STEAM, boiler.steam().fluid());
        assertEquals(0, boiler.inventory().get(SteamBoilerMachine.FUEL).count(),
                "the piece of coal was taken and burns now, like the fuel of a furnace");
    }

    @Test
    void aBoilerWithoutWaterDoesNotTakeFuel() {
        boiler.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.COAL, 1));

        boiler.tick(10.0f);

        assertEquals(1, boiler.inventory().get(SteamBoilerMachine.FUEL).count(),
                "a dry boiler does not chew through coal");
        assertFalse(boiler.isRunning());
        assertEquals(0, boiler.steam().amount());
        assertEquals(MachineError.NO_WATER, boiler.error(), "and it says what it is missing");
    }

    @Test
    void aBoilerWithoutFuelStandsStill() {
        boiler.water().fill(Fluids.WATER, 1000, false);

        boiler.tick(5.0f);

        assertFalse(boiler.isRunning());
        assertEquals(1000, boiler.water().amount(), "water that is not heated stays water");
        assertEquals(0, boiler.steam().amount());
    }

    @Test
    void aFullTankOfSteamPausesTheFlame() {
        boiler.water().fill(Fluids.WATER, 1000, false);
        boiler.steam().fill(Fluids.STEAM, SteamBoilerMachine.STEAM_CAPACITY, false);
        boiler.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.COAL, 1));

        boiler.tick(1.0f);

        assertFalse(boiler.isRunning(), "nothing burns while the steam has nowhere to go");
        assertEquals(1, boiler.inventory().get(SteamBoilerMachine.FUEL).count());
        assertEquals(1000, boiler.water().amount(), "and no water was boiled");
    }

    @Test
    void aFlameWaitsWhileTheWaterIsGone() {
        boiler.water().fill(Fluids.WATER, 100, false);
        boiler.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.COAL, 1));

        // Five seconds of burning boil the hundred units of water away.
        boiler.tick(5.0f);

        assertEquals(0, boiler.water().amount());
        assertTrue(boiler.isRunning(), "the flame is still there");
        float left = boiler.fuelSeconds();

        boiler.tick(2.0f);

        assertTrue(boiler.isRunning());
        assertEquals(left, boiler.fuelSeconds(), 0.001f, "the flame waits instead of burning away");
    }

    @Test
    void aFlameGoesOutWhenTheFuelIsUsedUp() {
        boiler.water().fill(Fluids.WATER, 1000, false);
        boiler.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.STICK, 1));

        // A stick burns for five seconds, see Fuels.
        boiler.tick(3.0f);
        assertTrue(boiler.isRunning());
        assertEquals(2.0f, boiler.fuelSeconds(), 0.001f);

        boiler.tick(3.0f);

        assertFalse(boiler.isRunning(), "the stick burned down");
        assertEquals(0, boiler.inventory().get(SteamBoilerMachine.FUEL).count());
        assertEquals(1000 - 5 * WATER_PER_SECOND, boiler.water().amount(), "five seconds of water");
        assertEquals(5 * STEAM_PER_SECOND, boiler.steam().amount(), "became steam");
    }

    @Test
    void theScreenOfTheBoilerCarriesOneSlotAndTwoTanks() {
        MachineMenu menu = new MachineMenu(boiler, new PlayerInventory());

        assertEquals("Bronze Boiler", menu.title());
        assertEquals(1 + PlayerInventory.SLOT_COUNT, menu.container().layout().slots().size(),
                "the fuel slot and the inventory of the player");
        assertEquals(2, menu.fluidSlots().size(), "a tank of water and a tank of steam");
        assertEquals(0.0f, menu.craftProgress(), 0.001f, "an empty boiler has nothing to show");

        boiler.steam().fill(Fluids.STEAM, SteamBoilerMachine.STEAM_CAPACITY / 2, false);

        assertEquals(0.5f, menu.craftProgress(), 0.001f, "the arrow shows how full the steam is");
    }

    @Test
    void theWorkOfTheBoilerSurvivesASaveGame() {
        boiler.water().fill(Fluids.WATER, 1000, false);
        boiler.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.COAL, 2));
        boiler.tick(2.5f);

        NbtCompound data = new NbtCompound("Boiler");
        boiler.save(data);
        SteamBoilerMachine restored = new SteamBoilerMachine();
        restored.load(data);

        assertEquals(boiler.water().amount(), restored.water().amount());
        assertEquals(Fluids.WATER, restored.water().fluid());
        assertEquals(boiler.steam().amount(), restored.steam().amount());
        assertEquals(boiler.fuelSeconds(), restored.fuelSeconds(), 0.001f);
        assertTrue(restored.isRunning(), "the flame burns on after the world was opened again");
        assertEquals(boiler.inventory().get(SteamBoilerMachine.FUEL).count(),
                restored.inventory().get(SteamBoilerMachine.FUEL).count());
    }
}
