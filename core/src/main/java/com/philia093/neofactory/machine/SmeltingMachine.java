package com.philia093.neofactory.machine;

import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.recipe.Recipe;
import com.philia093.neofactory.recipe.RecipeRegistry;
import com.philia093.neofactory.recipe.RecipeType;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.save.SaveTags;

import java.util.List;

/**
 * A machine that smelts what it is given, the furnace of the game.
 * <p>
 * The machine holds three slots - {@link #INPUT}, {@link #FUEL} and {@link #OUTPUT} -
 * and works without knowing a single recipe: it asks
 * {@link RecipeRegistry#recipes(RecipeType)} for the smelting recipes and lets them
 * decide whether the input is theirs. How long one craft takes and what it makes comes
 * from the recipe file, so a new smelting recipe needs no code at all.
 * <p>
 * While a recipe fits, the machine burns fuel. The progress grows while a flame lasts
 * and falls back when the flame goes out, which is what makes a furnace stop and start
 * with the fuel rather than losing what it had done. Nothing ticks the machine in the
 * world yet, see {@link Machine}.
 */
public final class SmeltingMachine extends Machine implements ProgressMachine, FuelMachine {

    /** Slot that holds what is smelted. */
    public static final int INPUT = 0;

    /** Slot that holds what keeps the furnace burning. */
    public static final int FUEL = 1;

    /** Slot the result appears in. */
    public static final int OUTPUT = 2;

    /** How much faster the progress falls back than it grows. */
    private static final float FALLBACK_FACTOR = 2.0f;

    private float craftSeconds;
    private float craftTotal = 1.0f;
    private float burnSeconds;
    private float burnTotal;

    /**
     * Screen of the furnace: the ore and the fuel in one column on the left and the product
     * on the right of the progress bar, which is the plain pair of arrows.
     */
    public static final MachineScreen SCREEN = new MachineScreen("Furnace", ProgressKind.GENERIC,
            List.of(SlotKind.SMELTING, SlotKind.SMELTING), List.of(SlotKind.GENERIC), 0, 0, false);

    /** Creates an empty furnace. */
    public SmeltingMachine() {
        super(SCREEN, new MachineInventory(MachineInventory.Role.INPUT,
                MachineInventory.Role.FUEL, MachineInventory.Role.OUTPUT),
                new SimpleEnergyStorage(0), List.of(RecipeType.SMELTING));
    }

    @Override
    public float craftProgress() {
        return craftTotal <= 0.0f ? 0.0f : Math.min(1.0f, craftSeconds / craftTotal);
    }

    @Override
    public float burnProgress() {
        return burnTotal <= 0.0f ? 0.0f : Math.min(1.0f, burnSeconds / burnTotal);
    }

    /** {@code true} while a flame is burning, the picture a screen draws the fire with. */
    public boolean isBurning() {
        return burnSeconds > 0.0f;
    }

    @Override
    public float fuelSeconds() {
        return burnSeconds;
    }

    /** {@code true} while the furnace is burning, it works as long as fuel lasts. */
    @Override
    public boolean isRunning() {
        return isBurning();
    }

    @Override
    protected void update(float delta) {
        MachineRecipe recipe = findRecipe();
        if (recipe == null || !recipe.fits(outputs())) {
            coolDown(delta);
            return;
        }
        craftTotal = recipe.seconds();
        if (burnSeconds <= 0.0f && !consumeFuel()) {
            // Without fuel nothing burns, the work done so far is kept for a while.
            coolDown(delta);
            return;
        }
        // Only the time a flame covers counts as work, so a furnace that runs out of
        // fuel stops right where the flame ended.
        float burning = Math.min(delta, burnSeconds);
        burn(delta);
        craftSeconds += burning;
        if (craftSeconds >= craftTotal) {
            craftSeconds = 0.0f;
            recipe.consume(inputs());
            recipe.produce(outputs());
        }
    }

    /** The first smelting recipe that recognises the input slot. */
    private MachineRecipe findRecipe() {
        for (Recipe recipe : RecipeRegistry.recipes(RecipeType.SMELTING)) {
            if (recipe instanceof MachineRecipe machineRecipe && machineRecipe.matches(inputs())) {
                return machineRecipe;
            }
        }
        return null;
    }

    /**
     * Takes one item out of the fuel slot and lights it.
     *
     * @return {@code true} when something started to burn
     */
    private boolean consumeFuel() {
        ItemStack fuel = inventory().get(FUEL);
        if (fuel.isEmpty()) {
            // A slot whose stack ran out still names its item, so it has to be tested
            // for being empty before it is asked what it holds.
            return false;
        }
        float seconds = Fuels.secondsOf(fuel.item());
        if (seconds <= 0.0f) {
            return false;
        }
        fuel.setCount(fuel.count() - 1);
        burnTotal = seconds;
        burnSeconds = seconds;
        return true;
    }

    /** Lets the current flame burn down by one frame. */
    private void burn(float delta) {
        burnSeconds = Math.max(0.0f, burnSeconds - delta);
    }

    /** Forgets a little of the work that was done. */
    private void coolDown(float delta) {
        craftSeconds = Math.max(0.0f, craftSeconds - delta * FALLBACK_FACTOR);
    }

    @Override
    protected void saveState(NbtCompound state) {
        state.putFloat(SaveTags.CRAFT_SECONDS, craftSeconds);
        state.putFloat(SaveTags.CRAFT_TOTAL, craftTotal);
        state.putFloat(SaveTags.BURN_SECONDS, burnSeconds);
        state.putFloat(SaveTags.BURN_TOTAL, burnTotal);
    }

    @Override
    protected void loadState(NbtCompound state) {
        craftSeconds = state.getFloat(SaveTags.CRAFT_SECONDS, 0.0f);
        float total = state.getFloat(SaveTags.CRAFT_TOTAL, 1.0f);
        craftTotal = total > 0.0f ? total : 1.0f;
        burnSeconds = state.getFloat(SaveTags.BURN_SECONDS, 0.0f);
        burnTotal = state.getFloat(SaveTags.BURN_TOTAL, 0.0f);
    }

    @Override
    public String toString() {
        return "SmeltingMachine(craft " + Math.round(craftProgress() * 100.0f) + "%, fuel "
                + Math.round(burnProgress() * 100.0f) + "%)";
    }
}
