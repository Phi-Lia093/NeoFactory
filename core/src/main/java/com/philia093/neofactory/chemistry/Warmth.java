package com.philia093.neofactory.chemistry;

/**
 * How hot a vessel of the industry is run, in the three settings a machine of it offers.
 * <p>
 * <b>Three settings and no number.</b> A temperature in kelvin is a measurement of a laboratory and not a
 * setting of a factory: a player does not dial a hundred degrees, they put a flame under the vessel, or a
 * cold bath around it, or do neither. Every reaction of the organic side that cares about heat at all can be
 * written against those three - the one that only goes over a flame, the one that must be held back in the
 * cold, and the one that goes as the vessel stands - so that is what a route names and what a machine has,
 * see {@link Conditions}. A route of the inorganic side, which is written against a real measurement, names
 * a range in kelvin instead and the two never have to agree on a number.
 * <p>
 * <b>The factor is what the setting costs, and it is a declaration of the game and not a measurement of
 * anything.</b> Holding a vessel back in the cold is the dearest of the three - a factory pays to take heat
 * away and then pays again to keep the cold where it wants it - a flame under a vessel is next, and a vessel
 * that is asked for nothing at all is the cheapest there is. What a reaction costs follows from it, see
 * {@link ReactionPower}.
 */
public enum Warmth {

    /** Run as the vessel stands, with nothing done to it either way. */
    AMBIENT("ambient", 1),

    /** Run over a flame, which is what a slow reaction and a stubborn one are given. */
    HEATED("heated", 2),

    /** Run in a cold bath, which is what a reaction that would run away with itself is given. */
    COLD("cold", 3);

    private final String label;
    private final int factor;

    Warmth(String label, int factor) {
        this.label = label;
        this.factor = factor;
    }

    /** The name of the setting, the word a file and a screen write. */
    public String label() {
        return label;
    }

    /**
     * What the setting multiplies the power of a reaction by, higher for the dearer.
     * <p>
     * A declaration of the game and not a number of physics: only the order of the three is meant, and it is
     * written down here once so that a rule and a machine cannot cost a reaction out twice.
     *
     * @return the multiplier
     */
    public int factor() {
        return factor;
    }

    /**
     * The setting a word names.
     *
     * @param label the name of a setting
     * @return the setting
     * @throws IllegalArgumentException when no setting carries that name
     */
    public static Warmth byName(String label) {
        for (Warmth warmth : values()) {
            if (warmth.label.equals(label)) {
                return warmth;
            }
        }
        throw new IllegalArgumentException("No setting of the three is called " + label);
    }

    @Override
    public String toString() {
        return label;
    }
}
