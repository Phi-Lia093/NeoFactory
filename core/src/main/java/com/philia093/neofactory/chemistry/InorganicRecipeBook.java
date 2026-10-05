package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The routes of the industry, written down by hand, and the pot that is matched against them.
 * <p>
 * A pot is looked up here before anything is inferred anywhere, and what is found here is the whole of what
 * happens: a route that fits the pot is run, and a pot that fits no route is left to the inferring of
 * {@link TemplateEngine} and to nothing else. <b>Nothing is ever extrapolated here.</b> A pot that no route
 * covers is answered with no reaction, which is the point of writing the routes down at all: the module may
 * say "not this" and never "something like this", because a reaction guessed out of a balance is a reaction
 * nobody wrote and nobody checked, see {@link InorganicRecipe}.
 * <p>
 * <b>A pot is looked up by the substances it holds.</b> The routes are kept under the substances they are
 * fed, so a pot reaches only the handful of routes that name what is really in it - a pot of sulfur and
 * oxygen never meets a route of ammonia - and among those only the ones whose every substance is there are
 * looked at at all. That is what keeps a book of a few hundred routes cheap enough to ask on every frame.
 * <p>
 * <b>Where more than one route fits, the narrower one wins.</b> A route that names more substances and asks
 * more of the vessel is the more particular reading of a pot, and a route written for a red heat beats one
 * written for any heat at all; the importance a route names of its own is read before either, so that a
 * route may be lifted over another one on purpose. Two routes that agree on all three are taken in the
 * order of their names, so that a pot always meets the same route first.
 */
public final class InorganicRecipeBook {

    private final List<InorganicRecipe> recipes = new ArrayList<>();
    private final Map<Chemical, List<InorganicRecipe>> bySubstance = new LinkedHashMap<>();

    /** Creates an empty book. */
    public InorganicRecipeBook() {
        // An empty book is a valid one: nothing is written down yet, so nothing is ever found here.
    }

    /**
     * Creates a book of the given routes.
     *
     * @param recipes the routes
     * @return the book
     */
    public static InorganicRecipeBook of(InorganicRecipe... recipes) {
        InorganicRecipeBook book = new InorganicRecipeBook();
        for (InorganicRecipe recipe : recipes) {
            book.add(recipe);
        }
        return book;
    }

    /**
     * Writes a route into the book.
     *
     * @param recipe the route
     * @return this book, for chaining
     */
    public InorganicRecipeBook add(InorganicRecipe recipe) {
        Objects.requireNonNull(recipe, "recipe");
        recipes.add(recipe);
        for (Chemical chemical : recipe.inputs().components().keySet()) {
            bySubstance.computeIfAbsent(chemical, key -> new ArrayList<>()).add(recipe);
        }
        return this;
    }

    /** Every route written in this book, in the order they were written. */
    public List<InorganicRecipe> all() {
        return List.copyOf(recipes);
    }

    /** Amount of routes written in this book. */
    public int count() {
        return recipes.size();
    }

    /**
     * The routes this pot could possibly run, the ones whose every substance is in it.
     *
     * @param pile what stands in the pot
     * @return the routes, empty when the pot reaches none of them
     */
    public List<InorganicRecipe> candidates(Blend pile) {
        Map<InorganicRecipe, Integer> reached = new LinkedHashMap<>();
        for (Chemical chemical : pile.components().keySet()) {
            List<InorganicRecipe> here = bySubstance.get(chemical);
            if (here == null) {
                continue;
            }
            for (InorganicRecipe recipe : here) {
                reached.merge(recipe, 1, Integer::sum);
            }
        }
        List<InorganicRecipe> candidates = new ArrayList<>();
        for (Map.Entry<InorganicRecipe, Integer> entry : reached.entrySet()) {
            if (entry.getValue() == entry.getKey().inputs().components().size()) {
                candidates.add(entry.getKey());
            }
        }
        return candidates;
    }

    /**
     * The route this pot runs, {@code null} when none of them does.
     *
     * @param pile what stands in the pot
     * @param offered what the machine reads of the vessel
     * @return the route, or {@code null}
     */
    public InorganicRecipe recipeFor(Blend pile, Conditions offered) {
        InorganicRecipe best = null;
        for (InorganicRecipe recipe : candidates(pile)) {
            if (!recipe.matches(pile, offered)) {
                continue;
            }
            if (best == null || narrower(recipe, best)) {
                best = recipe;
            }
        }
        return best;
    }

    /**
     * What this pot runs, {@code null} when no route of this book does.
     *
     * @param pile what stands in the pot
     * @param offered what the machine reads of the vessel
     * @return what the pot loses and gains, or {@code null}
     */
    public Outcome find(Blend pile, Conditions offered) {
        InorganicRecipe recipe = recipeFor(pile, offered);
        return recipe == null ? null : recipe.run(pile, offered);
    }

