package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.recipe.RecipeType;

import java.util.List;

/**
 * The electric furnace, the machine that melts what it is given on the power of a line.
 * <p>
 * It is the furnace of the age of electricity and the first machine a player builds once a generator turns:
 * an ore, a lump of clay or a potato goes into the slot, the work is paid for out of the buffer with the
 * power of the line, and what comes out is the ingot, the brick or the baked potato the furnace of the age
 * of steam made. <b>The recipe is the very same file</b> - {@code assets/recipes/smelting} is read by every
 * furnace of the game - so a machine of the line is a faster furnace and not a furnace of its own, see
 * {@link SmeltingMachine} and {@code ProcessingRecipe}.
 * <p>
 * A machine of the line exists three times, once per tier of the casing, and what a player reads at it is the
 * tier in front of its name: {@code LV Electric Furnace}, {@code HV Electric Furnace}. Everything else - the
 * slots, the group of recipes, the current it takes - stands in {@link MachineFamilies}, and the class itself
 * is the name a player owns.
 */
public class ElectricFurnaceMachine extends ElectricMachine {

    /** Slot that holds what is smelted. */
    public static final int INPUT = 0;

    /** Slot the product appears in. */
    public static final int OUTPUT = 1;

    /** The row of {@link MachineFamilies} this machine is built from. */
    public static final MachineFamilies.Family FAMILY = new MachineFamilies.Family(
            "electric_furnace", "Electric Furnace", List.of(RecipeType.SMELTING),
            List.of(SlotKind.SMELTING), List.of(SlotKind.GENERIC), ElectricMachine.STANDARD_AMPS,
            ElectricFurnaceMachine::new);

    /**
     * Creates an empty electric furnace of a tier.
     *
     * @param tier tier the machine was built for, one of {@code LV}, {@code MV} and {@code HV}
     */
    public ElectricFurnaceMachine(Voltage tier) {
        super(FAMILY.screenOf(tier), FAMILY.inventory(), tier, FAMILY.maxAmps(), FAMILY.recipeTypes());
    }
}
