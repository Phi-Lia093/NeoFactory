package com.philia093.neofactory.item;

import com.badlogic.gdx.graphics.Color;

import java.util.Locale;

/**
 * The five chemistries a battery of the industry is built of.
 * <p>
 * <b>A battery is read by what is inside it and by nothing else.</b> Every chemistry comes in the three
 * tiers of the line, so a small lithium cell and a large one are the same chemistry with more of it in
 * them, see {@link Batteries}. What a chemistry decides is what a player sees at a cell and what they may
 * do with it:
 * <ul>
 *     <li>{@link #colour()} - <b>the colour the window of the battery is painted in</b>, which is what
 *         tells two batteries of one tier apart in a slot. It is painted into the window and never
 *         multiplied over the whole picture: a battery is a grey picture of the pack with a window in it,
 *         the way a cell of fluid is, so the steel around the window stays steel whatever is inside, see
 *         {@link com.philia093.neofactory.render.CellIconFactory};</li>
 *     <li>{@link #isRechargeable()} - whether the charge may be put back. <b>An acid and a mercury cell are
 *         spent for good</b> and a player throws them away, while a sodium, a cadmium and a lithium one
 *         take a charge again, which is what a battery box is built for, see {@code BatteryBoxMachine};</li>
 *     <li>{@link #nominalSeconds()} - how long one ampere of any tier lasts on it. The number is the same
 *         for the three tiers of a chemistry, because a bigger cell of a chemistry is a cell that holds
 *         more and not one that gives more: a hundred and fifty six seconds of one ampere is what makes
 *         the lithium cell the one a workshop keeps its line fed with.</li>
 * </ul>
 */
public enum BatteryChemistry {

    /** The lead acid cell, the cheap one that cannot be filled again. */
    ACID("Acid", false, 0xE08A18FF, 28),

    /** The mercury cell, whose charge lasts longer and which is spent for good as well. */
    MERCURY("Mercury", false, 0xD8AEB4FF, 50),

    /** The sodium cell, the first one that takes a charge again. */
    SODIUM("Sodium", true, 0x2838D8FF, 78),

    /** The cadmium cell, which holds half again as much as the sodium one and charges as well. */
    CADMIUM("Cadmium", true, 0xB83A9EFF, 117),

    /** The lithium cell, the best one to keep a line of the power fed over, and the best to charge. */
    LITHIUM("Lithium", true, 0x9A8AE0FF, 156);

    private final String displayName;
    private final boolean rechargeable;
    private final Color colour;
    private final int nominalSeconds;

    BatteryChemistry(String displayName, boolean rechargeable, int rgba, int nominalSeconds) {
        this.displayName = displayName;
        this.rechargeable = rechargeable;
        this.colour = new Color(rgba);
        this.nominalSeconds = nominalSeconds;
    }

    /** Name of this chemistry as a word, used where a tooltip names the cell. */
    public String displayName() {
        return displayName;
    }

    /**
     * {@code true} when a cell of this chemistry takes a charge again.
     * <p>
     * A cell that does not is not a worse battery - an acid cell is the cheapest one of the three tiers it
     * comes in - it is simply a cell a player uses once, see {@link Battery#isRechargeable()}.
     */
    public boolean isRechargeable() {
        return rechargeable;
    }

    /**
     * Colour the window of a battery of this chemistry is painted in.
     * <p>
     * The colour is shared and never mutated: it is handed to the drawing of a slot as it is, so a copy of
     * it is made by whoever changes one, see {@link com.philia093.neofactory.gui.GuiItemRenderer}.
     *
     * @return the colour of the chemistry
     */
    public Color colour() {
        return colour;
    }

    /**
     * Seconds one ampere of a cell of this chemistry lasts.
     * <p>
     * <b>The number is what tells the chemistries of one tier apart</b>, and it is the same for all three
     * of them, see the note on the class. What one ampere counts against is the voltage of the cell, so a
     * cell of the middle voltage holds four times what one of the low voltage holds and lasts as long with
     * it.
     *
     * @return the time in seconds
     */
    public int nominalSeconds() {
        return nominalSeconds;
    }

    /** Name of this chemistry in lower case, the way a name of an item is written. */
    public String fileName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return fileName();
    }
}