    /** {@code true} when the first route is the narrower reading of a pot than the second. */
    private static boolean narrower(InorganicRecipe first, InorganicRecipe second) {
        if (first.priority() != second.priority()) {
            return first.priority() > second.priority();
        }
        if (first.specificity() != second.specificity()) {
            return first.specificity() > second.specificity();
        }
        return first.id().compareTo(second.id()) < 0;
    }

    @Override
    public String toString() {
        return "InorganicRecipeBook(" + recipes.size() + " routes)";
    }

    /**
     * The routes the industry of the game is built on, written by hand.
     * <p>
     * These are the readings a pot may not discover for itself. Two hydrogens and an oxygen are water when
     * somebody says so and hydrogen peroxide when somebody says that, and nothing in the arithmetic of the
     * two tells them apart; a route that runs at a red heat and stands still in a room is the same; so the
     * readings that matter are written down here with their amounts and their conditions, and everything a
     * pot does that is not written here is left to the inferring of the organic side, see
     * {@link InorganicRecipeBook} and {@code TemplateEngine}.
     * <p>
     * The list is a beginning and not a catalogue: it covers the routes an industry of the game grows out of
     * - the synthesis gas and the shift, ammonia and the acid that comes from it, the sulfur that becomes
     * sulfuric acid, the brine that becomes chlorine and lye, the carbide that becomes acetylene, and the
     * oxide that becomes iron - and every route behind them is added the very same way, one builder at a
     * time, see {@link InorganicRecipe#builder(String)}.
     */
    public static InorganicRecipeBook industry(Substances catalog) {
        Objects.requireNonNull(catalog, "catalog");
        InorganicRecipeBook book = new InorganicRecipeBook();

        // The synthesis gas: carbon and steam at a red heat, and the shift that follows it.
        book.add(InorganicRecipe.builder("syngas")
                .inputs(pile(catalog, "carbon", 100).plus(pile(catalog, "water", 100)))
                .outputs(pile(catalog, "carbon monoxide", 100).plus(pile(catalog, "hydrogen", 100)))
                .primary(of(catalog, "carbon monoxide"))
                .conditions(Conditions.builder().temperature(Fraction.of(1000), Fraction.of(1600))
                        .build())
                .comment("carbon and steam at a red heat")
                .build());
        book.add(InorganicRecipe.builder("water_gas_shift")
                .inputs(pile(catalog, "carbon monoxide", 100).plus(pile(catalog, "water", 100)))
                .outputs(pile(catalog, "carbon dioxide", 100).plus(pile(catalog, "hydrogen", 100)))
                .primary(of(catalog, "hydrogen"))
                .comment("the water gas shift")
                .build());

        // Ammonia over an iron catalyst, and the acid that comes from burning it.
        book.add(InorganicRecipe.builder("haber")
                .inputs(pile(catalog, "nitrogen", 100).plus(pile(catalog, "hydrogen", 300)))
                .outputs(pile(catalog, "ammonia", 200))
                .primary(of(catalog, "ammonia"))
                .conditions(Conditions.builder().temperature(Fraction.of(650), Fraction.of(850))
                        .pressure(Fraction.of(100), null).catalyst(of(catalog, "iron")).build())
                .comment("nitrogen and hydrogen over iron at a pressure")
                .build());
        book.add(InorganicRecipe.builder("ostwald_1")
                .inputs(pile(catalog, "ammonia", 400).plus(pile(catalog, "oxygen", 500)))
                .outputs(pile(catalog, "nitrogen monoxide", 400)
                        .plus(pile(catalog, "water", 600)))
                .primary(of(catalog, "nitrogen monoxide"))
                .conditions(Conditions.builder().temperature(Fraction.of(1000), Fraction.of(1200))
                        .catalyst(of(catalog, "platinum")).build())
                .comment("ammonia burnt over platinum")
                .build());
        book.add(InorganicRecipe.builder("ostwald_2")
                .inputs(pile(catalog, "nitrogen monoxide", 200).plus(pile(catalog, "oxygen", 100)))
                .outputs(pile(catalog, "nitrogen dioxide", 200))
                .primary(of(catalog, "nitrogen dioxide"))
                .comment("the monoxide takes up one more oxygen")
                .build());
        book.add(InorganicRecipe.builder("ostwald_3")
                .inputs(pile(catalog, "nitrogen dioxide", 300).plus(pile(catalog, "water", 100)))
                .outputs(pile(catalog, "nitric acid", 200)
                        .plus(pile(catalog, "nitrogen monoxide", 100)))
                .primary(of(catalog, "nitric acid"))
                .comment("the dioxide and water into nitric acid")
                .build());

        // The contact process: sulfur burnt, the dioxide joined with oxygen and the trioxide into acid.
        book.add(InorganicRecipe.builder("contact_1")
                .inputs(pile(catalog, "sulfur", 100).plus(pile(catalog, "oxygen", 100)))
                .outputs(pile(catalog, "sulfur dioxide", 100))
                .primary(of(catalog, "sulfur dioxide"))
                .comment("sulfur burnt in air")
                .build());
        book.add(InorganicRecipe.builder("contact_2")
                .inputs(pile(catalog, "sulfur dioxide", 200).plus(pile(catalog, "oxygen", 100)))
                .outputs(pile(catalog, "sulfur trioxide", 200))
                .primary(of(catalog, "sulfur trioxide"))
                .conditions(Conditions.builder().temperature(Fraction.of(650), Fraction.of(750))
                        .catalyst(of(catalog, "platinum")).build())
                .comment("the dioxide over platinum")
                .build());
        book.add(InorganicRecipe.builder("contact_3")
                .inputs(pile(catalog, "sulfur trioxide", 100).plus(pile(catalog, "water", 100)))
                .outputs(pile(catalog, "sulfuric acid", 100))
                .primary(of(catalog, "sulfuric acid"))
                .comment("the trioxide into water")
                .build());

        // The brine: a current through salt water gives chlorine, hydrogen and lye.
        book.add(InorganicRecipe.builder("chlor_alkali")
                .inputs(pile(catalog, "sodium ion", 200).plus(pile(catalog, "chloride", 200))
                        .plus(pile(catalog, "water", 200)))
                .outputs(pile(catalog, "sodium ion", 200).plus(pile(catalog, "hydroxide", 200))
                        .plus(pile(catalog, "chlorine", 100)).plus(pile(catalog, "hydrogen", 100)))
                .primary(of(catalog, "chlorine"))
                .conditions(Conditions.builder().current(true).build())
                .comment("brine through a current")
                .build());

        // And the current that splits water itself, which no flame will do.
        book.add(InorganicRecipe.builder("water_electrolysis")
                .inputs(pile(catalog, "water", 200))
                .outputs(pile(catalog, "hydrogen", 200).plus(pile(catalog, "oxygen", 100)))
                .primary(of(catalog, "hydrogen"))
                .conditions(Conditions.builder().current(true).build())
                .comment("water under a current")
                .build());

        // The carbide: lime and carbon at a red heat, and the acetylene the carbide gives to water.
        book.add(InorganicRecipe.builder("carbide_1")
                .inputs(pile(catalog, "calcium ion", 100).plus(pile(catalog, "oxide ion", 100))
                        .plus(pile(catalog, "carbon", 300)))
                .outputs(pile(catalog, "calcium ion", 100).plus(pile(catalog, "carbide ion", 100))
                        .plus(pile(catalog, "carbon monoxide", 100)))
                .primary(of(catalog, "carbide ion"))
                .conditions(Conditions.builder().temperature(Fraction.of(2300), Fraction.of(2500))
                        .build())
                .comment("lime and carbon in an electric furnace")
                .build());
        book.add(InorganicRecipe.builder("carbide_2")
                .inputs(pile(catalog, "calcium ion", 100).plus(pile(catalog, "carbide ion", 100))
                        .plus(pile(catalog, "water", 200)))
                .outputs(pile(catalog, "calcium ion", 100).plus(pile(catalog, "hydroxide", 200))
                        .plus(pile(catalog, "acetylene", 100)))
                .primary(of(catalog, "acetylene"))
                .comment("carbide dropped into water")
                .build());

        // The oxide that becomes iron, and the methanol of the synthesis gas.
        book.add(InorganicRecipe.builder("iron_smelting")
                .inputs(pile(catalog, "iron(III) ion", 200).plus(pile(catalog, "oxide ion", 300))
                        .plus(pile(catalog, "carbon monoxide", 300)))
                .outputs(pile(catalog, "iron", 200).plus(pile(catalog, "carbon dioxide", 300)))
                .primary(of(catalog, "iron"))
                .conditions(Conditions.builder().temperature(Fraction.of(1200), Fraction.of(1800))
                        .build())
                .comment("iron oxide and the gas of the furnace")
                .build());
        book.add(InorganicRecipe.builder("methanol")
                .inputs(pile(catalog, "carbon monoxide", 100).plus(pile(catalog, "hydrogen", 200)))
                .outputs(pile(catalog, "methanol", 100))
                .primary(of(catalog, "methanol"))
                .conditions(Conditions.builder().temperature(Fraction.of(450), Fraction.of(550))
                        .pressure(Fraction.of(50), null).catalyst(of(catalog, "copper")).build())
                .comment("the synthesis gas into methanol")
                .build());

        return book;
    }

    /** One substance of a catalog, by the name a player reads. */
    private static Chemical of(Substances catalog, String name) {
        return catalog.byName(name).chemical();
    }

    /** A pile of one substance of a catalog, in millibuckets. */
    private static Blend pile(Substances catalog, String name, long millibuckets) {
        return Blend.of(of(catalog, name), millibuckets);
    }
}
