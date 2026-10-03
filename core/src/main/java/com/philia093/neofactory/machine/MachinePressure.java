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
    LOW(1.0f, "", "bronze_casing/bronze_casing_side"),

    /** The machine of steel, which works the same recipes at twice the rate. */
    HIGH(0.5f, "High Pressure ", "steel_casing/steel_casing_side");

    private final float timeShare;
    private final String prefix;
    private final String casing;

    MachinePressure(float timeShare, String prefix, String casing) {
        this.timeShare = timeShare;
        this.prefix = prefix;
        this.casing = casing;
    }

    /**
     * Casing a machine of this pressure is built of.
     * <p>
     * A side of a machine that carries a job shows the casing of the machine with the overlay of that job over
     * it, and the casing of a machine of steel is the steel one: a pipe that stands at the side of a grinder of
     * steel meets a machine of steel and not the bronze of its smaller twin, see {@code Machine#casing}.
     *
     * @return the name of the picture of that casing, relative to {@code blocks/}
     */
    public String casing() {
        return casing;
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

    /**
     * Style of panel a machine of this pressure is drawn in.
     * <p>
     * The two pressures are two ages: a machine of bronze is drawn with the bronze panel of the sheet and a
     * machine driven harder with the dark panel of the age of steel, which carries the very same layout one
     * panel lower, see {@link MachineStyle}.
     *
     * @return the style the screen of a machine of this pressure uses
     */
    public MachineStyle style() {
        return isOfSteel() ? MachineStyle.STEEL : MachineStyle.BRONZE;
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
