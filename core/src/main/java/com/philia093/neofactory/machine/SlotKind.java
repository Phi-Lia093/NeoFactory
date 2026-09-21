package com.philia093.neofactory.machine;

/**
 * What a slot of a machine is for, which is also the picture it is drawn with.
 * <p>
 * The kinds name cells of the icon grid of {@code gui/machine_icons.png}: the first column
 * holds the item slots, the fifth the tanks of fluid, see
 * {@link com.philia093.neofactory.gui.panel.MachineTextures#icon(int, int)}. A machine
 * names the kind of every slot it declares, so its screen shows what a slot is for without
 * the machine knowing a pixel.
 */
public enum SlotKind {

    /** A plain slot, for an item that is not part of a recipe itself. */
    GENERIC(0, 0),

    /** A slot a smelting recipe takes its items from, the ore and the fuel of a furnace. */
    SMELTING(0, 1),

    /** A slot that holds the energy of a machine, a battery for example. */
    BATTERY(0, 2),

    /** A tank of fluid a recipe drains. */
    FLUID_INPUT(4, 0),

    /** A tank a machine pours the fluid it made into. */
    FLUID_OUTPUT(4, 1);

    private final int column;
    private final int row;

    SlotKind(int column, int row) {
        this.column = column;
        this.row = row;
    }

    /** Column of the cell this kind is drawn from. */
    public int column() {
        return column;
    }

    /** Row of the cell this kind is drawn from. */
    public int row() {
        return row;
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
