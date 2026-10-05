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
}
