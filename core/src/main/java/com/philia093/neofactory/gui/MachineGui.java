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
import com.philia093.neofactory.gui.panel.ArrowElement;
import com.philia093.neofactory.gui.panel.MachineTextures;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.machine.Machine;
import com.philia093.neofactory.machine.MachineMenu;
import com.philia093.neofactory.machine.ProgressKind;
import com.philia093.neofactory.machine.SlotKind;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.render.PixelFont;

/**
 * The screen of a machine.
 * <p>
 * It shows the slots of one machine with the inventory of the player below them and the
 * progress of the work between the two columns of slots. The panel, the slots and the
 * icons come from {@code gui/machine_icons.png}, see {@link MachineTextures}: its panel is
 * empty where a machine stands its own slots and carries the slots of the player inventory
 * in its lower half.
 * <p>
 * What a machine burns is written next to it instead of being drawn as a flame - the game
 * has no fire in its screens - so the screen writes the seconds of fuel that are left, see
 * {@link com.philia093.neofactory.machine.FuelMachine}.
 * <p>
 * The screen is opened by using a block, see
 * {@link com.philia093.neofactory.screen.GameScreen}, and it takes the input while it is
 * up: clicking a slot moves stacks around exactly like it does in the inventory, see
 * {@link com.philia093.neofactory.gui.container.ContainerMenu}.
 */
public final class MachineGui {

    /**
     * Colour of the two lines of text in the upper corners.
     * <p>
     * White with the shadow {@link PixelFont#drawShadowed} adds: the panel is a light grey,
     * so the dark edge of the shadow is what makes the text read. A dark grey of its own was
     * tried first and turned out muddy.
     */
    private static final Color TEXT_COLOR = new Color(Color.WHITE);

    /** Pixels between the error icon and the status line next to it. */
    private static final int ERROR_GAP = 2;

    private final BlockTextureCache textures;
    private final GuiViewport viewport;
    private final PixelFont font;
    private final ContainerView view;

    /** Pictures of a machine screen, drawn instead of the standard panel. */
    private final MachineTextures panel;

    /** The bar of every kind of progress, cut once when the screen is created. */
    private final ArrowElement[] arrows;

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
        this.panel = new MachineTextures(textures);
        this.view = new ContainerView(textures, font, viewport);
        // A machine is drawn from its own panel, which is empty where its slots stand.
        this.view.setAppearance(panel);
        this.arrows = new ArrowElement[ProgressKind.values().length];
        for (ProgressKind kind : ProgressKind.values()) {
            arrows[kind.ordinal()] = panel.arrows(kind);
        }
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
        view.render(batch, menu.container(), panelX(), panelY(), mouseX, mouseY, this::drawMachine);
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
        drawTitle(batch, panelX, panelY, panelHeight);
        drawStatus(batch, panelX, panelY, panelHeight);
        drawProgress(batch, panelX, panelY, panelHeight);
        drawFluidSlots(batch, panelX, panelY, panelHeight);
    }

    /** Writes the name of the machine in the upper left corner of the panel. */
    private void drawTitle(SpriteBatch batch, float panelX, float panelY, int panelHeight) {
        font.setColor(TEXT_COLOR);
        font.drawShadowed(batch, menu.title(), panelX + MachineMenu.TEXT_LEFT,
                lineY(panelY, panelHeight));
        font.setColor(Color.WHITE);
    }

    /**
     * Writes what the machine reports in the upper right corner of the panel.
     * <p>
     * The icon of an error stands next to the text, so a machine that waits for energy says
     * why it waits, see {@link com.philia093.neofactory.machine.MachineError}.
     */
    private void drawStatus(SpriteBatch batch, float panelX, float panelY, int panelHeight) {
        float lineY = lineY(panelY, panelHeight);
        float x = panelX + menu.statusRight();
        String status = menu.statusText();
        if (!status.isEmpty()) {
            x -= font.width(status);
            font.setColor(TEXT_COLOR);
            font.drawShadowed(batch, status, x, lineY);
            font.setColor(Color.WHITE);
            x -= ERROR_GAP;
        }
        TextureRegion error = panel.icon(menu.error());
        if (error != null) {
            batch.draw(error, x - MachineTextures.ICON_CELL,
                    lineY + font.lineHeight() - MachineTextures.ICON_CELL);
        }
    }

    /** Draws the bar that shows how far the work of the machine has come. */
    private void drawProgress(SpriteBatch batch, float panelX, float panelY, int panelHeight) {
        ArrowElement bar = arrows[menu.progressKind().ordinal()];
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
     * Fills the lower part of a tank with the fluid it holds, the way a bucket would show it.
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

    /** Y of the upper edge of a line of text in one of the upper corners. */
    private float lineY(float panelY, int panelHeight) {
        return panelY + panelHeight - MachineMenu.TEXT_TOP - font.lineHeight();
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
