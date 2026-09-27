package com.philia093.neofactory.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.philia093.neofactory.blockentity.ContainerBlockEntity;
import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.ContainerMenu;
import com.philia093.neofactory.gui.container.ContainerView;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.item.Inventory;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.render.PixelFont;

/**
 * The screen of a block that holds a bag of items.
 * <p>
 * The class is what a chest and every later container is shown with: it takes the slots of the
 * block, lays them out with the rows of the player inventory below them and lets a click move
 * stacks around exactly like the inventory screen does, see
 * {@link com.philia093.neofactory.gui.container.ContainerMenu}. Nothing about it is written twice:
 * what it draws comes from {@link ContainerView}, the very other half the inventory and the screens
 * of the machines are built from, and how large its panel is comes from the layout it was handed.
 * <p>
 * <b>The name of the container is the only thing this screen adds.</b> The panel of a machine
 * carries room for a title of its own; a container is drawn with the plain panel of the interface,
 * so the name is written into the band the layout keeps free at the top, see
 * {@link ChestLayout#TITLE_HEIGHT}.
 * <p>
 * <b>One screen shows one container at a time.</b> Opening another one closes the one that is up,
 * and a container whose chunk is unloaded is closed by the world instead, see
 * {@code GameScreen#closeContainerIfGone()}.
 */
public final class ContainerGui {

    /** Pixels between the upper edge of the panel and the top of the name. */
    public static final int TITLE_TOP = 6;

    /**
     * Colour of the name of the container.
     * <p>
     * White with the shadow {@link PixelFont#drawShadowed} adds: the panel is a light grey, so the
     * dark edge of the shadow is what makes the text read.
     */
    private static final Color TEXT_COLOR = new Color(Color.WHITE);

    private final GuiViewport viewport;
    private final PixelFont font;
    private final ContainerView view;

    /** Menu of the container that is up, {@code null} while the screen is closed. */
    private ContainerMenu menu;

    /** Container the screen is up for, {@code null} while the screen is closed. */
    private ContainerBlockEntity container;

    /** Name written in the upper left corner of the panel. */
    private String title = "";

    /**
     * Sink the items of a work field are handed to while the screen closes.
     * <p>
     * A container of this game may hold a work field - the grid of a table - so the screen hands
     * the sink on to every menu it opens. Without one the items of a closed field would go back
     * into the inventory of the player instead of the ground, see
     * {@link ContainerMenu#setDropper(ContainerMenu.StackDropper)}.
     */
    private ContainerMenu.StackDropper dropper;

    /**
     * Creates the screen.
     *
     * @param textures texture cache providing the panel and the item icons
     * @param font font used for the amounts, the name of an item and the title
     * @param viewport viewport of the interface, see {@link GuiViewport}
     */
    public ContainerGui(BlockTextureCache textures, PixelFont font, GuiViewport viewport) {
        this.viewport = viewport;
        this.font = font;
        this.view = new ContainerView(textures, font, viewport);
    }

    /** {@code true} while the screen covers the world. */
    public boolean isOpen() {
        return menu != null && menu.isOpen();
    }

    /** Container the screen shows right now, {@code null} while the screen is closed. */
    public ContainerBlockEntity container() {
        return container;
    }

    /** Name written at the top of the panel, empty while the screen is closed. */
    public String title() {
        return title;
    }

    /**
     * Menu behind the screen.
     *
     * @return the menu, or {@code null} while the screen is closed
     */
    public ContainerMenu menu() {
        return menu;
    }

    /** {@code true} when the pictures of the panel could be loaded. */
    public boolean isComplete() {
        return view.isComplete();
    }

    /**
     * Sets the sink the items of a work field are handed to.
     *
     * @param dropper sink to use, {@code null} to hand the items back to the player
     */
    public void setDropper(ContainerMenu.StackDropper dropper) {
        this.dropper = dropper;
        if (menu != null) {
            menu.setDropper(dropper);
        }
    }

    /**
     * Opens the screen on the slots of a container.
     * <p>
     * The layout is built by the caller, because only the caller knows what the block holds: a
     * chest brings three rows and the table of the workshop a field of three by three with a result
     * beside it, see {@link ChestLayout}.
     *
     * @param container container the screen shows, never {@code null}
     * @param layout layout of its slots
     * @param playerInventory inventory of the player, the side a stack is moved to
     * @param title name written at the top of the panel
     */
    public void open(ContainerBlockEntity container, ContainerLayout layout,
            Inventory playerInventory, String title) {
        close();
        this.container = container;
        this.title = title;
        this.menu = new ContainerMenu(layout, playerInventory);
        this.menu.setDropper(dropper);
        this.menu.open();
    }

    /**
     * Closes the screen.
     * <p>
     * A stack the mouse carries is put back into the inventory of the player, so nobody loses items
     * by closing a container, see {@link ContainerMenu#close()}.
     */
    public void close() {
        if (menu != null) {
            menu.close();
        }
        menu = null;
        container = null;
        title = "";
    }

    /** X coordinate of the left edge of the panel, centred in the interface. */
    public float panelX() {
        return MenuLayout.centeredX(viewport.guiWidth(), menu.layout().panelWidth());
    }

    /** Y coordinate of the lower edge of the panel, centred in the interface. */
    public float panelY() {
        return Math.round((viewport.guiHeight() - menu.layout().panelHeight()) * 0.5f);
    }

