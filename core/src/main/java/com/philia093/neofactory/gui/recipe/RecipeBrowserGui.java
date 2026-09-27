package com.philia093.neofactory.gui.recipe;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.gui.GuiItemRenderer;
import com.philia093.neofactory.gui.GuiViewport;
import com.philia093.neofactory.gui.MenuLayout;
import com.philia093.neofactory.gui.container.ContainerView;
import com.philia093.neofactory.gui.container.Slot;
import com.philia093.neofactory.gui.panel.NeiTextures;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemSearch;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.recipe.Recipe;
import com.philia093.neofactory.recipe.RecipeIndex;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.render.PixelFont;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The screen of recipes: what an item is made of, what it is used for and what a group holds.
 * <p>
 * It is opened on the item under the mouse - {@code R} asks how it is made, {@code U} what it is used for -
 * and writes its own page: the ingredients in the cells, the arrow, the product beside it and the number of
 * the page in the foot. <b>A click on the page lays the recipe out</b> into the field the player has open,
 * which is the gesture the screen exists for, see {@link Arranger}. A click on a cell walks on to that item,
 * the arrows of the head walk through the groups of the game, and the box at the foot of a list searches it
 * the way the box of the creative inventory does, see {@link ItemSearch}.
 * <p>
 * Nothing here is drawn from art of its own: the panel, the cells and the arrow are the pictures of the
 * interface, so the screen reads as part of the game, see {@link RecipeBrowserLayout}.
 */
public final class RecipeBrowserGui {

    /**
     * What a click on a page does.
     * <p>
     * The screen knows nothing about a field, an inventory or a game mode, so it asks the game: the answer
     * is the line it writes in its foot, which tells the player what was laid out and what is missing, see
     * {@link com.philia093.neofactory.recipe.RecipeArranger}.
     */
    public interface Arranger {

        /**
         * Lays a recipe out for the player.
         *
         * @param recipe recipe that was clicked
         * @return the line to write in the foot of the panel
         */
        String arrange(Recipe recipe);
    }

    /** Colour of the titles and of the numbers of the screen. */
    private static final Color TEXT_COLOR = new Color(Color.WHITE);

    /** Colour of the line that reports what a click did. */
    private static final Color MESSAGE_COLOR = new Color(0.95f, 0.9f, 0.6f, 1.0f);

    /** Seconds one blink of the cursor of the search box takes. */
    private static final float BLINK_SECONDS = 0.5f;

    /** Pixels between the upper edge of the panel and the top of a line of text. */
    private static final int TITLE_TOP = 4;

    /** Index of the cell that holds the product, which is not one of the cells of the pattern. */
    private static final int RESULT_CELL = -2;

