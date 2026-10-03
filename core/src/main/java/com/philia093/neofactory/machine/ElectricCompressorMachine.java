package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.recipe.RecipeType;

import java.util.List;

/**
 * The compressor, the machine that presses an item together on the power of a line.
 * <p>
 * It is the press of the age of electricity: an ingot becomes a plate and dust becomes a block of it, see
 * {@code assets/recipes/compressing}. The recipe is the very same file the compressor of bronze reads, so a
 * machine of the line works a material the way the workshop already does and only the line it stands on
 * changes, see {@link CompressorMachine} and {@code ProcessingRecipe}.
 * <p>
 * A machine of the line exists three times, once per tier of the casing, and what a player reads at it is the
 * tier in front of its name: {@code LV Compressor}, {@code HV Compressor}. Everything else - the slots, the
 * group of recipes, the current it takes - stands in {@link MachineFamilies}.
 */
public class ElectricCompressorMachine extends ElectricMachine {

    /** Slot that holds what is pressed. */
    public static final int INPUT = 0;

    /** Slot the pressed product appears in. */
    public static final int OUTPUT = 1;

    /** The row of {@link MachineFamilies} this machine is built from. */
    public static final MachineFamilies.Family FAMILY = new MachineFamilies.Family(
            "compressor", "Compressor", List.of(RecipeType.COMPRESSING),
            List.of(SlotKind.SMELTING), List.of(SlotKind.GENERIC), ElectricMachine.STANDARD_AMPS,
            ElectricCompressorMachine::new);

    /**
     * Creates an empty compressor of a tier.
     *
     * @param tier tier the machine was built for, one of {@code LV}, {@code MV} and {@code HV}
     */
    public ElectricCompressorMachine(Voltage tier) {
        super(FAMILY.screenOf(tier), FAMILY.inventory(), tier, FAMILY.maxAmps(), FAMILY.recipeTypes());
    }
}
