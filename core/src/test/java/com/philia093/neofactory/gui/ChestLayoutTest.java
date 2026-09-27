package com.philia093.neofactory.gui;

import com.philia093.neofactory.blockentity.ChestBlockEntity;
import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.PlayerInventory;
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
 * Checks the geometry of the screen of a container.
 * <p>
 * The panel shows the slots of the block above the rows of the player, and a click has to land on the
 * very slot the player sees: every cell lies on the panel, no two of them share a pixel and a corner
 * that belongs to no cell answers with nothing. The layout is asked without a window, the way the
 * inventory screen is.
 */
class ChestLayoutTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aChestShowsItsOwnSlotsAndTheRowsOfThePlayer() {
        Inventory contents = new Inventory(ChestBlockEntity.SLOTS);
        PlayerInventory player = new PlayerInventory();

        ContainerLayout layout = ChestLayout.of(contents, player);

        assertEquals(ChestBlockEntity.SLOTS + PlayerInventory.SLOT_COUNT, layout.size(),
                "three rows of the chest and four rows of the player");
        assertEquals(PanelTextures.PANEL_WIDTH, layout.panelWidth(),
                "the panel is as wide as the art of the interface");
        assertEquals(ChestLayout.HOTBAR_Y + Slot.size() + ContainerLayout.PADDING,
                layout.panelHeight(), "the panel ends one padding below the hotbar row");
        assertTrue(ChestLayout.CONTAINER_Y < ChestLayout.STORAGE_Y
                        && ChestLayout.STORAGE_Y < ChestLayout.HOTBAR_Y,
                "the chest stands above the storage of the player and the hotbar below it");
        assertSame(contents, layout.slots().get(0).inventory(), "the first cell is a cell of the chest");
    }

    @Test
    void everyCellLiesOnThePanelAndNoTwoShareAPixel() {
        ContainerLayout layout = ChestLayout.of(new Inventory(ChestBlockEntity.SLOTS),
                new PlayerInventory());

        for (Slot slot : layout.slots()) {
            assertTrue(layout.contains(slot.x(), slot.y()),
                    "a cell lies inside the panel: " + slot);
            assertTrue(layout.contains(slot.x() + Slot.size() - 1, slot.y() + Slot.size() - 1),
                    "a cell ends inside the panel: " + slot);
        }
        List<Slot> slots = layout.slots();
        for (int first = 0; first < slots.size(); first++) {
            for (int second = first + 1; second < slots.size(); second++) {
                assertFalse(overlaps(slots.get(first), slots.get(second)),
                        "two cells lie on each other: " + slots.get(first) + " and " + slots.get(second));
            }
        }
    }

    @Test
    void aClickLandsOnTheCellThePlayerSees() {
        ContainerLayout layout = ChestLayout.of(new Inventory(ChestBlockEntity.SLOTS),
                new PlayerInventory());

        Slot first = layout.slotAt(ContainerLayout.PADDING, ChestLayout.CONTAINER_Y);
        assertNotNull(first, "the upper left cell of the chest");
        assertEquals(0, first.index());

        Slot last = layout.slotAt(
                ContainerLayout.PADDING + (ChestLayout.COLUMNS - 1) * ContainerLayout.SLOT_PITCH,
                ChestLayout.CONTAINER_Y + (ChestLayout.ROWS - 1) * ContainerLayout.SLOT_PITCH);
        assertNotNull(last, "the lower right cell of the chest");
        assertEquals(ChestBlockEntity.SLOTS - 1, last.index());

        // The band between the chest and the rows of the player belongs to no cell at all.
        int gapY = ChestLayout.CONTAINER_Y + ChestLayout.ROWS * ContainerLayout.SLOT_PITCH + 2;
        assertNull(layout.slotAt(ContainerLayout.PADDING, gapY), "the gap is no cell");

        // The storage of the player follows the chest, and the hotbar is the last row.
        Slot storage = layout.slots().get(ChestBlockEntity.SLOTS);
        assertEquals(ChestLayout.STORAGE_Y, storage.y());
        assertEquals(PlayerInventory.HOTBAR_SLOTS, storage.index());
        Slot hotbar = layout.slots().get(layout.size() - 1);
        assertEquals(ChestLayout.HOTBAR_Y, hotbar.y(),
                "the last cell is the right end of the hotbar row");
        assertEquals(PlayerInventory.HOTBAR_SLOTS - 1, hotbar.index());
    }

    /** {@code true} when two cells share at least one pixel. */
    private static boolean overlaps(Slot first, Slot second) {
        return first.x() < second.x() + Slot.size() && second.x() < first.x() + Slot.size()
                && first.y() < second.y() + Slot.size() && second.y() < first.y() + Slot.size();
    }
}
