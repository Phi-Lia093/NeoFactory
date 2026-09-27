package com.philia093.neofactory.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.fluid.Fluid;
import com.philia093.neofactory.fluid.FluidStorage;
import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.ContainerView;
import com.philia093.neofactory.gui.container.ItemTooltip;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.gui.panel.ArrowElement;
import com.philia093.neofactory.gui.panel.MachineTextures;
import com.philia093.neofactory.gui.panel.NeiTextures;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.machine.EnergyStorage;
import com.philia093.neofactory.machine.Machine;
import com.philia093.neofactory.machine.MachineMenu;
import com.philia093.neofactory.machine.MachineStyle;
import com.philia093.neofactory.machine.ProgressKind;
import com.philia093.neofactory.machine.SlotKind;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.render.PixelFont;

import java.util.List;

/**
 * The screen of a machine.
 * <p>
 * It shows the slots of one machine with the inventory of the player below them and the
 * progress of the work between the two columns of slots. The panel, the slots and the
 * icons come from {@code gui/machine_icons.png}, see {@link MachineTextures}: its panel is
 * empty where a machine stands its own slots and carries the slots of the player inventory
 * in its lower half.
 * <p>
 * <b>The screen writes no word of its own.</b> The name of the machine stands behind the
 * mark of the upper left corner and what its fire reports stands behind the flame under the
 * slot of a fuel, both as a tooltip while the mouse rests on them, so a screen of every
 * machine looks the same whatever a machine has to say.
 * <p>
 * What a machine burns is written next to it instead of being drawn as a flame - the game
 * has no fire in its screens - so the screen writes the seconds of fuel that are left, see
 * {@link com.philia093.neofactory.machine.FuelMachine}.
 * <p>
 * The tanks at the foot of the panel are not slots: they are described by a tooltip of their
 * own while the mouse rests on one - what fluid lies in it, how much of it and how much the
 * tank takes - and a click on one trades a cell with the tank, see
 * {@link com.philia093.neofactory.item.CellTransfer}.
 * <p>
 * The screen is opened by using a block, see
 * {@link com.philia093.neofactory.screen.GameScreen}, and it takes the input while it is
 * up: clicking a slot moves stacks around exactly like it does in the inventory, see
 * {@link com.philia093.neofactory.gui.container.ContainerMenu}.
 */
public final class MachineGui {

    /** Colour the cell of energy is filled with, the red of the bars of the age of power. */
    private static final Color ENERGY_COLOR = new Color(0.72f, 0.12f, 0.12f, 1.0f);

    /** Seconds of one blink of a flame that is over its top, half a hertz. */
    private static final float BLINK_PERIOD = 2.0f;

    /** Seconds this screen has been open, which is what the blink of a flame is timed by. */
    private float elapsed;

    /** Pixels between the mark of the pack and the mark beside it. */

    private final BlockTextureCache textures;
    private final GuiViewport viewport;
    private final PixelFont font;
    private final ContainerView view;

    /**
     * Pictures of a machine screen, one set per style of panel.
     * <p>
     * The sheet carries a panel per age - the grey one and the bronze one - so the screen cuts all of them
     * once and draws the machine that is up with its own, see {@link MachineStyle}.
     */
    private final MachineTextures[] panels;

    /** The set of pictures of the machine that is up, the one of its style. */
    private MachineTextures panel;

    /** The bar of every kind of progress, one row per style, cut once when the screen is created. */
    private final ArrowElement[][] arrows;

    /** Menu of the machine that is up, {@code null} while the screen is closed. */
    private MachineMenu menu;

    /**
     * Creates the screen.
     *
     * @param textures texture cache providing the panel, the slots and the icons
     * @param font font used for the name of an item and for the two lines of text
     * @param viewport viewport of the interface, see {@link GuiViewport}
     */
    public MachineGui(BlockTextureCache textures, PixelFont font, GuiViewport viewport) {
        this.textures = textures;
        this.viewport = viewport;
        this.font = font;
        this.panels = new MachineTextures[MachineStyle.values().length];
        for (MachineStyle style : MachineStyle.values()) {
            panels[style.ordinal()] = new MachineTextures(textures, style);
        }
        this.panel = panels[MachineStyle.NORMAL.ordinal()];
        this.arrows = new ArrowElement[MachineStyle.values().length][ProgressKind.values().length];
        for (MachineStyle style : MachineStyle.values()) {
            for (ProgressKind kind : ProgressKind.values()) {
                arrows[style.ordinal()][kind.ordinal()] = panels[style.ordinal()].arrows(kind);
            }
        }
        this.view = new ContainerView(textures, font, viewport);
    }

