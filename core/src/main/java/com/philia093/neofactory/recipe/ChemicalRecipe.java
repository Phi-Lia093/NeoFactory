package com.philia093.neofactory.recipe;

import com.philia093.neofactory.chemistry.Blend;
import com.philia093.neofactory.chemistry.Chemical;
import com.philia093.neofactory.chemistry.Fraction;
import com.philia093.neofactory.chemistry.InorganicRecipe;
import com.philia093.neofactory.chemistry.Substance;
import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.fluid.FluidIngredient;
import com.philia093.neofactory.fluid.FluidStorage;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.item.ChemicalItems;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.machine.MachineInput;
import com.philia093.neofactory.machine.MachineRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A route of the industry, written down in a file instead of being inferred.
 * <p>
 * <b>A file of this kind names substances and millibuckets and never items and places.</b> Silicon is
 * written with steam as {@code "carbon": 100, "water": 100} into {@code "carbon monoxide": 100,
 * "hydrogen": 100}, which is the way the chemistry of the game counts and the way a balance can be asked
 * about it: a file that leaves an atom behind is refused while it is read, and no amount of items can say
 * what a hundred millibuckets of a gas poured into a tank is. Every route is held as the very
 * {@link InorganicRecipe} the rest of the industry reasons with, so a file and a machine never tell two
 * different stories.
 * <p>
 * <b>The file may name what the vessel has to be.</b> {@code "temperature": [1000, 1600]},
 * {@code "pressure": [50, null]}, {@code "catalysts": ["iron"]} and {@code "medium": ["water"]} are read
 * the way the chemistry reads them; a dimension a file does not name is one the route does not care
 * about, and a dimension a machine does not measure does not block, see
 * {@link com.philia093.neofactory.chemistry.Conditions}.
 * <p>
 * <b>A route of the {@code electrolysis} kind is one a current drives.</b> The folder it lies in says so,
 * so water never splits into its two gases in a vessel that is only heated, see
 * {@link RecipeType#ELECTROLYSIS}.
 * <p>
 * The time and the power are read the way every recipe a machine works through reads them: a route costs
 * what it draws a tick over the seconds it takes, and the voltage it asks for is the tier of the machine
 * that may run it, see {@link EnergyRecipe}.
 */
public final class ChemicalRecipe implements MachineRecipe, EnergyRecipe {

    /** Seconds a route takes when its file does not say. */
    public static final float DEFAULT_SECONDS = ProcessingRecipe.DEFAULT_SECONDS;

    private final String name;
    private final RecipeType type;
    private final InorganicRecipe route;
    private final List<ItemStack> products;
    private final List<FluidIngredient> fluidIngredients;
    private final List<FluidIngredient> fluidProducts;
    private final ItemStack result;
    private final float seconds;
    private final int euPerTick;
    private final int voltage;

    /**
     * Creates a route.
     *
     * @param name name of the route, the name of its file
     * @param type kind of the route, which is the folder its file lies in
     * @param route the route itself, which is what the chemistry reasons with
     * @param result the product the route is run for, one piece of it
     * @param seconds time one run takes
     * @param euPerTick power the route draws a tick, {@code 0} for a route that draws none
     * @param voltage voltage the route asks for, the tier of the machine that may run it
     */
    public ChemicalRecipe(String name, RecipeType type, InorganicRecipe route, ItemStack result,
            float seconds, int euPerTick, int voltage) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.route = Objects.requireNonNull(route, "route");
        this.result = Objects.requireNonNull(result, "result");
        // What the route holds is what a machine is paid: a substance that is a solid arrives as a dust in
        // a slot and everything that flows arrives in a tank, and the products leave the same way.
        this.products = solidPiecesOf(route.outputs());
        this.fluidProducts = fluidPiecesOf(route.outputs());
        this.fluidIngredients = fluidPiecesOf(route.inputs());
        this.seconds = seconds > 0.0f ? seconds : DEFAULT_SECONDS;
        this.euPerTick = Math.max(0, euPerTick);
        this.voltage = Math.max(0, voltage);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public RecipeType type() {
        return type;
    }

    /** The route itself, the reading of the file the whole industry reasons with. */
    public InorganicRecipe route() {
        return route;
    }

    @Override
    public float seconds() {
        return seconds;
    }

    @Override
    public int euPerTick() {
        return euPerTick;
    }

    @Override
    public int voltage() {
        return voltage;
    }

    /**
     * Energy one run of the route costs, the total a machine of the electric age pays.
     *
     * @return what the route draws over the seconds it takes
     */
    @Override
    public int energy() {
        return totalEu();
    }

    /**
     * What the route hands over, one stack per substance and piece.
     *
     * @return the products, in the order the file names them
     */
    @Override
    public List<ItemStack> products() {
        return products;
    }

    /**
     * The product the route is run for, one piece of it.
     * <p>
     * A screen that has room for one picture of what a file makes shows this one, see
     * {@link com.philia093.neofactory.gui.recipe.RecipeCategory}: a route makes several substances and
     * the file says which of them it is run for, see {@link InorganicRecipe#primary()}.
     */
    @Override
    public ItemStack result() {
        return result;
    }

    /** Fluid the route takes, one tankful per substance of its input that is not a solid. */
    @Override
    public List<FluidIngredient> fluidIngredients() {
        return fluidIngredients;
    }

    /** Fluid the route makes, one tankful per substance of its output that is not a solid. */
    @Override
    public List<FluidIngredient> fluidProducts() {
        return fluidProducts;
    }

    /**
     * {@code true} when the vessel holds everything one run of the route asks for.
     * <p>
     * <b>What the vessel holds is a pile of substances and not a set of items.</b> Every slot is read as
     * the pile of what stands in it and every tank as the substances of the fluid it holds, and the two
     * of them are added up: a route is paid out of the sum and not out of a place, see
     * {@link InorganicRecipe#runs(com.philia093.neofactory.chemistry.Blend)}, which is the very rule the
     * routes themselves are written with.
     *
     * @param input what the machine offers
     * @return {@code true} when {@link #consume(MachineInput)} may be called
     */
    @Override
    public boolean matches(MachineInput input) {
        return route.runs(pileOf(input)) >= 1;
    }

    /**
     * Takes one run of the route out of the machine.
     * <p>
     * A substance that is a solid is taken out of the slots, one whole piece at a time, which is what a
     * dust of it is worth; a substance that flows is drained out of a tank in millibuckets, which is what
     * a tank is measured in. What a route asks for beyond one run is left where it stands, so a vessel
     * that holds three runs is worked on one at a time.
     *
     * @param input what the machine offers
     */
    @Override
    public void consume(MachineInput input) {
        for (Map.Entry<Chemical, Fraction> need : route.inputs().components().entrySet()) {
            Item item = ChemicalItems.itemOf(need.getKey());
            if (item != null) {
                takePiecesOf(input.items(), item, need.getKey(), need.getValue());
            }
        }
        for (FluidIngredient fluid : fluidIngredients) {
            input.drain(fluid.fluid(), fluid.amount());
        }
    }

    /** Takes the whole pieces of a substance a route needs out of the slots of a machine. */
    private static void takePiecesOf(RecipeGrid grid, Item item, Chemical substance, Fraction amount) {
        Fraction piece = item.chemicals().amountOf(substance);
        if (piece.isZero()) {
            return;
        }
        Fraction pieces = amount.dividedBy(piece);
        if (pieces.denominator() != 1) {
            // A route whose amount is not a whole piece of the item can never be paid, and matches() said
            // as much: nothing is taken here.
            return;
        }
        int left = (int) pieces.numerator();
        for (int y = 0; y < grid.height() && left > 0; y++) {
            for (int x = 0; x < grid.width() && left > 0; x++) {
                ItemStack stack = grid.get(x, y);
                if (stack.isEmpty() || stack.item() != item) {
                    continue;
                }
                int taken = Math.min(left, stack.count());
                stack.setCount(stack.count() - taken);
                left -= taken;
            }
        }
    }

    /** The pile the slots and the tanks of a machine hold together, in millibuckets. */
    private static Blend pileOf(MachineInput input) {
        Blend pile = Blend.empty();
        RecipeGrid grid = input.items();
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                ItemStack stack = grid.get(x, y);
                if (!stack.isEmpty()) {
                    pile = pile.plus(stack.chemicals().times(stack.count()));
                }
            }
        }
        for (int index = 0; index < input.tankCount(); index++) {
            FluidStorage tank = input.tank(index);
            Fluid fluid = tank.fluid();
            if (fluid == null) {
                continue;
            }
            // A fluid of the industry is a substance, so a tank holds what it is made of. The lava of a
            // mountain is a mixture and carries nothing, so it is no reactant at all.
            for (Substance substance : fluid.substances()) {
                pile = pile.plus(Blend.of(substance.chemical(), tank.amount()));
            }
        }
        return pile;
    }

    /** The substances of a pile that are solids, as whole pieces of the item each one travels as. */
    private static List<ItemStack> solidPiecesOf(Blend pile) {
        List<ItemStack> pieces = new ArrayList<>();
        for (Map.Entry<Chemical, Fraction> entry : pile.components().entrySet()) {
            Item item = ChemicalItems.itemOf(entry.getKey());
            if (item == null || item.isFluidContainer()) {
                continue;
            }
            Fraction piece = item.chemicals().amountOf(entry.getKey());
            Fraction count = piece.isZero() ? Fraction.ZERO : entry.getValue().dividedBy(piece);
            if (count.denominator() == 1 && count.numerator() > 0) {
                pieces.add(ItemStack.of(item, (int) count.numerator()));
            }
        }
        return List.copyOf(pieces);
    }

    /** The substances of a pile that flow, as tankfuls of the fluid each one is. */
    private static List<FluidIngredient> fluidPiecesOf(Blend pile) {
        List<FluidIngredient> fluids = new ArrayList<>();
        for (Map.Entry<Chemical, Fraction> entry : pile.components().entrySet()) {
            Item item = ChemicalItems.itemOf(entry.getKey());
            Substance substance = ChemicalItems.substanceOf(item);
            Fluid fluid = substance == null ? null : Fluids.byName(substance.name());
            if (fluid != null && entry.getValue().denominator() == 1) {
                fluids.add(FluidIngredient.of(fluid, (int) entry.getValue().numerator()));
            }
        }
        return List.copyOf(fluids);
    }

    /**
     * {@code false}: a route of the industry is no arrangement of a grid.
     * <p>
     * A craft of the player's grid is found by laying items out, while a route is found by what stands in
     * a vessel and how much of it - millibuckets of a substance and not places of a grid - so no grid ever
     * matches one of these. The reactor asks the route itself, see
     * {@link com.philia093.neofactory.chemistry.InorganicRecipe#matches}.
     *
     * @param grid items that are offered
     * @return {@code false}, always
     */
    @Override
    public boolean matches(RecipeGrid grid) {
        return false;
    }

    /**
     * Takes nothing: a route is never paid out of a grid, see {@link #matches(RecipeGrid)}.
     *
     * @param grid items that are offered
     */
    @Override
    public void consume(RecipeGrid grid) {
        // A route that never matches a grid has nothing to take out of one.
    }

    @Override
    public String toString() {
        return "ChemicalRecipe(" + name + ", " + route + ", " + seconds + "s, " + euPerTick + "EU/t at "
                + voltage + "V)";
    }
}
