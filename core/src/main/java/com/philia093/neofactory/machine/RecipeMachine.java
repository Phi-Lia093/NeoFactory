package com.philia093.neofactory.machine;

import com.philia093.neofactory.recipe.Recipe;
import com.philia093.neofactory.recipe.RecipeRegistry;
import com.philia093.neofactory.recipe.RecipeType;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.save.SaveTags;

import java.util.List;

/**
 * A machine that works through a {@link MachineRecipe} on energy.
 * <p>
 * The class holds the loop every machine of the game repeats: look for a recipe that
 * recognises the input, make sure the products fit the output, pay for the work, count
 * the time and hand the products out. A machine therefore only describes its slots, its
 * tanks and the recipe types it reads, which keeps a large one - a reactor with two item
 * inputs, a fluid input, two item outputs, a fluid output and an upgrade slot - as short
 * as a furnace.
 * <p>
 * <b>Energy</b> is paid per frame, in the share of the cost of the whole craft that the
 * frame is worth: a recipe that costs 200 units over ten seconds takes 20 units a
 * second, and the fraction that does not make a whole unit is kept for the frames after
 * it. A machine without enough energy waits - so the work already done is kept, the same
 * way a furnace keeps its fire - and it never pays more than the recipe asked for.
 * <p>
 * A machine that also burns fuel, such as the furnace, does not extend this class: it
 * keeps its own {@link Machine#update(float)} and only shares the storage and the views
 * of {@link Machine}.
 */
public abstract class RecipeMachine extends Machine implements ProgressMachine {

    private float craftSeconds;
    private float craftTotal = 1.0f;

    /**
     * Recipe this machine works on, {@code null} while it is idle.
     * <p>
     * The recipe is remembered because the machine swallows its input when the work starts, see
     * {@link #startCraft(float)}: looking for a recipe again would not find one, because the input it was
     * looking for is gone.
     */
    private MachineRecipe craft;

    /**
     * Energy that is owed but was not paid yet, always below one unit.
     * <p>
     * A machine that pays in whole ticks of its own - a machine of the electric age, which spends the power of
     * its tier every tick - reads and writes this very field instead of keeping a second fraction of its own,
     * see {@code ElectricMachine#payForWork}: the rule "a fraction below a whole unit waits for the frames
     * after it" then lives in one place and travels with a save game.
     */
    protected float energyDebt;

    /**
     * Creates a machine.
     *
     * @param screen how this machine is shown, see {@link MachineScreen}
     * @param inventory inventory whose slots carry the roles of the machine
     * @param energy storage the machine takes its energy from
     * @param recipeTypes types of recipe the machine reads
     * @param tanks tanks of fluid the machine holds, may be empty
     */
    protected RecipeMachine(MachineScreen screen, MachineInventory inventory,
            EnergyStorage energy, List<RecipeType> recipeTypes, MachineTank... tanks) {
        super(screen, inventory, energy, recipeTypes, tanks);
    }

    /**
     * {@link MachineError#NO_POWER} while the machine has work but no energy to do it with.
     * <p>
     * The icon of the machine screen reports it, which is what tells a player why nothing moves although the
     * input is right. A machine that is idle asks the same question: an input that a recipe would recognise
     * and no energy to start it is worth the same icon.
     */
    @Override
    public MachineError error() {
        if (energy().amount() <= 0 && (craft != null || findRecipe() != null)) {
            return MachineError.NO_POWER;
        }
        return MachineError.NONE;
    }

    @Override
    public float craftProgress() {
        return craftTotal <= 0.0f ? 0.0f : Math.min(1.0f, craftSeconds / craftTotal);
    }

    /**
     * {@code 0}, because an electrical machine burns nothing.
     * <p>
     * A screen draws the buffer of the machine instead, see {@link #energy()}.
     */
    @Override
    public float burnProgress() {
        return 0.0f;
    }

    /** {@code true} while the machine works on a recipe it has already swallowed the input of. */
    @Override
    public boolean isRunning() {
        return craft != null;
    }

    /**
     * {@code true} while the machine works on a recipe, asked by a type that makes its own loop.
     *
     * @return {@code true} when a craft is under way
     */
    protected boolean hasWork() {
        return craft != null;
    }

    /**
     * The recipe this machine works on right now, {@code null} while it is idle.
     * <p>
     * <b>A type that has to know what the work costs asks this and not the field.</b> A machine of the electric
     * age reads the power of the recipe it runs to work out the amperes it asks a line for, see
     * {@code ElectricMachine#requestAmps()}, and it is the recipe of the craft and not the one the input
     * happens to match: what the machine swallows when the work starts is gone, so looking for a recipe again
     * would find another one or none.
     *
     * @return the recipe that runs, or {@code null} when the machine is idle
     */
    protected MachineRecipe currentCraft() {
        return craft;
    }

