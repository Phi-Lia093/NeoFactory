package com.philia093.neofactory.machine;

import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.TickClock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the bronze boiler, the machine that turns water into steam with a fire under it.
 * <p>
 * The boiler is a fire, a tank of water and a temperature, so it is checked here without a world and without
 * a window: the fire heats it, the water carries the heat away while it boils, and a fire with nothing to
 * boil heats the boiler until it is ruined.
 */
class SteamBoilerMachineTest {

    /** Water one second of boiling takes, which the tests measure against. */
    private static final int WATER_PER_SECOND = (int) SteamBoilerMachine.WATER_PER_SECOND;

    /** Steam one second of boiling makes. */
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

    /**
     * Lets a boiler run for a stretch of time the way the world does.
     * <p>
     * The machine is ticked in the steps of the game, see {@link TickClock#TICK_SECONDS}, because its
     * temperature follows the time and not the frames.
     *
     * @param machine boiler to run
     * @param seconds time to run it for
     */
    private static void run(SteamBoilerMachine machine, float seconds) {
        int ticks = Math.round(seconds / TickClock.TICK_SECONDS);
        for (int tick = 0; tick < ticks; tick++) {
            machine.tick(TickClock.TICK_SECONDS);
        }
    }

    @Test
    void aBoilerStartsAtTheTemperatureOfTheRoom() {
        assertEquals(SteamBoilerMachine.STANDARD_TEMPERATURE, boiler.temperature(), 0.001f);
        assertEquals("Temp: 298K", boiler.statusText(), "the corner of the panel reads the temperature");
        assertFalse(boiler.isRunning());
        assertEquals(MachineError.NONE, boiler.error(), "a cold boiler is not broken");
    }

