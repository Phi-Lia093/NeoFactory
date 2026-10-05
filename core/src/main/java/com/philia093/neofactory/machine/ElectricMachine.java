package com.philia093.neofactory.machine;

import com.philia093.neofactory.cable.Voltage;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.recipe.EnergyRecipe;
import com.philia093.neofactory.recipe.RecipeType;
import com.philia093.neofactory.world.TickClock;

import java.util.List;
import java.util.Objects;

/**
 * A machine of the electric age: one that pays for its work with the power of a line of cables.
 * <p>
 * <b>A recipe of this age says two things a recipe of the age of steam does not: how much power it draws a
 * tick and the voltage it asks for.</b> From the two, and from the tier of the machine that runs it, the
 * current follows - a line of a given voltage moves that voltage in units a tick for every ampere it is
 * built to carry - so a recipe that draws more than a line of its tier may deliver has to be spread over
 * several amperes, and one that draws more than the machine may take is no recipe for that machine at all.
 * That is the whole of what this class adds to {@link RecipeMachine}, and it is why a machine of this class
 * is built out of four numbers: its screen, its slots, the tier it is built for and the current it may take.
 * <p>
 * <b>A machine takes one ampere while it waits and the amperes of its work while it runs.</b> An idle machine
 * tops its buffer up to the brim at an ampere a tick, which is what makes it ready the moment a recipe is put
 * into it; a working machine asks for the current its recipe draws, see {@link #requestAmps()} and
 * {@code MachineBlockEntity#updateEnergy}. A line does not hand anything over by itself - a cable carries
 * nothing of its own - so the machine is the one that reaches for the power, and the block entity is what
 * carries the question to the line of the plug it takes power in through.
 * <p>
 * <b>A machine of a later tier over-clocks the recipes of an earlier one.</b> Every tier is four times the
 * one below it, so a recipe that draws fifteen units a tick can be run at sixty on a line four times as
 * strong: the same work in half the time, because a step of over-clock halves the time and quadruples the
 * power. What the whole craft costs therefore doubles with every step, see {@link #overclockSteps()} - a
 * convenience a player pays for - and it is taken as far as the current of the machine allows and no further.
 * <p>
 * <b>A machine that works is never a source of its line.</b> Its buffer may be filled and may not be emptied
 * by a line, which is what makes a line of machines that work a tree and never a ring: the machine spends
 * through {@link MachineEnergyStorage#spend}, the door that is the mirror of the one a generator fills its
 * own buffer through, see {@link MachineEnergyStorage} and {@link MachineEnergyStorage#make}.
 * <p>
 * A machine of the game therefore names its screen, its slots, its tier and its amperes and nothing else; the
 * arithmetic of the power, of the over-clock and of the frame that could not be paid for all lives here.
 */
public class ElectricMachine extends RecipeMachine {

    /**
     * Ticks of work the buffer of a machine holds.
     * <p>
     * Sixty four ticks is a little over three seconds of work at full speed, which is what a machine needs to
     * ride out the gap between two amperes of a line that is busy with a workshop of machines: enough to keep
     * working while the line turns to the next machine, and short enough that a workshop is still a place of
     * lines and not a hall of batteries.
     */
    public static final int BUFFER_TICKS = 64;

    /** Current a machine of the usual kind takes at most, in amperes. */
    public static final int STANDARD_AMPS = 2;

    private final Voltage tier;

    private final int maxAmps;

    private final MachineEnergyStorage buffer;

    /**
     * Creates a machine of the electric age.
     * <p>
     * <b>The machine adds the shelf it is fed with to the slots of its family.</b> A machine of the line takes
     * power in over a line of cables, and the cell of energy at the foot of its panel is a slot of its own so
     * that a player may feed it by hand instead, see {@link Reagents} and {@link MachineInventory#withRole}.
     *
     * @param screen how this machine is shown, see {@link MachineScreen}
     * @param inventory inventory whose slots carry the roles of the machine
     * @param tier tier this machine was built for, which is what it may run and what it may take
     * @param maxAmps largest current this machine takes, in amperes
     * @param recipeTypes types of recipe the machine reads
     */
    protected ElectricMachine(MachineScreen screen, MachineInventory inventory, Voltage tier,
            int maxAmps, List<RecipeType> recipeTypes) {
        this(screen, inventory, tier, maxAmps, recipeTypes, new MachineTank[0]);
    }

