package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.recipe.RecipeType;

import java.util.List;

/**
 * The alloy smelter, the machine that melts two metals into one on the power of a line.
 * <p>
 * It is the alloy furnace of the age of electricity: two slots feed it and one product leaves it, and a
 * recipe of {@code assets/recipes/alloy_smelting} names what goes together - bronze from copper and tin, for
 * instance. A recipe that asks for both slots needs both of them filled, because an ingredient is a place of
 * the input and not a stack, see {@code ProcessingRecipe}.
 * <p>
 * The recipe is the very same file the alloy furnace of bronze reads, so the two metals a player already
 * melts into bronze are the ones a machine of the line melts, see {@link AlloyFurnaceMachine}.
 * <p>
 * A machine of the line exists three times, once per tier of the casing, and what a player reads at it is the
 * tier in front of its name: {@code LV Alloy Smelter}, {@code HV Alloy Smelter}. Everything else - the slots,
 * the group of recipes, the current it takes - stands in {@link MachineFamilies}.
 */
public class ElectricAlloySmelterMachine extends ElectricMachine {

    /** First slot of the two materials that are melted together. */
    public static final int INPUT = 0;

    /** Second slot of the two materials that are melted together. */
    public static final int SECOND_INPUT = 1;

    /** Slot the alloy appears in. */
    public static final int OUTPUT = 2;

    /** The row of {@link MachineFamilies} this machine is built from. */
    public static final MachineFamilies.Family FAMILY = new MachineFamilies.Family(
            "alloy_smelter", "Alloy Smelter", List.of(RecipeType.ALLOY_SMELTING),
            List.of(SlotKind.SMELTING, SlotKind.SMELTING), List.of(SlotKind.GENERIC),
            0, 0,
            ElectricMachine.STANDARD_AMPS, ElectricAlloySmelterMachine::new);

    /**
     * Creates an empty alloy smelter of a tier.
     *
     * @param tier tier the machine was built for, one of {@code LV}, {@code MV} and {@code HV}
     */
    public ElectricAlloySmelterMachine(Voltage tier) {
        super(FAMILY.screenOf(tier), FAMILY.inventory(), tier, FAMILY.maxAmps(), FAMILY.recipeTypes());
    }
}
