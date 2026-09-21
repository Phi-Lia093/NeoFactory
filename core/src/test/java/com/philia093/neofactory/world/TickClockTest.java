package com.philia093.neofactory.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the clock that turns frames into ticks.
 * <p>
 * The whole simulation of the game hangs on this arithmetic: a machine must do the same
 * work whether the game runs at 60 frames per second or at 144, and a frame that took
 * long - a window that was dragged, a save game that was written - must not be replayed
 * until the game freezes. Both are numbers, so both are checked here.
 */
class TickClockTest {

    @Test
    void aFrameIsWorthTheTicksItCovers() {
        TickClock clock = new TickClock();

        assertEquals(20, TickClock.TICKS_PER_SECOND);
        assertEquals(1, clock.advance(TickClock.TICK_SECONDS));
        assertEquals(0, clock.advance(TickClock.TICK_SECONDS / 2.0f),
                "half a tick is not a tick yet");
        assertEquals(1, clock.advance(TickClock.TICK_SECONDS / 2.0f),
                "the halves add up to one");
        assertEquals(2L, clock.tickCount(), "one tick, then none, then one");
    }

    @Test
    void aLongFrameIsNotReplayedInFull() {
        TickClock clock = new TickClock();

        assertEquals(TickClock.MAX_TICKS_PER_FRAME, clock.advance(10.0f),
                "ten seconds are not replayed in one frame");
        assertEquals(0, clock.advance(0.0f), "a frame without time runs no tick");
        assertEquals(TickClock.MAX_TICKS_PER_FRAME, clock.tickCount(),
                "the time that could not be replayed is dropped");
    }

    @Test
    void theFrameRateDoesNotChangeTheWork() {
        TickClock fast = new TickClock();
        TickClock slow = new TickClock();
        for (int frame = 0; frame < 600; frame++) {
            fast.advance(1.0f / 60.0f);
        }
        for (int frame = 0; frame < 120; frame++) {
            slow.advance(1.0f / 12.0f);
        }

        long expected = 10L * TickClock.TICKS_PER_SECOND;
        assertEquals(expected, slow.tickCount());
        assertTrue(Math.abs(fast.tickCount() - expected) <= 1L,
                "60 frames per second ticked " + fast.tickCount() + " times instead of " + expected);
    }
}
