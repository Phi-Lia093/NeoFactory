package com.philia093.neofactory.render;

import com.philia093.neofactory.block.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks which frame of an animated sheet is shown at a tick.
 * <p>
 * The arithmetic of {@link BlockAnimation} is plain, so the run of a sheet can be checked
 * without a window: a frame lasts the ticks it was given, the round starts again at the end of
 * the sheet, and the index never leaves it - not even for a negative count, which the renderer
 * would hand in after a world was rewound.
 */
class BlockAnimationTest {

    /** Four frames of five ticks each, a round of twenty ticks. */
    private static final Block.Animation ANIMATION = new Block.Animation(4, 5);

    @Test
    void aFrameLastsItsTicks() {
        assertEquals(0, BlockAnimation.frameIndex(0, ANIMATION));
        assertEquals(0, BlockAnimation.frameIndex(4, ANIMATION), "the last tick of frame zero");
        assertEquals(1, BlockAnimation.frameIndex(5, ANIMATION), "the first tick of frame one");
        assertEquals(1, BlockAnimation.frameIndex(9, ANIMATION));
        assertEquals(2, BlockAnimation.frameIndex(10, ANIMATION));
        assertEquals(3, BlockAnimation.frameIndex(19, ANIMATION), "the last frame of the round");
    }

    @Test
    void theSheetRunsInCircles() {
        int cycle = ANIMATION.cycleTicks();

        for (long tick = 0; tick < cycle; tick++) {
            assertEquals(BlockAnimation.frameIndex(tick, ANIMATION),
                    BlockAnimation.frameIndex(tick + cycle, ANIMATION),
                    "the round repeats at tick " + tick);
        }
        assertEquals(0, BlockAnimation.frameIndex(cycle, ANIMATION), "the round starts again");
    }

    @Test
    void aSheetOfOneFrameStandsStill() {
        Block.Animation still = new Block.Animation(1, 4);

        for (long tick = 0; tick < 100; tick += 7) {
            assertEquals(0, BlockAnimation.frameIndex(tick, still));
        }
    }

    @Test
    void theIndexNeverLeavesTheSheet() {
        for (long tick = -100; tick < 500; tick++) {
            int frame = BlockAnimation.frameIndex(tick, ANIMATION);
            assertTrue(frame >= 0 && frame < ANIMATION.frames(), "frame " + frame + " at " + tick);
        }
    }

    @Test
    void aBrokenAnimationIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Block.Animation(0, 4));
        assertThrows(IllegalArgumentException.class, () -> new Block.Animation(4, 0));
        assertThrows(IllegalArgumentException.class, () -> new Block.Animation(-1, 4));
        assertThrows(IllegalArgumentException.class, () -> new Block.Animation(4, -1));

        assertEquals(20, ANIMATION.cycleTicks());
        assertTrue(ANIMATION.isAnimated());
        assertEquals(false, new Block.Animation(1, 1).isAnimated());
    }
}
