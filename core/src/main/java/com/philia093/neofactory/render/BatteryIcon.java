package com.philia093.neofactory.render;

import com.philia093.neofactory.item.Battery;
import com.philia093.neofactory.item.ItemStack;

/**
 * Picks the frame of a battery that shows how much of it is left.
 * <p>
 * <b>The picture of a battery is a strip and not one tile.</b> The game draws a cell from as many frames as
 * the window of its picture has rows, the first one a cell that is full and the last one a cell that is
 * spent, and the frames are stacked from the full cell downwards, see
 * {@code tools/verify/import_batteries.ps1}. Which of them a stack is drawn with is therefore read out of
 * its charge and out of nothing else, which is the whole reason a battery carries its charge in its own
 * stack instead of a field of a machine, see {@link Battery}.
 * <p>
 * <b>An empty cell has a frame of its own.</b> A window of six rows is drawn with seven frames, so a cell
 * that is spent keeps an empty window and does not show a sliver of colour that reads as a charge it does
 * not have, and the picture of a stack that was drained to the last unit is a picture no other amount of
 * charge produces.
 * <p>
 * The arithmetic is plain and never touches libGDX, so the frame of a stack can be checked without a
 * window, see {@code BatteryIconTest}.
 */
public final class BatteryIcon {

    private BatteryIcon() {
        // Utility class: never instantiated.
    }

    /**
     * Frame of the strip a stack of a battery is drawn with.
     * <p>
     * The charge of the stack is what decides it: a cell that holds everything it was built for is the
     * first frame of its strip and a cell that is spent the last one, and everything between them falls into
     * the frame that lies closest to the amount that is left. A cell that holds no energy at all or a strip
     * of a single frame is frame zero, which is what an item is drawn with where no stack is at hand.
     *
     * @param battery the cell the stack is of, may be {@code null}
     * @param stack   stack to draw, may be {@code null} or empty
     * @param frames  amount of frames the picture of the cell is stacked of
     * @return the frame, between {@code 0} and {@code frames - 1}
     */
    public static int frameOf(Battery battery, ItemStack stack, int frames) {
        if (battery == null || frames <= 1 || stack == null || stack.isEmpty()) {
            return 0;
        }
        float left = (float) battery.chargeOf(stack) / battery.capacity();
        return Math.round((1.0f - left) * (frames - 1));
    }
}
