package com.philia093.neofactory.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks the direction of the wheel.
 * <p>
 * The wheel walks the hotbar and the list of an interface, and the two have one direction between them: a
 * notch rolled <b>forwards</b>, away from the player, walks towards the first slot and towards the
 * beginning of a list. The backend reports that notch as a negative amount, so the sign is turned around
 * once inside {@link InputHandler#scrolled(float, float)} and every caller reads a positive number as a
 * notch forwards. A wheel that walks backwards is the kind of mistake no test of the callers finds, which
 * is why it is pinned down here - the handler is arithmetic and needs no screen.
 */
class InputHandlerTest {

    /** Amount one notch of the wheel is worth, as the callers of the wheel read it. */
    private static final float ONE_NOTCH = 1.0f;

    /** Tolerance of the comparisons, a wheel may report fractional amounts. */
    private static final float EPSILON = 1.0e-6f;

    @Test
    void aNotchRolledForwardsIsReadAsForwards() {
        InputHandler input = new InputHandler();

        input.scrolled(0.0f, -ONE_NOTCH);

        assertEquals(ONE_NOTCH, input.consumeZoomSteps(), EPSILON,
                "a notch forwards has to walk forwards, see GameScreen#applyWheel");
    }

    @Test
    void aNotchRolledBackwardsIsReadAsBackwards() {
        InputHandler input = new InputHandler();

        input.scrolled(0.0f, ONE_NOTCH);

        assertEquals(-ONE_NOTCH, input.consumeZoomSteps(), EPSILON,
                "and a notch backwards has to walk backwards");
    }

    @Test
    void theNotchesAreCollectedUntilTheyAreRead() {
        InputHandler input = new InputHandler();

        input.scrolled(0.0f, -ONE_NOTCH);
        input.scrolled(0.0f, -ONE_NOTCH);
        input.scrolled(0.0f, ONE_NOTCH);

        assertEquals(ONE_NOTCH, input.consumeZoomSteps(), EPSILON, "two forwards and one backwards");
        assertEquals(0.0f, input.consumeZoomSteps(), EPSILON, "a read leaves nothing behind");
    }

    @Test
    void aFrameWithoutANotchMovesNothing() {
        InputHandler input = new InputHandler();

        input.scrolled(0.0f, 0.0f);

        assertEquals(0.0f, input.consumeZoomSteps(), EPSILON,
                "a frame the wheel did not turn in moves nothing");
    }
}
