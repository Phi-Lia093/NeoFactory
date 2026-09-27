package com.philia093.neofactory.recipe;

import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.material.Materials;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the work field of the table of the workshop.
 * <p>
 * Three things are asked without a window and without a screen in front of them: which recipe nine
 * cells hold, what taking the product does to them, and that a pattern is looked for wherever it lies
 * inside the field. The last one is what lets a recipe of the two by two field of the player be made
 * on the table as well.
 */
class CraftingFieldTest {

    /** The shaped recipe of the game: two planks above each other make four sticks. */
    private static final String STICKS = "{ \"pattern\": [\"P\", \"P\"],"
            + " \"key\": { \"P\": \"planks_oak\" },"
            + " \"result\": { \"item\": \"stick\", \"count\": 4 } }";

    /** The shapeless recipe of the game: a log is four planks. */
    private static final String PLANKS = "{ \"ingredients\": [\"log_oak\"],"
            + " \"result\": { \"item\": \"planks_oak\", \"count\": 4 } }";

    /** A pattern of nine cells: a pickaxe of iron on sticks. */
    private static final String PICKAXE = "{ \"pattern\": [\"III\", \" S \", \" S \"],"
            + " \"key\": { \"I\": \"iron_ingot\", \"S\": \"stick\" },"
            + " \"result\": { \"item\": \"iron_pickaxe\" } }";

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @BeforeEach
    void registerRecipes() {
        RecipeRegistry.clear();
        RecipeRegistry.register(RecipeLoader.parse(RecipeType.CRAFTING_SHAPED, "stick", STICKS));
        RecipeRegistry.register(RecipeLoader.parse(RecipeType.CRAFTING_SHAPELESS, "oak_planks", PLANKS));
        RecipeRegistry.register(RecipeLoader.parse(RecipeType.CRAFTING_SHAPED, "iron_pickaxe", PICKAXE));
    }

    @Test
    void anEmptyFieldMakesNothing() {
        CraftingField field = new CraftingField(new Inventory(CraftingField.CELLS));

        assertFalse(field.makesSomething());
        assertNull(field.recipe());
        assertTrue(field.resultSlot().get(0).isEmpty(), "the result of an empty field is empty");
        assertTrue(field.take().isEmpty(), "an empty field gives nothing up");
    }

    @Test
    void aPatternIsFoundWhereverItLiesInTheField() {
        Inventory cells = new Inventory(CraftingField.CELLS);
        cells.set(4, ItemStack.of(Items.PLANKS_OAK, 2));
        cells.set(7, ItemStack.of(Items.PLANKS_OAK, 2));
        CraftingField field = new CraftingField(cells);

        assertNotNull(field.recipe(), "the pattern lies in the middle column of the field");
        assertEquals(4, field.resultSlot().get(0).count(), "two planks make four sticks");
    }

    @Test
    void aFieldOfNineCellsMakesAPatternOfNine() {
        Inventory cells = new Inventory(CraftingField.CELLS);
        for (int column = 0; column < 3; column++) {
            cells.set(column, ItemStack.of(Materials.IRON.ingot(), 3));
        }
        cells.set(4, ItemStack.of(Items.STICK, 2));
        cells.set(7, ItemStack.of(Items.STICK, 2));
        CraftingField field = new CraftingField(cells);

        assertEquals("iron_pickaxe", field.recipe().name());
        assertEquals(1, field.resultSlot().get(0).count());
    }

    @Test
    void takingTheProductGivesUpTheIngredients() {
        Inventory cells = new Inventory(CraftingField.CELLS);
        cells.set(0, ItemStack.of(Items.PLANKS_OAK, 2));
        cells.set(3, ItemStack.of(Items.PLANKS_OAK, 2));
        CraftingField field = new CraftingField(cells);

        ItemStack made = field.take();

        assertEquals(Items.STICK, made.item(), "the product of the field");
        assertEquals(4, made.count());
        assertEquals(1, cells.get(0).count(), "one plank of each place was used");
        assertEquals(1, cells.get(3).count());
        assertTrue(field.makesSomething(), "the planks that are left make another four sticks");
        assertEquals(4, field.resultSlot().get(0).count(), "the next result is already shown");
    }

    @Test
    void aBagOfItemsIsMadeWhenNoPatternFits() {
        Inventory cells = new Inventory(CraftingField.CELLS);
        cells.set(5, ItemStack.of(Items.LOG_OAK, 1));
        CraftingField field = new CraftingField(cells);

        assertNotNull(field.recipe(), "a log in any cell is enough");
        ItemStack made = field.take();

        assertEquals(Items.PLANKS_OAK, made.item());
        assertEquals(4, made.count());
        assertTrue(cells.get(5).isEmpty(), "the log was used up");
        assertFalse(field.makesSomething(), "an empty field makes nothing again");
    }

    @Test
    void aPatternBeatsABagOfTheSameItems() {
        // A recipe that takes planks as a bag and a pattern that asks for two of them: a field that holds
        // the pattern has to make the pattern, whatever order the two recipes were read in.
        RecipeRegistry.clear();
        RecipeRegistry.register(RecipeLoader.parse(RecipeType.CRAFTING_SHAPELESS, "planks_bag",
                "{ \"ingredients\": [\"planks_oak\"], \"result\": { \"item\": \"stick\" } }"));
        RecipeRegistry.register(RecipeLoader.parse(RecipeType.CRAFTING_SHAPED, "stick", STICKS));

        Inventory cells = new Inventory(CraftingField.CELLS);
        cells.set(0, ItemStack.of(Items.PLANKS_OAK, 2));
        cells.set(3, ItemStack.of(Items.PLANKS_OAK, 2));
        CraftingField field = new CraftingField(cells);

        assertEquals("stick", field.recipe().name(), "the pattern wins");
        assertEquals(4, field.resultSlot().get(0).count(), "and it makes its four sticks");
    }
}
