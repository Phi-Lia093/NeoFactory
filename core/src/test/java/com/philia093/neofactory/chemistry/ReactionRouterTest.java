package com.philia093.neofactory.chemistry;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.philia093.neofactory.recipe.ChemicalRecipe;
import com.philia093.neofactory.recipe.RecipeLoader;
import com.philia093.neofactory.recipe.RecipeType;
import com.philia093.neofactory.support.TestRegistries;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Checks the one order in which a pot is asked what it does.
 * <p>
 * A pot of the industry is answered by the route written for it and by nothing else; a pot nobody wrote a
 * route for but that the organic templates can rewrite is answered by the rewriting; and a pot that neither
 * answers is left alone, which is the rule the whole design turns on - the router may say "not this" and
 * never "something like this".
 */
class ReactionRouterTest {

    private static final Substances INDUSTRY = Substances.starter();

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    private static Chemical of(Substances catalog, String name) {
        return catalog.byName(name).chemical();
    }

    /** The two molecules the organic example of this test is written with. */
    private static Substances organicCatalog() {
        return new Substances()
                .register("ethene", "C=C", Phase.GAS)
                .register("hydrogen", "[H][H]", Phase.GAS)
                .register("ethane", "CC", Phase.GAS);
    }

    private static ReactionTemplate hydrogenation() {
        return ReactionTemplate.parse("hydrogenation", "[C:1]=[C:2].[H][H]>>[C:1][C:2]");
    }

    /** A pot of ethene and hydrogen, which both a route and a template may take. */
    private static Blend etheneAndHydrogen(Substances catalog) {
        return Blend.of(of(catalog, "ethene"), 100).plus(Blend.of(of(catalog, "hydrogen"), 100));
    }

    /**
     * A router over the routes the game ships, read from {@code assets/recipes} the way the game reads
     * them: a route of the industry is a file and nothing else.
     */
    private static ReactionRouter shipped() {
        InorganicRecipeBook book = new InorganicRecipeBook();
        for (RecipeType type : List.of(RecipeType.CHEMICAL_REACTING, RecipeType.ELECTROLYSIS)) {
            Path folder = Path.of("..", "assets", "recipes", type.name());
            try (Stream<Path> files = Files.list(folder)) {
                for (Path file : files.toList()) {
                    String name = file.getFileName().toString()
                            .replace(RecipeLoader.EXTENSION, "");
                    ChemicalRecipe recipe = (ChemicalRecipe) RecipeLoader.parse(type, name,
                            Files.readString(file, StandardCharsets.UTF_8));
                    book.add(recipe.route());
                }
            } catch (IOException e) {
                throw new IllegalStateException("the routes of the game cannot be read", e);
            }
        }
        return new ReactionRouter(book, new TemplateEngine(List.of()));
    }

    @Test
    void aWrittenRouteIsRunAndSaysWhereItCameFrom() {
        ReactionRouter router = shipped();
        Blend pot = Blend.of(of(INDUSTRY, "carbon"), 100).plus(Blend.of(of(INDUSTRY, "water"), 100));

        Outcome outcome = router.route(pot);

        assertNotNull(outcome, "carbon and steam are written together");
        assertEquals("syngas", outcome.source(), "the route of the industry is the answer");
        assertEquals(Fraction.of(100), outcome.produced().amountOf(of(INDUSTRY, "carbon monoxide")));
    }

    @Test
    void aPotNoRouteCoversIsLeftAlone() {
        ReactionRouter router = shipped();

        assertNull(router.route(Blend.of(of(INDUSTRY, "gold"), 100)),
                "nothing is extrapolated out of what happens to stand in the pot");
    }

    @Test
    void halfOfARouteDoingNothingIsStillNothing() {
        ReactionRouter router = shipped();

        assertNull(router.route(Blend.of(of(INDUSTRY, "carbon"), 1000)),
                "carbon without the steam it is written with is not a reaction");
    }

    @Test
    void aPotNoRouteCoversFallsToTheOrganicSide() {
        Substances catalog = organicCatalog();
        ReactionRouter router = new ReactionRouter(new InorganicRecipeBook(),
                new TemplateEngine(List.of(hydrogenation())));

        Outcome outcome = router.route(etheneAndHydrogen(catalog));

        assertNotNull(outcome, "no route was written, so the templates answer");
        assertEquals("organic", outcome.source());
        assertEquals(Fraction.of(100), outcome.produced().amountOf(of(catalog, "ethane")),
                "two hundred runs of one hydrogenation is a hundred of ethane");
    }

    @Test
    void theAnswerOfARouteIsCheckedAgainstTheRuleOfAPotOnceMore() {
        // A route whose products hold more than its reactants only balances because the medium moves whole.
        // The router asks the rule of a pot once more on the way out and lets such an answer through.
        InorganicRecipe inWater = InorganicRecipe.builder("in_water")
                .inputs(Blend.of(of(INDUSTRY, "hydrogen"), 200)
                        .plus(Blend.of(of(INDUSTRY, "oxygen"), 100)))
                .outputs(Blend.of(of(INDUSTRY, "water"), 400))
                .medium(of(INDUSTRY, "water"))
                .build();
        Blend pot = Blend.of(of(INDUSTRY, "hydrogen"), 200).plus(Blend.of(of(INDUSTRY, "oxygen"), 100));

        Outcome outcome = new ReactionRouter(InorganicRecipeBook.of(inWater),
                new TemplateEngine(List.of())).route(pot);

        assertNotNull(outcome);
        assertEquals("in_water", outcome.source());
        assertEquals(1, outcome.medium().size(), "and the answer carries what may move whole");
    }

    @Test
    void theWrittenRouteIsAskedBeforeTheOrganicOne() {
        Substances catalog = organicCatalog();
        TemplateEngine organic = new TemplateEngine(List.of(hydrogenation()));
        Blend pot = etheneAndHydrogen(catalog);

        Outcome inferred = new ReactionRouter(new InorganicRecipeBook(), organic).route(pot);
        assertNotNull(inferred, "with nothing written down, the organic side does rewrite this pot");
        assertEquals("organic", inferred.source());

        Outcome written = new ReactionRouter(
                InorganicRecipeBook.of(InorganicRecipe.builder("ethene_hydrogenation")
                        .inputs(Blend.of(of(catalog, "ethene"), 100)
                                .plus(Blend.of(of(catalog, "hydrogen"), 100)))
                        .outputs(Blend.of(of(catalog, "ethane"), 100))
                        .primary(of(catalog, "ethane"))
                        .build()), organic)
                .route(pot);

        assertEquals("ethene_hydrogenation", written.source(),
                "with a route written down, the route is what answers");
    }
}
