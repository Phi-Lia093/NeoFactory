package com.philia093.neofactory.machine;

/**
 * What a slot of a machine is for, which is also the picture it is drawn with.
 * <p>
 * The kinds name cells of the icon grid of {@code gui/machine_icons.png}, see
 * {@link com.philia093.neofactory.gui.panel.MachineTextures#icon(int, int)}. A machine
 * names the kind of every slot it declares, so its screen shows what a slot is for without
 * the machine knowing a pixel.
 * <p>
 * <b>The columns of the sheet.</b> The first column holds the item slots of the age of electricity - the
 * plain slot, the flame of a furnace and the slot of a battery - and the fifth holds the tanks of fluid. The
 * sixth column holds the slots of the age of steam, one picture per machine: the ore a grinder is given, the
 * dust it makes, the ingot a hammer beats, the flames of a furnace that burns, and the items an extractor and
 * a compressor are fed. The two ages share their tanks, so the steam of a bronze machine stands in the very
 * cell the water of an electrical one stands in, see {@link #FLUID_INPUT}.
 * <p>
 * {@link #GENERIC} names the plain slot of the panel a machine is drawn in: a machine of the bronze age is
 * drawn with the bronze panel and one of the electrical age with the grey panel, so the plain picture is not
 * a cell of the grid but the slot of the panel itself, see {@link MachineStyle} and
 * {@link com.philia093.neofactory.gui.container.Slot#DEFAULT_ICON}.
 */
public enum SlotKind {

    /** A plain slot, for an item that is not part of a recipe itself. */
    GENERIC(0, 0, -1, -1, 5, 13),

    /** A slot a smelting recipe takes its items from, the ore and the fuel of a furnace. */
    SMELTING(0, 1, 5, 3, 5, 10),

    /** A slot that holds the energy of a machine, a battery for example. */
    BATTERY(0, 2, -1, -1, -1, -1),

    /**
     * A tank of fluid a recipe drains, and how a machine is filled by hand.
     * <p>
     * A tank is not a slot: nothing is ever put into it, a player clicks it with a cell in hand instead, see
     * {@link com.philia093.neofactory.item.CellTransfer}. The cell does not change with the style: the age of
     * steam shows its steam in the tank of the age of electricity.
     */
    FLUID_INPUT(4, 0, -1, -1, -1, -1),

    /** A tank a machine pours the fluid it made into. */
    FLUID_OUTPUT(4, 1, -1, -1, -1, -1),

    /** The ore a grinder is given, the first picture of the steam column of the sheet. */
    GRINDER_INPUT(5, 0, -1, -1, 5, 7),

    /** The dust a grinder makes, the second picture of the steam column. */
    GRINDER_OUTPUT(5, 1, -1, -1, 5, 8),

    /** The ingot a forge hammer beats, the third picture of the steam column. */
    HAMMER_INPUT(5, 2, -1, -1, 5, 9),

    /** The item an extractor squeezes out, the fifth picture of the steam column. */
    EXTRACTOR_INPUT(5, 4, -1, -1, 5, 11),

    /** The item a compressor presses together, the last picture of the steam column. */
    COMPRESSOR_INPUT(5, 5, -1, -1, 5, 12);

    private final int column;
    private final int row;
    private final int bronzeColumn;
    private final int bronzeRow;
    private final int steelColumn;
    private final int steelRow;

    /** Rows the pictures of the age of steel stand below the ones of bronze in the machine column. */
    public static final int STEEL_ROW_SHIFT = 7;

    SlotKind(int column, int row, int bronzeColumn, int bronzeRow, int steelColumn, int steelRow) {
        this.column = column;
        this.row = row;
        this.bronzeColumn = bronzeColumn;
        this.bronzeRow = bronzeRow;
        this.steelColumn = steelColumn;
        this.steelRow = steelRow;
    }

    /**
     * Column of the cell this kind is drawn from in a style.
     * <p>
     * The two ages of steam draw the slots of a machine from the same column of the sheet, one below the
     * other: the pictures of the age of steel stand seven rows below the ones of bronze, and the plain slot a
     * machine of that age hands out for an item that belongs to no recipe is the last of them,
     * {@code (5, 13)}. A kind that names no picture of its own in an age is drawn with the slot of the panel,
     * see {@link com.philia093.neofactory.gui.container.Slot#DEFAULT_ICON}.
     *
     * @param style style the machine is drawn in
     * @return the column of the cell of that style
     */
    public int column(MachineStyle style) {
        if (style == MachineStyle.NORMAL) {
            return column;
        }
        if (style == MachineStyle.STEEL && steelColumn >= 0) {
            return steelColumn;
        }
        return bronzeColumn >= 0 ? bronzeColumn : column;
    }

    /**
     * Row of the cell this kind is drawn from in a style, read the way {@link #column(MachineStyle)} reads its
     * column: the row of bronze for a machine of bronze and the row of steel seven rows below it for a machine
     * that is driven harder.
     *
     * @param style style the machine is drawn in
     * @return the row of the cell of that style
     */
    public int row(MachineStyle style) {
        if (style == MachineStyle.NORMAL) {
            return row;
        }
        if (style == MachineStyle.STEEL && steelColumn >= 0) {
            return steelRow;
        }
        return bronzeColumn >= 0 ? bronzeRow : row;
    }

    /** Column of the cell this kind is drawn from, the one every style borrows it from. */
    public int column() {
        return column;
    }

    /** Row of the cell this kind is drawn from, the one every style borrows it from. */
    public int row() {
        return row;
    }

    /**
     * {@code true} when this kind is one the age of steam brought.
     * <p>
     * Such a kind is drawn from the bronze column of the sheet, so a screen of a machine that declares one
     * is the screen of the bronze age, see {@link MachineStyle}.
     */
    public boolean isBronze() {
        return bronzeColumn >= 0 || column == GRINDER_INPUT.column;
    }

    /** {@code true} when this kind is a tank of fluid and not a slot for items. */
    public boolean isFluid() {
        return this == FLUID_INPUT || this == FLUID_OUTPUT;
    }

    @Override
    public String toString() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
