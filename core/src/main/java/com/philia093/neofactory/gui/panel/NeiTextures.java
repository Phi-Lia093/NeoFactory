package com.philia093.neofactory.gui.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.render.BlockTextureCache;

/**
 * The pictures the screen of recipes is drawn with, which are the art of NEI.
 * <p>
 * One picture to a file below {@code gui/nei} and {@code gui/nei/icons}, which is how the art arrived:
 * <ul>
 *     <li>{@link #BACKGROUND} is the panel of a recipe, a frame of {@link #BORDER} pixels around a plain
 *         middle, so it is stretched in nine cells to whatever a recipe asks for, see
 *         {@link #drawPanel(SpriteBatch, float, float, float, float)};</li>
 *     <li>{@link #SINGLE} is the frame of the one recipe that is laid out, {@link #SLOT} the bevel of a
 *         cell an item goes into;</li>
 *     <li>{@link #TAB_SELECTED} and {@link #TAB_UNSELECTED} are the tabs of a group, and the three states
 *         of {@link #BUTTON} the button a page is turned with;</li>
 *     <li>the icons - an arrow, a flame, an info mark and the transfer of a recipe - stand on their own
 *         files as well, because a screen asks for one of them at a time.</li>
 * </ul>
 * A missing picture never breaks a screen: the panel falls back to a plain rectangle and the slots simply
 * stay empty, so the game stays usable while the art is being worked on, see {@link #isComplete()}.
 */
public final class NeiTextures {

    /** Folder holding the pictures of the screen of recipes. */
    public static final String FOLDER = BlockTextureCache.GUI_FOLDER + "nei/";

    /** Folder holding the small icons of that screen. */
    public static final String ICON_FOLDER = FOLDER + "icons/";

    /** Panel of a recipe, stretched in nine cells. */
    public static final String BACKGROUND = FOLDER + "gui_background";

    /** Frame of the one recipe that is laid out. */
    public static final String SINGLE = FOLDER + "single_recipe_background";

    /** Bevel of a cell an item goes into. */
    public static final String SLOT = FOLDER + "slot";

    /** The tab of the group that is on screen. */
    public static final String TAB_SELECTED = FOLDER + "tab_selected";

    /** The tab of a group that is not on screen. */
    public static final String TAB_UNSELECTED = FOLDER + "tab_unselected";

    /** The tab a machine is shown by, the one a recipe of it stands on. */
    public static final String TAB_CATALYST = FOLDER + "catalyst_tab";

    /** The ground of a button. */
    public static final String BUTTON = FOLDER + "button_enabled";

    /** The ground of a button the mouse points at. */
    public static final String BUTTON_HIGHLIGHT = FOLDER + "button_highlight";

    /** The ground of a button that cannot be used. */
    public static final String BUTTON_DISABLED = FOLDER + "button_disabled";

    /** The ground of the box a list is searched in. */
    public static final String SEARCH = FOLDER + "search_background";

    /** Arrow that turns one page back. */
    public static final String ARROW_PREVIOUS = ICON_FOLDER + "arrow_previous";

    /** Arrow that turns one page on. */
    public static final String ARROW_NEXT = ICON_FOLDER + "arrow_next";

    /** Flame of a machine that burns, drawn beside the fuel of a recipe. */
    public static final String FLAME = ICON_FOLDER + "flame";

    /** Mark of a recipe that carries something to know. */
    public static final String INFO = ICON_FOLDER + "info";

    /** Mark of the recipe that is laid out into the field of a machine. */
    public static final String TRANSFER = ICON_FOLDER + "recipe_transfer";

    /** Mark of a recipe that takes its ingredients wherever they lie. */
    public static final String SHAPELESS = ICON_FOLDER + "shapeless_icon";

    /** Side length of the panel picture in pixels. */
    public static final int SOURCE = 64;

    /** Thickness of the frame of the panel picture in pixels. */
    public static final int BORDER = 4;

    /** Side length of a slot picture in pixels, an item icon sits inside it. */
    public static final int SLOT_SIZE = 18;

    /** Side length of a tab in pixels. */
    public static final int TAB_SIZE = 24;

    /** Side length of a button in pixels. */
    public static final int BUTTON_SIZE = 20;

    /** Side length of a small button, the one the transfer of a recipe is marked with. */
    public static final int SMALL_BUTTON = 10;

    /** Width and height of an arrow in pixels. */
    public static final int ARROW_SIZE = 9;

