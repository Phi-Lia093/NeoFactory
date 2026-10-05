package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the routes written down by hand and the pot that is matched against them.
 * <p>
 * The rules under test are the ones that make a route a route and not a guess: a pot that holds what the
 * route needs runs it as many whole times as it can pay for, the rest of the pot stays where it is, and a
 * pot that no route covers is answered with nothing at all. A route that leaves an atom behind cannot even
 * be written down, and a route that asks for a heat or a catalyst is held to it by the vessel it is matched
 * against.
 */
class InorganicRecipeBookTest {

    private static final Substances CATALOG = Substances.starter();

    private static Chemical of(String name) {
        return CATALOG.byName(name).chemical();
    }

    /** Carbon and steam at a red heat, the first route of the industry. */
    private static InorganicRecipe syngas() {
        return InorganicRecipe.builder("syngas")
                .inputs(Blend.of(of("carbon"), 100).plus(Blend.of(of("water"), 100)))
                .outputs(Blend.of(of("carbon monoxide"), 100).plus(Blend.of(of("hydrogen"), 100)))
                .primary(of("carbon monoxide"))
                .comment("carbon and steam at a red heat")
                .build();
    }

    /** Nitrogen and hydrogen into ammonia. */
    private static InorganicRecipe haber() {
        return InorganicRecipe.builder("haber")
                .inputs(Blend.of(of("nitrogen"), 100).plus(Blend.of(of("hydrogen"), 300)))
                .outputs(Blend.of(of("ammonia"), 200))
                .primary(of("ammonia"))
                .build();
    }

    private static Blend syngasPot(int runs) {
        return Blend.of(of("carbon"), 100L * runs).plus(Blend.of(of("water"), 100L * runs));
    }

    @Test
    void aPotOfCarbonAndSteamRunsTheSyngasRoute() {
        Outcome outcome = InorganicRecipeBook.of(syngas()).find(syngasPot(1), Conditions.NONE);

        assertNotNull(outcome, "carbon and steam are written together");
        assertEquals("syngas", outcome.source());
        assertEquals(Fraction.of(100), outcome.consumed().amountOf(of("carbon")));
        assertEquals(Fraction.of(100), outcome.produced().amountOf(of("carbon monoxide")));
        assertEquals(Fraction.of(100), outcome.produced().amountOf(of("hydrogen")));
    }

    @Test
    void aPotThatHoldsThreeRunsRunsThreeTimes() {
        Outcome outcome = InorganicRecipeBook.of(syngas()).find(syngasPot(3), Conditions.NONE);

        assertEquals(Fraction.of(300), outcome.consumed().amountOf(of("carbon")));
        assertEquals(Fraction.of(300), outcome.produced().amountOf(of("hydrogen")));
    }

    @Test
    void whatTheRouteDoesNotUseStaysInThePot() {
        Blend pot = Blend.of(of("carbon"), 250).plus(Blend.of(of("water"), 400));

        Outcome outcome = InorganicRecipeBook.of(syngas()).find(pot, Conditions.NONE);

        assertEquals(Fraction.of(200), outcome.consumed().amountOf(of("carbon")),
                "two whole runs and not two and a half");
        assertEquals(Fraction.of(200), outcome.consumed().amountOf(of("water")),
                "both sides of the route are taken together");
    }

    @Test
    void aPotMissingOneOfTheSubstancesRunsNothing() {
        Blend pot = Blend.of(of("carbon"), 1000);
        InorganicRecipeBook book = InorganicRecipeBook.of(syngas());

        assertTrue(book.candidates(pot).isEmpty(), "the route is never even looked at");
        assertNull(book.find(pot, Conditions.NONE));
    }

    @Test
    void aPotNoRouteCoversIsAnsweredWithNothing() {
        InorganicRecipeBook book = InorganicRecipeBook.of(syngas(), haber());

        assertNull(book.find(Blend.of(of("gold"), 100), Conditions.NONE), "nothing is guessed here");
    }

    @Test
    void theRouteOfHigherImportanceWins() {
        InorganicRecipe low = InorganicRecipe.builder("low")
                .inputs(Blend.of(of("hydrogen"), 200).plus(Blend.of(of("oxygen"), 100)))
                .outputs(Blend.of(of("water"), 200))
                .build();
        InorganicRecipe high = InorganicRecipe.builder("high")
                .inputs(Blend.of(of("hydrogen"), 200).plus(Blend.of(of("oxygen"), 100)))
                .outputs(Blend.of(of("water"), 200))
                .priority(10)
                .build();
        Blend pot = Blend.of(of("hydrogen"), 200).plus(Blend.of(of("oxygen"), 100));

        assertEquals("high", InorganicRecipeBook.of(low, high).recipeFor(pot, Conditions.NONE).id());
    }

    @Test
    void aRouteThatLeavesAnAtomBehindCannotBeWritten() {
        assertThrows(IllegalStateException.class, () -> InorganicRecipe.builder("broken")
                .inputs(Blend.of(of("carbon"), 100))
                .outputs(Blend.of(of("carbon monoxide"), 100))
                .build());
    }

    @Test
    void aRouteOfARedHeatIsHeldToTheHeat() {
        InorganicRecipe hot = InorganicRecipe.builder("syngas")
                .inputs(Blend.of(of("carbon"), 100).plus(Blend.of(of("water"), 100)))
                .outputs(Blend.of(of("carbon monoxide"), 100).plus(Blend.of(of("hydrogen"), 100)))
                .conditions(Conditions.builder()
                        .temperature(Fraction.of(900), Fraction.of(1500)).build())
                .build();
        InorganicRecipeBook book = InorganicRecipeBook.of(hot);

        assertNotNull(book.find(syngasPot(1), Conditions.of(Fraction.of(1200), null, null)),
                "a red heat runs it");
        assertNull(book.find(syngasPot(1), Conditions.of(Fraction.of(300), null, null)),
                "a room does not");
        assertNotNull(book.find(syngasPot(1), Conditions.NONE),
                "a machine that measures nothing blocks nothing");
    }

    @Test
    void aRouteThatWantsACatalystIsHeldToIt() {
        InorganicRecipe catalysed = InorganicRecipe.builder("haber")
                .inputs(Blend.of(of("nitrogen"), 100).plus(Blend.of(of("hydrogen"), 300)))
                .outputs(Blend.of(of("ammonia"), 200))
                .conditions(Conditions.builder().catalyst(of("iron")).build())
                .build();
        InorganicRecipeBook book = InorganicRecipeBook.of(catalysed);
        Blend pot = Blend.of(of("nitrogen"), 100).plus(Blend.of(of("hydrogen"), 300));

        assertNull(book.find(pot, Conditions.builder().catalyst(of("copper")).build()),
                "copper is no iron");
        assertNotNull(book.find(pot, Conditions.builder().catalyst(of("iron")).build()));
    }
}
