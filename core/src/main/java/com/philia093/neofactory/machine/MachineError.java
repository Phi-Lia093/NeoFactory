package com.philia093.neofactory.machine;

/**
 * What a machine reports as being wrong, which is the icon its screen shows.
 * <p>
 * The kinds name cells of the icon grid of {@code gui/machine_icons.png}, in its fourth
 * column of error pictures. A machine that is fine reports {@link #NONE} and its screen
 * leaves the corner empty.
 * <p>
 * {@link #NO_POWER} is the red alarm of the fourth column - the one that says a machine has work and nothing to
 * run it with - while the two kinds of a machine that runs on steam report the alarm of the seventh column,
 * which is the one a machine of the bronze age shows, see {@link #NO_STEAM}.
 */
public enum MachineError {

    /** Nothing is wrong, a screen draws no icon. */
    NONE(-1, -1),

    /** The machine has work to do but no energy to do it with. */
    NO_POWER(3, 0),

    /**
     * The machine has work to do but no steam to do it with.
     * <p>
     * The picture is the cell of the steam of the sheet, the one the age of bronze draws in the seventh column
     * of the icons - an alarm of its own that says what a machine that runs on steam is missing, see
     * {@link com.philia093.neofactory.gui.panel.MachineTextures#icon(int, int)}. A machine that cannot vent
     * reports the very same picture, see {@link #NO_EXHAUST}: what a player has to look at is the machine, not
     * the wording of the alarm.
     */
    NO_STEAM(6, 0),

    /**
     * The machine has fuel to burn but no water to heat.
     * <p>
     * The picture is the third cell of the column of errors of the sheet. <b>That cell carries no art yet</b>,
     * so a screen reports this alarm as nothing at all and the status line of the boiler - the temperature it
     * stands at - is what tells a player that its tank ran dry. The kind keeps the cell it was given, so a
     * picture that is drawn there later needs no code.
     */
    NO_WATER(3, 2),

    /**
     * A steam machine finished a craft and found its exhaust blocked.
     * <p>
     * The picture is the one of {@link #NO_STEAM}, see there. A steam machine that cannot blow its steam out
     * refuses the next recipe, because the craft it already ran was paid for: what a player has to do is clear
     * the face the exhaust looks through, see {@code SteamMachine}.
     */
    NO_EXHAUST(6, 0);

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
