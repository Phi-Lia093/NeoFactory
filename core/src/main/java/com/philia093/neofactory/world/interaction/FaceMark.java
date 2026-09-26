package com.philia093.neofactory.world.interaction;

/**
 * What one cell of the grid of nine says about the face it stands for.
 * <p>
 * A cell of the grid is a side of a block, see {@link FaceGrid}, and the grid draws what that side does: a
 * side of a pipe that is not joined is crossed out by two diagonals, a side that only lets fluid out or in
 * carries a small arrow, and a side that is joined and lets the fluid run both ways shows the plain cell.
 * The grid is the only place the shape of a line is seen as a whole, so the marks of the nine cells are
 * asked of the block once per frame and handed to the renderer, see {@code GameScreen#updateFaceGrid} and
 * {@code WorldRenderer3D#renderFaceGrid}.
 * <p>
 * <b>A block without marks answers {@link #NOTHING}.</b> A machine that is only turned by a wrench has
 * nothing to say about its sides yet, so the default of {@link FaceOperable#faceMark} leaves the grid plain
 * and the marks are a question a block may answer and not one it has to.
 */
public enum FaceMark {

    /** Nothing is drawn: the side is joined, or the block says nothing about it. */
    NOTHING,

    /** The side is not joined: the cell is crossed out by its two diagonals. */
    CLOSED,

    /** The side only lets fluid enter the block: the cell carries an arrow that points in. */
    IN,

    /** The side only lets fluid leave the block: the cell carries an arrow that points out. */
    OUT;

    /** {@code true} when this mark crosses the cell out, see {@code FaceOverlay#diagonals}. */
    public boolean isCrossed() {
        return this == CLOSED;
    }

    /** {@code true} when this mark carries an arrow, see {@code FaceOverlay#arrow}. */
    public boolean isArrow() {
        return this == IN || this == OUT;
    }

    /** {@code true} when the arrow of this mark points away from the block. */
    public boolean pointsOut() {
        return this == OUT;
    }

    @Override
    public String toString() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
