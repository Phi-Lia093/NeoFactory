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

    /** Amount of lines the crossing of a cell is drawn with, two diagonals. */
    public static final int DIAGONAL_LINES = 2;

    /** Amount of floats {@link #diagonals} writes. */
    public static final int DIAGONAL_FLOATS = DIAGONAL_LINES * 2 * POINT_FLOATS;

    /** Amount of lines the arrow of a one way side is drawn with: a shaft and the two strokes of its head. */
    public static final int ARROW_LINES = 3;

    /** Amount of floats {@link #arrow} writes. */
    public static final int ARROW_FLOATS = ARROW_LINES * 2 * POINT_FLOATS;

    /**
     * How far the arrow of a side reaches out of its cell, in blocks.
     * <p>
     * It is drawn along the direction of the face the cell stands for and not along the face the grid lies
     * on, so a player sees which way the fluid runs: out of the block through that side, or into it.
     */
    private static final float ARROW_LENGTH = 0.34f;

    /** Length of the two strokes of the head of the arrow, in blocks. */
    private static final float ARROW_HEAD = 0.12f;

    /** How far the two strokes of the head spread to either side of the shaft, in blocks. */
    private static final float ARROW_SPREAD = 0.09f;

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
     * Places along a side of a face the lines of the grid are drawn at.
     * <p>
     * The grid is cut one to two to one, so the two cuts of a side lie a quarter and three quarters of the
     * way along it and the middle cell is twice as wide as the cell beside it, see {@link FaceGrid#SHARES}.
     */
    private static final float[] CUTS = { FaceGrid.toOf(0), FaceGrid.fromOf(2) };

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
        for (float cut : CUTS) {
            write(into, index, x, y, z, face, 0.0f, cut);
            write(into, index + POINT_FLOATS, x, y, z, face, 1.0f, cut);
            write(into, index + 2 * POINT_FLOATS, x, y, z, face, cut, 0.0f);
            write(into, index + 3 * POINT_FLOATS, x, y, z, face, cut, 1.0f);
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

        // The cell covers the share of the face its strip takes: the strips of a side are cut one to two to
        // one, so the middle cell is twice as wide as the two beside it, see FaceGrid#SHARES. The top row of
        // the grid is the one with the largest coordinate along the top of the grid.
        int column = FaceGrid.columnOf(cell);
        int row = FaceGrid.SIZE - 1 - FaceGrid.rowOf(cell);
        float lowUp = FaceGrid.fromOf(row);
        float highUp = FaceGrid.toOf(row);
        float lowRight = FaceGrid.fromOf(column);
        float highRight = FaceGrid.toOf(column);

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
     * Writes the two diagonals of one cell of the grid.
     * <p>
     * A cell stands for one side of the block, and the sides that are not joined are crossed out: two lines
     * are drawn from corner to corner of the cell, so a player sees at a glance which of the nine sides are
     * part of the line and which are not, see {@code FaceMark}. The corners are the ones
     * {@link #cellCorners} writes and they are written counter clockwise, so the pairs that are two apart
     * are across from each other: the second and the third change places and the two lines follow.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @param face face the grid is drawn on
     * @param viewerFacing side the player faces, only read for a face that lies flat
     * @param cell number of the cell, {@code 0} to {@link FaceGrid#CELLS} minus one
     * @param into array of at least {@link #DIAGONAL_FLOATS} floats to write into
     */
    public static void diagonals(int x, int y, int z, BlockFace face, BlockFace viewerFacing, int cell,
            float[] into) {
        cellCorners(x, y, z, face, viewerFacing, cell, into);
        for (int at = 0; at < POINT_FLOATS; at++) {
            float swap = into[POINT_FLOATS + at];
            into[POINT_FLOATS + at] = into[2 * POINT_FLOATS + at];
            into[2 * POINT_FLOATS + at] = swap;
        }
    }

    /**
     * Writes the arrow of a side that only lets the fluid run one way.
     * <p>
     * The shaft runs along the direction of the face the cell stands for and the little head spreads in the
     * plane of the face the grid is drawn on, because that is the one plane a player is sure to see. The
     * head sits at the far end of the shaft for a side that gives fluid away and at the face itself for a
     * side that takes it, so the two settings read apart from every angle, see {@code FaceMark} and
     * {@code PipeFlow}.
     *
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @param face face the grid is drawn on
     * @param viewerFacing side the player faces, only read for a face that lies flat
     * @param cell number of the cell, {@code 0} to {@link FaceGrid#CELLS} minus one
     * @param outward {@code true} to point away from the block, {@code false} to point into it
     * @param into array of at least {@link #ARROW_FLOATS} floats to write into
     */
    public static void arrow(int x, int y, int z, BlockFace face, BlockFace viewerFacing, int cell,
            boolean outward, float[] into) {
        BlockFace along = FaceGrid.faceOf(face, viewerFacing, cell);
        BlockFace up = FaceGrid.upOf(face, viewerFacing);
        // The head spreads along the axis of the grid that lies across the shaft: for the cell a player looks
        // at that is the top of the grid, for the cell that stands for the side above the block it is the
        // right one, and one of the two always lies across, because the shaft is a direction of the block.
        boolean spreadUp = dot(up, along) == 0;
        float midUp = (lowUpOf(cell) + highUpOf(cell)) * 0.5f;
        float midRight = (lowRightOf(cell) + highRightOf(cell)) * 0.5f;
        float tail = outward ? 0.0f : ARROW_LENGTH;
        float tip = outward ? ARROW_LENGTH : 0.0f;
        // The head sits a stroke's length from the tip of the shaft, on the side the shaft comes from: at the
        // far end of a side that gives fluid away and at the face itself for a side that takes it.
        float head = tip + (outward ? -ARROW_HEAD : ARROW_HEAD);
        float strokeUp = spreadUp ? ARROW_SPREAD : 0.0f;
        float strokeRight = spreadUp ? 0.0f : ARROW_SPREAD;

        // The shaft, and then the two strokes of the head: every one of them is drawn from where it starts
        // to the tip of the shaft, which is what makes the three lines read as one arrow.
        int line = 2 * POINT_FLOATS;
        writeAlong(into, 0, x, y, z, face, viewerFacing, midUp, midRight, along, tail);
        writeAlong(into, POINT_FLOATS, x, y, z, face, viewerFacing, midUp, midRight, along, tip);
        writeAlong(into, line, x, y, z, face, viewerFacing, midUp + strokeUp, midRight + strokeRight,
                along, head);
        writeAlong(into, line + POINT_FLOATS, x, y, z, face, viewerFacing, midUp, midRight, along, tip);
        writeAlong(into, 2 * line, x, y, z, face, viewerFacing, midUp - strokeUp, midRight - strokeRight,
                along, head);
        writeAlong(into, 2 * line + POINT_FLOATS, x, y, z, face, viewerFacing, midUp, midRight, along, tip);
    }

    /** Where a cell starts along the top of the grid, as a share of the face. */
    private static float lowUpOf(int cell) {
        return FaceGrid.fromOf(FaceGrid.SIZE - 1 - FaceGrid.rowOf(cell));
    }

    /** Where a cell ends along the top of the grid, as a share of the face. */
    private static float highUpOf(int cell) {
        return FaceGrid.toOf(FaceGrid.SIZE - 1 - FaceGrid.rowOf(cell));
    }

    /** Where a cell starts along the right of the grid, as a share of the face. */
    private static float lowRightOf(int cell) {
        return FaceGrid.fromOf(FaceGrid.columnOf(cell));
    }

    /** Where a cell ends along the right of the grid, as a share of the face. */
    private static float highRightOf(int cell) {
        return FaceGrid.toOf(FaceGrid.columnOf(cell));
    }

    /** How much two directions lie along each other, {@code 0} when they are across. */
    private static int dot(BlockFace first, BlockFace second) {
        return first.x() * second.x() + first.y() * second.y() + first.z() * second.z();
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

    /**
     * Writes one point of the face of a cell, moved away from that face along a direction of the block.
     * <p>
     * This is what draws the arrow of a one way side: the point is written the way {@link #write} writes it
     * and is then carried out of the cell along the direction of the face the cell stands for, so the arrow
     * of the side that stands over the block points up out of it.
     *
     * @param into array to write into
     * @param index place of the first float
     * @param x block X coordinate
     * @param y block Y coordinate, the height
     * @param z block Z coordinate
     * @param face face the grid is drawn on
     * @param viewerFacing side the player faces, only read for a face that lies flat
     * @param alongUp how far the point lies along the top of the grid
     * @param alongRight how far the point lies along the right of the grid
     * @param along direction the point is carried along
     * @param distance how far it is carried, in blocks
     */
    private static void writeAlong(float[] into, int index, int x, int y, int z, BlockFace face,
            BlockFace viewerFacing, float alongUp, float alongRight, BlockFace along, float distance) {
        BlockFace up = FaceGrid.upOf(face, viewerFacing);
        BlockFace right = FaceGrid.rightOf(face, viewerFacing);
        BlockFace first = firstOf(face);
        BlockFace second = secondOf(face);
        float a = across(up, right, first, alongUp, alongRight);
        float b = across(up, right, second, alongUp, alongRight);
        into[index] = x + face.cornerX(0) + a * first.x() + b * second.x() + face.x() * OUTSIDE
                + along.x() * distance;
        into[index + 1] = y + face.cornerY(0) + a * first.y() + b * second.y() + face.y() * OUTSIDE
                + along.y() * distance;
        into[index + 2] = z + face.cornerZ(0) + a * first.z() + b * second.z() + face.z() * OUTSIDE
                + along.z() * distance;
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
