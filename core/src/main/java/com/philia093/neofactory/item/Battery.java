package com.philia093.neofactory.item;

import com.philia093.neofactory.cable.Voltage;

/**
 * A cell that keeps energy and gives it back.
 * <p>
 * <b>A battery is an interface and not a kind of item.</b> What one day hands power to a machine or takes
 * it from a charger is a stack that is also a cell - a battery box, a tool that runs on it - and none of
 * those is a battery itself: they only ask one what it holds, what it may be filled with and how much of
 * it is left. The fifteen cells the game knows are listed by {@link Batteries}, and a stack carries its
 * own charge, see below.
 * <p>
 * <b>The charge of a battery is the wear of its stack.</b> A cell is an ordinary item whose life is its
 * capacity: a fresh battery holds everything it was built for and a battery that was drained to the last
 * unit is one whose life is spent. That is what makes the bar under its icon read as what is left in it,
 * what puts the charge into the tooltip of the stack as it is, and what carries a charged cell through a
 * save game without a field of its own, see {@link Damageable} and {@link ItemStack#damage()}.
 * <p>
 * <b>A battery that is empty is not destroyed.</b> Wear is what a tool is thrown away for, and a cell is
 * not a tool: it stands in a slot with nothing left in it until a player fills it again or takes it out,
 * see {@link #isDrained(ItemStack)}.
 * <p>
 * <b>One ampere is the unit a player reads and a euro is what the game counts.</b> A tier says what a cell
 * may be charged with, so a cell of the low voltage gives one ampere at its voltage for
 * {@link BatteryChemistry#nominalSeconds()}, and the units the game moves are what
 * {@link #capacity()}, {@link #insert(ItemStack, int)} and {@link #extract(ItemStack, int)} are counted
 * in. Nothing here charges a cell by itself: a machine of the line passes its own voltage on and a battery
 * box is what puts a charge back into a spent cell.
 */
public interface Battery {

    /**
     * Chemistry inside this cell.
     * <p>
     * It is what the colour of the cell, what a player is told about it in a tooltip and whether it takes a
     * charge again are read from, see {@link BatteryChemistry}.
     *
     * @return the chemistry, never {@code null}
     */
    BatteryChemistry chemistry();

    /**
     * Tier this cell was built for.
     * <p>
     * The tier is the line a cell is charged on and drained on, and it is the size of the cell as well: a
     * cell of the middle voltage holds four times what one of the low voltage holds.
     *
     * @return the tier, never {@code null}
     */
    Voltage voltage();

    /**
     * Energy a full cell holds, in units of the game.
     * <p>
     * The amount is the life of the item of the cell, so a stack of a battery that was built full reads as
     * a stack of this many units, see {@link Damageable#remainingDamage()}.
     *
     * @return the capacity, always positive
     */
    int capacity();

    /**
     * {@code true} when this cell takes a charge again.
     *
     * @return whether a battery box may fill it, see {@link BatteryChemistry#isRechargeable()}
     */
    default boolean isRechargeable() {
        return chemistry().isRechargeable();
    }

    /**
     * Energy a stack of this battery holds right now.
     * <p>
     * The stack must be a stack of the item of this battery, see {@link Batteries#of(Item)}. An empty
     * stack, or one whose item is no battery at all, holds nothing.
     *
     * @param stack stack to read, may be {@code null}
     * @return the charge in units of the game, never negative and at most {@link #capacity()}
     */
    default int chargeOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        return Math.min(stack.remainingDamage(), capacity());
    }

    /**
     * {@code true} while there is nothing left in the cell.
     * <p>
     * A drained cell is not destroyed and it is not broken either: it is a cell a player fills again, see
     * the note on the interface.
     *
     * @param stack stack to read, may be {@code null}
     * @return whether the cell holds nothing
     */
    default boolean isDrained(ItemStack stack) {
        return chargeOf(stack) <= 0;
    }

    /**
     * Puts energy into a cell.
     * <p>
     * <b>Only a cell that takes a charge again is filled</b>, so an acid and a mercury battery hand back a
     * zero for every unit offered, see {@link #isRechargeable()}. Nothing is lost over the brim either:
     * what does not fit is left with the caller, so a charger may offer all it has and keep the rest.
     *
     * @param stack stack of this battery to fill, may be {@code null}
     * @param eu    units of energy offered, may be zero or negative for no offer at all
     * @return units that went into the cell
     */
    default int insert(ItemStack stack, int eu) {
        if (!isRechargeable() || stack == null || eu <= 0) {
            return 0;
        }
        int room = Math.min(stack.damage(), capacity());
        int taken = Math.min(eu, room);
        stack.setDamage(stack.damage() - taken);
        return taken;
    }

    /**
     * Takes energy out of a cell.
     * <p>
     * A cell is emptied down to nothing and never past it, so a machine that asks for more than is left is
     * given what there was, see {@link #isDrained(ItemStack)}.
     *
     * @param stack stack of this battery to drain, may be {@code null}
     * @param eu    units of energy asked for, may be zero or negative for no request at all
     * @return units that came out of the cell
     */
    default int extract(ItemStack stack, int eu) {
        if (stack == null || eu <= 0) {
            return 0;
        }
        int given = Math.min(eu, chargeOf(stack));
        stack.applyDamage(given);
        return given;
    }
}