    /** Overlay drawn on the cell under the mouse. */
    private static final Color HOVER_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.5f);

    /** Colour of the track of the scroll bar. */
    private static final Color TRACK_COLOR = new Color(0.1f, 0.1f, 0.1f, 0.6f);

    /** Colour of the thumb of the scroll bar. */
    private static final Color THUMB_COLOR = new Color(0.75f, 0.75f, 0.75f, 0.9f);

    /** Colour of the plate the search box is drawn on. */
    private static final Color SEARCH_COLOR = new Color(0.05f, 0.05f, 0.05f, 0.85f);

    private final RecipeIndex index;
    private final GuiViewport viewport;
    private final PixelFont font;
    private final ContainerView view;
    private final GuiItemRenderer items;
    private final PanelTextures panel;

    /**
     * Art of NEI, which the panel and the slots of a recipe are drawn with.
     * <p>
     * A screen that shows a recipe is the screen of NEI and reads as one: its panel is the panel of that
     * screen and not the panel of a chest, and a cell an item goes into is the cell of that screen. The art
     * of the game's own interface stands in for it while a picture of the pack is missing, see
     * {@link NeiTextures#isComplete()}.
     */
    private final NeiTextures nei;

    /** Bar of the arrow of a page, which walks while the screen is up, see {@link ProgressAnimation}. */
    private final ProgressAnimation progress = new ProgressAnimation();
    private final TextureRegion pixel;
    private final List<RecipeCategory> groups;

    /** {@code true} while the screen covers the world. */
    private boolean open;

    /** {@code true} while a list of items is shown instead of a page of a recipe. */
    private boolean list;

    /** Item whose recipes are shown, {@code null} while a list is shown. */
    private Item item;

    /** {@code true} while the page shows what uses the item instead of what makes it. */
    private boolean uses;

    /** Recipes of the item that is shown. */
    private List<Recipe> recipes = List.of();

    /** Page of {@link #recipes} that is shown. */
    private int page;

    /** Group of the list that is shown. */
    private int group;

    /** First row of the list that is shown. */
    private int firstRow;

    /** Text of the search box of a list. */
    private String query = "";

    /** {@code true} while the search box owns the keyboard. */
    private boolean searchFocused;

    /** Line written in the foot, empty when there is nothing to report. */
    private String message = "";

    /** Seconds the screen is up, used for the cursor of the search box. */
    private float elapsed;

    /** Cell of the panel the mouse points at, {@code -1} while it points beside every cell. */
    private int hovered = -1;

    /** X coordinate of the mouse of the frame that is being drawn, for the buttons of the panel. */
    private float mouseX;

    /** Y coordinate of that mouse, from the bottom of the interface. */
    private float mouseY;

    /** Rectangle of the slot the screen was opened at, used to place the panel beside it. */
    private float anchorX;
    private float anchorY;
    private float anchorWidth;
    private float anchorHeight;

    /** What a click on a page does, {@code null} while the game offers no field. */
    private Arranger arranger;

    /**
     * Creates the screen.
     *
     * @param index index of the recipes of the game
     * @param textures texture cache providing the panel and the item icons
     * @param font font used for the titles, the numbers and the searched text
     * @param viewport viewport of the interface, see {@link GuiViewport}
     */
    public RecipeBrowserGui(RecipeIndex index, BlockTextureCache textures, PixelFont font,
            GuiViewport viewport) {
        this.index = Objects.requireNonNull(index, "index");
        this.viewport = Objects.requireNonNull(viewport, "viewport");
        this.font = Objects.requireNonNull(font, "font");
        this.view = new ContainerView(textures, font, viewport);
        this.items = new GuiItemRenderer(textures, font);
        this.panel = view.panelTextures();
        this.nei = new NeiTextures(textures);
        this.pixel = textures.whitePixel();
        this.groups = RecipeCategory.of(index);
    }

    /** Sets what a click on a page does. */
    public void setArranger(Arranger arranger) {
        this.arranger = arranger;
    }

    /**
     * Opens the screen on an item.
     * <p>
     * An empty item opens the list instead, which is what a player who pressed a key without pointing at
     * anything gets: a browse of the groups of the game.
     *
     * @param hovered item the mouse points at, may be empty
     * @param uses {@code true} for the recipes that take the item, {@code false} for the ones that make it
     * @param anchorX X coordinate of the slot it was opened at
     * @param anchorY Y coordinate of the slot it was opened at, from the bottom
     * @param anchorWidth width of that slot
     * @param anchorHeight height of that slot
     */
    public void open(ItemStack hovered, boolean uses, float anchorX, float anchorY,
            float anchorWidth, float anchorHeight) {
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorWidth = anchorWidth;
        this.anchorHeight = anchorHeight;
        this.message = "";
        this.hovered = -1;
        if (hovered == null || hovered.isEmpty()) {
            openList();
            return;
        }
        this.item = hovered.item();
        this.uses = uses;
        this.recipes = uses ? index.usesOf(item) : index.resultsOf(item);
        this.page = 0;
        this.list = false;
        this.searchFocused = false;
        this.open = true;
    }

    /** Opens the screen on the list of the first group, with the search box in charge of the keyboard. */
    public void openList() {
        this.item = null;
        this.recipes = List.of();
        this.page = 0;
        this.group = 0;
        this.firstRow = 0;
        this.query = "";
        this.list = true;
        this.searchFocused = true;
        this.open = true;
    }

    /** {@code true} while the screen covers the world. */
    public boolean isOpen() {
        return open;
    }

    /** {@code true} while a list of items is shown instead of a page of a recipe. */
    public boolean isList() {
        return list;
    }

    /** Item the page is up for, {@code null} while a list is shown. */
    public Item item() {
        return item;
    }

    /** {@code true} while the page shows what uses the item instead of what makes it. */
    public boolean isUses() {
        return uses;
    }

    /** Closes the screen. */
    public void close() {
        open = false;
        list = false;
        searchFocused = false;
        item = null;
        recipes = List.of();
        hovered = -1;
        message = "";
    }

    /** The groups of the screen, in the order the arrows of the head walk through them. */
    public List<RecipeCategory> groups() {
        return groups;
    }

    /** Index of the group that is shown. */
    public int groupIndex() {
        return group;
    }

    /** Name of the group that is shown, empty while a page of a recipe is up. */
    public String groupName() {
        return list && !groups.isEmpty() ? groups.get(group).name() : "";
    }

    /** Amount of recipes of the item that is shown. */
    public int recipeCount() {
        return recipes.size();
    }

    /** Index of the page that is shown. */
    public int pageIndex() {
        return page;
    }

    /** Amount of pages the recipes of the item are shown in, at least one. */
    public int pageCount() {
        return Math.max(1, recipes.size());
    }

    /** Row the list starts at. */
    public int firstRow() {
        return firstRow;
    }

    /**
     * The recipe page that is shown.
     *
     * @return the page, or {@code null} while a list is up or the item is made by nothing
     */
    public RecipePage page() {
        if (list || recipes.isEmpty()) {
            return null;
        }
        return RecipePage.of(recipes.get(Math.min(page, recipes.size() - 1)));
    }

    /** Text of the search box. */
    public String query() {
        return query;
    }

    /** {@code true} while the search box owns the keyboard. */
    public boolean isSearchFocused() {
        return searchFocused;
    }

    /** Line written in the foot of the panel. */
    public String message() {
        return message;
    }

    /** Cell of the panel the mouse pointed at while it was drawn, {@code -1} for none. */
    public int hoveredCell() {
        return hovered;
    }

    /** The items the list shows now, after the group and the search box were applied. */
    public List<Item> listedItems() {
        if (!list || groups.isEmpty()) {
            return List.of();
        }
        List<Item> found = new ArrayList<>();
        for (Item candidate : groups.get(group).items()) {
            if (ItemSearch.matches(candidate, query)) {
                found.add(candidate);
            }
        }
        return found;
    }

    /** Counts the seconds the screen is up, for the cursor of the search box. */
    public void update(float delta) {
        if (open) {
            elapsed += delta;
            // The bar of the arrow walks while a recipe is shown, at one speed for every recipe, see
            // ProgressAnimation.
            progress.update(delta);
        }
    }

    /** Width of the panel in virtual pixels. */
    public int panelWidth() {
        return RecipeBrowserLayout.WIDTH;
    }

    /** Height of the panel in virtual pixels, as tall as what it shows asks for. */
    public int panelHeight() {
        return RecipeBrowserLayout.height(list);
    }

    /** X coordinate of the left edge of the panel, in the middle of the interface. */
    public float panelX() {
        return Math.round((viewport.guiWidth() - RecipeBrowserLayout.WIDTH) * 0.5f);
    }

    /** Y coordinate of the lower edge of the panel, in the middle of the interface. */
    public float panelY() {
        return Math.round((viewport.guiHeight() - RecipeBrowserLayout.height(list)) * 0.5f);
    }

    /** {@code true} when a point of the interface lies on the panel. */
    public boolean isOnPanel(float guiX, float guiY) {
        if (!open) {
            return false;
        }
        float x = panelX();
        float y = panelY();
        return guiX >= x && guiX < x + RecipeBrowserLayout.WIDTH && guiY >= y
                && guiY < y + panelHeight();
    }

    /**
     * Handles a click on the panel.
     * <p>
     * <b>A click on a cell walks on to the item it holds</b> - the left button asks how it is made, the
     * right one what it is used for, the way a player of the original game expects - <b>and a click on the
     * rest of the page lays the recipe out</b>, which is what the screen exists for. A click beside the
     * panel closes it.
     *
     * @param guiX X coordinate of the mouse inside the interface
     * @param guiY Y coordinate of the mouse inside the interface, from the bottom
     * @param button mouse button that was pressed
     * @return {@code true} when the click was consumed
     */
    public boolean touchDown(float guiX, float guiY, int button) {
        if (!open) {
            return false;
        }
        if (!isOnPanel(guiX, guiY)) {
            close();
            return true;
        }
        int localX = localX(guiX);
        int localY = localY(guiY);
        if (RecipeBrowserLayout.isOnPrevious(localX, localY, list)) {
            walkPage(-1);
            return true;
        }
        if (RecipeBrowserLayout.isOnNext(localX, localY, list)) {
            walkPage(1);
            return true;
        }
        if (list) {
            if (RecipeBrowserLayout.isOnGroupPrevious(localX, localY)) {
                walkGroup(-1);
                return true;
            }
            if (RecipeBrowserLayout.isOnGroupNext(localX, localY)) {
                walkGroup(1);
                return true;
            }
            if (RecipeBrowserLayout.isOnSearch(localX, localY)) {
                searchFocused = true;
                return true;
            }
            if (RecipeBrowserLayout.isOnScroll(localX, localY)) {
                firstRow = RecipeBrowserLayout.rowAtScrollY(localY, maxRow());
                return true;
            }
            int cell = RecipeBrowserLayout.listCellAt(localX, localY);
            if (cell >= 0) {
                walkToCell(cell, button);
                return true;
            }
            searchFocused = false;
            return true;
        }
        ItemStack cell = cellUnder(localX, localY);
        if (cell != null && !cell.isEmpty()) {
            open(cell, button == Input.Buttons.RIGHT, guiX, guiY, 0, 0);
            this.anchorWidth = RecipeBrowserLayout.CELL;
            this.anchorHeight = RecipeBrowserLayout.CELL;
            return true;
        }
        layOut();
        return true;
    }

    /** Lets go of a click, which the screen has nothing left to do with. */
    public boolean touchUp(float guiX, float guiY, int button) {
        return open;
    }

    /**
     * Walks through the pages with the wheel.
     *
     * @param notches notches of the wheel, positive when it turns up
     * @return {@code true} when the screen took the notches
     */
    public boolean scrolled(float notches) {
        if (!open || notches == 0.0f) {
            return false;
        }
        if (list) {
            walkListRow(notches > 0.0f ? -1 : 1);
            return true;
        }
        walkPage(notches > 0.0f ? -1 : 1);
        return true;
    }

    /**
     * {@code true} while the box that is searched owns a key.
     *
     * @param keyCode key that was pressed, see {@link Input.Keys}
     * @return {@code true} for every key but the escape key
     */
    public boolean takesKey(int keyCode) {
        return open && searchFocused && keyCode != Input.Keys.ESCAPE;
    }

    /**
     * Handles a key of the search box.
     *
     * @param keyCode key that was pressed
     * @return {@code true} when the key belonged to the box
     */
    public boolean keyDown(int keyCode) {
        if (!takesKey(keyCode)) {
            return false;
        }
        if (keyCode == Input.Keys.BACKSPACE && !query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
            firstRow = 0;
        }
        return true;
    }

    /**
     * Writes a typed character into the search box.
     *
     * @param character character that was typed
     * @return {@code true} when the character belonged to the box
     */
    public boolean keyTyped(char character) {
        if (!open || !searchFocused) {
            return false;
        }
        if (character >= 32 && character != 127) {
            query += character;
            firstRow = 0;
        }
        return true;
    }

    /** Walks one page back or on, clamped to the recipes or to the rows of the list. */
    private void walkPage(int step) {
        if (list) {
            walkListRow(step);
            return;
        }
        if (recipes.isEmpty()) {
            return;
        }
        page = Math.max(0, Math.min(page + step, recipes.size() - 1));
    }

    /** Walks one row of the list. */
    private void walkListRow(int step) {
        firstRow = Math.max(0, Math.min(firstRow + step, maxRow()));
    }

    /** Last row the list may start at. */
    private int maxRow() {
        int items = listedItems().size();
        int rows = (items + RecipeBrowserLayout.LIST_COLUMNS - 1) / RecipeBrowserLayout.LIST_COLUMNS;
        return Math.max(0, rows - RecipeBrowserLayout.LIST_ROWS);
    }

    /** Walks on to the next group of items, wrapping around at both ends. */
    private void walkGroup(int step) {
        if (groups.isEmpty()) {
            return;
        }
        group = Math.floorMod(group + step, groups.size());
        firstRow = 0;
    }

    /** Opens the recipes of the item that lies in one cell of the list. */
    private void walkToCell(int cell, int button) {
        List<Item> listed = listedItems();
        int position = firstRow * RecipeBrowserLayout.LIST_COLUMNS + cell;
        if (position < 0 || position >= listed.size()) {
            return;
        }
        int column = cell % RecipeBrowserLayout.LIST_COLUMNS;
        int row = cell / RecipeBrowserLayout.LIST_COLUMNS;
        float slotX = panelX() + RecipeBrowserLayout.listX(column);
        float slotY = panelY() + panelHeight() - RecipeBrowserLayout.listY(row)
                - RecipeBrowserLayout.CELL;
        open(ItemStack.of(listed.get(position), 1), button == Input.Buttons.RIGHT, slotX, slotY,
                RecipeBrowserLayout.CELL, RecipeBrowserLayout.CELL);
    }

    /**
     * The item a cell of the page holds.
     *
     * @param localX X coordinate relative to the panel
     * @param localY Y coordinate relative to the panel, measured downwards
     * @return the stack, empty for a cell the recipe leaves empty, {@code null} beside every cell
     */
    private ItemStack cellUnder(int localX, int localY) {
        RecipePage shown = page();
        if (shown == null) {
            return null;
        }
        for (int row = 0; row < RecipeBrowserLayout.PAGE_ROWS; row++) {
            for (int column = 0; column < RecipeBrowserLayout.PAGE_COLUMNS; column++) {
                if (isInCell(localX, localY, RecipeBrowserLayout.cellX(column),
                        RecipeBrowserLayout.cellY(row))) {
                    return shown.cell(column, row);
                }
            }
        }
        if (isInCell(localX, localY, RecipeBrowserLayout.RESULT_X, RecipeBrowserLayout.RESULT_Y)) {
            return shown.result();
        }
        return null;
    }

    /** {@code true} when a point lies inside one cell of the panel. */
    private static boolean isInCell(int localX, int localY, int x, int y) {
        return localX >= x && localX < x + RecipeBrowserLayout.CELL && localY >= y
                && localY < y + RecipeBrowserLayout.CELL;
    }

    /** Lays the recipe of the page out for the player and reports what came of it. */
    private void layOut() {
        RecipePage shown = page();
        if (shown == null) {
            message = "nothing makes it";
            return;
        }
        if (arranger == null) {
            message = "no field is open";
            return;
        }
        message = arranger.arrange(shown.recipe());
    }

    /**
     * Draws the panel with the page or the list on it.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     */
    public void render(SpriteBatch batch, float mouseX, float mouseY) {
        if (!open) {
            return;
        }
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        float x = panelX();
        float y = panelY();
        int height = panelHeight();
        if (list) {
            hovered = hoveredListCell(mouseX, mouseY);
        } else {
            hovered = hoveredPageCell(mouseX, mouseY);
        }
        batch.setColor(Color.WHITE);
        // A recipe is shown in the art of NEI, so a player reads the panel as the screen of recipes and not as
        // the panel of a chest; the art of the game stands in while a picture of the pack is missing.
        if (nei.background() != null) {
            nei.drawPanel(batch, x, y, RecipeBrowserLayout.WIDTH, height);
        } else {
            panel.drawPanel(batch, x, y, RecipeBrowserLayout.WIDTH, height);
        }
        if (list) {
            renderList(batch, x, y, height);
        } else {
            renderPage(batch, x, y, height);
        }
        batch.setColor(Color.WHITE);
    }

    /** Draws the page of a recipe: the head, the ingredients, the arrow, the product and the foot. */
    private void renderPage(SpriteBatch batch, float x, float y, int height) {
        RecipePage shown = page();
        String head = item == null ? "" : item.displayName();
        String kind = shown == null ? "" : shown.kind();
        // The head of the panel is a line of the panel and nothing more: what a player reads there is the item
        // and what is done with it, and what does not fit is left out instead of running out of the panel, see
        // NeiPreviewTest, which measures every line of this screen against the panel it lies in.
        if (width(head) + width(kind) + 4 > RecipeBrowserLayout.WIDTH - 2 * RecipeBrowserLayout.PADDING) {
            kind = "";
        }
        if (width(head) > RecipeBrowserLayout.WIDTH - 2 * RecipeBrowserLayout.PADDING) {
            head = RecipeBrowserLayout.trimToPanel(head);
        }
        drawHead(batch, x, y, height, head);
        if (!kind.isEmpty()) {
            drawLine(batch, x, y, height,
                    RecipeBrowserLayout.WIDTH - RecipeBrowserLayout.PADDING - width(kind),
                    RecipeBrowserLayout.TEMPLATE_NAME_Y, kind, TEXT_COLOR);
        }
        if (shown == null) {
            drawFooter(batch, x, y, height, "nothing is known about it");
            return;
        }
        for (int row = 0; row < RecipeBrowserLayout.PAGE_ROWS; row++) {
            for (int column = 0; column < RecipeBrowserLayout.PAGE_COLUMNS; column++) {
                drawCell(batch, x, y, height, RecipeBrowserLayout.cellX(column),
                        RecipeBrowserLayout.cellY(row), shown.cell(column, row),
                        row * RecipeBrowserLayout.PAGE_COLUMNS + column);
            }
        }
        TextureRegion arrow = panel.arrowFull();
        if (arrow != null) {
            int full = arrow.getRegionWidth();
            // The bright part grows from the left of the track, so the picture of the arrow is cut at the
            // width the bar has walked to, see ProgressAnimation.
            arrow.setRegionWidth(Math.max(1, Math.round(full * progress.share())));
            batch.draw(arrow, x + RecipeBrowserLayout.ARROW_X,
                    y + height - RecipeBrowserLayout.ARROW_Y - arrow.getRegionHeight());
            arrow.setRegionWidth(full);
        }
        TextureRegion track = panel.arrowEmpty();
        if (track != null) {
            batch.draw(track, x + RecipeBrowserLayout.ARROW_X,
                    y + height - RecipeBrowserLayout.ARROW_Y - track.getRegionHeight());
        }
        drawCell(batch, x, y, height, RecipeBrowserLayout.RESULT_X, RecipeBrowserLayout.RESULT_Y,
                shown.result(), RESULT_CELL);
        // What the recipe costs stands in the rows the inventory of a machine stands in, and the page with
        // its two buttons stands right of it, see RecipeBrowserLayout#TEMPLATE_INFO_X.
        drawReport(batch, x, y, height, shown.recipe());
        drawFooter(batch, x, y, height, footLine());
    }

    /** Index of the cell of the page under the mouse, {@code -1} while it points beside every cell. */
    private int hoveredPageCell(float mouseX, float mouseY) {
        int localX = localX(mouseX);
        int localY = localY(mouseY);
        for (int row = 0; row < RecipeBrowserLayout.PAGE_ROWS; row++) {
            for (int column = 0; column < RecipeBrowserLayout.PAGE_COLUMNS; column++) {
                if (isInCell(localX, localY, RecipeBrowserLayout.cellX(column),
                        RecipeBrowserLayout.cellY(row))) {
                    return row * RecipeBrowserLayout.PAGE_COLUMNS + column;
                }
            }
        }
        if (isInCell(localX, localY, RecipeBrowserLayout.RESULT_X, RecipeBrowserLayout.RESULT_Y)) {
            return RESULT_CELL;
        }
        return -1;
    }

    /** Index of the cell of the list under the mouse, {@code -1} while it points beside every cell. */
    private int hoveredListCell(float mouseX, float mouseY) {
        return RecipeBrowserLayout.listCellAt(localX(mouseX), localY(mouseY));
    }

    /** Draws the list of a group: the head with the two group arrows, the cells and the search box. */
    private void renderList(SpriteBatch batch, float x, float y, int height) {
        drawHead(batch, x, y, height, (groupIndex() + 1) + "/" + groups.size() + "  " + groupName());
        drawButton(batch, x, y, height, RecipeBrowserLayout.GROUP_PREV_X, RecipeBrowserLayout.GROUP_Y,
                "<", !groups.isEmpty());
        drawButton(batch, x, y, height, RecipeBrowserLayout.GROUP_NEXT_X, RecipeBrowserLayout.GROUP_Y,
                ">", !groups.isEmpty());

        List<Item> listed = listedItems();
        for (int row = 0; row < RecipeBrowserLayout.LIST_ROWS; row++) {
            for (int column = 0; column < RecipeBrowserLayout.LIST_COLUMNS; column++) {
                int position = (firstRow + row) * RecipeBrowserLayout.LIST_COLUMNS + column;
                ItemStack stack = position < listed.size()
                        ? ItemStack.of(listed.get(position), 1) : ItemStack.EMPTY;
                drawCell(batch, x, y, height, RecipeBrowserLayout.listX(column),
                        RecipeBrowserLayout.listY(row), stack,
                        row * RecipeBrowserLayout.LIST_COLUMNS + column);
            }
        }
        drawScrollBar(batch, x, y, height);
        drawSearchBox(batch, x, y, height);
        drawButton(batch, x, y, height, RecipeBrowserLayout.PREV_X, RecipeBrowserLayout.SEARCH_Y, "<",
                firstRow > 0);
        drawButton(batch, x, y, height, RecipeBrowserLayout.NEXT_X, RecipeBrowserLayout.SEARCH_Y, ">",
                firstRow < maxRow());
        drawText(batch, x, y, height, RecipeBrowserLayout.PAGE_TEXT_X - 30,
                (firstRow + 1) + "/" + (maxRow() + 1), TEXT_COLOR);
    }

    /** Draws one cell of the panel: its bevel, the highlight under the mouse and the item. */
    private void drawCell(SpriteBatch batch, float x, float y, int height, int cellX, int cellY,
            ItemStack stack, int index) {
        float slotX = x + cellX;
        float slotY = y + height - cellY - RecipeBrowserLayout.CELL;
        if (nei.slot() != null) {
            nei.drawSlot(batch, slotX, slotY);
        } else {
            panel.drawSlot(batch, slotX, slotY, Slot.DEFAULT_ICON, Slot.DEFAULT_ICON);
        }
        if (index == hovered && pixel != null) {
            batch.setColor(HOVER_COLOR);
            batch.draw(pixel, slotX, slotY, RecipeBrowserLayout.CELL, RecipeBrowserLayout.CELL);
            batch.setColor(Color.WHITE);
        }
        if (!stack.isEmpty()) {
            items.render(batch, stack, slotX, slotY);
        }
    }

    /** Draws one of the small buttons of the screen, with its letter in the middle. */
    private void drawButton(SpriteBatch batch, float x, float y, int height, int buttonX, int buttonY,
            String letter, boolean active) {
        float buttonLeft = x + buttonX;
        float buttonBottom = y + height - buttonY - RecipeBrowserLayout.ARROW_SIZE;
        panel.drawSlot(batch, buttonLeft + 1, buttonBottom + 1, Slot.DEFAULT_ICON,
                Slot.DEFAULT_ICON);
        font.setColor(active ? TEXT_COLOR : MESSAGE_COLOR);
        font.draw(batch, letter, buttonLeft + 5, buttonBottom + 4);
        font.setColor(Color.WHITE);
    }

    /** Draws the track and the thumb of the scroll bar of a list. */
    private void drawScrollBar(SpriteBatch batch, float x, float y, int height) {
        if (pixel == null) {
            return;
        }
        float trackX = x + RecipeBrowserLayout.SCROLL_X;
        float trackY = y + height - RecipeBrowserLayout.SCROLL_Y - RecipeBrowserLayout.SCROLL_HEIGHT;
        batch.setColor(TRACK_COLOR);
        batch.draw(pixel, trackX, trackY, RecipeBrowserLayout.SCROLL_WIDTH,
                RecipeBrowserLayout.SCROLL_HEIGHT);
        batch.setColor(THUMB_COLOR);
        batch.draw(pixel, trackX, y + height - RecipeBrowserLayout.scrollThumbY(firstRow, maxRow())
                - RecipeBrowserLayout.SCROLL_THUMB_HEIGHT, RecipeBrowserLayout.SCROLL_WIDTH,
                RecipeBrowserLayout.SCROLL_THUMB_HEIGHT);
        batch.setColor(Color.WHITE);
    }

    /** Draws the plate and the text of the search box, with the cursor while it is in charge. */
    private void drawSearchBox(SpriteBatch batch, float x, float y, int height) {
        float boxX = x + RecipeBrowserLayout.SEARCH_X;
        float boxY = y + height - RecipeBrowserLayout.SEARCH_Y - RecipeBrowserLayout.SEARCH_HEIGHT;
        if (pixel != null) {
            batch.setColor(SEARCH_COLOR);
            batch.draw(pixel, boxX, boxY, RecipeBrowserLayout.SEARCH_WIDTH,
                    RecipeBrowserLayout.SEARCH_HEIGHT);
            batch.setColor(Color.WHITE);
        }
        String shown = query;
        if (searchFocused && (int) (elapsed / BLINK_SECONDS) % 2 == 0) {
            shown = shown + "_";
        }
        font.setColor(TEXT_COLOR);
        font.draw(batch, shown, boxX + 3, boxY + 4);
        font.setColor(Color.WHITE);
    }

    /**
     * Draws the page and its two buttons, the three of them in one row on the last row of the report.
     * <p>
     * The row carries {@code < X/Y >} and nothing else: the two buttons are the ground of NEI with the arrow
     * of NEI on top of it, the number of the page stands between them, and the row lies below the report, so
     * neither the buttons nor the number can ever stand on a line of it.
     */
    private void drawFooter(SpriteBatch batch, float x, float y, int height, String line) {
        drawPageButton(batch, x, y, height, RecipeBrowserLayout.TEMPLATE_PREV_X, page > 0,
                RecipeBrowserLayout.isOnPrevious(localX(mouseX), localY(mouseY), false),
                nei.arrowPrevious(), "<");
        drawPageButton(batch, x, y, height, RecipeBrowserLayout.TEMPLATE_NEXT_X,
                page + 1 < pageCount(),
                RecipeBrowserLayout.isOnNext(localX(mouseX), localY(mouseY), false),
                nei.arrowNext(), ">");
        drawLine(batch, x, y, height, RecipeBrowserLayout.TEMPLATE_PAGE_TEXT_X,
                RecipeBrowserLayout.TEMPLATE_PAGE_Y, (page + 1) + "/" + pageCount(), TEXT_COLOR);
        if (!line.isEmpty()) {
            drawLine(batch, x, y, height, RecipeBrowserLayout.TEMPLATE_INFO_X,
                    RecipeBrowserLayout.TEMPLATE_PAGE_Y, line, MESSAGE_COLOR);
        }
    }

    /**
     * Draws one button of the page: the ground of NEI with the arrow of NEI on top of it.
     * <p>
     * The ground of the pack is named the other way round from what it looks like:
     * {@code button_enabled} is the button the mouse is <b>not</b> over and {@code button_highlight} the one
     * it is, so the two are chosen by what the mouse points at and not by what the button can do.
     */
    private void drawPageButton(SpriteBatch batch, float x, float y, int height, int left, boolean usable,
            boolean pointed, TextureRegion arrow, String fallback) {
        int size = RecipeBrowserLayout.TEMPLATE_BUTTON_SIZE;
        float bottom = y + height - RecipeBrowserLayout.TEMPLATE_PAGE_Y - size;
        TextureRegion ground = groundOf(usable, pointed, size);
        if (ground != null) {
            batch.draw(ground, x + left, bottom, size, size);
        }
        if (arrow != null) {
            float inset = (size - arrow.getRegionWidth()) * 0.5f;
            batch.draw(arrow, x + left + inset, bottom + inset);
        } else {
            // The pack carries no arrow: the sign of the button is written where the arrow would lie.
            drawLine(batch, x, y, height, left, RecipeBrowserLayout.TEMPLATE_PAGE_Y
                    + (size - PixelFont.ASCII_CELL_SIZE) / 2, fallback, TEXT_COLOR);
        }
    }

    /**
     * The ground of a button: the art of the pack, cut down to the size the button is drawn in.
     * <p>
     * The button of NEI is {@code NeiTextures.BUTTON_SIZE} pixels wide and a small button is
     * {@link RecipeBrowserLayout#SMALL_BUTTON} of them: the picture is <b>cut</b> and not scaled, so the
     * bevel of the button keeps its pixels the way the frame of a panel keeps its own, see
     * {@link NeiTextures#smallButton(boolean, boolean)}.
     *
     * @param usable {@code true} when the button can be used
     * @param pointed {@code true} while the mouse points at it
     * @param size side the button is drawn in
     * @return the picture, {@code null} when the pack carries none
     */
    private TextureRegion groundOf(boolean usable, boolean pointed, int size) {
        return usable ? (pointed ? nei.buttonHighlight() : nei.button()) : nei.buttonDisabled();
    }

    /**
     * Writes what a recipe costs into the rows the inventory of a machine would stand in.
     * <p>
     * A screen that only looks at a recipe has no inventory of a player to show, so those rows carry the
     * report of the recipe instead, see {@link RecipeReport} and {@link RecipeBrowserLayout#TEMPLATE_INFO_Y}.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param x left edge of the panel
     * @param y lower edge of the panel
     * @param height height of the panel
     * @param recipe recipe the page shows
     */
    private void drawReport(SpriteBatch batch, float x, float y, int height, Recipe recipe) {
        List<String> lines = reportOf(recipe).lines();
        for (int line = 0; line < lines.size(); line++) {
            drawLine(batch, x, y, height, RecipeBrowserLayout.TEMPLATE_INFO_X,
                    RecipeBrowserLayout.templateLineY(line), lines.get(line), TEXT_COLOR);
        }
    }

    /**
     * What a recipe reports about itself.
     * <p>
     * An age that says nothing about energy reports none: the machines of this game burn fuel and steam, so
     * a recipe that is not an {@code EnergyRecipe} writes no volts at all, see {@link RecipeReport}.
     *
     * @param recipe recipe the page shows
     * @return the report of that recipe
     */
    private static RecipeReport reportOf(Recipe recipe) {
        RecipeReport report = new RecipeReport();
        if (recipe instanceof com.philia093.neofactory.recipe.SmeltingRecipe smelting) {
            report.time(smelting.seconds());
        }
        if (recipe instanceof com.philia093.neofactory.recipe.SteamRecipe steam) {
            report.time(steam.seconds()).steam(steam.steam());
        }
        if (recipe instanceof com.philia093.neofactory.recipe.EnergyRecipe energy) {
            report.energy(energy);
        }
        return report;
    }

    /** Width a line of the panel takes, counted with the widest a glyph of the font of the game can be. */
    private static int width(String text) {
        return RecipeBrowserLayout.textWidth(text);
    }

    /** Writes a line into the panel, its upper edge lying that many pixels below the upper edge of it. */
    private void drawLine(SpriteBatch batch, float x, float y, int height, int left, int top, String text,
            Color colour) {
        if (text.isEmpty()) {
            return;
        }
        font.setColor(colour);
        font.draw(batch, text, x + left, y + height - top - font.lineHeight());
        font.setColor(Color.WHITE);
    }

    /** The line of the foot: what a click did, or the name of the item under the mouse. */
    private String footLine() {
        if (!message.isEmpty()) {
            return message;
        }
        RecipePage shown = page();
        if (shown == null || hovered < 0) {
            return "";
        }
        ItemStack stack = hovered == RESULT_CELL ? shown.result()
                : shown.cell(hovered % RecipeBrowserLayout.PAGE_COLUMNS,
                        hovered / RecipeBrowserLayout.PAGE_COLUMNS);
        return stack.isEmpty() ? "" : stack.item().displayName();
    }

    /** Writes a line into the head of the panel. */
    private void drawHead(SpriteBatch batch, float x, float y, int height, String text) {
        drawText(batch, x, y, height, RecipeBrowserLayout.PADDING, text, TEXT_COLOR);
    }

    /** Writes a line into the panel, its top edge lying that many pixels below the upper edge. */
    private void drawText(SpriteBatch batch, float x, float y, int height, int left, String text,
            Color colour) {
        if (text.isEmpty()) {
            return;
        }
        font.setColor(colour);
        font.draw(batch, text, x + left, y + height - TITLE_TOP - font.lineHeight());
        font.setColor(Color.WHITE);
    }

    /** X coordinate of the mouse inside the panel. */
    private int localX(float guiX) {
        return Math.round(guiX) - Math.round(panelX());
    }

    /** Y coordinate of the mouse inside the panel, measured from its upper edge. */
    private int localY(float guiY) {
        return Math.round(panelY() + panelHeight() - guiY);
    }
}
