package com.philia093.neofactory.machine;

import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.recipe.InventoryGrid;
import com.philia093.neofactory.recipe.Recipe;
import com.philia093.neofactory.recipe.RecipeRegistry;
import com.philia093.neofactory.recipe.RecipeType;
import com.philia093.neofactory.recipe.SmeltingRecipe;

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
public final class SmeltingMachine extends Machine implements ProgressMachine {

    /** Slot that holds what is smelted. */
    public static final int INPUT = 0;

    /** Slot that holds what keeps the furnace burning. */
    public static final int FUEL = 1;

    /** Slot the result appears in. */
    public static final int OUTPUT = 2;

    /** How much faster the progress falls back than it grows. */
    private static final float FALLBACK_FACTOR = 2.0f;

    private final InventoryGrid inputGrid;

    private float craftSeconds;
    private float craftTotal = 1.0f;
    private float burnSeconds;
    private float burnTotal;

    /** Creates an empty furnace. */
    public SmeltingMachine() {
        super(new MachineInventory(MachineInventory.Role.INPUT, MachineInventory.Role.FUEL,
                MachineInventory.Role.OUTPUT), new SimpleEnergyStorage(0));
        this.inputGrid = new InventoryGrid(inventory(), INPUT, 1, 1);
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

    /** {@code true} while the furnace is burning, it works as long as fuel lasts. */
    @Override
    public boolean isRunning() {
        return isBurning();
    }

    @Override
    protected void update(float delta) {
        SmeltingRecipe recipe = findRecipe();
        if (recipe == null || !canStore(recipe.result())) {
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
            recipe.consume(inputGrid);
            store(recipe.result());
        }
    }

    /** The first smelting recipe that accepts the input slot. */
    private SmeltingRecipe findRecipe() {
        for (Recipe recipe : RecipeRegistry.recipes(RecipeType.SMELTING)) {
            if (recipe instanceof SmeltingRecipe smelting && smelting.matches(inputGrid)) {
                return smelting;
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

    /** {@code true} when the result fits into the output slot. */
    private boolean canStore(ItemStack result) {
        ItemStack output = inventory().get(OUTPUT);
        if (output.isEmpty()) {
            return true;
        }
        return output.isStackableWith(result) && output.room() >= result.count();
    }

    /** Puts a result into the output slot. */
    private void store(ItemStack result) {
        ItemStack output = inventory().get(OUTPUT);
        if (output.isEmpty()) {
            inventory().set(OUTPUT, result);
            return;
        }
        output.grow(result.count());
    }

    @Override
    public String toString() {
        return "SmeltingMachine(craft " + Math.round(craftProgress() * 100.0f) + "%, fuel "
                + Math.round(burnProgress() * 100.0f) + "%)";
    }
}
