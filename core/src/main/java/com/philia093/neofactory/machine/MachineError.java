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
    NO_POWER(3, 0),

    /**
     * The machine has work to do but no steam to do it with.
     * <p>
     * The picture is the second cell of the column of errors of the sheet, see
     * {@link com.philia093.neofactory.gui.panel.MachineTextures#icon(int, int)}.
     */
    NO_STEAM(3, 1),

    /**
     * The machine has fuel to burn but no water to heat.
     * <p>
     * The picture is the third cell of the column of errors of the sheet, the one that reads as a flask -
     * which is what a boiler that ran dry has to say.
     */
    NO_WATER(3, 2),

    /**
     * A steam machine finished a craft and found its exhaust blocked.
     * <p>
     * The picture is the fourth cell of the column of errors of the sheet. A steam machine that cannot blow
     * its steam out refuses the next recipe, because the craft it already ran was paid for: what a player
     * has to do is clear the face the exhaust looks through, see {@code SteamMachine}.
     */
    NO_EXHAUST(3, 3);

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
