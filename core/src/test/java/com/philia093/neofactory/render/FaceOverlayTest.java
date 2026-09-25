package com.philia093.neofactory.render;

import com.philia093.neofactory.block.BlockFace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the points the grid of faces is drawn with.
 * <p>
 * The geometry is plain arithmetic on the corners of a block, so it is checked here without a graphics
 * card: the four lines cut the face into thirds, a cell of the grid covers one of those thirds, and every
 * point stands the same hair outside the block so it is never drawn into the face itself.
 */
class FaceOverlayTest {

    /** Distance the grid stands outside a block, the hair the overlay uses. */
    private static final float OUTSIDE = 0.003f;

    /** Tolerance of the arithmetic, which is float. */
    private static final float TOLERANCE = 1.0e-5f;

    @Test
    void theLinesCutAWallIntoThirds() {
        float[] lines = new float[FaceOverlay.LINES_FLOATS];

        FaceOverlay.lines(0, 0, 0, BlockFace.NORTH, lines);

        // The north face is the plane of z = 0 that spans the height and the east, so its first tangent is
        // up and its second one east: a line of the grid is a third of the way along one of them.
        float third = 1.0f / 3;
        float[] expected = {
                third, 0.0f, -OUTSIDE, third, 1.0f, -OUTSIDE,
                0.0f, third, -OUTSIDE, 1.0f, third, -OUTSIDE,
                2 * third, 0.0f, -OUTSIDE, 2 * third, 1.0f, -OUTSIDE,
                0.0f, 2 * third, -OUTSIDE, 1.0f, 2 * third, -OUTSIDE};
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], lines[index], TOLERANCE, "point of the grid, float " + index);
        }
    }

    @Test
    void theLinesFollowTheCellTheyAreDrawnOn() {
        float[] lines = new float[FaceOverlay.LINES_FLOATS];

        FaceOverlay.lines(2, 5, 7, BlockFace.NORTH, lines);

        // Every point of the grid lies on the face of that very cell and a hair in front of it.
        for (int index = 0; index < FaceOverlay.LINES_FLOATS; index += FaceOverlay.POINT_FLOATS) {
            assertTrue(lines[index] >= 2.0f && lines[index] <= 3.0f, "x of the grid");
            assertTrue(lines[index + 1] >= 5.0f && lines[index + 1] <= 6.0f, "y of the grid");
            assertEquals(7.0f - OUTSIDE, lines[index + 2], TOLERANCE, "z of the grid");
        }
    }

    @Test
    void theMiddleCellCoversTheMiddleThirdOfTheFace() {
        float[] corners = new float[FaceOverlay.CELL_FLOATS];

        FaceOverlay.cellCorners(2, 5, 7, BlockFace.NORTH, BlockFace.SOUTH, 4, corners);

        float third = 1.0f / 3;
        float[] expected = {
                2 + 2 * third, 5 + third, 7.0f - OUTSIDE,
                2 + third, 5 + third, 7.0f - OUTSIDE,
                2 + third, 5 + 2 * third, 7.0f - OUTSIDE,
                2 + 2 * third, 5 + 2 * third, 7.0f - OUTSIDE};
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], corners[index], TOLERANCE, "corner, float " + index);
        }
    }

    @Test
    void aCellOfAGridOnAFloorLiesAwayFromThePlayerAndToTheirLeft() {
        float[] corners = new float[FaceOverlay.CELL_FLOATS];

        FaceOverlay.cellCorners(0, 0, 0, BlockFace.TOP, BlockFace.NORTH, 0, corners);

        // The top face is the plane of y = 1 and the grid turns with the player: the first cell of a player
        // who looks north covers the north and the west third of the face.
        float third = 1.0f / 3;
        for (int corner = 0; corner < FaceOverlay.CELL_CORNERS; corner++) {
            int at = corner * FaceOverlay.POINT_FLOATS;
            assertTrue(corners[at] >= 0.0f && corners[at] <= third + TOLERANCE,
                    "x of corner " + corner);
            assertEquals(1.0f + OUTSIDE, corners[at + 1], TOLERANCE, "y of corner " + corner);
            assertTrue(corners[at + 2] >= 0.0f && corners[at + 2] <= third + TOLERANCE,
                    "z of corner " + corner);
        }
    }

    @Test
    void theGridOfACellOfADifferentFaceIsDrawnOnThatFace() {
        float[] lines = new float[FaceOverlay.LINES_FLOATS];

        FaceOverlay.lines(0, 0, 0, BlockFace.TOP, lines);

        // The top face is the plane of y = 1, so every point of its grid lies a hair above it.
        for (int index = 0; index < FaceOverlay.LINES_FLOATS; index += FaceOverlay.POINT_FLOATS) {
            assertEquals(1.0f + OUTSIDE, lines[index + 1], TOLERANCE, "y of the grid");
        }
    }
}
