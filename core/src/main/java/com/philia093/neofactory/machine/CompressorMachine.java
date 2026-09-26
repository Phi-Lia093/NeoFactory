package com.philia093.neofactory.machine;

import com.philia093.neofactory.recipe.RecipeType;

import java.util.List;

/**
 * The compressor, the machine that presses an item together.
 * <p>
 * It squeezes what it is given until it takes less room: an ingot becomes a plate, dust becomes a block of it,
 * see {@code assets/recipes/compressing}. The bar of the compressor turns while the press works, which is why
 * it has a picture of its own among the bars of the sheet.
 */
public class CompressorMachine extends SteamMachine {

    /** Slot that holds what is pressed. */
    public static final int INPUT = 0;

    /** Slot the pressed product appears in. */
    public static final int OUTPUT = 1;

    /** Screen of the compressor: the item on the left, its plate on the right and one tank of steam. */
    public static final MachineScreen SCREEN = screen(MachinePressure.LOW);

    /**
     * Screen of a compressor of one pressure, named after the pressure it works at.
     *
     * @param pressure pressure the compressor works at
     * @return the screen of that compressor
     */
    public static MachineScreen screen(MachinePressure pressure) {
        return new MachineScreen(pressure.title("Compressor"), MachineStyle.BRONZE,
                ProgressKind.BRONZE_COMPRESSOR, List.of(SlotKind.COMPRESSOR_INPUT),
                List.of(SlotKind.GENERIC), 1, 0, false);
    }

    /** Creates an empty compressor of bronze. */
    public CompressorMachine() {
        this(MachinePressure.LOW);
    }

    /**
     * Creates an empty compressor of a pressure.
     *
     * @param pressure pressure the compressor works at
     */
    protected CompressorMachine(MachinePressure pressure) {
        super(screen(pressure), new MachineInventory(MachineInventory.Role.INPUT,
                MachineInventory.Role.OUTPUT), List.of(RecipeType.COMPRESSING), pressure);
    }
}
