package com.philia093.neofactory.world;

import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.save.LevelData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the clock of a world that is played in: how it starts, how it advances and what a save game
 * carries of it.
 * <p>
 * The world is a flat one, because nothing here is about the terrain: what is asked is what the game asks a
 * world - the hour of the day, the ticks of a frame and the flag of the rule - and no window is opened.
 */
class WorldTimeTest {

    /** Seed the worlds of these cases are built with, any fixed value does. */
    private static final int SEED = 20240924;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aNewWorldStartsInTheMorning() {
        World world = flatWorld();

        assertEquals(DayCycle.NEW_WORLD_TIME, world.worldTime());
        assertEquals("morning", DayCycle.timeName(world.worldTime()));
        assertTrue(world.daylightCycle(), "the day of a new world keeps moving");
        assertEquals(0L, world.tickCount(), "and no tick has run yet");
    }

    @Test
    void everyTickOfTheWorldIsATickOfTheDay() {
        World world = flatWorld();

        for (int tick = 0; tick < 20; tick++) {
            world.tick(TickClock.TICK_SECONDS);
        }

        assertEquals(DayCycle.NEW_WORLD_TIME + 20L, world.worldTime(),
                "a second of play is twenty ticks of the day");
        assertEquals(20L, world.tickCount());
        world.tick(0.0f);
        assertEquals(DayCycle.NEW_WORLD_TIME + 20L, world.worldTime(), "a frame without time is no tick");
    }

    @Test
    void aWorldWhoseDayCycleIsOffKeepsItsHour() {
        World world = flatWorld();
        world.setDaylightCycle(false);
        long before = world.worldTime();

        for (int tick = 0; tick < 100; tick++) {
            world.tick(TickClock.TICK_SECONDS);
        }

        assertEquals(before, world.worldTime(), "the sun stands still");
        assertEquals(100L, world.tickCount(), "the rest of the world keeps running");

        world.setDaylightCycle(true);
        world.tick(TickClock.TICK_SECONDS);
        assertEquals(before + 1L, world.worldTime(), "and the day moves on once it is switched back on");
    }

    @Test
    void theHourOfTheWorldCanBeSetAndMovedOn() {
        World world = flatWorld();

        world.setWorldTime(DayCycle.SUNSET);
        assertEquals(DayCycle.SUNSET, world.worldTime());
        assertEquals("dusk", DayCycle.timeName(world.worldTime()));

        world.addWorldTime(DayCycle.MIDNIGHT - DayCycle.SUNSET);
        assertEquals(DayCycle.MIDNIGHT, world.worldTime());
        world.addWorldTime(0L);
        world.addWorldTime(-500L);
        assertEquals(DayCycle.MIDNIGHT, world.worldTime(), "only the future is added");
    }

    @Test
    void theHourOfAWorldTravelsWithItsSaveGame() {
        LevelData data = new LevelData();
        assertEquals(DayCycle.NEW_WORLD_TIME, data.worldTime(), "a world that was never stored is at morning");
        assertTrue(data.daylightCycle());

        data.setWorldTime(DayCycle.SUNSET + 500L);
        data.setDaylightCycle(false);
        LevelData read = LevelData.read(data.write(new PlayerInventory()));

        assertEquals(DayCycle.SUNSET + 500L, read.worldTime(),
                "a world that is opened again stands at the hour it was left at");
        assertFalse(read.daylightCycle(), "and a day that was switched off stays switched off");
    }

    @Test
    void aWorldStoredBeforeTheDayCycleExistedStartsInTheMorning() {
        LevelData data = new LevelData();
        NbtCompound root = data.write(new PlayerInventory());
        root.remove("Time");

        LevelData read = LevelData.read(root);

        assertEquals(DayCycle.NEW_WORLD_TIME, read.worldTime(),
                "a file without an hour is read as the morning and not as the night");
        assertTrue(read.daylightCycle(), "and its day moves");
    }

    /** A flat world, the land every case of this test works in. */
    private static World flatWorld() {
        return new World(SEED, 0, 0, null, WorldType.FLAT);
    }
}
