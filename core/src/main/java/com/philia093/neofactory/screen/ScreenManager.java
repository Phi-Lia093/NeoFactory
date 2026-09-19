package com.philia093.neofactory.screen;

import com.badlogic.gdx.Screen;
import com.philia093.neofactory.NeoFactoryGame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.Map;

/**
 * Creates and switches between the screens of the game.
 * <p>
 * Screens are created lazily on first use and cached afterwards. The manager
 * replaces the previous screen through {@link com.badlogic.gdx.Game#setScreen},
 * which also disposes a screen that is not needed any more once that behaviour
 * is enabled.
 */
public class ScreenManager {

    /** Every screen the game currently knows about. */
    public enum ScreenType {
        /** Main menu, not implemented yet. */
        TITLE,
        /** The playable world. */
        GAME
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private final NeoFactoryGame game;
    private final Map<ScreenType, Screen> screens = new EnumMap<>(ScreenType.class);

    private ScreenType currentType;

    /**
     * Creates a manager bound to a game.
     *
     * @param game game instance used to install the screens
     */
    public ScreenManager(NeoFactoryGame game) {
        this.game = game;
    }

    /**
     * Shows the requested screen, creating it when it is used for the first time.
     *
     * @param type screen to show
     * @return the shown screen, never {@code null}
     */
    public Screen show(ScreenType type) {
        if (currentType == type && game.getScreen() != null) {
            return game.getScreen();
        }
        Screen screen = screens.get(type);
        if (screen == null) {
            screen = create(type);
            screens.put(type, screen);
            LOGGER.info("Created screen {}", type);
        }
        currentType = type;
        game.setScreen(screen);
        LOGGER.info("Switched to screen {}", type);
        return screen;
    }

    /** Screen that is currently visible, may be {@code null} before startup. */
    public ScreenType currentType() {
        return currentType;
    }

    /**
     * Returns the game screen, creating it when necessary.
     *
     * @return the playable screen
     */
    public GameScreen gameScreen() {
        return (GameScreen) show(ScreenType.GAME);
    }

    /**
     * Disposes every screen that was created.
     * <p>
     * Called when the application shuts down, screens are not disposed while the
     * game is running because the player may return to them.
     */
    public void dispose() {
        for (Screen screen : screens.values()) {
            screen.dispose();
        }
        screens.clear();
        currentType = null;
        LOGGER.info("Disposed all screens");
    }

    /** Instantiates a screen of the given type. */
    private Screen create(ScreenType type) {
        switch (type) {
            case TITLE:
                return new TitleScreen(game);
            case GAME:
            default:
                return new GameScreen(game);
        }
    }
}