    /** {@code true} while the screen covers the world. */
    public boolean isOpen() {
        return menu != null && menu.container().isOpen();
    }

    /**
     * Machine the screen shows right now.
     *
     * @return the machine, or {@code null} while the screen is closed
     */
    public Machine machine() {
        return menu == null ? null : menu.machine();
    }

    /** Menu of the machine that is up, {@code null} while the screen is closed. */
    public MachineMenu menu() {
        return menu;
    }

    /**
     * Opens the screen on a machine.
     * <p>
     * The menu is built for the machine that was used, so the screen always shows the
     * slots of the block the player stands at.
     *
     * @param machine machine to show
     * @param player inventory of the player, shown below the machine
     */
    public void open(Machine machine, PlayerInventory player) {
        menu = new MachineMenu(machine, player);
        menu.container().open();
        // A machine is drawn with the panel of its own age, which is empty where its slots stand.
        panel = panels[menu.style().ordinal()];
        view.setAppearance(panel);
    }

    /**
     * Closes the screen.
     * <p>
     * A stack the mouse carries is put back into the inventory, so no item is ever lost by
     * closing the screen, see
     * {@link com.philia093.neofactory.gui.container.ContainerMenu#close()}.
     */
    public void close() {
        if (menu != null) {
            menu.container().close();
        }
    }

    /** {@code true} when the pictures of a machine screen could be loaded. */
    public boolean isComplete() {
        return panel.isComplete() && view.isComplete();
    }

    /**
     * Forwards a click to the container of the machine.
     *
     * @param guiX X coordinate of the mouse inside the interface
     * @param guiY Y coordinate of the mouse inside the interface, from the bottom
     * @param button mouse button that was pressed
     * @return {@code true} when the click was used by the container
     */
    public boolean touchDown(float guiX, float guiY, int button) {
        if (!isOpen()) {
            return false;
        }
        return menu.container().touchDown(localX(guiX), localY(guiY), button, isShiftHeld());
    }

    /**
     * Forwards the release of a button to the container of the machine.
     *
     * @param guiX X coordinate of the mouse inside the interface
     * @param guiY Y coordinate of the mouse inside the interface, from the bottom
     * @param button mouse button that was released
     * @return {@code true} when the container used it
     */
    public boolean touchUp(float guiX, float guiY, int button) {
        if (!isOpen()) {
            return false;
        }
        return menu.container().touchUp(localX(guiX), localY(guiY), button, isShiftHeld());
    }

    /**
     * Forwards a drag to the container of the machine, which shares a stack out over the
     * slots it is dragged across.
     *
     * @param guiX X coordinate of the mouse inside the interface
     * @param guiY Y coordinate of the mouse inside the interface, from the bottom
     */
    public void touchDragged(float guiX, float guiY) {
        if (isOpen()) {
            menu.container().touchDragged(localX(guiX), localY(guiY));
        }
    }

    /**
     * Draws the panel, its slots, the items in them, the progress of the work and the line
     * that reports what the machine burns.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     */
    public void render(SpriteBatch batch, float mouseX, float mouseY) {
        if (!isOpen()) {
            return;
        }
        // The arrow and the fuel line are decorations of the container: they are drawn with
        // the slots and below the items, so they never cover the name of an item.
        elapsed += Gdx.graphics.getDeltaTime();
        view.render(batch, menu.container(), panelX(), panelY(), mouseX, mouseY, this::drawMachine);
        drawTankTooltip(batch, mouseX, mouseY);
        drawMarkTooltips(batch, mouseX, mouseY);
    }

