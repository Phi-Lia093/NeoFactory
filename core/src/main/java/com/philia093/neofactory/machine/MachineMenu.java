package com.philia093.neofactory.machine;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.fluid.FluidStorage;
import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.ContainerMenu;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
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
 *     <li>nothing of the state of a machine is written in words: the mark of the pack stands in the upper
 *         left corner of the panel and names the machine while the mouse rests on it, a flame under the slot
 *         of a fuel shows what is left of the fire or how hot a boiler is, and both say the exact number in a
 *         tooltip. What a machine reports, see {@link MachineMenu#flameTooltip};</li>
 *     <li>the input slots and the output slots form a block each, on the left and on the
 *         right of the progress bar. The shape of a block follows its size: one slot stands
 *         alone, two stand beside each other, four form a square and six fill two rows of
 *         three, see {@link MachineScreen#columns(int)};</li>
 *     <li>the tanks of fluid stand at the foot of the panel - the ones a recipe drains on the
 *         left, the ones a machine fills on the right - with the cell of energy between the
 *         two pairs of them. The panel of a machine of this workshop has no column for
 *         upgrades and no corner to configure;</li>
 *     <li>the sides of the machine are read from its front and are set from here as well as with the wrench:
 *         the tooltip of a tank names the side the tank is reached through while the modifier key is held,
 *         and the wheel walks that side on, see {@link #cycleTank(int, int, int)};</li>
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

    /** Left edge of the info mark in the upper left corner of the panel, where the name used to stand. */
    public static final int INFO_LEFT = 2;

    /** Upper edge of that mark, measured from the upper edge of the panel. */
    public static final int INFO_TOP = 2;

    /** Side of the info mark in pixels, the size of the mark of the pack. */
    public static final int INFO_SIZE = 16;

    /** Pixels between the mark of a state and the next mark beside it. */
    public static final int MARK_GAP = 2;

    /** Pixels between the slot a fuel lies in and the flame under it. */
    public static final int FLAME_GAP = 4;

    /** Kelvin a flame of a boiler is empty at, twenty five degrees Celsius. */
    public static final float FLAME_COLD_TEMPERATURE = 298.0f;

    /** Kelvin a flame of a boiler is full at, a hundred degrees Celsius. */
    public static final float FLAME_HOT_TEMPERATURE = 373.0f;

    /** Column of the three flames in the icon sheet: the copper, the steel and the plain one share it. */
    public static final int FLAME_COLUMN = 6;

    /** Row of the flame of copper while it burns. */
    public static final int FLAME_COPPER_ROW = 2;

    /** Row of the flame of steel while it burns. */
    public static final int FLAME_STEEL_ROW = 3;

    /** Row of the plain flame while it burns. */
    public static final int FLAME_NORMAL_ROW = 4;

    /** Row of the flame of copper while its fire is out. */
    public static final int FLAME_COPPER_OUT_ROW = 5;

    /** Row of the flame of steel while its fire is out. */
    public static final int FLAME_STEEL_OUT_ROW = 6;

    /** Row of the plain flame while its fire is out. */
    public static final int FLAME_NORMAL_OUT_ROW = 7;

    /** Pixels the flame stands left of the slot it belongs to. */
    public static final int FLAME_LEFT_PIXELS = 1;

    /** X of the mark of an error, in the spot the flame of a furnace and of a boiler stands in. */
    public static final int ERROR_LEFT = 50;

    /** Upper edge of that mark, which is the row of the flame of those two machines. */
    public static final int ERROR_TOP = 41;

    /** Side of a mark of state in pixels, the side of a cell of the icon grid. */
    public static final int MARK_SIZE = 18;

    /** Pixels the flame is lifted towards the slot it belongs to, the gap below it counted. */
    public static final int FLAME_UP_PIXELS = 2;

    /** Unit the tooltip of a tank writes behind an amount of fluid, one cell being a thousand of them. */
    public static final String FLUID_UNIT = "mB";

    /** Name the tooltip of a tank writes while nothing is in it. */
    public static final String EMPTY_TANK = "Empty";

    /** Word the tooltip of a tank writes before the side it is reached through. */
    public static final String FACING_PREFIX = "FACING: ";

    /** Name the cell of energy at the foot of the panel is written with in its tooltip. */
    public static final String ENERGY = "Energy";

    /** Unit the tooltip of the buffer writes behind an amount of energy, the unit of the original game. */
    public static final String ENERGY_UNIT = "EU";

    /** Word the tooltip of the buffer writes before the side the power of a line comes in through. */
    public static final String POWER_IN_PREFIX = "POWER IN: ";

    /** Word the tooltip of the buffer writes before the side the power of the machine leaves through. */
    public static final String POWER_OUT_PREFIX = "POWER OUT: ";

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
     * <p>
     * <b>The sides of a machine are named from its own front.</b> The tooltip of a tank says
     * {@code FACING: LEFT} and never {@code north}, because a player who turned a machine reads the flanks of
     * the machine they are looking at and not sides of the world - and the machine holds those words itself,
     * so a screen that is opened after the machine was turned names them the same way, see
     * {@link FaceConfig#nameOfTank(int)}.
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
        addEnergySlot(layout, inventory, style);
        this.fluidSlots = buildFluidSlots(machine, screen);
        layout.addGrid(PLAYER_LEFT, PLAYER_STORAGE_TOP, PLAYER_COLUMNS, PLAYER_STORAGE_ROWS, player,
                PlayerInventory.HOTBAR_SLOTS, Slot.Rule.NORMAL);
        layout.addGrid(PLAYER_LEFT, PLAYER_HOTBAR_TOP, PLAYER_COLUMNS, 1, player, 0,
                Slot.Rule.NORMAL);
        this.container = new ContainerMenu(layout, player);
        // The tanks of the machine are not slots of the container - nothing is ever put into them - so the
        // container asks back here when a click lands on one, see ContainerMenu#setTankFinder.
        container.setTankFinder(this::tankAt);
        // The cell of energy of a machine of the line is a slot and not a picture of how full the buffer is,
        // so the container asks back here for what may lie in it and for what its box says, see SlotRules.
        container.setSlotRules(new ContainerMenu.SlotRules() {

            @Override
            public boolean accepts(Slot slot, ItemStack stack) {
                return !isEnergySlot(slot) || Reagents.isReagent(stack);
            }

            @Override
            public boolean namesItsStack(Slot slot) {
                return !isEnergySlot(slot);
            }
        });
    }

    /**
     * Places the cell of energy at the foot of the panel, the one place a machine is fed by hand.
     * <p>
     * <b>The cell is a slot and not a bar.</b> What it shows is not how full the buffer of the machine is but
     * what a player put into it - a piece of the reagent the machine burns - and what the buffer holds is what
     * its box names while the mouse rests on it, see {@link #energyTooltip(boolean)} and
     * {@link com.philia093.neofactory.machine.ElectricMachine}. A machine that may not be fed by hand - a
     * machine of steam, a generator - keeps the cell as a picture of its own state and this adds no slot, see
     * {@link MachineInventory.Role#ENERGY}.
     *
     * @param layout layout of the panel
     * @param inventory inventory of the machine
     * @param style style the machine is drawn in
     */
    private static void addEnergySlot(ContainerLayout layout, MachineInventory inventory,
            MachineStyle style) {
        int slot = inventory.slotOf(MachineInventory.Role.ENERGY);
        if (slot < 0) {
            return;
        }
        layout.add(ENERGY_X, FOOT_TOP, inventory, slot, Slot.Rule.NORMAL,
                iconColumn(SlotKind.BATTERY, style), iconRow(SlotKind.BATTERY, style));
    }

    /**
     * Index of the cell of energy in the inventory of the machine.
     *
     * @return the slot a player feeds the machine through, {@code -1} for a machine that is not fed by hand
     */
    public int energySlot() {
        return machine.inventory().slotOf(MachineInventory.Role.ENERGY);
    }

    /** {@code true} when the cell of energy at the foot of the panel is a slot a player fills by hand. */
    public boolean hasEnergySlot() {
        return energySlot() >= 0;
    }

    /** {@code true} when a slot of the layout is the cell of energy of this machine. */
    private boolean isEnergySlot(Slot slot) {
        return slot.inventory() == machine.inventory() && slot.index() == energySlot();
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
     * Lines the tooltip of a tank is drawn from, without the side it is reached through.
     *
     * @param slot tank to describe
     * @return the lines of the box
     */
    public List<String> tankTooltip(FluidSlot slot) {
        return tankTooltip(slot, false);
    }

    /**
     * Lines the tooltip of a tank is drawn from.
     * <p>
     * A player who is about to click a tank wants to know what is in it and how much room is left, so the
     * box names the fluid first and then what the tank holds of how much it takes. A tank that is empty
     * says so, and a fluid the player has never seen is named by the fluid itself.
     * <p>
     * <b>The side the tank is reached through is named while the modifier key is held.</b> The last line
     * says {@code FACING: LEFT} - a side of the machine and not of the world, so a player who turned the
     * machine reads the flanks of the machine they are looking at - and a tank that no side reaches says
     * {@code FACING: NONE}, which is what a tank the wheel walked past the front reports, see
     * {@link #cycleTank(int, int, int)}.
     *
     * @param slot tank to describe
     * @param showSide {@code true} while the modifier key is held, which names the side of the tank
     * @return the lines of the box
     */
    public List<String> tankTooltip(FluidSlot slot, boolean showSide) {
        FluidStorage tank = machine.tank(slot.tank()).storage();
        String name = tank.isEmpty() ? EMPTY_TANK : Item.prettify(tank.fluid().name());
        String amount = tank.amount() + " / " + tank.capacity() + " " + FLUID_UNIT;
        if (!showSide) {
            return List.of(name, amount);
        }
        return List.of(name, amount, FACING_PREFIX + sideName(slot.tank()));
    }

    /**
     * Lines the tooltip of the cell of energy is drawn from.
     * <p>
     * The cell at the foot of the panel shows how much of the buffer of a machine is filled, so the box says
     * what that amount is; a machine of the age of steam has no buffer at all and says nothing, which is why
     * the answer may be empty. With the modifier key held the box names the sides the power is reached
     * through - the plug a line of cables feeds and the plug a generator hands its power over to it - in the
     * words a player reads, see {@link MachineSides#nameOf(BlockFace, BlockFace)}.
     *
     * @param showSide {@code true} while the modifier key is held, which names the plugs of the power
     * @return the lines of the box, empty for a machine without a buffer
     */
    public List<String> energyTooltip(boolean showSide) {
        EnergyStorage buffer = machine.energy();
        if (buffer == null || buffer.capacity() <= 0) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        lines.add(ENERGY);
        lines.add(buffer.amount() + " / " + buffer.capacity() + " " + ENERGY_UNIT);
        FaceConfig faces = machine.faces();
        if (showSide && faces.takesPower()) {
            lines.add(POWER_IN_PREFIX + faces.nameOfEnergyIn());
        }
        if (showSide && faces.givesPower()) {
            lines.add(POWER_OUT_PREFIX + faces.nameOfEnergyOut());
        }
        return List.copyOf(lines);
    }

    /**
     * Walks the tank under a point of the panel to its next side, which is what the wheel of the interface
     * does.
     * <p>
     * A tank of a machine is reached through one side of its block, and while its screen is open that side is
     * set with the wheel the way it is set with the wrench in the world: the sides are walked in the order a
     * player reads them and <b>the walk steps over the front and over what another part of the machine
     * owns</b>, so the wheel of a tank never takes the plug of the power away, see
     * {@link FaceConfig#cycleTank(int, int)}. A tank that is reached from nowhere is where the
     * walk starts, which is how a player takes a side away from a tank again.
     * <p>
     * A point that is not on a tank is not the business of the wheel, so nothing happens there.
     *
     * @param localX X coordinate relative to the panel
     * @param localY Y coordinate relative to the panel, measured downwards
     * @param notches notches the wheel was rolled, positive for forwards
     * @return {@code true} when the side of a tank changed
     */
    public boolean cycleTank(int localX, int localY, int notches) {
        FluidSlot slot = fluidSlotAt(localX, localY);
        if (slot == null || notches == 0) {
            return false;
        }
        return machine.faces().cycleTank(slot.tank(), notches > 0 ? 1 : -1);
    }

    /**
     * Name of the side one tank is reached through, in the words a player reads.
     *
     * @param tank index of the tank, {@code 0 <= tank < machine.tankCount()}
     * @return the name, {@link MachineSides#NONE} for a tank no side reaches
     * @throws IllegalArgumentException when the machine holds no tank of that index
     */
    public String sideName(int tank) {
        FaceConfig faces = machine.faces();
        if (tank < 0 || tank >= faces.tankCount()) {
            throw new IllegalArgumentException("The machine " + machine.name() + " holds " + faces.tankCount()
                    + " tanks and has no tank " + tank);
        }
        return faces.nameOfTank(tank);
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
    /**
     * Places the tanks at the foot of the panel.
     * <p>
     * The tank a recipe is drained from stands at the left of the foot and the tank a machine fills at the
     * right of it, and the cell of energy lies between the two pairs, see {@link #FLUID_INPUT_X},
     * {@link #ENERGY_X} and {@link #FLUID_OUTPUT_X}. A second tank of a side stands one pitch further out,
     * so an even number of tanks reads as a pair and never covers the cell of energy.
     * <p>
     * <b>A machine without a bar stands its tanks on the row of the bar.</b> The screen of a generator names
     * no progress to show, see {@link ProgressKind#NONE}, so the row the bar of every other machine stands on
     * - the row its slots stand on - carries the fluid the generator drinks and the fluid it makes: the place
     * of a bar that never fills shows what the machine works on instead.
     */
    private static List<FluidSlot> buildFluidSlots(Machine machine, MachineScreen screen) {
        int row = screen.progress().hasBar() ? FOOT_TOP : ARROW_Y;
        List<FluidSlot> found = new ArrayList<>();
        for (int index = 0; index < screen.fluidInputs(); index++) {
            found.add(new FluidSlot(rowX(screen, true, index), row,
                    tankIndex(machine, MachineTank.Role.INPUT, index), true));
        }
        for (int index = 0; index < screen.fluidOutputs(); index++) {
            found.add(new FluidSlot(rowX(screen, false, index), row,
                    tankIndex(machine, MachineTank.Role.OUTPUT, index), false));
        }
        return List.copyOf(found);
    }

    /**
     * X coordinate of one tank on its row.
     * <p>
     * A tank of a machine that has a bar stands at the foot of the panel, where the two pairs of tanks and the
     * cell of energy between them were laid out; a tank of a generator is set out around the place of the bar,
     * the ones it drinks at the left of it and the ones it makes at the right.
     *
     * @param screen screen of the machine
     * @param input {@code true} for a tank a recipe drains
     * @param index tank index among the tanks of that side
     * @return the coordinate of its left edge
     */
    private static int rowX(MachineScreen screen, boolean input, int index) {
        if (screen.progress().hasBar()) {
            return (input ? FLUID_INPUT_X : FLUID_OUTPUT_X) - index * ContainerLayout.SLOT_PITCH;
        }
        return ARROW_X - (input ? 0 : -ContainerLayout.SLOT_SIZE)
                - (input ? index : -index) * ContainerLayout.SLOT_PITCH;
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
     * Name of the machine, which its screen asks for while the mouse rests on the mark of the upper left
     * corner of the panel.
     *
     * @return the name
     */
    public String title() {
        return machine.screen().title();
    }

    /** Kind of the progress bar of this machine, given when it was registered. */
    public ProgressKind progressKind() {
        return machine.screen().progress();
    }

    /**
     * Icon the cell of energy at the foot of the panel wears, which is the cell of a battery.
     * <p>
     * <b>The place where the power of a machine is read is the very cell a battery would be put into in the
     * screens of the original game</b> - the icon at the column and row of {@link SlotKind#BATTERY} - so a
     * player reads it as the cell of the power of the machine and not as a slot that was left empty.
     * <p>
     * <b>A machine of the line is fed there.</b> Its cell is a slot and a player puts the reagent the machine
     * burns into it, see {@link Reagents} and {@link #hasEnergySlot}; <b>a generator</b> shows the plain cell
     * that counts what it makes, and the two of them wear the same picture because they are the same place - a
     * machine that makes power fills the cell and a machine that works is fed through it. A machine of the age
     * of steam that works on a recipe has neither a shelf nor a generator behind it, so its cell wears the
     * plain picture of its own panel, which is what a slot that belongs to no recipe is drawn with.
     *
     * @return kind of the icon of that cell
     */
    public SlotKind energySlotKind() {
        return hasEnergySlot() || !progressKind().hasBar() ? SlotKind.BATTERY : SlotKind.GENERIC;
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

    /** Column of the flame of this machine in the icon sheet, which the flavours of the sheet share. */
    public int flameColumn() {
        return FLAME_COLUMN;
    }

    /** Row of the flame of this machine while it burns, which is the one of its style. */
    public int flameRow() {
        return switch (style()) {
            case BRONZE -> FLAME_COPPER_ROW;
            case STEEL -> FLAME_STEEL_ROW;
            case NORMAL -> FLAME_NORMAL_ROW;
        };
    }

    /** Row of the flame of this machine while its fire is out, which is the one of its style. */
    public int flameOutRow() {
        return switch (style()) {
            case BRONZE -> FLAME_COPPER_OUT_ROW;
            case STEEL -> FLAME_STEEL_OUT_ROW;
            case NORMAL -> FLAME_NORMAL_OUT_ROW;
        };
    }

    /**
     * Cell of the slot the machine burns in, {@code null} for a machine that burns nothing.
     * <p>
     * The slots of the machine stand at the head of the layout in the order its inventory declares them, so
     * the cell of a role is the entry of the very index the role has, see {@link MachineInventory#slotOf}.
     *
     * @return the cell of the fuel, or {@code null} for a machine without such a slot
     */
    public Slot fuelCell() {
        int index = machine.inventory().slotOf(MachineInventory.Role.FUEL);
        if (index < 0 || index >= container.layout().slots().size()) {
            return null;
        }
        return container.layout().slots().get(index);
    }

    /** {@code true} for a machine whose screen draws a flame under the slot it burns in. */
    public boolean hasFlame() {
        return fuelCell() != null;
    }

    /**
     * Share of the flame of this machine, from nothing to everything.
     * <p>
     * A boiler is read by its temperature, which runs from the cold of the room to its boiling point, and a
     * machine that burns an item is read by what is left of that item. What a share means to a player is in
     * the tooltip, see {@link #flameTooltip}.
     *
     * @return a value between {@code 0} and {@code 1}
     */
    public float flameShare() {
        if (machine instanceof SteamBoilerMachine boiler) {
            float share = (boiler.temperature() - FLAME_COLD_TEMPERATURE)
                    / (FLAME_HOT_TEMPERATURE - FLAME_COLD_TEMPERATURE);
            return Math.max(0.0f, Math.min(1.0f, share));
        }
        if (machine instanceof FuelMachine fuel) {
            return Math.max(0.0f, Math.min(1.0f, fuel.burnProgress()));
        }
        return 0.0f;
    }

    /**
     * {@code true} while there is a fire at all, which is what picks between the two flames of the sheet.
     * <p>
     * A machine that has run out of fuel and a boiler at the temperature of the room both have no fire, and
     * both show the picture of a flame that is out, see {@link #flameOutRow}.
     */
    public boolean flameIsLit() {
        return flameShare() > 0.0f;
    }

    /** {@code true} while a flame over its top blinks, which a boiler over a hundred degrees does. */
    public boolean flameBlinks() {
        return machine instanceof SteamBoilerMachine boiler
                && boiler.temperature() > FLAME_HOT_TEMPERATURE;
    }

    /** {@code true} for a flame that is drawn on the picture of a flame that is out, which a boiler is. */
    public boolean flameHasBase() {
        return machine instanceof SteamBoilerMachine;
    }

    /** Lines the mark of an error is named with, empty while nothing is wrong. */
    public List<String> errorTooltip() {
        String text = error().text();
        return text.isEmpty() ? List.of() : List.of(text);
    }

    /** Lines the flame is named with, empty for a machine that reports nothing about its fire. */
    public List<String> flameTooltip() {
        String status = statusText();
        return status.isEmpty() ? List.of() : List.of(status);
    }

    /** Lines the info mark of the upper left corner is named with, which is the name of the machine. */
    public List<String> infoTooltip() {
        return List.of(title());
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
