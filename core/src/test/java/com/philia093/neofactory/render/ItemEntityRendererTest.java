package com.philia093.neofactory.render;

import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks how a dropped item looks.
 * <p>
 * Both numbers here are easy to change by accident and both are visible: an icon that
 * fills the whole block reads as a block of its own, and an item that does not move at
 * all reads as part of the terrain. The animation is a float on purpose, so a test that
 * expects a rising and falling offset keeps a later change from turning the item back
 * into something that turns around like a wheel.
 */
class ItemEntityRendererTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aDroppedItemCoversLessThanABlock() {
        float expected = (float) (Constants.ITEM_ICON_SIZE / Math.sqrt(2.0));

        assertEquals(expected, ItemEntityRenderer.iconSize(), 1.0e-4f);
        assertTrue(ItemEntityRenderer.iconSize() < Constants.ITEM_ICON_SIZE);
    }

    @Test
    void aDroppedItemFloatsUpAndDown() {
        float period = 1.0f / ItemEntityRenderer.BOB_SPEED;

        assertEquals(0.0f, ItemEntityRenderer.bobOffset(0.0f), 1.0e-3f, "it starts at its place");
        assertTrue(ItemEntityRenderer.bobOffset(period * 0.25f) > 0.0f, "then it rises");
        assertTrue(ItemEntityRenderer.bobOffset(period * 0.75f) < 0.0f, "and falls below its place");
        assertEquals(0.0f, ItemEntityRenderer.bobOffset(period), 1.0e-3f,
                "one period later it is back where it started");
    }

    @Test
    void theFloatStaysInsideTheCell() {
        float highest = 0.0f;
        for (int step = 0; step <= 200; step++) {
            highest = Math.max(highest, Math.abs(ItemEntityRenderer.bobOffset(step * 0.01f)));
        }

        assertEquals(ItemEntityRenderer.BOB_BLOCKS * Constants.BLOCK_SIZE, highest, 1.0e-2f,
                "the item rises exactly as high as the constant says");
        assertTrue(highest < Constants.TILE_SIZE * 0.125f, "the offset stays small: " + highest);
    }
}