    /** Side length of the flame in pixels. */
    public static final int FLAME_SIZE = 14;

    /** Side length of the small marks in pixels. */
    public static final int ICON_SIZE = 16;

    /** Colour the panel falls back to when its picture is missing. */
    private static final Color FALLBACK_FILL = new Color(0.776f, 0.776f, 0.776f, 1.0f);

    /** Cache the pictures were read from, asked for the white pixel of a fallback. */
    private final BlockTextureCache textures;

    /** The nine cells of the panel, ordered by column and then by row. */
    private final TextureRegion[] panel = new TextureRegion[NineSlice.COUNT * NineSlice.COUNT];

    private final TextureRegion single;
    private final TextureRegion slot;
    private final TextureRegion tabSelected;
    private final TextureRegion tabUnselected;
    private final TextureRegion tabCatalyst;
    private final TextureRegion button;
    private final TextureRegion buttonHighlight;
    private final TextureRegion buttonDisabled;
    private final TextureRegion search;
    private final TextureRegion arrowPrevious;
    private final TextureRegion arrowNext;
    private final TextureRegion flame;
    private final TextureRegion info;
    private final TextureRegion transfer;
    private final TextureRegion shapeless;

    /** One piece of a button that is cut down, reused instead of filling the heap with regions. */
    private final TextureRegion scratch = new TextureRegion();

    /**
     * Reads the pictures of the screen of recipes.
     *
     * @param textures texture cache providing the pictures below {@code gui/nei}
     */
    public NeiTextures(BlockTextureCache textures) {
        this.textures = textures;
        TextureRegion sheet = textures.region(BACKGROUND);
        if (sheet != null) {
            int middle = SOURCE - 2 * BORDER;
            int[] start = {0, BORDER, SOURCE - BORDER};
            int[] size = {BORDER, middle, BORDER};
            for (int column = 0; column < NineSlice.COUNT; column++) {
                for (int row = 0; row < NineSlice.COUNT; row++) {
                    panel[column + row * NineSlice.COUNT] = new TextureRegion(sheet, start[column],
                            start[row], size[column], size[row]);
                }
            }
        }
        this.single = textures.region(SINGLE);
        this.slot = textures.region(SLOT);
        this.tabSelected = textures.region(TAB_SELECTED);
        this.tabUnselected = textures.region(TAB_UNSELECTED);
        this.tabCatalyst = textures.region(TAB_CATALYST);
        this.button = textures.region(BUTTON);
        this.buttonHighlight = textures.region(BUTTON_HIGHLIGHT);
        this.buttonDisabled = textures.region(BUTTON_DISABLED);
        this.search = textures.region(SEARCH);
        this.arrowPrevious = textures.region(ARROW_PREVIOUS);
        this.arrowNext = textures.region(ARROW_NEXT);
        this.flame = textures.region(FLAME);
        this.info = textures.region(INFO);
        this.transfer = textures.region(TRANSFER);
        this.shapeless = textures.region(SHAPELESS);
    }

    /**
     * Draws the panel of a recipe, stretched to whatever it asks for.
     * <p>
     * The four corners keep their pixels, the edges are stretched along one axis and the middle along both,
     * which is what lets a single picture serve a recipe of three cells and a screen of nine.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param x left edge in interface pixels
     * @param y lower edge in interface pixels, the interface measures upwards
     * @param width width the panel is stretched to
     * @param height height the panel is stretched to
     */
    public void drawPanel(SpriteBatch batch, float x, float y, float width, float height) {
        if (panel[0] == null) {
            drawFallback(batch, x, y, width, height);
            return;
        }
        NineSlice slice = new NineSlice(Math.round(width), Math.round(height), BORDER);
        for (int column = 0; column < NineSlice.COUNT; column++) {
            int cellWidth = slice.width(column);
            if (cellWidth == 0) {
                continue;
            }
            for (int row = 0; row < NineSlice.COUNT; row++) {
                int cellHeight = slice.height(row);
                if (cellHeight == 0) {
                    continue;
                }
                // The grid is measured downwards from the upper edge of the picture, the interface upwards
                // from its lower edge.
                batch.draw(panel[column + row * NineSlice.COUNT], x + slice.x(column),
                        y + height - slice.y(row) - cellHeight, cellWidth, cellHeight);
            }
        }
    }

