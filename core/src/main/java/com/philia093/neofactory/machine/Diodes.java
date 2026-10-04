package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The diodes of the industry: the five widths every tier of the line of the power is built in.
 * <p>
 * <b>A diode is one piece of a line that carries it one way.</b> It is a block a line runs through - what
 * enters it by one side leaves it by the other, and nothing runs back - and it rates the line it stands in
 * the way a cable does: its <b>tier</b> is the highest voltage that may run through it and its <b>width</b>
 * is the current it carries, so a diode of one ampere in a run of a wide cable is a line of one ampere, see
 * {@code Conductor} and {@code energy.LineNode}. It loses nothing at all: it is a superconductor with a door
 * in it, which is why a diode of an earlier age melts where it stands in a line of a later one, exactly like
 * a cable of that age.
 * <p>
 * <b>The narrowest diode of a tier is the machine casing of that tier.</b> One ampere is what a machine of
 * the line carries over the side a player gave to the power of it, so the diode of one ampere is the block a
 * player builds when they want a line to run one way through the very side a machine stands on - it is drawn
 * as a cube of the casing of its tier and is named for it, while every wider diode names its width:
 * {@code LV Machine Casing} and {@code LV Cable Diode 2x} up to {@code 16x}.
 * <p>
 * <b>The order of this table is the order of the ids.</b> The five widths of the low voltage come first, then
 * the five of the middle one and the five of the high one, see {@code Blocks#DIODE_FIRST_ID} and
 * {@code Items#DIODE_FIRST_ID}: an id is permanent - a stored world and a stored inventory spell it out - so
 * a width that is added goes behind the five of every tier and never in the middle of them.
 */
public final class Diodes {

    /**
     * Widths a diode is built in, in the order they are registered, one ampere to sixteen.
     * <p>
     * The five of them are what one side of a machine carries: a machine of the line takes an ampere of its
     * tier, so the diode of one ampere passes what a machine hands over, and every wider one passes what a
     * bundle of that many lines would.
     */
    public static final List<Integer> WIDTHS = List.of(1, 2, 4, 8, 16);

    /**
     * Amount of diodes the game holds.
     * <p>
     * One per width and tier, which is what the blocks, the items and the block entities of the diodes are
     * counted by, see {@code Blocks#NEXT_FREE_ID}.
     */
    public static final int COUNT = WIDTHS.size() * MachineFamilies.TIERS.size();

    private Diodes() {
        // Utility class: never instantiated.
    }

    /** Tiers a diode is built in, in the order they are registered. */
    public static List<Voltage> tiers() {
        return MachineFamilies.TIERS;
    }

    /**
     * Name of a diode, which is its block, its item and its block entity all at once.
     *
     * @param tier tier the diode was built for
     * @param width current it carries, one of {@link #WIDTHS}
     * @return the name, such as {@code machine_casing_lv} or {@code cable_diode_mv_16}
     * @throws IllegalArgumentException when no such diode of the game exists
     */
    public static String nameOf(Voltage tier, int width) {
        require(tier, width);
        return width == WIDTHS.get(0) ? "machine_casing_" + tier.fileName()
                : "cable_diode_" + tier.fileName() + '_' + width;
    }

    /**
     * Title of a diode, the way a player reads it in the inventory.
     *
     * @param tier tier the diode was built for
     * @param width current it carries, one of {@link #WIDTHS}
     * @return the title, such as {@code LV Machine Casing} or {@code MV Cable Diode 16x}
     * @throws IllegalArgumentException when no such diode of the game exists
     */
    public static String titleOf(Voltage tier, int width) {
        require(tier, width);
        String age = tier.fileName().toUpperCase(Locale.ROOT);
        return width == WIDTHS.get(0) ? age + " Machine Casing" : age + " Cable Diode " + width + 'x';
    }

    /**
     * Current a diode of a width carries, in amperes.
     *
     * @param width width of the diode, one of {@link #WIDTHS}
     * @return the amperage
     * @throws IllegalArgumentException when the game holds no diode of that width
     */
    public static int amperageOf(int width) {
        if (!WIDTHS.contains(width)) {
            throw new IllegalArgumentException("A diode of the game carries one, two, four, eight or sixteen"
                    + " amperes, but " + width + " was asked for");
        }
        return width;
    }

    /** Checks that the two name a diode of the game, so a broken one fails where it is asked for. */
    private static void require(Voltage tier, int width) {
        Objects.requireNonNull(tier, "tier");
        if (!MachineFamilies.TIERS.contains(tier)) {
            throw new IllegalArgumentException("No diode is built for the " + tier.displayName()
                    + ": the industry has been drawn in three ages so far, see MachineFamilies#TIERS");
        }
        amperageOf(width);
    }
}
