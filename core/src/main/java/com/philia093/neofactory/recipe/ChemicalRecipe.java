package com.philia093.neofactory.recipe;

import com.philia093.neofactory.chemistry.InorganicRecipe;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.machine.MachineRecipe;

import java.util.List;
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
     * @param products what the route hands over, one stack per substance and piece
     * @param result the product the route is run for, one piece of it
     * @param seconds time one run takes
     * @param euPerTick power the route draws a tick, {@code 0} for a route that draws none
     * @param voltage voltage the route asks for, the tier of the machine that may run it
     */
    public ChemicalRecipe(String name, RecipeType type, InorganicRecipe route, List<ItemStack> products,
            ItemStack result, float seconds, int euPerTick, int voltage) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.route = Objects.requireNonNull(route, "route");
        this.products = List.copyOf(Objects.requireNonNull(products, "products"));
        this.result = Objects.requireNonNull(result, "result");
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
