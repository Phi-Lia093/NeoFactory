package com.philia093.neofactory.loot;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.item.ItemRegistry;
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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the loot tables of the game: the files that ship with it, the shapes a file may have and the rule
 * that a block without a file hands over itself.
 * <p>
 * Every file below {@code assets/loot_tables/blocks} is parsed here, so a typo in an item name fails the
 * build instead of showing up as a block that silently drops nothing. The shapes no file uses yet - a count
 * that rolls between two numbers - are checked with hand written text, and so are the files that have to be
 * refused.
 */
class LootTableLoaderTest {

    /** Assets of the project, the tests run inside the core module. */
    private static final Path TABLES = Path.of("..", "assets", LootTableLoader.FOLDER);

    /** Source of the rolls, fixed so the build does not depend on a chance of one in ten. */
    private static final Random RANDOM = new Random(20260925L);

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void everyFileOfTheGameCanBeRead() throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(TABLES)) {
            for (Path file : stream.toList()) {
                if (file.getFileName().toString().endsWith(LootTableLoader.EXTENSION)) {
                    files.add(file);
                }
            }
        }
        assertFalse(files.isEmpty(), "the game ships loot tables");

        for (Path file : files) {
            String name = file.getFileName().toString();
            name = name.substring(0, name.length() - LootTableLoader.EXTENSION.length());
            assertNotNull(BlockRegistry.byName(name),
                    file + " is named after a block of the game");

            LootTable table = LootTableLoader.parse(name, Files.readString(file, StandardCharsets.UTF_8));
            assertNotNull(table, file + " can be read");
        }
    }

    @Test
    void theTablesOfTheGameAreRegisteredWithTheirBlocks() {
        assertEquals(ItemRegistry.byBlock(Blocks.COBBLESTONE),
                dropOf(Blocks.STONE, 0).item(), "stone hands over cobblestone");
        assertEquals(4, dropOf(Blocks.CLAY, 0).min(), "and clay hands over four clay balls at a time");
        assertTrue(LootTableRegistry.byBlock(Blocks.GLASS).isEmpty(), "glass breaks into nothing");
        assertEquals(2, LootTableRegistry.byBlock(Blocks.GRAVEL).drops().size(),
                "gravel hands over itself and sometimes a flint");
        assertNull(LootTableRegistry.byBlock(Blocks.DIRT),
                "a block that names no table hands over itself, which the break decides");
    }

    @Test
    void aTableHandsOverWhatItsLinesRoll() {
        // Gravel: a gravel on every break and a flint now and then, so a roll has to show both.
        LootTable table = LootTableRegistry.byBlock(Blocks.GRAVEL);
        int gravel = 0;
        int flint = 0;
        for (int roll = 0; roll < 200; roll++) {
            for (ItemStack stack : table.roll(RANDOM)) {
                if (stack.item() == ItemRegistry.byBlock(Blocks.GRAVEL)) {
                    gravel++;
                } else if (stack.item() == Items.FLINT) {
                    flint++;
                }
            }
        }

        assertEquals(200, gravel, "a line without a chance happens on every break");
        assertTrue(flint > 0, "and a line with a chance of one in ten happens now and then");
        assertTrue(flint < 200, "but not on every break");
    }

    @Test
    void aDropRollsBetweenItsTwoAmounts() {
        LootTable table = LootTableLoader.parse("clay",
                "{ \"drops\": [ { \"item\": \"clay_ball\", \"count\": [2, 4] } ] }");
        Set<Integer> amounts = new HashSet<>();
        for (int roll = 0; roll < 200; roll++) {
            List<ItemStack> items = table.roll(RANDOM);
            assertEquals(1, items.size(), "one line, one stack");
            amounts.add(items.get(0).count());
        }

        assertEquals(Set.of(2, 3, 4), amounts, "every amount of the range comes up");
    }

    @Test
    void aFileThatMakesNoSenseIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> LootTableLoader.parse("broken", "{}"),
                "a table names its drops");
        assertThrows(IllegalArgumentException.class,
                () -> LootTableLoader.parse("broken", "{ \"drops\": [ { } ] }"),
                "every drop names an item");
        assertThrows(IllegalArgumentException.class,
                () -> LootTableLoader.parse("broken", "{ \"drops\": [ { \"item\": \"no_such_item\" } ] }"),
                "and the item has to be one the game knows");
        assertThrows(IllegalArgumentException.class, () -> LootTableLoader.parse("broken",
                "{ \"drops\": [ { \"item\": \"diamond_pickaxe\", \"count\": 5 } ] }"),
                "more items than fit in one stack are refused");
        assertThrows(IllegalArgumentException.class, () -> LootTableLoader.parse("broken",
                "{ \"drops\": [ { \"item\": \"stone\", \"count\": [4, 2] } ] }"),
                "so is a range that ends before it starts");
        assertThrows(IllegalArgumentException.class, () -> LootTableLoader.parse("broken",
                "{ \"drops\": [ { \"item\": \"stone\", \"chance\": 2.0 } ] }"),
                "and a chance above one");
    }

    /** One line of the table of a block, checked to be there. */
    private static LootTable.Drop dropOf(Block block, int index) {
        LootTable table = LootTableRegistry.byBlock(block);
        assertNotNull(table, "the game ships a loot table for '" + block.name() + "'");
        assertTrue(index < table.drops().size(), "the table of '" + block.name() + "' holds that line");
        return table.drops().get(index);
    }
}