    /**
     * Draws what the tank under the mouse holds, beside the mouse.
     * <p>
     * A tank is not a slot and has no item to name, so the box is not what {@link ContainerView} draws: it
     * names the fluid and says how much of it lies in the tank and how much room is left, see
     * {@link MachineMenu#tankTooltip}. It is drawn last, so it lies over everything else the screen shows.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     */
    private void drawTankTooltip(SpriteBatch batch, float mouseX, float mouseY) {
        MachineMenu.FluidSlot tank = menu.fluidSlotAt(localX(mouseX), localY(mouseY));
        if (tank == null) {
            return;
        }
        ItemTooltip.draw(batch, font, textures.whitePixel(), menu.tankTooltip(tank), mouseX, mouseY,
                viewport.guiWidth(), viewport.guiHeight());
    }

    /** X coordinate of the left edge of the panel, centred in the interface. */
    public float panelX() {
        return MenuLayout.centeredX(viewport.guiWidth(), menu.container().layout().panelWidth());
    }

    /** Y coordinate of the lower edge of the panel, centred in the interface. */
    public float panelY() {
        return Math.round((viewport.guiHeight() - menu.container().layout().panelHeight()) * 0.5f);
    }

    /** Width of the panel in virtual pixels. */
    public int panelWidth() {
        return menu.container().layout().panelWidth();
    }

    /** Height of the panel in virtual pixels, as large as its slots ask for. */
    public int panelHeight() {
        return menu.container().layout().panelHeight();
    }

    /**
     * Draws what is particular about a machine: how far its work has come and what it
     * burns.
     * <p>
     * The arrow fills with the progress of the craft and the line below the input column
     * writes the seconds of fuel that are left. The game draws no flame, so a machine that
     * is out of fuel says so in words.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param panelX left edge of the panel in interface pixels
     * @param panelY lower edge of the panel in interface pixels
     * @param panelWidth width of the panel in pixels
     * @param panelHeight height of the panel in pixels
     */
    private void drawMachine(SpriteBatch batch, float panelX, float panelY, int panelWidth,
            int panelHeight) {
        drawProgress(batch, panelX, panelY, panelHeight);
        drawFluidSlots(batch, panelX, panelY, panelHeight);
        drawEnergy(batch, panelX, panelY, panelHeight);
        drawFlame(batch, panelX, panelY, panelHeight);
        drawMarks(batch, panelX, panelY, panelHeight);
    }

    /** Writes the name of the machine on the label in the upper left corner of the panel. */
    private void drawMarks(SpriteBatch batch, float panelX, float panelY, int panelHeight) {
        // Nothing of the state of a machine is written in words any more: the mark of the pack stands in the
        // upper left corner and names the machine while the mouse rests on it, and the mark of what is wrong
        // with the machine stands beside it, see MachineMenu#infoTooltip and #flameTooltip.
        float top = panelY + panelHeight - MachineMenu.INFO_TOP - MachineMenu.INFO_SIZE;
        TextureRegion info = textures.region(NeiTextures.INFO);
        if (info != null) {
            batch.draw(info, panelX + MachineMenu.INFO_LEFT, top, MachineMenu.INFO_SIZE,
                    MachineMenu.INFO_SIZE);
        }
        TextureRegion error = panel.icon(menu.error());
        if (error != null) {
            batch.draw(error, panelX + MachineMenu.INFO_LEFT + MachineMenu.INFO_SIZE
                    + MachineMenu.MARK_GAP, top, MachineMenu.INFO_SIZE, MachineMenu.INFO_SIZE);
        }
    }

