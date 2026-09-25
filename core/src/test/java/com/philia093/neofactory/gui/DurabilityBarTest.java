package com.philia093.neofactory.gui;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.item.Damageable;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the bar that shows how much life is left in a piece of an inventory slot.
 * <p>
 * The bar is drawn and nothing reads it, so what is worth checking is the arithmetic behind it: how
 * long the strip is, which colour it takes and that a piece which never wears out draws nothing.
 * A piece of this test is enough for all of that, because the bar asks a {@link Damageable} and not a
 * stack - which is what lets the mortar of a chemist and the screwdriver of a workshop show their life
 * the same way a pickaxe does.
 */
class DurabilityBarTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aFreshPieceShowsTheWholeBarInGreen() {
        assertEquals(DurabilityBar.WIDTH, DurabilityBar.widthOf(new Piece(100, 0)));
        assertColour(new Color(0.0f, 1.0f, 0.0f, 1.0f), DurabilityBar.colorOf(new Piece(100, 0)));
    }

    @Test
    void theStripShrinksWithTheLifeThatIsLeft() {
        assertEquals(13, DurabilityBar.widthOf(new Piece(100, 0)));
        assertEquals(7, DurabilityBar.widthOf(new Piece(100, 50)), "half a life, half a bar");
        assertEquals(0, DurabilityBar.widthOf(new Piece(100, 100)), "a used up piece shows no strip");
    }

    @Test
    void theColourWalksFromGreenOverYellowToRed() {
        assertColour(new Color(1.0f, 1.0f, 0.0f, 1.0f), DurabilityBar.colorOf(new Piece(100, 50)));
        assertColour(new Color(1.0f, 0.0f, 0.0f, 1.0f), DurabilityBar.colorOf(new Piece(100, 100)));
    }

    @Test
    void aPieceThatNeverWearsDrawsNothing() {
        Damageable stone = new Piece(0, 0);

        assertFalse(stone.isDamageable());
        assertEquals(0, DurabilityBar.widthOf(stone), "no life, no strip");
        assertColour(Color.WHITE, DurabilityBar.colorOf(stone));
    }

    @Test
    void theBarServesAStackOfTheGameAsWell() {
        // The very same arithmetic shows the life of a tool of the game, which is what the slot of an
        // inventory and the grid of the creative inventory draw, see GuiItemRenderer.
        ItemStack pickaxe = ItemStack.of(Items.IRON_PICKAXE, 1);
        pickaxe.setDamage(Items.IRON_TOOL_DURABILITY / 2);

        assertEquals(7, DurabilityBar.widthOf(pickaxe));
        assertColour(new Color(1.0f, 1.0f, 0.0f, 1.0f), DurabilityBar.colorOf(pickaxe));
    }

    @Test
    void theBarFitsIntoTheLowerLeftCornerOfAnIcon() {
        // The coordinates are measured upwards from the lower edge of an icon, the opposite of the
        // original game, so a bar that leaves the icon would mean a sign was turned around.
        assertTrue(DurabilityBar.OFFSET_Y >= 0, "the strip lies over the icon and not under it");
        assertTrue(DurabilityBar.OFFSET_Y + DurabilityBar.HEIGHT <= Constants.ITEM_ICON_SIZE,
                "and it lies inside it");
        assertTrue(DurabilityBar.OFFSET_X + DurabilityBar.WIDTH <= Constants.ITEM_ICON_SIZE,
                "a full bar is not wider than its icon");
    }

    /** Checks a colour channel by channel, because the hue of a bar is computed in float. */
    private static void assertColour(Color expected, Color actual) {
        assertEquals(expected.r, actual.r, 1.0e-3f, "red");
        assertEquals(expected.g, actual.g, 1.0e-3f, "green");
        assertEquals(expected.b, actual.b, 1.0e-3f, "blue");
        assertEquals(expected.a, actual.a, 1.0e-3f, "alpha");
    }

    /**
     * A piece of the test that is worn as far as a case asks.
     * <p>
     * It is not a stack and not an item: the bar only needs something that knows its life and how much
     * of it is gone, see {@link Damageable}.
     */
    private record Piece(int maxDamage, int damage) implements Damageable {

        @Override
        public int applyDamage(int amount) {
            throw new UnsupportedOperationException("the bar never wears a piece down");
        }
    }
}
