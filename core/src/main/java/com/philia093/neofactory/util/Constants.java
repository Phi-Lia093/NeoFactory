package com.philia093.neofactory.util;

/**
 * Central place for every magic number used by the game.
 * <p>
 * One block covers {@link #TILE_SIZE} world units, which is also the pixel size
 * of a block texture. Terrain and entity logic therefore works with block
 * coordinates and converts them into world units when needed, while the renderer
 * places a tile at {@code blockX * TILE_SIZE}.
 */
public final class Constants {

    /** Application / window title. */
    public static final String WINDOW_NAME = "NeoFactory";

    /** Size of a single block in world units, equal to the texture size in pixels. */
    public static final int TILE_SIZE = 16;

    /**
     * Size of a single block in world units, the unit the world of cubes is measured in.
     * <p>
     * One block covers one unit, so a coordinate of {@code 5.5} is the middle of the block
     * {@code 5} and the height of a player is written as {@code 1.8} instead of {@code 28.8}. The
     * flat engine measured the world in pixels of the art instead, see {@link #TILE_SIZE}, which is
     * why every camera, movement and save number of the old world carries a factor of sixteen in
     * it. As the world grows its third axis, the two are told apart: this is the size of a block,
     * {@code TILE_SIZE} is the size of a picture, and the old constant is retired once nothing
     * counts in pixels any more.
     */
    public static final float BLOCK_SIZE = 1.0f;

    /** Side length in blocks of a single square chunk. */
    public static final int CHUNK_SIZE = 16;

    /** Side length in blocks of a single section, the unit a chunk is stored and meshed in. */
    public static final int SECTION_SIZE = 16;

    /**
     * Amount of sections a chunk holds along its vertical axis.
     * <p>
     * Sixteen sections make a column of {@link #MAX_Y} blocks, which is the height of the original
     * game. A chunk only pays for the sections that hold something, see
     * {@link com.philia093.neofactory.world.Section}, so the number is a ceiling and not a cost: it
     * says how tall a world may become before the storage format has to be told about it.
     */
    public static final int SECTION_COUNT = 16;

    /** Lowest block height of the world. */
    public static final int MIN_Y = 0;

    /**
     * Highest block height of the world.
     * <p>
     * A world is a column of {@link #SECTION_COUNT} sections of {@link #SECTION_SIZE} blocks. The
     * height travels with every stored chunk as the index of its sections and not as a count, so
     * moving this ceiling later is a change of this one number.
     */
    public static final int MAX_Y = MIN_Y + SECTION_COUNT * SECTION_SIZE - 1;

    /**
     * Height the surface of a body of water stands at.
     * <p>
     * A river, a lake and the sea fill up to this height, which is what makes water find its level
     * across a landscape instead of following every dip of the ground.
     */
    public static final int SEA_LEVEL = 64;

    /**
     * Player walking speed in blocks per second at zoom level one.
     * <p>
     * The number is the pace of the original game, which is what a body that stands in the world feels
     * right with: fast enough to cross a field, slow enough to look around while walking. The movement
     * code multiplies it by {@link #BLOCK_SIZE}, the size of a block of the world, and by nothing else:
     * how fast a body walks does not depend on where its view looks.
     */
    public static final float PLAYER_SPEED = 4.3f;

    /**
     * Height of the eyes of the player in blocks.
     * <p>
     * A body stands on its feet and looks out of its eyes, so the camera of a player that stands in the
     * world is put this far above the feet - a little under the top of the body, the way a person is
     * built.
     */
    /**
     * Pull of the world on a body, in blocks per second squared.
     * <p>
     * A body that steps off a ledge falls, and this number tells how fast. It is a little under the one
     * the original game uses, which keeps a fall feeling the same without a jump that overshoots.
     */
    public static final float GRAVITY = 28.0f;

    /**
     * Speed a jump leaves the ground with, in blocks per second.
     * <p>
     * With {@link #GRAVITY} this lifts the body a little over one block, which is exactly what a step of
     * one block asks for, see {@code Player#jump()}.
     */
    public static final float JUMP_SPEED = 8.4f;

    /**
     * Side of a dropped item in blocks, also the size of the cube it is drawn as.
     * <p>
     * It is what a body of the world measures its box with, the way a player measures with
     * {@link #PLAYER_HITBOX} and {@link #PLAYER_HEIGHT}, see {@code ItemEntity}.
     */
    public static final float ITEM_SIZE = 0.4f;

    /**
     * Speed a body climbs a ladder with, in blocks per second.
     * <p>
     * Slower than {@link #JUMP_SPEED}: the jump key climbs a ladder, but as a steady climb up the rungs
     * and not as a jump of the open air, see {@code Player#jump()}.
     */
    public static final float LADDER_CLIMB_SPEED = 3.0f;

    /**
     * Speed a body sinks on a ladder while it holds on, in blocks per second.
     * <p>
     * A ladder carries a body instead of letting it fall: letting go of the rungs is a slow slide down and
     * not the fall of the open air, see {@code Player#update}.
     */
    public static final float LADDER_SINK_SPEED = 2.0f;

    public static final float PLAYER_EYE_HEIGHT = 1.62f;

