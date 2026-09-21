package com.philia093.neofactory.render;

import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the projections of {@link BlockIconFactory}.
 * <p>
 * The factory is plain arithmetic on pixels, so a test can hand it a made up tile
 * and read the icon back pixel by pixel: no window, no texture and no asset is
 * needed to prove that a face is where it belongs, that the light falls from the
 * upper left and that a transparent tile stays transparent.
 */
class BlockIconFactoryTest {

    /** Side length the icons of the interface use. */
    private static final int SIZE = Constants.ITEM_ICON_SIZE;

    /**
     * Tile whose red channel names its column and green channel its row.
     * <p>
     * The pattern makes the mapping of a projection readable: an icon that shows the
     * colour of {@code (3, 3)} in its upper left corner took the tile from there.
     */
    private static final BlockIconFactory.IconSource PATTERN =
            (x, y) -> (x * 16 << 24) | (y * 16 << 16) | (0x80 << 8) | 0xFF;

    /** Tile without a single visible pixel. */
    private static final BlockIconFactory.IconSource EMPTY = (x, y) -> 0;

    /** Tile of a uniform grey, the shape a grey scale sheet such as grass has. */
    private static final BlockIconFactory.IconSource GREY =
            (x, y) -> (0xC8 << 24) | (0xC8 << 16) | (0xC8 << 8) | 0xFF;

    @Test
    void anIconHasTheSizeOfASlot() {
        int[] icon = BlockIconFactory.isometric(PATTERN, SIZE);

        assertEquals(SIZE * SIZE, icon.length);
        assertTrue(countVisible(icon) > 0, "the icon shows a cube");
    }

    @Test
    void anIsometricIconShowsACube() {
        int[] icon = BlockIconFactory.isometric(GREY, SIZE);

        assertEquals(0xC8, red(pixel(icon, 8, 4)), "the top face keeps the tile");
        assertTrue(red(pixel(icon, 2, 6)) < 0xC8, "the left face is darker");
        assertTrue(red(pixel(icon, 13, 6)) < 0xC8, "the right face is darker");
        assertEquals(0, alpha(pixel(icon, 0, 0)), "the upper left corner belongs to no face");
        assertEquals(0, alpha(pixel(icon, 15, 15)), "the lower right corner belongs to no face");
    }

    @Test
    void theFaceTowardsTheLightIsTheBrighterOne() {
        int[] icon = BlockIconFactory.isometric(GREY, SIZE);

        assertTrue(red(pixel(icon, 2, 6)) > red(pixel(icon, 13, 6)), "left against right face");
    }

    @Test
    void aFaceFadesTowardsItsLowerEdge() {
        int[] icon = BlockIconFactory.isometric(GREY, SIZE);

        assertTrue(red(pixel(icon, 2, 5)) > red(pixel(icon, 2, 12)), "upper against lower edge");
    }

    @Test
    void aTransparentTileStaysTransparent() {
        int[] icon = BlockIconFactory.isometric(EMPTY, SIZE);

        assertEquals(0, countVisible(icon), "nothing to draw, nothing to see");
    }

    @Test
    void theShadingKeepsAGreyTileGreySoTheTintStillWorks() {
        int[] icon = BlockIconFactory.isometric(GREY, SIZE);

        for (int colour : icon) {
            if (alpha(colour) == 0) {
                continue;
            }
            assertEquals(red(colour), green(colour), "a grey scale sheet stays grey");
            assertEquals(red(colour), blue(colour), "a grey scale sheet stays grey");
        }
    }

    @Test
    void anIsometricIconLeavesNoGapBetweenTheFaces() {
        int[] icon = BlockIconFactory.isometric(GREY, SIZE);

        // The cube covers three quarters of the icon: the diamond of the top face
        // and the two side faces below it. The corners around it stay empty, but a
        // column of the cube may not hold a hole between its upper and lower edge.
        assertTrue(countVisible(icon) >= SIZE * SIZE * 3 / 4, "the cube covers the icon");
        for (int x = 0; x < SIZE; x++) {
            assertColumnIsSolid(icon, x);
        }
    }

    @Test
    void theProjectionWorksForAnotherIconSizeAsWell() {
        int[] icon = BlockIconFactory.isometric(PATTERN, SIZE * 2);

        assertEquals(SIZE * 2 * SIZE * 2, icon.length);
        assertTrue(countVisible(icon) > 0);
    }

    @Test
    void anOutlineSurroundsTheIconWithoutTouchingIt() {
        int[] icon = BlockIconFactory.isometric(GREY, SIZE);
        int[] outlined = BlockIconFactory.outline(icon, SIZE, 1, 0xFFFFFFFF);
        int grown = SIZE + 2;

        assertEquals(grown * grown, outlined.length);

        // Every visible pixel of the icon is copied unchanged, moved one pixel to the
        // inside. A transparent place may be filled by the border, which reaches into the
        // gaps of the shape as well.
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (alpha(icon[y * SIZE + x]) == 0) {
                    continue;
                }
                assertEquals(icon[y * SIZE + x], outlined[(y + 1) * grown + x + 1],
                        "the icon at " + x + ", " + y);
            }
        }

        // The border lies outside of the icon and stays empty where nothing touches it.
        int borderPixels = 0;
        for (int colour : outlined) {
            if (colour == 0xFFFFFFFF) {
                borderPixels++;
            }
        }
        assertTrue(borderPixels > 0, "the icon got an outline of " + borderPixels + " pixels");
        assertEquals(0, outlined[0], "the outer corner belongs to nobody");
    }

    @Test
    void aTransparentIconGetsNoOutline() {
        int[] outlined = BlockIconFactory.outline(BlockIconFactory.isometric(EMPTY, SIZE), SIZE, 1,
                0xFFFFFFFF);

        for (int colour : outlined) {
            assertEquals(0, alpha(colour), "there is nothing to outline");
        }
    }

    /** Amount of pixels of an icon that are not completely transparent. */
    private static int countVisible(int[] icon) {
        int visible = 0;
        for (int colour : icon) {
            if (alpha(colour) != 0) {
                visible++;
            }
        }
        return visible;
    }

    /**
     * Checks that the visible pixels of one column form a single run.
     *
     * @param icon pixels of the icon
     * @param x column to check
     */
    private static void assertColumnIsSolid(int[] icon, int x) {
        int first = -1;
        int last = -1;
        for (int y = 0; y < SIZE; y++) {
            if (alpha(pixel(icon, x, y)) != 0) {
                if (first < 0) {
                    first = y;
                }
                last = y;
            }
        }
        assertTrue(first >= 0, "column " + x + " holds no pixel at all");
        for (int y = first; y <= last; y++) {
            assertTrue(alpha(pixel(icon, x, y)) != 0, "column " + x + " has a hole at row " + y);
        }
    }

    /** Colour of one pixel of an icon, packed as RGBA8888. */
    private static int pixel(int[] icon, int x, int y) {
        return icon[y * SIZE + x];
    }

    private static int red(int colour) {
        return (colour >>> 24) & 0xFF;
    }

    private static int green(int colour) {
        return (colour >>> 16) & 0xFF;
    }

    private static int blue(int colour) {
        return (colour >>> 8) & 0xFF;
    }

    private static int alpha(int colour) {
        return colour & 0xFF;
    }
}
