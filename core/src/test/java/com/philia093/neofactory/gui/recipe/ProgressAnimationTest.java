package com.philia093.neofactory.gui.recipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the walk of the bar of a recipe: one speed, one length, for every recipe of every machine.
 * <p>
 * The bar of the arrow is an animation and not a measurement, so what has to hold is that it walks at the very
 * same speed whatever is shown and that it starts over at the end instead of running away.
 */
class ProgressAnimationTest {

    @Test
    void theBarWalksAtOneSpeedForFiveSeconds() {
        ProgressAnimation bar = new ProgressAnimation();

        assertEquals(0.0f, bar.share(), 0.0001f, "a bar that has not moved is empty");
        bar.update(ProgressAnimation.CYCLE_SECONDS / 2.0f);
        assertEquals(0.5f, bar.share(), 0.0001f, "half the walk is half the bar");
        bar.update(ProgressAnimation.CYCLE_SECONDS / 4.0f);
        assertEquals(0.75f, bar.share(), 0.0001f, "and the speed is the same all the way");
        assertTrue(bar.share() <= 1.0f);
    }

    @Test
    void theBarStartsOverWhenItHasWalkedTheWholeOfIt() {
        ProgressAnimation bar = new ProgressAnimation();

        bar.update(ProgressAnimation.CYCLE_SECONDS + 1.0f);

        assertEquals(1.0f / ProgressAnimation.CYCLE_SECONDS, bar.share(), 0.0001f,
                "the walk begins again instead of standing at its end");
    }

    @Test
    void aFrameWithoutTimeLeavesTheBarWhereItIs() {
        ProgressAnimation bar = new ProgressAnimation();
        bar.update(1.0f);
        float walked = bar.share();

        bar.update(0.0f);
        bar.update(-1.0f);

        assertEquals(walked, bar.share(), 0.0001f, "a frame without time moves nothing");
        bar.restart();
        assertEquals(0.0f, bar.share(), 0.0001f, "and another recipe starts the bar over");
    }
}