    /**
     * One frame of a machine that works through a recipe.
     * <p>
     * <b>The machine swallows its input when the work starts and not when it ends.</b> A recipe that is
     * recognised and fits the output slots is paid for first, so that a machine without power eats nothing at
     * all, and the input is taken the very moment the work begins. Everything after that is the craft that
     * was started: the machine pays for the share of every frame, counts the time and hands the products over
     * when the recipe is done.
     * <p>
     * <b>A machine that cannot pay stands still.</b> A frame that is not paid for does not count, and the
     * work already done and the input that was swallowed are kept: a shortage of power, or a machine whose
     * block was broken and built again, costs the time it waited and no more, and a machine that comes back
     * to power takes its craft up where it left it.
     */
    @Override
    protected void update(float delta) {
        if (craft == null) {
            startCraft(delta);
            return;
        }
        float share = payForWork(craft, delta);
        if (share <= 0.0f) {
            // The machine could not pay anything this frame, so it waits with its work: what it swallowed is
            // kept and the next frame asks again, see payForWork.
            return;
        }
        craftSeconds += delta * share;
        if (craftSeconds >= craftTotal) {
            craft.produce(outputs());
            MachineRecipe finished = craft;
            forgetCraft();
            craftFinished(finished);
        }
    }

    /**
     * Does what a finished craft leaves behind.
     * <p>
     * The loop calls this once the products are handed over, which is the moment a machine may look at the
     * world again: a steam machine asks its block whether the exhaust of the recipe it just ran was free, see
     * {@link SteamMachine}. The recipe that ended is handed in because the machine has already forgotten it.
     *
     * @param recipe recipe whose products were just handed over
     */
    protected void craftFinished(MachineRecipe recipe) {
        // A machine that cares about more than its slots writes this, see SteamMachine.
    }

    /**
     * Swallows the input of a recipe and starts the work.
     * <p>
     * Nothing is taken unless a recipe is recognised, its products fit into the output slots and the frame
     * can be paid for, so a machine that has no power waits with its input still in the slot and reports
     * {@link MachineError#NO_POWER}.
     *
     * @param delta time since the last frame in seconds, what the first frame of the work costs
     */
    private void startCraft(float delta) {
        MachineRecipe recipe = findRecipe();
        if (recipe == null || !recipe.fits(outputs())) {
            return;
        }
        if (payForWork(recipe, delta) <= 0.0f) {
            return;
        }
        craft = recipe;
        craftTotal = craftTime(recipe);
        craftSeconds = 0.0f;
        energyDebt = 0.0f;
        recipe.consume(inputs());
    }

    /**
     * Seconds one craft of a recipe takes in this machine.
     * <p>
     * The time a recipe names is what a machine of the first age takes for it. <b>A machine that works faster
     * overrides this and not the loop around it:</b> a machine of pressure runs the very recipes of its bronze
     * twin in half the time, which is what makes it spend twice the steam a tick and the same steam on the
     * craft, see {@code MachinePressure}.
     *
     * @param recipe recipe that is about to run
     * @return the seconds a craft of it takes
     */
    protected float craftTime(MachineRecipe recipe) {
        return recipe.seconds();
    }

    /** Forgets the craft that runs, which is what an interruption and a finished work both do. */
    private void forgetCraft() {
        craft = null;
        craftSeconds = 0.0f;
        energyDebt = 0.0f;
    }

    /**
     * The first recipe of the types this machine reads that recognises its input.
     *
     * @return the recipe, or {@code null} when nothing fits
     */
    protected MachineRecipe findRecipe() {
        for (RecipeType type : recipeTypes()) {
            for (Recipe recipe : RecipeRegistry.recipes(type)) {
                if (recipe instanceof MachineRecipe machineRecipe
                        && machineRecipe.matches(inputs())) {
                    return machineRecipe;
                }
            }
        }
        return null;
    }

    /** Seconds this machine has worked on its current craft. */
    protected float craftSeconds() {
        return craftSeconds;
    }

