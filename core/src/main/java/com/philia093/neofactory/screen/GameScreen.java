package com.philia093.neofactory.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.philia093.neofactory.NeoFactoryGame;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.input.InputHandler;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.render.PlayerRenderer;
import com.philia093.neofactory.render.WorldRenderer;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The playable screen.
 * <p>
 * It owns the world, the player and everything needed to draw them. The camera
 * looks straight down on the world and follows the player, which is why the
 * player stays in the middle of the window while the terrain scrolls past.
 * <p>
 * Controls: {@code WASD} walks, the mouse aims, the wheel zooms, {@code F11}
 * switches to fullscreen and {@code ESC} closes the game.
 */
public class GameScreen extends NeoFactoryScreen {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Seed used when the game is started without a save game. */
    private static final int DEFAULT_SEED = 20260918;

    /** Block X coordinate of the default spawn. */
    private static final int SPAWN_X = 0;

    /** Block Y coordinate of the default spawn. */
    private static final int SPAWN_Y = 0;

    /** Interval in frames between two status log lines. */
    private static final int DEBUG_LOG_INTERVAL = 120;

    private final OrthographicCamera camera;
    private final ExtendViewport worldViewport;
    private final SpriteBatch batch;

    private final World world;
    private final Player player;
    private final InputHandler inputHandler;

    private final WorldRenderer worldRenderer;
    private final PlayerRenderer playerRenderer;

    /** Current camera zoom, {@code 1} is the neutral view again the values shrink. */
    private float zoom = 1.0f;

    private int debugFrameCounter;

    /**
     * Creates the playable screen.
     *
     * @param game game instance owning this screen
     */
    public GameScreen(NeoFactoryGame game) {
        super(game);

        BlockTextureCache textures = game.textures();
        this.batch = new SpriteBatch();
        this.camera = new OrthographicCamera();

        // The viewport keeps a fixed amount of blocks visible on the shorter axis,
        // the longer axis grows with the window aspect ratio.
        float visibleUnits = Constants.TILE_SIZE * Constants.VIEW_BLOCKS;
        this.worldViewport = new ExtendViewport(visibleUnits, visibleUnits, camera);

        this.world = new World(DEFAULT_SEED, SPAWN_X, SPAWN_Y);
        this.player = Player.spawnOnGround(world, world.spawnX(), world.spawnY());
        this.inputHandler = new InputHandler();

        this.worldRenderer = new WorldRenderer(batch, textures);
        this.playerRenderer = new PlayerRenderer(batch, textures);

        loadChunksAroundPlayer();
        centerCameraOnPlayer();

        LOGGER.info("Player spawned at block ({}, {})", player.blockX(), player.blockY());
    }

    /** World shown by this screen. */
    public World world() {
        return world;
    }

    /** Player controlled by this screen. */
    public Player player() {
        return player;
    }

    /** Current camera zoom, {@code 1} is the neutral view. */
    public float zoom() {
        return zoom;
    }

    @Override
    public void show() {
        // The input handler is asked first so that it can swallow the keys it
        // handles, the stage receives everything else.
        Gdx.input.setInputProcessor(new InputMultiplexer(inputHandler, stage));
    }

    @Override
    public void render(float delta) {
        handleInputAndUpdate(delta);

        clearScreen();

        worldViewport.apply();
        centerCameraOnPlayer();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        worldRenderer.render(world, camera);
        playerRenderer.render(player);
        batch.end();

        stage.act(delta);
        stage.draw();

        logDebugStatistics();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        worldViewport.update(width, height, false);
        loadChunksAroundPlayer();
        centerCameraOnPlayer();
    }

    @Override
    public void dispose() {
        batch.dispose();
        worldRenderer.dispose();
        super.dispose();
    }

    /**
     * Applies the player input, advances the simulation and reveals new terrain.
     *
     * @param delta time since the last frame in seconds
     */
    private void handleInputAndUpdate(float delta) {
        if (inputHandler.consumeExitRequest()) {
            LOGGER.info("Exit requested through the keyboard, closing the game");
            Gdx.app.exit();
            return;
        }
        if (inputHandler.consumeFullscreenToggle()) {
            toggleFullscreen();
        }
        applyZoomInput();

        inputHandler.update(player, camera);
        player.update(world, delta, zoom);
        loadChunksAroundPlayer();
    }

    /** Turns the collected wheel notches into a clamped, multiplicative zoom. */
    private void applyZoomInput() {
        float steps = inputHandler.consumeZoomSteps();
        if (steps == 0.0f) {
            return;
        }
        // Rolling the wheel forwards magnifies, which means a smaller camera zoom.
        float factor = (float) Math.pow(Constants.ZOOM_STEP, -steps);
        zoom = MathUtils.clamp(zoom * factor, Constants.ZOOM_MIN, Constants.ZOOM_MAX);
    }

    /**
     * Places the camera on the player and applies the current zoom.
     * <p>
     * This is what keeps the player centered on screen: the world coordinates of
     * the player are the world coordinates of the camera center.
     */
    private void centerCameraOnPlayer() {
        camera.position.set(player.position().x, player.position().y, 0.0f);
        camera.zoom = zoom;
        camera.up.set(0.0f, 1.0f, 0.0f);
        camera.direction.set(0.0f, 0.0f, -1.0f);
        camera.update();
    }

    /**
     * Makes sure every chunk visible at the current zoom is loaded.
     * <p>
     * The radius is derived from the viewport size rather than being a constant,
     * so zooming out reveals the terrain instead of showing empty space. One chunk
     * is added on top of the visible area: terrain and decorations are planted
     * when a chunk is finished, so the margin keeps new trees from appearing out of
     * nothing inside the view.
     */
    private void loadChunksAroundPlayer() {
        float visibleBlocksX = worldViewport.getWorldWidth() * zoom / Constants.TILE_SIZE;
        float visibleBlocksY = worldViewport.getWorldHeight() * zoom / Constants.TILE_SIZE;
        float halfBlocks = Math.max(visibleBlocksX, visibleBlocksY) * 0.5f;
        int chunkRadius = (int) Math.ceil(halfBlocks / Constants.CHUNK_SIZE) + 2;

        world.ensureChunksAround(player.position().x / Constants.TILE_SIZE,
                player.position().y / Constants.TILE_SIZE, chunkRadius);
    }

    /** Writes a short status line to the log now and then. */
    private void logDebugStatistics() {
        debugFrameCounter++;
        if (debugFrameCounter < DEBUG_LOG_INTERVAL) {
            return;
        }
        debugFrameCounter = 0;
        LOGGER.info("Block ({}, {}) | zoom {} | tiles {} | chunks {} | fps {}",
                player.blockX(), player.blockY(), String.format("%.2f", zoom),
                worldRenderer.drawnTileCount(), world.chunkCount(),
                Gdx.graphics.getFramesPerSecond());
    }
}

