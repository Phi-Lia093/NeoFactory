package com.philia093.neofactory.recipe;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.ToolType;
import com.philia093.neofactory.material.Materials;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that every tool of the game can be made and that the ladder of the game can be climbed.
 * <p>
 * A world is opened with a bare hand, and a hand may cut a tree and dig a hole, but the stone of a
 * landscape asks for a pickaxe of wood, the ore of iron for one of stone and obsidian for one of diamond,
 * see {@code HardnessMining} and {@code Blocks}. A recipe file that names an item nobody can make would
 * break that ladder, so every tool is offered the cells it asks for here and the whole chain - a log,
 * planks, sticks, a table, a wooden pickaxe, cobblestone, a stone pickaxe, iron, an iron pickaxe - is
 * walked through, one recipe at a time.
 * <p>
 * Every file below {@code assets/recipes} is read here the way the game reads them at startup, which is
 * also what makes a typo in a recipe of a tool this test's business instead of the audit's.
 */
class ToolRecipesTest {

    /** Assets of the project, the tests run inside the core module. */
    private static final Path RECIPES = Path.of("..", "assets", RecipeLoader.FOLDER);

    @BeforeAll
    static void registerGameData() throws IOException {
        TestRegistries.ensure();
        RecipeRegistry.clear();
        try (Stream<Path> files = Files.walk(RECIPES)) {
            for (Path file : files.toList()) {
                String fileName = file.getFileName().toString();
                if (!fileName.endsWith(RecipeLoader.EXTENSION)) {
                    continue;
                }
                RecipeType type = RecipeType.byName(file.getParent().getFileName().toString());
                String name = fileName.substring(0, fileName.length() - RecipeLoader.EXTENSION.length());
                RecipeRegistry.register(RecipeLoader.parse(type, name,
                        Files.readString(file, StandardCharsets.UTF_8)));
            }
        }
    }

    @Test
    void everyToolOfTheGameCanBeMade() {
        for (Item tool : toolsOfTheGame()) {
            assertNotNull(recipeOf(tool), tool.name() + " cannot be made by any recipe");
        }
    }

    @Test
    void theLadderFromALogLeadsToAnIronPickaxe() {
        Set<Item> owned = new LinkedHashSet<>();
        owned.add(Items.LOG_OAK);

        make(owned, "oak_planks");
        make(owned, "stick");
        make(owned, "crafting_table");
        make(owned, "wood_pickaxe");

        assertTrue(Items.WOOD_PICKAXE.toolLevel() >= Blocks.STONE.harvestLevel(),
                "a pickaxe of wood has to open the stone of a landscape");
        assertTrue(Items.WOOD_PICKAXE.toolLevel() < Blocks.IRON_ORE.harvestLevel(),
                "and it stops at the ore of iron");
        owned.add(Items.COBBLESTONE);

        make(owned, "stone_pickaxe");
        assertTrue(Items.STONE_PICKAXE.toolLevel() >= Blocks.IRON_ORE.harvestLevel(),
                "a pickaxe of stone has to open the ore of iron");
        owned.add(Items.IRON_ORE);

        smelt(owned, Items.IRON_ORE, Materials.IRON.ingot());
        make(owned, "iron_pickaxe");

        assertTrue(Items.IRON_PICKAXE.toolLevel() < Blocks.OBSIDIAN.harvestLevel(),
                "obsidian waits for a tool of diamond");
    }

    @Test
    void aPickaxeIsThreeOfItsMaterialOverTwoSticks() {
        Recipe pickaxe = RecipeRegistry.byName("iron_pickaxe");
        assertNotNull(pickaxe, "the recipe of the pickaxe of iron");

        Inventory cells = new Inventory(CraftingField.CELLS);
        InventoryGrid grid = new InventoryGrid(cells, 0, 3, 3);
        for (int column = 0; column < 3; column++) {
            cells.set(column, ItemStack.of(Materials.IRON.ingot(), 1));
        }
        cells.set(4, ItemStack.of(Items.STICK, 1));
        cells.set(7, ItemStack.of(Items.STICK, 1));

        assertTrue(pickaxe.matches(grid), "three ingots over two sticks are a pickaxe");
        pickaxe.consume(grid);

        assertEquals(0, cells.get(0).count(), "the ingots were used up");
        assertEquals(0, cells.get(4).count(), "and so were the sticks");
    }