    /**
     * Creates a machine of the electric age that also holds tanks of fluid.
     * <p>
     * The tanks are what a machine of the chemistry is fed with as well as its slots: a route of the
     * industry names substances in millibuckets, so what is a gas or a liquid arrives in a tank and what
     * is a solid arrives in a slot, see {@link com.philia093.neofactory.recipe.ChemicalRecipe}. The screen
     * of the machine draws one tank per one of them at the foot of its panel.
     *
     * @param screen how this machine is shown, which decides how many tanks the panel draws
     * @param inventory inventory whose slots carry the roles of the machine
     * @param tier tier this machine was built for
     * @param maxAmps largest current this machine takes, in amperes
     * @param recipeTypes types of recipe the machine reads
     * @param tanks tanks of fluid the machine holds, in the order the panel draws them
     */
    protected ElectricMachine(MachineScreen screen, MachineInventory inventory, Voltage tier,
            int maxAmps, List<RecipeType> recipeTypes, MachineTank... tanks) {
        this(screen, inventory.withRole(MachineInventory.Role.ENERGY), recipeTypes, tier,
                Math.max(1, maxAmps), bufferOf(tier, maxAmps), tanks);
    }

    /** Creates a machine whose buffer was built before this constructor, so that the field is set once. */
    private ElectricMachine(MachineScreen screen, MachineInventory inventory, List<RecipeType> recipeTypes,
            Voltage tier, int maxAmps, MachineEnergyStorage buffer, MachineTank... tanks) {
        super(screen, inventory, buffer, recipeTypes, tanks);
        this.tier = Objects.requireNonNull(tier, "tier");
        this.maxAmps = maxAmps;
        this.buffer = buffer;
    }

    /**
     * The buffer of a machine of a tier: sixty four ticks of work and a mouth as wide as its amperes.
     * <p>
     * <b>The limit of the buffer is the limit of a line and not of the machine.</b> What one call may add is
     * what the machine takes at most, so no line - however wide - may push more into a machine than the tier
     * it was built for allows; what one call may take is nothing at all, which is what keeps the machine out
     * of the sources of its own line. The capacity is how long the machine may run between two amperes.
     *
     * @param tier tier the machine was built for
     * @param maxAmps largest current the machine takes
     * @return the buffer
     */
    private static MachineEnergyStorage bufferOf(Voltage tier, int maxAmps) {
        Objects.requireNonNull(tier, "tier");
        int amperes = Math.max(1, maxAmps);
        return new MachineEnergyStorage(MachineEnergyStorage.capacityOf(tier), amperes * tier.euPerTick(), 0,
                tier);
    }

    /** Tier this machine was built for, which is the best line that may feed it. */
    public Voltage tier() {
        return tier;
    }

    /** Largest current this machine takes, in amperes. */
    public int maxAmps() {
        return maxAmps;
    }

    /** Buffer of this machine, the one a line fills and the machine spends. */
    public MachineEnergyStorage buffer() {
        return buffer;
    }

    /** Energy the buffer of this machine holds, {@value #BUFFER_TICKS} ticks of the tier it was built for. */
    public int bufferCapacity() {
        return buffer.capacity();
    }

    /** Index of the shelf this machine is fed by hand with, in the inventory of the machine. */
    public int reagentSlot() {
        return inventory().slotOf(MachineInventory.Role.ENERGY);
    }

