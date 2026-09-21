package com.philia093.neofactory.machine;

/**
 * What a machine reports as being wrong, which is the icon its screen shows.
 * <p>
 * The kinds name cells of the icon grid of {@code gui/machine_icons.png}, in its fourth
 * column of error pictures. A machine that is fine reports {@link #NONE} and its screen
 * leaves the corner empty.
 */
public enum MachineError {

    /** Nothing is wrong, a screen draws no icon. */
    NONE(-1, -1),

    /** The machine has work to do but no energy to do it with. */
    NO_POWER(3, 0);

    private final int column;
    private final int row;

    MachineError(int column, int row) {
        this.column = column;
        this.row = row;
    }

    /** {@code true} when this is not {@link #NONE}. */
    public boolean isError() {
        return this != NONE;
    }

    /** Column of the cell this error is drawn from. */
    public int column() {
        return column;
    }

    /** Row of the cell this error is drawn from. */
    public int row() {
        return row;
    }

    @Override
    public String toString() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
