package com.philia093.neofactory.recipe;

import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.machine.AlloyFurnaceMachine;
import com.philia093.neofactory.machine.Machine;
import com.philia093.neofactory.machine.MachineInventory;
import com.philia093.neofactory.machine.MachineRecipe;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the recipe files of the age of steam, one folder per machine.
 * <p>
 * A file of that age is read by {@link SteamRecipe} and says how much steam one craft spends, which is the
 * number its machine has to pay. The files are read from the assets of the project and not through the
 * registry, because the registry is filled while the game starts, see {@link RecipeLoader#parse}.
 */
class SteamRecipesTest {

    /** Folder holding every recipe of the game. */
    private static final Path FOLDER = TestRegistries.ASSETS.resolve(RecipeLoader.FOLDER);

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void everyRecipeOfTheAgeOfSteamIsReadAndSpendsSteam() throws IOException {
        int read = 0;
        for (RecipeType type : RecipeType.steamKinds()) {
            List<Path> files = filesOf(type);
            assertFalse(files.isEmpty(), type.name() + " is a kind of recipe a machine reads");
            for (Path file : files) {
                Recipe recipe = RecipeLoader.parse(type, nameOf(file), Files.readString(file));

                SteamRecipe steamRecipe = assertInstanceOf(SteamRecipe.class, recipe,
                        file.getFileName() + " is a recipe of the age of steam");
                assertEquals(type, steamRecipe.type(), "the folder of a recipe is the machine it belongs to");
                assertTrue(steamRecipe.steam() > 0,
                        recipe.name() + " says how much steam one craft spends");
                assertTrue(steamRecipe.seconds() > 0.0f, recipe.name() + " says how long a craft takes");
            }
            read += files.size();
        }
        assertEquals(21, read, "the recipes the age of steam ships with");
    }

    @Test
    void anIngredientIsAPlaceOfTheInputAndNotAStack() throws IOException {
        MachineRecipe recipe = assertInstanceOf(MachineRecipe.class,
                RecipeLoader.parse(RecipeType.ALLOY_SMELTING, "bronze_ingot",
                        Files.readString(FOLDER.resolve("alloy_smelting/bronze_ingot.json"))));
        Machine furnace = new AlloyFurnaceMachine();

        fill(furnace, 0, "copper_ingot", 1);
        fill(furnace, 1, "tin_ingot", 1);
        assertTrue(recipe.matches(furnace.inputs()), "the two metals a recipe names are in the furnace");

        // A whole stack in one slot is one place and not the two the recipe asks for.
        fill(furnace, 0, "copper_ingot", 64);
        furnace.inventory().set(1, ItemStack.EMPTY);
        assertFalse(recipe.matches(furnace.inputs()),
                "a stack of copper is not the tin of the second ingredient");
    }

    /** Puts a stack of an item into one slot of a machine. */
    private static void fill(Machine machine, int slot, String item, int count) {
        MachineInventory inventory = machine.inventory();
        inventory.set(slot, ItemStack.of(ItemRegistry.byName(item), count));
    }

    /** Every file below the folder of one kind of recipe, in the order they are named. */
    private static List<Path> filesOf(RecipeType type) throws IOException {
        List<Path> files = new ArrayList<>();
        Path folder = FOLDER.resolve(type.name());
        try (var entries = Files.list(folder)) {
            entries.filter(path -> path.getFileName().toString().endsWith(RecipeLoader.EXTENSION))
                    .sorted()
                    .forEach(files::add);
        }
        return files;
    }

    /** Name of a recipe, the name of its file without the extension. */
    private static String nameOf(Path file) {
        String name = file.getFileName().toString();
        return name.substring(0, name.length() - RecipeLoader.EXTENSION.length());
    }
}
