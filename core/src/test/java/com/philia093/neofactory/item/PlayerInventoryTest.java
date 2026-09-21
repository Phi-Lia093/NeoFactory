package com.philia093.neofactory.item;

import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the hotbar selection.
 * <p>
 * The wheel of the mouse is what changes the selection, so the direction of
 * {@link PlayerInventory#scrollSelection(int)} is what the player feels: rolling
 * forwards has to walk towards the first slot, and the selection may never leave
 * the hotbar row.
 */
class PlayerInventoryTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void scrollingForwardsWalksTowardsTheFirstSlot() {
        PlayerInventory inventory = new PlayerInventory();
        inventory.setSelectedSlot(0);

        // Rolling forwards is what the screen turns into a step of -1.
        inventory.scrollSelection(-1);
        assertEquals(PlayerInventory.HOTBAR_SLOTS - 1, inventory.selectedSlot());

        inventory.scrollSelection(1);
        assertEquals(0, inventory.selectedSlot());
    }

    @Test
    void theSelectionNeverLeavesTheHotbar() {
        PlayerInventory inventory = new PlayerInventory();
        inventory.setSelectedSlot(PlayerInventory.SLOT_COUNT + 3);

        assertTrue(inventory.isHotbarSlot(inventory.selectedSlot()));
        inventory.scrollSelection(-50);
        assertTrue(inventory.isHotbarSlot(inventory.selectedSlot()));
        inventory.scrollSelection(50);
        assertTrue(inventory.isHotbarSlot(inventory.selectedSlot()));
    }

    @Test
    void theHeldStackFollowsTheSelection() {
        PlayerInventory inventory = new PlayerInventory();
        inventory.set(1, ItemStack.of(Items.DIAMOND, 3));

        inventory.setSelectedSlot(1);
        assertEquals(Items.DIAMOND, inventory.heldStack().item());
        assertEquals(3, inventory.heldStack().count());

        inventory.setSelectedSlot(2);
        assertTrue(inventory.heldStack().isEmpty());
    }
}
