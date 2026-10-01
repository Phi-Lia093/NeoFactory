package com.philia093.neofactory.cable;

import com.philia093.neofactory.pipe.PipeSize;

/**
 * The sizes a cable is drawn in, from the single line to the widest one.
 * <p>
 * A cable of the industry comes in sizes the way a pipe does - one line through a cell, two, four, eight,
 * twelve or sixteen - and every size names <b>the multiple of the amperage of the material</b> it carries:
 * a copper line of one ampere is a single cable, and a copper cable of the sixteen fold width carries
 * sixteen amperes of the middle voltage, see {@link #factor()} and {@link Cables.Cable#amperage()}. The
 * voltage and the loss are properties of the <b>material</b> and never of the width, so a wide line of a
 * cheap metal loses more than a narrow one of a better metal.
 * <p>
 * <b>The thickness of a size is the thickness of a tube.</b> {@link #tube()} names the size of the fluid
 * system whose geometry a cable of this width is drawn with, which is the whole of the art of a cable: the
 * tube of the family of the kind, see {@link CableKind}, is the model of the line and the colour of the
 * material is multiplied over it. The widest cable of the table is drawn with the widest single tube the
 * pack holds.
 * <p>
 * <b>A size is appended and never inserted in the middle.</b> The blocks of the line are numbered in the
 * order the sizes are declared, so a size that arrives later stands at the end, exactly like a material,
 * see {@link Cables}.
 */
public enum CableSize {

    /** One line through a cell, the plain cable of the table. */
    SINGLE("1x", "1x", 1, PipeSize.TINY),

    /** Two lines through a cell. */
    DOUBLE("2x", "2x", 2, PipeSize.SMALL),

    /** Four lines through a cell. */
    QUADRUPLE("4x", "4x", 4, PipeSize.MEDIUM),

    /** Eight lines through a cell. */
    OCTUPLE("8x", "8x", 8, PipeSize.LARGE),

    /** Twelve lines through a cell. */
    TWELVE("12x", "12x", 12, PipeSize.HUGE),

    /** Sixteen lines through a cell, the widest cable of the table. */
    SIXTEEN("16x", "16x", 16, PipeSize.HUGE);

    private final String fileName;
    private final String displayName;
    private final int factor;
    private final PipeSize tube;

    CableSize(String fileName, String displayName, int factor, PipeSize tube) {
        this.fileName = fileName;
        this.displayName = displayName;
        this.factor = factor;
        this.tube = tube;
    }

    /**
     * Name of this size in lower case, the way it is written in files, such as {@code 4x}.
     *
     * @return the name, a part of the name of a cable
     */
    public String fileName() {
        return fileName;
    }

    /** Name of this size as a player reads it, such as {@code 4x}. */
    public String displayName() {
        return displayName;
    }

    /**
     * Multiple of the amperage of the table one cable of this size takes.
     *
     * @return the multiple, one for the single line
     */
    public int factor() {
        return factor;
    }

    /** Amount of lines this size carries through one cell, which is the factor itself. */
    public int lines() {
        return factor;
    }

    /**
     * Size of the fluid tube a cable of this width is drawn with.
     *
     * @return the size of the tube, the widest one the pack holds for the two widest cables
     */
    public PipeSize tube() {
        return tube;
    }

    @Override
    public String toString() {
        return fileName;
    }
}
