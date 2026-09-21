package com.philia093.neofactory.recipe;

import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * What one place of a recipe accepts.
 * <p>
 * A recipe file names the items it takes, for example {@code "planks_oak"} or a list
 * of alternatives such as {@code ["log_oak", "log_birch"]}. An ingredient that holds
 * no item at all stands for a place that has to stay empty, which is how a shaped
 * recipe keeps a pattern from matching a grid that has something in the way.
 */
public final class Ingredient {

    /** Ingredient that only an empty place satisfies. */
    private static final Ingredient EMPTY = new Ingredient(new LinkedHashSet<>());

    private final Set<Item> items;

    private Ingredient(Set<Item> items) {
        this.items = items;
    }

    /**
     * Creates an ingredient from a list of accepted items.
     *
     * @param items items that satisfy this ingredient
     * @return the ingredient, empty when no item is given
     */
    public static Ingredient of(Item... items) {
        if (items == null || items.length == 0) {
            return EMPTY;
        }
        return new Ingredient(new LinkedHashSet<>(Arrays.asList(items)));
    }

    /** Ingredient that only an empty place satisfies. */
    public static Ingredient empty() {
        return EMPTY;
    }

    /** {@code true} when this ingredient asks for an empty place. */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** Items this ingredient accepts. */
    public Set<Item> items() {
        return java.util.Collections.unmodifiableSet(items);
    }

    /**
     * {@code true} when a stack satisfies this ingredient.
     *
     * @param stack stack that stands in the grid, may be empty
     * @return {@code true} when the ingredient accepts it
     */
    public boolean matches(ItemStack stack) {
        if (items.isEmpty()) {
            return stack == null || stack.isEmpty();
        }
        return stack != null && !stack.isEmpty() && items.contains(stack.item());
    }

    @Override
    public String toString() {
        if (items.isEmpty()) {
            return "Ingredient(empty)";
        }
        StringBuilder text = new StringBuilder("Ingredient(");
        boolean first = true;
        for (Item item : items) {
            if (!first) {
                text.append(", ");
            }
            text.append(item.name());
            first = false;
        }
        return text.append(')').toString();
    }
}