    /**
     * A side of a machine of the line that carries a job stands on the casing of its own tier.
     * <p>
     * The model of a machine of the line draws the art of its family on five of its six sides, see
     * {@link MachineFamilies.Family#pictureOf(Voltage, String)}; the side a player gave a job to - the plug
     * the line of cables stands at - is drawn by the block entity of the machine, which writes the casing of
     * the machine under the overlay of that job. <b>That casing is the one of the tier and never the bronze
     * of the age of steam</b> a machine that says nothing falls back to, see {@link Machine.CASING} and
     * {@link MachineCasing}: a machine of the high voltage is a machine of the grey casing of the high
     * voltage, whatever side of it a player works.
     */
    @Override
    public String casing() {
        return MachineCasing.pictureOf(tier);
    }

    /**
     * A machine of the line keeps everything but the reagent it burns out of the shelf it is fed with.
     * <p>
     * <b>Nothing but dust goes into the cell of energy.</b> A machine that swallowed whatever a player dropped
     * on it would be a machine filled with junk by accident, and the one thing a player puts there is the
     * piece of redstone the machine burns a frame at a time, see {@link Reagents}. Every other slot of the
     * machine - the slots its family declares - takes what it always took.
     *
     * @param slot index of the slot inside {@link #inventory()}
     * @param stack stack that would go into it, never empty
     * @return {@code true} when the machine takes it
     */
    @Override
    public boolean acceptsItem(int slot, ItemStack stack) {
        return slot != reagentSlot() || Reagents.isReagent(stack);
    }

    /**
     * Burns one piece of the reagent that lies in the shelf of the machine, if the whole of one fits.
     * <p>
     * <b>A machine of the line is fed twice and this is the second way.</b> A player who has no line yet puts
     * redstone dust into the cell of energy at the foot of the panel, and every frame the machine takes one
     * piece of it and fills its own buffer with what it is worth - the same door a generator fills its buffer
     * through, and not the one a line meets, see {@link MachineEnergyStorage#make(int)}.
     * <p>
     * <b>Nothing is burned that would be lost.</b> A piece that does not fit into the room left in the buffer
     * whole is left in the shelf until the machine has spent enough of what it holds, so a player never pays
     * for five hundred units of a piece that is worth eight hundred. A shelf that holds no reagent - which is
     * every machine that was never filled by hand - burns nothing at all.
     */
    private void burnReagent() {
        int slot = reagentSlot();
        if (slot < 0) {
            return;
        }
        ItemStack reagent = inventory().get(slot);
        int worth = Reagents.energyOf(reagent);
        if (worth <= 0 || buffer.capacity() - buffer.amount() < worth) {
            return;
        }
        buffer.make(worth);
        if (reagent.count() <= 1) {
            inventory().set(slot, ItemStack.EMPTY);
        } else {
            reagent.setCount(reagent.count() - 1);
        }
    }

    /**
     * Advances the machine by one frame, burning the reagent in its shelf before the frame is worked on.
     * <p>
     * The dust of a machine that is fed by hand is part of the frame it pays for: what it is worth lies in the
     * buffer before the recipe of that frame is asked to pay for itself - and before the block that carries the
     * machine reaches for the line of its plug, so a machine that is fed by hand takes from the line only what
     * the dust did not cover, see {@code MachineBlockEntity#updateEnergy}.
     *
     * @param delta time since the last frame in seconds, always positive
     */
    @Override
    protected void update(float delta) {
        burnReagent();
        super.update(delta);
    }

    /**
     * Energy this machine wants from a line this tick, which is what the block asks the line for.
     * <p>
     * One ampere of the tier of the machine while it waits, and the amperes of the work it does while it
     * runs, see {@link #requestAmps()}. The amount is a question and not a promise: the line hands over what
     * it can and the block keeps the room left in the buffer in mind.
     */
    @Override
    public int requestEu() {
        return requestAmps() * tier.euPerTick();
    }

    /**
     * Current this machine asks a line for, in amperes.
     * <p>
     * <b>One ampere while it waits:</b> an idle machine fills its buffer and asks no more, which is what lets
     * a workshop of machines share a line without watching it. <b>The amperes of its work while it runs:</b>
     * a recipe that draws power asks for as many amperes as that power needs on the tier of the machine,
     * rounded up and never below one, and never above {@link #maxAmps()} - the two limits together are what
     * decides whether a recipe is one for this machine at all, see {@link #canRun(MachineRecipe)}.
     *
     * @return the amperes, at least one
     */
    public int requestAmps() {
        return isRunning() ? clampedAmps(power()) : 1;
    }

