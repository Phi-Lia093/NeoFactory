package com.philia093.neofactory.machine;

import com.philia093.neofactory.recipe.RecipeType;

import java.util.List;

/**
 * The forge hammer, the machine that beats an ingot into shape.
 * <p>
 * It is the machine a player builds when the anvil is too slow: an ingot goes in, a plate or a tool head comes
 * out, and the hammer falls on it until the work is done, see {@code assets/recipes/forging}. Its bar is the
 * tall one of the sheet - the hammer of the age of steam falls downwards, so its progress grows upwards from
 * the bottom, see {@link ProgressKind#BRONZE_HAMMER}.
 */
public class ForgeHammerMachine extends SteamMachine {

    /** Slot that holds the ingot that is beaten. */
    public static final int INPUT = 0;

    /** Slot the forged product appears in. */
    public static final int OUTPUT = 1;

    /** Screen of the hammer: the ingot on the left, what was forged on the right, one tank of steam. */
    public static final MachineScreen SCREEN = screen(MachinePressure.LOW);

    /**
     * Screen of a forge hammer of one pressure, named after the pressure it works at.
     *
     * @param pressure pressure the hammer works at
     * @return the screen of that hammer
     */
    public static MachineScreen screen(MachinePressure pressure) {
        return new MachineScreen(pressure.title("Forge Hammer"), MachineStyle.BRONZE,
                ProgressKind.BRONZE_HAMMER, List.of(SlotKind.HAMMER_INPUT), List.of(SlotKind.GENERIC),
                1, 0, false);
    }

    /** Creates an empty forge hammer of bronze. */
    public ForgeHammerMachine() {
        this(MachinePressure.LOW);
    }

    /**
     * Creates an empty forge hammer of a pressure.
     *
     * @param pressure pressure the hammer works at
     */
    protected ForgeHammerMachine(MachinePressure pressure) {
        super(screen(pressure), new MachineInventory(MachineInventory.Role.INPUT,
                MachineInventory.Role.OUTPUT), List.of(RecipeType.FORGING), pressure);
    }
}
