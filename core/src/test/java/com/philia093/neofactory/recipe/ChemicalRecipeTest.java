package com.philia093.neofactory.recipe;

import com.philia093.neofactory.chemistry.Blend;
import com.philia093.neofactory.chemistry.Chemical;
import com.philia093.neofactory.chemistry.Conditions;
import com.philia093.neofactory.chemistry.Fraction;
import com.philia093.neofactory.chemistry.InorganicRecipeBook;
import com.philia093.neofactory.chemistry.Outcome;
import com.philia093.neofactory.chemistry.Substances;
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
 * Checks the routes of the industry as the files they are.
 * <p>
 * A route is a file below {@code assets/recipes/chemical_reacting} and {@code .../electrolysis}: it names
 * substances and millibuckets, it may name the vessel it asks for, and it is handed to the rule of a pot
 * while it is read - so the count of the files the game ships is also the count of the routes that were
 * found to balance. A file that leaves an atom behind, or that names a substance the catalog has never
 * heard of, is refused instead of being run.
 */
class ChemicalRecipeTest {

    private static final Path RECIPES = Path.of("..", "assets", "recipes");

    private static final Substances CATALOG = Substances.starter();

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    /** Reads every route that ships in one folder, the way the game reads it. */
    private static List<ChemicalRecipe> shipped(RecipeType type) {
        List<ChemicalRecipe> recipes = new ArrayList<>();
        try (Stream<Path> files = Files.list(RECIPES.resolve(type.name()))) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString().replace(RecipeLoader.EXTENSION, "");
                recipes.add((ChemicalRecipe) RecipeLoader.parse(type, name,
                        Files.readString(file, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            throw new IllegalStateException("the routes of the game cannot be read", e);
        }
        return recipes;
    }

    /** Every route of the game in one book, which is what a reactor matches its pot against. */
    private static InorganicRecipeBook book() {
        InorganicRecipeBook book = new InorganicRecipeBook();
        for (RecipeType type : List.of(RecipeType.CHEMICAL_REACTING, RecipeType.ELECTROLYSIS)) {
            for (ChemicalRecipe recipe : shipped(type)) {
                book.add(recipe.route());
            }
        }
        return book;
    }

    private static Chemical of(String name) {
        return CATALOG.byName(name).chemical();
    }

    @Test
    void theRoutesOfTheGameAreFilesAndEveryOneOfThemBalances() {
        assertEquals(13, shipped(RecipeType.CHEMICAL_REACTING).size(), "the routes of the reactor");
        assertEquals(2, shipped(RecipeType.ELECTROLYSIS).size(), "and the two a current drives");
        assertEquals(15, book().count(), "no file was refused while it was read");
    }

    @Test
    void aRouteNamesItsSubstancesInMillibuckets() {
        ChemicalRecipe syngas = shipped(RecipeType.CHEMICAL_REACTING).stream()
                .filter(recipe -> recipe.name().equals("syngas")).findFirst().orElseThrow();

        assertEquals(Fraction.of(100), syngas.route().inputs().amountOf(of("carbon")));
        assertEquals(Fraction.of(100), syngas.route().inputs().amountOf(of("water")));
        assertEquals(Fraction.of(100), syngas.route().outputs().amountOf(of("hydrogen")));
        assertEquals(of("carbon monoxide"), syngas.route().primary());
        assertEquals(12.0f, syngas.seconds());
        assertEquals(32, syngas.voltage());
        assertEquals(30, syngas.euPerTick());
    }

    @Test
    void aRouteOfTheElectrolysisAsksForACurrentAndTheRestDoNot() {
        for (ChemicalRecipe recipe : shipped(RecipeType.ELECTROLYSIS)) {
            assertTrue(recipe.route().conditions().current(), recipe.name() + " is driven by a current");
        }
        for (ChemicalRecipe recipe : shipped(RecipeType.CHEMICAL_REACTING)) {
            assertFalse(recipe.route().conditions().current(),
                    recipe.name() + " runs in a vessel that is only heated");
        }
    }

    @Test
    void aPotOfSulfurAndAirIsBurnt() {
        Blend pot = Blend.of(of("sulfur"), 100).plus(Blend.of(of("oxygen"), 100));

        Outcome outcome = book().find(pot, Conditions.NONE);

        assertNotNull(outcome);
        assertEquals("contact_1", outcome.source(), "the route of the file is the answer");
        assertEquals(Fraction.of(100), outcome.produced().amountOf(of("sulfur dioxide")));
    }

    @Test
    void theBrineGivesChlorineAndLye() {
        Blend pot = Blend.of(of("sodium ion"), 200).plus(Blend.of(of("chloride"), 200))
                .plus(Blend.of(of("water"), 200));

        Outcome outcome = book().find(pot, Conditions.builder().current(true).build());

        assertNotNull(outcome);
        assertEquals("chlor_alkali", outcome.source());
        assertEquals(Fraction.of(100), outcome.produced().amountOf(of("chlorine")));
        assertEquals(Fraction.of(200), outcome.produced().amountOf(of("hydroxide")));
    }

    @Test
    void waterSplitsIntoItsGasesOnlyUnderACurrent() {
        Blend pot = Blend.of(of("water"), 200);

        assertNull(book().find(pot, Conditions.NONE), "no flame splits water into its gases");
        Outcome outcome = book().find(pot, Conditions.builder().current(true).build());
        assertNotNull(outcome, "a current does");
        assertEquals("water_electrolysis", outcome.source());
        assertEquals(Fraction.of(200), outcome.produced().amountOf(of("hydrogen")));
        assertEquals(Fraction.of(100), outcome.produced().amountOf(of("oxygen")));
    }

    @Test
    void aFileThatLeavesAnAtomBehindIsRefused() {
        assertThrows(RuntimeException.class, () -> RecipeLoader.parse(RecipeType.CHEMICAL_REACTING,
                "broken", "{ \"inputs\": { \"carbon\": 100 }, \"outputs\": { \"carbon monoxide\": 100 } }"),
                "a route that does not keep the rule of a pot is no route");
    }

    @Test
    void aFileThatNamesASubstanceNobodyKnowsIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> RecipeLoader.parse(
                RecipeType.CHEMICAL_REACTING, "broken",
                "{ \"inputs\": { \"unobtainium\": 100 }, \"outputs\": { \"unobtainium\": 100 } }"));
    }
}
