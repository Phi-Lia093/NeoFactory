package com.philia093.neofactory.material;

import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the id space of the items the materials bring.
 * <p>
 * The id of an item is permanent - a stored inventory and a dropped item spell it out - so two
 * things have to stay true while the game grows: the two ingots that moved out of {@code Items}
 * keep the numbers they were saved with, and every other shape takes a number from a run that
 * starts right above the hand written items and has no hole in it. A material that is inserted in
 * the middle of {@link Materials} would break the second one, which is why this test exists.
 */
class MaterialIdSpaceTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theIngotsKeptTheIdsTheyWereSavedWith() {
        assertEquals(Items.IRON_INGOT_ID, Materials.IRON.ingot().id(),
                "the iron ingot moved out of Items and has to keep its number");
        assertEquals(Items.GOLD_INGOT_ID, Materials.GOLD.ingot().id(),
                "the gold ingot moved out of Items and has to keep its number");
    }

    @Test
    void onlyTheOldIngotsSitBelowTheRunOfNewIds() {
        List<Integer> below = new ArrayList<>();
        for (Material material : Materials.all()) {
            for (MaterialForm form : material.forms()) {
                int id = material.form(form).id();
                if (id < Items.NEXT_FREE_ID) {
                    below.add(id);
                }
            }
        }
        below.sort(null);
        assertEquals(List.of(Items.IRON_INGOT_ID, Items.GOLD_INGOT_ID), below,
                "only the two ingots that moved out of Items declare an id of their own");
    }

    @Test
    void theOtherShapesAreARunWithoutAGap() {
        List<Item> appended = new ArrayList<>();
        for (Material material : Materials.all()) {
            for (MaterialForm form : material.forms()) {
                Item item = material.form(form);
                if (item.id() >= Items.NEXT_FREE_ID) {
                    appended.add(item);
                }
            }
        }
        appended.sort(Comparator.comparingInt(Item::id));

        int expected = Items.NEXT_FREE_ID;
        for (Item item : appended) {
            assertEquals(expected, item.id(), "a gap in the run of material ids at " + item.name());
            expected++;
        }
        assertEquals(Items.NEXT_FREE_ID + appended.size(), expected, "the run has to stay tight");
        assertTrue(appended.size() > 100, "the materials bring a lot of items with them");
    }

    @Test
    void noTwoItemsOfTheGameShareAnId() {
        Set<Integer> seen = new HashSet<>();
        for (Item item : ItemRegistry.all()) {
            assertTrue(seen.add(item.id()), "two items share the id " + item.id());
        }
    }

    @Test
    void everyMaterialItemIsInTheTableOfTheGame() {
        int fromMaterials = 0;
        for (Material material : Materials.all()) {
            for (Item item : material.items()) {
                assertEquals(item, ItemRegistry.byId(item.id()), item.name());
                assertEquals(item, ItemRegistry.byName(item.name()), item.name());
                fromMaterials++;
            }
        }
        assertTrue(fromMaterials > 0, "the materials brought no items at all");
    }
}
