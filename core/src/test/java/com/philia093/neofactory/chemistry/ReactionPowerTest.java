package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what an inferred reaction costs to run, which is the one thing a machine has to be told that the
 * reaction itself cannot say.
 * <p>
 * The three settings of {@link Warmth} are the whole of the temperature, so what is asked here is the order
 * of them and the way the cost follows from it: a vessel held in the cold costs more than one with a flame
 * under it, and that costs more than one that is only standing there. Then the two other terms - the size of
 * the reaction, which is its time, and the electrons of a current, which are power on top - are asked one at
 * a time, since a sum that answers everything at once answers nothing in particular.
 */
class ReactionPowerTest {

    @Test
    void aVesselHeldInTheColdCostsMoreThanOneOverAFlameAndThatMoreThanOneStandingThere() {
        Reaction reaction = someReaction();

        int ambient = ReactionPower.of(reaction, Warmth.AMBIENT).euPerTick();
        int heated = ReactionPower.of(reaction, Warmth.HEATED).euPerTick();
        int cold = ReactionPower.of(reaction, Warmth.COLD).euPerTick();

        assertEquals(ReactionPower.EU_PER_TICK, ambient, "a vessel that stands as it is draws the base");
        assertTrue(heated > ambient, "a flame costs something");
        assertTrue(cold > heated, "and the cold costs the most of the three");
    }

    @Test
    void aVesselThatNamesNoSettingIsRunAsItStands() {
        // Which is the cheapest of the three, and is what every vessel was before anything named a setting.
        Reaction reaction = someReaction();

        assertEquals(ReactionPower.of(reaction, Warmth.AMBIENT).euPerTick(),
                ReactionPower.of(reaction, (Warmth) null).euPerTick(),
                "a vessel that says nothing draws what one that stands there draws");
        assertEquals(ReactionPower.of(reaction, Warmth.AMBIENT).euPerTick(),
                ReactionPower.of(reaction, Conditions.NONE).euPerTick(),
                "and so does a vessel whose conditions name nothing");
        assertEquals(ReactionPower.of(reaction, Warmth.COLD).euPerTick(),
                ReactionPower.of(reaction, Conditions.at(Warmth.COLD)).euPerTick(),
                "the setting of the conditions is the one that is costed out");
    }

    @Test
    void aCurrentIsPowerOnTopOfTheSetting() {
        // Ethics aside: a reaction that takes two electrons from a circuit draws for them as well.
        Reaction driven = Reaction.of(Mixture.of(Chemical.parse("O"), 1),
                Mixture.of(Chemical.parse("[H][H]"), 1), 2, List.of());

        assertEquals(ReactionPower.EU_PER_TICK + 2 * ReactionPower.EU_PER_ELECTRON,
                ReactionPower.of(driven, Warmth.AMBIENT).euPerTick(),
                "the setting and the current are counted together");
    }

    @Test
    void aReactionThatMovesMoreMoleculesTakesLonger() {
        Reaction small = someReaction();
        Reaction large = Reaction.of(
                Mixture.of(Chemical.parse("C=C"), 1).plus(Mixture.of(Chemical.parse("[H][H]"), 1)),
                Mixture.of(Chemical.parse("CC"), 1).plus(Mixture.of(Chemical.parse("O"), 1)));

        assertEquals(ReactionPower.BASE_SECONDS + 2 * ReactionPower.SECONDS_PER_MOLECULE,
                ReactionPower.of(small, Warmth.AMBIENT).seconds(), 1.0e-6f,
                "the time is the base plus what the reaction moves");
        assertTrue(ReactionPower.of(large, Warmth.AMBIENT).seconds()
                        > ReactionPower.of(small, Warmth.AMBIENT).seconds(),
                "four molecules take longer than two");
    }

    /** A reaction of one molecule in and one out, which every term here is measured against. */
    private static Reaction someReaction() {
        return Reaction.of(Mixture.of(Chemical.parse("C=C"), 1), Mixture.of(Chemical.parse("CC"), 1));
    }
}
