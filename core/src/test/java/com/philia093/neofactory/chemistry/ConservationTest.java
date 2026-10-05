package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the one rule a reaction has to keep: the atoms and the charge both have to come out.
 * <p>
 * The rule is checked closed and open. Closed, in a vessel that holds nothing else, the two sides have to
 * match exactly. Open, in water, a difference that is a whole number of water molecules is allowed - the
 * water a reaction spends or makes without a player counting it - while a difference water cannot explain
 * is refused. The charge is checked against the electrons, which is what makes an electrolysis half
 * reaction balance only when it names the right number of them.
 */
class ConservationTest {

    private static final Chemical WATER = Chemical.parse("O");
    private static final Chemical HYDROGEN = Chemical.parse("[H][H]");
    private static final Chemical OXYGEN = Chemical.parse("O=O");
    private static final Chemical METHANOL = Chemical.parse("CO");
    private static final Chemical DIMETHYL_ETHER = Chemical.parse("COC");
    private static final Chemical COPPER_ION = Chemical.parse("[Cu+2]");
    private static final Chemical COPPER = Chemical.parse("[Cu]");
    private static final Chemical HYDROXIDE = Chemical.parse("[OH-]");
    private static final Chemical PROTON = Chemical.parse("[H+]");

    @Test
    void aReactionInAClosedVesselHasToMatchExactly() {
        Reaction burning = Reaction.of(
                Mixture.of(HYDROGEN, 2).plus(Mixture.of(OXYGEN, 1)),
                Mixture.of(WATER, 2));

        assertTrue(Conservation.balanced(burning));
        Conservation.check(burning);
    }

    @Test
    void aReactionThatDoesNotMatchIsRefused() {
        Reaction broken = Reaction.of(
                Mixture.of(HYDROGEN, 1).plus(Mixture.of(OXYGEN, 1)),
                Mixture.of(WATER, 1));

        assertFalse(Conservation.elementsBalanced(broken));
        assertThrows(IllegalStateException.class, () -> Conservation.check(broken));
    }

    @Test
    void waterStandsAroundFreelyAndMovesWhole() {
        // Two methanols into one dimethyl ether: the water that goes with it is the water of the medium.
        Mixture reactants = Mixture.of(METHANOL, 2);
        Mixture products = Mixture.of(DIMETHYL_ETHER, 1);

        assertFalse(Conservation.elementsBalanced(Reaction.of(reactants, products)),
                "without a background the difference is not nothing");
        assertTrue(Conservation.elementsBalanced(Reaction.of(reactants, products, 0, List.of(WATER))),
                "with water standing around, a whole water explains the difference");
    }

    @Test
    void aDifferenceWaterCannotExplainIsRefused() {
        // A proton set free is no whole water, so a reaction that only names water cannot explain it.
        Reaction givingAProton = Reaction.of(Mixture.of(HYDROXIDE, 1), Mixture.of(WATER, 1), 0,
                List.of(WATER));

        assertFalse(Conservation.elementsBalanced(givingAProton));
    }

    @Test
    void moreThanOneBackgroundSubstanceMayStandAround() {
        // The same proton is explained once a proton is part of what stands around.
        Reaction givingAProton = Reaction.of(Mixture.of(HYDROXIDE, 1), Mixture.of(WATER, 1), 0,
                List.of(WATER, PROTON));

        assertTrue(Conservation.elementsBalanced(givingAProton));
    }

    @Test
    void theChargeIsCheckedAgainstTheElectrons() {
        // A copper ion takes two electrons and leaves as copper metal.
        Reaction plating = Reaction.of(Mixture.of(COPPER_ION, 1), Mixture.of(COPPER, 1), 2, List.of());

        assertTrue(Conservation.chargeBalanced(plating));
        assertTrue(Conservation.balanced(plating));
        assertFalse(Conservation.chargeBalanced(
                Reaction.of(Mixture.of(COPPER_ION, 1), Mixture.of(COPPER, 1), 1, List.of())),
                "one electron cannot pay for two charges");
    }

    @Test
    void aHalfReactionThatGivesElectronsOutBalancesAsWell() {
        // Oxygen evolution: four hydroxides into one oxygen, two waters and four electrons handed out.
        Reaction evolving = Reaction.of(
                Mixture.of(HYDROXIDE, 4),
                Mixture.of(OXYGEN, 1).plus(Mixture.of(WATER, 2)),
                -4, List.of());

        assertTrue(Conservation.balanced(evolving));
    }
}
