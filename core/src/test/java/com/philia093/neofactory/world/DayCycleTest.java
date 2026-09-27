package com.philia093.neofactory.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the clock of a world: the hour of the day, the height of the sun, the brightness of the light of
 * the sky and the colour a frame is cleared with.
 * <p>
 * Everything here is a pure function of one number, so no world and no window is needed: what is asked is
 * what the game asks, see {@code DayCycle} and {@code GameScreen#renderCubes}.
 */
class DayCycleTest {

    @Test
    void aDayIsTwentyFourThousandTicks() {
        assertEquals(24000, DayCycle.DAY_TICKS);
        assertEquals(0L, DayCycle.ofDay(DayCycle.SUNRISE));
        assertEquals(23999L, DayCycle.ofDay(DayCycle.DAY_TICKS - 1));
        assertEquals(0L, DayCycle.ofDay(DayCycle.DAY_TICKS), "the next day starts at the sunrise again");
        assertEquals(500L, DayCycle.ofDay(DayCycle.DAY_TICKS + 500));
        assertEquals(0, DayCycle.NOON - DayCycle.DAY_TICKS / 4);
        assertEquals(0, DayCycle.SUNSET - DayCycle.DAY_TICKS / 2);
        assertEquals(0, DayCycle.MIDNIGHT - DayCycle.DAY_TICKS * 3 / 4);
    }

    @Test
    void theSunStandsHighestAtNoonAndLowestAtMidnight() {
        assertEquals(0.0f, DayCycle.sunHeight(DayCycle.SUNRISE), 0.001f);
        assertEquals(1.0f, DayCycle.sunHeight(DayCycle.NOON), 0.001f);
        assertEquals(0.0f, DayCycle.sunHeight(DayCycle.SUNSET), 0.001f);
        assertEquals(-1.0f, DayCycle.sunHeight(DayCycle.MIDNIGHT), 0.001f);
        assertTrue(DayCycle.sunIsUp(DayCycle.NEW_WORLD_TIME), "the sun of a new world is up");
        assertTrue(DayCycle.sunIsUp(DayCycle.NOON));
        assertFalse(DayCycle.sunIsUp(DayCycle.MIDNIGHT));
    }

    @Test
    void noonIsBrightAndTheNightIsDark() {
        assertEquals(DayCycle.NOON_BRIGHTNESS, DayCycle.brightness(DayCycle.NOON), 0.0001f);
        assertEquals(DayCycle.NIGHT_BRIGHTNESS, DayCycle.brightness(DayCycle.MIDNIGHT), 0.0001f);
        assertTrue(DayCycle.brightness(DayCycle.NEW_WORLD_TIME) > DayCycle.brightness(DayCycle.MIDNIGHT),
                "a new world starts in the morning and not at night");
        assertTrue(DayCycle.brightness(DayCycle.NEW_WORLD_TIME) < DayCycle.NOON_BRIGHTNESS,
                "and the morning is not yet the noon");
    }

    @Test
    void theDayGetsBrighterFromTheSunriseToTheNoon() {
        float previous = -1.0f;
        for (long tick = DayCycle.SUNRISE; tick <= DayCycle.NOON; tick += 250L) {
            float brightness = DayCycle.brightness(tick);
            assertTrue(brightness >= previous, "the brightness fell at tick " + tick);
            previous = brightness;
        }
        // And the night is darker than any hour of the day.
        assertTrue(DayCycle.brightness(DayCycle.SUNSET + 1) < DayCycle.brightness(DayCycle.SUNSET - 1),
                "the light goes down with the sun");
    }

    @Test
    void theSunRisesInTheEastAndSetsInTheWest() {
        Vector3 direction = new Vector3();

        DayCycle.sunDirection(DayCycle.SUNRISE, direction);
        assertEquals(1.0f, direction.x, 0.001f, "the sun comes up in the east");
        assertEquals(0.0f, direction.y, 0.001f);
        DayCycle.sunDirection(DayCycle.NOON, direction);
        assertEquals(0.0f, direction.x, 0.001f);
        assertEquals(1.0f, direction.y, 0.001f, "the sun stands overhead at noon");
        DayCycle.sunDirection(DayCycle.SUNSET, direction);
        assertEquals(-1.0f, direction.x, 0.001f, "the sun goes down in the west");
        assertEquals(0.0f, direction.y, 0.001f);
        DayCycle.moonDirection(DayCycle.MIDNIGHT, direction);
        assertEquals(1.0f, direction.y, 0.001f, "the moon stands overhead at midnight");
        assertEquals(1.0f, direction.len(), 0.001f, "a direction of the sky is a unit vector");
        assertTrue(DayCycle.sunAngle(DayCycle.NOON) > DayCycle.sunAngle(DayCycle.SUNRISE));
    }

    @Test
    void theSkyIsWarmAtTheSunriseAndColdAtMidnight() {
        Color noon = DayCycle.skyColor(DayCycle.NOON, new Color());
        Color midnight = DayCycle.skyColor(DayCycle.MIDNIGHT, new Color());
        Color sunrise = DayCycle.skyColor(DayCycle.SUNRISE, new Color());

        assertTrue(noon.b > noon.r, "the sky of the day is blue");
        assertTrue(midnight.b < 0.2f, "the sky of the night is almost black");
        assertTrue(sunrise.r > sunrise.b, "the sky of a sunrise is warm");
        assertEquals("dawn", DayCycle.timeName(DayCycle.SUNRISE));
        assertEquals("dusk", DayCycle.timeName(DayCycle.SUNSET));
        assertEquals("night", DayCycle.timeName(DayCycle.MIDNIGHT));
        assertEquals(1.0f, DayCycle.twilight(DayCycle.SUNRISE), 0.0001f);
        assertEquals(0.0f, DayCycle.twilight(DayCycle.NOON), 0.0001f);
        assertEquals(0.0f, DayCycle.twilight(DayCycle.MIDNIGHT), 0.0001f);
    }

    @Test
    void theClockReadsTheWayAPlayerReadsAClock() {
        assertEquals("06:00", DayCycle.format(DayCycle.SUNRISE));
        assertEquals("06:30", DayCycle.format(500));
        assertEquals("12:00", DayCycle.format(DayCycle.NOON));
        assertEquals("18:00", DayCycle.format(DayCycle.SUNSET));
        assertEquals("00:00", DayCycle.format(DayCycle.MIDNIGHT));
        assertEquals("06:00", DayCycle.format(DayCycle.DAY_TICKS), "the clock of the next morning");
        assertEquals("morning", DayCycle.timeName(DayCycle.NEW_WORLD_TIME));
        assertEquals("07:00 (1000 ticks, morning)", DayCycle.describe(DayCycle.NEW_WORLD_TIME));
        assertEquals("12:00 (6000 ticks, noon)", DayCycle.describe(DayCycle.NOON));
    }
}
