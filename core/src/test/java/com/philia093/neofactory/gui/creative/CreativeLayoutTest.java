package com.philia093.neofactory.gui.creative;

import com.philia093.neofactory.gui.GuiViewport;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the geometry of the creative inventory: where its tabs stand, how a click finds
 * them and that every tab keeps the very same panel.
 * <p>
 * The screen itself is painted by the game, so what is measured here is the arithmetic the
 * painting asks for: the tabs of the game spread over two rows - the first above the panel
 * and the second below it - without leaving its width, and the slots of the player fit
 * into the very panel the grid of items uses. That last part is what keeps the screen from
 * jumping when the player walks from one tab to the next.
 */
class CreativeLayoutTest {

    /** Amount of tabs the game shows, the case the two rows have to carry. */
    private static final int TABS = CreativeRegistry.tabs().size();

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void tabsThatFitIntoOneRowStayAboveThePanel() {
        int tabs = CreativeLayout.TAB_COLUMNS;

        assertEquals(tabs, CreativeLayout.tabColumns(tabs), "one row carries its tabs");
        for (int index = 0; index < tabs; index++) {
            assertEquals(0, CreativeLayout.tabRow(index, tabs));
            assertFalse(CreativeLayout.isTabBelow(index, tabs));
            assertEquals(CreativeLayout.TAB_X + index * CreativeLayout.TAB_STRIDE,
                    CreativeLayout.tabX(index, tabs));
            assertEquals(CreativeLayout.TAB_INSET - CreativeLayout.TAB_HEIGHT,
                    CreativeLayout.tabY(index, tabs), "the row hangs from the upper edge");
        }
    }

    @Test
    void moreTabsThanOneRowHoldsAreSplitOverTwoRows() {
        assertTrue(TABS > CreativeLayout.TAB_COLUMNS, "the game holds more tabs than one row");
        int above = CreativeLayout.tabColumns(TABS);

        assertEquals((TABS + 1) / 2, above, "the upper row takes the larger half");
        assertTrue(TABS - above > 0, "and the lower row is left with the rest");
        for (int index = 0; index < TABS; index++) {
            boolean below = CreativeLayout.isTabBelow(index, TABS);

            assertEquals(below ? 1 : 0, CreativeLayout.tabRow(index, TABS));
            assertEquals(below
                            ? CreativeLayout.PANEL_HEIGHT - CreativeLayout.TAB_INSET
                            : CreativeLayout.TAB_INSET - CreativeLayout.TAB_HEIGHT,
                    CreativeLayout.tabY(index, TABS), "the row of tab " + index);
            // Both rows start at the same margin and count from the left, so they read as
            // one frame around the panel instead of two loose strips of tabs.
            int column = below ? index - above : index;
            assertEquals(CreativeLayout.TAB_X + column * CreativeLayout.TAB_STRIDE,
                    CreativeLayout.tabX(index, TABS),
                    "tab " + index + " stands in column " + column);
            assertTrue(CreativeLayout.tabX(index, TABS) + CreativeLayout.TAB_WIDTH
                    <= CreativeLayout.PANEL_WIDTH, "tab " + index + " stays within the panel");
        }
    }

    @Test
    void aClickFindsTheTabOfEitherRow() {
        for (int index = 0; index < TABS; index++) {
            int x = CreativeLayout.tabX(index, TABS) + CreativeLayout.TAB_WIDTH / 2;
            int y = CreativeLayout.tabY(index, TABS) + CreativeLayout.TAB_HEIGHT / 2;

            assertEquals(index, CreativeLayout.tabAt(x, y, TABS),
                    "the middle of tab " + index + " reaches it");
        }
        assertEquals(-1, CreativeLayout.tabAt(0, CreativeLayout.PANEL_HEIGHT / 2, TABS),
                "a click inside the panel belongs to the slots and not to a tab");
    }

    @Test
    void theOverlapOfTwoTabsBelongsToTheRightOne() {
        // Two neighbouring tabs share a pixel, see CreativeLayout#TAB_STRIDE, and the
        // right one is painted on top: a click there has to reach the right one.
        int overlap = CreativeLayout.tabX(1, TABS);
        int row = CreativeLayout.TAB_INSET - 1;

        assertEquals(1, CreativeLayout.tabAt(overlap, row, TABS));
        assertEquals(0, CreativeLayout.tabAt(overlap - 1, row, TABS),
                "the pixel beside it still belongs to the left tab");
    }

