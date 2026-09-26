package com.philia093.neofactory.machine;

import com.philia093.neofactory.recipe.RecipeType;

import java.util.List;

/**
 * The grinder, the machine that turns ore into dust.
 * <p>
 * A grinder is what makes a metal worth dig: an ore that goes through it becomes dust, and dust melts into
 * more than the ore would, see {@code assets/recipes/grinding}. Its screen is the one of the grinder of the
 * sheet: the ore that is fed stands under the picture of a heap of it and the dust it makes under the picture
 * of what it leaves behind, with the bar of the grinder between the two.
 */
public class GrinderMachine extends SteamMachine {

    /** Slot that holds what is ground. */
    public static final int INPUT = 0;

    /** Slot the dust appears in. */
    public static final int OUTPUT = 1;

    /** Screen of the grinder: the ore on the left, the dust on the right and one tank of steam. */
    public static final MachineScreen SCREEN = screen(MachinePressure.LOW);

    /**
     * Screen of a grinder of one pressure, named after the pressure it works at.
     *
     * @param pressure pressure the grinder works at
     * @return the screen of that grinder
     */
    public static MachineScreen screen(MachinePressure pressure) {
        return new MachineScreen(pressure.title("Grinder"), MachineStyle.BRONZE,
                ProgressKind.BRONZE_GRINDER, List.of(SlotKind.GRINDER_INPUT),
                List.of(SlotKind.GRINDER_OUTPUT), 1, 0, false);
    }

    /** Creates an empty grinder of bronze. */
    public GrinderMachine() {
        this(MachinePressure.LOW);
    }

    /**
     * Creates an empty grinder of a pressure.
     *
     * @param pressure pressure the grinder works at
     */
    protected GrinderMachine(MachinePressure pressure) {
        super(screen(pressure), new MachineInventory(MachineInventory.Role.INPUT,
                MachineInventory.Role.OUTPUT), List.of(RecipeType.GRINDING), pressure);
    }
}
