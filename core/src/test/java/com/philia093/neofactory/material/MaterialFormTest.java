package com.philia093.neofactory.material;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the catalogue of shapes, the list a material picks from.
 * <p>
 * The shape owns what every material of it shares: the name of the item, the picture and the
 * stack size. Those three are read by the material registry while the items are built, so a shape
 * that names the same picture twice, or a name that collides with another shape, would quietly
 * give two items the same look. That is what this class watches.
 */
class MaterialFormTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void theCatalogueHoldsTheShapesTheArtPackHas() {
        assertEquals(20, MaterialForm.values().length,
                "the shapes of the game are the twenty the art pack holds");
    }

    @Test
    void everyShapeNamesItsItemAndItsPicture() {
        Set<String> names = new HashSet<>();
        Set<String> textures = new HashSet<>();
        for (MaterialForm form : MaterialForm.values()) {
            assertFalse(form.formName().isBlank(), form.name());
            assertFalse(form.displayName().isBlank(), form.name());
            assertEquals("_" + form.formName(), form.itemSuffix(), form.name());
            assertTrue(form.texture().startsWith(Item.ITEM_FOLDER), form.texture());
            assertTrue(form.maxStackSize() >= 1, form.name());

            assertTrue(names.add(form.formName()), "two shapes are called " + form.formName());
            assertTrue(names.add(form.displayName()), "two shapes are named " + form.displayName());
            assertTrue(textures.add(form.texture()), "two shapes share the picture " + form.texture());
        }
    }

    @Test
    void aShapeNamesAnOverlayOnlyWhenItIsDrawnFromTwoLayers() {
        int withOverlay = 0;
        for (MaterialForm form : MaterialForm.values()) {
            assertEquals(form.hasOverlay(), form.overlayTexture() != null, form.name());
            if (form.hasOverlay()) {
                assertTrue(form.overlayTexture().startsWith(Item.ITEM_FOLDER),
                        form.overlayTexture());
                assertFalse(form.overlayTexture().equals(form.texture()), form.name());
                withOverlay++;
            } else {
                assertNull(form.overlayTexture(), form.name());
            }
        }
        assertEquals(1, withOverlay, "the fine wire is the shape that covers its own core");
        assertTrue(MaterialForm.FINE_WIRE.hasOverlay());
    }

    @Test
    void aCellIsTheOnlyShapeThatDoesNotStack() {
        assertEquals(Item.SINGLE_ITEM_STACK, MaterialForm.FLUID_CELL.maxStackSize());
        for (MaterialForm form : MaterialForm.values()) {
            if (form != MaterialForm.FLUID_CELL) {
                assertEquals(Item.DEFAULT_MAX_STACK, form.maxStackSize(), form.name());
            }
        }
    }

    @Test
    void aMetalComesInEveryShapeButTheCell() {
        Set<MaterialForm> shapes = MaterialKind.METAL.defaultForms();
        assertEquals(MaterialForm.values().length - 1, shapes.size(),
                "the cell is the shape a material gets when it can be molten");
        assertFalse(shapes.contains(MaterialForm.FLUID_CELL));
        for (MaterialForm form : MaterialForm.values()) {
            if (form != MaterialForm.FLUID_CELL) {
                assertTrue(shapes.contains(form), "a metal does not come in " + form);
            }
        }
    }

    @Test
    void aShapeCannotBeDeclaredTwiceOrWithAnIdThatIsNotOne() {
        assertThrows(IllegalArgumentException.class, () -> Material.builder("test", "Test")
                .color(Color.WHITE).item(MaterialForm.DUST, -1));
        assertThrows(IllegalArgumentException.class, () -> Material.builder("test", "Test")
                .color(Color.WHITE).item(MaterialForm.DUST, 7).item(MaterialForm.DUST, 8));
    }
}
