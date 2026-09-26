package com.philia093.neofactory.machine;

import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.fluid.FluidStorage;
import com.philia093.neofactory.fluid.Fluids;
import com.philia093.neofactory.item.ItemDrops;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.recipe.RecipeType;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.util.nbt.NbtList;
import com.philia093.neofactory.world.save.SaveTags;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Base class of every machine of the game.
 * <p>
 * A machine owns the four things every machine is built from and nothing else:
 * <ul>
 *     <li>an inventory whose slots carry a {@link MachineInventory.Role role} - the
 *         input, the fuel, the output and the upgrade slots</li>
 *     <li>an {@link EnergyStorage} the machine takes its energy from</li>
 *     <li>the {@link MachineTank tanks} of fluid it holds, each with a role of its own</li>
 *     <li>the {@link RecipeType types of recipe} it reads</li>
 * </ul>
 * The roles are what turns that into something a recipe can work with: {@link #inputs()}
 * and {@link #outputs()} collect the slots and the tanks of both sides, so a recipe
 * never has to know how a machine is laid out. It takes what the machine offers and
 * hands its products back where the machine decides, see {@link MachineRecipe}.
 * <p>
 * What the machine does with all of it is decided by {@link #update(float)}, so a
 * furnace, a mill and a reactor only differ in that one method - or not even in it, when
 * they extend {@link RecipeMachine}, which holds the loop they all share.
 * <p>
 * A machine travels with its world: {@link #save(NbtCompound)} and
 * {@link #load(NbtCompound)} write and read the inventory, the buffer, the tanks and
 * whatever a type adds through {@link #saveState(NbtCompound)}, and
 * {@link MachineBlockEntity} is what calls them while a chunk is stored.
 */
public abstract class Machine {

    private final MachineScreen screen;
    private final MachineInventory inventory;
    private final EnergyStorage energy;
    private final MachineTank[] tanks;
    private final List<RecipeType> recipeTypes;

    /** Input side, built once because a machine never changes its slots. */
    private final MachineInputs inputs;

    /** Output side, built once for the same reason. */
    private final MachineOutputs outputs;

    /** {@code true} once this machine has ruined itself, see {@link #explode()}. */
    private boolean exploded;

    /**
     * Creates a machine.
     *
     * @param screen how this machine is shown, see {@link MachineScreen}
     * @param inventory inventory whose slots carry the roles of the machine
     * @param energy storage the machine takes its energy from
     * @param recipeTypes types of recipe the machine reads, may be empty
     * @param tanks tanks of fluid the machine holds, may be empty
     */
    protected Machine(MachineScreen screen, MachineInventory inventory, EnergyStorage energy,
            List<RecipeType> recipeTypes, MachineTank... tanks) {
        this.screen = Objects.requireNonNull(screen, "screen");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.energy = Objects.requireNonNull(energy, "energy");
        this.recipeTypes = List.copyOf(recipeTypes == null ? List.of() : recipeTypes);
        this.tanks = tanks == null ? new MachineTank[0] : tanks.clone();

        this.inputs = new MachineInputs(inventory.gridOf(MachineInventory.Role.INPUT),
                storages(MachineTank.Role.INPUT));
        this.outputs = new MachineOutputs(inventory, slotIndexes(MachineInventory.Role.OUTPUT),
                storages(MachineTank.Role.OUTPUT));
    }

    /** Name of this machine, the title its screen writes in its upper left corner. */
    public String name() {
        return screen.title();
    }

    /** How this machine wants to be shown, given when it was registered. */
    public MachineScreen screen() {
        return screen;
    }

    /**
     * What is wrong with this machine right now.
     * <p>
     * The screen of a machine draws the icon of the error in its upper right corner. A
     * machine that is fine reports {@link MachineError#NONE}; the machines of the game so
     * far only report that they have work but no energy, see
     * {@link RecipeMachine#error()}.
     *
     * @return the error, never {@code null}
     */
    public MachineError error() {
        return MachineError.NONE;
    }

    /** Inventory of this machine. */
    public MachineInventory inventory() {
        return inventory;
    }

    /** Storage this machine takes its energy from. */
    public EnergyStorage energy() {
        return energy;
    }

    /** Amount of tanks of this machine. */
    public int tankCount() {
        return tanks.length;
    }

    /**
     * One tank of this machine.
     *
     * @param index tank index, {@code 0 <= index < tankCount()}
     * @return the tank, with the role it was declared with
     */
    public MachineTank tank(int index) {
        return tanks[index];
    }

    /** Tanks of one role, in the order they were declared. */
    public List<MachineTank> tanksOf(MachineTank.Role role) {
        List<MachineTank> found = new ArrayList<>();
        for (MachineTank tank : tanks) {
            if (tank.role() == role) {
                found.add(tank);
            }
        }
        return found;
    }

    /** Types of recipe this machine reads, empty for a machine that reads none. */
    public List<RecipeType> recipeTypes() {
        return recipeTypes;
    }

    /**
     * What this machine offers to a recipe.
     * <p>
     * The view is built once when the machine is created and handed out again and
     * again, so a recipe may keep it while it decides.
     *
     * @return the input side of this machine
     */
    public MachineInput inputs() {
        return inputs;
    }

    /**
     * Where the products of a recipe end up.
     *
     * @return the output side of this machine
     */
    public MachineOutput outputs() {
        return outputs;
    }

    /**
     * Advances the machine by one frame.
     * <p>
     * A machine that is fed by hand is fed through its tanks and not through a slot: a player clicks a tank
     * with a cell in hand, see {@code ContainerMenu#setTankFinder}, so the fluid is in the tank before the
     * frame that uses it is ever worked on.
     *
     * @param delta time since the last frame in seconds, ignored when not positive
     */
    public final void tick(float delta) {
        if (delta > 0.0f && !exploded) {
            update(delta);
        }
    }

    /**
     * {@code true} once this machine has ruined itself.
     * <p>
     * A machine sets it by breaking its own rule - a boiler that was heated past its limit - and the block
     * that carries the machine takes it out of the world, see
     * {@link com.philia093.neofactory.blockentity.MachineBlockEntity#update(com.philia093.neofactory.world.World,
     * float)}. A machine knows no world of its own, so this flag is how it reports what happened.
     *
     * @return {@code true} while the machine is beyond saving
     */
    public boolean isExploded() {
        return exploded;
    }

    /**
     * Ruins this machine, which takes it out of the world together with what it holds.
     * <p>
     * Called by a machine whose own rule was broken. Nothing is handed back: the slots, the tanks and the
     * buffer go with the block, which is what makes a machine that is pushed past its limit expensive.
     */
    protected void explode() {
        exploded = true;
    }

    /** {@code true} while the machine is working on something. */
    public boolean isRunning() {
        return false;
    }

    /**
     * Does one step of work.
     *
     * @param delta time since the last frame in seconds, always positive
     */
    protected abstract void update(float delta);

    /**
     * Writes everything this machine holds into a group.
     * <p>
     * The shared part - the slots, the buffer and the tanks - is written here, so a
     * machine of any type is stored the same way. What a type adds goes into its own
     * group through {@link #saveState(NbtCompound)}, exactly like an entity keeps its
     * own fields apart, see {@link com.philia093.neofactory.entity.Entity}.
     *
     * @param data group to fill
     */
    public final void save(NbtCompound data) {
        data.put(SaveTags.writeInventory(inventory));
        data.putInt(SaveTags.ENERGY, energy.amount());
        NbtList storedTanks = new NbtList(SaveTags.TANKS);
        for (MachineTank tank : tanks) {
            FluidStorage storage = tank.storage();
            NbtCompound entry = new NbtCompound("");
            entry.putString(SaveTags.FLUID,
                    storage.fluid() == null ? "" : storage.fluid().name());
            entry.putInt(SaveTags.FLUID_AMOUNT, storage.amount());
            storedTanks.add(entry);
        }
        data.put(storedTanks);
        NbtCompound state = new NbtCompound(SaveTags.MACHINE_STATE);
        saveState(state);
        data.put(state);
    }

    /**
     * Fills this machine from a group written by {@link #save(NbtCompound)}.
     * <p>
     * A group that misses an entry leaves the machine as it is, so a machine stored
     * before a field existed still loads.
     *
     * @param data group to read, may be {@code null}
     */
    public final void load(NbtCompound data) {
        if (data == null) {
            return;
        }
        SaveTags.readInventory(inventory, data.getList(SaveTags.INVENTORY));
        restoreEnergy(data.getInt(SaveTags.ENERGY, 0));
        NbtList storedTanks = data.getList(SaveTags.TANKS);
        if (storedTanks != null) {
            for (int index = 0; index < Math.min(storedTanks.size(), tanks.length); index++) {
                NbtCompound entry = storedTanks.getCompound(index);
                Fluid fluid = Fluids.byName(entry.getString(SaveTags.FLUID, ""));
                int amount = entry.getInt(SaveTags.FLUID_AMOUNT, 0);
                if (fluid != null && amount > 0) {
                    tanks[index].storage().fill(fluid, amount, false);
                }
            }
        }
        NbtCompound state = data.getCompound(SaveTags.MACHINE_STATE);
        loadState(state == null ? new NbtCompound(SaveTags.MACHINE_STATE) : state);
    }

    /**
     * Hands everything this machine holds to the world.
     * <p>
     * Called while the block of the machine is broken: without it the content of a
     * machine would vanish with the block. The tanks are left as they are, a fluid has
     * no form a player could pick up yet.
     *
     * @param drops sink receiving the items
     * @param worldX world X coordinate of the block
     * @param worldY world Y coordinate of the block
     */
    public void dumpItems(ItemDrops drops, float worldX, float worldY) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            drops.drop(stack, worldX, worldY);
            inventory.set(slot, ItemStack.EMPTY);
        }
    }

    /**
     * Writes what this type adds to the shared fields.
     *
     * @param state group to fill, never {@code null}
     */
    protected void saveState(NbtCompound state) {
        // A machine without a state of its own writes nothing.
    }

    /**
     * Reads what this type added to the shared fields.
     *
     * @param state group to read, empty when nothing was stored
     */
    protected void loadState(NbtCompound state) {
        // A machine without a state of its own reads nothing.
    }

    /** Puts a stored amount back into the buffer, as far as it fits. */
    private void restoreEnergy(int stored) {
        if (stored <= 0) {
            return;
        }
        if (energy instanceof SimpleEnergyStorage simple) {
            // The limits of a buffer describe how fast it is filled, not how full it may
            // be after a save game was read.
            simple.setAmount(stored);
            return;
        }
        energy.receive(stored, false);
    }

    /** Storage of every tank of a role, in the order they were declared. */
    private FluidStorage[] storages(MachineTank.Role role) {
        List<FluidStorage> found = new ArrayList<>();
        for (MachineTank tank : tanks) {
            if (tank.role() == role) {
                found.add(tank.storage());
            }
        }
        return found.toArray(new FluidStorage[0]);
    }

    /** Slot indexes of a role, the order a machine writes its products in. */
    private int[] slotIndexes(MachineInventory.Role role) {
        List<Integer> slots = inventory.slotsOf(role);
        int[] indexes = new int[slots.size()];
        for (int index = 0; index < indexes.length; index++) {
            indexes[index] = slots.get(index);
        }
        return indexes;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + screen.title() + ", " + inventory + ", running "
                + isRunning() + ")";
    }
}
