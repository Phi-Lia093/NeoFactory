package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Finds the reactions of a solution by balancing what stands in it against the catalog.
 * <p>
 * The engine is offered a system - the substances that stand in the vessel and the medium around them - and
 * answers with the reactions that keep the rule of {@link Conservation} and are written with substances the
 * catalog knows. It does not know how any single reaction works; it writes down every reaction that could
 * be balanced out of what is there and then says which of them a hand of chemical sense would prefer, which
 * is the whole of what "inferring a reaction" means here.
 * <p>
 * <b>The whole catalog is tried, a substance at a time.</b> A candidate reaction is built by taking a few
 * of the substances that stand in the vessel and a few of the substances of the catalog as the two sides,
 * multiplying each by a small whole number, and asking whether the two sides balance. A substance that
 * holds an element which stands nowhere in the vessel, the medium included, cannot be part of the answer
 * and is dropped before it is tried, which is what keeps the search small: only the handful of catalog
 * entries of the same elements as what is in the pot ever meet it.
 * <p>
 * <b>A candidate that balances is not yet a reaction that happens.</b> Of two balanced writings - hydrogen
 * and oxygen into water, hydrogen and oxygen into hydrogen peroxide - both are arithmetically true and only
 * one is what a hand would reach for, and the difference is not in the arithmetic. The engine therefore
 * orders its answers with a score of its own, written down here and not hidden: a solid that drops out of a
 * solution counts for most, a gas that leaves counts for less, a substance that was not there before counts
 * for a little, and every substance a reaction needs counts against it. The score is a hand of chemical
 * sense and not a law, and it is written in one place so that a later age can argue with it.
 */
public final class BalanceEngine implements ReactionEngine {

    /** Amount of product substances a reaction of this engine may be made of. */
    public static final int DEFAULT_MAX_PRODUCTS = 2;

    /** Largest whole number a substance of a candidate reaction is taken in. */
    public static final int DEFAULT_MAX_COEFFICIENT = 4;

    /** What a product that drops out of a solution counts for. */
    private static final int PRECIPITATE_SCORE = 100;

    /** What a product that leaves as a gas counts for. */
    private static final int GAS_SCORE = 60;

    /** What any other product counts for. */
    private static final int PRODUCT_SCORE = 10;

    /** What a product that was not standing in the vessel before counts for on top of its phase. */
    private static final int NEW_PRODUCT_SCORE = 20;

    private final Substances catalog;
    private final int maxProducts;
    private final int maxCoefficient;

    /** The phase of a substance, looked up once and kept, so that scoring is not a labeling every time. */
    private final Map<Chemical, Phase> phaseCache = new HashMap<>();

    /**
     * Creates an engine over a catalog, with the usual bounds.
     *
     * @param catalog the substances the engine may write with
     */
    public BalanceEngine(Substances catalog) {
        this(catalog, DEFAULT_MAX_PRODUCTS, DEFAULT_MAX_COEFFICIENT);
    }