    /** Width of the panel in virtual pixels, as large as its slots ask for. */
    public int panelWidth() {
        return menu.layout().panelWidth();
    }

    /** Height of the panel in virtual pixels, as large as its slots ask for. */
    public int panelHeight() {
        return menu.layout().panelHeight();
    }

    /** Stack the mouse currently carries. */
    public ItemStack cursorStack() {
        return menu == null ? ItemStack.EMPTY : menu.cursorStack();
    }

    /**
     * Slot under the mouse.
     * <p>
     * Read by the screen of a recipe that is built later on: what a player points at is what it
     * asks for.
     *
     * @param guiX X coordinate of the mouse inside the interface
     * @param guiY Y coordinate of the mouse inside the interface, from the bottom
     * @return the slot, or {@code null} while the screen is closed or the mouse is beside it
     */
    public Slot slotUnderMouse(float guiX, float guiY) {
        if (!isOpen()) {
            return null;
        }
        return menu.slotAt(localX(guiX), localY(guiY));
    }

    /**
     * Handles a mouse button press.
     * <p>
     * The coordinates are virtual pixels of the interface, the very same ones the screen is drawn
     * in, so a click always lands on the slot the player sees. A press beside the panel throws the
     * carried stack away instead of doing nothing.
     *
     * @param guiX X coordinate of the mouse inside the interface
     * @param guiY Y coordinate of the mouse inside the interface, from the bottom
     * @param button mouse button that was pressed
     * @return {@code true} when the press was consumed
     */
    public boolean touchDown(float guiX, float guiY, int button) {
        if (!isOpen()) {
            return false;
        }
        if (!isOnPanel(guiX, guiY)) {
            menu.dropCursor();
            return true;
        }
        menu.touchDown(localX(guiX), localY(guiY), button, isShiftHeld());
        return true;
    }

    /**
     * Handles the mouse moving while a button is held.
     *
     * @param guiX X coordinate of the mouse inside the interface
     * @param guiY Y coordinate of the mouse inside the interface, from the bottom
     */
    public void touchDragged(float guiX, float guiY) {
        if (isOpen()) {
            menu.touchDragged(localX(guiX), localY(guiY));
        }
    }

    /**
     * Handles the release of a mouse button.
     *
     * @param guiX X coordinate of the mouse inside the interface
     * @param guiY Y coordinate of the mouse inside the interface, from the bottom
     * @param button mouse button that was released
     * @return {@code true} when the release was consumed
     */
    public boolean touchUp(float guiX, float guiY, int button) {
        if (!isOpen()) {
            return false;
        }
        if (!isOnPanel(guiX, guiY)) {
            return menu.dropDragRemainder();
        }
        return menu.touchUp(localX(guiX), localY(guiY), button, isShiftHeld());
    }

    /**
     * Drops what the mouse points at, the action of the drop key while the screen is open.
     *
     * @param guiX X coordinate of the mouse inside the interface
     * @param guiY Y coordinate of the mouse inside the interface, from the bottom
     * @param wholeStack {@code true} to drop the whole stack, {@code false} for one item
     * @return {@code true} when something was dropped
     */
    public boolean dropAt(float guiX, float guiY, boolean wholeStack) {
        if (!isOpen()) {
            return false;
        }
        if (!isOnPanel(guiX, guiY)) {
            return menu.dropCursor();
        }
        return menu.dropFrom(menu.slotAt(localX(guiX), localY(guiY)), wholeStack);
    }

    /**
     * Draws the panel, its slots, the items in them and the name of the container.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     */
    public void render(SpriteBatch batch, float mouseX, float mouseY) {
        if (!isOpen()) {
            return;
        }
        // The name is a decoration of the container: it is drawn together with the slots and below
        // the items, so it never covers the stack a player points at.
        view.render(batch, menu, panelX(), panelY(), mouseX, mouseY, this::drawTitle);
    }

    /** Writes the name of the container into the band the layout keeps free. */
    private void drawTitle(SpriteBatch batch, float panelX, float panelY, int panelWidth,
            int panelHeight) {
        if (title.isEmpty()) {
            return;
        }
        float y = panelY + panelHeight - TITLE_TOP - font.lineHeight();
        font.setColor(TEXT_COLOR);
        font.drawShadowed(batch, title, panelX + ContainerLayout.PADDING, y);
        font.setColor(Color.WHITE);
    }

    /** {@code true} when a point of the interface lies on the panel of the container. */
    private boolean isOnPanel(float guiX, float guiY) {
        return menu.layout().contains(localX(guiX), localY(guiY));
    }

    /** X coordinate of the mouse inside the panel. */
    private int localX(float guiX) {
        return Math.round(guiX) - Math.round(panelX());
    }

    /** Y coordinate of the mouse inside the panel, measured from its upper edge. */
    private int localY(float guiY) {
        return Math.round(panelY() + menu.layout().panelHeight() - guiY);
    }

    /** {@code true} while a key that moves a stack to the other side is held. */
    private static boolean isShiftHeld() {
        return Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
    }

    @Override
    public String toString() {
        return "ContainerGui(" + (isOpen()
                ? title + ", " + menu.layout().size() + " slots" : "closed") + ")";
    }
}
