package com.philia093.neofactory.gui.panel;

import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the geometry of {@link NineSlice}.
 * <p>
 * The class is plain arithmetic on rectangles, so a test can ask it about every
 * size a container may have: the cells have to touch without overlapping, they have
 * to cover the whole target and the frame may never be stretched.
 */
class NineSliceTest {

    @Test
    void thePanelPictureIsSplitAtItsFrame() {
        NineSlice slice = new NineSlice(PanelTextures.PANEL_WIDTH, PanelTextures.PANEL_HEIGHT,
                PanelTextures.BORDER);

        assertEquals(PanelTextures.BORDER, slice.border());
        assertEquals(0, slice.x(0));
        assertEquals(PanelTextures.BORDER, slice.width(0));
        assertEquals(PanelTextures.BORDER, slice.x(1));
        assertEquals(PanelTextures.PANEL_WIDTH - 2 * PanelTextures.BORDER, slice.width(1));
        assertEquals(PanelTextures.PANEL_WIDTH - PanelTextures.BORDER, slice.x(2));
        assertEquals(PanelTextures.BORDER, slice.width(2));

        assertEquals(0, slice.y(0));
        assertEquals(PanelTextures.BORDER, slice.height(0));
        assertEquals(PanelTextures.BORDER, slice.y(1));
        assertEquals(PanelTextures.PANEL_HEIGHT - 2 * PanelTextures.BORDER, slice.height(1));
        assertEquals(PanelTextures.PANEL_HEIGHT - PanelTextures.BORDER, slice.y(2));
        assertEquals(PanelTextures.BORDER, slice.height(2));
    }

    @Test
    void theCellsCoverTheTargetWithoutOverlapping() {
        int[] widths = {1, 2, 3, 4, 5, 7, 10, 18, 100, PanelTextures.PANEL_WIDTH, 300};
        int[] heights = {1, 2, 3, 4, 6, 20, PanelTextures.PANEL_HEIGHT, 400};

        for (int width : widths) {
            for (int height : heights) {
                NineSlice slice = new NineSlice(width, height, PanelTextures.BORDER);
                assertAxis(slice, width, true);
                assertAxis(slice, height, false);
            }
        }
    }

    @Test
    void aTinyTargetKeepsTheFrameAndLosesTheMiddle() {
        NineSlice small = new NineSlice(4, 4, PanelTextures.BORDER);

        // The frame shrinks to half of the target, so nothing is drawn outside of it.
        assertEquals(2, small.border());
        assertEquals(2, small.width(0));
        assertEquals(0, small.width(1));
        assertEquals(2, small.width(2));
        assertFalse(small.isVisible(1, 1), "no room for a middle");
        assertTrue(small.isVisible(0, 0), "the corners are drawn");
    }

    @Test
    void anEmptyTargetDrawsNothing() {
        NineSlice none = new NineSlice(0, 0, PanelTextures.BORDER);

        assertEquals(0, none.border());
        for (int column = 0; column < NineSlice.COUNT; column++) {
            for (int row = 0; row < NineSlice.COUNT; row++) {
                assertFalse(none.isVisible(column, row));
            }
        }
    }

    @Test
    void aCellOutsideTheGridIsRefused() {
        NineSlice slice = new NineSlice(40, 30, PanelTextures.BORDER);

        assertThrows(IllegalArgumentException.class, () -> slice.x(-1));
        assertThrows(IllegalArgumentException.class, () -> slice.width(NineSlice.COUNT));
        assertThrows(IllegalArgumentException.class, () -> slice.y(NineSlice.COUNT));
        assertThrows(IllegalArgumentException.class, () -> slice.height(-1));
    }

    /** Checks that the three cells of one axis touch and add up to the target. */
    private static void assertAxis(NineSlice slice, int size, boolean columns) {
        int total = 0;
        for (int index = 0; index < NineSlice.COUNT; index++) {
            int start = columns ? slice.x(index) : slice.y(index);
            int extent = columns ? slice.width(index) : slice.height(index);

            assertTrue(start >= 0, "a cell starts inside the target");
            assertTrue(start + extent <= size, "a cell ends inside the target, cell " + index
                    + " of " + size);
            if (index > 0) {
                int previous = columns ? slice.x(index - 1) + slice.width(index - 1)
                        : slice.y(index - 1) + slice.height(index - 1);
                assertEquals(previous, start, "the cells touch, cell " + index);
            }
            total += extent;
        }
        assertEquals(size, total, "the cells cover the target of " + size);
    }

    @Test
    void theItemIconFitsItsSlotBevel() {
        assertEquals(Constants.ITEM_ICON_SIZE + 2 * PanelTextures.SLOT_BEVEL,
                PanelTextures.SLOT_SIZE, "the slot picture carries a bevel of one pixel");
    }
}
