package com.philia093.neofactory.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.philia093.neofactory.NeoFactoryGame;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.gui.HotbarGui;
import com.philia093.neofactory.gui.InventoryGui;
import com.philia093.neofactory.input.InputHandler;
import com.philia093.neofactory.item.InventoryDrops;
import com.philia093.neofactory.item.ItemDrops;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.render.PixelFont;
import com.philia093.neofactory.render.PlayerRenderer;
import com.philia093.neofactory.render.SelectionRenderer;
import com.philia093.neofactory.render.WorldRenderer;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.interaction.BlockPlacer;
import com.philia093.neofactory.world.interaction.BlockTarget;
import com.philia093.neofactory.world.interaction.BlockTargeting;
import com.philia093.neofactory.world.interaction.InstantMining;
import com.philia093.neofactory.world.interaction.MiningController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The playable screen.
 * <p>
 * It owns the world, the player and everything needed to draw them. The camera
 * looks straight down on the world and follows the player, which is why the
 * player stays in the middle of the window while the terrain scrolls past.
 * <p>
 * Controls: {@code WASD} walks, the mouse aims, the wheel zooms, keys {@code 1} to
 * {@code 9} select a hotbar slot, {@code E} opens and closes the inventory,
 * {@code F11} switches to fullscreen and {@code ESC} closes the inventory or, when
 * it is closed already, the game.
 * <p>
 * The left mouse button breaks the block the player aims at while it is held, the
 * right button builds the held block. Both actions apply to the layer the player
 * stands in; holding {@code SHIFT} moves them to the layer below the feet, which is
 * the ground the player walks on. Aiming further away than the reach of the player
 * falls back to the line of sight, see {@link BlockTargeting}.
 * <p>
 * An action never skips a layer: the ground below the feet can only be dug or filled
 * while the layer the player stands in is empty, and a block can only be built into
 * that layer while it has ground below. The colour of the frame tells which kind of
 * column the player aims at, see {@link SelectionRenderer}.
 * <p>
 * While the inventory is open the world keeps running, but the keyboard only drives
 * the interface and the player stands still.
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

    /** Hotbar drawn at the bottom of the window, shown while the player walks. */
    private final HotbarGui hotbarGui;

    /** Inventory screen, opened and closed with the inventory key. */
    private final InventoryGui inventoryGui;

    /** Frame drawn around the block the player aims at. */
    private final SelectionRenderer selectionRenderer;

    /** Sink that receives the items of a broken block, see {@link ItemDrops}. */
    private final ItemDrops drops;

    /** Breaks the targeted block while the left button is held. */
    private final MiningController mining;

    /** Cell a break or a build would touch, {@code null} while nothing is aimed at. */
    private BlockTarget target;

    /** Position of the mouse in world units, reused every frame. */
    private final Vector2 worldMouse = new Vector2();

    /** Position of the mouse in the virtual pixels of the interface, reused every frame. */
    private final Vector2 interfaceMouse = new Vector2();

    /**
     * Forwards mouse presses to the inventory screen and builds blocks.
     * <p>
     * The screen works in the virtual pixels of the interface, so the position of the
     * mouse is converted through the viewport first. While the screen is open it gets
     * the press; while it is closed the world does, see {@link #buildBlock()}.
     */
    private final InputAdapter interfaceInput = new InputAdapter() {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            uiViewport.unproject(screenX, screenY, interfaceMouse);
            if (inventoryGui.touchDown(interfaceMouse.x, interfaceMouse.y, button)) {
                return true;
            }
            if (InputHandler.isBuildButton(button)) {
                return buildBlock();
            }
            return false;
        }
    };

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
        PixelFont font = game.font();
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
        this.hotbarGui = new HotbarGui(textures, font, uiViewport);
        this.inventoryGui = new InventoryGui(textures, font, player.inventory(), uiViewport);
        this.selectionRenderer = new SelectionRenderer(batch, textures);

        // Items of a broken block go straight into the inventory until dropped item
        // entities exist; switching that is one line here, see InventoryDrops.
        this.drops = new InventoryDrops(player.inventory());
        this.mining = new MiningController(new InstantMining(), drops);

        fillDebugInventory(player.inventory());
        loadChunksAroundPlayer();
        centerCameraOnPlayer();

        LOGGER.info("Player spawned at block ({}, {}) holding {}",
                player.blockX(), player.blockY(), player.inventory());
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
        // The inventory is asked first, because it swallows the mouse while it is
        // open; the input handler then receives the keys it handles and the stage
        // gets everything else.
        Gdx.input.setInputProcessor(new InputMultiplexer(interfaceInput, inputHandler, stage));
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
        selectionRenderer.render(world, target);
        batch.end();

        renderInterface(delta);

        logDebugStatistics();
    }

    /**
     * Draws the interface on top of the world.
     * <p>
     * The interface viewport blows the interface up by a whole number and owns its
     * coordinates, so the batch is switched to its projection and the mouse is
     * converted into the same virtual pixels before anything is drawn. That way a
     * click on the screen always matches the slot the player sees, in fullscreen and
     * after a resize as well.
     *
     * @param delta time since the last frame in seconds
     */
    private void renderInterface(float delta) {
        uiViewport.apply();
        uiViewport.unproject(Gdx.input.getX(), Gdx.input.getY(), interfaceMouse);

        batch.setProjectionMatrix(uiViewport.getCamera().combined);
        batch.begin();
        hotbarGui.render(batch, player.inventory(),
                interfaceMouse.x, interfaceMouse.y, !inventoryGui.isOpen());
        inventoryGui.render(batch, interfaceMouse.x, interfaceMouse.y);
        batch.end();
        batch.setColor(Color.WHITE);

        stage.act(delta);
        stage.draw();
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
     * <p>
     * While the inventory is open the world still runs, but only the interface is
     * driven: the player is stopped and neither the movement keys nor the wheel are
     * forwarded. The collected wheel notches are dropped in that case, so closing
     * the screen never jumps the camera.
     *
     * @param delta time since the last frame in seconds
     */
    private void handleInputAndUpdate(float delta) {
        handleInterfaceKeys();

        if (inputHandler.consumeExitRequest()) {
            if (inventoryGui.isOpen()) {
                // The escape key leaves the screen first and the game afterwards.
                inventoryGui.close();
            } else {
                LOGGER.info("Exit requested through the keyboard, closing the game");
                Gdx.app.exit();
                return;
            }
        }
        if (inputHandler.consumeFullscreenToggle()) {
            toggleFullscreen();
        }

        float zoomSteps = inputHandler.consumeZoomSteps();
        if (inventoryGui.isOpen()) {
            player.halt();
            target = null;
        } else {
            applyZoom(zoomSteps);
            inputHandler.update(player, camera);
            player.update(world, delta, zoom);
            updateInteraction(delta);
        }
        loadChunksAroundPlayer();
    }

    /**
     * Aims at a cell and mines it while the left button is held.
     * <p>
     * The mouse is unprojected through the viewport of the world, which is the same
     * mapping the camera uses, so the frame the player sees matches the cell that is
     * really addressed. The shift key moves the action from the layer the player
     * stands in to the layer below the feet, see {@link InputHandler#isGroundLayerDown()}.
     *
     * @param delta time since the last frame in seconds
     */
    private void updateInteraction(float delta) {
        worldMouse.set(Gdx.input.getX(), Gdx.input.getY());
        worldViewport.unproject(worldMouse);

        int layer = inputHandler.isGroundLayerDown() ? Chunk.LAYER_FLOOR : Chunk.LAYER_OBJECT;
        target = BlockTargeting.select(world, player, worldMouse.x, worldMouse.y, layer);

        boolean broken = mining.update(delta, world, target, player.inventory().heldStack(),
                inputHandler.isBreakingDown());
        if (broken) {
            LOGGER.info("Broke block ({}, {}) of layer {}", target.x(), target.y(), target.layer());
        }
    }

    /**
     * Builds the held block into the targeted cell.
     * <p>
     * Called from the mouse event while the inventory screen is closed, so a click
     * builds exactly one block, see
     * {@link BlockPlacer#place(World, Player, BlockTarget, PlayerInventory)}.
     *
     * @return {@code true} when a block was built
     */
    private boolean buildBlock() {
        if (target == null) {
            return false;
        }
        String itemName = player.inventory().heldStack().item().displayName();
        int x = target.x();
        int y = target.y();
        int layer = target.layer();
        if (!BlockPlacer.place(world, player, target, player.inventory())) {
            return false;
        }
        LOGGER.info("Built {} into block ({}, {}) of layer {}", itemName, x, y, layer);
        return true;
    }

    /** Applies the keys that belong to the interface instead of the world. */
    private void handleInterfaceKeys() {
        if (inputHandler.consumeInventoryToggle()) {
            inventoryGui.toggle();
            LOGGER.info("Inventory {}", inventoryGui.isOpen() ? "opened" : "closed");
        }
        int hotbarSlot = inputHandler.consumeHotbarSelection();
        if (hotbarSlot >= 0) {
            player.inventory().setSelectedSlot(hotbarSlot);
        }
    }

    /**
     * Turns the collected wheel notches into a clamped, multiplicative zoom.
     *
     * @param steps wheel notches collected since the last frame
     */
    private void applyZoom(float steps) {
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
        LOGGER.info("Block ({}, {}) | zoom {} | tiles {} | chunks {} | hotbar {} | inventory {} "
                        + "| target {} | gui {} | fps {}",
                player.blockX(), player.blockY(), String.format("%.2f", zoom),
                worldRenderer.drawnTileCount(), world.chunkCount(),
                player.inventory().selectedSlot(),
                inventoryGui.isOpen() ? "open" : "closed",
                targetText(),
                uiViewport.scale(),
                Gdx.graphics.getFramesPerSecond());
    }

    /** Short description of the targeted cell, used by the status log. */
    private String targetText() {
        if (target == null) {
            return "none";
        }
        return target.x() + "," + target.y() + " layer " + target.layer()
                + (target.fromRay() ? " (sight)" : " (mouse)");
    }

    /**
     * Fills the inventory with a fixed set of stacks.
     * <p>
     * Temporary: the game has no save game and no way to obtain items yet, so a hand
     * written kit is what makes the hotbar and the inventory screen show something.
     * The kit covers block items, materials, food, tools and armour, which is one
     * example of every kind of icon the game knows.
     *
     * @param inventory inventory to fill
     */
    private void fillDebugInventory(PlayerInventory inventory) {
        inventory.add(ItemStack.of(Items.DIAMOND_PICKAXE, 1));
        inventory.add(ItemStack.of(Items.DIAMOND_SWORD, 1));
        inventory.add(ItemStack.of(Items.IRON_SWORD, 1));
        inventory.add(ItemStack.of(Items.GRASS, 64));
        inventory.add(ItemStack.of(Items.DIRT, 64));
        inventory.add(ItemStack.of(Items.STONE, 64));
        inventory.add(ItemStack.of(Items.SAND, 21));
        inventory.add(ItemStack.of(Items.SNOW, 9));
        inventory.add(ItemStack.of(Items.BEDROCK, 1));

        inventory.add(ItemStack.of(Items.LOG_OAK, 12));
        inventory.add(ItemStack.of(Items.PLANKS_OAK, 40));
        inventory.add(ItemStack.of(Items.LEAVES_OAK, 8));
        inventory.add(ItemStack.of(Items.TALL_GRASS, 16));
        inventory.add(ItemStack.of(Items.COAL_ORE, 5));
        inventory.add(ItemStack.of(Items.IRON_ORE, 5));
        inventory.add(ItemStack.of(Items.SANDSTONE, 7));
        inventory.add(ItemStack.of(Items.IRON_INGOT, 24));
        inventory.add(ItemStack.of(Items.GOLD_INGOT, 6));

        inventory.add(ItemStack.of(Items.DIAMOND, 7));
        inventory.add(ItemStack.of(Items.EMERALD, 2));
        inventory.add(ItemStack.of(Items.COAL, 30));
        inventory.add(ItemStack.of(Items.CHARCOAL, 4));
        inventory.add(ItemStack.of(Items.STICK, 32));
        inventory.add(ItemStack.of(Items.REDSTONE_DUST, 64));
        inventory.add(ItemStack.of(Items.GLOWSTONE_DUST, 11));
        inventory.add(ItemStack.of(Items.CLAY_BALL, 13));
        inventory.add(ItemStack.of(Items.FLINT, 3));
        inventory.add(ItemStack.of(Items.FEATHER, 9));
        inventory.add(ItemStack.of(Items.LEATHER, 6));
        inventory.add(ItemStack.of(Items.BONE, 4));
        inventory.add(ItemStack.of(Items.STRING, 8));
        inventory.add(ItemStack.of(Items.PAPER, 12));
        inventory.add(ItemStack.of(Items.WHEAT, 18));
        inventory.add(ItemStack.of(Items.SEEDS_WHEAT, 16));
        inventory.add(ItemStack.of(Items.GUNPOWDER, 5));
        inventory.add(ItemStack.of(Items.BLAZE_ROD, 2));
        inventory.add(ItemStack.of(Items.BLAZE_POWDER, 3));
        inventory.add(ItemStack.of(Items.SUGAR, 17));
    }
}
