package com.philia093.neofactory.machine;

import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;

/**
 * What a machine of the line is fed with by hand, and what a piece of it is worth.
 * <p>
 * <b>A machine of the electrical age is fed twice.</b> Power arrives over a line of cables, which is what the
 * industry is built on, and a player who has no line yet - or one that stands still - may put a handful of
 * redstone dust into the cell at the foot of the panel instead: the dust is burned in the buffer of the
 * machine, and the machine works on it exactly as it works on what a line hands over, see
 * {@link MachineEnergyStorage#make(int)} and {@link ElectricMachine}.
 * <p>
 * <b>One reagent and no other</b>, because the shelf of a machine is not a place to keep things: a slot that
 * took what a player dropped on it would be a machine that is filled with junk by accident, so what may be
 * burned stands in one place and the slots of a machine refuse everything else, see {@link ElectricMachine}
 * and {@code MachineMenu}. Which item that is and what it is worth is the whole of this class - the sibling of
 * {@link Fuels}, which says the same for the machines that burn a flame.
 */
public final class Reagents {

    /**
     * Energy one piece of reagent is worth, in units of the game.
     * <p>
     * Eight hundred units is twenty five ticks of the low voltage, a little over a second of work of the first
     * machine of the line, and a cell of a machine of the high voltage holds four times that - so the amount
     * is a share of a buffer and not a way to run a workshop on a pile of dust, see
     * {@link ElectricMachine#BUFFER_TICKS}.
     */
    public static final int DUST_ENERGY = 800;

    private Reagents() {
        // Utility class: never instantiated.
    }

    /**
     * {@code true} when a stack is a reagent a machine of the line burns.
     *
     * @param stack stack to ask about, may be {@code null} or empty
     * @return {@code true} when the machine of a line may be fed with it
     */
    public static boolean isReagent(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.item() == Items.REDSTONE_DUST;
    }

    /**
     * Energy a piece of reagent is worth.
     *
     * @param stack stack to ask about, may be {@code null} or empty
     * @return the energy in units of the game, {@code 0} for a stack that is no reagent
     */
    public static int energyOf(ItemStack stack) {
        return isReagent(stack) ? DUST_ENERGY : 0;
    }
}
