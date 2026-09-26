package com.philia093.neofactory.render;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.world.interaction.FaceGrid;
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
    void theLinesCutAWallIntoTheSharesOfTheGrid() {
        float[] lines = new float[FaceOverlay.LINES_FLOATS];

        FaceOverlay.lines(0, 0, 0, BlockFace.NORTH, lines);

        // The north face is the plane of z = 0 that spans the height and the east, so its first tangent is
        // up and its second one east: the grid is cut one to two to one, so a line lies a quarter or three
        // quarters of the way along one of them and the middle cell is twice as wide as the two beside it.
        float quarter = FaceGrid.toOf(0);
        float threeQuarters = FaceGrid.fromOf(2);
        float[] expected = {
                quarter, 0.0f, -OUTSIDE, quarter, 1.0f, -OUTSIDE,
                0.0f, quarter, -OUTSIDE, 1.0f, quarter, -OUTSIDE,
                threeQuarters, 0.0f, -OUTSIDE, threeQuarters, 1.0f, -OUTSIDE,
                0.0f, threeQuarters, -OUTSIDE, 1.0f, threeQuarters, -OUTSIDE};
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
    void theMiddleCellCoversTheMiddleHalfOfTheFace() {
        float[] corners = new float[FaceOverlay.CELL_FLOATS];

        FaceOverlay.cellCorners(2, 5, 7, BlockFace.NORTH, BlockFace.SOUTH, 4, corners);

        // The middle cell takes the two middle shares of both sides of the face, so it reaches from a
        // quarter of the way along each to three quarters of it.
        float quarter = FaceGrid.fromOf(1);
        float threeQuarters = FaceGrid.toOf(1);
        float[] expected = {
                2 + threeQuarters, 5 + quarter, 7.0f - OUTSIDE,
                2 + quarter, 5 + quarter, 7.0f - OUTSIDE,
                2 + quarter, 5 + threeQuarters, 7.0f - OUTSIDE,
                2 + threeQuarters, 5 + threeQuarters, 7.0f - OUTSIDE};
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], corners[index], TOLERANCE, "corner, float " + index);
        }
    }

    @Test
    void aCellOfAGridOnAFloorLiesAwayFromThePlayerAndToTheirLeft() {
        float[] corners = new float[FaceOverlay.CELL_FLOATS];

        FaceOverlay.cellCorners(0, 0, 0, BlockFace.TOP, BlockFace.NORTH, 0, corners);

        // The top face is the plane of y = 1 and the grid turns with the player: the first cell of a player
        // who looks north covers the north and the west quarter of the face.
        float quarter = FaceGrid.toOf(0);
        for (int corner = 0; corner < FaceOverlay.CELL_CORNERS; corner++) {
            int at = corner * FaceOverlay.POINT_FLOATS;
            assertTrue(corners[at] >= 0.0f && corners[at] <= quarter + TOLERANCE,
                    "x of corner " + corner);
            assertEquals(1.0f + OUTSIDE, corners[at + 1], TOLERANCE, "y of corner " + corner);
            assertTrue(corners[at + 2] >= 0.0f && corners[at + 2] <= quarter + TOLERANCE,
                    "z of corner " + corner);
        }
    }

    @Test
    void theDiagonalsOfACellCrossItFromCornerToCorner() {
        float[] diagonals = new float[FaceOverlay.DIAGONAL_FLOATS];
        float[] corners = new float[FaceOverlay.CELL_FLOATS];

        FaceOverlay.diagonals(2, 5, 7, BlockFace.NORTH, BlockFace.SOUTH, 4, diagonals);
        FaceOverlay.cellCorners(2, 5, 7, BlockFace.NORTH, BlockFace.SOUTH, 4, corners);

        // The first line runs from the first corner of the cell to the one across from it and the second one
        // from the other pair, which is what crosses the cell out, see FaceMark#CLOSED.
        assertLine(corners, 0, corners, 2, diagonals, 0);
        assertLine(corners, 1, corners, 3, diagonals, 1);
    }

    @Test
    void theArrowOfASidePointsOutOfTheBlockOrIntoIt() {
        float[] outward = new float[FaceOverlay.ARROW_FLOATS];
        float[] inward = new float[FaceOverlay.ARROW_FLOATS];
        float[] corners = new float[FaceOverlay.CELL_FLOATS];
        float[][] both = { outward, inward };

        FaceOverlay.arrow(0, 0, 0, BlockFace.NORTH, BlockFace.SOUTH, 4, true, outward);
        FaceOverlay.arrow(0, 0, 0, BlockFace.NORTH, BlockFace.SOUTH, 4, false, inward);
        FaceOverlay.cellCorners(0, 0, 0, BlockFace.NORTH, BlockFace.SOUTH, 4, corners);

        for (float[] arrow : both) {
            // The middle cell of the grid on a wall stands for the very face the player looks at, so the
            // shaft of its arrow runs along the north of the block and reaches out of the face.
            float shaft = arrow[2];
            float far = arrow[2 + FaceOverlay.POINT_FLOATS];
            assertTrue(Math.abs(far - shaft) > 0.1f, "the shaft of the arrow is a line and not a dot");
            assertEquals(-OUTSIDE, Math.max(shaft, far), TOLERANCE,
                    "the shaft of the arrow starts at the face of the block");
            // The head sits between the face and the far end of the shaft, which is what tells the two ways
            // apart: at the far end of a side that gives fluid away and at the face for a side that takes it.
            float headOne = arrow[2 * FaceOverlay.POINT_FLOATS + 2];
            float headTwo = arrow[2 * (2 * FaceOverlay.POINT_FLOATS) + 2];
            assertEquals(headOne, headTwo, TOLERANCE,
                    "the two strokes of the head start at the same place");
            assertTrue(Math.abs(headOne) > Math.abs(Math.min(Math.abs(shaft), Math.abs(far)))
                            && Math.abs(headOne) < Math.abs(Math.max(Math.abs(shaft), Math.abs(far))),
                    "the head of the arrow lies between the face and the far end of the shaft");
            // Every point of an arrow lies in or in front of the cell it belongs to.
            float leastX = Math.min(corners[0], corners[6]);
            float mostX = Math.max(corners[0], corners[6]);
            float leastY = Math.min(corners[1], corners[7]);
            float mostY = Math.max(corners[1], corners[7]);
            for (int at = 0; at < FaceOverlay.ARROW_FLOATS; at += FaceOverlay.POINT_FLOATS) {
                assertTrue(arrow[at] >= leastX - TOLERANCE && arrow[at] <= mostX + TOLERANCE,
                        "x of the arrow");
                assertTrue(arrow[at + 1] >= leastY - TOLERANCE && arrow[at + 1] <= mostY + TOLERANCE,
                        "y of the arrow");
                assertTrue(arrow[at + 2] <= -OUTSIDE + TOLERANCE, "z of the arrow");
            }
        }
    }

    /** Asserts that one line of an overlay is the line between two points of another. */
    private static void assertLine(float[] from, int fromPoint, float[] to, int toPoint, float[] line,
            int index) {
        for (int axis = 0; axis < FaceOverlay.POINT_FLOATS; axis++) {
            assertEquals(from[fromPoint * FaceOverlay.POINT_FLOATS + axis],
                    line[index * 2 * FaceOverlay.POINT_FLOATS + axis], TOLERANCE,
                    "start of line " + index);
            assertEquals(to[toPoint * FaceOverlay.POINT_FLOATS + axis],
                    line[(index * 2 + 1) * FaceOverlay.POINT_FLOATS + axis], TOLERANCE,
                    "end of line " + index);
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
