package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what happens to the picture of a fluid cell when a fluid is poured into it.
 * <p>
 * A cell is one grey scale picture and only its window belongs to the fluid, so the whole point of
 * the split is that the steel around the window stays the grey the art pack drew: a cell of water
 * has to look like a cell and not like a blue bucket. The arithmetic is plain pixels, so the rim
 * of the window can be walked over without a window.
 */
class CellIconFactoryTest {

    /** Side of the picture of a cell, the cell size of the game. */
    private static final int SIZE = Constants.ITEM_ICON_SIZE;

    /** A pixel of the steel of the picture, a mid grey. */
    private static final int STEEL = 0x505050FF;

    /** Water, the colour a filled cell has to show. */
    private static final Color WATER = new Color(0.19f, 0.28f, 1.0f, 1.0f);

    @Test
    void theWindowIsTwoByTenInTheMiddleOfThePicture() {
        assertEquals(2, CellIconFactory.WINDOW_WIDTH, "the glass of a cell is narrow");
        assertEquals(10, CellIconFactory.WINDOW_HEIGHT);

        int middle = SIZE / 2;
        assertTrue(CellIconFactory.WINDOW_X <= middle,
                "the window starts left of the middle, at " + CellIconFactory.WINDOW_X);
        assertTrue(CellIconFactory.WINDOW_X + CellIconFactory.WINDOW_WIDTH > middle,
                "and reaches over it");
    }

    @Test
    void onlyTheWindowTakesTheColourOfTheFluid() {
        int[] cell = new int[SIZE * SIZE];
        Arrays.fill(cell, STEEL);

        int[] filled = CellIconFactory.colour(cell, SIZE, WATER);

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int colour = filled[y * SIZE + x];
                if (CellIconFactory.isWindow(x, y)) {
                    assertTrue(blue(colour) > red(colour), "the fluid shows at " + x + ", " + y);
                    assertEquals(0xFF, alpha(colour), "and keeps the opacity of the picture");
                } else {
                    assertEquals(STEEL, colour, "the steel stayed grey at " + x + ", " + y);
                }
            }
        }
    }

    @Test
    void theRimOfTheWindowIsExact() {
        assertTrue(CellIconFactory.isWindow(CellIconFactory.WINDOW_X, CellIconFactory.WINDOW_Y));
        assertFalse(CellIconFactory.isWindow(CellIconFactory.WINDOW_X - 1,
                CellIconFactory.WINDOW_Y), "one pixel to the left is steel");
        assertFalse(CellIconFactory.isWindow(CellIconFactory.WINDOW_X,
                CellIconFactory.WINDOW_Y - 1), "one pixel above is steel");
        assertFalse(CellIconFactory.isWindow(
                CellIconFactory.WINDOW_X + CellIconFactory.WINDOW_WIDTH, CellIconFactory.WINDOW_Y),
                "one pixel to the right is steel");
        assertFalse(CellIconFactory.isWindow(CellIconFactory.WINDOW_X,
                CellIconFactory.WINDOW_Y + CellIconFactory.WINDOW_HEIGHT),
                "one pixel below is steel");
    }

    @Test
    void theWindowKeepsTheShadingOfThePicture() {
        int[] cell = new int[SIZE * SIZE];
        Arrays.fill(cell, STEEL);
        cell[CellIconFactory.WINDOW_Y * SIZE + CellIconFactory.WINDOW_X] = rgba(0xC8);
        cell[(CellIconFactory.WINDOW_Y + 1) * SIZE + CellIconFactory.WINDOW_X] = rgba(0x64);

        int[] filled = CellIconFactory.colour(cell, SIZE,
                new Color(1.0f, 1.0f, 1.0f, 1.0f));

        assertTrue(red(filled[CellIconFactory.WINDOW_Y * SIZE + CellIconFactory.WINDOW_X])
                > red(filled[(CellIconFactory.WINDOW_Y + 1) * SIZE + CellIconFactory.WINDOW_X]),
                "a window drawn with shading inside stays shaded");
    }

    /** Colour of the steel of a cell with a given brightness, packed as RGBA8888. */
    private static int rgba(int brightness) {
        return (brightness << 24) | (brightness << 16) | (brightness << 8) | 0xFF;
    }

    /** Red share of a pixel packed as RGBA8888. */
    private static int red(int colour) {
        return (colour >>> 24) & 0xFF;
    }

    /** Blue share of a pixel packed as RGBA8888. */
    private static int blue(int colour) {
        return (colour >>> 8) & 0xFF;
    }

    /** Opacity of a pixel packed as RGBA8888. */
    private static int alpha(int colour) {
        return colour & 0xFF;
    }
}
