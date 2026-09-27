package com.philia093.neofactory.chat.command;

import com.philia093.neofactory.chat.ChatMessage;
import com.philia093.neofactory.support.TestCommandContext;
import com.philia093.neofactory.world.DayCycle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the command that reports and moves the hour of the world.
 * <p>
 * The command is driven through a hand written context, so no window and no world is needed: what is asked is
 * the very thing the game asks, see {@code CommandContext#worldTime()}.
 */
class TimeCommandTest {

    private final CommandRegistry registry = CommandRegistry.withDefaults();
    private final TestCommandContext context = new TestCommandContext();

    @Test
    void theHourOfTheWorldIsReported() {
        registry.run("/time", context);

        assertEquals(ChatMessage.Kind.SYSTEM, context.lastLine().kind());
        assertEquals("It is 07:00 (1000 ticks, morning).", context.lastLine().text());
    }

    @Test
    void anHourCanBeSetByName() {
        registry.run("/time set noon", context);

        assertEquals(DayCycle.NOON, context.worldTime());
        assertEquals("The time is now 12:00 (6000 ticks, noon).", context.lastLine().text());
    }

    @Test
    void everyHourOfTheDayHasAName() {
        registry.run("/time set dawn", context);
        assertEquals(DayCycle.SUNRISE, context.worldTime());

        registry.run("/time set dusk", context);
        assertEquals(DayCycle.SUNSET, context.worldTime());

        registry.run("/time set midnight", context);
        assertEquals(DayCycle.MIDNIGHT, context.worldTime());

        registry.run("/time set night", context);
        assertEquals(DayCycle.SUNSET + DayCycle.TWILIGHT_TICKS, context.worldTime());
        assertEquals("night", DayCycle.timeName(context.worldTime()));
    }

    @Test
    void anHourCanBeSetByTicksOfTheDay() {
        registry.run("/time set 18000", context);

        assertEquals(DayCycle.MIDNIGHT, context.worldTime());
        assertEquals("00:00 (18000 ticks, night)", DayCycle.describe(context.worldTime()));
    }

    @Test
    void aJumpToAnHourKeepsTheDayOfTheWorld() {
        context.setWorldTime(DayCycle.DAY_TICKS * 3L + 500L);

        registry.run("/time set noon", context);

        assertEquals(DayCycle.DAY_TICKS * 3L + DayCycle.NOON, context.worldTime(),
                "the third day of a world is still the third day");
    }

    @Test
    void ticksCanBeAddedToTheHour() {
        registry.run("/time add 1000", context);

        assertEquals(DayCycle.NEW_WORLD_TIME + 1000L, context.worldTime());
        assertEquals("The time is now 08:00 (2000 ticks, morning).", context.lastLine().text());
    }

    @Test
    void addingStopsAtAnAmountThatIsNoAmount() {
        registry.run("/time add many", context);
        assertEquals(ChatMessage.Kind.ERROR, context.lastLine().kind());

        registry.run("/time add -5", context);
        assertEquals(ChatMessage.Kind.ERROR, context.lastLine().kind(), "there is no way back to yesterday");
        registry.run("/time add 0", context);
        assertEquals(ChatMessage.Kind.ERROR, context.lastLine().kind(), "no amount at all is no amount");
        assertEquals(DayCycle.NEW_WORLD_TIME, context.worldTime(), "nothing was moved");
    }

    @Test
    void settingStopsAtAnHourThatIsNoHour() {
        registry.run("/time set breakfast", context);

        assertEquals(ChatMessage.Kind.ERROR, context.lastLine().kind());
        assertEquals(DayCycle.NEW_WORLD_TIME, context.worldTime(), "nothing was moved");
    }

    @Test
    void aLineThatIsNotASetOrAnAddReportsTheUsage() {
        registry.run("/time set", context);
        assertEquals(ChatMessage.Kind.ERROR, context.lastLine().kind());
        assertTrue(context.lastLine().text().startsWith("Usage: /time"), context.lastLine().text());

        registry.run("/time tomorrow", context);
        assertEquals(ChatMessage.Kind.ERROR, context.lastLine().kind());
    }
}