    /**
     * Creates an engine over a catalog.
     *
     * @param catalog the substances the engine may write with
     * @param maxProducts most product substances a reaction may be made of, at least one
     * @param maxCoefficient largest whole number a substance is taken in, at least one
     */
    public BalanceEngine(Substances catalog, int maxProducts, int maxCoefficient) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.maxProducts = Math.max(1, maxProducts);
        this.maxCoefficient = Math.max(1, maxCoefficient);
    }

    @Override
    public List<Reaction> infer(System system) {
        List<Chemical> species = new ArrayList<>(system.content().components().keySet());
        if (species.isEmpty()) {
            return List.of();
        }
        List<Substance> usable = usableSubstances(allowedElements(system));
        List<Reaction> found = new ArrayList<>();
        collect(system, species, usable, new int[species.size()], 0, found);
        found.sort(bestFirst());
        return List.copyOf(found);
    }

    /**
     * The reaction the engine would run in a system, {@code null} when it found none.
     *
     * @param system what stands in the vessel
     * @return the best reaction, or {@code null}
     */
    public Reaction best(System system) {
        List<Reaction> found = infer(system);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Takes the substances in the pot in every whole number they may be taken in and looks for products.
     * <p>
     * A pot of two hydrogen and one oxygen may react all of it or a part of it - two hydrogen with one
     * oxygen, or four with two - and each way the pot may be split is a separate candidate, which is why the
     * amounts are walked one substance at a time and the products are looked for behind every split.
     */
    private void collect(System system, List<Chemical> species, List<Substance> usable,
            int[] coefficients, int index, List<Reaction> found) {
        if (index == species.size()) {
            Mixture reactants = mixtureOf(species, coefficients);
            if (!reactants.isEmpty()) {
                productsFor(system, reactants, usable, found);
            }
            return;
        }
        int limit = Math.min(system.content().amountOf(species.get(index)), maxCoefficient);
        for (int coefficient = 1; coefficient <= limit; coefficient++) {
            coefficients[index] = coefficient;
            collect(system, species, usable, coefficients, index + 1, found);
        }
    }

    /** The pile a split of the pot is. */
    private static Mixture mixtureOf(List<Chemical> species, int[] coefficients) {
        Map<Chemical, Integer> amounts = new LinkedHashMap<>();
        for (int index = 0; index < species.size(); index++) {
            if (coefficients[index] > 0) {
                amounts.put(species.get(index), coefficients[index]);
            }
        }
        return Mixture.of(amounts);
    }

    /** Looks for the product sides of a reacting pile, one substance at a time and then two. */
    private void productsFor(System system, Mixture reactants, List<Substance> usable,
            List<Reaction> found) {
        for (Substance entry : usable) {
            for (int coefficient = 1; coefficient <= maxCoefficient; coefficient++) {
                add(system, reactants, Mixture.of(entry.chemical(), coefficient), found);
            }
        }
        if (maxProducts < 2) {
            return;
        }
        for (int first = 0; first < usable.size(); first++) {
            for (int second = first; second < usable.size(); second++) {
                for (int left = 1; left <= maxCoefficient; left++) {
                    int from = first == second ? left : 1;
                    for (int right = from; right <= maxCoefficient; right++) {
                        Mixture products = Mixture.of(usable.get(first).chemical(), left)
                                .plus(Mixture.of(usable.get(second).chemical(), right));
                        add(system, reactants, products, found);
                    }
                }
            }
        }
    }

    /** Keeps a product side when it balances, when it is no product side at all, and when it changes. */
    private void add(System system, Mixture reactants, Mixture products, List<Reaction> found) {
        if (products.isEmpty() || products.equals(reactants)) {
            return;
        }
        Reaction reaction = Reaction.of(reactants, products, 0, system.background());
        if (Conservation.balanced(reaction)) {
            found.add(reaction);
        }
    }

    /** Every substance of the catalog whose elements stand somewhere in the vessel. */
    private List<Substance> usableSubstances(Set<String> allowed) {
        List<Substance> usable = new ArrayList<>();
        for (Substance entry : catalog.all()) {
            if (allowed.containsAll(entry.chemical().composition().elements())) {
                usable.add(entry);
            }
        }
        return usable;
    }

    /** The elements that stand anywhere in the vessel, the medium included. */
    private static Set<String> allowedElements(System system) {
        Set<String> allowed = new HashSet<>(system.content().composition().elements());
        for (Chemical chemical : system.background()) {
            allowed.addAll(chemical.composition().elements());
        }
        return allowed;
    }

    /**
     * Orders the answers of the engine: the explicit one first, then the shortest, then the best scored.
     * <p>
     * <b>A reaction that writes its water out is preferred over one that leaves it to the medium.</b> Both
     * {@code HCl + OH- -> Cl- + H2O} and {@code HCl + OH- -> Cl-} keep the rule when water stands around,
     * because the second lets the medium take the water the first writes down; but a reaction that leans on
     * the medium to hide a substance it makes is a writing that says less than it does, so the reaction
     * whose elements already come out - the one that needs nothing of the medium - is taken first. Only
     * behind those are the writings that really spend or make a substance of the medium ranked, and they are
     * ranked by how few substances they need to say the same change.
     * <p>
     * <b>A shorter writing of the same change is the more likely reading.</b> Both {@code 2H2 + O2 -> 2H2O}
     * and {@code H2 + O2 -> H2O2} balance hydrogen and oxygen, and of two writings that both balance the one
     * that needs fewer substances to say the same thing is the one a hand reaches for, so the count of the
     * substances of both sides comes next. Where two need the same count the score decides - what the
     * products do to drive the reaction on - and where the score is equal as well a settled spelling decides,
     * so that two answers of one rank cannot swap places between two runs of the same pot.
     *
     * @return the order
     */
    private Comparator<Reaction> bestFirst() {
        return (first, second) -> {
            int byExplicitness = Boolean.compare(needsBackground(first), needsBackground(second));
            if (byExplicitness != 0) {
                return byExplicitness;
            }
            int bySubstances = Integer.compare(substances(first), substances(second));
            if (bySubstances != 0) {
                return bySubstances;
            }
            int byScore = Integer.compare(score(second), score(first));
            if (byScore != 0) {
                return byScore;
            }
            return describe(first).compareTo(describe(second));
        };
    }

    /** {@code true} when a reaction leans on the medium to explain what it changed. */
    private static boolean needsBackground(Reaction reaction) {
        return !Conservation.difference(reaction).isEmpty();
    }

    /**
     * What a reaction counts for on top of being the shortest: the worth of what it makes.
     * <p>
     * A solid that drops out of a solution counts for most, because it leaves the solution and drives the
     * reaction on; a gas that leaves counts for less, for the same reason a little weaker; any other product
     * counts for a little. A substance that was not standing in the vessel and is made by the reaction adds
     * a little more, so that a reaction which changes something outranks one that only shuffles what was
     * there. The worth of the best product is taken and not the sum, because a reaction that makes two
     * worthwhile substances is no more of a reaction than one that makes the better of the two, and summing
     * would have the engine invent a second product only to raise the score.
     *
     * @param reaction the reaction
     * @return the score, higher for better
     */
    private int score(Reaction reaction) {
        int best = 0;
        boolean madeSomethingNew = false;
        for (Chemical product : reaction.products().components().keySet()) {
            Phase phase = phaseOf(product);
            int worth;
            if (phase == Phase.SOLID) {
                worth = PRECIPITATE_SCORE;
            } else if (phase == Phase.GAS) {
                worth = GAS_SCORE;
            } else {
                worth = PRODUCT_SCORE;
            }
            best = Math.max(best, worth);
            if (!reaction.reactants().components().containsKey(product)) {
                madeSomethingNew = true;
            }
        }
        return best + (madeSomethingNew ? NEW_PRODUCT_SCORE : 0);
    }

    /** The phase the catalog knows a substance in, {@code null} for one it does not hold, kept once found. */
    private Phase phaseOf(Chemical chemical) {
        if (phaseCache.containsKey(chemical)) {
            return phaseCache.get(chemical);
        }
        Phase found = null;
        for (Phase phase : Phase.values()) {
            if (catalog.byChemical(chemical, phase) != null) {
                found = phase;
                break;
            }
        }
        phaseCache.put(chemical, found);
        return found;
    }

    /** How many substances a reaction needs, both sides together. */
    private static int substances(Reaction reaction) {
        return reaction.reactants().size() + reaction.products().size();
    }

    /** A settled spelling of a reaction, so that two answers of one score come out in one order. */
    private static String describe(Reaction reaction) {
        return reaction.reactants() + "->" + reaction.products();
    }
}
