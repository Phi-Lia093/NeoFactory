package com.philia093.neofactory.machine;

/**
 * How a machine draws the bar that shows how far its work has come.
 * <p>
 * The sheet holds two pairs of arrows, one that looks like a plain process and one that
 * looks like a chemical reaction, and a machine names the pair it wants when it is
 * registered, see {@link MachineScreen#progress()}. Each pair is a bright arrow that grows
 * with the progress and a dark track behind it.
 */
public enum ProgressKind {

    /** The plain pair, used by a machine that works on items. */
    GENERIC(1, 0, 2, 0),

    /** The pair that reads as a reaction, for a machine that mixes things. */
    CHEMICAL(1, 1, 2, 1);

    private final int fullColumn;
    private final int fullRow;
    private final int emptyColumn;
    private final int emptyRow;

    ProgressKind(int fullColumn, int fullRow, int emptyColumn, int emptyRow) {
        this.fullColumn = fullColumn;
        this.fullRow = fullRow;
        this.emptyColumn = emptyColumn;
        this.emptyRow = emptyRow;
    }

    /** Column of the bright arrow, the part that grows with the progress. */
    public int fullColumn() {
        return fullColumn;
    }

    /** Row of the bright arrow. */
    public int fullRow() {
        return fullRow;
    }

    /** Column of the dark track, drawn behind the bright arrow. */
    public int emptyColumn() {
        return emptyColumn;
    }

    /** Row of the dark track. */
    public int emptyRow() {
        return emptyRow;
    }

    @Override
    public String toString() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
