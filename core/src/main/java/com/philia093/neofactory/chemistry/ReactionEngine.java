package com.philia093.neofactory.chemistry;

import java.util.List;

/**
 * What may happen inside a vessel: a system goes in, the reactions it could run come back out.
 * <p>
 * The module has more than one kind of reaction and they are found in more than one way. The reactions of a
 * solution are found by balancing what is in it against the catalog, and a later stage finds the reactions
 * of an organic molecule by matching a structure, see {@link com.philia093.neofactory.recipe.ChemicalRecipe} and the template engine of the
 * age that follows. What the two have in common is the whole of this interface: they are handed what stands
 * in the vessel and answer with the reactions that could be written there, each of them already forced
 * through {@link Conservation}.
 * <p>
 * The engine is a question and not a state: an engine built around a catalog answers for any system and
 * keeps nothing between two answers, so a machine may ask again every time it looks for work and a test may
 * ask without a world.
 */
public interface ReactionEngine {

    /**
     * The reactions that could run in a system, the better ones first.
     * <p>
     * The list is meant to be read from the front: the first reaction is the one the engine would run, and
     * the ones behind it are the alternatives that would be left standing when the first is taken. An empty
     * list means nothing the engine knows of could happen - the closed world found no way to balance what
     * stands in the vessel - and is not an error.
     *
     * @param system what stands in the vessel
     * @return the reactions, the best first, empty when none could happen
     */
    List<Reaction> infer(System system);
}