    /** The five kinds of tool in every material the game makes tools of. */
    private static List<Item> toolsOfTheGame() {
        return List.of(Items.WOOD_PICKAXE, Items.WOOD_AXE, Items.WOOD_SHOVEL, Items.WOOD_HOE,
                Items.WOOD_SWORD, Items.STONE_PICKAXE, Items.STONE_AXE, Items.STONE_SHOVEL,
                Items.STONE_HOE, Items.STONE_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE,
                Items.IRON_SHOVEL, Items.IRON_HOE, Items.IRON_SWORD, Items.DIAMOND_PICKAXE,
                Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE, Items.DIAMOND_SWORD);
    }

    /** The recipe that makes an item, {@code null} when the game holds none. */
    private static Recipe recipeOf(Item made) {
        for (Recipe recipe : RecipeRegistry.recipes(RecipeType.CRAFTING_SHAPED)) {
            if (recipe.result().item() == made) {
                return recipe;
            }
        }
        for (Recipe recipe : RecipeRegistry.recipes(RecipeType.CRAFTING_SHAPELESS)) {
            if (recipe.result().item() == made) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * Makes one recipe out of what a player owns.
     * <p>
     * The cells are filled from the ingredients of the recipe, so a recipe whose ingredient no material
     * of the game satisfies fails here with the name of the recipe, and the whole chain of the test is
     * walked with the very items the game hands a player.
     *
     * @param owned items the player has made so far, the recipe's product is added to them
     * @param name name of the recipe, the name of its file
     */
    private static void make(Set<Item> owned, String name) {
        Recipe recipe = RecipeRegistry.byName(name);
        assertNotNull(recipe, "the game has a recipe named " + name);

        Inventory cells = new Inventory(CraftingField.CELLS);
        InventoryGrid grid = new InventoryGrid(cells, 0, 3, 3);
        fill(cells, owned, recipe);

        assertTrue(recipe.matches(grid), "the cells of " + name + " hold the recipe");
        recipe.consume(grid);
        owned.add(recipe.result().item());
    }

    /** Lays the ingredients of a recipe into the cells of a field. */
    private static void fill(Inventory cells, Set<Item> owned, Recipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            for (int row = 0; row < shaped.height(); row++) {
                for (int column = 0; column < shaped.width(); column++) {
                    Ingredient ingredient = shaped.ingredient(column, row);
                    if (ingredient.isEmpty()) {
                        continue;
                    }
                    Item item = pick(owned, ingredient, recipe.name());
                    cells.set(row * CraftingField.COLUMNS + column, ItemStack.of(item, 1));
                }
            }
            return;
        }
        if (recipe instanceof ShapelessRecipe shapeless) {
            int cell = 0;
            for (Ingredient ingredient : shapeless.ingredients()) {
                Item item = pick(owned, ingredient, recipe.name());
                cells.set(cell++, ItemStack.of(item, 1));
            }
            return;
        }
        throw new AssertionError("A field cannot lay out " + recipe);
    }

    /** Picks an item a player owns that satisfies an ingredient. */
    private static Item pick(Set<Item> owned, Ingredient ingredient, String recipeName) {
        for (Item item : owned) {
            if (ingredient.matches(ItemStack.of(item, 1))) {
                return item;
            }
        }
        throw new AssertionError("No item of " + recipeName + " can be made from "
                + owned.stream().map(Item::name).toList() + ": " + ingredient);
    }

    /** Smelts one item into another with the recipes of the game. */
    private static void smelt(Set<Item> owned, Item ore, Item product) {
        Inventory cells = new Inventory(1);
        cells.set(0, ItemStack.of(ore, 1));
        RecipeGrid grid = new InventoryGrid(cells, 0, 1, 1);

        Recipe recipe = RecipeRegistry.find(RecipeType.SMELTING, grid);
        assertNotNull(recipe, "the game knows how to smelt " + ore.name());
        assertTrue(recipe.result().item() == product, ore.name() + " is smelted into "
                + recipe.result().item().name() + " and not into " + product.name());

        recipe.consume(grid);
        owned.add(product);
    }
}
