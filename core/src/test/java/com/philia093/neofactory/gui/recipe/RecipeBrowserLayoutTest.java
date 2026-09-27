package com.philia093.neofactory.gui.recipe;

import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.machine.MachineMenu;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the geometry of the screen of recipes.
 * <p>
 * The two shapes of the panel - a page of a recipe and a list of items - have to fit the art of the
 * interface, and every cell, arrow, box and track a player can hit has to lie inside it. The numbers are
 * asked without a window, the way the layout of a machine and of a container screen is.
 */
class RecipeBrowserLayoutTest {

    @Test
    void theTwoShapesOfThePanelFitTheArtOfTheInterface() {
        assertEquals(PanelTextures.PANEL_WIDTH, RecipeBrowserLayout.WIDTH,
                "the panel is as wide as the art of the interface");
        assertTrue(RecipeBrowserLayout.height(false) > 0);
        assertEquals(RecipeBrowserLayout.TEMPLATE_HEIGHT, RecipeBrowserLayout.height(false),
                "a page is as tall as the panel of a machine, because it is laid out as one: its own rows "
                        + "carry what the recipe costs, see TEMPLATE_INFO_Y");

        int pageRight = RecipeBrowserLayout.RESULT_X + RecipeBrowserLayout.CELL
                + RecipeBrowserLayout.PADDING;
        assertTrue(pageRight <= RecipeBrowserLayout.WIDTH,
                "the product of a page ends inside the panel: " + pageRight);
        assertTrue(RecipeBrowserLayout.FOOTER_Y + RecipeBrowserLayout.FOOTER_HEIGHT
                        + RecipeBrowserLayout.PADDING <= RecipeBrowserLayout.height(false),
                "the foot of a page lies inside the panel");

        int listRight = RecipeBrowserLayout.LIST_X + RecipeBrowserLayout.LIST_COLUMNS
                * RecipeBrowserLayout.PITCH;
        assertTrue(listRight >= RecipeBrowserLayout.SEARCH_X + RecipeBrowserLayout.SEARCH_WIDTH,
                "the search box is not wider than the list");
        assertTrue(RecipeBrowserLayout.SEARCH_X + RecipeBrowserLayout.SEARCH_WIDTH
                        < RecipeBrowserLayout.SCROLL_X,
                "the search box ends before the scroll bar");
        assertTrue(RecipeBrowserLayout.SCROLL_X + RecipeBrowserLayout.SCROLL_WIDTH
                        + RecipeBrowserLayout.PADDING <= RecipeBrowserLayout.WIDTH,
                "the scroll bar ends inside the panel");
    }

    @Test
    void aPageAndAListEachAnswerForTheCellThePlayerSees() {
        assertTrue(RecipeBrowserLayout.isOnPage(RecipeBrowserLayout.FIELD_X,
                RecipeBrowserLayout.FIELD_Y), "the first cell of a page");
        assertTrue(RecipeBrowserLayout.isOnPage(RecipeBrowserLayout.RESULT_X,
                RecipeBrowserLayout.RESULT_Y), "the product of a page");
        assertFalse(RecipeBrowserLayout.isOnPage(RecipeBrowserLayout.PADDING,
                RecipeBrowserLayout.FOOTER_Y), "the foot is no cell of the page");

        assertEquals(0, RecipeBrowserLayout.listCellAt(RecipeBrowserLayout.LIST_X,
                RecipeBrowserLayout.LIST_Y), "the upper left cell of a list");
        assertEquals(RecipeBrowserLayout.LIST_COLUMNS,
                RecipeBrowserLayout.listCellAt(RecipeBrowserLayout.LIST_X,
                        RecipeBrowserLayout.LIST_Y + RecipeBrowserLayout.PITCH),
                "the cell below it");
        int bevel = RecipeBrowserLayout.LIST_X + RecipeBrowserLayout.CELL;
        assertEquals(-1, RecipeBrowserLayout.listCellAt(bevel, RecipeBrowserLayout.LIST_Y),
                "the bevel between two cells belongs to neither of them");
        assertEquals(-1, RecipeBrowserLayout.listCellAt(RecipeBrowserLayout.SEARCH_X,
                RecipeBrowserLayout.SEARCH_Y), "the search box is no cell");

        assertTrue(RecipeBrowserLayout.isOnSearch(RecipeBrowserLayout.SEARCH_X + 2,
                RecipeBrowserLayout.SEARCH_Y + 2));
        assertTrue(RecipeBrowserLayout.isOnScroll(RecipeBrowserLayout.SCROLL_X + 2,
                RecipeBrowserLayout.SCROLL_Y + 2));
    }

