package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.BlockFace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks the grid of nine cells that reaches every face of a block.
 * <p>
 * The mapping is plain arithmetic on the six faces of a cube, so it is checked here without a world and
 * without a window: the middle cell is the face the grid is drawn on, the four cells beside it are the
 * faces around, and the four corners lead behind the block - which is how a player tells a pipe to let go
 * of the line that runs behind it.
 */
class FaceGridTest {

    @Test
    void theGridHasNineCells() {
        assertEquals(3, FaceGrid.SIZE);
        assertEquals(9, FaceGrid.CELLS);
    }

    @Test
    void theMiddleCellIsTheFaceTheGridIsDrawnOn() {
        for (BlockFace drawn : BlockFace.ALL) {
            for (BlockFace viewer : BlockFace.SIDES) {
                assertEquals(drawn, FaceGrid.faceOf(drawn, viewer, 4),
                        "the middle cell of the grid on the " + drawn + " face");
            }
        }
    }

    @Test
    void theFourCornersLeadToTheFaceBehindTheGrid() {
        for (BlockFace drawn : BlockFace.ALL) {
            for (int cell : new int[] {0, 2, 6, 8}) {
                assertEquals(drawn.opposite(), FaceGrid.faceOf(drawn, BlockFace.NORTH, cell),
                        "cell " + cell + " of the grid on the " + drawn + " face");
            }
        }
    }

    @Test
    void theCellsBesideTheMiddleAreTheFacesAroundIt() {
        // The grid is drawn on the north face of a block, which a player looks at while facing south.
        BlockFace drawn = BlockFace.NORTH;

        assertEquals(BlockFace.TOP, FaceGrid.faceOf(drawn, BlockFace.SOUTH, 1), "the top row");
        assertEquals(BlockFace.BOTTOM, FaceGrid.faceOf(drawn, BlockFace.SOUTH, 7), "the bottom row");
        assertEquals(BlockFace.EAST, FaceGrid.faceOf(drawn, BlockFace.SOUTH, 3), "the left column");
        assertEquals(BlockFace.WEST, FaceGrid.faceOf(drawn, BlockFace.SOUTH, 5), "the right column");

        assertEquals(BlockFace.TOP, FaceGrid.upOf(drawn, BlockFace.SOUTH));
        assertEquals(BlockFace.BOTTOM, FaceGrid.downOf(drawn, BlockFace.SOUTH));
        assertEquals(BlockFace.EAST, FaceGrid.leftOf(drawn, BlockFace.SOUTH));
        assertEquals(BlockFace.WEST, FaceGrid.rightOf(drawn, BlockFace.SOUTH));
    }

    @Test
    void theTopOfAGridOnAWallIsTheSkyForEveryPlayer() {
        assertEquals(BlockFace.TOP, FaceGrid.upOf(BlockFace.WEST, BlockFace.NORTH));
        assertEquals(BlockFace.TOP, FaceGrid.upOf(BlockFace.EAST, BlockFace.SOUTH));
        // A player who looks east has the south on their right, so the right column of the grid is south.
        assertEquals(BlockFace.SOUTH, FaceGrid.rightOf(BlockFace.WEST, BlockFace.NORTH));
    }

    @Test
    void theTopOfAGridOnAFloorTurnsWithThePlayer() {
        // A grid that lies flat has no sky in it, so its top row is where the player looks.
        assertEquals(BlockFace.NORTH, FaceGrid.upOf(BlockFace.TOP, BlockFace.NORTH));
        assertEquals(BlockFace.SOUTH, FaceGrid.downOf(BlockFace.TOP, BlockFace.NORTH));
        assertEquals(BlockFace.EAST, FaceGrid.rightOf(BlockFace.TOP, BlockFace.NORTH));

        assertEquals(BlockFace.EAST, FaceGrid.upOf(BlockFace.TOP, BlockFace.EAST));
        // A player who looks east has the south on their right, so the right column of the grid is south.
        assertEquals(BlockFace.SOUTH, FaceGrid.rightOf(BlockFace.TOP, BlockFace.EAST));
    }

    @Test
    void thePlaceTheEyesMetAWallNamesACell() {
        // The top left cell of the north face is the one high up and towards the east, which a player who
        // faces south sees on their left.
        assertEquals(0, FaceGrid.cellOf(BlockFace.NORTH, BlockFace.SOUTH, 0.9f, 0.9f, 0.0f));
        assertEquals(2, FaceGrid.cellOf(BlockFace.NORTH, BlockFace.SOUTH, 0.1f, 0.9f, 0.0f));
        assertEquals(6, FaceGrid.cellOf(BlockFace.NORTH, BlockFace.SOUTH, 0.9f, 0.1f, 0.0f));
        assertEquals(8, FaceGrid.cellOf(BlockFace.NORTH, BlockFace.SOUTH, 0.1f, 0.1f, 0.0f));
        assertEquals(4, FaceGrid.cellOf(BlockFace.NORTH, BlockFace.SOUTH, 0.5f, 0.5f, 0.0f));
    }

    @Test
    void thePlaceTheEyesMetAFloorTurnsWithThePlayer() {
        // Looking north, the cell that lies far away and to the left is the first one of the grid.
        assertEquals(0, FaceGrid.cellOf(BlockFace.TOP, BlockFace.NORTH, 0.1f, 1.0f, 0.1f));
        assertEquals(2, FaceGrid.cellOf(BlockFace.TOP, BlockFace.NORTH, 0.9f, 1.0f, 0.1f));
        assertEquals(8, FaceGrid.cellOf(BlockFace.TOP, BlockFace.NORTH, 0.9f, 1.0f, 0.9f));
    }

    @Test
    void aPlaceOutsideTheFaceIsBroughtBackOntoTheGrid() {
        // The arithmetic of a ray may land a hair outside a face, which must never leave a player without
        // a cell of the grid.
        assertEquals(0, FaceGrid.cellOf(BlockFace.NORTH, BlockFace.SOUTH, 1.5f, 1.5f, 0.0f));
        assertEquals(8, FaceGrid.cellOf(BlockFace.NORTH, BlockFace.SOUTH, -0.5f, -0.5f, 0.0f));
    }

    @Test
    void aCellIsReadRowByRow() {
        assertEquals(0, FaceGrid.columnOf(0));
        assertEquals(0, FaceGrid.rowOf(0));
        assertEquals(2, FaceGrid.columnOf(2));
        assertEquals(1, FaceGrid.rowOf(5));
        assertEquals(2, FaceGrid.rowOf(8));
    }

    @Test
    void aCellOutsideTheGridIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> FaceGrid.faceOf(BlockFace.NORTH, null, FaceGrid.SIZE, 0));
        assertThrows(IllegalArgumentException.class,
                () -> FaceGrid.faceOf(BlockFace.NORTH, null, 0, -1));
    }
}