    /**
     * Pays for the share of a craft that one frame is worth.
     * <p>
     * The machine of this class pays with the energy of its buffer, see {@link EnergyStorage}. What one
     * frame costs is the share of the whole craft it is worth: a recipe that costs 200 units over ten
     * seconds takes 20 units a second, and the fraction that does not make a whole unit is kept for the
     * frames after it.
     * <p>
     * <b>What comes back is the part of that frame the machine could really pay</b>, between nothing and the
     * whole of it: a machine that could pay half of its share advances its craft by half a frame and waits
     * for the next one, see {@link #update(float)}. What could not be paid is dropped rather than carried
     * over, so a craft never costs more than the recipe asked for however often the power comes and goes.
     * <p>
     * <b>A machine that runs on something else overrides this and not the loop around it.</b> A steam
     * machine pays with the steam of one of its tanks, see {@link MachineTank}, while the search for a
     * recipe, the check that the products fit and the counting of the time stay in one place.
     *
     * @param recipe recipe that runs
     * @param delta time since the last frame in seconds
     * @return the share of the frame that was paid for, {@code 0} when the machine could pay nothing
     */
    protected float payForWork(MachineRecipe recipe, float delta) {
        int cost = recipe.energy();
        if (cost <= 0) {
            // A recipe that costs nothing is a frame that is always paid for.
            return 1.0f;
        }
        float seconds = Math.max(recipe.seconds(), delta);
        energyDebt += cost * delta / seconds;
        int whole = (int) energyDebt;
        if (whole <= 0) {
            // Not a whole unit yet: this frame is free, the next ones pay for it.
            return 1.0f;
        }
        int paid = energy().extract(whole, false);
        if (paid <= 0) {
            // Nothing at all was paid, so the frame counts for nothing: the machine waits and the next
            // frame asks for its own share again instead of piling up a debt that could never be paid.
            energyDebt = 0.0f;
            return 0.0f;
        }
        if (paid < whole) {
            // Part of the frame was paid: the work advances by that part and the rest of the frame is
            // dropped rather than carried over, so a craft never costs more than the recipe asked for.
            energyDebt = 0.0f;
            return (float) paid / whole;
        }
        energyDebt -= paid;
        return 1.0f;
    }

    @Override
    protected void saveState(NbtCompound state) {
        state.putFloat(SaveTags.CRAFT_SECONDS, craftSeconds);
        state.putFloat(SaveTags.CRAFT_TOTAL, craftTotal);
        state.putFloat(SaveTags.ENERGY_DEBT, energyDebt);
        state.putString(SaveTags.CRAFT_RECIPE, craft == null ? "" : craft.name());
    }

    @Override
    protected void loadState(NbtCompound state) {
        craftSeconds = state.getFloat(SaveTags.CRAFT_SECONDS, 0.0f);
        float total = state.getFloat(SaveTags.CRAFT_TOTAL, 1.0f);
        craftTotal = total > 0.0f ? total : 1.0f;
        energyDebt = state.getFloat(SaveTags.ENERGY_DEBT, 0.0f);
        craft = restoredCraft(state.getString(SaveTags.CRAFT_RECIPE, ""));
    }

    /**
     * The recipe a stored machine was working on.
     * <p>
     * The name is looked up again, because a recipe is not part of a save game: a name that no recipe of the
     * types this machine reads answers to leaves the machine idle, so a recipe that was renamed or removed
     * costs the input that was swallowed for it and nothing else.
     *
     * @param name name of the recipe that was stored, empty for a machine that was idle
     * @return the recipe, or {@code null} when the machine has to start over
     */
    private MachineRecipe restoredCraft(String name) {
        Recipe recipe = RecipeRegistry.byName(name);
        if (!(recipe instanceof MachineRecipe machine) || !recipeTypes().contains(machine.type())) {
            return unknownCraft(name);
        }
        return machine;
    }

    /**
     * The recipe a stored machine was working on that no file of the game answers to, or {@code null}.
     * <p>
     * <b>A recipe may live in the machine and not in a file.</b> A reactor that reads the molecules of its
     * own vessel infers a reaction nobody wrote down, and what it swallowed to begin is gone from the slots
     * and the tanks the moment the work starts: a machine that came back from a save would hold the products
     * of a reaction it can no longer remember, which is matter that arrived out of nothing. A machine that
     * runs recipes of its own therefore writes the one it is working on into its own save state and answers
     * here with it, see {@code ChemicalReactorMachine}.
     *
     * @param name name of the recipe that was stored
     * @return the recipe, or {@code null} when the machine has to start over
     */
    protected MachineRecipe unknownCraft(String name) {
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + name() + ", craft "
                + Math.round(craftProgress() * 100.0f) + "%, energy " + energy() + ")";
    }
}
