package com.philia093.neofactory.gui.creative;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.render.BlockTextureCache;

/**
 * The pictures the creative inventory is built from.
 * <p>
 * They live in the sheet of the interface panels, {@code gui/inventory_icons.png},
 * which {@code build/verify/extract_creative.ps1} copied the art of the original game
 * into, see {@link PanelTextures} for the other half of the sheet:
 * <ul>
 *     <li>two panels of {@value CreativeLayout#PANEL_WIDTH} by
 *         {@value CreativeLayout#PANEL_HEIGHT} pixels, one with the grid of items and
 *         one with the grid and the search box above it;</li>
 *     <li>a tab in two states, {@link #tab(boolean)} picks the idle or the chosen
 *         one;</li>
 *     <li>the track and the thumb of the scroll bar.</li>
 * </ul>
 * A missing picture never breaks the screen: the panels fall back to the plain panel
 * of {@link PanelTextures} and the tabs, the scroll bar and the search box are simply
 * left out, so the game stays usable while the art is being worked on.
 */
public final class CreativeTextures {

    /** Sheet holding the panels of the creative inventory, without extension. */
    public static final String SHEET = PanelTextures.SHEET;

    /** Where the pieces lie inside the sheet, read from its pixels. */
    private static final int ITEMS_X = 256;
    private static final int ITEMS_Y = 0;
    private static final int SEARCH_X = 256;
    private static final int SEARCH_Y = 136;
    private static final int TAB_IDLE_X = 256;
    private static final int TAB_IDLE_Y = 280;
    private static final int TAB_ACTIVE_X = 285;
    private static final int TAB_ACTIVE_Y = 280;
    private static final int THUMB_ACTIVE_X = 256;
    private static final int THUMB_ACTIVE_Y = 316;
    private static final int THUMB_IDLE_X = 270;
    private static final int THUMB_IDLE_Y = 316;

    private final TextureRegion itemsPanel;
    private final TextureRegion searchPanel;
    private final TextureRegion tabIdle;
    private final TextureRegion tabActive;
    private final TextureRegion tabIdleBelow;
    private final TextureRegion tabActiveBelow;
    private final TextureRegion scrollActive;
    private final TextureRegion scrollIdle;

    /**
     * Cuts the pictures of the sheet out.
     *
     * @param textures texture cache providing {@code gui/inventory_icons.png}
     */
    public CreativeTextures(BlockTextureCache textures) {
        this.itemsPanel = textures.region(SHEET, ITEMS_X, ITEMS_Y, CreativeLayout.PANEL_WIDTH,
                CreativeLayout.PANEL_HEIGHT);
        this.searchPanel = textures.region(SHEET, SEARCH_X, SEARCH_Y, CreativeLayout.PANEL_WIDTH,
                CreativeLayout.PANEL_HEIGHT);
        this.tabIdle = textures.region(SHEET, TAB_IDLE_X, TAB_IDLE_Y, CreativeLayout.TAB_WIDTH,
                CreativeLayout.TAB_HEIGHT);
        this.tabActive = textures.region(SHEET, TAB_ACTIVE_X, TAB_ACTIVE_Y,
                CreativeLayout.TAB_WIDTH, CreativeLayout.TAB_HEIGHT);
        this.tabIdleBelow = turned(tabIdle);
        this.tabActiveBelow = turned(tabActive);
        this.scrollActive = textures.region(SHEET, THUMB_ACTIVE_X, THUMB_ACTIVE_Y,
                CreativeLayout.SCROLL_WIDTH, CreativeLayout.SCROLL_THUMB_HEIGHT);
        this.scrollIdle = textures.region(SHEET, THUMB_IDLE_X, THUMB_IDLE_Y,
                CreativeLayout.SCROLL_WIDTH, CreativeLayout.SCROLL_THUMB_HEIGHT);
    }

    /**
     * The panel with the grid of items.
     *
     * @param searching {@code true} for the panel that carries the search box
     * @return the picture, {@code null} when the sheet is missing
     */
    public TextureRegion panel(boolean searching) {
        return searching ? searchPanel : itemsPanel;
    }

    /**
     * The picture of a tab.
     * <p>
     * The tabs below the panel share the art of the tabs above it: their picture is turned
     * by half a circle, which brings its straight edge to the top so it sits on the lower
     * edge of the panel, and then mirrored left to right, which turns the shading of the
     * picture back: a chosen tab of the lower row lights up on the same side as a chosen
     * tab of the upper one instead of the mirrored side.
     *
     * @param chosen {@code true} for the chosen tab
     * @param below {@code true} for a tab of the lower row
     * @return the picture, {@code null} when the sheet is missing
     */
    public TextureRegion tab(boolean chosen, boolean below) {
        if (below) {
            return chosen ? tabActiveBelow : tabIdleBelow;
        }
        return chosen ? tabActive : tabIdle;
    }

    /**
     * The same picture turned by half a circle and mirrored left to right.
     *
     * @param region picture of a tab above the panel, {@code null} when there is none
     * @return the picture for a tab below the panel, {@code null} when there is none
     */
    private static TextureRegion turned(TextureRegion region) {
        if (region == null) {
            return null;
        }
        TextureRegion turned = new TextureRegion(region);
        // Half a circle, so the straight edge of the tab points upwards.
        turned.flip(true, true);
        // And back left to right, so its shading stays on the side the player knows.
        turned.flip(true, false);
        return turned;
    }

    /**
     * Thumb of the scroll bar.
     * <p>
     * The original game keeps two of them. The bright one says the list goes on and can be
     * dragged, the dark one says everything fits into the panel and there is nothing to
     * scroll. The track itself is part of the panel picture, so it is not a separate one.
     *
     * @param scrollable {@code true} while the list is longer than the grid
     * @return the picture, {@code null} when the sheet is missing
     */
    public TextureRegion scrollThumb(boolean scrollable) {
        return scrollable ? scrollActive : scrollIdle;
    }

    /** {@code true} when the sheet could be cut out and every element is available. */
    public boolean isComplete() {
        return itemsPanel != null && searchPanel != null && tabIdle != null && tabActive != null
                && tabIdleBelow != null && tabActiveBelow != null && scrollActive != null
                && scrollIdle != null;
    }
}
