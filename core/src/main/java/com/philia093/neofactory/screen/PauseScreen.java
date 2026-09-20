package com.philia093.neofactory.screen;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.NeoFactoryGame;
import com.philia093.neofactory.gui.MenuLayout;
import com.philia093.neofactory.gui.widget.ButtonWidget;
import com.philia093.neofactory.gui.widget.LabelWidget;
import com.philia093.neofactory.world.save.SaveSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Menu shown on top of the world the player is in.
 * <p>
 * The world itself is drawn by the playable screen, see
 * {@link GameScreen#renderFrozen(float)}: the pause menu asks it to draw one frame
 * and then lays a dark layer and its buttons on top of it. That is what makes the
 * world stay visible behind the menu while its simulation is stopped, which is how
 * the original game feels.
 * <p>
 * {@code Back to Game} returns to the world, {@code Save} writes it right away and
 * {@code Quit to Title} saves it and leaves it.
 */
public class PauseScreen extends WidgetScreen {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Heading of the menu. */
    private static final String TITLE = "Game Menu";

    /** Text of the button that returns to the world. */
    private static final String BACK_TO_GAME = "Back to Game";

    /** Text of the button that writes the world right away. */
    private static final String SAVE = "Save";

    /** Text of the button that saves the world and leaves it. */
    private static final String QUIT = "Save and Quit to Title";

    /** Colour of the layer that darkens the world behind the menu. */
    private static final Color SHADE = new Color(0.0f, 0.0f, 0.0f, 0.6f);

    /** Message shown after the world was written. */
    private static final String SAVED = "World saved";

    /** Message shown while the world could not be written. */
    private static final String SAVE_FAILED = "World could not be saved, see the log";

    private final LabelWidget heading;
    private final LabelWidget status;
    private final ButtonWidget backToGame;
    private final ButtonWidget save;
    private final ButtonWidget quit;

    /**
     * Creates the menu.
     *
     * @param game game instance owning this screen
     */
    public PauseScreen(NeoFactoryGame game) {
        super(game);
        this.heading = addWidget(new LabelWidget(game.font(), TITLE, true));
        this.status = addWidget(new LabelWidget(game.font(), "", true));
        this.backToGame = addWidget(new ButtonWidget(game.font(), widgetStyle(), BACK_TO_GAME,
                this::backToGame));
        this.save = addWidget(new ButtonWidget(game.font(), widgetStyle(), SAVE, this::saveWorld));
        this.quit = addWidget(new ButtonWidget(game.font(), widgetStyle(), QUIT, this::quitToTitle));
    }

    @Override
    public void show() {
        super.show();
        status.setText("");
    }

    /** Returns to the running world. */
    private void backToGame() {
        game.screens().resumeWorld();
    }

    /** Writes the world without leaving it. */
    private void saveWorld() {
        GameScreen world = game.screens().gameScreen();
        if (world == null) {
            return;
        }
        try {
            world.save();
            status.setText(SAVED);
        } catch (RuntimeException e) {
            LOGGER.error("Unable to save the world", e);
            status.setText(SAVE_FAILED);
        }
    }

    /** Saves the world and goes back to the title screen. */
    private void quitToTitle() {
        game.screens().leaveWorld();
    }

    @Override
    protected boolean onKeyDown(int keyCode) {
        if (keyCode == Input.Keys.ESCAPE) {
            backToGame();
            return true;
        }
        return false;
    }

    @Override
    protected void drawContent(float delta, float mouseX, float mouseY) {
        GameScreen world = game.screens().gameScreen();
        if (world != null) {
            // The world is frozen, but it stays visible behind the menu.
            world.renderFrozen(delta);
        }
        TextureRegion pixel = widgetStyle().whitePixel();
        if (pixel != null) {
            batch().setColor(SHADE);
            batch().draw(pixel, 0.0f, 0.0f, uiViewport.guiWidth(), uiViewport.guiHeight());
            batch().setColor(Color.WHITE);
        }
    }

    @Override
    protected void layoutWidgets() {
        float width = uiViewport.guiWidth();
        float height = uiViewport.guiHeight();
        float smallHeight = game.font().glyphHeight();
        float buttonX = MenuLayout.centeredX(width, MenuLayout.BUTTON_WIDTH);
        SaveSummary world = game.screens().activeWorld();

        heading.layout(buttonX, height * 0.76f, MenuLayout.BUTTON_WIDTH, smallHeight);
        status.layout(MenuLayout.centeredX(width, 420.0f), height * 0.64f, 420.0f, smallHeight);

        float buttonsCenter = height * 0.42f;
        backToGame.layout(buttonX, MenuLayout.stackY(buttonsCenter, 0, 3),
                MenuLayout.BUTTON_WIDTH, MenuLayout.BUTTON_HEIGHT);
        save.layout(buttonX, MenuLayout.stackY(buttonsCenter, 1, 3),
                MenuLayout.BUTTON_WIDTH, MenuLayout.BUTTON_HEIGHT);
        quit.layout(buttonX, MenuLayout.stackY(buttonsCenter, 2, 3),
                MenuLayout.BUTTON_WIDTH, MenuLayout.BUTTON_HEIGHT);

        if (world != null) {
            heading.setText(TITLE + " - " + world.displayName());
        }
    }
}
