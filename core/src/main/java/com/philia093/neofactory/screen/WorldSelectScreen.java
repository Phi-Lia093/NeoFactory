package com.philia093.neofactory.screen;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.philia093.neofactory.NeoFactoryGame;
import com.philia093.neofactory.gui.MenuLayout;
import com.philia093.neofactory.gui.widget.ButtonWidget;
import com.philia093.neofactory.gui.widget.LabelWidget;
import com.philia093.neofactory.gui.widget.ListEntry;
import com.philia093.neofactory.gui.widget.ScrollListWidget;
import com.philia093.neofactory.gui.widget.Widget;
import com.philia093.neofactory.gui.widget.WidgetStyle;
import com.philia093.neofactory.render.PixelFont;
import com.philia093.neofactory.world.save.SaveException;
import com.philia093.neofactory.world.save.SaveSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Lists the stored worlds and opens, creates, renames or deletes them.
 * <p>
 * The screen is the single player menu of the original game: the worlds scroll past
 * in the middle, the buttons below work on the selected one. A button that cannot
 * do anything yet - because no world is selected - is greyed out instead of being
 * hidden, so the menu always looks the same.
 * <p>
 * The list is read from the save folder every time the screen is shown, which keeps
 * it correct after a world was created, renamed or deleted.
 */
public class WorldSelectScreen extends WidgetScreen {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Heading of the screen. */
    private static final String TITLE = "Select World";

    /** Text of the button that opens the selected world. */
    private static final String PLAY = "Play Selected World";

    /** Text of the button that leads to the creation form. */
    private static final String CREATE = "Create New World";

    /** Text of the button that asks for a rename. */
    private static final String RENAME = "Rename";

    /** Text of the button that asks for a deletion. */
    private static final String DELETE = "Delete";

    /** Text of the button that returns to the title screen. */
    private static final String BACK = "Back";

    /** Text shown while no world was created yet. */
    private static final String NO_WORLDS = "No worlds yet - create one to start playing";

    /** Pattern the last played time is shown with. */
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm";

    /** Height factor the stack of buttons is centred on. */
    private static final float BUTTONS_CENTER = 0.26f;

    /** Amount of buttons that stand below the list. */
    private static final int BUTTON_COUNT = 5;

    private final LabelWidget heading;
    private final ScrollListWidget list;
    private final ButtonWidget play;
    private final ButtonWidget create;
    private final ButtonWidget rename;
    private final ButtonWidget delete;
    private final ButtonWidget back;
    private final LabelWidget empty;
    private final LabelWidget status;

    /** Worlds that are currently in the save folder. */
    private final List<SaveSummary> worlds = new ArrayList<>();

