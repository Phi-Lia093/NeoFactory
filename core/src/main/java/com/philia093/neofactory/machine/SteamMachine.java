package com.philia093.neofactory.machine;

import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.fluid.SimpleFluidStorage;
import com.philia093.neofactory.recipe.RecipeType;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.save.SaveTags;

import java.util.List;

/**
 * A machine that runs on the steam of a tank and blows it out of an exhaust.
 * <p>
 * This is the shape of every steam machine of the industry: a tank of {@value #STEAM_CAPACITY}
 * millibuckets that a line of pipes or a cell fills, a recipe that names how much steam it spends -
 * {@link MachineRecipe#steam()} - and an exhaust the steam leaves through. The three of them are what makes
 * a steam machine different from an electrical one, and all three are here so that the machines themselves
 * are a screen, a list of recipe types and nothing else.
 * <p>
 * <b>The steam is spent while the craft runs</b>, the way an electrical machine spends its energy: the
 * number of a recipe is what the whole craft costs, so one frame pays its share of it and the fraction that
 * does not make a whole millibucket is kept for the frames after it. A machine with no steam waits with its
 * input still in the slot and reports {@link MachineError#NO_STEAM}.
 * <p>
 * <b>The exhaust is a face of the block</b>, which the block entity knows and which a player sets with the
 * wrench, see {@code MachineBlockEntity#exhaustFace()}. <b>It has to be free:</b> the moment a craft ends
 * the block looks at the face, and a solid block standing there is reported and the machine refuses the
 * next recipe until the way is open again - the craft that was already running is finished, because the
 * steam of it was paid for and used.
 */
public abstract class SteamMachine extends RecipeMachine {

    /** Steam the tank of a steam machine holds, in millibuckets. */
    public static final int STEAM_CAPACITY = 16000;

    private final SimpleFluidStorage steam;

    /** Steam that is owed for the craft that runs, always below one millibucket. */
    private float steamDebt;

    /** {@code true} while the next recipe waits for the exhaust of the machine to be free. */
    private boolean exhaustBlocked;

    /** {@code true} between the end of a craft and the moment the block looked at the exhaust. */
    private boolean exhaustToCheck;

    /**
     * Creates a steam machine of the standard tank.
     *
     * @param screen how this machine is shown
     * @param inventory inventory whose slots carry the roles of the machine
     * @param recipeTypes types of recipe the machine reads
     */
    protected SteamMachine(MachineScreen screen, MachineInventory inventory,
            List<RecipeType> recipeTypes) {
        this(screen, inventory, recipeTypes, new SimpleFluidStorage(STEAM_CAPACITY));
    }

    /**
     * Creates a steam machine with a tank of its own.
     *
     * @param screen how this machine is shown
     * @param inventory inventory whose slots carry the roles of the machine
     * @param recipeTypes types of recipe the machine reads
     * @param tank the tank that holds its steam
     */
    protected SteamMachine(MachineScreen screen, MachineInventory inventory,
            List<RecipeType> recipeTypes, SimpleFluidStorage tank) {
        super(screen, inventory, new SimpleEnergyStorage(0), recipeTypes,
                new MachineTank(tank, MachineTank.Role.INPUT));
        this.steam = tank;
    }

    /** Steam in the tank of this machine, the one a line or a cell fills. */
    public SimpleFluidStorage steam() {
        return steam;
    }

    /**
     * Steam one second of a recipe takes.
     *
     * @param recipe recipe that runs
     * @return the millibuckets of one second, {@code 0} for a recipe that spends none
     */
    public static int steamPerSecond(MachineRecipe recipe) {
        return Math.max(0, recipe.steam());
    }

    /** {@code true} while the exhaust was found blocked and the next recipe has to wait for it. */
    public boolean isWaitingForExhaust() {
        return exhaustBlocked;
    }

    /**
     * Says how the exhaust of this machine was found, asked after every craft that spends steam.
     *
     * @param blocked {@code true} when a solid block stands in the way of the steam
     */
    public void reportExhaust(boolean blocked) {
        exhaustBlocked = blocked;
    }

    /**
     * Hands the check of the exhaust to the block, once after a craft that spent steam.
     *
     * @return {@code true} when the block has to look at the exhaust of this machine now
     */
    public boolean takesAnExhaustCheck() {
        boolean pending = exhaustToCheck;
        exhaustToCheck = false;
        return pending;
    }

