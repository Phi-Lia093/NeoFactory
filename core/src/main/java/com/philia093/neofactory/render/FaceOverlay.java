package com.philia093.neofactory.render;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.world.interaction.FaceGrid;

/**
 * The drawing of the grid of faces of a block, worked out without a graphics card.
 * <p>
 * The grid itself is arithmetic, see {@link FaceGrid}; this class turns it into the points a renderer
 * draws: four lines that cut one face of a cell into nine, and the four corners of the cell of the grid a
 * player points at. Both are written into an array the caller owns, because a frame that draws a grid
 * must not fill the heap with points - the same reason the vectors of libGDX are written into instead of
 * replaced.
 * <p>
 * <b>The grid is a hair outside the block.</b> Two cards in the very same place are one card to a
 * graphics card, so a line drawn exactly on the face of a block would be drawn together with it and win
 * or lose by chance; the same hair {@link BreakOverlay} uses is enough.
 * <p>
 * Nothing here knows about the nine faces a cell stands for: that is the business of the interaction,
 * which hands the number of the cell over so the right corner of the grid lights up.
 */
public final class FaceOverlay {

    /** Amount of floats one point takes. */
    public static final int POINT_FLOATS = 3;

    /** Amount of lines one face is cut into, two in every direction. */
    public static final int LINES = 4;

    /** Amount of floats {@link #lines(int, int, int, BlockFace, float[])} writes. */
    public static final int LINES_FLOATS = LINES * 2 * POINT_FLOATS;

    /** Amount of corners one cell of the grid has. */
    public static final int CELL_CORNERS = 4;

    /** Amount of floats {@link #cellCorners} writes. */
    public static final int CELL_FLOATS = CELL_CORNERS * POINT_FLOATS;

    /**
     * Distance the grid stands outside the block, in blocks.
     * <p>
     * Small enough that the lines still read as lying on the face and large enough that they are never
     * drawn into the face itself.
     */
    private static final float OUTSIDE = 0.003f;

    private FaceOverlay() {
        // Utility class: never instantiated.
    }

    /**
     * Writes the four lines that cut one face of a cell into nine.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @param face face the grid is drawn on
     * @param into array of at least {@link #LINES_FLOATS} floats to write into
     */
    public static void lines(int x, int y, int z, BlockFace face, float[] into) {
        int index = 0;
        for (int step = 1; step < FaceGrid.SIZE; step++) {
            float third = (float) step / FaceGrid.SIZE;
            write(into, index, x, y, z, face, 0.0f, third);
            write(into, index + POINT_FLOATS, x, y, z, face, 1.0f, third);
            write(into, index + 2 * POINT_FLOATS, x, y, z, face, third, 0.0f);
            write(into, index + 3 * POINT_FLOATS, x, y, z, face, third, 1.0f);
            index += 4 * POINT_FLOATS;
        }
    }

    /**
     * Writes the four corners of one cell of the grid, counter clockwise as seen by the player.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @param face face the grid is drawn on
     * @param viewerFacing side the player faces, only read for a face that lies flat
     * @param cell number of the cell, {@code 0} to {@link FaceGrid#CELLS} minus one
     * @param into array of at least {@link #CELL_FLOATS} floats to write into
     */
    public static void cellCorners(int x, int y, int z, BlockFace face, BlockFace viewerFacing,
            int cell, float[] into) {
        BlockFace up = FaceGrid.upOf(face, viewerFacing);
        BlockFace right = FaceGrid.rightOf(face, viewerFacing);
        BlockFace first = firstOf(face);
        BlockFace second = secondOf(face);

        // The cell covers a third of the face in both directions: the top row of the grid is the one
        // with the largest coordinate along the top of the grid.
        float lowUp = (float) (FaceGrid.SIZE - 1 - FaceGrid.rowOf(cell)) / FaceGrid.SIZE;
        float highUp = (float) (FaceGrid.SIZE - FaceGrid.rowOf(cell)) / FaceGrid.SIZE;
        float lowRight = (float) FaceGrid.columnOf(cell) / FaceGrid.SIZE;
        float highRight = (float) (FaceGrid.columnOf(cell) + 1) / FaceGrid.SIZE;

        write(into, 0, x, y, z, face, across(up, right, first, lowUp, lowRight),
                across(up, right, second, lowUp, lowRight));
        write(into, POINT_FLOATS, x, y, z, face, across(up, right, first, lowUp, highRight),
                across(up, right, second, lowUp, highRight));
        write(into, 2 * POINT_FLOATS, x, y, z, face, across(up, right, first, highUp, highRight),
                across(up, right, second, highUp, highRight));
        write(into, 3 * POINT_FLOATS, x, y, z, face, across(up, right, first, highUp, lowRight),
                across(up, right, second, highUp, lowRight));
    }

    /**
     * Writes one point of the face of a cell.
     * <p>
     * Corner 0 of the face is the origin and the two tangents of the face span it, so a point is named by
     * how far it lies along each of them - and it is then pushed a hair outside the block.
     */
    private static void write(float[] into, int index, int x, int y, int z, BlockFace face, float a,
            float b) {
        BlockFace first = firstOf(face);
        BlockFace second = secondOf(face);
        into[index] = x + face.cornerX(0) + a * first.x() + b * second.x() + face.x() * OUTSIDE;
        into[index + 1] = y + face.cornerY(0) + a * first.y() + b * second.y() + face.y() * OUTSIDE;
        into[index + 2] = z + face.cornerZ(0) + a * first.z() + b * second.z() + face.z() * OUTSIDE;
    }

    /** Direction the first tangent of a face points in, read from two of its corners. */
    private static BlockFace firstOf(BlockFace face) {
        return FaceGrid.directionOf(face.cornerX(1) - face.cornerX(0),
                face.cornerY(1) - face.cornerY(0), face.cornerZ(1) - face.cornerZ(0));
    }

    /** Direction the second tangent of a face points in, read from two of its corners. */
    private static BlockFace secondOf(BlockFace face) {
        return FaceGrid.directionOf(face.cornerX(3) - face.cornerX(0),
                face.cornerY(3) - face.cornerY(0), face.cornerZ(3) - face.cornerZ(0));
    }

    /**
     * How far a corner of the grid lies along one tangent of the face.
     * <p>
     * The grid is named by its own two directions - the top of the grid and its right column - while the
     * points are read along the tangents of the face. Both pairs span the face, so exactly one direction
     * of the grid is parallel to one tangent: it contributes, a direction that runs against the tangent
     * counts from the other end of the face, and a direction that is neither contributes nothing.
     */
    private static float across(BlockFace up, BlockFace right, BlockFace tangent, float alongUp,
            float alongRight) {
        return contribution(up, tangent, alongUp) + contribution(right, tangent, alongRight);
    }

    /** How far one direction of the grid lies along one tangent of the face. */
    private static float contribution(BlockFace direction, BlockFace tangent, float along) {
        int dot = direction.x() * tangent.x() + direction.y() * tangent.y()
                + direction.z() * tangent.z();
        if (dot > 0) {
            return along;
        }
        return dot < 0 ? 1.0f - along : 0.0f;
    }

    @Override
    public String toString() {
        return "FaceOverlay(" + LINES + " lines, " + CELL_CORNERS + " corners)";
    }
}