    /** Power the work of this machine draws a tick, in units, {@code 0} while it is idle. */
    public int power() {
        MachineRecipe recipe = currentCraft();
        return recipe == null ? 0 : powerFor(recipe);
    }

    /** Steps of over-clock applied to the work of this machine, {@code 0} while it is idle. */
    public int overclockSteps() {
        MachineRecipe recipe = currentCraft();
        return recipe == null ? 0 : overclockFor(recipe);
    }

    /**
     * {@code true} when this machine may run a recipe at all.
     * <p>
     * Two questions are asked and both have to be answered with a yes: the recipe has to ask for a voltage
     * this machine was built for or a worse one - a recipe of a later age is the business of a later machine
     * - and the current it draws has to fit into {@link #maxAmps()}, for the over-clock is a gift of the
     * voltage and never of the current. A recipe that fails either is no recipe for this machine: it is never
     * started and the input of it stays in the slot, see {@link #findRecipe()}.
     *
     * @param recipe recipe that was recognised
     * @return {@code true} when this machine may work on it
     */
    public boolean canRun(MachineRecipe recipe) {
        EnergyRecipe energy = energyOf(recipe);
        if (energy == null) {
            return false;
        }
        Voltage asks = Voltage.byEuPerTick(energy.voltage());
        if (asks == null || !tier.isAtLeast(asks)) {
            return false;
        }
        int base = baseOf(recipe);
        return base <= 0 || ampsWanted(base) <= maxAmps;
    }

    /**
     * The first recipe the input is and that this machine may run.
     * <p>
     * {@link RecipeMachine} finds the recipe the input asks for; a recipe of a later age, or one whose work
     * draws more current than this machine takes, is dropped here and the machine waits for one it can pay
     * for, see {@link #canRun(MachineRecipe)}.
     */
    @Override
    protected MachineRecipe findRecipe() {
        MachineRecipe recipe = recognisedRecipe();
        return recipe != null && canRun(recipe) ? recipe : null;
    }

    /**
     * The first recipe the input answers, whether or not this machine may run it.
     * <p>
     * The question "is the input right" and the question "is this a recipe for this machine" stay in one
     * place: {@link RecipeMachine} looks through the recipes of the types a machine reads, a machine that
     * carries its own recipes - a test, for instance - overrides this, and {@link #canRun(MachineRecipe)} is
     * what separates the two.
     *
     * @return the recipe, or {@code null} when the input is nothing this machine knows
     */
    protected MachineRecipe recognisedRecipe() {
        return super.findRecipe();
    }

    /**
     * Seconds one craft takes in this machine, the time of the recipe divided by the over-clock.
     * <p>
     * A step of over-clock halves the time and leaves the power at four times the one below it, which is what
     * makes a craft of a later machine cost twice what the same craft costs on the tier of its recipe, see
     * {@link #overclockSteps()}.
     */
    @Override
    protected float craftTime(MachineRecipe recipe) {
        return recipe.seconds() / (float) (1 << overclockFor(recipe));
    }

    /**
     * Spends the power of a frame out of the buffer, the way a steam machine spends its steam.
     * <p>
     * <b>The number a recipe draws is what it draws every tick</b>, so one frame spends that much and the
     * fraction below a whole unit waits for the frames after it. <b>A frame that cannot be paid for in full
     * counts for the part that was paid</b> - a machine on a line that is turning to its neighbour works
     * slower instead of standing still - and a frame that could not be paid for at all leaves the work and the
     * input where they are, see {@link RecipeMachine#payForWork}.
     *
     * @param recipe recipe that runs
     * @param delta time since the last frame in seconds
     * @return the share of the frame that was paid for, {@code 0} when the buffer could give nothing
     */
    @Override
    protected float payForWork(MachineRecipe recipe, float delta) {
        int perTick = powerFor(recipe);
        if (perTick <= 0) {
            // A recipe that draws no power is a frame that is always paid for.
            return 1.0f;
        }
        energyDebt += perTick * delta * TickClock.TICKS_PER_SECOND;
        int whole = (int) energyDebt;
        if (whole <= 0) {
            // Not a whole unit yet: this frame is free, the next ones pay for it.
            return 1.0f;
        }
        int paid = buffer.spend(whole);
        if (paid <= 0) {
            // Nothing at all was paid, so the frame counts for nothing: the machine waits with its work and
            // the next frame asks for its own share again.
            energyDebt = 0.0f;
            return 0.0f;
        }
        if (paid < whole) {
            // Part of the frame was paid: the work advances by that part and the rest is dropped rather than
            // carried over, so a craft never costs more than it draws.
            energyDebt = 0.0f;
            return (float) paid / whole;
        }
        energyDebt -= paid;
        return 1.0f;
    }

