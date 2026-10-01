package com.philia093.neofactory.cable;

/**
 * The ladder of the voltages of the industry, from the first line a workshop builds to the largest one
 * the game names.
 * <p>
 * A voltage is a property of the <b>material</b> of a cable and never of the cable itself: copper is a
 * cable of medium voltage, silver one of high voltage, and a machine that asks for more than the material
 * of its line may carry is a machine the line cannot feed, see {@link CableMaterial}.
 * <p>
 * <b>The order of the ladder is the order the enum declares.</b> {@link #isAtLeast(Voltage)} is a
 * comparison of that order, so a tier is never compared by its number of units: a later tier of the
 * industry always stands behind an earlier one, and the constant that is written down last is the top
 * of the ladder.
 * <p>
 * <b>Every tier is four times the one before it.</b> The first line of the workshop is the ultra low
 * voltage of eight units a tick, the line above it is thirty two, and every step of four carries the
 * name of the age: low, medium, high, extreme, insane, ludicrous, the zero point module, ultimate,
 * ultra high, ultra excessive, ultra immense and ultra massive. The names are the ones the industry
 * uses, and the number behind them is the amount of energy one tick of a line of that tier carries.
 */
public enum Voltage {

    /** The line of the first workshop, eight units a tick. */
    ULTRA_LOW("ulv", "Ultra Low Voltage", 8),

    /** The line of the age of steam, thirty two units a tick. */
    LOW("lv", "Low Voltage", 32),

    /** The line of the first machines that work on items, a hundred and twenty eight units a tick. */
    MEDIUM("mv", "Medium Voltage", 128),

    /** The line a workshop of some size runs on, five hundred and twelve units a tick. */
    HIGH("hv", "High Voltage", 512),

    /** The line of the machines that press and melt in volume, two thousand and forty eight a tick. */
    EXTREME("ev", "Extreme Voltage", 2048),

    /** The line of the age of the larger industry, eight thousand one hundred and ninety two a tick. */
    INSANE("iv", "Insane Voltage", 8192),

    /** The line of the alloys that carry the most heat, thirty two thousand seven hundred and sixty eight. */
    LUDICROUS("luv", "Ludicrous Voltage", 32768),

    /** The line named after the zero point module, a hundred and thirty one thousand and seventy two. */
    ZERO_POINT_MODULE("zpm", "Zero Point Module", 131072),

    /** The line of the ultimate voltage, five hundred and twenty four thousand two hundred and eighty eight. */
    ULTIMATE("uv", "Ultimate Voltage", 524288),

    /** The line of the ultra high voltage, two million and ninety seven thousand one hundred and fifty two. */
    ULTRA_HIGH("uhv", "Ultra High Voltage", 2097152),

    /** The line of the ultra excessive voltage, eight million three hundred and eighty eight thousand six hundred and eight. */
    ULTRA_EXCESSIVE("uev", "Ultra Excessive Voltage", 8388608),

    /** The line of the ultra immense voltage, thirty three million five hundred and fifty four thousand four hundred and thirty two. */
    ULTRA_IMMENSE("uiv", "Ultra Immense Voltage", 33554432),

    /** The line of the ultra massive voltage, a hundred and thirty four million two hundred and seventeen thousand seven hundred and twenty eight. */
    ULTRA_MASSIVE("umv", "Ultra Massive Voltage", 134217728);

    private final String fileName;
    private final String displayName;
    private final int euPerTick;

    Voltage(String fileName, String displayName, int euPerTick) {
        this.fileName = fileName;
        this.displayName = displayName;
        this.euPerTick = euPerTick;
    }

    /** Name of this tier in lower case, the way it is written in files and in the name of an item. */
    public String fileName() {
        return fileName;
    }

    /** Name of this tier as a word, used where a tooltip names the line. */
    public String displayName() {
        return displayName;
    }

    /**
     * Energy a line of this tier carries a tick.
     *
     * @return the voltage in units of the game, always a power of four times eight
     */
    public int euPerTick() {
        return euPerTick;
    }

    /**
     * {@code true} when this tier is a tier and not the one below it.
     * <p>
     * The question a machine asks of a line: a machine of high voltage runs on a line of the extreme
     * voltage as well - a better line feeds a worse machine - while a machine of the extreme voltage on
     * a line of high voltage is a machine that never runs.
     *
     * @param other tier to compare against
     * @return {@code true} when this tier stands at or above {@code other}
     */
    public boolean isAtLeast(Voltage other) {
        return ordinal() >= other.ordinal();
    }

    @Override
    public String toString() {
        return fileName;
    }
}
