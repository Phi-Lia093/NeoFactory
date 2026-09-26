package com.philia093.neofactory.pipe;

/**
 * The sizes a pipe comes in, from the thin tube to the bundle.
 * <p>
 * The seven sizes are the ones the art of the pack draws, see {@code blocks/pipe_metal}: five single
 * tubes of growing width - tiny, small, medium, large and huge - and two sizes that carry several tubes
 * through one cell, the quadruple with four and the nonuple with nine.
 * <p>
 * <b>A size is a number of sixteenths.</b> {@link #thickness()} is how wide the tube of a size is, in the
 * unit the art of the game is drawn in, so the geometry of a pipe is written down here once and the
 * models are generated from it, see {@code tools/gen_pipe_models.ps1}.
 */
public enum PipeSize {

    /** The narrowest tube. */
    TINY("tiny", "Tiny", 4),

    /** A thin tube. */
    SMALL("small", "Small", 6),

    /** The standard tube of a line. */
    MEDIUM("medium", "Medium", 8),

    /** A wide tube. */
    LARGE("large", "Large", 10),

    /** The widest single tube. */
    HUGE("huge", "Huge", 14),

    /** Four tubes through one cell. */
    QUADRUPLE("quadruple", "Quadruple", 16),

    /** Nine tubes through one cell. */
    NONUPLE("nonuple", "Nonuple", 16);

    /** Amount of sixteenths one block is measured in, the unit of the art of the game. */
    public static final int UNITS = 16;

    private final String displayName;
    private final int thickness;

    PipeSize(String name, String displayName, int thickness) {
        this.displayName = displayName;
        this.thickness = thickness;
    }

    /** Name of this size in lower case, the way it is written in files, such as {@code medium}. */
    public String fileName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Name of this size as a word, used where an item names what it is. */
    public String displayName() {
        return displayName;
    }

    /** Width of the tube of this size, in sixteenths of a block. */
    public int thickness() {
        return thickness;
    }

    /** {@code true} when this size carries several tubes through one cell. */
    public boolean isBundle() {
        return this == QUADRUPLE || this == NONUPLE;
    }

    /** Lower edge of the tube of this size, in sixteenths of a block. */
    public int tubeFrom() {
        return (UNITS - thickness) / 2;
    }

    /** Upper edge of the tube of this size, in sixteenths of a block. */
    public int tubeTo() {
        return (UNITS + thickness) / 2;
    }

    @Override
    public String toString() {
        return fileName();
    }
}
