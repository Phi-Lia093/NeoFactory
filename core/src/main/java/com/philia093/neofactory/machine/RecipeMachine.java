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

    /** How much faster the progress falls back than it grows. */
    private static final float FALLBACK_FACTOR = 2.0f;

    private float craftSeconds;
    private float craftTotal = 1.0f;

    /** Energy that is owed but was not paid yet, always below one unit. */
    private float energyDebt;

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
     * {@ink MachineError#NO_POWER} while the machine has work but no energy to do it with.
     * <p>
     * The icon of the machine screen reports it, which is what tells a player why nothing
     * moves although the input is right.
     */
    @Override
    public MachineError error() {
        if (energy().amount() <= 0 && findRecipe() != null) {
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

    @Override
    public boolean isRunning() {
        return craftSeconds > 0.0f;
    }

    @Override
    protected void update(float delta) {
        MachineRecipe recipe = findRecipe();
        if (recipe == null || !recipe.fits(outputs())) {
            coolDown(delta);
            return;
        }
        craftTotal = recipe.seconds();
        if (!payForWork(recipe, delta)) {
            coolDown(delta);
            return;
        }
        craftSeconds += delta;
        if (craftSeconds >= craftTotal) {
            craftSeconds = 0.0f;
            recipe.consume(inputs());
            recipe.produce(outputs());
        }
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

    /** Forgets a little of the work that was done. */
    protected void coolDown(float delta) {
        craftSeconds = Math.max(0.0f, craftSeconds - delta * FALLBACK_FACTOR);
    }

    /**
     * Pays for the share of a craft that one frame is worth.
     * <p>
     * The machine of this class pays with the energy of its buffer, see {@link EnergyStorage}. What one
     * frame costs is the share of the whole craft it is worth: a recipe that costs 200 units over ten
     * seconds takes 20 units a second, and the fraction that does not make a whole unit is kept for the
     * frames after it. A machine without enough energy waits - so the work already done is kept, the same
     * way a furnace keeps its fire - and it never pays more than the recipe asked for.
     * <p>
     * <b>A machine that runs on something else overrides this and not the loop around it.</b> A steam
     * machine pays with the steam of one of its tanks, see {@link MachineTank}, while the search for a
     * recipe, the check that the products fit and the counting of the time stay in one place.
     *
     * @param recipe recipe that runs
     * @param delta time since the last frame in seconds
     * @return {@code true} when the frame was paid for
     */
    protected boolean payForWork(MachineRecipe recipe, float delta) {
        int cost = recipe.energy();
        if (cost <= 0) {
            return true;
        }
        float seconds = Math.max(recipe.seconds(), delta);
        energyDebt += cost * delta / seconds;
        int whole = (int) energyDebt;
        if (whole <= 0) {
            // Not a whole unit yet: this frame is free, the next ones pay for it.
            return true;
        }
        int paid = energy().extract(whole, false);
        if (paid < whole) {
            // The machine waits, so this frame counts for nothing and the debt is
            // dropped: the next frame asks for its own share again instead of piling up
            // a debt that could never be paid.
            energyDebt = 0.0f;
            return false;
        }
        energyDebt -= paid;
        return true;
    }

    @Override
    protected void saveState(NbtCompound state) {
        state.putFloat(SaveTags.CRAFT_SECONDS, craftSeconds);
        state.putFloat(SaveTags.CRAFT_TOTAL, craftTotal);
        state.putFloat(SaveTags.ENERGY_DEBT, energyDebt);
    }

    @Override
    protected void loadState(NbtCompound state) {
        craftSeconds = state.getFloat(SaveTags.CRAFT_SECONDS, 0.0f);
        float total = state.getFloat(SaveTags.CRAFT_TOTAL, 1.0f);
        craftTotal = total > 0.0f ? total : 1.0f;
        energyDebt = state.getFloat(SaveTags.ENERGY_DEBT, 0.0f);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + name() + ", craft "
                + Math.round(craftProgress() * 100.0f) + "%, energy " + energy() + ")";
    }
}
