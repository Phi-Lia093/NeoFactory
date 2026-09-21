package com.philia093.neofactory.render;

import com.philia093.neofactory.block.Block;

/**
 * Picks the frame of an animated block that belongs to a tick.
 * <p>
 * A block with an animation carries a sheet of frames instead of a single picture, see
 * {@link Block.Animation}, and this class answers the only question the renderer has: which
 * cell of that sheet is shown right now. The arithmetic is plain and never touches libGDX, so
 * the run of a sheet can be checked without a window, see {@code BlockAnimationTest}.
 * <p>
 * The same tick count serves every animated block, which is why a lake of water looks like one
 * body and not like a field of pictures that each started at their own moment.
 */
public final class BlockAnimation {

    private BlockAnimation() {
        // Utility class: never instantiated.
    }

    /**
     * Frame of an animation at a tick.
     * <p>
     * The tick may be any number, the frame is always a cell of the sheet: a count before the
     * start of the world wraps around like a count far in the future.
     *
     * @param tickCount tick the world is in, see
     *                  {@link com.philia093.neofactory.world.TickClock#tickCount()}
     * @param animation frames of the sheet and how long one is shown
     * @return index of the frame to draw, between zero and {@code frames - 1}
     */
    public static int frameIndex(long tickCount, Block.Animation animation) {
        long ticks = Math.floorMod(tickCount, (long) animation.cycleTicks());
        return (int) (ticks / animation.frameTicks());
    }
}
