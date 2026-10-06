package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the asking of a vessel how it is run: what a route demands of one and what one does not block.
 * <p>
 * The whole of the layer is one rule and one exception to it. <b>A dimension a route names is asked of the
 * vessel</b> - a route for a flame does not run in a vessel that says it stands there, a route over nickel
 * does not run in one that says it holds copper - and <b>a dimension neither of them names is not looked at
 * at all</b>, which is what let every route of the game keep running the day the layer was written and the
 * reason a caller that says nothing is never refused anything.
 * <p>
 * The two conditions that are demanded rather than wondered at are asked of a vessel that says nothing just
 * the same: a route a current drives, and a route a light drives, do not run in a pot that names neither,
 * because a vessel that is not lit is not a vessel that might be lit.
 */
class ConditionsTest {

    /** A route that asks for the one setting and nothing else. */
    private static final Conditions HEATED = Conditions.at(Warmth.HEATED);

    /** A route that asks to be run over nickel. */
    private static final Conditions OVER_NICKEL = Conditions.at(Warmth.AMBIENT,
            java.util.Set.of(Chemical.parse("[Ni]")));

    @Test
    void aVesselThatSaysNothingIsNeverRefused() {
        // What every route of the game was before anything was asked: a route that names a setting runs in a
        // vessel that names none, and so does a route that names a catalyst.
        assertTrue(HEATED.within(Conditions.NONE), "a route for a flame runs in a vessel that says nothing");
        assertTrue(OVER_NICKEL.within(Conditions.NONE), "and so does one over a metal");
    }

    @Test
    void aSettingASettingIsAskedOfTheVessel() {
        assertTrue(HEATED.within(Conditions.at(Warmth.HEATED)), "over a flame");
        assertFalse(HEATED.within(Conditions.at(Warmth.AMBIENT)), "and not standing there");
        assertFalse(HEATED.within(Conditions.at(Warmth.COLD)), "and not in the cold");
        assertTrue(Conditions.at(Warmth.COLD).within(Conditions.at(Warmth.COLD)),
                "the setting of the cold is asked of the cold");
    }

    @Test
    void aCatalystIsAskedOfWhatStandsInTheVessel() {
        assertTrue(OVER_NICKEL.within(Conditions.at(Warmth.AMBIENT,
                java.util.Set.of(Chemical.parse("[Ni]")))), "nickel stands there");
        assertFalse(OVER_NICKEL.within(Conditions.at(Warmth.AMBIENT,
                java.util.Set.of(Chemical.parse("[Cu]")))), "copper does not answer for nickel");
    }

    @Test
    void aMediumIsAskedOfWhatTheVesselIsFilledWith() {
        Conditions inWater = Conditions.at(Warmth.HEATED, Chemical.parse("O"));

        assertTrue(inWater.within(Conditions.at(Warmth.HEATED, Chemical.parse("O"))), "water in water");
        assertFalse(inWater.within(Conditions.at(Warmth.HEATED, Chemical.parse("CCO"))),
                "and a vessel filled with an alcohol is not one filled with water");
    }

    @Test
    void aLightAndACurrentAreDemandedAndNotMerelyWonderedAt() {
        // Both are the same statement: a reaction that only goes in the light does not go in the dark, and a
        // vessel that says nothing of a light is the dark.
        Conditions lit = Conditions.builder().lighted(true).build();
        Conditions unlit = Conditions.builder().build();

        assertTrue(lit.within(lit), "a route of the light runs under one");
        assertFalse(lit.within(unlit), "and not in a vessel that says nothing of a light");
        assertTrue(Conditions.NONE.within(unlit), "a route that asks for no light runs in the dark");

        Conditions driven = Conditions.builder().current(true).build();
        assertFalse(driven.within(unlit), "and a route a current drives is asked the same way");
        assertTrue(driven.within(driven), "and runs when the current is there");
    }
}