    @Test
    void aFlameHeatsTheBoilerAndItCoolsDownAgain() {
        boiler.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.STICK, 1));

        // A stick burns for five seconds, see Fuels.
        run(boiler, 5.0f);

        assertEquals(SteamBoilerMachine.STANDARD_TEMPERATURE + 5 * SteamBoilerMachine.HEAT_PER_SECOND,
                boiler.temperature(), 0.01f);
        assertFalse(boiler.isRunning(), "the stick burned down");

        run(boiler, 2.0f);

        assertEquals(SteamBoilerMachine.STANDARD_TEMPERATURE + 5 * SteamBoilerMachine.HEAT_PER_SECOND
                        - 2 * SteamBoilerMachine.COOL_PER_SECOND,
                boiler.temperature(), 0.01f, "the boiler loses heat without a flame");
    }

    @Test
    void theBoilerBoilsAtOneHundredDegreesAndStaysThere() {
        boiler.water().fill(Fluids.WATER, 8000, false);
        boiler.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.COAL, 1));

        run(boiler, 10.0f);

        assertEquals(SteamBoilerMachine.BOILING_TEMPERATURE, boiler.temperature(), 0.01f,
                "water that boils carries the heat away, so the boiler stays at the boiling point");
        assertEquals(Fluids.STEAM, boiler.steam().fluid());
        assertFalse(boiler.isExploded(), "a boiler that boils is a boiler that works");

        int water = boiler.water().amount();
        int steam = boiler.steam().amount();
        run(boiler, 1.0f);

        assertEquals(WATER_PER_SECOND, water - boiler.water().amount(), "one second of boiling");
        assertEquals(STEAM_PER_SECOND, boiler.steam().amount() - steam);
    }

    @Test
    void theFuelDecidesHowLongTheBoilerStaysHotAndNotHowFastItBoils() {
        SteamBoilerMachine withCoal = new SteamBoilerMachine();
        SteamBoilerMachine withBlazeRod = new SteamBoilerMachine();
        for (SteamBoilerMachine one : List.of(withCoal, withBlazeRod)) {
            one.water().fill(Fluids.WATER, 8000, false);
        }
        withCoal.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.COAL, 1));
        withBlazeRod.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.BLAZE_ROD, 1));

        run(withCoal, 10.0f);
        run(withBlazeRod, 10.0f);

        assertEquals(withCoal.temperature(), withBlazeRod.temperature(), 0.01f);
        assertEquals(withCoal.water().amount(), withBlazeRod.water().amount(),
                "the same stretch of fire boils the same water, whatever it burns");
        assertEquals(withCoal.steam().amount(), withBlazeRod.steam().amount());
    }

    @Test
    void aBoilerThatBoiledDryIsRuinedByTheFirstWater() {
        boiler.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.COAL, 1));

        // Eight seconds of a fire under an empty tank take the boiler past the boiling point.
        run(boiler, 8.0f);

        assertTrue(boiler.temperature() >= SteamBoilerMachine.BOILING_TEMPERATURE);
        assertFalse(boiler.isExploded(), "a hot boiler that ran dry is not lost yet");
        assertEquals(MachineError.NO_WATER, boiler.error(), "and it says what it is missing");

        boiler.water().fill(Fluids.WATER, 1000, false);
        run(boiler, TickClock.TICK_SECONDS);

        assertTrue(boiler.isExploded(), "water cracks a boiler that boiled dry while it was hot");
    }

    @Test
    void aBoilerThatGetsTooHotIsRuined() {
        boiler.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.BLAZE_ROD, 1));

        // The temperature climbs ten kelvin a second while nothing boils, so twenty seconds are more than
        // the two hundred degrees a boiler can take.
        run(boiler, 20.0f);

        assertTrue(boiler.isExploded());
        float temperature = boiler.temperature();
        run(boiler, 1.0f);

        assertEquals(temperature, boiler.temperature(), 0.001f,
                "a ruined boiler stops where it is instead of burning on");
    }

    @Test
    void steamThatCannotLeaveHeatsTheBoilerAsWell() {
        boiler.water().fill(Fluids.WATER, 1000, false);
        boiler.steam().fill(Fluids.STEAM, SteamBoilerMachine.STEAM_CAPACITY, false);
        boiler.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.COAL, 1));

        run(boiler, 10.0f);

        assertTrue(boiler.temperature() > SteamBoilerMachine.BOILING_TEMPERATURE,
                "a full tank of steam leaves the heat nowhere to go");
        assertEquals(1000, boiler.water().amount(), "and no water was boiled");
    }

    @Test
    void waterIsHarmlessOnceTheBoilerCooledDownAgain() {
        boiler.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.STICK, 2));

        // Ten seconds of fire under an empty tank: two sticks take the boiler past the boiling point.
        run(boiler, 10.0f);

        assertTrue(boiler.temperature() >= SteamBoilerMachine.BOILING_TEMPERATURE);
        assertFalse(boiler.isExploded(), "a hot boiler that ran dry is not lost yet");

        // The fire is out and the boiler falls below the boiling point.
        run(boiler, 6.0f);

        assertTrue(boiler.temperature() < SteamBoilerMachine.BOILING_TEMPERATURE);
        boiler.water().fill(Fluids.WATER, 1000, false);
        boiler.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.STICK, 8));
        run(boiler, 10.0f);

        assertFalse(boiler.isExploded(), "a boiler that had cooled down takes water again");
        assertTrue(boiler.steam().amount() > 0, "and it boils like any other boiler");
    }

    @Test
    void theBarShowsTheFlameAndTheCornerTheTemperature() {
        MachineMenu menu = new MachineMenu(boiler, new PlayerInventory());

        assertEquals("Temp: 298K", menu.statusText());
        assertEquals(0.0f, menu.craftProgress(), 0.001f, "nothing burns in an empty boiler");
        assertEquals(0.0f, menu.fuelSeconds(), 0.001f, "the corner carries no fuel line any more");
        assertEquals(2 + PlayerInventory.SLOT_COUNT, menu.container().layout().slots().size(),
                "the fuel, the ash and the inventory of the player");
        assertEquals(2, menu.fluidSlots().size(), "a tank of water and a tank of steam");
        assertTrue(menu.container().layout().slots().get(SteamBoilerMachine.OUTPUT).isOutput(),
                "the slot the ash will land in is a product slot");

        // A boiler that boils does not get hot, so the coal of this test may burn without ruining it.
        boiler.water().fill(Fluids.WATER, 8000, false);
        boiler.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.COAL, 1));
        boiler.tick(TickClock.TICK_SECONDS);

        assertTrue(menu.craftProgress() < 0.01f, "the bar has barely started");

        run(boiler, 40.0f);

        assertEquals(0.5f, menu.craftProgress(), 0.01f,
                "half of the coal is burned, so the bar filled from the left");
    }

    @Test
    void theWorkOfTheBoilerSurvivesASaveGame() {
        boiler.water().fill(Fluids.WATER, 1000, false);
        boiler.inventory().set(SteamBoilerMachine.FUEL, ItemStack.of(Items.COAL, 2));
        run(boiler, 3.0f);

        NbtCompound data = new NbtCompound("Boiler");
        boiler.save(data);
        SteamBoilerMachine restored = new SteamBoilerMachine();
        restored.load(data);

        assertEquals(boiler.temperature(), restored.temperature(), 0.01f);
        assertEquals(boiler.water().amount(), restored.water().amount());
        assertEquals(boiler.steam().amount(), restored.steam().amount());
        assertEquals(boiler.craftProgress(), restored.craftProgress(), 0.001f);
        assertEquals(boiler.statusText(), restored.statusText());
        assertTrue(restored.isRunning(), "the flame burns on after the world was opened again");
    }
}
