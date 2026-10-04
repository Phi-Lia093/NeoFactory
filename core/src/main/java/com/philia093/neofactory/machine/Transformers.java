package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The transformers of the industry: two tiers of the line in three sizes each.
 * <p>
 * <b>A transformer joins two ages of the line and hands the energy of one to the other.</b> One side of it
 * carries the <b>high</b> voltage and the five others the <b>low</b> one, and what it does a tick is what a
 * line of the high side brings a tick: <b>one ampere of the high voltage is four amperes of the low one</b>,
 * because every tier of the line is four times the one below it, see {@link Voltage} - the energy is the
 * energy and the current is what changes. The three sizes are how much of it a transformer is built for:
 * one, four or sixteen amperes of the high side, which are four, sixteen and sixty four of the low one.
 * <p>
 * <b>The tier of a transformer is the low side of it.</b> The transformer of the low voltage joins the low
 * voltage and the middle one, and the one of the middle voltage joins the middle and the high one; there is
 * no transformer of the high voltage yet, because the age above it has not been drawn, see
 * {@link MachineFamilies#TIERS}. A player reads which one stands there off its casing and off the colours of
 * its two terminals, see {@code MachineTerminals}.
 * <p>
 * <b>The order of this table is the order of the ids.</b> The three sizes of the low transformer come first
 * and the three of the middle one behind them, see {@code Blocks#TRANSFORMER_FIRST_ID}.
 */
public final class Transformers {

    /** Tiers of the low side a transformer is built in, in the order they are registered. */
    public static final List<Voltage> TIERS = List.of(Voltage.LOW, Voltage.MEDIUM);

    /**
     * Sizes a transformer is built in: the amperes its high side carries, in the order they are registered.
     */
    public static final List<Integer> SIZES = List.of(1, 4, 16);

    /**
     * Amount of transformers the game holds.
     * <p>
     * Two tiers of the line and three sizes of each, which is what the blocks, the items and the block
     * entities of the transformers are counted by, see {@code Blocks#NEXT_FREE_ID}.
     */
    public static final int COUNT = TIERS.size() * SIZES.size();

    /** Amperes the low side of a transformer carries for every ampere of its high side. */
    public static final int STEP = 4;

    private Transformers() {
        // Utility class: never instantiated.
    }

    /**
     * Tier the high side of a transformer of a tier carries, which is the age of the line above it.
     *
     * @param tier tier of the low side of the transformer
     * @return the tier of its high side
     * @throws IllegalArgumentException when no transformer of the game is built for that tier
     */
    public static Voltage highOf(Voltage tier) {
        requireTier(tier);
        return MachineFamilies.TIERS.get(MachineFamilies.TIERS.indexOf(tier) + 1);
    }

    /**
     * Amperes the low side of a transformer of a size carries.
     *
     * @param size amperes of the high side, one of {@link #SIZES}
     * @return the amperage of the low side, four times that
     * @throws IllegalArgumentException when the game holds no transformer of that size
     */
    public static int lowAmperageOf(int size) {
        return STEP * amperageOf(size);
    }

    /**
     * Amperes the high side of a transformer of a size carries.
     *
     * @param size size of the transformer, one of {@link #SIZES}
     * @return the amperage
     * @throws IllegalArgumentException when the game holds no transformer of that size
     */
    public static int amperageOf(int size) {
        if (!SIZES.contains(size)) {
            throw new IllegalArgumentException("A transformer of the game is built for one, four or sixteen"
                    + " amperes of the high voltage, but " + size + " was asked for");
        }
        return size;
    }

    /**
     * Name of a transformer, which is its block, its item and its block entity all at once.
     *
     * @param tier tier of the low side of the transformer
     * @param size size of the transformer, one of {@link #SIZES}
     * @return the name, such as {@code transformer_lv} or {@code transformer_mv_16}
     * @throws IllegalArgumentException when no such transformer of the game exists
     */
    public static String nameOf(Voltage tier, int size) {
        require(tier, size);
        String name = "transformer_" + tier.fileName();
        return size == SIZES.get(0) ? name : name + '_' + size;
    }

    /**
     * Title of a transformer, the way a player reads it in the inventory.
     * <p>
     * What a player reads is the age of the low side in front of the name of the machine and the size of the
     * transformer inside it - {@code LV 4x Transformer} - the way a box of cells names its own size.
     *
     * @param tier tier of the low side of the transformer
     * @param size size of the transformer, one of {@link #SIZES}
     * @return the title, such as {@code LV Transformer} or {@code MV 16x Transformer}
     * @throws IllegalArgumentException when no such transformer of the game exists
     */
    public static String titleOf(Voltage tier, int size) {
        require(tier, size);
        String age = tier.fileName().toUpperCase(Locale.ROOT);
        return size == SIZES.get(0) ? age + " Transformer" : age + ' ' + size + "x Transformer";
    }

    /** Checks that the two name a transformer of the game, so a broken one fails where it is asked for. */
    private static void require(Voltage tier, int size) {
        requireTier(tier);
        amperageOf(size);
    }

    /** Checks that a tier is one a transformer of the game is built for. */
    private static void requireTier(Voltage tier) {
        Objects.requireNonNull(tier, "tier");
        if (!TIERS.contains(tier)) {
            throw new IllegalArgumentException("No transformer of the game is built for the "
                    + tier.displayName() + ": the age above the high voltage has not been drawn, see"
                    + " MachineFamilies#TIERS");
        }
    }
}
