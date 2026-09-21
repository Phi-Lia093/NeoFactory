package com.philia093.neofactory.world;

/**
 * Turns the time of a frame into whole ticks of the simulation.
 * <p>
 * The world is advanced in fixed steps instead of by the length of a frame: a machine
 * that works for ten seconds does the same work whether the game runs at 60 or at 144
 * frames per second, and a frame that took long is caught up by running the steps it
 * missed. Everything a machine does per second is therefore expressed in ticks and never
 * in frames, see {@link #TICKS_PER_SECOND}.
 * <p>
 * A frame never runs more than {@link #MAX_TICKS_PER_FRAME} steps. A window that was
 * dragged, a save game that was written or a pause takes longer than a frame, and a
 * machine that fell behind is better off running a little slower for a moment than
 * freezing the game while thousands of steps are replayed.
 */
public final class TickClock {

    /** Ticks the simulation runs per second, the rate the game is balanced for. */
    public static final int TICKS_PER_SECOND = 20;

    /** Length of one tick in seconds, the delta a block entity is advanced by. */
    public static final float TICK_SECONDS = 1.0f / TICKS_PER_SECOND;

    /** Largest amount of ticks one frame may run. */
    public static final int MAX_TICKS_PER_FRAME = 10;

    /** Length of one tick as a {@code double}, the value the clock counts with. */
    private static final double EXACT_TICK_SECONDS = 1.0 / TICKS_PER_SECOND;

    /**
     * Slack added before a tick is counted, in ticks.
     * <p>
     * One microsecond of a tick: small enough to be invisible, large enough to absorb the
     * rounding error of a sum of frame times.
     */
    private static final double TIMING_EPSILON = 1.0e-6;

    /** Time that was collected but not turned into a tick yet. */
    private double accumulator;

    /** Ticks this clock reported since it was created. */
    private long tickCount;

    /** Ticks the most recent call to {@link #advance(float)} reported. */
    private int lastTicks;

    /**
     * Counts the time of a frame and reports how many ticks it is worth.
     * <p>
     * The time is collected as a {@code double} and a tick that is missed by a rounding
     * error of the last bit is still counted: without that, half a tick added twice would
     * be worth a hair less than a whole one and a machine would lose a tick now and then.
     *
     * @param delta time since the last frame in seconds, ignored when not positive
     * @return amount of ticks to run, between {@code 0} and {@link #MAX_TICKS_PER_FRAME}
     */
    public int advance(float delta) {
        if (delta > 0.0f) {
            accumulator += delta;
        }
        int ticks = (int) Math.floor(accumulator / EXACT_TICK_SECONDS + TIMING_EPSILON);
        if (ticks > MAX_TICKS_PER_FRAME) {
            // The time that could not be replayed is dropped instead of being kept for the
            // next frames, which would only delay them as well.
            ticks = MAX_TICKS_PER_FRAME;
            accumulator = 0.0;
        } else {
            ticks = Math.max(0, ticks);
            accumulator -= ticks * EXACT_TICK_SECONDS;
            if (accumulator < 0.0) {
                accumulator = 0.0;
            }
        }
        tickCount += ticks;
        lastTicks = ticks;
        return ticks;
    }

    /** Ticks that were reported since this clock was created. */
    public long tickCount() {
        return tickCount;
    }

    /** Ticks the most recent call to {@link #advance(float)} reported. */
    public int lastTicks() {
        return lastTicks;
    }

    /** Forgets the time that was collected, used when a world is left. */
    public void reset() {
        accumulator = 0.0f;
        lastTicks = 0;
    }

    @Override
    public String toString() {
        return "TickClock(" + tickCount + " ticks, last " + lastTicks + ")";
    }
}
