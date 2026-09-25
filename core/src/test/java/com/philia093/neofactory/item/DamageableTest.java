package com.philia093.neofactory.item;

import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a piece of the workshop does while it is used.
 * <p>
 * Durability is asked of the stack and not of a tool, because a tool is not the only thing that wears
 * out: a mortar of the chemist, a screwdriver of a workshop or a part inside a machine declares its
 * own life the same way, see {@link Damageable}. What a case can check without a window is that the
 * damage counts up to the life of the piece and stops there, that a thing which never wears out keeps
 * its zero, that two stacks only merge while they are worn the same and that a copy keeps what a piece
 * has taken - the last two are also what a stored inventory relies on, see {@code SaveTags}.
 */
class DamageableTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void aFreshToolHasItsWholeLifeLeft() {
        ItemStack pickaxe = ItemStack.of(Items.IRON_PICKAXE, 1);

        assertTrue(Items.IRON_PICKAXE.isTool(), "a pickaxe is a tool");
        assertTrue(pickaxe.isDamageable(), "and it wears out");
        assertEquals(Items.IRON_TOOL_DURABILITY, pickaxe.maxDamage());
        assertEquals(0, pickaxe.damage(), "a piece that is handed out is new");
        assertEquals(Items.IRON_TOOL_DURABILITY, pickaxe.remainingDamage());
        assertEquals(0.0f, pickaxe.wear());
        assertFalse(pickaxe.isBroken());
    }

    @Test
    void damageCountsUpToTheEndOfTheLife() {
        ItemStack pickaxe = ItemStack.of(Items.DIAMOND_PICKAXE, 1);

        assertEquals(1, pickaxe.applyDamage(1), "one use is taken");
        assertEquals(1, pickaxe.damage());
        assertEquals(Items.DIAMOND_TOOL_DURABILITY - 1, pickaxe.remainingDamage());
        assertFalse(pickaxe.isBroken(), "a tool that dug once still has a life ahead of it");

        pickaxe.setDamage(Items.DIAMOND_TOOL_DURABILITY - 2);
        assertEquals(2, pickaxe.applyDamage(5), "the last two uses are what is left");
        assertTrue(pickaxe.isBroken());
        assertEquals(0, pickaxe.applyDamage(1), "and a piece that is used up takes nothing more");
        assertEquals(1.0f, pickaxe.wear());
    }

    @Test
    void aMaterialNeverWearsOut() {
        ItemStack stone = ItemStack.of(Items.STONE, 8);

        assertFalse(stone.isDamageable(), "a block does not wear out");
        assertEquals(0, stone.maxDamage());
        assertEquals(0, stone.applyDamage(3), "so nothing can be taken from it");
        assertEquals(0, stone.damage());
        assertFalse(stone.isBroken(), "and it can never be used up");
        assertEquals(0.0f, stone.wear());
    }

    @Test
    void anythingThatWearsDeclaresItsOwnLife() {
        // The mortar of a chemist: it is not a tool - it mines nothing, so it has no kind and no mining
        // level - and it still wears out, which is why the life is declared by the item itself. The item
        // is built by the case and never registered, so its id is only a number of this test.
        Item mortar = Item.builder(4000, "mortar")
                .displayName("Mortar")
                .maxStackSize(Item.DEFAULT_MAX_STACK)
                .maxDamage(64)
                .build();
        ItemStack stack = ItemStack.of(mortar, 2);

        assertFalse(mortar.isTool(), "a mortar is not a tool of the mine");
        assertTrue(stack.isDamageable(), "and it wears anyway");
        assertEquals(64, stack.maxDamage());
        stack.setDamage(30);
        assertEquals(34, stack.remainingDamage());
        assertEquals(30.0f / 64.0f, stack.wear(), 1.0e-6f);
    }

    @Test
    void aWornStackDoesNotMergeWithANewOne() {
        Item mortar = Item.builder(4000, "mortar")
                .maxStackSize(Item.DEFAULT_MAX_STACK)
                .maxDamage(64)
                .build();
        ItemStack fresh = ItemStack.of(mortar, 2);
        ItemStack worn = ItemStack.of(mortar, 2);
        worn.setDamage(30);

        assertTrue(fresh.isStackableWith(ItemStack.of(mortar, 1)), "two new pieces are one pile");
        assertFalse(fresh.isStackableWith(worn), "a worn piece is a piece of its own");
        assertEquals(worn, worn.copy(), "and a copy is worn the same way");
        assertEquals(mortar.maxDamage(), worn.copy().maxDamage());
    }

    @Test
    void theSharedEmptyStackCannotWear() {
        assertThrows(IllegalArgumentException.class, () -> ItemStack.EMPTY.setDamage(1));
        assertEquals(0, ItemStack.EMPTY.damage());
        assertEquals(0, ItemStack.EMPTY.applyDamage(1), "an empty slot has nothing to wear");
        assertTrue(ItemStack.EMPTY.copy().isEmpty(), "and it owes no life either");
    }
}
