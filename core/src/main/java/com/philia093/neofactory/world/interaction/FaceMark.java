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
    NOTHING(0x000000, false, false),

    /** The side is not joined: the cell is crossed out by its two diagonals. */
    CLOSED(0x000000, true, false),

    /** The side only lets fluid enter the block: the cell carries an arrow in the yellow of the fluid. */
    IN(0xFAC73D, false, true),

    /** The side only lets fluid leave the block: the cell carries an arrow in the yellow of the fluid. */
    OUT(0xFAC73D, false, true),

    /** The side takes the power of a line in: the arrow of the plug, in the green of the power. */
    ENERGY_IN(0x59D959, false, true),

    /** The side gives the power a machine made away: the same arrow, pointing out. */
    ENERGY_OUT(0x59D959, false, true),

    /**
     * The side is the vent of a machine of steam, which is drawn in the red of what is spent.
     * <p>
     * A vent carries the very same picture as the side a fluid runs in or out of - a machine blows its steam
     * out of a stub of a pipe like it drinks it - so the colour of the arrow is what tells a player that
     * nothing is caught on the other side of it, see {@link FaceRole#EXHAUST}.
     */
    EXHAUST(0xD94033, false, true);

    private final int colour;
    private final boolean crossed;
    private final boolean arrow;

    FaceMark(int colour, boolean crossed, boolean arrow) {
        this.colour = colour;
        this.crossed = crossed;
        this.arrow = arrow;
    }

    /** {@code true} when this mark crosses the cell out, see {@code FaceOverlay#diagonals}. */
    public boolean isCrossed() {
        return crossed;
    }

    /** {@code true} when this mark carries an arrow, see {@code FaceOverlay#arrow}. */
    public boolean isArrow() {
        return arrow;
    }

    /** {@code true} when the arrow of this mark points away from the block. */
    public boolean pointsOut() {
        return this == OUT || this == ENERGY_OUT || this == EXHAUST;
    }

    /**
     * Colour the mark of this side is drawn in, as the three channels of a colour packed into one number.
     * <p>
     * <b>The colour says what a side moves and not only which way it runs.</b> A side of the fluid system is
     * drawn in the yellow every pipe of the game uses, a plug of the power in green and the vent of a machine
     * of steam in red, see {@code WorldRenderer3D#renderFaceGrid}.
     *
     * @return the colour, {@code 0x000000} for a mark that is not drawn in a colour of its own
     */
    public int colour() {
        return colour;
    }

    @Override
    public String toString() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
