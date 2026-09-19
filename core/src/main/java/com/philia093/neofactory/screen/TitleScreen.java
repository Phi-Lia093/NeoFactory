package com.philia093.neofactory.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.philia093.neofactory.NeoFactoryGame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main menu of the game.
 * <p>
 * The title screen is intentionally minimal for now: it only shows a hint and
 * waits for a key press before handing control to the playable screen. Widgets,
 * the panorama background and a settings menu can be added on top of the stage
 * that this screen already owns.
 */
public class TitleScreen extends NeoFactoryScreen {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Text shown in the middle of the window until a real menu exists. */
    private static final String HINT = "NeoFactory - press SPACE or ENTER to start";

    /**
     * Creates the title screen.
     *
     * @param game game instance owning this screen
     */
    public TitleScreen(NeoFactoryGame game) {
        super(game);
    }

    @Override
    public void render(float delta) {
        clearScreen();

        boolean startRequested = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER);
        if (startRequested) {
            LOGGER.info("Start requested from the title screen");
            game.screens().show(ScreenManager.ScreenType.GAME);
            return;
        }

        stage.act(delta);
        stage.draw();
    }

    /** Text currently displayed by the menu, exposed for future UI work. */
    public String hint() {
        return HINT;
    }
}