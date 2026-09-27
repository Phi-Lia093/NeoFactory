import com.philia093.neofactory.gui.MenuLayout;

/**
 * Checks the geometry of the menus.
 * <p>
 * The world list menu shares its screen with a list, so the buttons and the list have
 * to be laid out so that they never overlap, at every window size the game may be
 * played in. The numbers are the ones {@code WorldSelectScreen} uses.
 */
public final class MenuLayoutHarness {

    /** Amount of checks that passed. */
    private static int passed;

    /** Amount of checks that failed. */
    private static int failed;

    private MenuLayoutHarness() {
        // Utility class: never instantiated.
    }

    /**
     * Runs every check.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        int[] widths = {320, 426, 640, 1280};
        int[] heights = {240, 266, 300, 400, 480, 800};
        for (int height : heights) {
            for (int width : widths) {
                selectScreen(width, height);
                stackedScreen(width, height, 3);
                stackedScreen(width, height, 2);
            }
        }
        System.out.println("MenuLayoutHarness: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** Mirrors the layout of {@code WorldSelectScreen}. */
    private static void selectScreen(float width, float height) {
        String where = "select " + (int) width + "x" + (int) height;
        float glyph = 9.0f;
        float count = 5.0f;
        float center = height * 0.26f;
        float top = MenuLayout.stackTop(center, (int) count, MenuLayout.SHORT_BUTTON_HEIGHT,
                MenuLayout.SHORT_BUTTON_GAP);
        float bottom = MenuLayout.stackY(center, 4, (int) count, MenuLayout.SHORT_BUTTON_HEIGHT,
                MenuLayout.SHORT_BUTTON_GAP);
        float listBottom = top + glyph;
        float listTop = height - glyph * 4.5f;
        float listHeight = Math.max(glyph * 2.0f, listTop - listBottom);

        check("the stack starts inside the interface: " + where + " bottom=" + bottom, bottom > 0.0f);
        check("the stack ends inside the interface: " + where + " top=" + top, top < height);
        check("the buttons stay below the heading: " + where, top < height - glyph * 3.0f);
        check("the list sits above the buttons: " + where + " list=" + listBottom + ".."
                + (listBottom + listHeight) + " buttons=" + bottom + ".." + top,
                listBottom > top);
        check("the list has at least two rows: " + where + " height=" + listHeight,
                listHeight >= 2.0f * glyph);
        check("the list ends below the heading: " + where, listBottom + listHeight <= height - glyph * 3.0f);
        check("the buttons grow downwards from the top of the stack: " + where,
                MenuLayout.stackY(center, 0, (int) count, MenuLayout.SHORT_BUTTON_HEIGHT,
                        MenuLayout.SHORT_BUTTON_GAP) > bottom);
    }

    /** Mirrors the layout of the title, pause and form screens. */
    private static void stackedScreen(float width, float height, int count) {
        String where = "stack of " + count + " at " + (int) width + "x" + (int) height;
        float center = height * 0.34f;
        float top = MenuLayout.stackTop(center, count, MenuLayout.BUTTON_HEIGHT, MenuLayout.BUTTON_GAP);
        float bottom = MenuLayout.stackY(center, count - 1, count);
        check("the stack stays inside the interface: " + where + " " + bottom + ".." + top,
                bottom >= 0.0f && top <= height);
        check("the buttons are as tall as the pictures: " + where,
                Math.abs(MenuLayout.stackHeight(count)
                        - (count * MenuLayout.BUTTON_HEIGHT + (count - 1) * MenuLayout.BUTTON_GAP)) < 0.001f);
        check("a short button is flatter than a full one: " + where,
                MenuLayout.SHORT_BUTTON_HEIGHT < MenuLayout.BUTTON_HEIGHT);
        check("the short label fits its box: " + where,
                MenuLayout.SHORT_BUTTON_SCALE * MenuLayout.BUTTON_HEIGHT
                        <= MenuLayout.SHORT_BUTTON_HEIGHT + 0.001f);
    }

    /**
     * Records one check.
     *
     * @param what description of what is checked
     * @param ok result of the check
     */
    private static void check(String what, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAILED: " + what);
        }
    }
}
