package com.philia093.neofactory.gui.recipe;

/**
 * How far the bar of a recipe stands, which walks from nothing to the whole of it in a fixed time.
 * <p>
 * A screen of recipes shows a recipe and not a machine that is really working, so the bar of the arrow is an
 * animation and not a measurement: it walks at a constant speed and starts over when it reaches the end, once
 * every {@link #CYCLE_SECONDS} seconds, whatever the recipe is and however long it really takes. That is what
 * tells a player at a glance that the arrow <b>is</b> the work of the machine, and one length of the walk for
 * every recipe keeps the screen quiet instead of turning a comparison of two recipes into a race of bars.
 */
public final class ProgressAnimation {

    /** Seconds one walk of the bar takes, the same for every recipe of a machine. */
    public static final float CYCLE_SECONDS = 5.0f;

    /** Seconds the bar has walked since it started. */
    private float elapsed;

    /**
     * Lets the bar walk on.
     *
     * @param delta seconds since the last frame, ignored when not positive
     */
    public void update(float delta) {
        if (delta > 0.0f) {
            elapsed += delta;
        }
    }

    /**
     * Share of the bar that stands.
     *
     * @return the share, {@code 0} at the start of a walk and just below {@code 1} before it starts over
     */
    public float share() {
        return elapsed % CYCLE_SECONDS / CYCLE_SECONDS;
    }

    /** Starts the walk over, called when another recipe is shown. */
    public void restart() {
        elapsed = 0.0f;
    }

    @Override
    public String toString() {
        return "ProgressAnimation(" + Math.round(share() * 100.0f) + "% of " + CYCLE_SECONDS + "s)";
    }
}