    /**
     * Degrees the view turns per pixel the pointer moves.
     * <p>
     * The pointer is captured while the world is played, so it reports how far it moved instead of
     * where it is: this factor is what turns that movement into a turn of the view, and it is the one
     * number a player would ask to change if the view felt too fast or too slow.
     */
    public static final float MOUSE_SENSITIVITY = 0.08f;

    /** Highest angle the view may look up or down, in degrees, just short of straight up. */
    public static final float PITCH_LIMIT = 89.0f;

    /**
     * Field of view of the eye that stands in the world, in degrees, measured across the vertical axis.
     * <p>
     * The number is the one a first-person view of the original game uses. Everything that is placed in
     * the frame of the eye depends on it: the view itself is drawn with it - narrowed when the player
     * zooms in - and the hand of the player is placed so that it stands in the picture this cone shows,
     * see {@code HandPose}. A hand placed for another cone hangs outside the frame and is never seen.
     */
    public static final float VIEW_FIELD_OF_VIEW = 70.0f;

    /** Side length of the square player collision box in blocks. */
    public static final float PLAYER_HITBOX = 0.7f;

    /**
     * Height of the body of the player in blocks.
     * <p>
     * The body of the flat view was a square that slid over the ground and had no height at all. A
     * world of cubes gives it one: a player stands on its feet and is as tall as a person, which is
     * what a doorway, a tunnel and a step have to fit around it.
     */
    public static final float PLAYER_HEIGHT = 1.8f;

    /**
     * Visual scale of the player marker. The marker is an 8 by 8 pixel icon,
     * a scale of two makes it fill a single block tile.
     */
    public static final float PLAYER_ICON_SCALE = 0.125f;

    /**
     * Distance in blocks the player can reach.
     * <p>
     * A block closer than this to the player center may be broken or built on by
     * pointing at it. A cell further away is not used: the action falls back to the
     * block the line of sight meets inside the reach, see the interaction classes of
     * the world package.
     */
    public static final float PLAYER_REACH = 4.5f;

    /**
     * Seconds the shortest break may last.
     * <p>
     * While blocks are broken instantly this is what paces a held button, so that
     * holding it digs a trail of blocks instead of a whole chunk in one frame. A
     * rule that needs more time for a block ignores this value, because a break
     * never finishes faster than the rule allows.
     */
    public static final float MINIMUM_BREAK_TIME = 0.2f;

    /** Amount of blocks visible on the shorter screen axis at zoom level one. */
    public static final float VIEW_BLOCKS = 32.0f;

    /** Smallest camera zoom, the closest view, a block appears twice as large. */
    public static final float ZOOM_MIN = 0.5f;

    /** Largest camera zoom, the most distant view, a block shrinks to a third. */
    public static final float ZOOM_MAX = 3.0f;

    /** Cell size in pixels of a single item icon. */
    public static final int ITEM_ICON_SIZE = 16;

    /** Multiplicative zoom step applied per mouse wheel notch. */
    public static final float ZOOM_STEP = 1.1f;

    /**
     * Wheel notches per second that a held zoom key adds.
     * <p>
     * The keys {@code -} and {@code =} feed the same clamped multiplicative zoom
     * as the wheel, they simply keep rolling while they are held down.
     */
    public static final float ZOOM_KEY_STEPS_PER_SECOND = 6.0f;

    /** Number of chunks generated around the spawn position at world creation. */
    public static final int SPAWN_CHUNK_RADIUS = 2;

    /**
     * Chunks kept around the player.
     * <p>
     * The value is a minimum, not the whole truth: the view is zoomed in and out,
     * so the streamer also keeps every chunk that is visible on screen, see
     * {@link com.philia093.neofactory.world.ChunkStreamer}.
     */
    public static final int CHUNK_VIEW_DISTANCE = 6;

    /** Smallest view distance the player may pick. */
    public static final int CHUNK_VIEW_DISTANCE_MIN = 3;

    /** Largest view distance the player may pick. */
    public static final int CHUNK_VIEW_DISTANCE_MAX = 12;

    /**
     * Extra chunks kept outside the loaded area before anything is dropped.
     * <p>
     * Walking back and forth across a chunk border would otherwise load and unload
     * the same chunk over and over. The margin makes the area that is kept larger
     * than the area that is filled, so a chunk only leaves memory once the player
     * really walked away.
     */
    public static final int CHUNK_UNLOAD_MARGIN = 2;

    /**
     * Chunks generated per frame while revealing terrain.
     * <p>
     * Walking into new terrain costs one chunk after the other instead of one long
     * frame: the budget is spent on the chunks closest to the player, so the
     * terrain directly around them is complete at any time.
     */
    public static final int CHUNK_LOADS_PER_FRAME = 4;

    /** Cell index inside {@code map/map_icons.png} used as the player marker. */
    public static final int MAP_ICON_PLAYER = 0;

    /** Side length in pixels of a single cell inside {@code map/map_icons.png}. */
    public static final int MAP_ICON_CELL_SIZE = 8;

    /** Color used to clear the frame before the world is drawn. */
    public static final float SKY_RED = 0.42f;
    public static final float SKY_GREEN = 0.62f;
    public static final float SKY_BLUE = 0.92f;

    private Constants() {
        // Utility class: never instantiated.
    }
}
