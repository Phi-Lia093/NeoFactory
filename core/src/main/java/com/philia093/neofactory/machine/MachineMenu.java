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
 *     <li>the title stands on the label in the upper left corner of the panel and the status
 *         of the machine in the upper right one;</li>
 *     <li>the input slots and the output slots form a block each, on the left and on the
 *         right of the progress bar. The shape of a block follows its size: one slot stands
 *         alone, two stand beside each other, four form a square and six fill two rows of
 *         three, see {@link MachineScreen#columns(int)};</li>
 *     <li>the tanks of fluid stand at the foot of the panel - the ones a recipe drains on the
 *         left, the ones a machine fills on the right - with the cell of energy between the
 *         two pairs of them. The panel of a machine of this workshop has no column for
 *         upgrades and no corner to configure;</li>
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

    /** X of the input slot nearest the progress bar: a second input stands one pitch to its left. */
    public static final int INPUT_RIGHT = 51;

    /** X of the output slot nearest the progress bar: a second output stands one pitch to its right. */
    public static final int OUTPUT_LEFT = 106;

    /** Upper edge of the first row of machine slots, the row every machine stands on. */
    public static final int MACHINE_TOP = 23;

    /** X of the progress bar, which stands between the two blocks of slots. */
    public static final int ARROW_X = 79;

    /** Y of the progress bar, on the row the slots of the machine stand on. */
    public static final int ARROW_Y = 23;

    /** Upper edge of the foot of the panel, where the tanks and the cell of energy lie. */
    public static final int FOOT_TOP = 62;

    /** X of the first tank a recipe drains: a second tank stands one pitch to its left. */
    public static final int FLUID_INPUT_X = 51;

    /** X of the first tank a machine fills: a second tank stands one pitch to its left. */
    public static final int FLUID_OUTPUT_X = 125;

    /** X of the cell of energy at the foot, between the two pairs of tanks. */
    public static final int ENERGY_X = 79;

    /** Left edge of the label the name of a machine is written on, in the upper left corner. */
    public static final int LABEL_LEFT = 1;

    /** Upper edge of that label, measured from the upper edge of the panel. */
    public static final int LABEL_TOP = 1;

    /** Height of that label, the picture of a tab stretched to it. */
    public static final int LABEL_HEIGHT = 12;

    /** Pixels between the edge of the label and the name written on it. */
    public static final int LABEL_PAD = 4;

    /** X of the title, written on the label and not on the frame of the panel. */
    public static final int TEXT_LEFT = 5;

    /**
     * Y of the title and of the status line, measured from the upper edge of the panel.
     * <p>
     * The name stands on the label of {@link #LABEL_TOP} and {@link #LABEL_HEIGHT} and not on the frame of
     * the panel: the label carries the name, so what the picture of a panel looks like below it can never
     * move a letter of the screen. Both numbers are written out for the same reason - moving the label
     * moves the name, and nothing else moves with either of them.
     */
    public static final int TEXT_TOP = 3;

    /** X the status line of the upper right corner is aligned to. */
    public static final int TEXT_RIGHT = WIDTH - TEXT_LEFT;

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
        if (!upgrades.isEmpty() || !configure.isEmpty() || screen.configureSlot()) {
            // The panel of every machine of the workshop holds two blocks of slots, the tanks at its foot and
            // the cell of energy between them - and no column for upgrades and no corner to configure. A
            // machine that asks for one of those would have slots with no cell to be put in, which is a
            // mistake of the machine and not something a player should ever see.
            throw new IllegalArgumentException("The machine " + machine.name()
                    + " asks for upgrade or configure slots, and the panel has no room for them");
        }

        ContainerLayout layout = new ContainerLayout();
        MachineStyle style = screen.style();
        addBlock(layout, inventory, inputs, screen.inputs(), true, style);
        addBlock(layout, inventory, outputs, screen.outputs(), false, style);
        this.fluidSlots = buildFluidSlots(machine, screen);
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
     * Column of the cell a kind of slot is drawn from.
     * <p>
     * The plain kind names no cell of the sheet: it is the slot of the very panel the machine stands in, which
     * is grey for a machine of the electrical age and bronze for one of the steam age, so such a slot is handed
     * to the screen without a picture, see {@link Slot#DEFAULT_ICON} and {@link MachineStyle}.
     *
     * @param kind kind the machine declared for a slot
     * @param style style the machine is drawn in
     * @return the column of the cell, {@link Slot#DEFAULT_ICON} for the plain slot of the style
     */
    private static int iconColumn(SlotKind kind, MachineStyle style) {
        return kind == SlotKind.GENERIC ? Slot.DEFAULT_ICON : kind.column(style);
    }

    /** Row of the cell a kind of slot is drawn from, {@link Slot#DEFAULT_ICON} for the plain slot. */
    private static int iconRow(SlotKind kind, MachineStyle style) {
        return kind == SlotKind.GENERIC ? Slot.DEFAULT_ICON : kind.row(style);
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
     * The block is read from the progress bar outwards: the first kind of the inputs stands against the bar
     * and the next ones to its left, the products the other way round, so both blocks keep the same distance
     * from the bar whichever shape a machine asks for. A block of one kind takes the row every machine stands
     * on, and a block of four or six fills two rows of two or three, the second row one pitch below the first.
     */
    private static void addBlock(ContainerLayout layout, MachineInventory inventory,
            List<Integer> slots, List<SlotKind> kinds, boolean input, MachineStyle style) {
        if (slots.isEmpty()) {
            // A machine may hold no slot of a side: a boiler has no product and a tank of fluid has no
            // item at all, so there is nothing to arrange, see MachineScreen#columns(int).
            return;
        }
        int columns = MachineScreen.columns(slots.size());
        int rows = MachineScreen.rows(slots.size());
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int index = row * columns + column;
                if (index >= slots.size()) {
                    continue;
                }
                int x = input ? INPUT_RIGHT - column * ContainerLayout.SLOT_PITCH
                        : OUTPUT_LEFT + column * ContainerLayout.SLOT_PITCH;
                SlotKind kind = kinds.get(index);
                layout.add(x, MACHINE_TOP + row * ContainerLayout.SLOT_PITCH, inventory,
                        slots.get(index), input ? Slot.Rule.NORMAL : Slot.Rule.OUTPUT,
                        iconColumn(kind, style), iconRow(kind, style));
            }
        }
    }

    /**
     * Places the tanks at the foot of the panel.
     * <p>
     * The tank a recipe is drained from stands at the left of the foot and the tank a machine fills at the
     * right of it, and the cell of energy lies between the two pairs, see {@link #FLUID_INPUT_X},
     * {@link #ENERGY_X} and {@link #FLUID_OUTPUT_X}. A second tank of a side stands one pitch further out,
     * so an even number of tanks reads as a pair and never covers the cell of energy.
     */
    private static List<FluidSlot> buildFluidSlots(Machine machine, MachineScreen screen) {
        List<FluidSlot> found = new ArrayList<>();
        for (int index = 0; index < screen.fluidInputs(); index++) {
            found.add(new FluidSlot(FLUID_INPUT_X - index * ContainerLayout.SLOT_PITCH, FOOT_TOP,
                    tankIndex(machine, MachineTank.Role.INPUT, index), true));
        }
        for (int index = 0; index < screen.fluidOutputs(); index++) {
            found.add(new FluidSlot(FLUID_OUTPUT_X - index * ContainerLayout.SLOT_PITCH, FOOT_TOP,
                    tankIndex(machine, MachineTank.Role.OUTPUT, index), false));
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

    /**
     * X the status line of the upper right corner is aligned to, the right frame of the panel.
     *
     * @return the coordinate in pixels of the panel
     */
    public int statusRight() {
        return TEXT_RIGHT;
    }

    /** Title of the machine, written in the upper left corner of the panel. */
    public String title() {
        return machine.screen().title();
    }

    /** Kind of the progress bar of this machine, given when it was registered. */
    public ProgressKind progressKind() {
        return machine.screen().progress();
    }

    /** Style the machine is drawn in, which is the panel and the slots its screen uses. */
    public MachineStyle style() {
        return machine.screen().style();
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
