package com.philia093.neofactory.screen;

import com.badlogic.gdx.Screen;
import com.philia093.neofactory.NeoFactoryGame;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.save.LevelData;
import com.philia093.neofactory.world.save.SaveSummary;
import com.philia093.neofactory.world.save.WorldLoader;
import com.philia093.neofactory.world.save.WorldStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.Map;

/**
 * Creates and switches between the screens of the game and owns the world that is
 * currently loaded.
 * <p>
 * Most screens are created once and then reused, which is why the menu screens are
 * cached. The playable screen is the exception: it belongs to one save game, so it
 * is created when a world is opened and thrown away when the player leaves it. The
 * manager keeps it in {@link #gameScreen()} together with the summary it came from.
 * <p>
 * Leaving a world always saves it first, which is why the pause menu and the world
 * list both go through {@link #leaveWorld()}.
 */
public class ScreenManager {

    /** Every screen the game knows about. */
    public enum ScreenType {
        /** Main menu with the panorama and the two buttons. */
        TITLE,
        /** List of stored worlds. */
        WORLD_SELECT,
        /** Form that creates a new world. */
        WORLD_CREATE,
        /** Small screen that confirms a deletion or renames a world. */
        WORLD_MANAGE,
        /** The playable world. */
        GAME,
        /** Menu shown on top of the playable world. */
        PAUSE
    }

    /** What the manage screen is asked to do with a world. */
    public enum ManageMode {
        /** Ask whether the world should really be removed. */
        DELETE,
        /** Let the player type a new name. */
        RENAME
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private final NeoFactoryGame game;
    private final Map<ScreenType, Screen> screens = new EnumMap<>(ScreenType.class);
    private final WorldStorage storage = new WorldStorage();

    private ScreenType currentType;

    /** World that is currently loaded, {@code null} while the player is in a menu. */
    private GameScreen gameScreen;

    /** Save game the open world belongs to, {@code null} while no world is open. */
    private SaveSummary activeWorld;

    /** World the manage screen works on. */
    private SaveSummary managedWorld;

    /** Action the manage screen performs. */
    private ManageMode manageMode = ManageMode.DELETE;

    /**
     * Creates a manager bound to a game.
     *
     * @param game game instance used to install the screens
     */
    public ScreenManager(NeoFactoryGame game) {
        this.game = game;
    }

    /**
     * Shows a screen, creating it when it is used for the first time.
     *
     * @param type screen to show
     * @return the shown screen, never {@code null}
     */
    public Screen show(ScreenType type) {
        if (currentType == type && game.getScreen() != null) {
            return game.getScreen();
        }
        if (type == ScreenType.GAME && gameScreen == null) {
            // Without a world there is nothing to play, the title screen is the way back.
            LOGGER.warn("No world is loaded, showing the title screen instead");
            return show(ScreenType.TITLE);
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

    /** Storage that holds the save games. */
    public WorldStorage storage() {
        return storage;
    }

    /** World that is currently loaded, {@code null} while the player is in a menu. */
    public GameScreen gameScreen() {
        return gameScreen;
    }

    /** Save game the open world belongs to, {@code null} while no world is open. */
    public SaveSummary activeWorld() {
        return activeWorld;
    }

    /** World the manage screen works on. */
    public SaveSummary managedWorld() {
        return managedWorld;
    }

    /** Action the manage screen performs. */
    public ManageMode manageMode() {
        return manageMode;
    }

    /**
     * Opens a stored world and shows it.
     *
     * @param summary save game to open
     * @return the playable screen of that world
     * @throws com.philia093.neofactory.world.save.SaveException when the file is damaged
     */
    public GameScreen loadWorld(SaveSummary summary) {
        WorldLoader loader = WorldLoader.open(storage, summary, 0, 0);
        return startWorld(summary, loader.world(), loader.data(), false);
    }

    /**
     * Creates a world and opens it right away.
     *
     * @param name requested name of the world
     * @param seed seed of the terrain
     * @return the playable screen of the new world
     */
    public GameScreen createWorld(String name, int seed) {
        SaveSummary summary = storage.create(name, seed, 0, 0);
        LevelData data = new LevelData();
        data.setWorldName(summary.displayName());
        data.setSeed(seed);
        data.setCreated(System.currentTimeMillis());
        data.setLastPlayed(data.created());

        World world = new World(seed, 0, 0);
        data.setSpawn(world.spawnX(), world.spawnY());
        LOGGER.info("Created world '{}' (seed {})", summary.displayName(), seed);
        return startWorld(summary, world, data, true);
    }

    /** Saves the open world and returns to the title screen. */
    public void leaveWorld() {
        if (gameScreen != null) {
            try {
                gameScreen.save();
            } catch (RuntimeException e) {
                LOGGER.error("Unable to save the world before leaving it", e);
            }
            releaseWorld();
        }
        show(ScreenType.TITLE);
    }

    /** Returns to the world that is still loaded, used by the pause menu. */
    public void resumeWorld() {
        if (gameScreen == null) {
            LOGGER.warn("No world is loaded, the title screen is shown instead");
            show(ScreenType.TITLE);
            return;
        }
        show(ScreenType.GAME);
    }

    /**
     * Asks the manage screen to work on a world.
     *
     * @param summary world to manage
     * @param mode action to perform
     */
    public void manageWorld(SaveSummary summary, ManageMode mode) {
        this.managedWorld = summary;
        this.manageMode = mode;
        show(ScreenType.WORLD_MANAGE);
    }

    /**
     * Disposes every screen that was created.
     * <p>
     * Called when the application shuts down. Screens are not disposed while the game
     * runs, because the player may return to them; the playable screen is the only
     * one that is thrown away when its world is left.
     */
    public void dispose() {
        for (Screen screen : screens.values()) {
            screen.dispose();
        }
        screens.clear();
        gameScreen = null;
        activeWorld = null;
        currentType = null;
        LOGGER.info("Disposed all screens");
    }

    /**
     * Creates the playable screen of a world and shows it.
     *
     * @param summary save game of the world
     * @param world world to play in
     * @param data level data of the world
     * @return the playable screen
     */
    private GameScreen startWorld(SaveSummary summary, World world, LevelData data, boolean fresh) {
        releaseWorld();
        GameScreen screen = new GameScreen(game, summary, world, data, fresh);
        screens.put(ScreenType.GAME, screen);
        gameScreen = screen;
        activeWorld = summary;
        currentType = ScreenType.GAME;
        game.setScreen(screen);
        LOGGER.info("Entered world '{}' ({})", summary.displayName(), summary.id());
        return screen;
    }

    /** Throws the open world away, freeing the screen it was played on. */
    private void releaseWorld() {
        if (gameScreen == null) {
            return;
        }
        screens.remove(ScreenType.GAME);
        gameScreen.dispose();
        gameScreen = null;
        activeWorld = null;
    }

    /** Instantiates a screen of the given type. */
    private Screen create(ScreenType type) {
        switch (type) {
            case WORLD_SELECT:
                return new WorldSelectScreen(game);
            case WORLD_CREATE:
                return new WorldCreateScreen(game);
            case WORLD_MANAGE:
                return new WorldManageScreen(game);
            case PAUSE:
                return new PauseScreen(game);
            case GAME:
                // The playable screen is created together with its world, see startWorld.
                throw new IllegalStateException("The game screen belongs to a world");
            case TITLE:
            default:
                return new TitleScreen(game);
        }
    }
}
