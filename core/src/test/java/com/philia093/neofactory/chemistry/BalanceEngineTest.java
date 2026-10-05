package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the engine that finds the reactions of a solution by balancing what stands in it.
 * <p>
 * Every reaction is checked by its two sides and not by how it was found: the engine is free to look for
 * them in any order, but the answer for a pot is a fact, and the tests pin the one it reaches. A pot that
 * holds nothing a reaction can be written out of is checked just as hard, because an engine that answered
 * something there would be inventing a reaction out of an element and no catalog would stop it.
 */
class BalanceEngineTest {

    private static final Substances CATALOG = Substances.starter();
    private static final BalanceEngine ENGINE = new BalanceEngine(CATALOG);

    private static Chemical of(String name) {
        return CATALOG.byName(name).chemical();
    }

    @Test
    void twoGasesBurnTogetherIntoWater() {
        Mixture pot = Mixture.of(of("hydrogen"), 2).plus(Mixture.of(of("oxygen"), 1));
        System system = System.of(pot, Phase.GAS);

        Reaction reaction = ENGINE.best(system);

        assertNotNull(reaction, "hydrogen and oxygen balance into water");
        assertEquals(Mixture.of(of("water"), 2), reaction.products());
        assertEquals(pot, reaction.reactants());
        assertTrue(Conservation.balanced(reaction));
    }

    @Test
    void anAcidAndABaseNeutraliseEachOther() {
        Mixture pot = Mixture.of(of("proton"), 1).plus(Mixture.of(of("hydroxide"), 1));
        System solution = System.solution(pot, of("water"));

        Reaction reaction = ENGINE.best(solution);

        assertNotNull(reaction, "a proton and a hydroxide make water");
        assertEquals(Mixture.of(of("water"), 1), reaction.products());
    }

    @Test
    void anAcidDisplacesAGasFromASolution() {
        Mixture pot = Mixture.of(of("hydrogen chloride"), 1).plus(Mixture.of(of("hydroxide"), 1));
        System solution = System.solution(pot, of("water"));

        Reaction reaction = ENGINE.best(solution);

        assertNotNull(reaction, "hydrochloric acid and a hydroxide make water and a chloride");
        assertEquals(Mixture.of(of("water"), 1).plus(Mixture.of(of("chloride"), 1)),
                reaction.products());
    }

    @Test
    void anAcidIsRebuiltOutOfItsIon() {
        Mixture pot = Mixture.of(of("sulfate"), 1).plus(Mixture.of(of("proton"), 2));
        System solution = System.solution(pot, of("water"));

        Reaction reaction = ENGINE.best(solution);

        assertNotNull(reaction, "a sulfate and two protons are sulfuric acid");
        assertEquals(Mixture.of(of("sulfuric acid"), 1), reaction.products());
    }

    @Test
    void nothingHappensToALoneMetal() {
        System pot = System.of(Mixture.of(of("gold"), 1), Phase.SOLID);

        assertTrue(ENGINE.infer(pot).isEmpty(), "a bar of gold is the floor of the balance");
        assertNull(ENGINE.best(pot));
    }

    @Test
    void theAnswerIsTheSameEveryTime() {
        System system = System.of(
                Mixture.of(of("hydrogen"), 2).plus(Mixture.of(of("oxygen"), 1)), Phase.GAS);

        assertEquals(ENGINE.best(system).products(), ENGINE.best(system).products());
        assertEquals(ENGINE.infer(system).size(), ENGINE.infer(system).size());
    }
}
