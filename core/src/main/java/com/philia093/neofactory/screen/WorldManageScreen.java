package com.philia093.neofactory.screen;

import com.badlogic.gdx.Input;
import com.philia093.neofactory.NeoFactoryGame;
import com.philia093.neofactory.gui.MenuLayout;
import com.philia093.neofactory.gui.widget.ButtonWidget;
import com.philia093.neofactory.gui.widget.LabelWidget;
import com.philia093.neofactory.gui.widget.TextFieldWidget;
import com.philia093.neofactory.world.save.SaveException;
import com.philia093.neofactory.world.save.SaveSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Confirms a deletion or renames a world.
 * <p>
 * The screen works on the world the {@link ScreenManager} was asked to manage, see
 * {@link ScreenManager#manageWorld(SaveSummary, ScreenManager.ManageMode)}, and it
 * has two shapes:
 * <ul>
 *     <li>{@code DELETE} - the name of the world, the question whether it should
 *         really be removed and the buttons {@code Yes} and {@code Cancel}.</li>
 *     <li>{@code RENAME} - a text field holding the current name and a button that
 *         writes the new one.</li>
 * </ul>
 * Deleting throws the folder of the world away, so the question is worth asking.
 * Renaming only rewrites the header of its file, see
 * {@link com.philia093.neofactory.world.save.WorldStorage#rename(SaveSummary, String)}.
 */
public class WorldManageScreen extends WidgetScreen {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Heading shown while a world is deleted. */
    private static final String DELETE_TITLE = "Delete World";

    /** Heading shown while a world is renamed. */
    private static final String RENAME_TITLE = "Rename World";

    /** Message shown before a world is deleted. */
    private static final String DELETE_QUESTION = "Delete this world? It cannot be undone.";

    /** Label above the name field. */
    private static final String NAME_LABEL = "New Name";

    /** Text of the button that confirms a deletion. */
    private static final String YES = "Yes";

    /** Text of the button that confirms a rename. */
    private static final String SAVE = "Save";

    /** Text of the button that goes back without changing anything. */
    private static final String CANCEL = "Cancel";

    private final LabelWidget heading;
    private final LabelWidget worldName;
    private final LabelWidget message;
    private final LabelWidget nameLabel;
    private final TextFieldWidget nameField;
    private final ButtonWidget confirm;
    private final ButtonWidget cancel;

    /** {@code true} while the screen asks about a deletion. */
    private boolean deleting;

    /**
     * Creates the screen.
     *
     * @param game game instance owning this screen
     */
    public WorldManageScreen(NeoFactoryGame game) {
        super(game);
        this.heading = addWidget(new LabelWidget(game.font(), DELETE_TITLE, true));
        this.worldName = addWidget(new LabelWidget(game.font(), "", true));
        this.message = addWidget(new LabelWidget(game.font(), DELETE_QUESTION, true));
        this.nameLabel = addWidget(new LabelWidget(game.font(), NAME_LABEL, false));
        this.nameField = addWidget(new TextFieldWidget(game.font(), widgetStyle()));
        this.confirm = addWidget(new ButtonWidget(game.font(), widgetStyle(), YES, this::confirm));
        this.cancel = addWidget(new ButtonWidget(game.font(), widgetStyle(), CANCEL, this::back));
        nameField.setOnSubmit(this::confirm);
    }

    @Override
    public void show() {
        super.show();
        SaveSummary summary = game.screens().managedWorld();
        deleting = game.screens().manageMode() == ScreenManager.ManageMode.DELETE;

        heading.setText(deleting ? DELETE_TITLE : RENAME_TITLE);
        worldName.setText(summary == null ? "" : summary.displayName());
        message.setVisible(deleting);
        nameLabel.setVisible(!deleting);
        nameField.setVisible(!deleting);
        confirm.setLabel(deleting ? YES : SAVE);
        if (!deleting && summary != null) {
            nameField.setText(summary.displayName());
        }
    }

    /** Applies the action the screen was opened for. */
    private void confirm() {
        SaveSummary summary = game.screens().managedWorld();
        if (summary == null) {
            back();
            return;
        }
        if (deleting) {
            deleteWorld(summary);
        } else {
            renameWorld(summary);
        }
    }

    /** Removes the world from the save folder. */
    private void deleteWorld(SaveSummary summary) {
        boolean removed = game.screens().storage().delete(summary);
        LOGGER.info("Deleting world '{}': {}", summary.displayName(),
                removed ? "done" : "the folder is still there");
        back();
    }

    /** Writes a new name into the stored world. */
    private void renameWorld(SaveSummary summary) {
        String name = nameField.text();
        if (name.isBlank()) {
            back();
            return;
        }
        try {
            SaveSummary updated = game.screens().storage().rename(summary, name);
            LOGGER.info("World '{}' is now called '{}'", summary.displayName(),
                    updated.displayName());
        } catch (SaveException e) {
            LOGGER.error("World '{}' could not be renamed", summary.displayName(), e);
        }
        back();
    }

    /** Returns to the world list. */
    private void back() {
        game.screens().show(ScreenManager.ScreenType.WORLD_SELECT);
    }

    @Override
    protected boolean onKeyDown(int keyCode) {
        if (keyCode == Input.Keys.ESCAPE) {
            back();
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
        float fieldX = MenuLayout.centeredX(width, MenuLayout.FIELD_WIDTH);

        heading.layout(buttonX, height - smallHeight * 3.0f, MenuLayout.BUTTON_WIDTH, smallHeight);
        worldName.layout(buttonX, height * 0.62f, MenuLayout.BUTTON_WIDTH, smallHeight);
        message.layout(MenuLayout.centeredX(width, 420.0f), height * 0.50f, 420.0f, smallHeight);

        float fieldY = height * 0.46f;
        nameLabel.layout(fieldX, MenuLayout.labelY(fieldY, MenuLayout.FIELD_HEIGHT, smallHeight),
                MenuLayout.FIELD_WIDTH, smallHeight);
        nameField.layout(fieldX, fieldY, MenuLayout.FIELD_WIDTH, MenuLayout.FIELD_HEIGHT);

        float buttonsCenter = height * 0.26f;
        confirm.layout(buttonX, MenuLayout.stackY(buttonsCenter, 0, 2),
                MenuLayout.BUTTON_WIDTH, MenuLayout.BUTTON_HEIGHT);
        cancel.layout(buttonX, MenuLayout.stackY(buttonsCenter, 1, 2),
                MenuLayout.BUTTON_WIDTH, MenuLayout.BUTTON_HEIGHT);
    }

    @Override
    protected void drawContent(float delta, float mouseX, float mouseY) {
        drawMenuBackground();
    }
}
