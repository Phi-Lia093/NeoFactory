package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.recipe.RecipeType;

import java.util.List;

/**
 * The electrolyzer, the machine a current drives a route through.
 * <p>
 * <b>It runs the routes of {@code assets/recipes/electrolysis} and no others.</b> Water does not split into
 * its two gases over a flame however hot the flame is, so a route of that folder is one a current has to
 * drive: the folder says so and this machine is what says "a current is here", see {@code ChemicalRecipe}
 * and {@link com.philia093.neofactory.chemistry.Conditions#current()}.
 * <p>
 * <b>One slot in, four out, and the tanks of the salts and the gases.</b> What is fed to it is a
 * substance - a dust in the slot or a solution in the tank at the foot of the panel - and what leaves it
 * is what the route hands over: brine and water into lye, chlorine and hydrogen, with the two gases
 * arriving in tanks of their own.
 * <p>
 * A machine of the line exists three times, once per tier of the casing, and what a player reads at it is
 * the tier in front of its name: {@code LV Electrolyzer}, {@code HV Electrolyzer}. Everything else stands
 * in {@link MachineFamilies}.
 */
public class ElectrolyzerMachine extends ElectricMachine {

    /** The slot a route is fed with. */
    public static final int INPUT = 0;

    /** First of the four slots a route hands its products into. */
    public static final int FIRST_OUTPUT = 1;

    /** Fluid one tank of the electrolyzer holds, which is four cells of a substance. */
    public static final int TANK_CAPACITY = 4_000;

    /** Current an electrolyzer takes at most, in amperes, twice what a machine of the usual kind takes. */
    public static final int FOUR_AMPS = 4;

    /** The row of {@link MachineFamilies} this machine is built from. */
    public static final MachineFamilies.Family FAMILY = new MachineFamilies.Family(
            "electrolyzer", "Electrolyzer", List.of(RecipeType.ELECTROLYSIS),
            List.of(SlotKind.GENERIC),
            List.of(SlotKind.GENERIC, SlotKind.GENERIC, SlotKind.GENERIC, SlotKind.GENERIC),
            1, 2, FOUR_AMPS, ElectrolyzerMachine::new);

    /**
     * Creates an empty electrolyzer of a tier.
     *
     * @param tier tier the machine was built for, one of {@code LV}, {@code MV} and {@code HV}
     */
    public ElectrolyzerMachine(Voltage tier) {
        super(FAMILY.screenOf(tier), FAMILY.inventory(), tier, FAMILY.maxAmps(), FAMILY.recipeTypes(),
                MachineTank.of(TANK_CAPACITY, MachineTank.Role.INPUT),
                MachineTank.of(TANK_CAPACITY, MachineTank.Role.OUTPUT),
                MachineTank.of(TANK_CAPACITY, MachineTank.Role.OUTPUT));
    }
}
