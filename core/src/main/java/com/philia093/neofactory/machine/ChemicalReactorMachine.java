package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.chemistry.Blend;
import com.philia093.neofactory.chemistry.Chemical;
import com.philia093.neofactory.chemistry.Conditions;
import com.philia093.neofactory.chemistry.InorganicRecipe;
import com.philia093.neofactory.chemistry.Outcome;
import com.philia093.neofactory.chemistry.Phase;
import com.philia093.neofactory.chemistry.PolarEngine;
import com.philia093.neofactory.chemistry.PolarReactions;
import com.philia093.neofactory.chemistry.ReactionRouter;
import com.philia093.neofactory.chemistry.Substance;
import com.philia093.neofactory.chemistry.Warmth;
import com.philia093.neofactory.fluid.FluidStorage;
import com.philia093.neofactory.item.ChemicalItems;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.recipe.ChemicalRecipe;
import com.philia093.neofactory.recipe.RecipeGrid;
import com.philia093.neofactory.recipe.RecipeType;

import java.util.List;

/**
 * The chemical reactor, the machine a route of the industry runs in.
 * <p>
 * <b>A written route is asked first and the simple side of the table is asked after it.</b> Whatever stands
 * in the slots and the tanks of the machine is weighed as a pile of substances and handed to the routes the
 * game ships, see {@code ChemicalRecipe} and {@code assets/recipes/chemical_reacting}: a route that fits is
 * run, and a vessel no route fits is handed to the inference of the organic side, which rewrites the
 * molecules it finds by the ordinary rules of a solution, see {@code ReactionRouter}. A vessel that neither
 * of them answers does nothing at all - nothing is extrapolated and nothing is guessed, which is the line
 * the whole design is drawn on.
 * <p>
 * <b>What a reactor of one block infers is the simple side of the table.</b> A pot with two slots and two
 * tanks on a line is the flask of the trade and not a workshop: the families of the solution are run in it
 * and the art that needs a vessel of its own - a reaction of the light, a ring closed by six electrons, a
 * chain that grows itself, the reagents of a metal - is left to a machine built for it, see
 * {@link PolarReactions#simple()}. The routes written by hand are the whole of them either way, because what
 * an industry does is a fact somebody wrote down.
 * <p>
 * <b>Two slots and two tanks, in and out.</b> A route names substances and millibuckets: what is a solid
 * arrives as a dust in one of the two slots and what is a gas or a liquid arrives in one of the two tanks
 * at the foot of the panel. The products leave the same way - two slots for what is a solid, two tanks for
 * what flows - so the reactor of the low voltage and the one of the high voltage hold the very same thing
 * on a line of another strength. The panel arranges itself: a square of two slots stands for the two of a
 * side, see {@link MachineScreen#columns(int)}, so nothing here counts a pixel.
 * <p>
 * A machine of the line exists three times, once per tier of the casing, and what a player reads at it is
 * the tier in front of its name: {@code LV Chemical Reactor}, {@code HV Chemical Reactor}. Everything else
 * - the slots, the tanks, the group of recipes, the current it takes - stands in {@link MachineFamilies}.
 */
public class ChemicalReactorMachine extends ElectricMachine {

    /** First of the two slots a route is fed with. */
    public static final int FIRST_INPUT = 0;

    /** Last of the two slots a route hands its products into. */
    public static final int LAST_OUTPUT = 3;

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
            List.of(SlotKind.GENERIC, SlotKind.GENERIC),
            List.of(SlotKind.GENERIC, SlotKind.GENERIC),
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

    /**
     * The routes written by hand and the inference of the simple side, in that order.
     * <p>
     * A router is stateless: it is handed a pile and a vessel and answers with what the pile loses and gains,
     * so one of them serves every reactor of every tier and there is nothing in it to keep in step with a
     * save game.
     */
    private static final ReactionRouter ROUTER =
            ReactionRouter.of(new PolarEngine(PolarReactions.simple()));

    /**
     * What a reactor of one block offers a reaction of its own.
     * <p>
     * <b>A warmth and nothing else.</b> The machine has no thermometer and no pressure gauge, so the only
     * dimension it can honestly claim is that a working reactor is warm; a rule that asks for a catalyst, a
     * pressure or the light therefore never runs in it, which is the whole of what "a reactor of one block"
     * means. The written routes are read the same way and a dimension they do not name does not block them.
     */
    private static final Conditions REACTOR_CONDITIONS = Conditions.at(Warmth.HEATED);

    /** Seconds an inferred reaction takes, so that a pot of it is a craft and not a flash. */
    private static final float INFERRED_SECONDS = 10.0f;

    /** Millibuckets one piece of a solid substance carries, which is the dust of its shape. */
    private static final long SOLID_PIECE = 100;

