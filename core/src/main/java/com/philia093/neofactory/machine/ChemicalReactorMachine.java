package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.recipe.RecipeType;

import java.util.List;

/**
 * The chemical reactor, the machine a route of the industry runs in.
 * <p>
 * <b>It is the machine of the written routes.</b> Whatever stands in its slots and its tanks is weighed
 * and handed to the routes the game ships, see {@code ChemicalRecipe} and
 * {@code assets/recipes/chemical_reacting}: a route that fits what the vessel holds is run, and a vessel
 * no route fits does nothing at all. Nothing is inferred here, which is the whole point of writing the
 * routes down - the machine may say "not this" and never "something like this".
 * <p>
 * <b>Four slots and four tanks, in and out.</b> A route names substances and millibuckets: what is a solid
 * arrives as a dust in one of the four slots and what is a gas or a liquid arrives in one of the two tanks
 * at the foot of the panel. The products leave the same way - four slots for what is a solid, two tanks for
 * what flows - so the reactor of the low voltage and the one of the high voltage hold the very same thing
 * on a line of another strength.
 * <p>
 * A machine of the line exists three times, once per tier of the casing, and what a player reads at it is
 * the tier in front of its name: {@code LV Chemical Reactor}, {@code HV Chemical Reactor}. Everything else
 * - the slots, the tanks, the group of recipes, the current it takes - stands in {@link MachineFamilies}.
 */
public class ChemicalReactorMachine extends ElectricMachine {

    /** First of the four slots a route is fed with. */
    public static final int FIRST_INPUT = 0;

    /** Last of the four slots a route hands its products into. */
    public static final int LAST_OUTPUT = 7;

    /** Fluid one tank of the reactor holds, which is four cells of a substance. */
    public static final int TANK_CAPACITY = 4_000;

    /**
     * Current a reactor takes at most, in amperes.
     * <p>
     * Twice what a machine of the usual kind takes: a route of the industry moves a hundred millibuckets
     * of a substance at once and draws more than a craft of an item does, so a reactor is built to take
     * the amperes of it rather than to be spread over two lines.
     */
    public static final int FOUR_AMPS = 4;

    /** The row of {@link MachineFamilies} this machine is built from. */
    public static final MachineFamilies.Family FAMILY = new MachineFamilies.Family(
            "chemical_reactor", "Chemical Reactor", List.of(RecipeType.CHEMICAL_REACTING),
            List.of(SlotKind.GENERIC, SlotKind.GENERIC, SlotKind.GENERIC, SlotKind.GENERIC),
            List.of(SlotKind.GENERIC, SlotKind.GENERIC, SlotKind.GENERIC, SlotKind.GENERIC),
            2, 2, FOUR_AMPS, ChemicalReactorMachine::new);

    /**
     * Creates an empty chemical reactor of a tier.
     *
     * @param tier tier the machine was built for, one of {@code LV}, {@code MV} and {@code HV}
     */
    public ChemicalReactorMachine(Voltage tier) {
        super(FAMILY.screenOf(tier), FAMILY.inventory(), tier, FAMILY.maxAmps(), FAMILY.recipeTypes(),
                MachineTank.of(TANK_CAPACITY, MachineTank.Role.INPUT),
                MachineTank.of(TANK_CAPACITY, MachineTank.Role.INPUT),
                MachineTank.of(TANK_CAPACITY, MachineTank.Role.OUTPUT),
                MachineTank.of(TANK_CAPACITY, MachineTank.Role.OUTPUT));
    }
}
