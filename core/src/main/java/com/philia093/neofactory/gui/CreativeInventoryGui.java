package com.philia093.neofactory.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.gui.container.ContainerLayout;
import com.philia093.neofactory.gui.container.ContainerMenu;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.gui.creative.CreativeInventory;
import com.philia093.neofactory.gui.creative.CreativeLayout;
import com.philia093.neofactory.gui.creative.CreativeTab;
import com.philia093.neofactory.gui.creative.CreativeTextures;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.render.PixelFont;
import com.philia093.neofactory.util.Constants;

/**
 * The creative inventory: a grid of every item of the game, its tabs and a search box.
 * <p>
 * The screen is the creative mode twin of {@link InventoryGui}: the player picks an
 * item out of the grid instead of taking what is already owned, and the grid never runs
 * empty - a stack that was taken is there again right away, see
 * {@link CreativeInventory#sourceAt(int)}. The tabs stand above the panel, the wheel
 * scrolls the list and the search box filters it by name; an empty box lists everything.
 * Every tab carries the icon of its group, drawn with the item itself because the tabs of
 * the art are empty shapes.
 * <p>
 * The panel, the tabs and the scroll bar come from {@code gui/inventory_icons.png} and
 * are drawn at the places {@link CreativeLayout} names, which is also where a click is
 * measured, so the drawn slots and the slots a click can hit are always the same. The
 * items are drawn by {@link GuiItemRenderer}, so a block shows the same small cube here
 * as it does in the hotbar. The scroll bar comes in the two shapes of the original game,
 * one for a list that fits into the grid and one for a list that goes on.
 * <p>
 * Every slot of the grid hands items out, so a stack that is put into one has no place to
 * stay and is destroyed instead, see {@link ContainerMenu#setVoidsOverflow(boolean)}. A
 * stack that is dragged out of the panel lands on the ground like in any other container,
 * which is the one way a creative player gets rid of something for good.
 * <p>
 * The last tab hands over to the inventory of the player: instead of a list of items it
 * shows the very screen {@link InventoryGui} draws, so a player keeps the crafting field
 * while being in creative mode. {@link #showsPlayerInventory()} tells the two apart, and
 * only one of the two panels is ever up.
 * <p>
 * All coordinates are virtual pixels of {@link GuiViewport}.
 */
public final class CreativeInventoryGui {

    /** Distance between the mouse and the nearest corner of a tooltip. */
    private static final int TOOLTIP_OFFSET = 12;

    /** Pixels of background between the frame of a tooltip and its text. */
    private static final int TOOLTIP_PADDING = 3;

    /** Colour of a tooltip background, shared and never mutated. */
    private static final Color TOOLTIP_FILL = new Color(0.062f, 0.0f, 0.062f, 0.94f);

    /** Colour of the frame around a tooltip, shared and never mutated. */
    private static final Color TOOLTIP_FRAME = new Color(0.313f, 0.313f, 0.0f, 1.0f);

