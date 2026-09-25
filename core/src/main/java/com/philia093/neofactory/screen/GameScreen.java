package com.philia093.neofactory.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.philia093.neofactory.NeoFactoryGame;
import com.philia093.neofactory.blockentity.BlockEntity;
import com.philia093.neofactory.blockentity.MachineBlockEntity;
import com.philia093.neofactory.entity.EntityTypes;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.chat.ChatController;
import com.philia093.neofactory.chat.ChatLog;
import com.philia093.neofactory.chat.command.CommandContext;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.chat.command.CommandRegistry;
import com.philia093.neofactory.gui.ChatOverlay;
import com.philia093.neofactory.gui.CreativeInventoryGui;
import com.philia093.neofactory.gui.HotbarGui;
import com.philia093.neofactory.gui.InventoryGui;
import com.philia093.neofactory.gui.MachineGui;
import com.philia093.neofactory.input.InputHandler;
import com.philia093.neofactory.item.ItemDrops;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.item.Items;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.item.WorldDrops;
import com.philia093.neofactory.material.Materials;
import com.philia093.neofactory.render.BlockIconRenderer;
import com.philia093.neofactory.render.HumanoidRenderer;
import com.philia093.neofactory.render.BlockPictures;
import com.philia093.neofactory.render.BlockShader;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.render.EntityRendererRegistry;
import com.philia093.neofactory.render.PixelFont;
import com.philia093.neofactory.render.SectionMeshCache;
import com.philia093.neofactory.render.WorldRenderer3D;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.ChunkStreamer;
import com.philia093.neofactory.world.GameMode;
import com.philia093.neofactory.world.TickClock;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.interaction.BlockPlacer;
import com.philia093.neofactory.world.interaction.BlockTarget;
import com.philia093.neofactory.world.interaction.BlockTargeting;
import com.philia093.neofactory.world.interaction.InstantMining;
import com.philia093.neofactory.world.interaction.MiningController;
import com.philia093.neofactory.world.save.LevelData;
import com.philia093.neofactory.world.save.SaveSummary;
import com.philia093.neofactory.world.save.WorldSaver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The playable screen.
 * <p>
 * It owns the world, the player and everything needed to draw them. The camera
 * looks straight down on the world and follows the player, which is why the
 * player stays in the middle of the window while the terrain scrolls past.
 * <p>
 * Controls: {@code WASD} walks, the mouse aims, the wheel selects a hotbar slot
 * (while {@code CTRL} is held it zooms the camera instead), a left click on a
 * hotbar slot selects it as well and keys {@code 1} to {@code 9} do the same.
 * Zooming is done with {@code -} and {@code =}, which keep rolling while they are
 * held, and {@code [} / {@code ]} change how many chunks are kept around the
 * player. {@code E} opens and closes the inventory, {@code F11} switches to
 * fullscreen and {@code ESC} closes the inventory or, when it is closed already,
 * opens the pause menu, see {@link PauseScreen}.
 * <p>
 * The left mouse button breaks the block the player aims at while it is held, the
 * right button builds the held block. Both actions apply to the layer the player
 * stands in; holding {@code SHIFT} moves them to the layer below the feet, which is
 * the ground the player walks on. Aiming further away than the reach of the player
 * falls back to the line of sight, see {@link BlockTargeting}.
 * <p>
 * An action never skips a layer: the ground below the feet can only be dug or filled
 * while the layer the player stands in is empty, and a block can only be built into
 * that layer while it has ground below. The frame the renderer draws around the cell
 * the view meets tells which kind of column the player aims at, see
 * {@link com.philia093.neofactory.render.WorldRenderer3D#renderSelection}.
 * <p>
 * While the inventory is open the world keeps running, but the keyboard only drives
 * the interface and the player stands still.
 * <p>
 * The screen belongs to one save game: it is created when a world is opened and
 * thrown away when the player leaves it, see
 * {@link ScreenManager#loadWorld(com.philia093.neofactory.world.save.SaveSummary)}.
 * Leaving or closing the game writes the world, and it is written again every few
 * minutes while it is played, see {@link #save()}.
 */
public class GameScreen extends NeoFactoryScreen implements CommandContext {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Interval in frames between two status log lines. */
    private static final int DEBUG_LOG_INTERVAL = 120;

    /** Seconds between two automatic saves while the world is played. */
    private static final float AUTOSAVE_INTERVAL = 300.0f;

    /** Field of view of the camera that stands in the world, in degrees. */
    private static final float CUBE_FIELD_OF_VIEW = Constants.VIEW_FIELD_OF_VIEW;

    /** Distance the camera of a world of cubes starts drawing at, in blocks. */
    private static final float CUBE_NEAR = 0.05f;

    /** Distance that camera sees, in blocks, which is also where the fog ends. */
    private static final float CUBE_VIEW_DISTANCE = 192.0f;

    /** Share of the view distance the fog starts at, the same share the world renderer uses. */
    private static final float FOG_START_SHARE = 0.55f;

    /** Distance the view of the body from behind stands behind the eye of the body, in blocks. */
    private static final float THIRD_PERSON_BACK = 4.5f;

    /** Height the view of the body from behind stands above the eye of the body, in blocks. */
    private static final float THIRD_PERSON_UP = 1.1f;

    /** Distance the view from behind keeps from the eye, in blocks, when the terrain is in the way. */
    private static final float THIRD_PERSON_CLOSEST = 1.2f;

    /** Distance behind the player that camera stands, in blocks. */
    private static final float CUBE_CAMERA_BACK = 6.0f;

    /** Height above the player that camera stands, in blocks. */
    private static final float CUBE_CAMERA_HEIGHT = 4.0f;

    /** Colour the frame is cleared with and the distance fades into, shared and never written. */
    private static final Color SKY =
            new Color(Constants.SKY_RED, Constants.SKY_GREEN, Constants.SKY_BLUE, 1.0f);

    /** Distance of the line that reports how fast the game runs from the upper left corner. */
    private static final float PERFORMANCE_MARGIN = 3.0f;

    /** Length of one arm of the crosshair, in interface pixels. */
    private static final float CROSSHAIR_ARM = 4.0f;

    /** Thickness of the crosshair, in interface pixels. */
    private static final float CROSSHAIR_THICKNESS = 1.0f;

    /** Gap the arms keep from the middle, in interface pixels, so the cell is not covered. */
    private static final float CROSSHAIR_GAP = 1.0f;

    /** Colour of the crosshair: a grey that reads on a picture of any colour, never written. */
    private static final Color CROSSHAIR_COLOR = new Color(0.8f, 0.8f, 0.8f, 0.6f);

    /**
     * Longest step the movement of the bodies may take, in seconds.
     * <p>
     * A slow frame - the first ones of a world, or a stall - is a long step for a body that falls, long
     * enough to pass through ground that is one block thick, see the rule in {@code Player}. The movement
     * is never handed more than this, so a slow frame becomes a moment of slow motion instead of a hole in
     * the world.
     */
    private static final float LONGEST_PHYSICS_STEP = 0.05f;

    /** Frames counted since the last second was up. */
    private int framesThisSecond;

    /** Seconds counted since the last time the frame rate was worked out. */
    private float fpsSeconds;

    /** Frames the last second held, what the status line reports. */
    private int frameRate;

    private final OrthographicCamera camera;
    private final ExtendViewport worldViewport;
    private final SpriteBatch batch;

    /** Save game this world belongs to. */
    private final SaveSummary summary;

    /** Everything about the world that is not a block, written on every save. */
    private final LevelData data;

    private final World world;
    private final Player player;
    private final InputHandler inputHandler;

    /**
     * The world drawn as cubes.
     * <p>
     * The launcher asks for an OpenGL 3.2 core context, which is what a texture array needs, see
     * {@link BlockPictures}: the pictures of every block become one array and the mesher turns a
     * section into the faces that array is drawn with. This renderer is therefore what the game draws
     * with and nothing else - the view from above it replaced is gone.
     */
    private final BlockPictures pictures;
    private final BlockShader blockShader;
    private final SectionMeshCache sectionMeshes;
    private final WorldRenderer3D cubeRenderer;
    private final PerspectiveCamera cubeCamera;
    /** Draws the icon of a block by drawing the block, {@code null} while the flat view is used. */
    private final BlockIconRenderer blockIconRenderer;

    /**
     * The body of the player, drawn from the skin of the assets, {@code null} while the world cannot
     * be drawn as cubes.
     * <p>
     * A view from inside a body shows what the hand of that body holds, see
     * {@link HumanoidRenderer#renderHand}: a block sits in the lower right of the picture as its cube,
     * swaying with every step and thrown into the picture when a block is broken or built. <b>A tool is not
     * drawn in a hand</b>, and neither is the bare arm - an arm of boxes across the picture reads worse than
     * no arm - so a hand that holds no block shows nothing. A view of the body from behind draws the whole
     * figure, see {@link #renderEntities()}.
     */
    private final HumanoidRenderer humanoid;

    /**
     * {@code true} while the world is seen from behind the player instead of through its eyes.
     * <p>
     * The key {@code F5} switches between the two, see {@link InputHandler#consumeViewToggle()}. A view
     * from inside a body draws its arm and leaves the body out, so a player never looks at the inside
     * of their own head; the view from behind draws the whole body and no arm.
     */
    private boolean thirdPerson;


    /** Reused direction of the view, so a frame does not fill the heap with vectors. */
    private final Vector3 lookDirection = new Vector3();

    /** Every renderer of the world, one per entity type. */
    private final EntityRendererRegistry entityRenderers = new EntityRendererRegistry();

    /** Hotbar drawn at the bottom of the window, shown while the player walks. */
    private final HotbarGui hotbarGui;

    /** Keeps the chunks around the player in memory and drops the ones behind. */
    private final ChunkStreamer streamer = new ChunkStreamer();

    /** Inventory screen, opened and closed with the inventory key. */
    private final InventoryGui inventoryGui;

    /** The screen the inventory key opens while the world is played in creative mode. */
    private final CreativeInventoryGui creativeGui;

    /** Screen of a machine, opened by using a machine with the build button. */
    private final MachineGui machineGui;

    /** Machine the screen is up for, {@code null} while no machine screen is open. */
    private MachineBlockEntity openMachine;

    /**
     * Chat and command line of this world.
     * <p>
     * This screen is the {@link CommandContext} of the chat, so a command works on
     * the player and the world that are running right here.
     */
    private final ChatController chat;

    /** Draws the chat lines and the input line. */
    private final ChatOverlay chatOverlay;

    /**
     * Lines of the chat.
     * <p>
     * The screen owns the log and hands it out as {@link #log()}, which is where both
     * the messages of the player and the answers of the commands end up: one list,
     * one place to read.
     */
    private final ChatLog chatLog = new ChatLog();

    /** Sink that receives the items of a broken block, see {@link ItemDrops}. */
    private final ItemDrops drops;

    /** Breaks the targeted block while the left button is held. */
    private final MiningController mining;

    /** Cell a break or a build would touch, {@code null} while nothing is aimed at. */
    private BlockTarget target;

    /**
     * Turns frames into ticks of the simulation.
     * <p>
     * The machines of the world advance in fixed steps, see {@link TickClock}, so a
     * machine works the same amount whether the frame was short or long.
     */
    private final TickClock tickClock = new TickClock();

    /** Seconds since the world was written the last time. */
    private float autosaveTimer;

    /** Position of the mouse in world units, reused every frame. */
    private final Vector2 worldMouse = new Vector2();

    /** Position of the mouse in the virtual pixels of the interface, reused every frame. */
    private final Vector2 interfaceMouse = new Vector2();

    /**
     * Forwards mouse presses to the inventory screen and builds blocks.
     * <p>
     * The screen works in the virtual pixels of the interface, so the position of the
     * mouse is converted through the viewport first. While the screen is open it gets
     * the press; while it is closed the world does, see {@link #buildBlock()}. A button
     * that is held and dragged over the slots of the inventory shares a stack out, see
     * {@link InventoryGui#touchDragged(float, float)}.
     */
    private final InputAdapter interfaceInput = new InputAdapter() {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (chat.isOpen()) {
                // While the player types, a click belongs to nobody: it must not build
                // a block behind the input line.
                return true;
            }
            uiViewport.unproject(screenX, screenY, interfaceMouse);
            if (creativeGui.touchDown(interfaceMouse.x, interfaceMouse.y, button)) {
                return true;
            }
            if (inventoryGui.touchDown(interfaceMouse.x, interfaceMouse.y, button)) {
                return true;
            }
            if (machineGui.touchDown(interfaceMouse.x, interfaceMouse.y, button)) {
                return true;
            }
            if (button == Input.Buttons.LEFT) {
                int slot = hotbarGui.slotAt(interfaceMouse.x, interfaceMouse.y);
                if (slot >= 0) {
                    // A click on the bar selects the slot instead of building a block.
                    player.inventory().setSelectedSlot(slot);
                    return true;
                }
            }
            if (InputHandler.isBuildButton(button)) {
                return buildBlock();
            }
            return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            uiViewport.unproject(screenX, screenY, interfaceMouse);
            // A held button that is dragged over the slots of the inventory shares the
            // carried stack out over them.
            creativeGui.touchDragged(interfaceMouse.x, interfaceMouse.y);
            inventoryGui.touchDragged(interfaceMouse.x, interfaceMouse.y);
            machineGui.touchDragged(interfaceMouse.x, interfaceMouse.y);
            return false;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            if (chat.isOpen()) {
                return true;
            }
            uiViewport.unproject(screenX, screenY, interfaceMouse);
            if (creativeGui.touchUp(interfaceMouse.x, interfaceMouse.y, button)) {
                return true;
            }
            if (inventoryGui.touchUp(interfaceMouse.x, interfaceMouse.y, button)) {
                return true;
            }
            if (machineGui.touchUp(interfaceMouse.x, interfaceMouse.y, button)) {
                return true;
            }
            return false;
        }

        /**
         * Handles a key press before the game sees it.
         * <p>
         * While the player types, the chat takes every key: walking, dropping,
         * opening the inventory or switching to fullscreen behind a half typed
         * message would be a surprise. When the line is closed, the two keys that
         * open it are handled here, and only when the inventory screen is not open,
         * because that screen owns the same corner of the keyboard.
         */
        @Override
        public boolean keyDown(int keyCode) {
            if (chat.isOpen()) {
                return chat.keyDown(keyCode);
            }
            if (creativeGui.takesKey(keyCode)) {
                // The search box owns the keyboard, so the letter belongs into it and not
                // into the hotkeys below: a typed E closes nothing while the player types.
                creativeGui.keyDown(keyCode);
                return true;
            }
            if (creativeGui.keyDown(keyCode)) {
                return true;
            }
            if (!isInterfaceOpen() && ChatController.isOpenKey(keyCode)) {
                chat.open(keyCode);
                LOGGER.info("Chat opened with {}", keyCode == ChatController.KEY_CHAT
                        ? "T" : "/");
                return true;
            }
            return false;
        }

        @Override
        public boolean keyTyped(char character) {
            if (creativeGui.keyTyped(character)) {
                return true;
            }
            return chat.keyTyped(character);
        }
    };

    /** Current camera zoom, {@code 1} is the neutral view again the values shrink. */
    private float zoom = 1.0f;

    /** Frame counter of the status log. */
    private int debugFrameCounter;

    /**
     * Creates the playable screen.
     *
     * @param game game instance owning this screen
     */
    /**
     * Creates the screen of a world.
     *
     * @param game game instance owning this screen
     * @param summary save game this world belongs to
     * @param world world to play in
     * @param data level data of the world
     * @param fresh {@code true} for a world that was just created, which starts at its
     *              spawn point and receives the starter kit
     */
    public GameScreen(NeoFactoryGame game, SaveSummary summary, World world, LevelData data,
            boolean fresh) {
        super(game);

        BlockTextureCache textures = game.textures();
        PixelFont font = game.font();
        this.summary = summary;
        this.data = data;
        this.world = world;
        this.batch = new SpriteBatch();
        this.camera = new OrthographicCamera();

        // The viewport keeps a fixed amount of blocks visible on the shorter axis,
        // the longer axis grows with the window aspect ratio.
        float visibleUnits = Constants.BLOCK_SIZE * Constants.VIEW_BLOCKS;
        this.worldViewport = new ExtendViewport(visibleUnits, visibleUnits, camera);

        this.player = preparePlayer(world, data, fresh);
        this.inputHandler = new InputHandler();

        // The world is drawn as cubes wherever the driver holds a texture array, which is what the
        // launcher asks for: the pictures of every block become one array, the mesher turns a section
        // into the faces that are seen and the shader draws them through a camera that stands in the
        // world. A driver that cannot answer that request keeps the flat renderer above.
        if (Gdx.gl30 != null) {
            this.pictures = new BlockPictures();
            pictures.build();
            this.blockShader = new BlockShader();
            this.sectionMeshes = new SectionMeshCache(pictures);
            this.cubeRenderer = new WorldRenderer3D(sectionMeshes, blockShader, pictures);
            this.cubeCamera = new PerspectiveCamera(CUBE_FIELD_OF_VIEW, 1.0f, 1.0f);
            cubeCamera.near = CUBE_NEAR;
            cubeCamera.far = CUBE_VIEW_DISTANCE;
            // A slot shows a block by drawing the block itself, and that drawing shares the shader and the
            // pictures of the world, so what a slot holds looks like what the world holds. The icons are
            // drawn right away and not while a slot asks for them, see BlockIconRenderer#prime.
            this.blockIconRenderer = new BlockIconRenderer(blockShader, pictures);
            blockIconRenderer.prime();
            textures.setBlockIconRenderer(blockIconRenderer);
            this.humanoid = new HumanoidRenderer(pictures, cubeRenderer.itemCubes());
            LOGGER.info("The world is drawn as cubes, {} pictures in the array", pictures.layerCount());
        } else {
            this.pictures = null;
            this.blockShader = null;
            this.sectionMeshes = null;
            this.cubeRenderer = null;
            this.cubeCamera = null;
            this.blockIconRenderer = null;
            this.humanoid = null;
            LOGGER.warn("This driver holds no texture array, the world of cubes cannot be drawn");
        }
        // The player is an entity like any other, so it is drawn by the same pass over the entity
        // list; the body of the player itself is left out while the view is the one from inside it,
        // because a player must not look at the inside of their own head, see renderEntities().
        entityRenderers.register(EntityTypes.PLAYER, entity -> {
            Player body = (Player) entity;
            if (body == player && !thirdPerson) {
                return;
            }
            humanoid.renderBody(blockShader, body.position().x, body.position().y,
                    body.position().z, body.yaw(),
                    // The body of this screen carries the step and the hit of this screen; a world of
                    // several players would give every one of them a pose of its own.
                    humanoid.currentPose(0.0f, body.pitch()));
        });
        this.hotbarGui = new HotbarGui(textures, font, uiViewport);
        this.inventoryGui = new InventoryGui(textures, font, player.inventory(), uiViewport);
        this.creativeGui = new CreativeInventoryGui(textures, font, player.inventory(), uiViewport);
        // The screen of a machine is opened by using a machine, see #openMachine().
        this.machineGui = new MachineGui(textures, font, uiViewport);
        // The chat is the only place a player types, and its commands work on this very
        // screen: it is its own command context. Handing "this" out while the
        // constructor still runs is safe here, because the chat only stores it and
        // asks for the player and the world once a command is typed.
        this.chat = new ChatController(CommandRegistry.withDefaults(), this);
        this.chatOverlay = new ChatOverlay(textures, font);

        // Items of a broken block fall on the ground and are picked up by walking
        // over them, see WorldDrops and ItemEntity.
        this.drops = new WorldDrops(world);
        this.mining = new MiningController(new InstantMining(), drops);
        // The crafting field of the inventory is a work field: when the screen closes, what
        // is left in it is dropped where the player stands instead of being hidden, see
        // ContainerMenu and Slot.Rule.WORK.
        inventoryGui.setDropper(stack -> drops.drop(stack, player.position().x, player.position().z));
        // A stack that a creative player drags out of the grid lands on the ground instead
        // of going back into an inventory that refills itself anyway.
        creativeGui.setDropper(stack -> drops.drop(stack, player.position().x, player.position().z));

        if (fresh) {
            // A new world starts at its spawn point and gets a starter kit, because
            // there is no crafting yet to turn the first blocks into tools.
            fillDebugInventory(player.inventory());
        }

        streamChunks(true);
        centerCameraOnPlayer();

        LOGGER.info("World '{}' ready, player at block ({}, {}) holding {}",
                summary.displayName(), player.blockX(), player.blockY(), player.inventory());
    }

    /**
     * Returns the player of a world, creating one when the world has none.
     * <p>
     * A stored world carries its player in the entity list, so it comes back with
     * the position, the facing direction and the inventory it was left with. A world
     * written before entities existed has no entry there, and neither has a new one;
     * both fall back to the spawn point and, for an older world, to the player fields
     * the level data kept for exactly this case.
     *
     * @param world world the player belongs to
     * @param data level data of the world
     * @param fresh {@code true} for a world that was just created
     * @return the player, never {@code null}
     */
    private static Player preparePlayer(World world, LevelData data, boolean fresh) {
        Player stored = world.entities().player();
        if (stored != null) {
            return stored;
        }
        Player player = createPlayer(world, data, fresh);
        world.entities().spawn(player);
        return player;
    }

    /**
     * Places the player where the world left it, or at the spawn point of a new world.
     *
     * @param world world the player belongs to
     * @param data level data of the world
     * @param fresh {@code true} for a world that was just created
     * @return the player
     */
    private static Player createPlayer(World world, LevelData data, boolean fresh) {
        boolean storedPosition = !fresh && (data.playerX() != 0.0f || data.playerZ() != 0.0f);
        if (!storedPosition) {
            Player spawned = Player.spawnOnGround(world, world.spawnX(), world.spawnZ());
            data.setSpawn(spawned.blockX(), spawned.blockZ());
            return spawned;
        }
        int blockX = MathUtils.floor(data.playerX() / Constants.BLOCK_SIZE);
        int blockZ = MathUtils.floor(data.playerZ() / Constants.BLOCK_SIZE);
        Player player = new Player(data.playerX(),
                world.surfaceY(blockX, blockZ) * Constants.BLOCK_SIZE, data.playerZ());
        player.setView(data.rotationX(), data.rotationY());
        data.applyInventory(player.inventory());
        return player;
    }

    /** Save game this world belongs to. */
    public SaveSummary summary() {
        return summary;
    }

    /**
     * Writes the world into its save game.
     * <p>
     * Called when the player leaves the world, when the game closes, from the pause
     * menu and every few minutes while the world is played. The player state is taken
     * from the live player first, so the position and the inventory are always part of
     * the file.
     *
     * @return amount of chunks that were written
     */
    public int save() {
        data.setLastPlayed(System.currentTimeMillis());
        data.capturePlayer(player.position().x, player.position().z,
                player.yaw(), player.pitch(), player.inventory());
        int chunks = WorldSaver.save(game.screens().storage(), summary, data, world,
                player.inventory());
        autosaveTimer = 0.0f;
        return chunks;
    }

    /** World shown by this screen. */
    public World world() {
        return world;
    }

    /**
     * Player controlled by this screen, which is also the player a command works on.
     *
     * @return the player of this world
     */
    @Override
    public Player player() {
        return player;
    }

    /**
     * Chat a command writes its answers into.
     *
     * @return the log of the chat of this world
     */
    @Override
    public ChatLog log() {
        return chatLog;
    }

    /**
     * Seed of the world this screen plays.
     *
     * @return the seed the terrain was generated from
     */
    @Override
    public int seed() {
        return data.seed();
    }

    @Override
    public GameMode gameMode() {
        return data.gameMode();
    }

    @Override
    public void setGameMode(GameMode mode) {
        if (mode == null || mode == data.gameMode()) {
            return;
        }
        data.setGameMode(mode);
        if (mode == GameMode.SURVIVAL) {
            // The creative screen belongs to the mode: leaving it closes the screen,
            // so the next press of the inventory key opens the one that fits.
            creativeGui.close();
        }
        LOGGER.info("Game mode of '{}' switched to {}", summary.displayName(), mode.modeName());
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
        renderCubes();

        countFrames(delta);
        renderInterface(delta);

        logDebugStatistics();
        countPlayTime(delta);
    }

    /**
     * Draws one frame of the world as cubes.
     * <p>
     * The camera is the eye of the player: it stands where the eyes are, {@link Constants#PLAYER_EYE_HEIGHT}
     * above the feet, and looks where the view points, so the world is seen from inside the body and the
     * pointer turns it all the way around. Its field of view is set with the window, so a resize does not
     * stretch the world, and the depth buffer is cleared and tested, which is what makes a block hide the
     * one behind it.
     */
    private void renderCubes() {
        cubeCamera.viewportWidth = Gdx.graphics.getWidth();
        cubeCamera.viewportHeight = Gdx.graphics.getHeight();
        player.lookDirection(lookDirection);
        float eyeX = player.position().x;
        float eyeY = player.position().y + Constants.PLAYER_EYE_HEIGHT;
        float eyeZ = player.position().z;
        if (thirdPerson) {
            // The view from behind stands where the eye looks from, only further back and a little
            // higher, and it looks at the eye of the body, which is what puts the figure in front of
            // the camera instead of in it.
            float back = thirdPersonDistance(eyeX, eyeY, eyeZ);
            cubeCamera.position.set(eyeX, eyeY, eyeZ)
                    .mulAdd(lookDirection, -back)
                    .add(0.0f, THIRD_PERSON_UP, 0.0f);
            cubeCamera.direction.set(eyeX - cubeCamera.position.x,
                    eyeY - cubeCamera.position.y, eyeZ - cubeCamera.position.z).nor();
        } else {
            cubeCamera.position.set(eyeX, eyeY, eyeZ);
            cubeCamera.direction.set(lookDirection);
        }
        cubeCamera.up.set(0.0f, 1.0f, 0.0f);
        // The zoom of the view is the field of view of the camera: rolling the wheel in narrows what the
        // eye sees, which is what moving closer to the world looks like from inside a body.
        cubeCamera.fieldOfView = CUBE_FIELD_OF_VIEW / zoom;
        cubeCamera.update();

        Gdx.gl.glClearColor(SKY.r, SKY.g, SKY.b, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        // The state the world is drawn with is set up here and never inherited, the way the interface does
        // it for its own sprites, see #renderInterface. The batch of the interface switches the depth mask
        // off while it draws, and the passes of the world leave the culling the way the last of them wanted
        // it: a world drawn without a depth mask keeps no depth at all and its back faces are drawn as well,
        // so the face that was drawn last wins wherever two of them overlap. What the pause menu then shows
        // behind its dark layer is the bedrock at the bottom of the terrain instead of the grass the player
        // stands on, because the sections of the bottom are drawn last.
        Gdx.gl.glDepthMask(true);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        cubeRenderer.render(world, cubeCamera, SKY);
        cubeRenderer.renderSelection(cubeCamera, target);
        renderEntities();
        renderHand();
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
    }

    /**
     * How far the view of the body from behind stands behind the eye.
     * <p>
     * The distance is walked back from the eye and stops in front of the first block that hides what is
     * behind it: a camera that stands inside the terrain shows the inside of a block instead of the
     * player, which is what a fixed distance does in a valley, in a cave or under a tree.
     *
     * @param eyeX world X coordinate of the eye of the body
     * @param eyeY world Y coordinate of the eye of the body
     * @param eyeZ world Z coordinate of the eye of the body
     * @return the distance to use, at least {@link #THIRD_PERSON_CLOSEST}
     */
    private float thirdPersonDistance(float eyeX, float eyeY, float eyeZ) {
        for (float step = THIRD_PERSON_CLOSEST; step <= THIRD_PERSON_BACK; step += 0.5f) {
            int x = MathUtils.floor(eyeX - lookDirection.x * step);
            int y = MathUtils.floor(eyeY + THIRD_PERSON_UP - lookDirection.y * step);
            int z = MathUtils.floor(eyeZ - lookDirection.z * step);
            Block block = world.peekBlock(x, y, z);
            if (!block.isAir() && !block.isTransparent()) {
                return Math.max(THIRD_PERSON_CLOSEST, step - 0.5f);
            }
        }
        return THIRD_PERSON_BACK;
    }

    /**
     * Draws the bodies of the world: the player and, one day, every other living thing.
     * <p>
     * A body is drawn with the very shader and the very pictures the terrain is drawn with, so it fades
     * into the sky like a block does and needs no pass of its own; the renderer of a body is registered
     * per entity type, see {@link #entityRenderers}. The body the camera stands in is left out while the
     * view is the one from inside it, see {@link #humanoid}.
     */
    private void renderEntities() {
        if (cubeRenderer == null || humanoid == null || !humanoid.isReady()) {
            return;
        }
        blockShader.begin(cubeCamera, pictures, SKY, cubeCamera.far * FOG_START_SHARE,
                cubeCamera.far);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        entityRenderers.render(world.entities().all());
        blockShader.end();
    }

    /**
     * Draws what the hand of the player holds, in front of the world.
     * <p>
     * The item is part of the view and not of the world: it is drawn with the field of view of a pair of
     * eyes, so zooming the world in does not push it out of the picture, and the depth buffer is cleared
     * before it is drawn, so nothing the world holds can cut into it. A view of the body from behind draws
     * no hand, because there the body itself carries the arm, see {@link #renderEntities()}.
     */
    private void renderHand() {
        if (humanoid == null || thirdPerson) {
            return;
        }
        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
        cubeCamera.fieldOfView = CUBE_FIELD_OF_VIEW;
        cubeCamera.update();
        humanoid.renderHand(cubeCamera, blockShader, player.inventory().heldStack());
    }

    /**
     * Draws one frame of the world without advancing it.
     * <p>
     * Used by the pause menu, which draws the world behind its own widgets so the
     * player still sees where they are. Simulation and input stay untouched, and the
     * played time is not counted while the world is frozen.
     *
     * @param delta time since the last frame in seconds
     */
    public void renderFrozen(float delta) {
        clearScreen();
        countFrames(delta);

        // The camera of the flat view follows the player here as well, so the pointer keeps deciding
        // where the player looks while the pause menu is up, see GameScreen#render.
        worldViewport.apply();
        centerCameraOnPlayer();

        renderCubes();
        renderInterface(delta);
    }

    /**
     * Counts the played time and writes the world now and then.
     *
     * @param delta time since the last frame in seconds
     */
    private void countPlayTime(float delta) {
        data.addPlayedMillis((long) (delta * 1000.0f));
        autosaveTimer += delta;
        if (autosaveTimer >= AUTOSAVE_INTERVAL) {
            LOGGER.info("Autosaving world '{}'", summary.displayName());
            save();
        }
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
        // The interface is drawn with blending and without the depth test or the culling of the world's
        // boxes, and the passes of the world leave those the way a block wants them. The state is <b>set up
        // here</b> and never inherited: a pass before it that draws nothing - a hand that is empty draws no
        // item at all - used to skip the one call that switched the culling back off, and the whole
        // interface of the game, hotbar and panels alike, was culled away with it.
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        updateInterfaceMouse();
        creativeGui.update(delta);

        batch.setProjectionMatrix(uiViewport.getCamera().combined);
        batch.begin();
        hotbarGui.render(batch, player.inventory(),
                interfaceMouse.x, interfaceMouse.y, !isInterfaceOpen());
        creativeGui.render(batch, interfaceMouse.x, interfaceMouse.y);
        inventoryGui.render(batch, interfaceMouse.x, interfaceMouse.y);
        machineGui.render(batch, interfaceMouse.x, interfaceMouse.y);
        chatOverlay.render(batch, chat, uiViewport);
        drawCrosshair();
        drawPerformance();
        // Every pass that draws into a frame of its own - the icon of a block is drawn that way - leaves
        // the viewport of the window changed behind it, and the geometry above is only sent to the card
        // when the batch ends. The viewport is applied once more here, so the interface is drawn through
        // the viewport it was laid out in, whatever ran in the middle.
        uiViewport.apply();
        batch.end();
        batch.setColor(Color.WHITE);

        stage.act(delta);
        stage.draw();
    }

    /**
     * Counts a frame and works out how many the last second held.
     * <p>
     * A line that changes once a second is readable where a number that jumps every frame is not; the
     * same counter keeps the line cheap, because the frame rate is worked out once per second and not
     * once per frame.
     *
     * @param delta time since the last frame in seconds
     */
    private void countFrames(float delta) {
        framesThisSecond++;
        fpsSeconds += delta;
        if (fpsSeconds >= 1.0f) {
            frameRate = Math.round(framesThisSecond / fpsSeconds);
            framesThisSecond = 0;
            fpsSeconds = 0.0f;
        }
    }

    /**
     * Writes how fast the game runs and how much of the world is being drawn.
     * <p>
     * The frame rate alone says that something is slow and not what, so the line carries the number of
     * chunks in memory, the sections the cube renderer drew, the meshes it sent and the sections whose
     * meshes it holds: a frame rate that drops while the number of meshes jumps is a mesher that is
     * running again and again, and one that drops while the sections stay still is the drawing itself.
     * <p>
     * The caller has to have begun the batch of the interface.
     */
    private void drawPerformance() {
        PixelFont font = game.font();
        font.setColor(Color.WHITE);
        font.drawShadowed(batch, performanceText(), PERFORMANCE_MARGIN,
                uiViewport.getWorldHeight() - PERFORMANCE_MARGIN);
    }

    /** The line {@link #drawPerformance()} writes. */
    private String performanceText() {
        if (cubeRenderer == null) {
            return "FPS " + frameRate + " | flat | chunks " + world.chunkCount();
        }
        return "FPS " + frameRate + " | chunks " + world.chunkCount()
                + " | sections " + cubeRenderer.drawnSectionCount()
                + " | meshes " + cubeRenderer.drawnMeshCount()
                + " | cached " + cubeRenderer.cachedSectionCount();
    }

    /**
     * Draws the little mark in the middle of the screen that shows where the view points.
     * <p>
     * A world drawn as cubes is aimed at with the middle of the screen: the cell a click touches is the
     * nearest one along the line through that middle, see the target of the player, so a small cross there
     * is what tells a player where the hand will land. It is drawn only while the world is played - an
     * interface owns the pointer, and a view from above aims with the pointer itself, so neither needs a
     * mark.
     * <p>
     * The arms keep a gap from the middle, so the cell under the mark stays visible, and the grey is light
     * enough to read against a bright sky and dark enough to read against stone. The four bars are whole
     * pixels wide, which is what keeps the mark sharp when the interface is blown up.
     * <p>
     * The caller has to have begun the batch of the interface.
     */
    private void drawCrosshair() {
        if (cubeRenderer == null || isInterfaceOpen()) {
            return;
        }
        TextureRegion pixel = game.textures().whitePixel();
        if (pixel == null) {
            return;
        }
        float middleX = Math.round(uiViewport.getWorldWidth() * 0.5f);
        float middleY = Math.round(uiViewport.getWorldHeight() * 0.5f);
        float half = CROSSHAIR_THICKNESS * 0.5f;
        batch.setColor(CROSSHAIR_COLOR);
        batch.draw(pixel, middleX - CROSSHAIR_GAP - CROSSHAIR_ARM, middleY - half,
                CROSSHAIR_ARM, CROSSHAIR_THICKNESS);
        batch.draw(pixel, middleX + CROSSHAIR_GAP, middleY - half,
                CROSSHAIR_ARM, CROSSHAIR_THICKNESS);
        batch.draw(pixel, middleX - half, middleY - CROSSHAIR_GAP - CROSSHAIR_ARM,
                CROSSHAIR_THICKNESS, CROSSHAIR_ARM);
        batch.draw(pixel, middleX - half, middleY + CROSSHAIR_GAP,
                CROSSHAIR_THICKNESS, CROSSHAIR_ARM);
        batch.setColor(Color.WHITE);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        worldViewport.update(width, height, false);
        streamChunks(true);
        centerCameraOnPlayer();
    }

    /**
     * Lets the pointer go while the world is not the screen in front.
     * <p>
     * A captured pointer never leaves the window and does not name a place on screen, so a menu that
     * opened on top of the world could not be clicked at all. The world hands the pointer over here and
     * asks for it again while it is played, see {@code GameScreen#handleInputAndUpdate}.
     */
    @Override
    public void hide() {
        Gdx.input.setCursorCatched(false);
        inputHandler.setLookCaptured(false);
        super.hide();
    }

    @Override
    public void pause() {
        Gdx.input.setCursorCatched(false);
        inputHandler.setLookCaptured(false);
        super.pause();
    }

    @Override
    public void dispose() {
        // The world is written before the screen goes away, which covers leaving a
        // world and closing the game alike.
        try {
            save();
        } catch (RuntimeException e) {
            LOGGER.error("Unable to save the world while closing it", e);
        }
        batch.dispose();
        if (humanoid != null) {
            humanoid.dispose();
        }
        if (blockIconRenderer != null) {
            // The icons are textures of this screen, and the cache that hands them out - which outlives
            // the screen - has to stop asking for them before they are gone.
            game.textures().setBlockIconRenderer(null);
            blockIconRenderer.dispose();
        }
        if (cubeRenderer != null) {
            cubeRenderer.dispose();
        }
        super.dispose();
    }

    /**
     * Applies the player input, advances the simulation and reveals new terrain.
     * <p>
     * While an interface is open the world still runs, but only the interface is
     * driven: the player is stopped and neither the movement keys nor the wheel are
     * forwarded. The collected wheel notches are dropped in that case, so closing
     * the screen never jumps the camera.
     *
     * @param delta time since the last frame in seconds
     */
    private void handleInputAndUpdate(float delta) {
        updateInterfaceMouse();
        handleInterfaceKeys();
        chat.update(delta);
        chatOverlay.update(delta);

        if (inputHandler.consumePauseToggle()) {
            if (machineGui.isOpen()) {
                // The escape key closes whatever is on top first.
                machineGui.close();
            } else if (creativeGui.isOpen()) {
                // The escape key closes whatever is on top first.
                creativeGui.close();
            } else if (inventoryGui.isOpen()) {
                inventoryGui.close();
            } else {
                LOGGER.info("Pause menu requested through the keyboard");
                game.screens().show(ScreenManager.ScreenType.PAUSE);
                return;
            }
        }
        if (inputHandler.consumeFullscreenToggle()) {
            toggleFullscreen();
        }

        float zoomSteps = inputHandler.consumeZoomSteps();

        // The pointer turns the view while the world is played and is handed back to the interface the
        // moment a screen opens, so a slot can be clicked. A captured pointer never leaves the window,
        // which is what lets a player turn all the way around.
        boolean captured = cubeRenderer != null && !isInterfaceOpen();
        Gdx.input.setCursorCatched(captured);
        inputHandler.setFirstPerson(cubeRenderer != null);
        inputHandler.setLookCaptured(captured);
        if (creativeGui.isOpen()) {
            // While the creative inventory is up the wheel walks through its list
            // instead of zooming the camera.
            creativeGui.scrolled(zoomSteps);
        }
        if (isInterfaceOpen()) {
            // The world keeps running while an interface is open: a drop lying next to
            // the player is still picked up, which is what lets a full inventory take an
            // item after one was lifted onto the mouse. The player itself stands still
            // and neither aims nor mines, so the interface stays in charge of the input.
            player.halt();
            target = null;
        } else {
            applyWheel(zoomSteps);
            applyZoomDemand(delta);
            // The world is played: the keyboard walks and the pointer turns the view, see InputHandler.
            // A screen that is open keeps both to itself, which is why this runs here and not above.
            inputHandler.update(player, camera);
            if (inputHandler.isJumpDown()) {
                player.jump();
            }
            updateInteraction(delta);
        }
        // The bodies are moved with a step no slow frame may stretch, see LONGEST_PHYSICS_STEP.
            float step = Math.min(delta, LONGEST_PHYSICS_STEP);
            world.entities().update(world, step, player.blockX(), player.blockZ());
        tickWorld(delta);
        closeMachineIfUnloaded();
        streamChunks(false);
    }

    /**
     * Closes the screen of a machine that is no longer where it was.
     * <p>
     * A chunk the player walked away from is dropped from memory, and its machines leave
     * with it. A screen that stayed open would belong to a block nobody can reach: what a
     * player put into it would go into an entity that is written as soon as the chunk is
     * loaded again at best, and into nothing at all at worst. Closing it hands the stack
     * on the mouse back to the player, see
     * {@link com.philia093.neofactory.gui.container.ContainerMenu#close()}.
     */
    private void closeMachineIfUnloaded() {
        if (openMachine == null || !machineGui.isOpen()) {
            return;
        }
        BlockEntity current = world.blockEntity(openMachine.x(), openMachine.y(),
                openMachine.z());
        if (current != openMachine) {
            LOGGER.info("Machine at block ({}, {}) is gone, its screen is closed",
                    openMachine.x(), openMachine.y());
            machineGui.close();
            openMachine = null;
        }
    }

    /**
     * Advances the block entities of the world, the machines and whatever else a block
     * carries, by the ticks the frame is worth.
     * <p>
     * Everything a machine does per second is settled in ticks and not in frames, which
     * is what keeps a furnace that was left alone from working faster on a machine with a
     * high frame rate than on a slow one, see {@link TickClock}.
     *
     * @param delta time since the last frame in seconds
     */
    private void tickWorld(float delta) {
        int ticks = tickClock.advance(delta);
        for (int tick = 0; tick < ticks; tick++) {
            world.tick(TickClock.TICK_SECONDS);
        }
    }

    /**
     * {@code true} while something of the interface covers the world.
     * <p>
     * These are the screens that take the input while the world keeps running: the
     * inventory of the player, the creative inventory and the chat. The pause menu is a
     * screen of its own and stops the simulation by not drawing the game screen at all.
     * <p>
     * A screen listed here also keeps the chat closed, because the keys that open it
     * belong to the panel as long as anything is up - so a command can never be typed
     * behind an open container. A new screen has to be added to this list to get that
     * for itself.
     *
     * @return {@code true} while the player types or moves items around
     */
    private boolean isInterfaceOpen() {
        return inventoryGui.isOpen() || creativeGui.isOpen() || machineGui.isOpen()
                || chat.isOpen();
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
        // A body that stands in the world aims with its eyes: the cell is what the ray from them meets
        // inside the reach, and the face it entered through is what a block is built against, see
        // BlockTargeting#selectInSight.
        target = BlockTargeting.selectInSight(world, player, player.lookDirection(lookDirection));

        if (humanoid != null) {
            // The hand sways with the steps of the body, which is what the speed over the ground says.
            boolean walking = Math.abs(player.velocity().x) + Math.abs(player.velocity().z) > 0.05f;
            humanoid.update(delta, walking);
        }
        boolean broken = mining.update(delta, world, target, player.inventory().heldStack(),
                inputHandler.isBreakingDown());
        if (broken) {
            LOGGER.info("Broke block ({}, {}, {})", target.x(), target.y(), target.z());
            if (humanoid != null) {
                humanoid.swing();
            }
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
        if (openMachine()) {
            return true;
        }
        String itemName = player.inventory().heldStack().item().displayName();
        int x = target.x();
        int y = target.y();
        int z = target.z();
        if (!BlockPlacer.place(world, player, target, player.inventory())) {
            return false;
        }
        if (humanoid != null) {
            humanoid.swing();
        }
        LOGGER.info("Built {} into block ({}, {}, {})", itemName, x, y, z);
        return true;
    }

    /**
     * Opens the screen of the machine the player aims at.
     * <p>
     * Using a block that carries a machine shows it instead of building the held block: a
     * machine holds slots, a buffer and tanks, and all of it belongs to the block that was
     * placed. A cell that carries no machine reports {@code false}, so the build button
     * keeps building wherever the player aims at.
     *
     * @return {@code true} when a machine stood there and its screen is up now
     */
    private boolean openMachine() {
        BlockEntity entity = world.blockEntity(target.x(), target.y(), target.z());
        if (!(entity instanceof MachineBlockEntity machine)) {
            return false;
        }
        machineGui.open(machine.machine(), player.inventory());
        openMachine = machine;
        LOGGER.info("Opened {} at block ({}, {}) of layer {}", machine.machine().name(),
                target.x(), target.y(), target.z());
        return true;
    }

    /** Applies the keys that belong to the interface instead of the world. */
    private void handleInterfaceKeys() {
        if (inputHandler.consumeViewToggle()) {
            // The view of a world is either the one from inside the body or the one of it from behind,
            // see renderEntities: only the second shows a player themselves.
            thirdPerson = !thirdPerson;
            LOGGER.info("The view is now the one {}", thirdPerson
                    ? "of the body from behind" : "from inside the body");
        }
        if (inputHandler.consumeInventoryToggle()) {
            if (gameMode() == GameMode.CREATIVE) {
                // In creative mode the inventory key opens the list of every item, see
                // CreativeInventoryGui.
                creativeGui.toggle();
                LOGGER.info("Creative inventory {}", creativeGui.isOpen() ? "opened" : "closed");
            } else {
                inventoryGui.toggle();
                LOGGER.info("Inventory {}", inventoryGui.isOpen() ? "opened" : "closed");
            }
        }
        if (inputHandler.consumeDrop()) {
            dropRequested(inputHandler.isDropWholeStack());
        }
        int hotbarSlot = inputHandler.consumeHotbarSelection();
        if (hotbarSlot >= 0) {
            player.inventory().setSelectedSlot(hotbarSlot);
        }
        int viewStep = inputHandler.consumeViewDistanceStep();
        if (viewStep != 0) {
            // Fewer chunks means less memory and a smaller save game, more chunks
            // mean more terrain on screen; both are worth trying while playing.
            streamer.setViewDistance(streamer.viewDistance() + viewStep);
        }
    }

    /**
     * Routes the collected wheel notches.
     * <p>
     * The wheel selects hotbar slots, the way it does in the original game: rolling
     * forwards walks towards the first slot. Zooming moved to the keys {@code -} and
     * {@code =} when the wheel was taken over by the hotbar; the wheel still zooms
     * while {@code CTRL} is held, which keeps the familiar gesture available without
     * mixing the two up, see {@link InputHandler#isZoomScrollDown()}.
     *
     * @param steps wheel notches collected since the last frame
     */
    private void applyWheel(float steps) {
        if (steps == 0.0f) {
            return;
        }
        if (inputHandler.isZoomScrollDown()) {
            applyZoom(steps);
            return;
        }
        player.inventory().scrollSelection(steps > 0.0f ? -1 : 1);
    }

    /**
     * Applies a multiplicative zoom and keeps it inside the allowed range.
     *
     * @param steps wheel notches to zoom by, positive means rolled forwards
     */
    private void applyZoom(float steps) {
        // Rolling the wheel forwards magnifies, which means a smaller camera zoom.
        float factor = (float) Math.pow(Constants.ZOOM_STEP, -steps);
        zoom = MathUtils.clamp(zoom * factor, Constants.ZOOM_MIN, Constants.ZOOM_MAX);
    }

    /**
     * Applies continuous zoom from the keyboard.
     * <p>
     * Holding {@code =} magnifies and holding {@code -} shrinks the view at a
     * fixed notch rate, which is the way the camera is zoomed now that the wheel
     * belongs to the hotbar.
     *
     * @param delta time since the last frame in seconds
     */
    private void applyZoomDemand(float delta) {
        float demand = inputHandler.zoomKeyDemand();
        if (demand == 0.0f) {
            return;
        }
        applyZoom(demand * Constants.ZOOM_KEY_STEPS_PER_SECOND * delta);
    }

    /**
     * Unprojects the mouse into the virtual pixels of the interface.
     * <p>
     * Called once per frame before the input handling, so the wheel routing and
     * the interface rendering read the same position.
     */
    private void updateInterfaceMouse() {
        uiViewport.unproject(Gdx.input.getX(), Gdx.input.getY(), interfaceMouse);
    }

    /**
     * Places the camera on the player.
     * <p>
     * This is what keeps the player centered on screen: the world coordinates of the player are the world
     * coordinates of the camera center. The zoom of the view is the field of view of the camera that
     * stands in the world, so this only keeps the flat camera of the interface in step.
     */
    private void centerCameraOnPlayer() {
        camera.position.set(player.position().x, player.position().z, 0.0f);
        camera.up.set(0.0f, 1.0f, 0.0f);
        camera.direction.set(0.0f, 0.0f, -1.0f);
        camera.update();
    }

    /**
     * Makes sure every chunk visible at the current zoom is in memory, and drops
     * the chunks the player walked away from.
     * <p>
     * The radius is derived from the viewport size rather than being a constant,
     * so zooming out reveals the terrain instead of showing empty space. One chunk
     * is added on top of the visible area: terrain and decorations are planted
     * when a chunk is finished, so the margin keeps new trees from appearing out of
     * nothing inside the view.
     * <p>
     * Loading is spread over the frames, dropping is not, see
     * {@link ChunkStreamer}. A changed chunk is written into the save game before it
     * is dropped, which is what makes the memory of a long session stay flat without
     * losing what the player built.
     *
     * @param immediate {@code true} to fill the whole area in one go, used while the
     *                  world is opened and after a resize
     */
    private void streamChunks(boolean immediate) {
        float visibleBlocksX = worldViewport.getWorldWidth() * zoom / Constants.BLOCK_SIZE;
        float visibleBlocksY = worldViewport.getWorldHeight() * zoom / Constants.BLOCK_SIZE;
        float halfBlocks = Math.max(visibleBlocksX, visibleBlocksY) * 0.5f;
        float blockX = player.position().x / Constants.BLOCK_SIZE;
        float blockY = player.position().z / Constants.BLOCK_SIZE;

        if (immediate) {
            streamer.fill(world, blockX, blockY, halfBlocks);
        } else {
            streamer.update(world, blockX, blockY, halfBlocks);
        }
    }

    /**
     * Which inventory screen is up and which tab it shows, for the status line.
     *
     * @return a short description of the open screen
     */
    private String inventoryText() {
        if (creativeGui.isOpen()) {
            return "creative(" + creativeGui.inventory().selectedTab().name() + ")";
        }
        return inventoryGui.isOpen() ? "open" : "closed";
    }

    /** Writes a short status line to the log now and then. */
    private void logDebugStatistics() {
        debugFrameCounter++;
        if (debugFrameCounter < DEBUG_LOG_INTERVAL) {
            return;
        }
        debugFrameCounter = 0;
        LOGGER.info("World '{}' | Block ({}, {}) | zoom {} | mode {} | chunks {} "
                        + "(view {}, stored {}, changed {}) | stream +{}/-{} | entities {} | machines {} "
                        + "| ticks {} | hotbar {} | inventory {} | chat {} | target {} | gui {} | fps {}",
                summary.displayName(),
                player.blockX(), player.blockY(), String.format("%.2f", zoom),
                gameMode().modeName(),
                world.chunkCount(), streamer.viewDistance(),
                world.storedChunkCount(), world.modifiedChunkCount(),
                streamer.lastLoaded(), streamer.lastUnloaded(),
                world.entities().count(),
                world.blockEntityCount(),
                tickClock.tickCount(),
                player.inventory().selectedSlot(),
                inventoryText(),
                chat.isOpen() ? "typing '" + chat.text() + "'" : "closed",
                targetText(),
                uiViewport.scale(),
                Gdx.graphics.getFramesPerSecond());
    }

    /**
     * Drops what the drop key points at.
     * <p>
     * While the inventory screen is open the slot under the mouse is dropped, which lets
     * the key work on whatever the player points at. During play the selected hotbar slot
     * is dropped in front of the player, a single item or the whole stack.
     *
     * @param wholeStack {@code true} to drop the whole stack, {@code false} for one item
     */
    private void dropRequested(boolean wholeStack) {
        if (inventoryGui.isOpen()) {
            inventoryGui.dropAt(interfaceMouse.x, interfaceMouse.y, wholeStack);
            return;
        }
        PlayerInventory inventory = player.inventory();
        ItemStack held = inventory.heldStack();
        if (held.isEmpty()) {
            return;
        }
        int amount = wholeStack ? held.count() : 1;
        ItemStack dropped = ItemStack.of(held.item(), amount);
        held.setCount(held.count() - amount);
        if (held.isEmpty()) {
            inventory.set(inventory.selectedSlot(), ItemStack.EMPTY);
        }
        drops.drop(dropped, player.position().x, player.position().z);
    }

    /** Short description of the targeted cell, used by the status log. */
    private String targetText() {
        if (target == null) {
            return "none";
        }
        return target.x() + "," + target.y() + "," + target.z()
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
        inventory.add(ItemStack.of(Items.COBBLESTONE, 64));
        inventory.add(ItemStack.of(Items.BEDROCK, 1));

        inventory.add(ItemStack.of(Items.LOG_OAK, 12));
        inventory.add(ItemStack.of(Items.PLANKS_OAK, 40));
        inventory.add(ItemStack.of(Items.LEAVES_OAK, 8));
        inventory.add(ItemStack.of(Items.SAPLING_OAK, 16));
        inventory.add(ItemStack.of(Items.GLASS, 32));
        inventory.add(ItemStack.of(Items.STONE_BRICK, 32));
        inventory.add(ItemStack.of(Items.STONE_SLAB, 32));
        inventory.add(ItemStack.of(Items.BRICK, 32));
        inventory.add(ItemStack.of(Items.GLOWSTONE, 16));
        inventory.add(ItemStack.of(Items.OBSIDIAN, 8));
        inventory.add(ItemStack.of(Items.CRAFTING_TABLE, 1));
        inventory.add(ItemStack.of(Items.ANVIL, 1));
        inventory.add(ItemStack.of(Items.CAULDRON, 1));
        inventory.add(ItemStack.of(Items.TORCH, 16));
        inventory.add(ItemStack.of(Items.LADDER, 8));
        inventory.add(ItemStack.of(Items.COAL_ORE, 5));
        inventory.add(ItemStack.of(Items.IRON_ORE, 5));
        inventory.add(ItemStack.of(Items.SANDSTONE, 7));
        inventory.add(ItemStack.of(Materials.IRON.ingot(), 24));
        inventory.add(ItemStack.of(Materials.GOLD.ingot(), 6));

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
