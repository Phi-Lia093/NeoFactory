package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.recipe.RecipeType;

import java.util.List;

/**
 * The extractor, the machine that squeezes an item out of another on the power of a line.
 * <p>
 * It is the extractor of the age of electricity: clay gives up its balls, gravel gives up flint and an ore of
 * coal gives up the coal in it, see {@code assets/recipes/extracting}. The recipe is the very same file the
 * extractor of bronze reads, so a machine of the line works a material the way the workshop already does, see
 * {@link ExtractorMachine} and {@code ProcessingRecipe}.
 * <p>
 * A machine of the line exists three times, once per tier of the casing, and what a player reads at it is the
 * tier in front of its name: {@code LV Extractor}, {@code MV Extractor}. Everything else - the slots, the
 * group of recipes, the current it takes - stands in {@link MachineFamilies}.
 */
public class ElectricExtractorMachine extends ElectricMachine {

    /** Slot that holds what is squeezed. */
    public static final int INPUT = 0;

    /** Slot what was squeezed out appears in. */
    public static final int OUTPUT = 1;

    /** The row of {@link MachineFamilies} this machine is built from. */
    public static final MachineFamilies.Family FAMILY = new MachineFamilies.Family(
            "extractor", "Extractor", List.of(RecipeType.EXTRACTING),
            List.of(SlotKind.SMELTING), List.of(SlotKind.GENERIC), 0, 0, ElectricMachine.STANDARD_AMPS,
            ElectricExtractorMachine::new);

    /**
     * Creates an empty extractor of a tier.
     *
     * @param tier tier the machine was built for, one of {@code LV}, {@code MV} and {@code HV}
     */
    public ElectricExtractorMachine(Voltage tier) {
        super(FAMILY.screenOf(tier), FAMILY.inventory(), tier, FAMILY.maxAmps(), FAMILY.recipeTypes());
    }
}
