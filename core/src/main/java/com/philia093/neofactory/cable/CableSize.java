package com.philia093.neofactory.cable;

/**
 * The sizes a cable is drawn in, from the single line to the widest one.
 * <p>
 * A cable of the industry comes in sizes the way a pipe does - one line through a cell, two, four, eight,
 * twelve or sixteen - and the amperage of the table of the cables is the amperage of the <b>first</b> of
 * them: {@link CableMaterial#amperage()} is what one single cable takes, and a wider one carries the
 * multiple of it, see {@link #factor()}. The loss is a property of the material and never grows with the
 * width, which is why a wide line loses no more over a block than a narrow one does.
 * <p>
 * <b>Only the single line is drawn today.</b> The table of the industry names one cable per material - one
 * ampere, two, four, up to the sixty four of a superconductor - and the sizes above it are the step the
 * blocks of the line grow by, appended to this enum when the art of them arrives. A size is appended and
 * never inserted in the middle, exactly like a material, because the blocks of the line are numbered in
 * the order the sizes are declared, see {@link Cables}.
 */
public enum CableSize {

    /** One line through a cell, the plain cable of the table. */
    SINGLE("1x", "1x", 1);

    private final String fileName;
    private final String displayName;
    private final int factor;

    CableSize(String fileName, String displayName, int factor) {
        this.fileName = fileName;
        this.displayName = displayName;
        this.factor = factor;
    }

    /**
     * Name of this size in lower case, the way it is written in files, such as {@code 1x}.
     *
     * @return the name, the tail of the name of a cable
     */
    public String fileName() {
        return fileName;
    }

    /** Name of this size as a player reads it, such as {@code 1x}. */
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

    /** Amount of this size that fits into one line, which is the factor itself. */
    public int lines() {
        return factor;
    }

    @Override
    public String toString() {
        return fileName;
    }
}
