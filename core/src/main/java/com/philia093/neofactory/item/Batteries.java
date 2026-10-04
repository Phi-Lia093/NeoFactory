package com.philia093.neofactory.item;

import com.philia093.neofactory.cable.Voltage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The fifteen batteries of the industry, one for every chemistry and tier.
 * <p>
 * <b>They are written down as one table and everything else is read out of it.</b> The items of them, the
 * ids they take, the pictures that are drawn for them and what a player reads at one are all derived from
 * the rows below, so a battery that is added later is one line here and nothing else, see
 * {@link #all()} and {@link Items#BATTERY_FIRST_ID}.
 * <p>
 * <b>What a battery is built of decides most of it, and its size decides the rest.</b> The chemistry is
 * what a player sees at a cell - the colour painted into its window, whether it takes a charge again and
 * how long its charge lasts - while the tier is how much of it there is: a cell of the middle voltage holds
 * four times what one of the low voltage holds and one of the high voltage four times again, see
 * {@link BatteryChemistry} and {@link #capacityOf(BatteryChemistry, Voltage)}.
 * <p>
 * <b>The size of a cell is read as a word and not as a tier</b> - a small, a medium and a large one - which
 * is what the player sees at the name of an item, see {@link #displayNameOf(Cell)}. The tier is what stands
 * in the name it is stored under.
 */
public final class Batteries {

    /**
     * The tiers a battery of the industry is built for.
     * <p>
     * They are the three tiers the machines of the line come in, and a battery of a tier is charged and
     * drained on a line of that tier only, see {@link Voltage}.
     */
    public static final List<Voltage> TIERS = List.of(Voltage.LOW, Voltage.MEDIUM, Voltage.HIGH);

    /**
     * One battery: a chemistry and the tier it was built for.
     * <p>
     * The record is the whole of what a battery is, because the charge of a stack is not part of it: it
     * belongs to the stack that is filled, see {@link Battery}. Two stacks of one cell are therefore the
     * same battery no matter how much of one is left in them.
     *
     * @param chemistry what is inside the cell
     * @param voltage   tier the cell was built for
     * @param capacity  energy a full cell holds, in units of the game
     */
    public record Cell(BatteryChemistry chemistry, Voltage voltage, int capacity) implements Battery {

        /** Validates the three parts of a battery, so a wrong row of the table is caught while it loads. */
        public Cell {
            Objects.requireNonNull(chemistry, "chemistry");
            Objects.requireNonNull(voltage, "voltage");
            if (capacity <= 0) {
                throw new IllegalArgumentException(
                        "A battery holds a positive amount of energy: " + capacity + " on " + chemistry);
            }
        }

        /** What a player reads at a cell, such as {@code Small Acid Battery}. */
        @Override
        public String toString() {
            return displayNameOf(this);
        }
    }

    /**
     * Every battery of the game, in the order they are built and named.
     * <p>
     * <b>The rows go by tier and then by chemistry</b>, so the cheap acid cells of the low voltage stand
     * together and the lithium cells that a workshop builds its line around stand at the end of the table,
     * see {@link #capacityOf(BatteryChemistry, Voltage)} for the amounts and {@link Items#BATTERY_FIRST_ID}
     * for the numbers the run takes.
     */
    private static final List<Cell> ALL = List.of(
            // The cells of the low voltage, the first line a workshop of the industry is built around.
            new Cell(BatteryChemistry.ACID, Voltage.LOW, 18_000),
            new Cell(BatteryChemistry.MERCURY, Voltage.LOW, 32_000),
            new Cell(BatteryChemistry.SODIUM, Voltage.LOW, 50_000),
            new Cell(BatteryChemistry.CADMIUM, Voltage.LOW, 75_000),
            new Cell(BatteryChemistry.LITHIUM, Voltage.LOW, 100_000),
            // Four times as much of the same five, for the line of the middle voltage.
            new Cell(BatteryChemistry.ACID, Voltage.MEDIUM, 72_000),
            new Cell(BatteryChemistry.MERCURY, Voltage.MEDIUM, 128_000),
            new Cell(BatteryChemistry.SODIUM, Voltage.MEDIUM, 200_000),
            new Cell(BatteryChemistry.CADMIUM, Voltage.MEDIUM, 300_000),
            new Cell(BatteryChemistry.LITHIUM, Voltage.MEDIUM, 400_000),
            // And four times again, where a cell of the high voltage is what a line is kept fed with.
            new Cell(BatteryChemistry.ACID, Voltage.HIGH, 288_000),
            new Cell(BatteryChemistry.MERCURY, Voltage.HIGH, 512_000),
            new Cell(BatteryChemistry.SODIUM, Voltage.HIGH, 800_000),
            new Cell(BatteryChemistry.CADMIUM, Voltage.HIGH, 1_200_000),
            new Cell(BatteryChemistry.LITHIUM, Voltage.HIGH, 1_600_000));

    private Batteries() {
    }

    /** Every battery of the game, in the order they are built and named, see {@link Item#battery()}. */
    public static List<Cell> all() {
        return ALL;
    }

    /**
     * The battery of a chemistry and a tier.
     *
     * @param chemistry what is inside the cell
     * @param voltage   tier the cell was built for
     * @return the cell, which is one of the fifteen of the table
     * @throws IllegalArgumentException when the two do not name a battery of the table
     */
    public static Cell of(BatteryChemistry chemistry, Voltage voltage) {
        for (Cell cell : ALL) {
            if (cell.chemistry() == chemistry && cell.voltage() == voltage) {
                return cell;
            }
        }
        throw new IllegalArgumentException("There is no battery of " + chemistry + " at " + voltage);
    }

    /**
     * The battery an item is.
     *
     * @param item item to read, may be {@code null}
     * @return the battery of the item, or {@code null} for an item that is no battery
     */
    public static Battery of(Item item) {
        return item == null ? null : item.battery();
    }

    /**
     * Energy a cell of a chemistry and a tier holds.
     * <p>
     * <b>A tier is four times the one below it</b>, because that is what the ladder of the voltages does:
     * a line of the middle voltage carries four times what one of the low voltage carries, so a cell that
     * feeds it has to hold four times as much to last the same time. The amounts of the table are what a
     * player is shown in the tooltip of an item, see
     * {@link com.philia093.neofactory.item.ItemTooltip}.
     *
     * @param chemistry what is inside the cell
     * @param voltage   tier the cell was built for
     * @return the capacity in units of the game
     */
    public static int capacityOf(BatteryChemistry chemistry, Voltage voltage) {
        return of(chemistry, voltage).capacity();
    }

    /** The cells of one chemistry, in the order of the tiers. */
    public static List<Cell> of(BatteryChemistry chemistry) {
        Objects.requireNonNull(chemistry, "chemistry");
        List<Cell> cells = new ArrayList<>(TIERS.size());
        for (Cell cell : ALL) {
            if (cell.chemistry() == chemistry) {
                cells.add(cell);
            }
        }
        return cells;
    }

    /** The cells of one tier, in the order of the chemistries. */
    public static List<Cell> of(Voltage voltage) {
        Objects.requireNonNull(voltage, "voltage");
        List<Cell> cells = new ArrayList<>(BatteryChemistry.values().length);
        for (Cell cell : ALL) {
            if (cell.voltage() == voltage) {
                cells.add(cell);
            }
        }
        return cells;
    }

    /**
     * Name an item of a battery is stored under.
     * <p>
     * <b>The name carries the tier and the display name carries the size</b>, the way the machines of the
     * line are named after their family and their tier, see {@link #displayNameOf(Cell)}.
     *
     * @param cell battery to name
     * @return the technical name, such as {@code battery_acid_lv}
     */
    public static String itemNameOf(Cell cell) {
        Objects.requireNonNull(cell, "cell");
        return "battery_" + cell.chemistry().fileName() + '_' + cell.voltage().fileName();
    }

    /**
     * What a player reads at a battery, such as {@code Small Acid Battery}.
     * <p>
     * <b>The word for the size comes first, as it does at the machines of the line</b>, see
     * {@link #sizeOf(Voltage)}.
     *
     * @param cell battery to name
     * @return the display name
     */
    public static String displayNameOf(Cell cell) {
        Objects.requireNonNull(cell, "cell");
        return sizeOf(cell.voltage()) + ' ' + cell.chemistry().displayName() + " Battery";
    }

    /**
     * Name of the picture a battery is drawn from, relative to the asset root and without extension.
     * <p>
     * <b>There are two pictures of a battery and not five</b>, because the window of a cell is painted in
     * the colour of its chemistry while the pack around it is the same steel whatever is inside: one small
     * cell for the low voltage and one large one for the two tiers above it, see {@link #isSmall(Voltage)}
     * and {@code tools/verify/import_batteries.ps1}.
     *
     * @param cell battery to name
     * @return the path of the picture, such as {@code items/battery_acid_small}
     */
    public static String pictureOf(Cell cell) {
        Objects.requireNonNull(cell, "cell");
        return Item.ITEM_FOLDER + "battery_" + cell.chemistry().fileName()
                + (isSmall(cell.voltage()) ? "_small" : "_large");
    }

    /**
     * {@code true} when a battery of a tier is drawn from the small picture of the pack.
     * <p>
     * A cell of the low voltage is the one a workshop is founded on and the one a player carries around, so
     * it is drawn small; the two tiers above it are large, see {@link #pictureOf(Cell)}.
     *
     * @param voltage tier to ask about
     * @return whether the cells of the tier are drawn small
     */
    public static boolean isSmall(Voltage voltage) {
        return Objects.requireNonNull(voltage, "voltage") == Voltage.LOW;
    }

    /**
     * What the size of a battery of a tier is called.
     *
     * @param voltage tier to name
     * @return {@code Small}, {@code Medium} or {@code Large}
     */
    public static String sizeOf(Voltage voltage) {
        return switch (Objects.requireNonNull(voltage, "voltage")) {
            case LOW -> "Small";
            case MEDIUM -> "Medium";
            default -> "Large";
        };
    }
}
