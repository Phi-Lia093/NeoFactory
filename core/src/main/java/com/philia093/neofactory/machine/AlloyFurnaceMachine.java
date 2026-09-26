package com.philia093.neofactory.machine;

import com.philia093.neofactory.recipe.RecipeType;

import java.util.List;

/**
 * The alloy furnace, the machine that melts two metals into one.
 * <p>
 * It is the smelter of an alloy: two slots feed it and one product leaves it, and a recipe of
 * {@code assets/recipes/alloy_smelting} names what goes together - bronze from copper and tin, for instance.
 * A recipe that asks for both slots needs both of them filled, because an ingredient is a place of the input
 * and not a stack, see {@link com.philia093.neofactory.recipe.SteamRecipe}.
 * <p>
 * Like every machine of its age it is paid for with steam and blows it out of an exhaust, see
 * {@link SteamMachine}. Its screen shows the flame of the bronze age for both of the slots it is fed and the
 * plain slot of the bronze panel for what it makes.
 */
public class AlloyFurnaceMachine extends SteamMachine {

    /** First slot of the two materials that are melted together. */
    public static final int INPUT = 0;

    /** Second slot of the two materials that are melted together. */
    public static final int SECOND_INPUT = 1;

    /** Slot the alloy appears in. */
    public static final int OUTPUT = 2;

    /** Screen of the alloy furnace: two slots of ore on the left, the alloy on the right, one tank of steam. */
    public static final MachineScreen SCREEN = screen(MachinePressure.LOW);

    /**
     * Screen of an alloy furnace of one pressure, named after the pressure it works at.
     *
     * @param pressure pressure the furnace works at
     * @return the screen of that furnace
     */
    public static MachineScreen screen(MachinePressure pressure) {
        return new MachineScreen(pressure.title("Alloy Furnace"), MachineStyle.BRONZE,
                ProgressKind.BRONZE, List.of(SlotKind.SMELTING, SlotKind.SMELTING),
                List.of(SlotKind.GENERIC), 1, 0, false);
    }

    /** Creates an empty alloy furnace of bronze. */
    public AlloyFurnaceMachine() {
        this(MachinePressure.LOW);
    }

    /**
     * Creates an empty alloy furnace of a pressure.
     *
     * @param pressure pressure the furnace works at
     */
    protected AlloyFurnaceMachine(MachinePressure pressure) {
        super(screen(pressure), new MachineInventory(MachineInventory.Role.INPUT,
                MachineInventory.Role.INPUT, MachineInventory.Role.OUTPUT),
                List.of(RecipeType.ALLOY_SMELTING), pressure);
    }
}
