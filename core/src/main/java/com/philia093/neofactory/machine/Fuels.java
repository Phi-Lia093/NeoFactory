package com.philia093.neofactory.machine;

import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * What a machine may burn, and for how long.
 * <p>
 * A furnace needs fuel rather than energy, and which items burn is a table of its own -
 * the same item may be a material for one recipe and a fuel for the furnace. The table
 * is filled on first use, because the items of the game are only registered once the
 * game has started, see {@code Items#registerAll()}.
 */
public final class Fuels {

    private static Map<Item, Float> table;

    private Fuels() {
        // Utility class: never instantiated.
    }

    /**
     * Seconds one item of this kind burns.
     *
     * @param item item to look up, may be {@code null} or air
     * @return the time in seconds, {@code 0} when the item does not burn
     */
    public static float secondsOf(Item item) {
        if (item == null) {
            return 0.0f;
        }
        Float seconds = table().get(item);
        return seconds == null ? 0.0f : seconds;
    }

    /**
     * {@code true} when an item may be used as fuel.
     *
     * @param item item to look up
     * @return {@code true} when it burns
     */
    public static boolean isFuel(Item item) {
        return secondsOf(item) > 0.0f;
    }

    /** The table of fuels, filled once so that items may be compared by identity. */
    private static Map<Item, Float> table() {
        if (table == null) {
            Map<Item, Float> fuels = new HashMap<>();
            fuels.put(Items.COAL, 80.0f);
            fuels.put(Items.CHARCOAL, 80.0f);
            fuels.put(Items.LOG_OAK, 15.0f);
            fuels.put(Items.PLANKS_OAK, 15.0f);
            fuels.put(Items.STICK, 5.0f);
            fuels.put(Items.BLAZE_ROD, 120.0f);
            table = fuels;
        }
        return table;
    }
}
