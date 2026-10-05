package com.philia093.neofactory.chemistry;

import com.philia093.neofactory.recipe.ChemicalRecipe;
import com.philia093.neofactory.recipe.Recipe;
import com.philia093.neofactory.recipe.RecipeRegistry;
import com.philia093.neofactory.recipe.RecipeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The one place a pot is asked what it does, and the one order in which it is asked.
 * <p>
 * A machine never has to know which way its answer came. It hands its pot and its vessel here and is given
 * back what the pot loses, what it gains and what the current owes, or nothing at all when nothing happens.
 * The order the two ways are asked in is the whole of the design:
 * <ol>
 *   <li><b>The routes written by hand are asked first</b>, and a route that fits the pot is the answer. An
 *       inorganic pot is decided here and nowhere else: what an industry does is a fact somebody wrote down,
 *       not a reading to be found in a balance, see {@link InorganicRecipeBook}.</li>
 *   <li><b>Only then is the organic side allowed to infer</b>, and it infers freely - a rewriting its
 *       templates allow is a reaction, whether or not anybody has heard of the molecule, see
 *       {@link TemplateEngine}. An organic molecule comes in too many shapes for a table, so the table is
 *       not asked to hold them.</li>
 *   <li><b>A pot that neither of them answers does nothing.</b> Nothing is extrapolated, nothing is guessed,
 *       and a pot nobody has written a route for is a pot the game leaves alone. That is the price of the
 *       whole design and it is paid on purpose: a reaction nobody wrote is a reaction nobody can explain to
 *       a player who asks why.</li>
 * </ol>
 * <p>
 * <b>Both answers are made to keep the rule of a pot before either is handed over.</b> A route is already
 * checked while it is written down, and an inferred rewriting is checked by its own engine, but the rule is
 * asked once more here, on the way out, so that there is exactly one place where a machine can be certain
 * that what it is about to do to a pot neither makes matter nor unmakes it, see {@link BlendConservation}.
 */
public final class ReactionRouter {

    private final InorganicRecipeBook book;
    private final ReactionEngine organic;

    /**
     * Creates a router over the two ways a pot may be answered.
     *
     * @param book the routes written by hand
     * @param organic the engine that rewrites organic molecules
     */
    public ReactionRouter(InorganicRecipeBook book, ReactionEngine organic) {
        this.book = Objects.requireNonNull(book, "book");
        this.organic = Objects.requireNonNull(organic, "organic");
    }

    /**
     * Creates a router over the reactions the organic side of the game ships with.
     * <p>
     * The rules of the polar families are a table of groups and steps, see {@link PolarReactions}; a route of
     * the industry is a file somebody wrote down and is asked first.
     *
     * @return the router
     */
    public static ReactionRouter industry() {
        return new ReactionRouter(InorganicRecipeBook.of(writtenRoutes()),
                new PolarEngine(PolarReactions.all()));
    }

    /**
     * Creates a router over the routes the game ships and the templates that came with it.
     * <p>
     * The routes are the files below {@code assets/recipes/chemical_reacting} and
     * {@code assets/recipes/electrolysis}, which are read and refused for a balance before a world is ever
     * opened: a route of the industry is a fact somebody wrote down and checked, and the file is where it
     * stands, see {@link ChemicalRecipe}.
     *
     * @param templates the templates of the organic side
     * @return the router
     */
    public static ReactionRouter industry(List<ReactionTemplate> templates) {
        return new ReactionRouter(InorganicRecipeBook.of(writtenRoutes()),
                new TemplateEngine(templates));
    }

    /** Every route written down in the assets of the game, in the order they were read. */
    private static InorganicRecipe[] writtenRoutes() {
        List<InorganicRecipe> routes = new ArrayList<>();
        for (RecipeType type : List.of(RecipeType.CHEMICAL_REACTING, RecipeType.ELECTROLYSIS)) {
            for (Recipe recipe : RecipeRegistry.recipes(type)) {
                if (recipe instanceof ChemicalRecipe chemical) {
                    routes.add(chemical.route());
                }
            }
        }
        return routes.toArray(new InorganicRecipe[0]);
    }

