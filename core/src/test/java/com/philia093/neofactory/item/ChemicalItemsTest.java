package com.philia093.neofactory.item;

import com.philia093.neofactory.chemistry.Blend;
import com.philia093.neofactory.chemistry.Chemical;
import com.philia093.neofactory.chemistry.Fraction;
import com.philia093.neofactory.chemistry.Substance;
import com.philia093.neofactory.chemistry.Substances;
import com.philia093.neofactory.material.Materials;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the items the substances of the catalog travel as.
 * <p>
 * The rule under test is that a substance is an item of its own and not a label on a stack: every
 * substance of the catalog has one, the one it has carries that substance and nothing else and in the
 * amount its state is measured in - a hundred millibuckets for a solid, a thousand for everything that
 * flows - and an item the game already had for a substance is the one that is used rather than a second
 * one being made.
 */
class ChemicalItemsTest {

    private static final Substances CATALOG = Substances.starter();

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    private static Item of(String name) {
        Item item = ChemicalItems.item(CATALOG.byName(name));
        assertNotNull(item, "no item stands for " + name);
        return item;
    }

    @Test
    void everySubstanceOfTheCatalogHasAnItemOfItsOwn() {
        assertEquals(CATALOG.count(), ChemicalItems.count(),
                "the items of the chemistry and the catalog are the same list");
        assertTrue(CATALOG.count() > 40, "the catalog holds the substances of the industry");
    }

    @Test
    void anItemOfASubstanceCarriesThatSubstanceAndNothingElse() {
        Blend pile = of("iron").chemicals();

        assertEquals(1, pile.components().size(), "one item is one substance");
        assertTrue(pile.components().containsKey(Chemical.parse("[Fe]")));
        assertEquals(Fraction.of(100), pile.total(), "a metal is a dust of a hundred millibuckets");
    }

    @Test
    void aSolidTravelsAsADustAndEverythingElseInACell() {
        assertTrue(of("carbon").name().endsWith("_dust"), "carbon is a solid: " + of("carbon").name());
        assertTrue(of("carbon monoxide").name().endsWith("_cell"),
                "a gas travels in a cell: " + of("carbon monoxide").name());
        assertEquals(Fraction.of(100), of("carbon").chemicals().total());
        assertEquals(Fraction.of(1000), of("carbon monoxide").chemicals().total());
    }

    @Test
    void aSubstanceTheGameAlreadyHadAnItemForKeepsIt() {
        assertSame(Items.WATER_CELL, of("water"), "the cell of water is the cell of the fluid");
        assertEquals("iron_dust", of("iron").name(), "the dust of the metal is the dust of iron");
        assertSame(Materials.IRON.dust(), of("iron"), "and the industry already had it");
    }

    @Test
    void theSubstanceOfAnItemComesBack() {
        Substance substance = ChemicalItems.substanceOf(of("carbon monoxide"));

        assertNotNull(substance);
        assertEquals("carbon monoxide", substance.name());
        assertFalse(substance.phase() == com.philia093.neofactory.chemistry.Phase.SOLID);
    }

    @Test
    void anItemThatIsNoSubstanceIsNoSubstance() {
        assertFalse(ChemicalItems.isChemical(Items.STICK));
        assertFalse(ChemicalItems.isChemical(Items.AIR));
        assertFalse(ChemicalItems.isChemical(null));
        assertEquals(null, ChemicalItems.substanceOf(Items.STICK));
    }
}
