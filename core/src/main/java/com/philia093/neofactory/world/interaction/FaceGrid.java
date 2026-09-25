package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.BlockFace;

/**
 * The grid of nine cells a player works on a face of a block with.
 * <p>
 * A block is worked on from the side it is looked at, and the faces a player could never reach from
 * there - the one behind and the four around it - are reached through this grid: it is drawn on the face
 * the eyes meet and every one of its nine cells stands for one face of the block. A cell of it is picked
 * with a tool and does what that tool is good for, see {@link FaceOperable}.
 * <p>
 * <b>The middle cell is the face the grid is drawn on</b>, the four cells beside it are the four faces
 * around, and all four corners lead to the face behind - the one a player would have to turn around for.
 * Everything is named the way the player sees it, so the top row of the grid is the top of the face:
 *
 * <pre>
 *     behind    up      behind
 *     left     front     right
 *     behind    down    behind
 * </pre>
 *
 * <b>Which way "up" is depends on the face.</b> On a wall the top of the grid is the sky, so the grid
 * stands still no matter where the player looks. On a floor or a ceiling there is no sky in it, so the
 * top of the grid is the direction the player faces and the whole grid turns with them - which is what
 * makes it usable from above. The class is plain arithmetic on coordinates: it needs no window and no
 * world, see {@code FaceGridTest}.
 */
public final class FaceGrid {

    /** Amount of cells along one side of the grid. */
    public static final int SIZE = 3;

    /** Amount of cells of the grid. */
    public static final int CELLS = SIZE * SIZE;

    private FaceGrid() {
        // Utility class: never instantiated.
    }

    /**
     * The face the top of the grid stands for.
     *
     * @param drawn face the grid is drawn on
     * @param viewerFacing side the player faces, only read for a face that lies flat
     * @return the face of the top row of the grid
     */
    public static BlockFace upOf(BlockFace drawn, BlockFace viewerFacing) {
        if (drawn == null) {
            throw new NullPointerException("drawn");
        }
        if (!drawn.isVertical()) {
            // A wall is looked at from the side, so the top of the grid is the sky for every player.
            // A face that looks up or down instead lies flat and has no sky in it, so the grid turns
            // with the player and its top row is where they look.
            return BlockFace.TOP;
        }
        return viewerFacing == null ? BlockFace.NORTH : viewerFacing;
    }

    /** The face the bottom row of the grid stands for. */
    public static BlockFace downOf(BlockFace drawn, BlockFace viewerFacing) {
        return upOf(drawn, viewerFacing).opposite();
    }

    /**
     * The face the right column of the grid stands for.
     * <p>
     * It is the face that lies to the right of a player who looks at the grid, which is the cross
     * product of the top of the grid and the face it is drawn on.
     */
    public static BlockFace rightOf(BlockFace drawn, BlockFace viewerFacing) {
        BlockFace up = upOf(drawn, viewerFacing);
        int x = up.y() * drawn.z() - up.z() * drawn.y();
        int y = up.z() * drawn.x() - up.x() * drawn.z();
        int z = up.x() * drawn.y() - up.y() * drawn.x();
        return directionOf(x, y, z);
    }

    /** The face the left column of the grid stands for. */
    public static BlockFace leftOf(BlockFace drawn, BlockFace viewerFacing) {
        return rightOf(drawn, viewerFacing).opposite();
    }

    /**
     * The face one cell of the grid stands for.
     *
     * @param drawn face the grid is drawn on, the one the player looks at
     * @param viewerFacing side the player faces, only read for a face that lies flat
     * @param column column of the cell, {@code 0} for the left one
     * @param row row of the cell, {@code 0} for the top one
     * @return the face of the block that cell belongs to
     * @throws IllegalArgumentException when the cell lies outside the grid
     */
    public static BlockFace faceOf(BlockFace drawn, BlockFace viewerFacing, int column, int row) {
        if (column < 0 || column >= SIZE || row < 0 || row >= SIZE) {
            throw new IllegalArgumentException("Cell (" + column + ", " + row + ") lies outside a grid "
                    + "of " + SIZE + " by " + SIZE + " cells");
        }
        if (row == 1) {
            if (column == 0) {
                return leftOf(drawn, viewerFacing);
            }
            return column == 2 ? rightOf(drawn, viewerFacing) : drawn;
        }
        if (column == 1) {
            return row == 0 ? upOf(drawn, viewerFacing) : downOf(drawn, viewerFacing);
        }
        // The four corners all lead to the face behind the grid, the one a player would have to turn
        // around for - which is how a pipe is told to let go of the line that runs behind a block.
        return drawn.opposite();
    }

