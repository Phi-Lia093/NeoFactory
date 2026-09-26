package com.philia093.neofactory.machine;

import com.philia093.neofactory.fluid.FluidStorage;
import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.ContainerMenu;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The screen of a machine, without a window.
 * <p>
 * The class turns what a machine registered, see {@link MachineScreen}, into the layout of a
 * container, so a new machine never mentions a pixel:
 * <ul>
 *     <li>the title stands in the upper left corner of the panel and the status of the
 *         machine in the upper right one;</li>
 *     <li>the input slots and the output slots form a block each, on the left and on the
 *         right of the progress bar. The shape of a block follows its size: one slot stands
 *         alone, two stand above each other, four form a square and six fill two rows of
 *         three, see {@link MachineScreen#columns(int)};</li>
 *     <li>the tanks of fluid stand at the foot of the panel - the ones a recipe drains in the
 *         lower left, the ones a machine fills next to them - with the configure slot in the
 *         lower right corner;</li>
 *     <li>the inventory of the player follows below, on the rows its own panel carries.</li>
 * </ul>
 * A window only has to hand the {@link #container()} to
 * {@link com.philia093.neofactory.gui.container.ContainerView} and draw the progress, the
 * tanks and the two lines of text on top of it, see
 * {@link com.philia093.neofactory.gui.MachineGui}.
 */
public final class MachineMenu {

    /** Width of the panel of a machine in pixels, the art of {@code gui/machine_icons.png}. */
    public static final int WIDTH = 176;

    /** Height of the panel of a machine in pixels. */
    public static final int HEIGHT = 166;

    /** X of the rightmost column of the block of input slots. */
    public static final int INPUT_RIGHT = 50;

    /** X of the leftmost column of the block of output slots. */
    public static final int OUTPUT_LEFT = 93;

    /**
     * X of the column of the upgrade slots, the one at the right edge of the panel.
     * <p>
     * The column belongs to the upgrades alone, so the block of the products stops before it
     * and the status line of the upper right corner stops beside it, see {@link #statusRight()}.
     */
    public static final int UPGRADE_LEFT = 152;

    /** Upper edge of the first machine slot. */
    public static final int MACHINE_TOP = 18;

    /** Y the column of the upgrade slots grows up from, its lower right corner. */
    public static final int UPGRADE_BOTTOM = 58;

    /** Largest amount of upgrade slots that fit into the column at the right edge. */
    public static final int MAX_UPGRADES = 4;

    /** X of the progress bar, which stands between the two blocks of slots. */
    public static final int ARROW_X = 71;

    /** Y of the progress bar, centred on the two rows of machine slots. */
    public static final int ARROW_Y = 27;

    /** Upper edge of the foot of the panel, where the tanks and the configure slot lie. */
    public static final int FOOT_TOP = 58;

    /** X of the first tank of the foot. */
    public static final int FOOT_LEFT = ContainerLayout.PADDING;

    /** Pixels between the tanks a recipe drains and the tanks a machine fills. */
    public static final int TANK_GAP = 8;

    /**
     * X of the configure slot, in the foot of the panel behind the tanks.
     * <p>
     * The corner of the panel itself belongs to the upgrade slots, see {@link #UPGRADE_LEFT},
     * so the configure slot stands at the right of the foot instead.
     */
    public static final int CONFIGURE_LEFT = 96;

    /** X of the title in the upper left corner, at the frame of the panel. */
    public static final int TEXT_LEFT = 4;

    /** Y of the title and of the status line, at the frame of the panel. */
    public static final int TEXT_TOP = 4;

    /** X the status line is aligned to when no upgrade slot stands at the right edge. */
    public static final int TEXT_RIGHT = WIDTH - TEXT_LEFT;

    /** Pixels between the status line and the column of the upgrade slots. */
    public static final int TEXT_GAP = 4;

    /** Unit the tooltip of a tank writes behind an amount of fluid, one cell being a thousand of them. */
    public static final String FLUID_UNIT = "mB";

    /** Name the tooltip of a tank writes while nothing is in it. */
    public static final String EMPTY_TANK = "Empty";

    /** Left edge of the first slot of the player inventory. */
    public static final int PLAYER_LEFT = ContainerLayout.PADDING;

    /** Upper edge of the first storage row of the player inventory. */
    public static final int PLAYER_STORAGE_TOP = 84;

    /** Upper edge of the hotbar row of the player inventory. */
    public static final int PLAYER_HOTBAR_TOP = 142;

    /** Amount of columns of the inventory of the player. */
    private static final int PLAYER_COLUMNS = 9;

    /** Amount of storage rows of the inventory of the player, the hotbar not counted. */
    private static final int PLAYER_STORAGE_ROWS = PlayerInventory.STORAGE_ROWS;

    /**
     * One tank at the foot of the panel.
     *
     * @param x left edge of the cell in pixels of the panel
     * @param y upper edge of the cell in pixels of the panel
     * @param tank index of the tank inside the machine, see {@link Machine#tank(int)}
     * @param input {@code true} for a tank a recipe drains, {@code false} for one a machine
     *              fills
     */
    public record FluidSlot(int x, int y, int tank, boolean input) {
    }

    private final Machine machine;
    private final ContainerMenu container;
    private final List<FluidSlot> fluidSlots;

    /**
     * Creates the menu of a machine.
     *
     * @param machine machine to show
     * @param player inventory of the player, shown below the machine
     * @throws IllegalArgumentException when the screen of the machine does not describe the
     *         slots its inventory holds, which is a mistake of the machine and not of the
     *         player
     */
    public MachineMenu(Machine machine, PlayerInventory player) {
        this.machine = Objects.requireNonNull(machine, "machine");
        MachineInventory inventory = machine.inventory();
        MachineScreen screen = machine.screen();

        List<Integer> inputs = inputSlots(inventory);
        List<Integer> outputs = inventory.slotsOf(MachineInventory.Role.OUTPUT);
        List<Integer> upgrades = inventory.slotsOf(MachineInventory.Role.UPGRADE);
        List<Integer> configure = inventory.slotsOf(MachineInventory.Role.CONFIGURE);
        require(machine, "input slots", screen.inputSlots(), inputs.size());
        require(machine, "output slots", screen.outputSlots(), outputs.size());
        require(machine, "input tanks", screen.fluidInputs(),
                machine.tanksOf(MachineTank.Role.INPUT).size());
        require(machine, "output tanks", screen.fluidOutputs(),
                machine.tanksOf(MachineTank.Role.OUTPUT).size());
        if (upgrades.size() > MAX_UPGRADES) {
            throw new IllegalArgumentException("The machine " + machine.name() + " holds "
                    + upgrades.size() + " upgrade slots, but only " + MAX_UPGRADES
                    + " fit into the column at its right edge");
        }

        ContainerLayout layout = new ContainerLayout();
        addBlock(layout, inventory, inputs, screen.inputs(), true);
        addBlock(layout, inventory, outputs, screen.outputs(), false);
        addUpgrades(layout, inventory, upgrades);
        this.fluidSlots = buildFluidSlots(machine, screen);
        if (!configure.isEmpty()) {
            layout.add(CONFIGURE_LEFT, FOOT_TOP, inventory, configure.get(0), Slot.Rule.NORMAL,
                    SlotKind.GENERIC.column(), SlotKind.GENERIC.row());
        }
        layout.addGrid(PLAYER_LEFT, PLAYER_STORAGE_TOP, PLAYER_COLUMNS, PLAYER_STORAGE_ROWS, player,
                PlayerInventory.HOTBAR_SLOTS, Slot.Rule.NORMAL);
        layout.addGrid(PLAYER_LEFT, PLAYER_HOTBAR_TOP, PLAYER_COLUMNS, 1, player, 0,
                Slot.Rule.NORMAL);
        this.container = new ContainerMenu(layout, player);
        // The tanks of the machine are not slots of the container - nothing is ever put into them - so the
        // container asks back here when a click lands on one, see ContainerMenu#setTankFinder.
        container.setTankFinder(this::tankAt);
    }

    /**
     * The slots a machine takes input from, in the order it declared them.
     * <p>
     * The fuel comes after the inputs because the machine collects the slots of a role one after the
     * other; the kinds of {@link MachineScreen#inputs()} have to be listed in the very same order.
     */
    private static List<Integer> inputSlots(MachineInventory inventory) {
        List<Integer> found = new ArrayList<>(inventory.slotsOf(MachineInventory.Role.INPUT));
        found.addAll(inventory.slotsOf(MachineInventory.Role.FUEL));
        return found;
    }

    /**
     * Tank of the panel under a point, for the container that trades cells with it.
     *
     * @param localX X coordinate relative to the panel
     * @param localY Y coordinate relative to the panel, measured downwards
     * @return the tank, or {@code null} when the point is not on one
     */
    private ContainerMenu.Tank tankAt(int localX, int localY) {
        FluidSlot slot = fluidSlotAt(localX, localY);
        if (slot == null) {
            return null;
        }
        return new ContainerMenu.Tank(machine.tank(slot.tank()).storage(), slot.input());
    }

    /**
     * Tank of the panel under a point, which is also the cell a tooltip is drawn at.
     *
     * @param localX X coordinate relative to the panel
     * @param localY Y coordinate relative to the panel, measured downwards
     * @return the tank, or {@code null} when the point is not on one
     */
    public FluidSlot fluidSlotAt(int localX, int localY) {
        for (FluidSlot slot : fluidSlots) {
            if (localX >= slot.x() && localX < slot.x() + ContainerLayout.SLOT_SIZE
                    && localY >= slot.y() && localY < slot.y() + ContainerLayout.SLOT_SIZE) {
                return slot;
            }
        }
        return null;
    }

    /**
     * Lines the tooltip of a tank is drawn from.
     * <p>
     * A player who is about to click a tank wants to know what is in it and how much room is left, so the
     * box names the fluid first and then what the tank holds of how much it takes. A tank that is empty
     * says so, and a fluid the player has never seen is named by the fluid itself.
     *
     * @param slot tank to describe
     * @return the lines of the box
     */
    public List<String> tankTooltip(FluidSlot slot) {
        FluidStorage tank = machine.tank(slot.tank()).storage();
        String name = tank.isEmpty() ? EMPTY_TANK : Item.prettify(tank.fluid().name());
        return List.of(name, tank.amount() + " / " + tank.capacity() + " " + FLUID_UNIT);
    }

    /**
     * Adds the column of the upgrade slots at the right edge of the panel.
     * <p>
     * The column grows upwards: the first upgrade stands in its lower right corner and the
     * next ones above it, so a machine with a single upgrade keeps the corner and a machine
     * with four fills the column up to the status line, see {@link #statusRight()}.
     */
    private static void addUpgrades(ContainerLayout layout, MachineInventory inventory,
            List<Integer> upgrades) {
        for (int index = 0; index < upgrades.size(); index++) {
            layout.add(UPGRADE_LEFT, UPGRADE_BOTTOM - index * ContainerLayout.SLOT_PITCH,
                    inventory, upgrades.get(index), Slot.Rule.NORMAL, SlotKind.GENERIC.column(),
                    SlotKind.GENERIC.row());
        }
    }

    /** Says what a machine and its screen disagree about. */
    private static void require(Machine machine, String what, int declared, int held) {
        if (declared != held) {
            throw new IllegalArgumentException("The screen of " + machine.name() + " declares "
                    + declared + " " + what + " but the machine holds " + held);
        }
    }

    /**
     * Adds one block of slots.
     * <p>
     * The block is filled row by row from its upper left corner, the order the kinds are
     * listed in. The block of the inputs grows to the left of the progress bar and the block
     * of the products to its right, so the bar stays where it is whichever shape a machine
     * asks for.
     */
    private static void addBlock(ContainerLayout layout, MachineInventory inventory,
            List<Integer> slots, List<SlotKind> kinds, boolean input) {
        if (slots.isEmpty()) {
            // A machine may hold no slot of a side: a boiler has no product and a tank of fluid has no
            // item at all, so there is nothing to arrange, see MachineScreen#columns(int).
            return;
        }
        int columns = MachineScreen.columns(slots.size());
        int rows = MachineScreen.rows(slots.size());
        int left = input ? INPUT_RIGHT - (columns - 1) * ContainerLayout.SLOT_PITCH : OUTPUT_LEFT;
        int top = MACHINE_TOP + (rows == 1 ? ContainerLayout.SLOT_PITCH / 2 : 0);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int index = row * columns + column;
                if (index >= slots.size()) {
                    continue;
                }
                SlotKind kind = kinds.get(index);
                layout.add(left + column * ContainerLayout.SLOT_PITCH,
                        top + row * ContainerLayout.SLOT_PITCH, inventory, slots.get(index),
                        input ? Slot.Rule.NORMAL : Slot.Rule.OUTPUT, kind.column(), kind.row());
            }
        }
    }

    /** Places the tanks at the foot of the panel, the input tanks first. */
    private static List<FluidSlot> buildFluidSlots(Machine machine, MachineScreen screen) {
        List<FluidSlot> found = new ArrayList<>();
        int x = FOOT_LEFT;
        for (int index = 0; index < screen.fluidInputs(); index++) {
            found.add(new FluidSlot(x, FOOT_TOP,
                    tankIndex(machine, MachineTank.Role.INPUT, index), true));
            x += ContainerLayout.SLOT_PITCH;
        }
        if (screen.fluidInputs() > 0 && screen.fluidOutputs() > 0) {
            x += TANK_GAP;
        }
        for (int index = 0; index < screen.fluidOutputs(); index++) {
            found.add(new FluidSlot(x, FOOT_TOP,
                    tankIndex(machine, MachineTank.Role.OUTPUT, index), false));
            x += ContainerLayout.SLOT_PITCH;
        }
        return List.copyOf(found);
    }

    /** Index of the n-th tank of a role inside a machine, {@code -1} when there is none. */
    private static int tankIndex(Machine machine, MachineTank.Role role, int nth) {
        int seen = 0;
        for (int index = 0; index < machine.tankCount(); index++) {
            if (machine.tank(index).role() == role && seen++ == nth) {
                return index;
            }
        }
        return -1;
    }

    /** Machine this menu shows. */
    public Machine machine() {
        return machine;
    }

    /** Container holding the slots of the machine and of the player. */
    public ContainerMenu container() {
        return container;
    }

    /** Tanks at the foot of the panel, the input tanks first. */
    public List<FluidSlot> fluidSlots() {
        return fluidSlots;
    }

    /** {@code true} when the machine keeps the lower right corner for a configure slot. */
    public boolean hasConfigureSlot() {
        return machine.inventory().slotOf(MachineInventory.Role.CONFIGURE) >= 0;
    }

    /** {@code true} when the machine holds upgrade slots in the column at its right edge. */
    public boolean hasUpgradeColumn() {
        return machine.inventory().slotOf(MachineInventory.Role.UPGRADE) >= 0;
    }

    /**
     * X the status line of the upper right corner is aligned to.
     * <p>
     * The column at the right edge of the panel belongs to the upgrade slots, and a full
     * column reaches up to the line of the title. A machine that holds upgrades therefore
     * keeps its status line to their left instead of running under them, see
     * {@link #UPGRADE_BOTTOM}.
     *
     * @return the coordinate in pixels of the panel
     */
    public int statusRight() {
        return hasUpgradeColumn() ? UPGRADE_LEFT - TEXT_GAP : TEXT_RIGHT;
    }

    /** Title of the machine, written in the upper left corner of the panel. */
    public String title() {
        return machine.screen().title();
    }

    /** Kind of the progress bar of this machine, given when it was registered. */
    public ProgressKind progressKind() {
        return machine.screen().progress();
    }

    /** What is wrong with the machine right now, {@link MachineError#NONE} for a fine one. */
    public MachineError error() {
        return machine.error();
    }

    /**
     * Line a machine writes in the upper right corner of its panel.
     * <p>
     * A machine that has a number of its own answers first, see {@link StatusMachine} - the temperature of a
     * boiler is what a player watches there. Next comes what the furnace reports, what is left of the fuel it
     * burns, see {@link FuelMachine}; a machine that has nothing to say leaves the corner empty.
     *
     * @return the text, empty when the machine reports nothing
     */
    public String statusText() {
        if (machine instanceof StatusMachine status) {
            return status.statusText();
        }
        if (machine instanceof FuelMachine fuel) {
            return "Remain fuel: " + Math.round(fuel.fuelSeconds()) + "s";
        }
        return "";
    }

    /** Seconds of fuel the machine has left, {@code 0} for a machine that burns nothing. */
    public float fuelSeconds() {
        return machine instanceof FuelMachine fuel ? fuel.fuelSeconds() : 0.0f;
    }

    /**
     * Share of the work of the machine that is done.
     *
     * @return a value between {@code 0} and {@code 1}, {@code 0} for a machine that does not
     *         report a progress
     */
    public float craftProgress() {
        return machine instanceof ProgressMachine progress ? progress.craftProgress() : 0.0f;
    }

    /** {@code true} while the machine works, so a screen may draw it as running. */
    public boolean isRunning() {
        return machine.isRunning();
    }

    @Override
    public String toString() {
        return "MachineMenu(" + machine + ")";
    }
}