    /**
     * What a pot does, or nothing when nothing does.
     *
     * @param pile what stands in the pot, in millibuckets
     * @param offered what the machine reads of the vessel
     * @return what the pot loses and gains, or {@code null} when the pot is left alone
     */
    public Outcome route(Blend pile, Conditions offered) {
        Outcome written = book.find(pile, offered);
        if (written != null) {
            return checked(written);
        }
        return checked(infer(pile, offered));
    }

    /**
     * What a pot does in a vessel that is read as nothing, the shape a plain pot has.
     *
     * @param pile what stands in the pot, in millibuckets
     * @return what the pot loses and gains, or {@code null} when the pot is left alone
     */
    public Outcome route(Blend pile) {
        return route(pile, Conditions.NONE);
    }

    /** Asks the rule of a pot once more, on the way out. */
    private static Outcome checked(Outcome outcome) {
        if (outcome == null) {
            return null;
        }
        BlendConservation.check(outcome.consumed(), outcome.produced(), outcome.electrons(),
                outcome.medium());
        return outcome;
    }

    /**
     * The organic side: one rewriting of the pot, taken as many whole times as the pot can pay for.
     * <p>
     * The written routes were asked first and none of them fitted, so what is left is a molecule that the
     * templates can rewrite. The pot is handed to the engine as molecules and not as millibuckets - one of
     * the millibuckets a balance is measured in is one molecule of the reaction, see {@link Blend} - and
     * what comes back is read the same way a written route is read: as many whole runs as every substance
     * of it allows, and the rest of the pot left where it stands.
     */
    private Outcome infer(Blend pile, Conditions offered) {
        if (pile.isEmpty()) {
            return null;
        }
        Reaction reaction = organic.best(systemOf(pile, offered));
        if (reaction == null) {
            return null;
        }
        Blend consumed = blendOf(reaction.reactants());
        Blend produced = blendOf(reaction.products());
        int runs = runs(consumed, pile);
        if (runs < 1) {
            return null;
        }
        return new Outcome("organic", consumed.times(runs), produced.times(runs),
                (int) Math.min(Integer.MAX_VALUE, (long) reaction.electrons() * runs),
                reaction.background());
    }

    /**
     * Turns a pot into the vessel an inferring engine reads: the whole molecules of it.
     * <p>
     * A vessel whose state is not read is taken as a liquid, which is what an organic reaction of the game
     * is carried out in when nobody has said otherwise; the state is only ever read by an engine that cares
     * about it, and the two engines that were written before it did not.
     */
    private static System systemOf(Blend pile, Conditions offered) {
        Mixture content = Mixture.empty();
        for (Map.Entry<Chemical, Fraction> entry : pile.components().entrySet()) {
            long whole = entry.getValue().numerator() / entry.getValue().denominator();
            if (whole > 0) {
                content = content.plus(Mixture.of(entry.getKey(), (int) Math.min(Integer.MAX_VALUE, whole)));
            }
        }
        return System.of(content, offered.phase() == null ? Phase.LIQUID : offered.phase());
    }

    /** A mixture of molecules read as a pile, one molecule to one millibucket. */
    private static Blend blendOf(Mixture mixture) {
        Blend blend = Blend.empty();
        for (Map.Entry<Chemical, Integer> entry : mixture.components().entrySet()) {
            if (entry.getValue() != 0) {
                blend = blend.plus(Blend.of(entry.getKey(), entry.getValue()));
            }
        }
        return blend;
    }

    /**
     * How many whole reactions a pot can pay for, which is the reading the written routes use as well: the
     * bottom of the quotients of every substance the reaction needs, and none at all when one of them is
     * missing or short.
     */
    private static int runs(Blend inputs, Blend pile) {
        if (inputs.isEmpty()) {
            return 0;
        }
        long runs = Long.MAX_VALUE;
        for (Map.Entry<Chemical, Fraction> entry : inputs.components().entrySet()) {
            Fraction have = pile.amountOf(entry.getKey());
            Fraction need = entry.getValue();
            if (have.compareTo(need) < 0) {
                return 0;
            }
            long whole = Math.multiplyExact(have.numerator(), need.denominator())
                    / Math.multiplyExact(need.numerator(), have.denominator());
            runs = Math.min(runs, whole);
        }
        return (int) Math.min(Integer.MAX_VALUE, runs);
    }
}
