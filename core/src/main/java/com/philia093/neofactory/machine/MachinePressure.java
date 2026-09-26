package com.philia093.neofactory.machine;

import java.util.Locale;

/**
 * The pressure a machine of the age of steam works at.
 * <p>
 * Every machine of that age exists twice: once as the machine of bronze, which the boiler of bronze feeds, and
 * once as the machine of steel, which is the very same machine driven harder. <b>A machine of pressure reads
 * the recipes of its twin</b> - a grinder grinds what a grinder grinds, an alloy furnace melts what an alloy
 * furnace melts - and it works them at another pace:
 * <ul>
 *     <li>{@link #LOW} is the machine of bronze: it takes the time a recipe names and spends the steam a
 *         recipe names;</li>
 *     <li>{@link #HIGH} is the machine of steel: it finishes a craft in half the time and drinks twice the
 *         steam every tick.</li>
 * </ul>
 * <b>The two of them spend the very same steam on a craft.</b> The time is halved and the rate is doubled, so
 * the millibuckets one craft costs are the millibuckets the recipe names, to the drop: a machine of pressure
 * does not get more work out of a tank of steam, it gets the same work done faster - and it needs a boiler
 * that can feed it that fast, see {@code SteamBoilerMachine#STEEL_STEAM_PER_SECOND}.
 */
public enum MachinePressure {

    /** The machine of bronze, the first of the age of steam. */
    LOW(1.0f, ""),

    /** The machine of steel, which works the same recipes at twice the rate. */
    HIGH(0.5f, "High Pressure ");

    private final float timeShare;
    private final String prefix;

    MachinePressure(float timeShare, String prefix) {
        this.timeShare = timeShare;
        this.prefix = prefix;
    }

    /**
     * Share of the time of a recipe a machine of this pressure takes.
     *
     * @return {@code 1} for bronze, {@code 0.5} for steel
     */
    public float timeShare() {
        return timeShare;
    }

    /**
     * Steam a machine of this pressure drinks, as a share of the steam of a machine of bronze.
     * <p>
     * It is the other half of the rule above: a craft of half the time takes twice the steam every tick, which
     * is what keeps the two machines spending the same millibuckets on the same recipe.
     *
     * @return {@code 1} for bronze, {@code 2} for steel
     */
    public float rateShare() {
        return 1.0f / timeShare;
    }

    /**
     * Title of a machine of this pressure, the name its screen writes.
     *
     * @param name name of the machine, such as {@code Grinder}
     * @return the name for a player, such as {@code High Pressure Grinder}
     */
    public String title(String name) {
        return prefix + name;
    }

    /** {@code true} for the machine of steel, which is built of the metal of its age. */
    public boolean isOfSteel() {
        return this == HIGH;
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
