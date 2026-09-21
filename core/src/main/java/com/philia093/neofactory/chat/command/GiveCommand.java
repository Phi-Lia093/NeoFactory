package com.philia093.neofactory.chat.command;

import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;

import java.util.List;
import java.util.Locale;

/**
 * Puts items into the inventory of the player.
 * <p>
 * The item is named the way the registry knows it, for example {@code planks_oak}
 * or {@code diamond_pickaxe}, see {@link ItemRegistry#byName(String)}. Without an
 * amount a single item is given, and a bigger amount is filled into the inventory
 * stack by stack - exactly like items that are picked up, which is why a count the
 * inventory cannot hold is reported instead of being dropped somewhere.
 */
public final class GiveCommand implements Command {

    /** Amount given when the line names no amount. */
    private static final int DEFAULT_COUNT = 1;

    @Override
    public String name() {
        return "give";
    }

    @Override
    public String usage() {
        return "/give <item> [count]";
    }

    @Override
    public String description() {
        return "Puts items into the inventory, for example \"/give planks_oak 32\"";
    }

    @Override
    public void run(CommandContext context, List<String> args) {
        if (args.isEmpty()) {
            context.log().addError("Usage: " + usage());
            return;
        }
        Item item = ItemRegistry.byName(args.get(0).toLowerCase(Locale.ROOT));
        if (item == null || item == Items.AIR) {
            context.log().addError("There is no item called \"" + args.get(0) + "\".");
            return;
        }
        int requested = DEFAULT_COUNT;
        if (args.size() > 1) {
            Integer parsed = parseCount(args.get(1));
            if (parsed == null) {
                context.log().addError("Usage: " + usage() + " - \"" + args.get(1)
                        + "\" is not a whole number above zero.");
                return;
            }
            requested = parsed;
        }
        PlayerInventory inventory = context.player().inventory();
        // No line can hand out more than the inventory could ever hold, which keeps a
        // mistyped amount from turning into a long loop over hundreds of stacks.
        int wanted = Math.min(requested, item.maxStackSize() * inventory.size());
        int leftover = wanted;
        while (leftover > 0) {
            int perStack = Math.min(leftover, item.maxStackSize());
            int refused = inventory.add(ItemStack.of(item, perStack));
            int stored = perStack - refused;
            leftover -= stored;
            if (stored <= 0) {
                break;
            }
        }
        int given = wanted - leftover;
        if (given <= 0) {
            context.log().addError("Unable to give " + item.displayName()
                    + ": the inventory is full.");
            return;
        }
        StringBuilder answer = new StringBuilder("Gave ").append(given).append(' ')
                .append(item.displayName());
        if (leftover > 0) {
            answer.append(". The inventory is full, ").append(leftover).append(" did not fit");
        }
        context.log().addSystem(answer.append('.').toString());
    }

    /** Reads an amount, {@code null} when it is not a positive whole number. */
    private static Integer parseCount(String value) {
        try {
            int count = Integer.parseInt(value);
            return count > 0 ? count : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
