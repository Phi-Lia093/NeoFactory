package com.philia093.neofactory.gui;

import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.recipe.CraftingField;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the geometry of the screen of the table of the workshop.
 * <p>
 * The field of nine cells, the result beside it and the rows of the player all have to lie on one
 * panel without sharing a pixel, and a click has to land on the cell the player sees. The test also
 * pins down the one thing the two screens of a container differ in: every cell of the field is a plain
 * slot of the block, while the result is a slot nothing may be put into.
 */
class CraftingLayoutTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aTableShowsItsFieldItsResultAndTheRowsOfThePlayer() {
        CraftingField field = new CraftingField(new Inventory(CraftingField.CELLS));
        PlayerInventory player = new PlayerInventory();

        ContainerLayout layout = CraftingLayout.of(field.resultSlot(), field.contents(), player);

        assertEquals(CraftingField.CELLS + 1 + PlayerInventory.SLOT_COUNT, layout.size(),
                "nine cells, the result and the slots of the player");
        assertEquals(PanelTextures.PANEL_WIDTH, layout.panelWidth(),
                "the panel is as wide as the art of the interface");
        assertEquals(CraftingLayout.HOTBAR_Y + Slot.size() + ContainerLayout.PADDING,
                layout.panelHeight(), "the panel ends one padding below the hotbar row");
        assertSame(field.contents(), layout.slots().get(0).inventory(),
                "the first cell is a cell of the field");
        assertTrue(layout.slots().get(CraftingField.CELLS).isOutput(),
                "the result is a slot nothing may be put into");
    }

    @Test
    void everyCellLiesOnThePanelAndNoTwoShareAPixel() {
        CraftingField field = new CraftingField(new Inventory(CraftingField.CELLS));
        ContainerLayout layout = CraftingLayout.of(field.resultSlot(), field.contents(),
                new PlayerInventory());

        for (Slot slot : layout.slots()) {
            assertTrue(layout.contains(slot.x(), slot.y()), "a cell lies inside the panel: " + slot);
            assertTrue(layout.contains(slot.x() + Slot.size() - 1, slot.y() + Slot.size() - 1),
                    "a cell ends inside the panel: " + slot);
        }
        List<Slot> slots = layout.slots();
        for (int first = 0; first < slots.size(); first++) {
            for (int second = first + 1; second < slots.size(); second++) {
                assertFalse(overlaps(slots.get(first), slots.get(second)),
                        "two cells lie on each other: " + slots.get(first) + " and "
                                + slots.get(second));
            }
        }
    }

    @Test
    void aClickLandsOnTheCellThePlayerSees() {
        CraftingField field = new CraftingField(new Inventory(CraftingField.CELLS));
        ContainerLayout layout = CraftingLayout.of(field.resultSlot(), field.contents(),
                new PlayerInventory());

        Slot first = layout.slotAt(CraftingLayout.FIELD_X, CraftingLayout.FIELD_Y);
        assertNotNull(first, "the upper left cell of the field");
        assertEquals(0, first.index());

        Slot lowerRight = layout.slotAt(
                CraftingLayout.FIELD_X + (CraftingField.COLUMNS - 1) * ContainerLayout.SLOT_PITCH,
                CraftingLayout.FIELD_Y + (CraftingField.ROWS - 1) * ContainerLayout.SLOT_PITCH);
        assertNotNull(lowerRight, "the lower right cell of the field");
        assertEquals(CraftingField.CELLS - 1, lowerRight.index());

        Slot result = layout.slotAt(CraftingLayout.RESULT_X, CraftingLayout.RESULT_Y);
        assertNotNull(result, "the result stands beside the middle row of the field");
        assertTrue(result.isOutput());

        // The band between the field and the rows of the player belongs to no cell at all.
        int gapY = CraftingLayout.FIELD_Y + CraftingField.ROWS * ContainerLayout.SLOT_PITCH + 2;
        assertNull(layout.slotAt(CraftingLayout.FIELD_X, gapY), "the gap is no cell");

        assertEquals(CraftingLayout.STORAGE_Y,
                layout.slots().get(CraftingField.CELLS + 1).y(), "the storage row of the player");
        assertEquals(CraftingLayout.HOTBAR_Y, layout.slots().get(layout.size() - 1).y(),
                "the hotbar row of the player");
    }

    /** {@code true} when two cells share at least one pixel. */
    private static boolean overlaps(Slot first, Slot second) {
        return first.x() < second.x() + Slot.size() && second.x() < first.x() + Slot.size()
                && first.y() < second.y() + Slot.size() && second.y() < first.y() + Slot.size();
    }
}
