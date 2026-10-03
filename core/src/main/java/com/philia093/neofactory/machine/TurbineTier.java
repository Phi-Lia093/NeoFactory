package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;

/**
 * The three turbines of the age of steam, with the numbers every one of them works by.
 * <p>
 * A turbine turns the steam of a boiler into the power of a line of cables, and the tier it is built for is
 * what its numbers come to: <b>the energy it makes a tick is one ampere of its tier</b> - thirty two units of
 * the low voltage, a hundred and twenty eight of the middle one and five hundred and twelve of the high one -
 * and the steam it drinks is what that energy is worth, divided by the efficiency of the machine.
 * <p>
 * <b>One unit of energy is worth two millibuckets of steam</b>, see {@link #STEAM_PER_EU}, and a turbine does
 * not get all of it: the blades of a better machine turn faster and lose more of what the steam carried, so
 * the youngest of the three is the greediest of it. That is what makes a turbine of a later age need more
 * steam for the same energy rather than less - the steam of a boiler is what a workshop of this age is built
 * around, and the work of the turbine is to turn it into the power the machines of the electrical age ask for.
 * <ul>
 *     <li>{@link #LV} drinks seventy six millibuckets a tick of a boiler and hands thirty two units to the
 *         line, which is the ampère every machine of the low voltage runs on;</li>
 *     <li>{@link #MV} takes the steam of four boilers of bronze and hands over an ampère of the middle
 *         voltage;</li>
 *     <li>{@link #HV} is the machine a workshop of some size is built around: an ampère of the high voltage,
 *         which is what the first large lines of the industry carry.</li>
 * </ul>
 * <b>The steam a tick is rounded up</b>, because the energy a tick is a whole unit of it: the machine makes
 * the amperage of its tier exactly when the steam of a tick is worth at least that much, and a fraction of a
 * millibucket is no steam a tank could be asked of. The three numbers that come out of it are pinned by a
 * test, see {@code SteamTurbineTest}: a change of a price here has to be a change of the test as well.
 * <p>
 * The tier also names the casing the machine is built of and the panel its screen is drawn in, so a player
 * reads the age of a turbine off its block and not off its name, see {@link #casing()}.
 */
public enum TurbineTier {

    /** The first turbine, the machine that feeds the first machines of the electrical age. */
    LV(Voltage.LOW, 0.85f, 3072, "machine_lv/machine_lv"),

    /** The turbine of the middle voltage, built of steel with the blades of a faster machine. */
    MV(Voltage.MEDIUM, 0.75f, 10752, "machine_mv/machine_mv"),

    /** The turbine of the high voltage, the machine a workshop of some size is built around. */
    HV(Voltage.HIGH, 0.66f, 41472, "machine_hv/machine_hv");

    /** Steam one unit of energy is worth, in millibuckets, before the efficiency of a machine is counted. */
    public static final float STEAM_PER_EU = 2.0f;

    private final Voltage voltage;
    private final float efficiency;
    private final int capacity;
    private final String casing;

    TurbineTier(Voltage voltage, float efficiency, int capacity, String casing) {
        this.voltage = voltage;
        this.efficiency = efficiency;
        this.capacity = capacity;
        this.casing = casing;
    }

    /** Tier of the line this machine feeds, which is also the ampère it hands over a tick. */
    public Voltage voltage() {
        return voltage;
    }

    /** Share of the steam's worth this machine really turns into energy, from nothing to everything. */
    public float efficiency() {
        return efficiency;
    }

    /** Steam this machine drinks a tick, in millibuckets, rounded up so that a whole ampère arrives. */
    public int steamPerTick() {
        return (int) Math.ceil(voltage.euPerTick() * STEAM_PER_EU / efficiency);
    }

    /**
     * Energy this machine hands to the line a tick: one ampere of its tier.
     *
     * @return the units, always a whole ampère of {@link #voltage()}
     */
    public int euPerTick() {
        return (int) (steamPerTick() / STEAM_PER_EU * efficiency);
    }

    /** Energy the buffer of this machine holds, which is a few seconds of what it makes. */
    public int capacity() {
        return capacity;
    }

    /** Casing this machine is built of, the picture a side that carries a job is drawn with. */
    public String casing() {
        return casing;
    }

    /** Name of this tier, which is the name its block and its item carry. */
    public String fileName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Name of this machine, the way its screen writes it and the way a player reads it in the inventory.
     *
     * @return the name, such as {@code Steam Turbine (LV)}
     */
    public String displayName() {
        return "Steam Turbine (" + name() + ")";
    }

    @Override
    public String toString() {
        return fileName();
    }
}