    /** The energy a recipe draws in this machine, over-clock applied. */
    private int powerFor(MachineRecipe recipe) {
        return overclocked(baseOf(recipe), overclockFor(recipe));
    }

    /**
     * Steps of over-clock this machine applies to a recipe: as many as the tier allows and the current takes.
     * <p>
     * One step for every tier the machine stands above the recipe, until the current the recipe would draw
     * exceeds {@link #maxAmps()}: a step that does not fit is one step too far, so the machine walks back
     * until the work is one it may pay for. A recipe of a later tier than the machine is no recipe for it and
     * the answer is no step at all.
     */
    private int overclockFor(MachineRecipe recipe) {
        EnergyRecipe energy = energyOf(recipe);
        Voltage asks = energy == null ? null : Voltage.byEuPerTick(energy.voltage());
        if (asks == null || !tier.isAtLeast(asks)) {
            return 0;
        }
        int steps = tier.ordinal() - asks.ordinal();
        while (steps > 0 && !ampsFit(baseOf(recipe), steps)) {
            steps--;
        }
        return steps;
    }

    /** {@code true} when the work of a recipe under that many steps of over-clock fits the amperes. */
    private boolean ampsFit(int base, int steps) {
        return ampsWanted(overclocked(base, steps)) <= maxAmps;
    }

    /**
     * Energy of a step of over-clock: four times the power, which is what halves the time.
     * <p>
     * The count is pushed out of the reach of a whole number of the game before it is handed back: a number
     * that would not fit is one no machine may pay for anyway, and it is the question "does this fit the
     * amperes" that is asked of it.
     */
    private int overclocked(int base, int steps) {
        return (int) Math.min(Integer.MAX_VALUE, (long) base << (2 * steps));
    }

    /**
     * Amperes the power of a frame needs on this tier, at least one and never capped.
     * <p>
     * <b>Twice the power and not the power itself.</b> The margin is what keeps a machine of the usual kind
     * from living on the very edge of its line: a recipe that draws close to the voltage of a machine asks
     * for two amperes although one would carry it, so a workshop of machines leaves room for the one that
     * starts next and a player who fills a line to the brim finds it out.
     */
    private int ampsWanted(int power) {
        if (power <= 0) {
            return 1;
        }
        return (int) Math.floor(power * 2.0 / tier.euPerTick()) + 1;
    }

    /** Amperes a line is asked for, always between one and what this machine may take. */
    private int clampedAmps(int power) {
        return Math.max(1, Math.min(ampsWanted(power), maxAmps));
    }

    /** The energy of a recipe, or {@code null} for a recipe of an age that draws no power. */
    private static EnergyRecipe energyOf(MachineRecipe recipe) {
        return recipe instanceof EnergyRecipe energy ? energy : null;
    }

    /** Energy a recipe draws a tick at its own tier, {@code 0} for a recipe that names no energy. */
    private static int baseOf(MachineRecipe recipe) {
        EnergyRecipe energy = energyOf(recipe);
        return energy == null ? 0 : Math.max(0, energy.euPerTick());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + name() + ", " + tier + ", power " + power() + "EU/t, craft "
                + Math.round(craftProgress() * 100.0f) + "%, " + buffer + ")";
    }
}
