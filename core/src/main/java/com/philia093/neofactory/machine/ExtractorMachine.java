package com.philia093.neofactory.machine;

import com.philia093.neofactory.recipe.RecipeType;

import java.util.List;

/**
 * The extractor, the machine that squeezes what is in an item out of it.
 * <p>
 * Where a compressor presses a metal into a shape, an extractor takes something apart: latex out of a log, oil
 * out of seeds, see {@code assets/recipes/extracting}. It is the one machine of its age that is fed by hand
 * for a while - what comes out of it is worth the trips, and its steam goes out of the exhaust the wrench of a
 * player sets, see {@link SteamMachine}.
 */
public class ExtractorMachine extends SteamMachine {

    /** Slot that holds what is squeezed out. */
    public static final int INPUT = 0;

    /** Slot the product appears in. */
    public static final int OUTPUT = 1;

    /** Screen of the extractor: the item on the left, what comes out of it on the right, one tank of steam. */
    public static final MachineScreen SCREEN = screen(MachinePressure.LOW);

    /**
     * Screen of an extractor of one pressure, named after the pressure it works at.
     *
     * @param pressure pressure the extractor works at
     * @return the screen of that extractor
     */
    public static MachineScreen screen(MachinePressure pressure) {
        return new MachineScreen(pressure.title("Extractor"), MachineStyle.BRONZE,
                ProgressKind.BRONZE_EXTRACTOR, List.of(SlotKind.EXTRACTOR_INPUT),
                List.of(SlotKind.GENERIC), 1, 0, false);
    }

    /** Creates an empty extractor of bronze. */
    public ExtractorMachine() {
        this(MachinePressure.LOW);
    }

    /**
     * Creates an empty extractor of a pressure.
     *
     * @param pressure pressure the extractor works at
     */
    protected ExtractorMachine(MachinePressure pressure) {
        super(screen(pressure), new MachineInventory(MachineInventory.Role.INPUT,
                MachineInventory.Role.OUTPUT), List.of(RecipeType.EXTRACTING), pressure);
    }
}
