package com.philia093.neofactory.chemistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Finds the reactions a current drives: the ones that take a substance apart into its elements.
 * <p>
 * An electrolysis is a decomposition a current pays for. Water does not split into hydrogen and oxygen by
 * itself, and a salt of a metal does not give up its metal by itself; both happen when a line of cables
 * forces the electrons one way at one electrode and the other way at the other, which is why an electrolyzer
 * runs reactions no reactor of the same pot would ever run. What the module can see of that, without an
 * energy of its own, is the shape of the reaction: a substance of several elements, of the catalog, taken
 * apart into substances of one element each.
 * <p>
 * <b>The engine keeps the writings that take a compound apart into elements.</b> It asks the balance of a
 * solution for every reaction it could write, as {@link BalanceEngine} does, and keeps the ones that make a
 * substance of a single element which was not standing in the vessel before - the metal out of a salt, the
 * oxygen out of an oxide, the hydrogen out of water. Everything else a solution could do is somebody else's:
 * a reaction that only shuffles what is there is no decomposition and is left to the reactor.
 * <p>
 * <b>The electrons of the two electrodes cancel and are not written.</b> At the cathode a substance takes
 * electrons in and at the anode a substance gives them out, and the two counts are the same, because the
 * current that feeds one drains the other; the reaction of the cell as a whole therefore changes no charge
 * at all and names no electron, which is what the balance here answers with. The number of electrons the
 * cell really moves, and the over-potential and the concentration that decide which of two possible
 * decompositions a current would rather drive, belong to a later stage: the hooks for them are reserved,
 * and nothing here pretends to know them.
 */
public final class Electrolysis implements ReactionEngine {

    /** Amount of product substances a decomposition of this engine may be made of. */
    public static final int DEFAULT_MAX_PRODUCTS = 3;

    /** Largest whole number a substance of a candidate decomposition is taken in. */
    public static final int DEFAULT_MAX_COEFFICIENT = 4;

    private final Substances catalog;
    private final int maxProducts;
    private final int maxCoefficient;

    /**
     * Creates an engine over a catalog, with the usual bounds.
     *
     * @param catalog the substances the engine may write with
     */
    public Electrolysis(Substances catalog) {
        this(catalog, DEFAULT_MAX_PRODUCTS, DEFAULT_MAX_COEFFICIENT);
    }

    /**
     * Creates an engine over a catalog.
     *
     * @param catalog the substances the engine may write with
     * @param maxProducts most product substances a decomposition may be made of, at least one
     * @param maxCoefficient largest whole number a substance is taken in, at least one
     */
    public Electrolysis(Substances catalog, int maxProducts, int maxCoefficient) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.maxProducts = Math.max(1, maxProducts);
        this.maxCoefficient = Math.max(1, maxCoefficient);
    }

    @Override
    public List<Reaction> infer(System system) {
        BalanceEngine balance = new BalanceEngine(catalog, maxProducts, maxCoefficient);
        List<Reaction> found = new ArrayList<>();
        for (Reaction reaction : balance.infer(system)) {
            if (freedElements(reaction) > 0) {
                found.add(reaction);
            }
        }
        found.sort(bestFirst());
        return List.copyOf(found);
    }

    /**
     * The decomposition the engine would run in a system, {@code null} when it found none.
     *
     * @param system what stands in the vessel
     * @return the best decomposition, or {@code null}
     */
    public Reaction best(System system) {
        List<Reaction> found = infer(system);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * Orders the decompositions: the one that frees the most elements first, then the shortest.
     * <p>
     * An electrolysis is run to win the metals and the gases out of a compound, so a decomposition that
     * frees two elements is the one a workshop wants where a decomposition that frees one is a half job.
     * Where two free the same count the one that needs fewer substances wins, and where that is equal as
     * well a settled spelling decides, so that two runs of one cell cannot answer differently.
     *
     * @return the order
     */
    private Comparator<Reaction> bestFirst() {
        return (first, second) -> {
            int byElements = Integer.compare(freedElements(second), freedElements(first));
            if (byElements != 0) {
                return byElements;
            }
            int bySubstances = Integer.compare(substances(first), substances(second));
            if (bySubstances != 0) {
                return bySubstances;
            }
            return (first.reactants() + "->" + first.products())
                    .compareTo(second.reactants() + "->" + second.products());
        };
    }

    /** How many substances of a single element a decomposition frees that were not there before. */
    private static int freedElements(Reaction reaction) {
        int freed = 0;
        for (Chemical product : reaction.products().components().keySet()) {
            if (product.kind() == Chemical.Kind.ELEMENT
                    && !reaction.reactants().components().containsKey(product)) {
                freed++;
            }
        }
        return freed;
    }

    /** How many substances a decomposition needs, both sides together. */
    private static int substances(Reaction reaction) {
        return reaction.reactants().size() + reaction.products().size();
    }
}
