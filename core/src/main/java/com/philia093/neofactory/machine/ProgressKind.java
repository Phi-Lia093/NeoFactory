package com.philia093.neofactory.machine;

/**
 * How a machine draws the bar that shows how far its work has come.
 * <p>
 * The sheet holds a pair of arrows per kind of work, and a machine names the pair it wants when it is
 * registered, see {@link MachineScreen#progress()}. A pair is a bright arrow that grows with the progress and
 * a dark track behind it.
 * <p>
 * <b>The two ages draw with different bars.</b> The machines of the electrical age work on items and mix
 * things - {@link #GENERIC} and {@link #CHEMICAL}, the grey pairs of the sheet - while a machine that runs on
 * steam shows the bar of its own work, see the {@code BRONZE_} kinds. Their pictures live in the eighth and
 * ninth column of the sheet, the dark track first and the bright part next to it.
 * <p>
 * <b>The hammer is drawn on its side.</b> The bar of a forge hammer is a tall one, so its bright part grows
 * upwards instead of to the right: {@link #BRONZE_HAMMER} is {@link #vertical()} and its track is the one
 * heavy picture of the sheet, which is larger than the bright part it belongs to, see
 * {@link com.philia093.neofactory.gui.panel.ArrowElement}.
 */
public enum ProgressKind {

    /**
     * A machine that has no bar at all, which is what a machine that makes power is.
     * <p>
     * A generator works by the tick and has no craft to show: a turbine drinks its steam and hands the power
     * over, so there is nothing a bar could fill towards. <b>A screen that names this kind is the screen of a
     * generator</b>, and the panel of such a machine stands its tanks on the row the bar of a machine stands
     * on, so the place of the bar shows the fluid the machine works on instead of a bar that never fills, see
     * {@code MachineMenu#buildFluidSlots} and {@code MachineGui#drawMachine}.
     */
    NONE(-1, -1, -1, -1, false),

    /** The plain pair, used by a machine that works on items. */
    GENERIC(1, 0, 2, 0, false),

    /** The pair that reads as a reaction, for a machine that mixes things. */
    CHEMICAL(1, 1, 2, 1, false),

    /**
     * The plain pair of the age of steam, used by a machine that burns its work into shape.
     * <p>
     * The bronze boiler fills this bar with the fuel it burns and the furnace of the bronze age with the ore
     * it smelts, so the bar reads the same way the grey one of the furnace of the electrical age does.
     */
    BRONZE(8, 2, 7, 2, false),

    /** The bar of a grinder, which eats ore and turns it into dust. */
    BRONZE_GRINDER(8, 0, 7, 0, false),

    /**
     * The bar of a forge hammer, which beats an ingot into shape.
     * <p>
     * The bar stands on its side: it grows upwards from the bottom of its cell, and its track is the heavy
     * picture of the sheet - the one that is larger than the bright part, see {@link #vertical()}.
     */
    BRONZE_HAMMER(8, 1, 7, 1, true),

    /** The bar of an extractor, which squeezes what it is given. */
    BRONZE_EXTRACTOR(8, 3, 7, 3, false),

    /** The bar of a compressor, which presses what it is given together. */
    BRONZE_COMPRESSOR(8, 4, 7, 4, false);

    private final int fullColumn;
    private final int fullRow;
    private final int emptyColumn;
    private final int emptyRow;
    private final boolean vertical;

    /** Rows the bars of the age of steel stand below the ones of bronze. */
    public static final int STEEL_ROW_SHIFT = 5;

    /** Row the bright part of a tall bar stands in for the age of steel, the plain arrow of that age. */
    public static final int HAMMER_BRIGHT_ROW = 6;

    /** Column the bars of the age of steam start at, the first of the pair. */
    private static final int STEAM_COLUMN = 7;

    ProgressKind(int fullColumn, int fullRow, int emptyColumn, int emptyRow, boolean vertical) {
        this.fullColumn = fullColumn;
        this.fullRow = fullRow;
        this.emptyColumn = emptyColumn;
        this.emptyRow = emptyRow;
        this.vertical = vertical;
    }

    /**
     * {@code true} when a machine of this kind has a bar to draw at all.
     * <p>
     * A machine that names {@link #NONE} has none: the screen it belongs to is the screen of a generator and
     * the place of the bar carries the tank the machine drinks from instead, see {@code MachineMenu}.
     *
     * @return {@code false} for {@link #NONE}, {@code true} for every bar of the game
     */
    public boolean hasBar() {
        return this != NONE;
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

    /** {@code true} for a bar of the age of steam, which stands in the seventh and eighth column. */
    public boolean isSteam() {
        return emptyColumn >= STEAM_COLUMN;
    }

    /**
     * Row of the bright arrow in a style.
     * <p>
     * The two ages of steam draw their bars from the same two columns, one below the other: the bars of the
     * age of steel stand {@link #STEEL_ROW_SHIFT} rows below the ones of bronze.
     *
     * @param style style the machine is drawn in
     * @return the row of the cell of that style
     */
    public int fullRow(MachineStyle style) {
        if (style != MachineStyle.STEEL || !isSteam()) {
            return fullRow;
        }
        // The bright part of the tall bar of a hammer is the plain arrow of its own age: it stands in the same
        // row as the bar of a machine that works on items, and the heavy picture of its own age stands behind
        // it, see MachineTextures#heavyTrackOf.
        return vertical ? HAMMER_BRIGHT_ROW : fullRow + STEEL_ROW_SHIFT;
    }

    /** Row of the dark track in a style. */
    public int emptyRow(MachineStyle style) {
        return style == MachineStyle.STEEL && isSteam() ? emptyRow + STEEL_ROW_SHIFT : emptyRow;
    }

    /**
     * {@code true} when the bar grows upwards instead of to the right.
     * <p>
     * A vertical bar is drawn from the bottom of its cell up, and its track is the heavy picture of the sheet
     * rather than the cell {@link #emptyColumn()} names, because a tall bar of a machine of the bronze age is
     * larger than the bright part inside it.
     */
    public boolean vertical() {
        return vertical;
    }

    @Override
    public String toString() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}