    /** Formatter used for the second line of a row. */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN, Locale.ROOT);

    /**
     * Creates the world list screen.
     *
     * @param game game instance owning this screen
     */
    public WorldSelectScreen(NeoFactoryGame game) {
        super(game);
        this.heading = addWidget(new LabelWidget(game.font(), TITLE, true));
        this.list = addWidget(new ScrollListWidget(game.font(), widgetStyle()));
        this.empty = addWidget(new LabelWidget(game.font(), NO_WORLDS, true));
        this.status = addWidget(new LabelWidget(game.font(), "", true));
        this.status.color().set(WidgetStyle.SECONDARY_TEXT_COLOR);

        this.play = addWidget(new ButtonWidget(game.font(), widgetStyle(), PLAY, this::playSelected));
        this.create = addWidget(new ButtonWidget(game.font(), widgetStyle(), CREATE,
                this::openCreationForm));
        this.rename = addWidget(new ButtonWidget(game.font(), widgetStyle(), RENAME,
                this::renameSelected));
        this.delete = addWidget(new ButtonWidget(game.font(), widgetStyle(), DELETE,
                this::deleteSelected));
        this.back = addWidget(new ButtonWidget(game.font(), widgetStyle(), BACK,
                this::backToTitle));

        // A double click on a row opens that world, like in the original game.
        this.list.setOnActivate(this::playSelected);
    }

    @Override
    public void show() {
        super.show();
        refresh();
    }

    /** Reads the save folder and fills the list. */
    private void refresh() {
        worlds.clear();
        worlds.addAll(game.screens().storage().list());
        List<ListEntry> rows = new ArrayList<>(worlds.size());
        for (SaveSummary summary : worlds) {
            rows.add(new WorldRow(summary));
        }
        list.setEntries(rows);
        empty.setVisible(worlds.isEmpty());
        updateButtons();
    }

    /** Enables the buttons that need a selected world. */
    private void updateButtons() {
        boolean hasSelection = selected() != null;
        play.setEnabled(hasSelection);
        rename.setEnabled(hasSelection);
        delete.setEnabled(hasSelection);
    }

    /** Save game of the selected row, {@code null} while nothing is selected. */
    private SaveSummary selected() {
        int index = list.selectedIndex();
        return index >= 0 && index < worlds.size() ? worlds.get(index) : null;
    }

    /** Opens the selected world. */
    private void playSelected() {
        SaveSummary summary = selected();
        if (summary == null) {
            return;
        }
        LOGGER.info("Opening world '{}'", summary.displayName());
        try {
            game.screens().loadWorld(summary);
        } catch (SaveException e) {
            // The damaged file was moved aside by the loader, the list is reloaded.
            LOGGER.error("World '{}' could not be opened", summary.displayName(), e);
            status.setText("World could not be opened, the damaged file was moved aside");
            refresh();
        }
    }

    /** Shows the form that creates a world. */
    private void openCreationForm() {
        game.screens().show(ScreenManager.ScreenType.WORLD_CREATE);
    }

    /** Asks the manage screen to confirm a deletion. */
    private void deleteSelected() {
        SaveSummary summary = selected();
        if (summary != null) {
            game.screens().manageWorld(summary, ScreenManager.ManageMode.DELETE);
        }
    }

    /** Asks the manage screen to rename the world. */
    private void renameSelected() {
        SaveSummary summary = selected();
        if (summary != null) {
            game.screens().manageWorld(summary, ScreenManager.ManageMode.RENAME);
        }
    }

    /** Returns to the title screen. */
    private void backToTitle() {
        game.screens().show(ScreenManager.ScreenType.TITLE);
    }

    @Override
    protected boolean onKeyDown(int keyCode) {
        if (keyCode == Input.Keys.ESCAPE) {
            backToTitle();
            return true;
        }
        return false;
    }

    @Override
    protected void layoutWidgets() {
        float width = uiViewport.guiWidth();
        float height = uiViewport.guiHeight();
        float smallHeight = game.font().glyphHeight();
        float buttonX = MenuLayout.centeredX(width, MenuLayout.BUTTON_WIDTH);
        float listWidth = Math.min(width - 20.0f, 420.0f);

        heading.layout(buttonX, height - smallHeight * 3.0f, MenuLayout.BUTTON_WIDTH, smallHeight);
        layoutButton(play, 0);
        layoutButton(create, 1);
        layoutButton(rename, 2);
        layoutButton(delete, 3);
        layoutButton(back, 4);

        // The list takes the room left between the heading and the buttons instead of a
        // share of the height. A share would let the buttons reach into the list at a
        // small interface and hide the worlds behind them.
        float listBottom = MenuLayout.stackTop(height * BUTTONS_CENTER, BUTTON_COUNT,
                MenuLayout.SHORT_BUTTON_HEIGHT, MenuLayout.SHORT_BUTTON_GAP) + smallHeight;
        float listTop = height - smallHeight * 4.5f;
        float listHeight = Math.max(smallHeight * 2.0f, listTop - listBottom);
        list.layout(MenuLayout.centeredX(width, listWidth), listBottom, listWidth, listHeight);
        empty.layout(MenuLayout.centeredX(width, 320.0f),
                listBottom + (listHeight - smallHeight) * 0.5f, 320.0f, smallHeight);
        status.layout(buttonX, listTop + smallHeight * 0.5f, MenuLayout.BUTTON_WIDTH, smallHeight);
    }

    /**
     * Places one button of the stack at the bottom of the screen.
     * <p>
     * The buttons are flatter than a full sized menu button, see
     * {@link MenuLayout#SHORT_BUTTON_HEIGHT}, and their labels are scaled down with them
     * so that the whole stack fits below the list of worlds.
     *
     * @param button button to place
     * @param index position in the stack, {@code 0} is the topmost button
     */
    private void layoutButton(ButtonWidget button, int index) {
        button.setScale(MenuLayout.SHORT_BUTTON_SCALE);
        button.layout(MenuLayout.centeredX(uiViewport.guiWidth(), MenuLayout.BUTTON_WIDTH),
                MenuLayout.stackY(uiViewport.guiHeight() * BUTTONS_CENTER, index, BUTTON_COUNT,
                        MenuLayout.SHORT_BUTTON_HEIGHT, MenuLayout.SHORT_BUTTON_GAP),
                MenuLayout.BUTTON_WIDTH, MenuLayout.SHORT_BUTTON_HEIGHT);
    }

    @Override
    protected void onWidgetPressed(Widget widget) {
        // The selection changed, so the buttons have to follow it.
        updateButtons();
    }

    @Override
    protected void drawContent(float delta, float mouseX, float mouseY) {
        drawMenuBackground();
    }

    /** One row of the world list. */
    private final class WorldRow implements ListEntry {

        private final SaveSummary summary;

        private WorldRow(SaveSummary summary) {
            this.summary = summary;
        }

        @Override
        public void render(SpriteBatch batch, PixelFont font, float x, float y, float width,
                float height, boolean hovered, boolean selected) {
            float previous = font.scale();
            font.setColor(selected ? WidgetStyle.SELECTED_TEXT_COLOR : WidgetStyle.TEXT_COLOR);
            float glyphHeight = font.glyphHeight();
            font.drawShadowed(batch, summary.displayName(), x, y + height - glyphHeight);
            font.setColor(WidgetStyle.SECONDARY_TEXT_COLOR);
            font.drawShadowed(batch, describe(), x, y + height - glyphHeight * 3.0f);
            font.setScale(previous);
        }

        /** Second line of the row: when the world was played and how big it is. */
        private String describe() {
            String played = dateFormat.format(new Date(summary.lastPlayed()));
            long kilobytes = Math.max(1L, summary.sizeBytes() / 1024L);
            return played + "   " + kilobytes + " KB";
        }

        @Override
        public String label() {
            return summary.displayName();
        }
    }
}
