package com.philia093.neofactory.chemistry;

import java.util.Objects;

/**
 * What a reaction of the organic side costs to run, out of the heat, the medium and the catalysts it is run
 * with.
 * <p>
 * <b>A reaction that was inferred has no file to have written its cost in.</b> A route of the industry is
 * written down by hand and its file may say how long it takes and what it draws, see
 * {@link com.philia093.neofactory.recipe.ChemicalRecipe}; a reaction of the organic side is found out of the
 * groups that stand in the vessel, so there is no file and the cost has to follow from the reaction itself.
 * What follows is this: <b>what the vessel is asked for is what the operation costs</b>.
 * <p>
 * <b>The setting is the whole of the temperature and it is what most of the cost is.</b> A reaction run as
 * the vessel stands draws the base power and no more; one over a flame draws twice it, since the vessel has
 * to be held hotter than everything around it; and one held back in the cold draws three times, since the
 * heat has to be taken away and then kept away. A vessel that names no setting at all is run as it stands,
 * which is the cheapest of the three - and it is also what every vessel was before anything named a
 * setting, so nothing about the engine's answers changes for a caller that says nothing, see
 * {@link Warmth#factor()}.
 * <p>
 * <b>The time follows from how much the reaction moves.</b> A reaction that moves four molecules takes
 * longer than one that moves two, and the seconds of it are counted rather than measured. And a reaction a
 * current drives costs the current on top: every electron it takes from the circuit or hands to one is
 * power the machine has to put in, which is why an electrolysis is dearer than the same change made by
 * heat.
 * <p>
 * <b>Every number here is a declaration of the game and never a measurement of chemistry.</b> What is meant
 * is the order of the three settings and the fact that a bigger reaction takes longer; a player reads a
 * number of EU and not a quantity of heat, and the engine is never asked whether a reaction is possible
 * from what it costs, only what it would cost.
 */
public final class ReactionPower {

    /** Power a reaction of the organic side draws every tick at the setting it stands at, in EU. */
    public static final int EU_PER_TICK = 8;

    /** Seconds one operation takes before what it moves is counted. */
    public static final float BASE_SECONDS = 5.0f;

    /** Seconds every molecule a reaction moves adds to the time it takes. */
    public static final float SECONDS_PER_MOLECULE = 0.5f;

    /** Power every electron a reaction takes from a current, or hands to one, adds in EU a tick. */
    public static final int EU_PER_ELECTRON = 8;

    private ReactionPower() {
        // Utility class: never instantiated.
    }

    /**
     * What one operation of a reaction costs at a setting.
     *
     * @param reaction the reaction to cost out
     * @param warmth the setting the vessel is run at, {@code null} for one that stands as it is
     * @return the seconds it takes and the power it draws, both always positive
     */
    public static Draw of(Reaction reaction, Warmth warmth) {
        Objects.requireNonNull(reaction, "reaction");
        Warmth setting = warmth == null ? Warmth.AMBIENT : warmth;
        int electrons = Math.abs(reaction.electrons());
        int euPerTick = EU_PER_TICK * setting.factor() + EU_PER_ELECTRON * electrons;
        float seconds = BASE_SECONDS + SECONDS_PER_MOLECULE * molecules(reaction);
        return new Draw(seconds, euPerTick);
    }

    /**
     * What one operation of a reaction costs in a vessel of the conditions it is run with.
     *
     * @param reaction the reaction to cost out
     * @param conditions the conditions of the vessel, {@code null} for one that stands as it is
     * @return the seconds it takes and the power it draws, both always positive
     */
    public static Draw of(Reaction reaction, Conditions conditions) {
        return of(reaction, conditions == null ? null : conditions.warmth());
    }

    /** How many molecules a reaction moves in and out of the vessel. */
    private static int molecules(Reaction reaction) {
        int count = 0;
        for (int amount : reaction.reactants().components().values()) {
            count += amount;
        }
        for (int amount : reaction.products().components().values()) {
            count += amount;
        }
        return count;
    }

    /**
     * What one operation of a reaction costs: how long it takes and what it draws while it runs.
     * <p>
     * A machine of the industry works the way every machine of the game does - it draws the power for the
     * ticks the operation runs - so the two numbers here are the whole of what it needs, see
     * {@link com.philia093.neofactory.recipe.EnergyRecipe}.
     *
     * @param seconds seconds one operation takes
     * @param euPerTick energy the operation draws every tick
     */
    public record Draw(float seconds, int euPerTick) {

        /** Seconds one operation takes. */
        @Override
        public float seconds() {
            return seconds;
        }

        /** Energy the operation draws every tick. */
        @Override
        public int euPerTick() {
            return euPerTick;
        }

        /**
         * Energy the whole operation takes.
         *
         * @return the total in EU, counted the way a recipe of the game counts one
         */
        public int totalEu() {
            return Math.round(euPerTick * seconds * 20.0f);
        }

        @Override
        public String toString() {
            return "Draw(" + seconds + "s, " + euPerTick + "EU/t, " + totalEu() + "EU)";
        }
    }
}