    @Test
    void theArrowsOfAPageAndOfAListLieWhereTheyAreHit() {
        assertTrue(RecipeBrowserLayout.isOnPrevious(RecipeBrowserLayout.PREV_X + 2,
                RecipeBrowserLayout.FOOTER_Y + 2, false), "the page carries its arrows in its foot");
        assertTrue(RecipeBrowserLayout.isOnNext(RecipeBrowserLayout.NEXT_X + 2,
                RecipeBrowserLayout.FOOTER_Y + 2, false));
        assertTrue(RecipeBrowserLayout.isOnPrevious(RecipeBrowserLayout.PREV_X + 2,
                RecipeBrowserLayout.SEARCH_Y + 2, true), "a list carries them beside its box");
        assertFalse(RecipeBrowserLayout.isOnPrevious(RecipeBrowserLayout.PREV_X + 2,
                RecipeBrowserLayout.FOOTER_Y + 2, true), "and not in a foot it does not have");
        assertTrue(RecipeBrowserLayout.isOnGroupNext(RecipeBrowserLayout.GROUP_NEXT_X + 2,
                RecipeBrowserLayout.GROUP_Y + 2), "the arrows of the head walk the groups");
        assertTrue(RecipeBrowserLayout.isOnGroupPrevious(RecipeBrowserLayout.GROUP_PREV_X + 2,
                RecipeBrowserLayout.GROUP_Y + 2));
    }

    @Test
    void theScrollBarAsksForTheRowThePlayerClicks() {
        assertEquals(RecipeBrowserLayout.SCROLL_Y, RecipeBrowserLayout.scrollThumbY(0, 8),
                "a list at its first row has its thumb at the top");
        assertEquals(0, RecipeBrowserLayout.rowAtScrollY(RecipeBrowserLayout.SCROLL_Y, 8));
        assertEquals(8, RecipeBrowserLayout.rowAtScrollY(
                RecipeBrowserLayout.SCROLL_Y + RecipeBrowserLayout.SCROLL_HEIGHT, 8),
                "a click at the lower end asks for the last row");
    }

    @Test
    void theReportOfARecipeStandsInTheRowsTheInventoryOfAMachineStandsIn() {
        assertTrue(RecipeBrowserLayout.TEMPLATE_INFO_Y >= MachineMenu.FOOT_TOP,
                "the report begins below the body of the machine and not over its own slots");
        assertTrue(RecipeBrowserLayout.TEMPLATE_INFO_Y >= MachineMenu.MACHINE_TOP,
                "and below the band the name of the machine is written in");
        assertTrue(RecipeBrowserLayout.templateReportFits(),
                "every line of the report lies inside the panel of the machine");
        assertEquals(RecipeBrowserLayout.TEMPLATE_LINE_HEIGHT,
                RecipeBrowserLayout.templateLineY(1) - RecipeBrowserLayout.templateLineY(0),
                "the lines of the report are laid out one after the other");
    }

    @Test
    void thePageAndTheTwoButtonsAreNamedRightOfTheReportAndNeverOverIt() {
        assertTrue(RecipeBrowserLayout.TEMPLATE_INFO_X + RecipeBrowserLayout.TEMPLATE_INFO_WIDTH
                        <= RecipeBrowserLayout.TEMPLATE_HINT_X,
                "the report ends before the column of the page");
        assertTrue(RecipeBrowserLayout.TEMPLATE_HINT_X + RecipeBrowserLayout.TEMPLATE_HINT_WIDTH
                        + RecipeBrowserLayout.PADDING <= RecipeBrowserLayout.WIDTH,
                "the column of the page ends inside the panel");
        assertTrue(RecipeBrowserLayout.TEMPLATE_HINT_X > RecipeBrowserLayout.TEMPLATE_INFO_X,
                "the page is named to the right of the report");
    }
}
