package com.philia093.neofactory.recipe;

import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the recipes of the game: the files that ship with it and the shapes a file
 * may have.
 * <p>
 * Every file below {@code assets/recipes} is parsed here, so a typo in a name or a
 * missing result fails the build instead of showing up as a recipe that silently does
 * nothing. The shapes that no file of the game uses yet - alternatives, blank symbols
 * - are checked with hand written text.
 */
class RecipeLoaderTest {

    /** Assets of the project, the tests run inside the core module. */
    private static final Path RECIPES = Path.of("..", "assets", RecipeLoader.FOLDER);

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void everyFileOfTheGameCanBeRead() throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(RECIPES)) {
            for (Path file : stream.toList()) {
                if (file.getFileName().toString().endsWith(RecipeLoader.EXTENSION)) {
                    files.add(file);
                }
            }
        }
        assertFalse(files.isEmpty(), "the game ships recipes");

        for (Path file : files) {
            RecipeType type = RecipeType.byName(file.getParent().getFileName().toString());
            assertNotNull(type, "the folder of " + file + " names a type");
            String name = file.getFileName().toString();
            name = name.substring(0, name.length() - RecipeLoader.EXTENSION.length());

            Recipe recipe = RecipeLoader.parse(type, name, Files.readString(file,
                    StandardCharsets.UTF_8));

            assertEquals(type, recipe.type());
            assertEquals(name, recipe.name());
            assertFalse(recipe.result().isEmpty(), file + " makes something");
        }
    }

    @Test
    void aLogIsTurnedIntoFourPlanks() throws IOException {
        Recipe recipe = read(RecipeType.CRAFTING_SHAPELESS, "oak_planks");

        assertTrue(recipe instanceof ShapelessRecipe);
        assertEquals(4, recipe.result().count());
        assertTrue(recipe.result().sameItem(ItemStack.of(Items.PLANKS_OAK, 1)));

        Inventory field = new Inventory(4);
        field.set(0, ItemStack.of(Items.LOG_OAK, 3));
        RecipeGrid grid = new InventoryGrid(field, 0, 2, 2);
        assertTrue(recipe.matches(grid), "a log is enough");

        recipe.consume(grid);
        assertEquals(2, field.get(0).count(), "one log was used");
    }

    @Test
    void aShapelessRecipeNeedsOnePlacePerIngredient() throws IOException {
        Recipe recipe = read(RecipeType.CRAFTING_SHAPELESS, "clay");
        Inventory field = new Inventory(4);
        RecipeGrid grid = new InventoryGrid(field, 0, 2, 2);

        // Four balls in a single slot are one filled place: not four, and a single craft
        // must not eat the whole stack either.
        field.set(0, ItemStack.of(Items.CLAY_BALL, 4));
        assertFalse(recipe.matches(grid), "one place is not four");

        // Spread over the field the recipe works.
        field.set(0, ItemStack.of(Items.CLAY_BALL, 1));
        field.set(1, ItemStack.of(Items.CLAY_BALL, 1));
        field.set(2, ItemStack.of(Items.CLAY_BALL, 1));
        field.set(3, ItemStack.of(Items.CLAY_BALL, 1));
        assertTrue(recipe.matches(grid), "one ball in each slot works");
        recipe.consume(grid);
        assertTrue(field.isEmpty(), "every ball was used, one per place");

        // Something the recipe does not want keeps it from matching.
        field.set(0, ItemStack.of(Items.CLAY_BALL, 1));
        field.set(1, ItemStack.of(Items.STONE, 1));
        field.set(2, ItemStack.of(Items.CLAY_BALL, 1));
        field.set(3, ItemStack.of(Items.CLAY_BALL, 1));
        assertFalse(recipe.matches(grid), "a stone is in the way");

        // Three places are not enough either.
        field.clear();
        field.set(0, ItemStack.of(Items.CLAY_BALL, 1));
        field.set(1, ItemStack.of(Items.CLAY_BALL, 1));
        field.set(2, ItemStack.of(Items.CLAY_BALL, 1));
        assertFalse(recipe.matches(grid), "three places are not four");
    }

    @Test
    void aPatternIsFoundWhereverItSits() throws IOException {
        Recipe recipe = read(RecipeType.CRAFTING_SHAPED, "stick");
        Inventory field = new Inventory(4);
        RecipeGrid grid = new InventoryGrid(field, 0, 2, 2);

        // Two planks above each other, in the left column.
        field.set(0, ItemStack.of(Items.PLANKS_OAK, 1));
        field.set(2, ItemStack.of(Items.PLANKS_OAK, 1));
        assertTrue(recipe.matches(grid), "the pattern fits the left column");

        // The same pattern in the right column.
        field.clear();
        field.set(1, ItemStack.of(Items.PLANKS_OAK, 1));
        field.set(3, ItemStack.of(Items.PLANKS_OAK, 1));
        assertTrue(recipe.matches(grid), "the pattern fits the right column");

        // Something beside the pattern keeps it from matching.
        field.set(0, ItemStack.of(Items.DIRT, 1));
        assertFalse(recipe.matches(grid), "the field has something in the way");
    }

    @Test
    void alternativesAreAccepted() {
        Recipe recipe = RecipeLoader.parse(RecipeType.SMELTING, "wood",
                "{ \"ingredient\": [\"log_oak\", \"planks_oak\"],"
                        + " \"result\": { \"item\": \"charcoal\" } }");
        Inventory input = new Inventory(1);
        RecipeGrid grid = new InventoryGrid(input, 0, 1, 1);

        input.set(0, ItemStack.of(Items.LOG_OAK, 1));
        assertTrue(recipe.matches(grid), "the first alternative");

        input.set(0, ItemStack.of(Items.PLANKS_OAK, 1));
        assertTrue(recipe.matches(grid), "the second alternative");

        input.set(0, ItemStack.of(Items.STONE, 1));
        assertFalse(recipe.matches(grid), "an item that is not named");
    }

    @Test
    void aBlankSymbolAsksForAnEmptyPlace() {
        Recipe recipe = RecipeLoader.parse(RecipeType.CRAFTING_SHAPED, "ring",
                "{ \"pattern\": [\"X.X\"], \"key\": { \"X\": \"stone\" },"
                        + " \"result\": { \"item\": \"sandstone\" } }");
        Inventory field = new Inventory(9);
        RecipeGrid grid = new InventoryGrid(field, 0, 3, 3);
        field.set(0, ItemStack.of(Items.STONE, 1));
        field.set(1, ItemStack.of(Items.STONE, 1));
        field.set(2, ItemStack.of(Items.STONE, 1));

        assertFalse(recipe.matches(grid), "the blank place is occupied");

        field.set(1, ItemStack.EMPTY);
        assertTrue(recipe.matches(grid), "the blank place is free");
    }

    @Test
    void aFileThatDoesNotDescribeItsTypeIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> RecipeLoader.parse(
                RecipeType.CRAFTING_SHAPED, "broken",
                "{ \"pattern\": [\"X\"], \"result\": { \"item\": \"sandstone\" } }"));
        assertThrows(IllegalArgumentException.class, () -> RecipeLoader.parse(
                RecipeType.CRAFTING_SHAPELESS, "broken",
                "{ \"ingredients\": [\"unknown_item\"], \"result\": { \"item\": \"sandstone\" } }"));
        assertThrows(IllegalArgumentException.class, () -> RecipeLoader.parse(
                RecipeType.SMELTING, "broken", "{ \"ingredient\": \"iron_ore\" }"));
    }

    @Test
    void aSmeltingRecipeTakesASingleItem() throws IOException {
        Recipe recipe = read(RecipeType.SMELTING, "iron_ingot");

        assertTrue(recipe instanceof SmeltingRecipe);
        assertEquals(10.0f, ((SmeltingRecipe) recipe).seconds(), 0.001f);

        Inventory input = new Inventory(1);
        RecipeGrid grid = new InventoryGrid(input, 0, 1, 1);
        assertFalse(recipe.matches(grid), "an empty input smelts nothing");

        input.set(0, ItemStack.of(Items.IRON_ORE, 2));
        assertTrue(recipe.matches(grid));
        recipe.consume(grid);
        assertEquals(1, input.get(0).count(), "one ore was used");
    }

    @Test
    void aRecipeIsFoundThroughTheRegistry() throws IOException {
        RecipeRegistry.clear();
        RecipeRegistry.register(read(RecipeType.CRAFTING_SHAPELESS, "oak_planks"));
        Inventory field = new Inventory(4);
        field.set(0, ItemStack.of(Items.LOG_OAK, 1));
        RecipeGrid grid = new InventoryGrid(field, 0, 2, 2);

        Recipe found = RecipeRegistry.find(RecipeType.CRAFTING_SHAPELESS, grid);

        assertNotNull(found, "the registry found the recipe");
        assertEquals("oak_planks", found.name());
        assertNull(RecipeRegistry.find(RecipeType.CRAFTING_SHAPED, grid), "no shaped recipe fits");
        assertEquals(1, RecipeRegistry.count());

        RecipeRegistry.clear();
        assertEquals(0, RecipeRegistry.count());
    }

    /** Reads one of the recipes that ship with the game. */
    private static Recipe read(RecipeType type, String name) throws IOException {
        Path file = RECIPES.resolve(type.name()).resolve(name + RecipeLoader.EXTENSION);
        return RecipeLoader.parse(type, name, Files.readString(file, StandardCharsets.UTF_8));
    }
}
