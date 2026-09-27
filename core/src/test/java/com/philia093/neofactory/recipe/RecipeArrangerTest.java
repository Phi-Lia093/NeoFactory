package com.philia093.neofactory.recipe;

import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.material.Materials;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks what a click on a recipe does to the field of a table.
 * <p>
 * Three things have to hold without a screen in front of them: the pattern lands where the recipe says,
 * the items come out of the inventory of the player and out of nothing else, and an ingredient nobody has
 * is reported instead of being invented. A creative player pays for nothing, so the same click fills the
 * cells there as well, see {@code GameMode}.
 */
class RecipeArrangerTest {

    /** The pattern of a pickaxe of iron: three ingots over two sticks. */
    private static final String PICKAXE = "{ \"pattern\": [\"III\", \" S \", \" S \"],"
            + " \"key\": { \"I\": \"iron_ingot\", \"S\": \"stick\" },"
            + " \"result\": { \"item\": \"iron_pickaxe\" } }";

    /** A recipe that takes a log as a bag of ingredients. */
    private static final String PLANKS = "{ \"ingredients\": [\"log_oak\"],"
            + " \"result\": { \"item\": \"planks_oak\", \"count\": 4 } }";

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @BeforeEach
    void registerRecipes() {
        RecipeRegistry.clear();
        RecipeRegistry.register(RecipeLoader.parse(RecipeType.CRAFTING_SHAPED, "iron_pickaxe", PICKAXE));
        RecipeRegistry.register(RecipeLoader.parse(RecipeType.CRAFTING_SHAPELESS, "oak_planks", PLANKS));
    }

    @Test
    void thePatternLandsWhereTheRecipeSaysAndTheFieldThenMatches() {
        PlayerInventory player = new PlayerInventory();
        player.set(0, ItemStack.of(Materials.IRON.ingot(), 5));
        player.set(1, ItemStack.of(Items.STICK, 4));
        CraftingField field = new CraftingField(new Inventory(CraftingField.CELLS));

        RecipeArranger.Arrangement arrangement = RecipeArranger.arrange(
                RecipeRegistry.byName("iron_pickaxe"), field.grid(), player, false);

        assertTrue(arrangement.isComplete(), "everything the recipe asks for was there");
        assertEquals(5, arrangement.placed());
        assertEquals(Materials.IRON.ingot(), field.contents().get(0).item(), "the ingots lie on top");
        assertEquals(Items.STICK, field.contents().get(4).item(), "the sticks stand below them");
        assertEquals(2, player.get(0).count(), "three ingots were taken out of the inventory");
        assertEquals(2, player.get(1).count(), "and two sticks");
        // The screen asks the field again after every click that changed it, see CraftingPart.
        field.refresh();
        assertTrue(field.makesSomething(), "the field holds the pattern now");
        assertEquals(Items.IRON_PICKAXE, field.resultSlot().get(0).item());
    }

    @Test
    void anItemNobodyHasIsReportedAndNotInvented() {
        PlayerInventory player = new PlayerInventory();
        player.set(0, ItemStack.of(Items.STICK, 4));
        CraftingField field = new CraftingField(new Inventory(CraftingField.CELLS));

        RecipeArranger.Arrangement arrangement = RecipeArranger.arrange(
                RecipeRegistry.byName("iron_pickaxe"), field.grid(), player, false);

        assertFalse(arrangement.isComplete(), "the ingots are missing");
        assertEquals(2, arrangement.placed(), "the two sticks were laid out");
        assertEquals(3, arrangement.missing().size(), "one entry per cell that stayed empty");
        assertEquals(Materials.IRON.ingot(), arrangement.missing().get(0));
        field.refresh();
        assertFalse(field.makesSomething(), "an incomplete pattern makes nothing");
    }

    @Test
    void whatAlreadyLiesRightIsLeftAlone() {
        PlayerInventory player = new PlayerInventory();
        player.set(0, ItemStack.of(Materials.IRON.ingot(), 4));
        player.set(1, ItemStack.of(Items.STICK, 2));
        Inventory cells = new Inventory(CraftingField.CELLS);
        cells.set(4, ItemStack.of(Items.STICK, 1));
        CraftingField field = new CraftingField(cells);
        ItemStack before = cells.get(4);

        RecipeArranger.arrange(RecipeRegistry.byName("iron_pickaxe"), field.grid(), player, false);

        assertSame(before, cells.get(4), "the stick that was already lying there was not moved");
        assertEquals(1, cells.get(4).count());
        assertEquals(1, player.get(0).count(), "three of the four ingots were taken");
        assertEquals(1, player.get(1).count(), "and one of the two sticks, because one was lying there");
        field.refresh();
        assertTrue(field.makesSomething(), "the pattern lies in the field");
    }

    @Test
    void aCreativePlayerPaysForNothing() {
        PlayerInventory player = new PlayerInventory();
        CraftingField field = new CraftingField(new Inventory(CraftingField.CELLS));

        RecipeArranger.Arrangement arrangement = RecipeArranger.arrange(
                RecipeRegistry.byName("iron_pickaxe"), field.grid(), player, true);

        assertTrue(arrangement.isComplete(), "a creative player has everything");
        assertTrue(player.isEmpty(), "and nothing was taken out of an empty inventory");
        field.refresh();
        assertTrue(field.makesSomething(), "the pattern lies in the field");
    }

    @Test
    void aRecipeOfABagIsLaidOutRowByRow() {
        PlayerInventory player = new PlayerInventory();
        player.set(8, ItemStack.of(Items.LOG_OAK, 1));
        CraftingField field = new CraftingField(new Inventory(CraftingField.CELLS));

        RecipeArranger.Arrangement arrangement = RecipeArranger.arrange(
                RecipeRegistry.byName("oak_planks"), field.grid(), player, false);

        assertTrue(arrangement.isComplete());
        assertEquals(Items.LOG_OAK, field.contents().get(0).item(), "the log lies in the first cell");
        assertTrue(player.isEmpty(), "the log left the inventory");
        field.refresh();
        assertEquals(Items.PLANKS_OAK, field.resultSlot().get(0).item(), "and the field makes planks");
    }

    @Test
    void aRecipeThatIsWiderThanTheGridIsReportedAsSuch() {
        Recipe pickaxe = RecipeRegistry.byName("iron_pickaxe");
        assertNotNull(pickaxe);

        InventoryGrid field = new InventoryGrid(new Inventory(4), 0, 2, 2);
        PlayerInventory player = new PlayerInventory();
        player.set(0, ItemStack.of(Materials.IRON.ingot(), 5));
        player.set(1, ItemStack.of(Items.STICK, 4));

        assertFalse(RecipeArranger.fits(pickaxe, field), "a pattern of nine cells is wider");
        RecipeArranger.Arrangement arrangement = RecipeArranger.arrange(pickaxe, field, player, false);
        assertFalse(arrangement.isComplete(), "and it cannot be laid out whole");
        assertTrue(RecipeArranger.fits(RecipeRegistry.byName("oak_planks"), field),
                "a single cell fits every field");
    }
}
