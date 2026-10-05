package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the catalog of substances, the closed world a reaction is written inside.
 * <p>
 * The two rules under test are what the catalog is looked up by. Two writings of one molecule have to find
 * one entry - the key is the molecule and not the name - and one molecule in two phases has to be two
 * entries, which is what tells the water a player pours from the vapour a boiler draws off. The catalog
 * also has to refuse two names for one molecule, because that would be two substances the game could not
 * tell apart.
 */
class SubstancesTest {

    @Test
    void twoWritingsOfOneMoleculeAreOneSubstance() {
        Substances catalog = new Substances().register("benzene", "c1ccccc1", Phase.LIQUID);

        assertEquals(catalog.byName("benzene"),
                catalog.byChemical(Chemical.parse("C1=CC=CC=C1"), Phase.LIQUID));
        assertNotNull(catalog.byChemical(Chemical.parse("c1ccccc1"), Phase.LIQUID));
    }

    @Test
    void oneMoleculeInTwoPhasesIsTwoSubstances() {
        Substances catalog = new Substances()
                .register("water", "O", Phase.LIQUID)
                .register("steam", "O", Phase.GAS);

        assertEquals(2, catalog.count());
        assertEquals("water", catalog.byChemical(Chemical.parse("O"), Phase.LIQUID).name());
        assertEquals("steam", catalog.byChemical(Chemical.parse("O"), Phase.GAS).name());
    }

    @Test
    void theStarterCatalogHoldsWhatAFirstReactionNeeds() {
        Substances catalog = Substances.starter();

        assertNotNull(catalog.byName("water"));
        assertNotNull(catalog.byName("iron"));
        assertEquals(Phase.AQUEOUS, catalog.byName("sulfate").phase());
        assertEquals(-2, catalog.byName("sulfate").charge(), "a sulfate carries two negative charges");
        assertEquals("H2O4S", catalog.byName("sulfuric acid").formula(), "the Hill formula H2SO4 written out");
        assertTrue(catalog.count() > 40, "the starter catalog is a real handful of substances");
        assertFalse(catalog.contains(Chemical.parse("[Xe]"), Phase.GAS), "a rare gas is not named yet");
    }

    @Test
    void twoNamesForOneMoleculeAndPhaseAreRefused() {
        Substances catalog = new Substances().register("water", "O", Phase.LIQUID);

        assertThrows(IllegalArgumentException.class, () -> catalog.register("aqua", "O", Phase.LIQUID));
    }

    @Test
    void registeringTheSameSubstanceAgainIsHarmless() {
        Substances catalog = new Substances().register("water", "O", Phase.LIQUID);

        catalog.register("water", "O", Phase.LIQUID);

        assertEquals(1, catalog.count());
    }
}
