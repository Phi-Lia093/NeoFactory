package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks the valence an atom of the organic subset falls back on, and the hydrogens that follow from it.
 * <p>
 * The two rules worth pinning are the ones that are easy to get wrong: the default valence of sulfur and
 * phosphorus is their low count and not their high one - the high one comes from the double bonds a string
 * draws - and a metal is handed no implicit hydrogen at all, which is what keeps a bracketed ion from
 * growing atoms its brackets never named.
 */
class DefaultValenceTest {

    @Test
    void theDefaultValenceIsTheLowCountOfAnElement() {
        assertEquals(4, DefaultValence.defaultValence("C"));
        assertEquals(3, DefaultValence.defaultValence("N"));
        assertEquals(2, DefaultValence.defaultValence("O"));
        assertEquals(1, DefaultValence.defaultValence("F"));
        assertEquals(1, DefaultValence.defaultValence("Cl"));

        // Sulfur falls back on two bonds; the six of a sulfate come from the string, not from here.
        assertEquals(2, DefaultValence.defaultValence("S"));
        assertEquals(3, DefaultValence.defaultValence("P"));
    }

    @Test
    void anElementOutsideTheOrganicSubsetHasNoDefaultValence() {
        assertThrows(IllegalArgumentException.class, () -> DefaultValence.defaultValence("Fe"));
        assertThrows(IllegalArgumentException.class, () -> DefaultValence.defaultValence("Na"));
    }

    @Test
    void aChargeMovesTheValence() {
        assertEquals(4, DefaultValence.targetValence("N", 1), "the nitrogen of an ammonium");
        assertEquals(2, DefaultValence.targetValence("N", -1));
        assertEquals(1, DefaultValence.targetValence("O", -1), "the oxygen of a hydroxide");
        assertEquals(3, DefaultValence.targetValence("O", 1));
        assertEquals(3, DefaultValence.targetValence("C", -1));
    }

    @Test
    void implicitHydrogensFollowTheBonds() {
        assertEquals(4, DefaultValence.implicitHydrogens("C", 0, false, 0));
        assertEquals(3, DefaultValence.implicitHydrogens("N", 0, false, 0));
        assertEquals(2, DefaultValence.implicitHydrogens("O", 0, false, 0));
        assertEquals(0, DefaultValence.implicitHydrogens("O", 0, false, 2), "water needs no more");

        // A ring carbon holds one hydrogen; the bare nitrogen of a ring holds none.
        assertEquals(1, DefaultValence.implicitHydrogens("C", 0, true, 2));
        assertEquals(0, DefaultValence.implicitHydrogens("N", 0, true, 2));

        // A metal is never handed one, and the count is never below zero.
        assertEquals(0, DefaultValence.implicitHydrogens("Fe", 2, false, 0));
        assertEquals(0, DefaultValence.implicitHydrogens("O", 0, false, 5));
    }

    @Test
    void anUnpairedElectronTakesABondAwayFromTheHydrogens() {
        // A methyl radical holds three hydrogens, not the four of a methane carbon.
        assertEquals(3, DefaultValence.implicitHydrogens("C", 0, false, 0, 1));
        // A carbon that already carries three bonds holds none once it is left a radical.
        assertEquals(0, DefaultValence.implicitHydrogens("C", 0, false, 3, 1));
        // An alkoxy radical: an oxygen with one bond and one unpaired electron holds no hydrogen at all.
        assertEquals(0, DefaultValence.implicitHydrogens("O", 0, false, 1, 1));
        // Two unpaired electrons are two bonds the atom never gets to form.
        assertEquals(2, DefaultValence.implicitHydrogens("C", 0, false, 0, 2));
        // A closed shell is the same rule with nothing taken off.
        assertEquals(4, DefaultValence.implicitHydrogens("C", 0, false, 0, 0));
    }

    @Test
    void aChargeTakesABondFromACarbonWhicheverWayItGoes() {
        // A carbon has no lone pair to give up, so its cation and its anion both hang on three bonds.
        assertEquals(4, DefaultValence.targetValence("C", 0));
        assertEquals(3, DefaultValence.targetValence("C", 1), "the methyl cation");
        assertEquals(3, DefaultValence.targetValence("C", -1), "the methyl anion");
        // A boron is short of a pair to begin with, so its anion is the four-bonded one.
        assertEquals(4, DefaultValence.targetValence("B", -1), "the borate");
        assertEquals(2, DefaultValence.targetValence("B", 1));
        // An element that holds a lone pair follows the charge instead.
        assertEquals(4, DefaultValence.targetValence("N", 1));
        assertEquals(1, DefaultValence.targetValence("O", -1));
    }

    @Test
    void aCationKeepsNoHydrogensItNeverHad() {
        // The carbon of a cation that hangs on three bonds holds no hydrogen at all.
        assertEquals(0, DefaultValence.implicitHydrogens("C", 1, false, 3));
        assertEquals(2, DefaultValence.implicitHydrogens("C", 1, false, 1));
        assertEquals(2, DefaultValence.implicitHydrogens("C", -1, false, 1), "the methyl anion");
    }

    @Test
    void theLonePairsFollowFromTheElectronsLeftOver() {
        assertEquals(2, DefaultValence.lonePairs("O", 0, 2, 0), "water");
        assertEquals(3, DefaultValence.lonePairs("O", -1, 1, 0), "a hydroxide");
        assertEquals(1, DefaultValence.lonePairs("N", 0, 3, 0), "an amine");
        assertEquals(0, DefaultValence.lonePairs("C", 1, 3, 0), "a carbocation");
        assertEquals(1, DefaultValence.lonePairs("C", -1, 3, 0), "a carbanion");
        assertEquals(0, DefaultValence.lonePairs("C", 0, 3, 1), "a methyl radical");
    }
}