    /** Overlay drawn on the slot under the mouse, shared and never mutated. */
    private static final Color HOVER_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.5f);

    /** Colour of the text inside the search box. */
    private static final Color SEARCH_COLOR = new Color(1.0f, 1.0f, 1.0f, 1.0f);

    /** Seconds the cursor of the search box stays visible before it blinks again. */
    private static final float BLINK_INTERVAL = 0.5f;

    private final GuiViewport viewport;
    private final PixelFont font;
    private final GuiItemRenderer items;

    /** The art of the creative inventory, the panels, the tabs and the scroll bar. */
    private final CreativeTextures art;

    /** The plain panel, used when the art of the creative inventory is missing. */
    private final PanelTextures panels;

    /** A single white pixel, the source of the frames and the hover highlight. */
    private final TextureRegion pixel;

    private final PlayerInventory player;

    /** What the screen shows, see {@link CreativeInventory}. */
    private final CreativeInventory creative;

    /** The clicks of the grid and of the hotbar, see {@link ContainerMenu}. */
    private final ContainerMenu menu;

    /**
     * The screen of the player inventory.
     * <p>
     * The last tab hands the panel over to it instead of showing a grid of items, so a
     * creative player keeps the crafting field and handles their own items exactly the
     * way they do in survival mode.
     */
    private final InventoryGui playerScreen;

    /** {@code true} while the scroll bar is dragged. */
    private boolean draggingScroll;

    /** {@code true} while the search box owns the keyboard. */
    private boolean searchFocused;

    /** Seconds since the screen was drawn last, drives the blinking cursor. */
    private float elapsed;

    /**
     * Creates the screen.
     *
     * @param cache texture cache providing the panels, the tabs and the item icons
     * @param font font used for the search box, the amounts and the tooltips
     * @param player inventory of the player, the hotbar it shows
     * @param viewport viewport of the interface, see {@link GuiViewport}
     */
    public CreativeInventoryGui(BlockTextureCache cache, PixelFont font,
            PlayerInventory player, GuiViewport viewport) {
        this.viewport = viewport;
        this.font = font;
        this.player = player;
        this.items = new GuiItemRenderer(cache, font);
        this.art = new CreativeTextures(cache);
        this.panels = new PanelTextures(cache);
        this.pixel = cache.whitePixel();
        this.creative = new CreativeInventory();
        this.menu = new ContainerMenu(layout(), player);
        this.playerScreen = new InventoryGui(cache, font, player, viewport);
        // The grid only hands items out, so a slot that was emptied asks for the next
        // stack; this supply is endless, which is what a creative player owns.
        this.menu.setResultFiller(slot -> creative.sourceAt(slot.index()));
        // The grid owns every item of the game and fills itself again, so dropping a
        // stack into it means throwing it away instead of finding no room.
        this.menu.setVoidsOverflow(true);
    }

    /** The grid of items and the hotbar row, both inside the panel. */
    private ContainerLayout layout() {
        ContainerLayout grid = new ContainerLayout();
        grid.addGrid(CreativeLayout.GRID_X, CreativeLayout.GRID_Y, CreativeLayout.COLUMNS,
                CreativeLayout.ROWS, creative.grid(), 0, Slot.Rule.OUTPUT);
        grid.addGrid(CreativeLayout.GRID_X, CreativeLayout.HOTBAR_Y, CreativeLayout.HOTBAR_COLUMNS,
                1, player, 0, Slot.Rule.NORMAL);
        return grid;
    }

    /** {@code true} while the screen is up. */
    public boolean isOpen() {
        return menu.isOpen();
    }

    /** Opens the screen or closes it when it is already up. */
    public void toggle() {
        if (menu.isOpen()) {
            close();
        } else {
            open();
        }
    }

    /** Opens the screen on the tab that was shown last. */
    public void open() {
        menu.open();
        if (creative.showsPlayerInventory()) {
            playerScreen.open();
        }
    }

    /** Closes the screen and gives the carried stack to the player. */
    public void close() {
        menu.close();
        if (menu.isOpen()) {
            // The carried stack did not fit into a full inventory, so the screen stays up
            // - and the panel that belongs to the shown tab stays up with it.
            return;
        }
        playerScreen.close();
        draggingScroll = false;
        searchFocused = false;
    }

    /**
     * Sets the sink both screens hand a stack to when a click lands beside the panel.
     *
     * @param dropper sink to use, {@code null} to give the items back to the player
     */
    public void setDropper(ContainerMenu.StackDropper dropper) {
        menu.setDropper(dropper);
        playerScreen.setDropper(dropper);
    }

    /** Counts the time for the blinking cursor of the search box. */
    public void update(float delta) {
        elapsed += delta;
    }

    /** What the screen shows, the source of the grid and of the search. */
    public CreativeInventory inventory() {
        return creative;
    }

    /** {@code true} while the search box owns the keyboard. */
    public boolean isSearchFocused() {
        return searchFocused;
    }

    /** {@code true} while the last tab shows the inventory of the player instead. */
    public boolean showsPlayerInventory() {
        return creative.showsPlayerInventory();
    }

    /** X coordinate of the left edge of the panel that is shown. */
    public float panelX() {
        return creative.showsPlayerInventory()
                ? playerScreen.panelX()
                : CreativeLayout.panelX(viewport.guiWidth());
    }

    /** Y coordinate of the lower edge of the panel that is shown. */
    public float panelY() {
        return creative.showsPlayerInventory()
                ? playerScreen.panelY()
                : CreativeLayout.panelY(viewport.guiHeight());
    }

    /** Y coordinate of the upper edge of the panel that is shown. */
    private float panelTop() {
        return panelY() + panelHeight();
    }

    /** Height of the panel that is shown, the inventory of the player is the taller one. */
    private int panelHeight() {
        return creative.showsPlayerInventory()
                ? playerScreen.panelHeight()
                : CreativeLayout.PANEL_HEIGHT;
    }

    /** X coordinate of the mouse inside the panel. */
    private int localX(float guiX) {
        return Math.round(guiX) - Math.round(panelX());
    }

    /** Y coordinate of the mouse inside the panel, measured from its upper edge. */
    private int localY(float guiY) {
        return Math.round(panelTop() - guiY);
    }

    /**
     * Handles a click on the screen.
     * <p>
     * A click on a tab chooses it, a click on the search box takes the keyboard and a
     * click on the scroll bar moves the list; everything else goes to the container, so
     * the grid and the hotbar behave like every other container of the game.
     *
     * @param guiX X coordinate of the mouse inside the interface
     * @param guiY Y coordinate of the mouse inside the interface, from the bottom
     * @param button button that was pressed
     * @return {@code true} when the screen consumed the click
     */
    public boolean touchDown(float guiX, float guiY, int button) {
        if (!menu.isOpen()) {
            return false;
        }
        int x = localX(guiX);
        int y = localY(guiY);

        int tab = CreativeLayout.tabAt(x, y, creative.tabs().size());
        if (tab >= 0) {
            selectTab(tab);
            return true;
        }
        if (creative.showsPlayerInventory()) {
            // The panel belongs to the screen of the player inventory, so it handles its
            // own clicks, including a drag that ends beside the panel and drops a stack
            // into the world.
            return playerScreen.touchDown(guiX, guiY, button);
        }
        if (creative.isSearching() && CreativeLayout.isOnSearchBox(x, y)) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;
        if (button == 0 && creative.maxRow() > 0 && CreativeLayout.isOnScrollTrack(x, y)) {
            draggingScroll = true;
            creative.setFirstRow(CreativeLayout.rowAtScrollY(y, creative.maxRow()));
            return true;
        }
        menu.touchDown(x, y, button, isShiftHeld());
        return true;
    }

    /**
     * Handles the mouse moving while a button is held.
     *
     * @param guiX X coordinate of the mouse inside the interface
     * @param guiY Y coordinate of the mouse inside the interface, from the bottom
     */
    public void touchDragged(float guiX, float guiY) {
        if (!menu.isOpen()) {
            return;
        }
        if (draggingScroll) {
            // The thumb follows the mouse for as long as the button is held, see
            // CreativeLayout#rowAtScrollY.
            creative.setFirstRow(CreativeLayout.rowAtScrollY(localY(guiY), creative.maxRow()));
            return;
        }
        if (creative.showsPlayerInventory()) {
            playerScreen.touchDragged(guiX, guiY);
            return;
        }
        int x = localX(guiX);
        int y = localY(guiY);
        menu.touchDragged(x, y);
    }

    /**
     * Handles the end of a drag.
     *
     * @param guiX X coordinate of the mouse inside the interface
     * @param guiY Y coordinate of the mouse inside the interface, from the bottom
     * @param button button that was released
     * @return {@code true} when the screen consumed the click
     */
    public boolean touchUp(float guiX, float guiY, int button) {
        if (!menu.isOpen()) {
            return false;
        }
        if (draggingScroll) {
            // A drag of the thumb ends here and must not reach the slots below it.
            draggingScroll = false;
            return true;
        }
        if (creative.showsPlayerInventory()) {
            return playerScreen.touchUp(guiX, guiY, button);
        }
        return menu.touchUp(localX(guiX), localY(guiY), button, isShiftHeld());
    }

    /**
     * Scrolls the list of items by a whole row.
     *
     * @param amount notches of the wheel, positive when the wheel turns up, {@code 0} on
     *               a frame without a notch
     * @return {@code true} when the screen consumed the wheel
     */
    public boolean scrolled(float amount) {
        if (!menu.isOpen() || creative.showsPlayerInventory()) {
            return false;
        }
        creative.scrollBy(amount);
        return true;
    }

    /**
     * Chooses a tab and hands the panel over when the chosen tab asks for it.
     * <p>
     * The last tab shows the inventory of the player, so the screen that draws that
     * inventory is opened or closed together with it. Both screens are never open at the
     * same time, which is what keeps a click from reaching two panels at once.
     *
     * @param index index of the tab inside {@link CreativeInventory#tabs()}
     */
    private void selectTab(int index) {
        if (index < 0 || index >= creative.tabs().size()) {
            return;
        }
        creative.select(index);
        searchFocused = creative.isSearching();
        draggingScroll = false;
        if (creative.showsPlayerInventory()) {
            playerScreen.open();
        } else if (playerScreen.isOpen()) {
            playerScreen.close();
        }
    }

    /**
     * Handles a key that deletes inside the search box.
     *
     * @param keyCode key code, see {@link Input.Keys}
     * @return {@code true} when the screen consumed the key
     */
    public boolean keyDown(int keyCode) {
        if (!menu.isOpen() || !searchFocused) {
            return false;
        }
        if (keyCode == Input.Keys.BACKSPACE) {
            String query = creative.query();
            if (!query.isEmpty()) {
                creative.setQuery(query.substring(0, query.length() - 1));
            }
            return true;
        }
        return false;
    }

    /**
     * Adds a character to the search box.
     *
     * @param character character that was typed
     * @return {@code true} when the screen consumed the character
     */
    public boolean keyTyped(char character) {
        if (!menu.isOpen() || !searchFocused) {
            return false;
        }
        if (character < ' ' || character > '~') {
            // Everything else is a control character or lies beside the pixel font of
            // the game, which only covers the printable part of ASCII.
            return false;
        }
        creative.setQuery(creative.query() + character);
        return true;
    }

    /** {@code true} while a key that moves a stack to the other side is held. */
    private static boolean isShiftHeld() {
        return Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
    }

    /**
     * Draws the screen: the tabs, the panel, the items, the scroll bar and what the
     * mouse carries.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     */
    public void render(SpriteBatch batch, float mouseX, float mouseY) {
        if (!menu.isOpen()) {
            return;
        }
        float x = panelX();
        float top = panelTop();

        if (creative.showsPlayerInventory()) {
            // The panel, the items and the tooltips of the player inventory belong to the
            // screen that owns them; only the tabs are drawn here, on top of the panel edge.
            playerScreen.render(batch, mouseX, mouseY);
            drawTabs(batch, x, top);
            batch.setColor(Color.WHITE);
            return;
        }

        drawPanel(batch, x, panelY());
        drawTabs(batch, x, top);
        if (creative.isSearching()) {
            drawSearchBox(batch, x, top);
        }
        drawScrollBar(batch, x, top);
        drawItems(batch, x, top);
        drawHover(batch, x, top, mouseX, mouseY);
        drawTooltip(batch, mouseX, mouseY);
        drawCursorStack(batch, mouseX, mouseY);
        batch.setColor(Color.WHITE);
    }

    /** Draws the panel of the chosen tab, or the plain panel when the art is missing. */
    private void drawPanel(SpriteBatch batch, float x, float y) {
        TextureRegion panel = art.panel(creative.isSearching());
        if (panel == null) {
            panels.drawPanel(batch, x, y, CreativeLayout.PANEL_WIDTH, CreativeLayout.PANEL_HEIGHT);
            return;
        }
        batch.setColor(Color.WHITE);
        batch.draw(panel, x, y, CreativeLayout.PANEL_WIDTH, CreativeLayout.PANEL_HEIGHT);
    }

    /** Draws the tabs above the panel, the chosen one lit, and the icon of every tab. */
    private void drawTabs(SpriteBatch batch, float x, float top) {
        TextureRegion idle = art.tab(false);
        TextureRegion active = art.tab(true);
        if (idle == null || active == null) {
            return;
        }
        float tabY = top - CreativeLayout.TAB_INSET;
        for (int index = 0; index < creative.tabs().size(); index++) {
            batch.setColor(Color.WHITE);
            batch.draw(index == creative.selectedIndex() ? active : idle,
                    x + CreativeLayout.tabX(index), tabY, CreativeLayout.TAB_WIDTH,
                    CreativeLayout.TAB_HEIGHT);
            drawTabIcon(batch, x + CreativeLayout.tabX(index), tabY, creative.tabs().get(index));
        }
    }

    /**
     * Draws the icon of a tab.
     * <p>
     * The tabs of the original art are empty shapes, so what tells one group from another
     * is the item itself, drawn here the same way a slot draws it and without an amount.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param tabX X coordinate of the left edge of the tab
     * @param tabY Y coordinate of the lower edge of the tab
     * @param tab tab whose icon is drawn
     */
    private void drawTabIcon(SpriteBatch batch, float tabX, float tabY, CreativeTab tab) {
        if (!tab.hasIcon()) {
            return;
        }
        items.render(batch, ItemStack.of(tab.icon(), 1), tabX + CreativeLayout.TAB_ICON_X,
                tabY + CreativeLayout.TAB_HEIGHT - CreativeLayout.TAB_ICON_Y
                        - CreativeLayout.SLOT_SIZE);
    }

    /** Draws the text the player typed into the search box and its cursor. */
    private void drawSearchBox(SpriteBatch batch, float x, float top) {
        float textX = x + CreativeLayout.SEARCH_X + CreativeLayout.SEARCH_INSET;
        float textTop = top - CreativeLayout.SEARCH_Y - CreativeLayout.SEARCH_INSET;
        font.setColor(SEARCH_COLOR);
        font.drawShadowed(batch, creative.query(), textX, textTop);
        batch.setColor(Color.WHITE);

        if (searchFocused && elapsed % (BLINK_INTERVAL * 2.0f) < BLINK_INTERVAL && pixel != null) {
            batch.setColor(SEARCH_COLOR);
            batch.draw(pixel, textX + font.width(creative.query()),
                    textTop - font.glyphHeight(), 1.0f, font.glyphHeight());
            batch.setColor(Color.WHITE);
        }
    }

    /**
     * Draws the thumb of the scroll bar.
     * <p>
     * The track is part of the panel picture and the thumb comes in two shapes: the bright
     * one is dragged while the list goes on, the dark one says everything fits into the
     * grid already.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param x X coordinate of the left edge of the panel
     * @param top Y coordinate of the upper edge of the panel
     */
    private void drawScrollBar(SpriteBatch batch, float x, float top) {
        TextureRegion thumb = art.scrollThumb(creative.maxRow() > 0);
        if (thumb == null) {
            return;
        }
        batch.setColor(Color.WHITE);
        batch.draw(thumb, x + CreativeLayout.SCROLL_X,
                top - CreativeLayout.scrollThumbY(creative.firstRow(), creative.maxRow())
                        - CreativeLayout.SCROLL_THUMB_HEIGHT,
                CreativeLayout.SCROLL_WIDTH, CreativeLayout.SCROLL_THUMB_HEIGHT);
    }

    /** Draws the items of the grid and the stacks of the hotbar. */
    private void drawItems(SpriteBatch batch, float x, float top) {
        for (int index = 0; index < CreativeInventory.PAGE_SIZE; index++) {
            items.render(batch, creative.stackAt(index), x + CreativeLayout.slotIconX(index),
                    slotY(top, CreativeLayout.slotIconY(index)));
        }
        for (int slot = 0; slot < CreativeLayout.HOTBAR_COLUMNS; slot++) {
            items.render(batch, player.get(slot), x + CreativeLayout.hotbarIconX(slot),
                    slotY(top, CreativeLayout.hotbarIconY()));
        }
    }

    /**
     * Lower edge of a cell of the panel inside the interface.
     *
     * @param top Y coordinate of the upper edge of the panel inside the interface
     * @param localY upper edge of the cell inside the panel, measured downwards
     * @return the coordinate of the lower edge of the cell
     */
    private static float slotY(float top, int localY) {
        return top - localY - CreativeLayout.SLOT_SIZE;
    }

    /** Lightens the slot under the mouse, the same feedback the inventory gives. */
    private void drawHover(SpriteBatch batch, float x, float top, float mouseX, float mouseY) {
        if (pixel == null) {
            return;
        }
        int localX = localX(mouseX);
        int localY = localY(mouseY);
        int slot = CreativeLayout.slotAt(localX, localY);
        if (slot >= 0) {
            batch.setColor(HOVER_COLOR);
            batch.draw(pixel, x + CreativeLayout.slotIconX(slot),
                    slotY(top, CreativeLayout.slotIconY(slot)), CreativeLayout.SLOT_SIZE,
                    CreativeLayout.SLOT_SIZE);
            batch.setColor(Color.WHITE);
            return;
        }
        int hotbar = CreativeLayout.hotbarAt(localX, localY);
        if (hotbar >= 0 && !player.get(hotbar).isEmpty()) {
            batch.setColor(HOVER_COLOR);
            batch.draw(pixel, x + CreativeLayout.hotbarIconX(hotbar),
                    slotY(top, CreativeLayout.hotbarIconY()), CreativeLayout.SLOT_SIZE,
                    CreativeLayout.SLOT_SIZE);
            batch.setColor(Color.WHITE);
        }
    }

    /** Draws the name of a tab or of an item next to the mouse. */
    private void drawTooltip(SpriteBatch batch, float mouseX, float mouseY) {
        String label = labelUnderMouse(mouseX, mouseY);
        if (label == null || pixel == null) {
            return;
        }
        int border = TOOLTIP_PADDING + 1;
        int boxWidth = Math.round(font.width(label)) + 2 * border;
        int boxHeight = Math.round(font.lineHeight()) + 2 * border;
        int boxX = clamp(Math.round(mouseX) + TOOLTIP_OFFSET,
                Math.round(viewport.guiWidth()) - boxWidth);
        int boxY = clamp(Math.round(mouseY) + TOOLTIP_OFFSET,
                Math.round(viewport.guiHeight()) - boxHeight);

        batch.setColor(TOOLTIP_FRAME);
        batch.draw(pixel, boxX, boxY, boxWidth, boxHeight);
        batch.setColor(TOOLTIP_FILL);
        batch.draw(pixel, boxX + 1, boxY + 1, boxWidth - 2, boxHeight - 2);

        font.setColor(Color.WHITE);
        font.drawShadowed(batch, label, boxX + border, boxY + boxHeight - border);
        batch.setColor(Color.WHITE);
    }

    /** Draws the stack the mouse carries, centred on the cursor. */
    private void drawCursorStack(SpriteBatch batch, float mouseX, float mouseY) {
        ItemStack cursor = menu.cursorStack();
        if (cursor.isEmpty()) {
            return;
        }
        items.render(batch, cursor, mouseX - Constants.ITEM_ICON_SIZE * 0.5f,
                mouseY - Constants.ITEM_ICON_SIZE * 0.5f);
    }

    /**
     * Name of what stands under the mouse.
     *
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     * @return the title of a tab, the name of an item, or {@code null} for nothing
     */
    private String labelUnderMouse(float mouseX, float mouseY) {
        int localX = localX(mouseX);
        int localY = localY(mouseY);

        int tab = CreativeLayout.tabAt(localX, localY, creative.tabs().size());
        if (tab >= 0) {
            return creative.tabs().get(tab).title();
        }
        int slot = CreativeLayout.slotAt(localX, localY);
        ItemStack stack = slot >= 0 ? creative.stackAt(slot) : ItemStack.EMPTY;
        if (stack.isEmpty()) {
            int hotbar = CreativeLayout.hotbarAt(localX, localY);
            stack = hotbar >= 0 ? player.get(hotbar) : ItemStack.EMPTY;
        }
        return stack.isEmpty() ? null : stack.item().displayName();
    }

    /** Keeps a coordinate inside the window, never below zero. */
    private static int clamp(int value, int maximum) {
        return Math.max(0, Math.min(value, maximum));
    }
}