    /**
     * Draws the bevel of one slot, so an empty cell still reads as a place an item goes into.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param x left edge of the cell the slot belongs to
     * @param y lower edge of that cell
     */
    public void drawSlot(SpriteBatch batch, float x, float y) {
        if (slot != null) {
            batch.draw(slot, x - (SLOT_SIZE - 16) / 2.0f, y - (SLOT_SIZE - 16) / 2.0f, SLOT_SIZE,
                    SLOT_SIZE);
        }
    }

    /** Panel of a recipe, {@code null} when the art is missing. */
    public TextureRegion background() {
        return panel[0];
    }

    /**
     * Draws the ground of a button.
     * <p>
     * The ground of the pack is named the other way round from what it looks like:
     * {@code button_enabled} is the button the mouse is <b>not</b> over and {@code button_highlight} the one
     * it is. A button smaller than {@link #BUTTON_SIZE} is the big one <b>cut</b> and not scaled: the four
     * corners of the picture keep their pixels and the middle of it is left out, which is what keeps the bevel
     * of a button a bevel however small the button is drawn, the way the frame of a panel keeps its own.
     *
     * @param batch batch switched to the projection of the interface viewport
     * @param x left edge of the button
     * @param y lower edge of the button
     * @param size side the button is drawn in
     * @param usable {@code true} when the button can be used
     * @param pointed {@code true} while the mouse points at it
     */
    public void drawButton(SpriteBatch batch, float x, float y, int size, boolean usable, boolean pointed) {
        TextureRegion art = usable ? (pointed ? buttonHighlight : button) : buttonDisabled;
        if (art == null || size <= 0) {
            return;
        }
        if (size >= BUTTON_SIZE) {
            batch.draw(art, x, y, size, size);
            return;
        }
        int cut = Math.max(1, (BUTTON_SIZE - size) / 2);
        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 2; row++) {
                int sourceX = column == 0 ? 0 : BUTTON_SIZE - cut;
                int sourceY = row == 0 ? 0 : BUTTON_SIZE - cut;
                int targetX = column == 0 ? 0 : size - cut;
                int targetY = row == 0 ? 0 : size - cut;
                scratch.setRegion(art, sourceX, sourceY, cut, cut);
                batch.draw(scratch, x + targetX, y + targetY, cut, cut);
            }
        }
    }

    /** Frame of the one recipe that is laid out, {@code null} when the art is missing. */
    public TextureRegion single() {
        return single;
    }

    /** Bevel of a slot, {@code null} when the art is missing. */
    public TextureRegion slot() {
        return slot;
    }

    /** Tab of the group on screen, {@code null} when the art is missing. */
    public TextureRegion tabSelected() {
        return tabSelected;
    }

    /** Tab of a group that is not on screen, {@code null} when the art is missing. */
    public TextureRegion tabUnselected() {
        return tabUnselected;
    }

    /** Tab a machine is shown by. */
    public TextureRegion tabCatalyst() {
        return tabCatalyst;
    }

    /** Ground of a button that can be used, {@code null} when the art is missing. */
    public TextureRegion button() {
        return button;
    }

    /** Ground of a button the mouse points at. */
    public TextureRegion buttonHighlight() {
        return buttonHighlight;
    }

    /** Ground of a button that cannot be used. */
    public TextureRegion buttonDisabled() {
        return buttonDisabled;
    }

    /** Ground of the box a list is searched in. */
    public TextureRegion search() {
        return search;
    }

    /** Arrow that turns one page back. */
    public TextureRegion arrowPrevious() {
        return arrowPrevious;
    }

    /** Arrow that turns one page on. */
    public TextureRegion arrowNext() {
        return arrowNext;
    }

    /** Flame of a machine that burns. */
    public TextureRegion flame() {
        return flame;
    }

    /** Mark of a recipe that carries something to know. */
    public TextureRegion info() {
        return info;
    }

    /** Mark of the recipe that is laid out into the field of a machine. */
    public TextureRegion transfer() {
        return transfer;
    }

    /** Mark of a recipe that takes its ingredients wherever they lie. */
    public TextureRegion shapeless() {
        return shapeless;
    }

    /** {@code true} when the panel and the slots of the screen could be read. */
    public boolean isComplete() {
        return panel[0] != null && single != null && slot != null;
    }

    /** Draws a plain rectangle in place of the missing panel picture. */
    private void drawFallback(SpriteBatch batch, float x, float y, float width, float height) {
        TextureRegion pixel = textures.whitePixel();
        if (pixel == null) {
            return;
        }
        batch.setColor(FALLBACK_FILL);
        batch.draw(pixel, x, y, width, height);
        batch.setColor(Color.WHITE);
    }
}