    /**
     * Draws the flame under the slot a machine burns in.
     * <p>
     * A furnace shows what is left of the item it burns and a boiler how hot it is; both are a share between
     * nothing and everything, and the flame is drawn as the part of its picture the share fills, from the
     * bottom up, the way a tank shows the fluid it holds. A boiler over its boiling point blinks at half a
     * hertz, which is what tells a player that the water is gone and the machine is being ruined.
     */
    private void drawFlame(SpriteBatch batch, float panelX, float panelY, int panelHeight) {
        if (!menu.hasFlame() || !isBlinkVisible()) {
            return;
        }
        Slot fuel = menu.fuelCell();
        int size = MachineTextures.ICON_CELL;
        float x = panelX + flameX(fuel);
        float y = panelY + panelHeight - flameTop(fuel) - size;
        if (!menu.flameIsLit() && menu.flameIsTemperature() && menu.flameShare() <= 0.0f) {
            // The flame of a boiler is its temperature and a cold boiler has no fire at all: the picture of a
            // flame that is out is what a machine that burns an item shows when its fire went out, and a
            // boiler that is at the temperature of the room shows nothing.
            return;
        }
        int row = menu.flameIsLit() ? menu.flameRow() : menu.flameOutRow();
        TextureRegion flame = panel.icon(menu.flameColumn(), row);
        if (flame == null) {
            return;
        }
        if (!menu.flameIsLit()) {
            batch.draw(flame, x, y, size, size);
            return;
        }
        // The part that is filled is cut out of the picture from its lower edge, like the bright part of a
        // progress bar is cut out of the track.
        int full = flame.getRegionHeight();
        int filled = Math.max(1, Math.round(full * menu.flameShare()));
        flame.setRegionHeight(filled);
        flame.setRegionY(flame.getRegionY() + full - filled);
        batch.draw(flame, x, y, size, filled);
        flame.setRegionY(flame.getRegionY() - full + filled);
        flame.setRegionHeight(full);
    }

    /** {@code true} while a flame is in the half of its blink that shows. */
    private boolean isBlinkVisible() {
        return !menu.flameBlinks() || elapsed % BLINK_PERIOD < BLINK_PERIOD * 0.5f;
    }

    /**
     * Draws what the mark under the mouse names.
     * <p>
     * Nothing of the screen is written in words, so the two marks are what a player asks: the mark of the
     * upper left corner names the machine and the flame says what is left of the fire or how hot a boiler is,
     * see {@link MachineMenu#infoTooltip} and {@link MachineMenu#flameTooltip}.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     */
    private void drawMarkTooltips(SpriteBatch batch, float mouseX, float mouseY) {
        int localX = localX(mouseX);
        int localY = localY(mouseY);
        List<String> lines;
        if (isOnInfo(localX, localY)) {
            lines = menu.infoTooltip();
        } else if (menu.hasFlame() && isOnFlame(localX, localY)) {
            lines = menu.flameTooltip();
        } else {
            return;
        }
        if (lines.isEmpty()) {
            return;
        }
        ItemTooltip.draw(batch, font, textures.whitePixel(), lines, mouseX, mouseY, viewport.guiWidth(),
                viewport.guiHeight());
    }

    /** {@code true} while a point of the panel lies on the mark that names the machine. */
    private static boolean isOnInfo(int localX, int localY) {
        return localX >= MachineMenu.INFO_LEFT && localX < MachineMenu.INFO_LEFT + MachineMenu.INFO_SIZE
                && localY >= MachineMenu.INFO_TOP && localY < MachineMenu.INFO_TOP + MachineMenu.INFO_SIZE;
    }

    /** {@code true} while a point of the panel lies on the flame of the machine, if it has one. */
    private boolean isOnFlame(int localX, int localY) {
        if (!menu.hasFlame()) {
            return false;
        }
        Slot fuel = menu.fuelCell();
        int top = flameTop(fuel);
        return localX >= flameX(fuel) && localX < flameX(fuel) + MachineTextures.ICON_CELL
                && localY >= top && localY < top + MachineTextures.ICON_CELL;
    }

    /** X of the flame of a machine, which stands one cell left of the slot it belongs to. */
    private static float flameX(Slot fuel) {
        return fuel.x() - MachineMenu.FLAME_LEFT_CELLS * MachineTextures.ICON_CELL;
    }

    /** Row of the upper edge of the flame of a machine, which stands two cells above that slot. */
    private static int flameTop(Slot fuel) {
        return fuel.y() + ContainerLayout.SLOT_SIZE + MachineMenu.FLAME_GAP
                - MachineMenu.FLAME_UP_CELLS * MachineTextures.ICON_CELL;
    }

    /**
     * Slot under the mouse.
     * <p>
     * Read by the screen of recipes: what a player points at is the item it asks about.
     *
     * @param guiX X coordinate of the mouse inside the interface
     * @param guiY Y coordinate of the mouse inside the interface, from the bottom
     * @return the slot, or {@code null} while the screen is closed or the mouse is beside it
     */
    public Slot slotUnderMouse(float guiX, float guiY) {
        if (!isOpen()) {
            return null;
        }
        return menu.container().slotAt(localX(guiX), localY(guiY));
    }

