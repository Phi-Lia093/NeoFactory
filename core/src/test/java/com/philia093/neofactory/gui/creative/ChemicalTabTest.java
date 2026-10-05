package com.philia093.neofactory.gui.creative;

import com.philia093.neofactory.item.ChemicalItems;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the tab the chemistry of the industry is listed in.
 * <p>
 * Every substance of the catalog is an item and every one of them stands in one tab together, so a
 * player who looks for the dust a recipe asks for looks in one place. The tab is checked for what it
 * holds and for what it leaves to the others: an item of a substance is listed there and nowhere else,
 * which is what keeps the tabs of the game a partition of the items and not a pile of overlaps, see
 * {@link CreativeRegistry}.
 */
class ChemicalTabTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    private static CreativeTab tab(String name) {
        for (CreativeTab tab : CreativeRegistry.tabs()) {
            if (tab.name().equals(name)) {
                return tab;
            }
        }
        throw new IllegalStateException("no tab named " + name);
    }

    @Test
    void theTabOfTheChemistryHoldsEverySubstanceOfTheCatalog() {
        CreativeTab chemical = tab("chemical");

        assertTrue(chemical.isItems(), "the tab lists a category");
        assertTrue(chemical.hasIcon(), "and carries a picture");
        for (Item item : ChemicalItems.all()) {
            assertTrue(chemical.matches(item), item.name() + " is a substance of the catalog");
        }
    }

    @Test
    void aSubstanceIsListedByThatTabAlone() {
        CreativeTab chemical = tab("chemical");

        for (CreativeTab other : CreativeRegistry.tabs()) {
            if (other == chemical || !other.isItems()) {
                continue;
            }
            for (Item item : ChemicalItems.all()) {
                assertFalse(other.matches(item), item.name() + " is listed by two tabs");
            }
        }
    }

    @Test
    void anItemThatIsNoSubstanceIsNoSubstance() {
        assertFalse(tab("chemical").matches(Items.STICK), "a stick is no substance of the catalog");
        assertFalse(tab("chemical").matches(Items.STONE), "and neither is a stone");
        assertFalse(tab("chemical").matches(Items.FURNACE), "nor a machine");
    }
}
