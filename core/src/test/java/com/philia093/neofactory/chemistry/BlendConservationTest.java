package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the rule a reaction of the industry keeps, in the measured amounts a machine really spends.
 * <p>
 * The reactions are the ones the other engine is checked with, written in millibuckets instead of molecules,
 * and they have to come out the same way: water splits into the two gases, an acid and a base make a salt
 * and water, a current of two hundred electrons plates a hundred millibuckets of copper. The pile the
 * measured level adds is a mixture, which the molecule level cannot hold at all, so a bronze ingot taken
 * apart into its copper and its tin is checked here as well.
 */
class BlendConservationTest {

    private static final Substances CATALOG = Substances.starter();

    private static Chemical of(String name) {
        return CATALOG.byName(name).chemical();
    }

    @Test
    void waterSplitsIntoTheTwoGases() {
        Blend water = Blend.of(of("water"), 200);
        Blend gases = Blend.of(of("hydrogen"), 200).plus(Blend.of(of("oxygen"), 100));

        assertTrue(BlendConservation.balanced(water, gases, 0, List.of()));
        assertEquals(ElementAmounts.empty(), BlendConservation.difference(water, gases),
                "the two gases hold the water's atoms exactly");
    }

    @Test
    void aReactionThatDoesNotMatchIsRefused() {
        Blend water = Blend.of(of("water"), 100);
        Blend hydrogen = Blend.of(of("hydrogen"), 100);

        assertFalse(BlendConservation.elementsBalanced(water, hydrogen, List.of()),
                "the oxygen of the water went nowhere");
        assertThrows(IllegalStateException.class,
                () -> BlendConservation.check(water, hydrogen, 0, List.of()));
    }

    @Test
    void anAcidAndABaseMakeASaltAndWater() {
        Blend reactants = Blend.of(of("hydrogen chloride"), 100).plus(Blend.of(of("hydroxide"), 100));
        Blend products = Blend.of(of("chloride"), 100).plus(Blend.of(of("water"), 100));

        assertTrue(BlendConservation.balanced(reactants, products, 0, List.of()),
                "the two of them need no medium at all");
    }

    @Test
    void aCurrentPlatesCopper() {
        Blend ions = Blend.of(of("copper(II) ion"), 100);
        Blend metal = Blend.of(of("copper"), 100);

        assertTrue(BlendConservation.balanced(ions, metal, 200, List.of()),
                "a hundred millibuckets of a doubly charged ion is two hundred electrons");
        assertFalse(BlendConservation.chargeBalanced(ions, metal, 100));
    }

    @Test
    void waterStandsAroundFreelyForTheMeasuredAmountsAsWell() {
        // Two methanols into one dimethyl ether leaves a water, which the medium may take.
        Blend reactants = Blend.of(of("methanol"), 200);
        Blend products = Blend.of(of("dimethyl ether"), 100);

        assertFalse(BlendConservation.elementsBalanced(reactants, products, List.of()));
        assertTrue(BlendConservation.elementsBalanced(reactants, products, List.of(of("water"))));
    }

    @Test
    void aMixtureIsTakenApartIntoItsSubstances() {
        // A bronze ingot is copper and tin in one pile, and the pile is exactly the two of them.
        Blend bronze = Blend.of(of("copper"), Fraction.of(75))
                .plus(Blend.of(of("tin"), Fraction.of(25)));
        Blend apart = Blend.of(of("copper"), Fraction.of(75))
                .plus(Blend.of(of("tin"), Fraction.of(25)));

        assertTrue(BlendConservation.balanced(bronze, apart, 0, List.of()));
        assertEquals(ElementAmounts.empty(), BlendConservation.difference(bronze, apart));
    }
}
