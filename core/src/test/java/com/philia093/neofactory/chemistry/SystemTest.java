package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the vessel a reaction runs in: what was put in, what stands around and in which state.
 * <p>
 * The line a system draws is the one a balance leans on: the content is what a player measures out and the
 * background is the medium that may move whole. A solution is the common case - solutes in a solvent - and
 * is checked to carry its solvent as background and its solutes as content, so that a reaction written
 * inside it neither consumes the water it is carried out in nor hands it back as a product.
 */
class SystemTest {

    @Test
    void aSolutionIsSolutesInASolvent() {
        Chemical water = Chemical.parse("O");
        Chemical salt = Chemical.parse("[Na+]");

        System solution = System.solution(Mixture.of(salt, 1), water);

        assertEquals(Phase.AQUEOUS, solution.phase());
        assertEquals(1, solution.background().size());
        assertTrue(solution.background().contains(water));
        assertEquals(1, solution.content().size());
        assertEquals(salt, solution.content().components().keySet().iterator().next());
    }

    @Test
    void aSystemWithoutAMediumHasNothingStandingAround() {
        System dry = System.of(Mixture.of(Chemical.parse("[Fe]"), 1), Phase.SOLID);

        assertTrue(dry.background().isEmpty());
        assertEquals(Phase.SOLID, dry.phase());
    }
}