    /**
     * The face one cell of the grid stands for.
     *
     * @param drawn face the grid is drawn on
     * @param viewerFacing side the player faces, only read for a face that lies flat
     * @param cell number of the cell, {@code 0} to {@link #CELLS} minus one, read row by row
     * @return the face of the block that cell belongs to
     */
    public static BlockFace faceOf(BlockFace drawn, BlockFace viewerFacing, int cell) {
        return faceOf(drawn, viewerFacing, columnOf(cell), rowOf(cell));
    }

    /**
     * The cell of the grid a point of a face falls into.
     * <p>
     * The point is the place the eyes met the face, in the coordinates of one cell - {@code 0} to
     * {@code 1} along every axis, the way the model of a block is written. A point that lies a hair
     * outside is brought back onto the grid, so the arithmetic of a ray never leaves a player without a
     * cell.
     *
     * @param drawn face the grid is drawn on
     * @param viewerFacing side the player faces, only read for a face that lies flat
     * @param hitX X coordinate of the point within the cell
     * @param hitY Y coordinate of the point within the cell
     * @param hitZ Z coordinate of the point within the cell
     * @return the cell, {@code 0} for the upper left one, read row by row
     */
    public static int cellOf(BlockFace drawn, BlockFace viewerFacing, float hitX, float hitY,
            float hitZ) {
        float alongRight = position(hitX, hitY, hitZ, rightOf(drawn, viewerFacing));
        float alongUp = position(hitX, hitY, hitZ, upOf(drawn, viewerFacing));
        // The top row of the grid is the one the largest coordinate of the face belongs to.
        int row = SIZE - 1 - stripOf(alongUp);
        return row * SIZE + stripOf(alongRight);
    }

    /** Column of a cell, counted from the left. */
    public static int columnOf(int cell) {
        return Math.floorMod(cell, SIZE);
    }

    /** Row of a cell, counted from the top. */
    public static int rowOf(int cell) {
        return Math.floorDiv(cell, SIZE);
    }

    /**
     * How far a point lies along a direction of the block, counted from the face behind that direction.
     *
     * @param x X coordinate of the point within the cell
     * @param y Y coordinate of the point within the cell
     * @param z Z coordinate of the point within the cell
     * @param direction direction to measure along, one of the six faces of the block
     * @return the position, {@code 0} at the near side of the block and {@code 1} at the far one
     */
    private static float position(float x, float y, float z, BlockFace direction) {
        if (direction.y() != 0) {
            return direction.y() > 0 ? y : 1.0f - y;
        }
        if (direction.z() != 0) {
            return direction.z() > 0 ? z : 1.0f - z;
        }
        return direction.x() > 0 ? x : 1.0f - x;
    }

    /** Third of a face a position falls into, brought back onto the grid at both ends. */
    private static int stripOf(float position) {
        return Math.min(SIZE - 1, Math.max(0, (int) (position * SIZE)));
    }

    /** Face whose normal is a direction, which the six faces of a block are. */
    public static BlockFace directionOf(int x, int y, int z) {
        for (BlockFace face : BlockFace.ALL) {
            if (face.x() == x && face.y() == y && face.z() == z) {
                return face;
            }
        }
        throw new IllegalArgumentException("(" + x + ", " + y + ", " + z
                + ") is no direction of a block");
    }

    @Override
    public String toString() {
        return "FaceGrid(" + SIZE + " x " + SIZE + ")";
    }
}
