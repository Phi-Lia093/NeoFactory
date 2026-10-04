package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The boxes of cells of the industry: the four sizes every tier of the line is built in.
 * <p>
 * <b>A box of cells is a machine of one tier and one size.</b> The tier is what its cells have to be - a box
 * of the middle voltage takes the cells of the middle voltage and no others - and the size is how many of
 * them stand in it at once: one, four, nine or sixteen, which is also the shape of its panel, see
 * {@link MachineScreen#gridSlots()}. That is twelve boxes, and everything else about one of them follows from
 * the two: the name it is registered under, the title of its screen and of its item, the panel it is drawn
 * in, the casing of its block and what each cell adds to what it may take in and give out, see
 * {@link BatteryBoxMachine} and {@link BatteryBank}.
 * <p>
 * <b>The order of this table is the order of the ids.</b> The four boxes of the low voltage come first, then
 * the four of the middle one and the four of the high one, see {@code Blocks#BATTERY_BOX_FIRST_ID} and
 * {@code Items#BATTERY_BOX_FIRST_ID}: an id is permanent - a stored world and a stored inventory spell it out
 * - so a size that is added goes behind the four of every tier and never in the middle of them.
 */
public final class BatteryBoxes {

    /**
     * Sizes a box of cells is built in, in the order they are registered.
     * <p>
     * The four of them are the four grids the panel of a machine has room for, so the size of a box and the
     * shape of its panel can never disagree, see {@link MachineScreen#GRID_SHAPES}.
     */
    public static final List<Integer> CELLS = MachineScreen.GRID_SHAPES;

    /**
     * Amount of boxes the game holds.
     * <p>
     * One per size and tier, which is what the blocks, the items and the block entities of the boxes are
     * counted by, see {@code Blocks#NEXT_FREE_ID}.
     */
    public static final int COUNT = CELLS.size() * MachineFamilies.TIERS.size();

    private BatteryBoxes() {
        // Utility class: never instantiated.
    }

    /** Tiers a box of cells is built in, in the order they are registered. */
    public static List<Voltage> tiers() {
        return MachineFamilies.TIERS;
    }

    /**
     * Name of a box, which is its block, its item and its block entity all at once.
     *
     * @param tier tier the box was built for
     * @param cells amount of cells the box holds
     * @return the name, such as {@code battery_box_lv_4}
     * @throws IllegalArgumentException when no such box of the game exists
     */
    public static String nameOf(Voltage tier, int cells) {
        require(tier, cells);
        return "battery_box_" + tier.fileName() + '_' + cells;
    }

    /**
     * Title of a box, the way its screen writes it and the way a player reads it in the inventory.
     * <p>
     * What a player reads is the tier in front of the name of the machine, the way it is at every machine of
     * the line, and the size behind it, because the four boxes of a tier are four machines and not one.
     *
     * @param tier tier the box was built for
     * @param cells amount of cells the box holds
     * @return the title, such as {@code LV Battery Box (4 Cells)}
     * @throws IllegalArgumentException when no such box of the game exists
     */
    public static String titleOf(Voltage tier, int cells) {
        require(tier, cells);
        return tier.fileName().toUpperCase(Locale.ROOT) + " Battery Box (" + cells
                + (cells == 1 ? " Cell)" : " Cells)");
    }

    /**
     * The screen of a box, which is a grid of plain slots and nothing else.
     *
     * @param tier tier the box was built for
     * @param cells amount of cells the box holds
     * @return the screen
     * @throws IllegalArgumentException when no such box of the game exists
     */
    public static MachineScreen screenOf(Voltage tier, int cells) {
        return new MachineScreen(titleOf(tier, cells), cells);
    }

    /**
     * The slots of a box, one per cell it holds.
     * <p>
     * Every slot of a box carries the same role - the cell of energy a player puts into it and takes out of
     * it again - so the slots of a box are the shape of its panel and nothing else, see
     * {@link MachineInventory.Role#BATTERY}.
     *
     * @param cells amount of cells the box holds
     * @return the inventory of the box
     * @throws IllegalArgumentException when no such box of the game exists
     */
    public static MachineInventory inventoryOf(int cells) {
        if (!CELLS.contains(cells)) {
            throw new IllegalArgumentException("A box of cells holds " + cells + " cells, but the four boxes "
                    + "of the game hold one, four, nine or sixteen of them, see GRID_SHAPES");
        }
        MachineInventory.Role[] roles = new MachineInventory.Role[cells];
        Arrays.fill(roles, MachineInventory.Role.BATTERY);
        return new MachineInventory(roles);
    }

    /** Checks that the two name a box of the game, so a broken one fails where it is asked for. */
    private static void require(Voltage tier, int cells) {
        Objects.requireNonNull(tier, "tier");
        if (!MachineFamilies.TIERS.contains(tier)) {
            throw new IllegalArgumentException("No box of cells is built for the " + tier.displayName()
                    + ": the industry has been drawn in three ages so far, see MachineFamilies#TIERS");
        }
        if (!CELLS.contains(cells)) {
            throw new IllegalArgumentException("A box of cells holds " + cells
                    + " cells, but the four boxes of the game hold one, four, nine or sixteen of them");
        }
    }
}
