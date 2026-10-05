package com.philia093.neofactory.chemistry;

/**
 * A thing that is some amount of some substances: what an item of the industry really is.
 * <p>
 * An iron ingot is a hundred millibuckets of iron, an iron block is a thousand, a nugget is ten; a bronze
 * ingot is copper and tin in one pile, because an alloy is a mixture and never a compound; a salt is a
 * lattice of two ions in one pile, which is the same shape again. None of them is a single molecule and
 * every one of them is a pile of substances, so what an item answers with is a {@link Blend} - the
 * substances it is made of and how much of each - and nothing else.
 * <p>
 * <b>The pile is what a machine hands to a reaction.</b> A reactor of the industry is fed cells, dusts and
 * ingots and never molecules, so the amount a stack holds is the amount the reaction spends, and the
 * elements of that pile are the quantities that balance, see {@link Blend#elementAmounts()}. An item that
 * does not answer is an item no reaction can use; an item that answers with an empty pile is one that holds
 * no chemistry at all, which a tool and a torch are.
 * <p>
 * <b>The interface lives here and the items live in the game.</b> This module knows nothing of a stack or a
 * picture; it says only what a thing that holds chemistry answers with, and the game makes its items answer
 * so. That keeps the balance of a reaction free of the game and lets a test hold chemistry in a value of
 * its own, see {@link Blend}.
 */
public interface ChemicalHolder {

    /**
     * The substances this thing is made of and how much of each.
     *
     * @return the pile, never {@code null} - an empty pile for a thing that holds no chemistry
     */
    Blend chemicals();
}