    /** Draws the bar that shows how far the work of the machine has come. */
    private void drawProgress(SpriteBatch batch, float panelX, float panelY, int panelHeight) {
        ArrowElement bar = arrows[menu.style().ordinal()][menu.progressKind().ordinal()];
        bar.draw(batch, panelX + MachineMenu.ARROW_X,
                panelY + panelHeight - MachineMenu.ARROW_Y - bar.height(), menu.craftProgress());
    }

    /** Draws the tanks at the foot of the panel, with the fluid they hold. */
    private void drawFluidSlots(SpriteBatch batch, float panelX, float panelY, int panelHeight) {
        for (MachineMenu.FluidSlot slot : menu.fluidSlots()) {
            float x = panelX + slot.x();
            float y = panelY + panelHeight - slot.y() - ContainerLayout.SLOT_SIZE;
            TextureRegion picture = panel.icon(
                    slot.input() ? SlotKind.FLUID_INPUT : SlotKind.FLUID_OUTPUT);
            if (picture != null) {
                batch.draw(picture, x - PanelTextures.SLOT_BEVEL, y - PanelTextures.SLOT_BEVEL,
                        MachineTextures.ICON_CELL, MachineTextures.ICON_CELL);
            }
            drawFluid(batch, x, y, menu.machine().tank(slot.tank()).storage());
        }
    }

    /**
     * Draws the cell of energy at the foot of the panel, between the two pairs of tanks.
     * <p>
     * The cell is not a slot: no player ever puts anything into it. It shows how much of the buffer of the
     * machine is filled, the way a tank shows the fluid it holds, so a machine that waits for power says so
     * on the same row it waits on. A machine of the age of steam has no buffer and shows an empty cell.
     */
    private void drawEnergy(SpriteBatch batch, float panelX, float panelY, int panelHeight) {
        float x = panelX + MachineMenu.ENERGY_X;
        float y = panelY + panelHeight - MachineMenu.FOOT_TOP - ContainerLayout.SLOT_SIZE;
        TextureRegion cell = panel.icon(SlotKind.GENERIC);
        if (cell != null) {
            batch.draw(cell, x - PanelTextures.SLOT_BEVEL, y - PanelTextures.SLOT_BEVEL,
                    MachineTextures.ICON_CELL, MachineTextures.ICON_CELL);
        }
        EnergyStorage buffer = menu.machine().energy();
        TextureRegion pixel = textures.whitePixel();
        if (pixel == null || buffer == null || buffer.capacity() <= 0 || buffer.amount() <= 0) {
            return;
        }
        float inside = ContainerLayout.SLOT_SIZE - 2;
        float filled = inside * Math.min(1.0f, (float) buffer.amount() / buffer.capacity());
        batch.setColor(ENERGY_COLOR);
        batch.draw(pixel, x + 1, y + 1, inside, filled);
        batch.setColor(Color.WHITE);
    }

    /**
     * Fills the lower part of a tank with the fluid it holds, the way a full cell shows it.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param x left edge of the cell of the tank
     * @param y lower edge of that cell
     * @param tank tank to show, may be {@code null}
     */
    private void drawFluid(SpriteBatch batch, float x, float y, FluidStorage tank) {
        TextureRegion pixel = textures.whitePixel();
        if (pixel == null || tank == null || tank.isEmpty() || tank.capacity() <= 0) {
            return;
        }
        float inside = ContainerLayout.SLOT_SIZE - 2;
        float filled = inside * Math.min(1.0f, (float) tank.amount() / tank.capacity());
        batch.setColor(tank.fluid().color());
        batch.draw(pixel, x + 1, y + 1, inside, filled);
        batch.setColor(Color.WHITE);
    }

    /** X coordinate of the mouse inside the panel. */
    private int localX(float guiX) {
        return Math.round(guiX) - Math.round(panelX());
    }

    /** Y coordinate of the mouse inside the panel, measured from its upper edge. */
    private int localY(float guiY) {
        return Math.round(panelY() + panelHeight() - guiY);
    }

    /** {@code true} while a key that moves a stack to the other side is held. */
    private static boolean isShiftHeld() {
        return Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
    }
}