    @Test
    void theTabsOfBothRowsHangFromTheEdgesOfThePanel() {
        float top = 200.0f;
        float bottom = top - CreativeLayout.PANEL_HEIGHT;

        for (int index = 0; index < TABS; index++) {
            float tabBottom = CreativeLayout.tabBottom(top, index, TABS);
            if (CreativeLayout.isTabBelow(index, TABS)) {
                // The lower row sits on the lower edge of the panel and reaches into it by
                // the inset, so it is painted below the panel and not among the upper row.
                assertEquals(bottom + CreativeLayout.TAB_INSET - CreativeLayout.TAB_HEIGHT,
                        tabBottom, 0.001f, "the lower row sits below the panel");
                assertTrue(tabBottom + CreativeLayout.TAB_HEIGHT > bottom,
                        "and reaches into it by the inset");
                assertTrue(tabBottom < bottom, "the rest of it hangs below the panel");
            } else {
                assertEquals(top - CreativeLayout.TAB_INSET, tabBottom, 0.001f,
                        "the upper row hangs from the upper edge");
                assertTrue(tabBottom < top, "and reaches into it by the inset");
                assertTrue(tabBottom + CreativeLayout.TAB_HEIGHT > top,
                        "the rest of it stands above the panel");
            }
        }
    }

    @Test
    void theIconOfATabStaysAwayFromTheEdgeItTouchesThePanelWith() {
        float top = 200.0f;
        float panelX = 30.0f;

        for (int index = 0; index < TABS; index++) {
            float bottom = CreativeLayout.tabBottom(top, index, TABS);
            float iconBottom = CreativeLayout.tabIconBottom(top, index, TABS);

            assertEquals(panelX + CreativeLayout.tabX(index, TABS) + CreativeLayout.TAB_ICON_X,
                    CreativeLayout.tabIconX(panelX, index, TABS), 0.001f,
                    "the icon stands at the same margin from the left in both rows");
            assertTrue(iconBottom >= bottom
                            && iconBottom + CreativeLayout.SLOT_SIZE
                                    <= bottom + CreativeLayout.TAB_HEIGHT,
                    "the icon of tab " + index + " stays inside its tab");
            if (CreativeLayout.isTabBelow(index, TABS)) {
                assertEquals(bottom + CreativeLayout.TAB_ICON_Y, iconBottom, 0.001f,
                        "the lower row keeps its icon away from the edge that touches the panel");
            } else {
                assertEquals(bottom + CreativeLayout.TAB_HEIGHT - CreativeLayout.TAB_ICON_Y
                        - CreativeLayout.SLOT_SIZE, iconBottom, 0.001f,
                        "and the upper row does the same");
            }
        }
    }

    @Test
    void theRowOfTabsBelowThePanelStaysClearOfTheHotbar() {
        int guiHeight = GuiViewport.MIN_HEIGHT;
        float panelY = CreativeLayout.panelY(guiHeight);
        float bottom = panelY - (CreativeLayout.TAB_HEIGHT - CreativeLayout.TAB_INSET);

        assertTrue(bottom >= CreativeLayout.HOTBAR_MARGIN,
                "the lower row of tabs ends above the hotbar of the player");
        assertTrue(panelY + CreativeLayout.PANEL_HEIGHT + CreativeLayout.TAB_HEIGHT <= guiHeight,
                "and the upper row ends inside the interface");
        assertTrue(CreativeLayout.HOTBAR_MARGIN < panelY,
                "the panel itself stands above the hotbar as well");
    }

    @Test
    void theInventoryOfThePlayerFitsIntoTheSamePanelAsTheGrid() {
        int storageBottom = CreativeLayout.GRID_Y
                + (PlayerInventory.STORAGE_ROWS - 1) * CreativeLayout.SLOT_PITCH
                + CreativeLayout.SLOT_SIZE;

        assertEquals(PlayerInventory.STORAGE_ROWS * CreativeInventory.COLUMNS,
                PlayerInventory.MAIN_SLOTS, "the storage of the player is three rows of nine");
        assertTrue(storageBottom <= CreativeLayout.HOTBAR_Y,
                "the storage of the player stays above its hotbar");
        assertTrue(CreativeLayout.HOTBAR_Y + CreativeLayout.SLOT_SIZE
                <= CreativeLayout.PANEL_HEIGHT, "the hotbar stays inside the panel");
        assertTrue(CreativeLayout.GRID_Y + CreativeInventory.ROWS * CreativeLayout.SLOT_PITCH
                <= CreativeLayout.HOTBAR_Y, "the grid of items stays above the hotbar too");
        assertEquals(CreativeInventory.PAGE_SIZE, CreativeInventory.COLUMNS * CreativeInventory.ROWS,
                "a page of items is the grid and nothing else");
    }
}
