package com.philia093.neofactory.machine;

import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.ContainerMenu;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.item.PlayerInventory;

import java.util.Objects;

/**
 * The screen of a machine, without a window.
 * <p>
 * The class turns the slots of a machine into the layout of a container: the roles of
 * {@link MachineInventory} decide where a slot is drawn - input and fuel in a column on
 * the left, the output on the right, with the room a progress picture needs between
 * them - and the inventory of the player follows below. A machine therefore never
 * mentions a pixel and a screen never mentions a machine.
 * <p>
 * A window only has to hand the {@link #container()} to
 * {@link com.philia093.neofactory.gui.container.ContainerView} and draw the progress on
 * top of it, so a new machine gets its screen for free.
 */
public final class MachineMenu {

    /** Upper edge of the first machine slot, below the frame of the panel. */
    private static final int TOP = ContainerLayout.PADDING + ContainerLayout.SLOT_PITCH;

    /** Columns between the input of a machine and its output. */
    private static final int OUTPUT_COLUMNS = 4;

    /** Amount of columns of the inventory of the player. */
    private static final int PLAYER_COLUMNS = 9;

    /** Amount of rows of the inventory of the player. */
    private static final int PLAYER_ROWS = 4;

    private final Machine machine;
    private final ContainerMenu container;

    /**
     * Creates the menu of a machine.
     *
     * @param machine machine to show
     * @param player inventory of the player, shown below the machine slots
     */
    public MachineMenu(Machine machine, PlayerInventory player) {
        this.machine = Objects.requireNonNull(machine, "machine");
        MachineInventory inventory = machine.inventory();
        ContainerLayout layout = new ContainerLayout();
        int left = ContainerLayout.PADDING + ContainerLayout.SLOT_PITCH;
        int row = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.role(slot) == MachineInventory.Role.OUTPUT) {
                layout.add(left + OUTPUT_COLUMNS * ContainerLayout.SLOT_PITCH, TOP, inventory, slot,
                        Slot.Rule.OUTPUT);
            } else {
                layout.add(left, TOP + row * ContainerLayout.SLOT_PITCH, inventory, slot,
                        Slot.Rule.NORMAL);
                row++;
            }
        }
        int playerY = TOP + (row + 1) * ContainerLayout.SLOT_PITCH;
        layout.addGrid(ContainerLayout.PADDING, playerY, PLAYER_COLUMNS, PLAYER_ROWS, player, 0,
                Slot.Rule.NORMAL);
        this.container = new ContainerMenu(layout, player);
    }

    /** Machine this menu shows. */
    public Machine machine() {
        return machine;
    }

    /** Container holding the slots of the machine and of the player. */
    public ContainerMenu container() {
        return container;
    }

    /**
     * Share of the work of the machine that is done.
     *
     * @return a value between {@code 0} and {@code 1}, {@code 0} for a machine that
     *         does not report a progress
     */
    public float craftProgress() {
        return machine instanceof ProgressMachine progress ? progress.craftProgress() : 0.0f;
    }

    /**
     * Share of the fuel that is left to burn.
     *
     * @return a value between {@code 0} and {@code 1}, {@code 0} for a machine that
     *         does not burn anything
     */
    public float burnProgress() {
        return machine instanceof ProgressMachine progress ? progress.burnProgress() : 0.0f;
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
