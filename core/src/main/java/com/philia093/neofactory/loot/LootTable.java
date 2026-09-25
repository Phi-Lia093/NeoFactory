package com.philia093.neofactory.loot;

import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * What a block leaves behind when it is broken.
 * <p>
 * A break of a block first asks whether the tool may harvest it at all, see
 * {@code MiningRule#canHarvest}, and then asks this table what to hand over. A table is a list of drops:
 * every drop names an item, how many of it are handed over and how often that happens, so a block that
 * always leaves one item and a block that leaves something rare are the same kind of file.
 * <p>
 * <b>A block without a table hands over itself.</b> That is the rule of the game and not of this class:
 * a table that exists is used, and a block that names none - which is most of them - drops the item of its
 * own block, see {@code MiningController}. A table with an empty list is therefore not the same as no
 * table at all: it is a block that breaks into nothing, such as glass.
 * <p>
 * The numbers of a drop are checked while it is read, so a table that asks for more items than fit in one
 * stack fails where it is written instead of silently handing out what fits.
 */
public final class LootTable {

    /**
     * One line of a table: an item, how many of it, and how often.
     *
     * @param item item this drop hands over
     * @param min least amount handed over, at least one
     * @param max most amount handed over, at least {@code min} and at most the stack of the item
     * @param chance share of the breaks this drop happens for, above zero and at most one
     */
    public record Drop(Item item, int min, int max, float chance) {

        /** Checks the numbers of a drop, called while a file is read. */
        public Drop {
            Objects.requireNonNull(item, "item");
            if (min < 1 || max < min) {
                throw new IllegalArgumentException("A drop of '" + item.name() + "' asks for " + min
                        + " to " + max + " items");
            }
            if (max > item.maxStackSize()) {
                throw new IllegalArgumentException("A drop of '" + item.name() + "' asks for " + max
                        + " items, but only " + item.maxStackSize() + " fit in one stack");
            }
            if (chance <= 0.0f || chance > 1.0f) {
                throw new IllegalArgumentException("The chance of a drop of '" + item.name()
                        + "' has to lie above 0 and at most 1: " + chance);
            }
        }

        /** {@code true} when this drop happens for one break, which is what its chance decides. */
        public boolean happens(Random random) {
            return chance >= 1.0f || random.nextFloat() < chance;
        }

        /** Amount this drop hands over for one break. */
        public int amount(Random random) {
            return min == max ? min : min + random.nextInt(max - min + 1);
        }

        /** One amount of a drop that happens, rolled into the stack a break hands over. */
        public ItemStack roll(Random random) {
            return ItemStack.of(item, amount(random));
        }
    }

    /** Table that hands nothing over, the table a block such as glass names. */
    private static final LootTable NOTHING = new LootTable(List.of());

    private final List<Drop> drops;

    /**
     * Creates a table.
     *
     * @param drops the lines of the table, in the order they are rolled
     */
    public LootTable(List<Drop> drops) {
        this.drops = List.copyOf(Objects.requireNonNull(drops, "drops"));
    }

    /** The lines of this table, in the order they are rolled. */
    public List<Drop> drops() {
        return drops;
    }

    /** {@code true} for a table that hands nothing over. */
    public boolean isEmpty() {
        return drops.isEmpty();
    }

    /** Table that hands nothing over, see {@link #isEmpty()}. */
    public static LootTable nothing() {
        return NOTHING;
    }

    /**
     * Rolls this table once, the way one broken block rolls it.
     *
     * @param random source of the amounts and of the chances
     * @return what the break hands over, empty for a table that hands nothing over
     */
    public List<ItemStack> roll(Random random) {
        List<ItemStack> rolled = new ArrayList<>(drops.size());
        for (Drop drop : drops) {
            if (drop.happens(random)) {
                rolled.add(drop.roll(random));
            }
        }
        return rolled;
    }

    @Override
    public String toString() {
        return "LootTable(" + drops.size() + " drops)";
    }
}
