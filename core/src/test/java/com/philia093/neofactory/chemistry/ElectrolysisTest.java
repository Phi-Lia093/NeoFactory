package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the engine that finds the decompositions a current pays for.
 * <p>
 * An electrolysis is checked by what it takes apart and what it frees: water into the two gases, a salt of a
 * metal into the metal and the halogen. A pot that holds nothing to take apart is checked the same way a
 * single metal is checked in the other engine - a bar of gold has nowhere to go - and the reaction of a cell
 * is checked to name no electron of its own, because the two electrodes cancel each other, see
 * {@link Electrolysis}.
 */
class ElectrolysisTest {

    private static final Substances CATALOG = Substances.starter();
    private static final Electrolysis ENGINE = new Electrolysis(CATALOG);

    private static Chemical of(String name) {
        return CATALOG.byName(name).chemical();
    }

    @Test
    void waterSplitsIntoTheTwoGases() {
        System pot = System.of(Mixture.of(of("water"), 2), Phase.LIQUID);

        Reaction reaction = ENGINE.best(pot);

        assertNotNull(reaction, "water splits into hydrogen and oxygen");
        assertEquals(Mixture.of(of("hydrogen"), 2).plus(Mixture.of(of("oxygen"), 1)),
                reaction.products());
        assertEquals(0, reaction.electrons(), "the two electrodes cancel and name no electron");
        assertTrue(Conservation.balanced(reaction));
    }

    @Test
    void aSaltGivesUpItsMetal() {
        System solution = System.solution(
                Mixture.of(of("copper(II) ion"), 1).plus(Mixture.of(of("chloride"), 2)),
                of("water"));

        Reaction reaction = ENGINE.best(solution);

        assertNotNull(reaction, "a copper salt gives up its copper");
        assertEquals(Mixture.of(of("copper"), 1).plus(Mixture.of(of("chlorine"), 1)),
                reaction.products());
    }

    @Test
    void aBarOfGoldHasNowhereToGo() {
        System pot = System.of(Mixture.of(of("gold"), 1), Phase.SOLID);

        assertTrue(ENGINE.infer(pot).isEmpty(), "an element cannot be taken apart");
        assertNull(ENGINE.best(pot));
    }

    @Test
    void aPotOfMetalsHoldsNothingADecompositionCouldStartFrom() {
        // Two metals are two elements already; there is no compound standing there to be taken apart.
        System pot = System.of(
                Mixture.of(of("copper"), 1).plus(Mixture.of(of("gold"), 1)), Phase.SOLID);

        assertTrue(ENGINE.infer(pot).isEmpty(), "no compound stands in the pot to be taken apart");
    }

    @Test
    void theAnswerIsTheSameEveryTime() {
        System pot = System.of(Mixture.of(of("water"), 2), Phase.LIQUID);

        assertEquals(ENGINE.best(pot).products(), ENGINE.best(pot).products());
        List<Reaction> first = ENGINE.infer(pot);
        assertEquals(first.size(), ENGINE.infer(pot).size());
    }
}