    @Override
    public MachineError error() {
        if (exhaustBlocked) {
            return MachineError.NO_EXHAUST;
        }
        if (hasWork()) {
            return MachineError.NONE;
        }
        MachineRecipe recognised = recognisedRecipe();
        return recognised != null && !canSpend(recognised) ? MachineError.NO_STEAM : MachineError.NONE;
    }

    /**
     * The first recipe that recognises the input, refused while the exhaust is blocked or the steam is short.
     * <p>
     * A recipe that asks for more steam than the tank holds is no recipe for this machine yet: it waits with
     * its input in the slot and reports {@link MachineError#NO_STEAM}, the way an electrical machine waits for
     * its energy.
     */
    @Override
    protected MachineRecipe findRecipe() {
        if (exhaustBlocked) {
            return null;
        }
        MachineRecipe recipe = recognisedRecipe();
        return canSpend(recipe) ? recipe : null;
    }

    /**
     * The first recipe this machine recognises, whether or not it could pay for it.
     * <p>
     * {@link RecipeMachine} looks through the recipes of the types a machine reads, which is what a machine
     * of the game answers with; a machine that carries its own recipes - a test, for instance - overrides
     * this and not {@link #findRecipe()}, so that the question "is the input right" and the question "can
     * this machine pay" stay in one place.
     *
     * @return the recipe, or {@code null} when the input is nothing this machine knows
     */
    protected MachineRecipe recognisedRecipe() {
        return super.findRecipe();
    }

    /**
     * {@code true} when the tank can pay for a whole craft of a recipe.
     * <p>
     * A recipe that asks for more steam than the tank holds is no recipe for this machine yet: it waits with
     * its input in the slot and reports {@link MachineError#NO_STEAM}, the way an electrical machine waits for
     * its energy. A machine that is already running is not asked again - the steam of that craft is paid frame
     * by frame - which is why the question is asked while the recipe is looked for and not while it is paid.
     *
     * @param recipe recipe that was recognised, may be {@code null} without one
     * @return {@code true} when the machine may start it
     */
    protected boolean canSpend(MachineRecipe recipe) {
        if (recipe == null) {
            return false;
        }
        if (recipe.steam() <= 0) {
            return true;
        }
        return steam.fluid() == Fluids.STEAM && steam.amount() >= recipe.steam();
    }

    /**
     * Pays for one frame with the steam of the tank.
     * <p>
     * The number of the recipe is what the whole craft costs, so one frame pays the share of it that the
     * frame is worth and the fraction below a millibucket waits for the frames after it, exactly the way
     * {@link RecipeMachine} pays with energy.
     */
    @Override
    protected boolean payForWork(MachineRecipe recipe, float delta) {
        int cost = steamPerSecond(recipe);
        if (cost <= 0) {
            return true;
        }
        float seconds = Math.max(recipe.seconds(), delta);
        steamDebt += cost * delta / seconds;
        int whole = (int) steamDebt;
        if (whole <= 0) {
            // Not a whole millibucket yet: this frame is free, the next ones pay for it.
            return true;
        }
        int taken = steam.fluid() == Fluids.STEAM ? steam.drain(whole, false) : 0;
        if (taken < whole) {
            // The machine waits, so this frame counts for nothing and the debt is dropped.
            steamDebt = 0.0f;
            return false;
        }
        steamDebt -= taken;
        return true;
    }

    @Override
    protected void craftFinished(MachineRecipe recipe) {
        if (steamPerSecond(recipe) > 0) {
            // The steam of a recipe goes out of the exhaust, so the block has to look at that face before
            // the next recipe may start, see MachineBlockEntity and SteamMachine#takesAnExhaustCheck().
            exhaustToCheck = true;
        }
    }

    @Override
    protected void saveState(NbtCompound state) {
        super.saveState(state);
        state.putBoolean(SaveTags.EXHAUST_BLOCKED, exhaustBlocked);
    }

    @Override
    protected void loadState(NbtCompound state) {
        super.loadState(state);
        exhaustBlocked = state.getBoolean(SaveTags.EXHAUST_BLOCKED, false);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + name() + ", craft "
                + Math.round(craftProgress() * 100.0f) + "%, steam " + steam
                + (exhaustBlocked ? ", exhaust blocked" : "") + ")";
    }
}
