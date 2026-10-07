package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks that an alloy is a mixture of substances and never a molecule, and that it is read the way a machine
 * has to read one: what stands in a pot of bronze is so much copper and so much tin, and nothing of the alloy
 * itself.
 */
class AlloyTest {

    @Test
    void anAlloyIsItsPartsAndTheShareOfEach() {
        Substances catalog = bronze();
        Alloy bronze = catalog.alloy("bronze");

        assertNotNull(bronze, "the catalog holds the alloy");
        assertEquals("[Cu].[Sn]", bronze.smiles(), "an alloy is its parts with dots between them");

        Blend hundred = bronze.blend(Fraction.of(100));
        assertEquals(Fraction.of(1100, 12), hundred.amountOf(catalog.byName("copper").chemical()),
                "eleven parts of twelve of a hundred of it are copper");
        assertEquals(Fraction.of(100, 12), hundred.amountOf(catalog.byName("tin").chemical()),
                "and the twelfth part of it is tin");
    }

    @Test
    void oneAlloyWrittenTwoWaysIsOneEntry() {
        Substances catalog = new Substances();
        Substance copper = catalog.register("copper", "[Cu]", Phase.SOLID).byName("copper");
        Substance tin = catalog.register("tin", "[Sn]", Phase.SOLID).byName("tin");
        catalog.registerAlloy(Alloy.of("bronze", Phase.SOLID, Alloy.part(copper, 11),
                Alloy.part(tin, 1)));

        // The same alloy with its parts the other way round: copper and tin, whichever way it is written.
        catalog.registerAlloy(Alloy.of("bronze", Phase.SOLID, Alloy.part(tin, 1),
                Alloy.part(copper, 11)));

        assertEquals(1, catalog.alloys().size(), "one alloy is one entry, however it was written");
    }

    @Test
    void anAlloyOfASubstanceTheCatalogDoesNotHoldIsRefused() {
        Substances catalog = new Substances();
        Substance sodium = new Substance("sodium", Chemical.parse("[Na]"), Phase.SOLID);
        Substance tin = catalog.register("tin", "[Sn]", Phase.SOLID).byName("tin");

        assertThrows(IllegalArgumentException.class,
                () -> catalog.registerAlloy(Alloy.of("solder", Phase.SOLID, Alloy.part(sodium, 1),
                        Alloy.part(tin, 1))),
                "an alloy is made of substances the catalog holds and of nothing else");
    }

    /** A catalog holding the two metals of bronze and the alloy itself. */
    private static Substances bronze() {
        Substances catalog = new Substances();
        catalog.register("copper", "[Cu]", Phase.SOLID);
        catalog.register("tin", "[Sn]", Phase.SOLID);
        catalog.registerAlloy(Alloy.of("bronze", Phase.SOLID,
                Alloy.part(catalog.byName("copper"), 11), Alloy.part(catalog.byName("tin"), 1)));
        return catalog;
    }
}
