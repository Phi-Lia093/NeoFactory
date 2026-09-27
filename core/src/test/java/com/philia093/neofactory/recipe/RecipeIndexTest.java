package com.philia093.neofactory.recipe;

import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.material.Materials;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the two questions a screen of recipes asks about an item.
 * <p>
 * <b>How is this made</b> is answered by the recipes whose product is the item, <b>what is this used
 * for</b> by the recipes that take it as a material. Both are read from the very recipe objects the game
 * runs, so a click on a recipe and a machine that performs it can never mean two different things.
 */
class RecipeIndexTest {

    /** The pattern of a pickaxe of iron, the recipe every question of this test is asked about. */
    private static final String PICKAXE = "{ \"pattern\": [\"III\", \" S \", \" S \"],"
            + " \"key\": { \"I\": \"iron_ingot\", \"S\": \"stick\" },"
            + " \"result\": { \"item\": \"iron_pickaxe\" } }";

    /** The recipe of a stick, which takes planks apart. */
    private static final String STICKS = "{ \"pattern\": [\"P\", \"P\"],"
            + " \"key\": { \"P\": \"planks_oak\" },"
            + " \"result\": { \"item\": \"stick\", \"count\": 4 } }";

    /** A recipe of a machine, which takes an ore and makes an ingot over time. */
    private static final String SMELTING = "{ \"ingredient\": \"iron_ore\","
            + " \"result\": { \"item\": \"iron_ingot\" }, \"time\": 10 }";

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @BeforeEach
    void registerRecipes() {
        RecipeRegistry.clear();
        RecipeRegistry.register(RecipeLoader.parse(RecipeType.CRAFTING_SHAPED, "iron_pickaxe", PICKAXE));
        RecipeRegistry.register(RecipeLoader.parse(RecipeType.CRAFTING_SHAPED, "stick", STICKS));
        RecipeRegistry.register(RecipeLoader.parse(RecipeType.SMELTING, "iron_ingot", SMELTING));
    }

    @Test
    void howAnItemIsMadeIsFoundByItsProduct() {
        RecipeIndex index = new RecipeIndex();

        List<Recipe> recipes = index.resultsOf(Items.IRON_PICKAXE);
        assertEquals(1, recipes.size(), "one recipe makes a pickaxe of iron");
        assertEquals("iron_pickaxe", recipes.get(0).name());

        List<Recipe> ingots = index.resultsOf(Materials.IRON.ingot());
        assertEquals(1, ingots.size(), "the ingot is made by the furnace");
        assertEquals(RecipeType.SMELTING, ingots.get(0).type());

        assertTrue(index.resultsOf(Items.DIAMOND).isEmpty(), "nothing makes a diamond so far");
    }

    @Test
    void whatAnItemIsUsedForIsFoundByItsMaterials() {
        RecipeIndex index = new RecipeIndex();

        List<Recipe> withIngot = index.usesOf(Materials.IRON.ingot());
        assertEquals(1, withIngot.size(), "the ingot is used by the pickaxe");
        assertEquals("iron_pickaxe", withIngot.get(0).name());

        List<Recipe> withSticks = index.usesOf(Items.STICK);
        assertEquals(1, withSticks.size(), "and the sticks are used by it as well");

        List<Recipe> ores = index.usesOf(Items.IRON_ORE);
        assertEquals(1, ores.size(), "the ore is used by the furnace");
        assertEquals(RecipeType.SMELTING, ores.get(0).type());

        assertTrue(index.usesOf(Items.DIAMOND).isEmpty(), "nothing takes a diamond so far");
    }

    @Test
    void aRecipeKnowsWhatItIsMadeOf() {
        Recipe pickaxe = RecipeRegistry.byName("iron_pickaxe");

        List<Ingredient> ingredients = pickaxe.ingredients();
        assertEquals(5, ingredients.size(), "three ingots and two sticks");
        assertTrue(ingredients.get(0).matches(com.philia093.neofactory.item.ItemStack.of(
                Materials.IRON.ingot(), 1)));
        assertTrue(ingredients.get(4).matches(com.philia093.neofactory.item.ItemStack.of(
                Items.STICK, 1)));

        Recipe smelting = RecipeRegistry.byName("iron_ingot");
        assertEquals(1, smelting.ingredients().size(),
                "a smelting recipe is made of the one item it takes");
    }

    @Test
    void aRecipeIsFoundByTheNameOfItsProduct() {
        RecipeIndex index = new RecipeIndex();

        assertEquals(1, index.find("pickaxe").size(), "the recipe of the pickaxe");
        assertEquals(3, index.find("").size(), "an empty box finds every recipe");
        assertTrue(index.find("nothing_at_all").isEmpty());
        assertEquals(1, index.ofType(RecipeType.SMELTING).size(),
                "the recipes of one kind, for a category of the screen");
    }
}
