package com.philia093.neofactory.screen;

import com.badlogic.gdx.Input;
import com.philia093.neofactory.NeoFactoryGame;
import com.philia093.neofactory.gui.MenuLayout;
import com.philia093.neofactory.gui.widget.ButtonWidget;
import com.philia093.neofactory.gui.widget.LabelWidget;
import com.philia093.neofactory.gui.widget.TextFieldWidget;
import com.philia093.neofactory.world.WorldType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

/**
 * Form that creates a new world.
 * <p>
 * The player enters a name and, if wanted, a seed. An empty seed means "surprise
 * me": a random number is used, which is what the original game does. Any other
 * text is turned into a number - a plain number is used as it is and a word is
 * hashed, so a seed like {@code forest} gives the same world every time.
 * <p>
 * The land of the world is picked with one button, see {@link WorldType}: the generated landscape of
 * the seed, or the table of blocks a flat world is. The choice is part of the world and cannot be
 * changed later, so a form that is opened again starts at the generated landscape - the world the
 * game is meant to be played in.
 * <p>
 * The form is applied with the button or with the enter key, because enter submits
 * the field that is focused.
 */
public class WorldCreateScreen extends WidgetScreen {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Heading of the screen. */
    private static final String TITLE = "Create New World";

    /** Label above the name field. */
    private static final String NAME_LABEL = "World Name";

    /** Label above the seed field. */
    private static final String SEED_LABEL = "World Seed";

    /** Label above the button that switches the land of the world. */
    private static final String TYPE_LABEL = "World Type";

    /** Hint shown while the name field is empty, also the fallback name. */
    private static final String NAME_HINT = "New World";

    /** Hint shown while the seed field is empty. */
    private static final String SEED_HINT = "Leave empty for a random seed";

    /** Text of the button that creates the world. */
    private static final String CREATE = "Create New World";

    /** Text of the button that returns to the world list. */
    private static final String CANCEL = "Cancel";

    private final LabelWidget heading;
    private final LabelWidget nameLabel;
    private final TextFieldWidget nameField;
    private final LabelWidget seedLabel;
    private final TextFieldWidget seedField;
    private final LabelWidget typeLabel;
    private final ButtonWidget typeButton;
    private final ButtonWidget create;
    private final ButtonWidget cancel;

    /** Land the new world is made of, the generated landscape until the player says otherwise. */
    private WorldType type = WorldType.NORMAL;

    /**
     * Creates the form.
     *
     * @param game game instance owning this screen
     */
    public WorldCreateScreen(NeoFactoryGame game) {
        super(game);
        this.heading = addWidget(new LabelWidget(game.font(), TITLE, true));
        this.nameLabel = addWidget(new LabelWidget(game.font(), NAME_LABEL, false));
        this.nameField = addWidget(new TextFieldWidget(game.font(), widgetStyle()));
        this.seedLabel = addWidget(new LabelWidget(game.font(), SEED_LABEL, false));
        this.seedField = addWidget(new TextFieldWidget(game.font(), widgetStyle()));
        this.typeLabel = addWidget(new LabelWidget(game.font(), TYPE_LABEL, false));
        this.typeButton = addWidget(new ButtonWidget(game.font(), widgetStyle(), type.displayName(),
                this::toggleType));
        this.create = addWidget(new ButtonWidget(game.font(), widgetStyle(), CREATE,
                this::createWorld));
        this.cancel = addWidget(new ButtonWidget(game.font(), widgetStyle(), CANCEL, this::back));

        nameField.setHint(NAME_HINT);
        seedField.setHint(SEED_HINT);
        nameField.setOnSubmit(this::createWorld);
        seedField.setOnSubmit(this::createWorld);
    }

    @Override
    public void show() {
        super.show();
        // A fresh form every time, so an old name is not created twice by accident and nobody creates
        // the flat world they picked an hour ago without noticing.
        nameField.clear();
        seedField.clear();
        type = WorldType.NORMAL;
        refreshTypeButton();
    }

    /** Switches the land of the new world between the generated landscape and a flat table. */
    private void toggleType() {
        type = type == WorldType.NORMAL ? WorldType.FLAT : WorldType.NORMAL;
        refreshTypeButton();
    }

    /** Shows the chosen land on the button. */
    private void refreshTypeButton() {
        typeButton.setLabel(type.displayName());
    }

    /** Creates the world and opens it. */
    private void createWorld() {
        String entered = nameField.text();
        String name = entered.isBlank() ? NAME_HINT : entered;
        int seed = seedFrom(seedField.text());
        LOGGER.info("Creating world '{}' with seed {} and {} land", name, seed, type.typeName());
        game.screens().createWorld(name, seed, type);
    }

    /**
     * Turns the entered seed into a number.
     *
     * @param text text the player typed, may be empty
     * @return a random seed for an empty field, the number for a number, and a hash
     *         of the text for everything else
     */
    static int seedFrom(String text) {
        if (text == null || text.isBlank()) {
            return new Random().nextInt();
        }
        String trimmed = text.trim();
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            // A word is a seed as well, the same word always gives the same world.
            return trimmed.hashCode();
        }
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

        float nameY = height * 0.62f;
        nameLabel.layout(fieldX, MenuLayout.labelY(nameY, MenuLayout.FIELD_HEIGHT, smallHeight),
                MenuLayout.FIELD_WIDTH, smallHeight);
        nameField.layout(fieldX, nameY, MenuLayout.FIELD_WIDTH, MenuLayout.FIELD_HEIGHT);

        float seedY = height * 0.44f;
        seedLabel.layout(fieldX, MenuLayout.labelY(seedY, MenuLayout.FIELD_HEIGHT, smallHeight),
                MenuLayout.FIELD_WIDTH, smallHeight);
        seedField.layout(fieldX, seedY, MenuLayout.FIELD_WIDTH, MenuLayout.FIELD_HEIGHT);

        float typeY = height * 0.30f;
        typeLabel.layout(fieldX, MenuLayout.labelY(typeY, MenuLayout.BUTTON_HEIGHT, smallHeight),
                MenuLayout.FIELD_WIDTH, smallHeight);
        typeButton.layout(fieldX, typeY, MenuLayout.FIELD_WIDTH, MenuLayout.BUTTON_HEIGHT);

        float buttonsCenter = height * 0.24f;
        create.layout(buttonX, MenuLayout.stackY(buttonsCenter, 0, 2),
                MenuLayout.BUTTON_WIDTH, MenuLayout.BUTTON_HEIGHT);
        cancel.layout(buttonX, MenuLayout.stackY(buttonsCenter, 1, 2),
                MenuLayout.BUTTON_WIDTH, MenuLayout.BUTTON_HEIGHT);
    }

    @Override
    protected void drawContent(float delta, float mouseX, float mouseY) {
        drawMenuBackground();
    }
}
