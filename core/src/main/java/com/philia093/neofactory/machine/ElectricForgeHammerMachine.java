package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.recipe.RecipeType;

import java.util.List;

/**
 * The forge hammer, the machine that beats an ingot into shape on the power of a line.
 * <p>
 * It is the hammer of the age of electricity: an ingot is beaten into a plate or a foil, see
 * {@code assets/recipes/forging}. The recipe is the very same file the forge hammer of bronze reads, so a
 * machine of the line works a metal the way the workshop already does, see {@link ForgeHammerMachine} and
 * {@code ProcessingRecipe}.
 * <p>
 * A machine of the line exists three times, once per tier of the casing, and what a player reads at it is the
 * tier in front of its name: {@code LV Forge Hammer}, {@code HV Forge Hammer}. Everything else - the slots,
 * the group of recipes, the current it takes - stands in {@link MachineFamilies}.
 */
public class ElectricForgeHammerMachine extends ElectricMachine {

    /** Slot that holds the metal that is beaten. */
    public static final int INPUT = 0;

    /** Slot what was beaten into shape appears in. */
    public static final int OUTPUT = 1;

    /** The row of {@link MachineFamilies} this machine is built from. */
    public static final MachineFamilies.Family FAMILY = new MachineFamilies.Family(
            "hammer", "Forge Hammer", List.of(RecipeType.FORGING),
            List.of(SlotKind.SMELTING), List.of(SlotKind.GENERIC), ElectricMachine.STANDARD_AMPS,
            ElectricForgeHammerMachine::new);

    /**
     * Creates an empty forge hammer of a tier.
     *
     * @param tier tier the machine was built for, one of {@code LV}, {@code MV} and {@code HV}
     */
    public ElectricForgeHammerMachine(Voltage tier) {
        super(FAMILY.screenOf(tier), FAMILY.inventory(), tier, FAMILY.maxAmps(), FAMILY.recipeTypes());
    }
}
