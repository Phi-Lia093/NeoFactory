package com.philia093.neofactory.machine;

import com.philia093.neofactory.recipe.RecipeType;

import java.util.List;

/**
 * The steam furnace, the machine that smelts ore with the steam of the boiler.
 * <p>
 * It is the first machine a player builds once the boiler stands: a heap of ore goes in, an ingot comes out,
 * and the work is paid for with the steam of the boiler instead of with a flame of its own - which is why it
 * needs no fuel slot and no flame, see {@link SteamMachine}. Its recipes live in
 * {@code assets/recipes/steam_smelting} and each of them says how much steam one craft spends, so an ore that
 * melts easily costs less of it than one that does not.
 * <p>
 * The screen is the bronze one of the age of steam: the flame of the bronze age for the ore and the plain slot
 * for the ingot, with the tank of steam at the foot of the panel.
 */
public class SteamFurnaceMachine extends SteamMachine {

    /** Slot that holds what is smelted. */
    public static final int INPUT = 0;

    /** Slot the result appears in. */
    public static final int OUTPUT = 1;

    /** Screen of the furnace: the ore on the left, the ingot on the right and one tank of steam. */
    public static final MachineScreen SCREEN = screen(MachinePressure.LOW);

    /**
     * Screen of a steam furnace of one pressure.
     * <p>
     * The two furnaces of the age of steam are the same machine, so they are the same screen with another
     * name: {@code Steam Furnace} for the furnace of bronze and {@code High Pressure Steam Furnace} for the
     * one of steel, see {@link MachinePressure#title(String)}.
     *
     * @param pressure pressure the furnace works at
     * @return the screen of that furnace
     */
    public static MachineScreen screen(MachinePressure pressure) {
        return new MachineScreen(pressure.title("Steam Furnace"), MachineStyle.BRONZE, ProgressKind.BRONZE,
                List.of(SlotKind.SMELTING), List.of(SlotKind.GENERIC), 1, 0, false);
    }

    /** Creates an empty steam furnace of bronze. */
    public SteamFurnaceMachine() {
        this(MachinePressure.LOW);
    }

    /**
     * Creates an empty steam furnace of a pressure.
     * <p>
     * It reads the very recipes of the furnace of bronze and works them in half the time at twice the steam a
     * tick, see {@link MachinePressure}.
     *
     * @param pressure pressure the furnace works at
     */
    protected SteamFurnaceMachine(MachinePressure pressure) {
        super(screen(pressure), new MachineInventory(MachineInventory.Role.INPUT,
                MachineInventory.Role.OUTPUT), List.of(RecipeType.STEAM_SMELTING), pressure);
    }
}