    /** Millibuckets one piece of a fluid substance carries, which is the cell of its shape. */
    private static final long FLUID_PIECE = 1_000;

    /**
     * {@link MachineRecipe} this reactor works on: the routes written by hand first, then the inference.
     * <p>
     * <b>The written route is the whole of the machine's honesty</b> - a pot an industry has written down is
     * run by what was written and never by what looks like it - so the table is asked exactly as it was
     * before this override existed and the inference only ever sees a pot the table has already refused.
     *
     * @return the recipe, or {@code null} when the vessel is left alone
     */
    @Override
    protected MachineRecipe findRecipe() {
        MachineRecipe written = super.findRecipe();
        return written != null ? written : inferred();
    }

    /**
     * The reaction the molecules of the vessel are read to run, or {@code null}.
     * <p>
     * The pile of the vessel is read the way a written route reads it - every item of every slot as the
     * substance it stands for and every tank as the substances of its fluid, in millibuckets - and handed to
     * the router, which answers with what the pot loses and gains. What comes back is dressed as a route of
     * the industry, so that the loop of {@link RecipeMachine} pays for it, counts it and hands its products
     * over exactly the way it does for a route somebody wrote: there is one place where a machine pays and
     * one place where it hands things out.
     * <p>
     * <b>A product the catalog does not name is not a product a reactor can hand over.</b> What leaves a
     * machine leaves as an item or as a cell, and only a substance of the catalog has one; a pot whose
     * products are named is therefore run, and a pot that would make something nobody has a picture of is
     * left where it stands, which keeps a machine from inventing a substance it cannot show.
     *
     * @return the recipe, or {@code null} when nothing of the kind runs in this vessel
     */
    private MachineRecipe inferred() {
        Blend pile = pileOf();
        if (pile.isEmpty()) {
            return null;
        }
        Outcome outcome = ROUTER.route(pile, REACTOR_CONDITIONS);
        if (outcome == null || outcome.produced().isEmpty()) {
            return null;
        }
        Chemical primary = outcome.produced().components().keySet().iterator().next();
        Item result = ChemicalItems.itemOf(primary);
        if (result == null) {
            return null;
        }
        InorganicRecipe route = InorganicRecipe.builder("inferred").inputs(outcome.consumed())
                .outputs(outcome.produced()).primary(primary).electrons(outcome.electrons())
                .comment("inferred by the rules of the simple side").build();
        int euPerTick = Math.max(1, (int) Math.ceil(outcome.electrons() / INFERRED_SECONDS));
        return new ChemicalRecipe("inferred", RecipeType.CHEMICAL_REACTING, route,
                ItemStack.of(result, 1), INFERRED_SECONDS, euPerTick, 0);
    }

    /**
     * What the vessel holds, as millibuckets of the substances that stand in it.
     * <p>
     * <b>An item is the substance it was made for</b>, and how much of it one piece carries follows from the
     * shape it travels in: a solid arrives as a dust of a hundred millibuckets and everything else in a cell
     * of a thousand, the very reading {@code Amounts} hands out, so that a pot of molecules and a pot of
     * cells are weighed the same way a written route weighs them. A cell in a slot is therefore the fluid it
     * was filled with, an item the catalog does not know is not a substance at all - a stick in a slot is no
     * pot - and a tank is the substances of its fluid, which may be more than one: a solution is a fluid of
     * several substances and the whole of its millibuckets belongs to each of them, exactly as a written
     * route reads it.
     *
     * @return the pile of the vessel
     */
    private Blend pileOf() {
        MachineInput vessel = inputs();
        Blend pile = Blend.empty();
        RecipeGrid slots = vessel.items();
        for (int y = 0; y < slots.height(); y++) {
            for (int x = 0; x < slots.width(); x++) {
                ItemStack stack = slots.get(x, y);
                if (stack.isEmpty()) {
                    continue;
                }
                Substance substance = ChemicalItems.substanceOf(stack.item());
                if (substance != null) {
                    pile = pile.plus(Blend.of(substance.chemical(), pieceOf(substance) * stack.count()));
                }
            }
        }
        for (int index = 0; index < vessel.tankCount(); index++) {
            FluidStorage tank = vessel.tank(index);
            if (tank.fluid() == null) {
                continue;
            }
            for (Substance substance : tank.fluid().substances()) {
                pile = pile.plus(Blend.of(substance.chemical(), tank.amount()));
            }
        }
        return pile;
    }

    /** Millibuckets one piece of a substance carries, read from the shape it travels in. */
    private static long pieceOf(Substance substance) {
        return substance.phase() == Phase.SOLID ? SOLID_PIECE : FLUID_PIECE;
    }
}
