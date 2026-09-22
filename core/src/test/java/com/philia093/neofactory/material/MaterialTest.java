package com.philia093.neofactory.material;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a material is and what its items look like.
 * <p>
 * A material is a handful of values, so there is not much to test about one: what is worth
 * checking is that the values reach the items - the name, the colour, the formula, the picture of
 * the shape - because that is the whole point of the class. A material that declares a shape it
 * does not come in has to answer {@code null} for it, which is what lets a later kind of material
 * own a shorter list.
 */
class MaterialTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void everyMetalOfTheGameComesInTheShapesOfItsKind() {
        assertFalse(Materials.all().isEmpty(), "the game has to have materials");
        for (Material material : Materials.all()) {
            assertEquals(MaterialKind.METAL.defaultForms(), material.forms(),
                    material.name() + " does not come in the shapes of a metal");
        }
    }

    @Test
    void anItemIsNamedAfterItsMaterialAndItsShape() {
        assertEquals("iron_plate", Materials.IRON.plate().name());
        assertEquals("Iron Plate", Materials.IRON.plate().displayName());
        assertEquals("copper_gear", Materials.COPPER.gear().name());
        assertEquals("Copper Gear", Materials.COPPER.gear().displayName());
        assertEquals("tungsten_long_rod", Materials.TUNGSTEN.longRod().name());
        assertEquals("Tungsten Long Rod", Materials.TUNGSTEN.longRod().displayName());
    }

    @Test
    void aMaterialSpellsTheNameOfItsItemOut() {
        assertEquals("iron_dust", Materials.IRON.itemName(MaterialForm.DUST));
        assertEquals("iron_fine_wire", Materials.IRON.itemName(MaterialForm.FINE_WIRE));
        assertEquals("silver_cell", Materials.SILVER.itemName(MaterialForm.FLUID_CELL));
    }

    @Test
    void everyItemCarriesTheColourAndTheFormulaOfItsMaterial() {
        for (Material material : Materials.all()) {
            assertTrue(material.hasChemicalFormula(), material.name() + " has no formula");
            for (Item item : material.items()) {
                assertEquals(material.color(), item.tint(), item.name());
                assertEquals(material.chemicalFormula(), item.chemicalFormula(), item.name());
                assertTrue(item.hasChemicalFormula(), item.name());
            }
        }
    }

    @Test
    void aShapeWithoutAnItemIsSimplyNotThere() {
        Material nuggetsOnly = Material.builder("test", "Test").color(Color.WHITE)
                .onlyForms(MaterialForm.NUGGET).build();

        assertTrue(nuggetsOnly.has(MaterialForm.NUGGET));
        assertFalse(nuggetsOnly.has(MaterialForm.PLATE));
        assertFalse(nuggetsOnly.has(null));
        assertNull(nuggetsOnly.plate(), "a shape the material does not come in has no item");
        // The shape is declared but nothing was registered for it, which is a mistake in the
        // setup and not an empty answer.
        assertThrows(IllegalStateException.class, nuggetsOnly::nugget);
    }

    @Test
    void aMaterialNeedsAColourAndKeepsItsOwnCopy() {
        Color given = new Color(1.0f, 0.0f, 0.0f, 1.0f);
        Material material = Material.builder("test", "Test").color(given)
                .onlyForms(MaterialForm.NUGGET).build();

        assertEquals(given, material.color());
        assertNotSame(given, material.color(), "the colour belongs to the material, not the caller");
        // The copy was taken while the material was built: a caller that paints its own colour
        // afterwards does not repaint the items of the material.
        given.set(0.0f, 0.0f, 1.0f, 1.0f);
        assertEquals(new Color(1.0f, 0.0f, 0.0f, 1.0f), material.color());

        assertThrows(IllegalArgumentException.class,
                () -> Material.builder("test", "Test").onlyForms(MaterialForm.NUGGET).build());
        assertThrows(IllegalArgumentException.class,
                () -> Material.builder("test", "Test").color(Color.WHITE).onlyForms());
    }

    @Test
    void aMaterialThatDeclaresNoFormulaDrawsNoSecondLine() {
        Material plain = Material.builder("test", "Test").color(Color.WHITE)
                .forms(MaterialForm.DUST).build();

        assertFalse(plain.hasChemicalFormula());
        assertEquals("", plain.chemicalFormula());
    }

    @Test
    void theMaterialsAreFoundByName() {
        assertEquals(Materials.IRON, MaterialRegistry.byName("iron"));
        assertEquals(Materials.GOLD, MaterialRegistry.byName("gold"));
        assertNull(MaterialRegistry.byName("unobtainium"));
        assertNull(MaterialRegistry.byName(null));
        assertEquals(Materials.all().size(), MaterialRegistry.count());
    }

    @Test
    void everyMaterialOwnsEveryItemItRegistered() {
        for (Material material : Materials.all()) {
            assertEquals(material.forms().size(), material.items().size(), material.name());
            for (MaterialForm form : material.forms()) {
                Item item = material.form(form);
                assertNotNull(item, material.name() + " has no " + form);
                assertEquals(material.itemName(form), item.name());
                assertTrue(material.items().contains(item), item.name());
            }
        }
    }
}
