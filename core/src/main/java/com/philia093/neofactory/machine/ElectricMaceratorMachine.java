package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.recipe.RecipeType;

import java.util.List;

/**
 * The macerator, the machine that turns ore into dust on the power of a line.
 * <p>
 * It is the grinder of the age of electricity: an ore goes in and dust comes out, and dust melts into more
 * than the ore would, see {@code assets/recipes/grinding}. The recipe is the very same file the grinder of
 * bronze reads, so a player who has ground an ore knows how a macerator works the moment it is built, see
 * {@link GrinderMachine} and {@code ProcessingRecipe}.
 * <p>
 * A machine of the line exists three times, once per tier of the casing, and what a player reads at it is the
 * tier in front of its name: {@code LV Macerator}, {@code MV Macerator}. Everything else - the slots, the
 * group of recipes, the current it takes - stands in {@link MachineFamilies}.
 */
public class ElectricMaceratorMachine extends ElectricMachine {

    /** Slot that holds what is ground. */
    public static final int INPUT = 0;

    /** Slot the dust appears in. */
    public static final int OUTPUT = 1;

    /** The row of {@link MachineFamilies} this machine is built from. */
    public static final MachineFamilies.Family FAMILY = new MachineFamilies.Family(
            "macerator", "Macerator", List.of(RecipeType.GRINDING),
            List.of(SlotKind.SMELTING), List.of(SlotKind.GENERIC), 0, 0, ElectricMachine.STANDARD_AMPS,
            ElectricMaceratorMachine::new);

    /**
     * Creates an empty macerator of a tier.
     *
     * @param tier tier the machine was built for, one of {@code LV}, {@code MV} and {@code HV}
     */
    public ElectricMaceratorMachine(Voltage tier) {
        super(FAMILY.screenOf(tier), FAMILY.inventory(), tier, FAMILY.maxAmps(), FAMILY.recipeTypes());
    }
}
