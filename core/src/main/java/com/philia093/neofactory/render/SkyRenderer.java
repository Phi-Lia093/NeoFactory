package com.philia093.neofactory.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.philia093.neofactory.world.DayCycle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Draws the sky of a world: the sun, the moon and the layer of clouds over the terrain.
 * <p>
 * The three of them are the art of the asset pack and no drawing of this class: the sun and the moon of
 * {@code environment/sun.png} and {@code environment/moon_phases.png} are pictures of a glowing body on a
 * black ground and the clouds of {@code environment/clouds.png} are white shapes on a transparent one. The
 * program of the pass therefore reads a texel as <b>its own alpha times how bright it is</b>: the black ground
 * of a body is left out of the picture, which is what makes the body itself the only thing that is drawn, and
 * a cloud keeps the gaps its own transparency gives it. Nothing of the ground of a picture is ever drawn into
 * the sky, and nothing of the sky washes the colour of a body out the way adding it would.
 * <p>
 * Everything here is anchored to the world and not to the eye. A body stands in the direction the clock of
 * the world points at, at a distance of the view, and the layer of clouds is a field of tiles of
 * {@link #CLOUD_TILE} blocks that drifts with the day, so a player who walks under it walks under the same
 * clouds and a player who turns around does not turn the sky with them.
 * <p>
 * The sky is drawn <b>before</b> the world, without a depth test and without writing depth, so a block drawn
 * afterwards covers whatever stood behind it and the sun goes down behind a mountain without anything here
 * knowing about the terrain. A body fades out as it reaches the horizon, which is what keeps the sun of the
 * night - the one below the ground - out of the picture. The state the pass draws with is set up here and not
 * inherited, so it runs before the passes of the world set up their own, see {@code GameScreen#renderCubes}.
 */
public final class SkyRenderer implements Disposable {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Sun of the sky, a glowing body on a black ground. */
    public static final String SUN_PICTURE = "environment/sun.png";

    /** The phases of the moon, {@link #MOON_COLUMNS} to a row, each on a black ground. */
    public static final String MOON_PICTURES = "environment/moon_phases.png";

    /** The layer of clouds, white shapes on a transparent ground. */
    public static final String CLOUD_PICTURE = "environment/clouds.png";

    /** Side of one phase of the moon inside {@link #MOON_PICTURES}, in pixels. */
    public static final int MOON_CELL = 32;

    /** Phases in a row of {@link #MOON_PICTURES}. */
    public static final int MOON_COLUMNS = 4;

    /** Phases of the moon, one to a day, so a world sees a whole moon every eight days. */
    public static final int MOON_PHASES = 8;

    /** Blocks one pixel of {@link #CLOUD_PICTURE} is drawn as. */
    public static final int CLOUD_BLOCKS_PER_PIXEL = 2;

    /** Blocks one copy of the layer of clouds covers, a square of the picture of the same name. */
    public static final int CLOUD_TILE = 256 * CLOUD_BLOCKS_PER_PIXEL;

    /** Tiles of the layer of clouds drawn around the camera, on either side of it. */
    public static final int CLOUD_RADIUS = 1;

    /** Height the layer of clouds is drawn at, above the terrain a player walks on. */
    public static final int CLOUD_Y = 180;

    /** How far the layer of clouds drifts per tick, in blocks. */
    public static final float CLOUD_DRIFT = 0.012f;

    /** Share of the view distance a body of the sky is drawn at, far enough to stand behind anything. */
    private static final float BODY_DISTANCE_SHARE = 0.9f;

    /** How quickly a body fades away as it reaches the horizon. */
    private static final float BODY_FADE = 6.0f;

    /** Quads the batch of the sky is built for, which is the bodies and the tiles of the clouds. */
    private static final int SKY_SPRITES = 4 * 4 + 2;

    /** Vertex program of the sky, the one a batch of libGDX asks for. */
    private static final String VERTEX_SHADER = """
            #version 150
            in vec2 a_position;
            in vec2 a_texCoord0;
            in vec4 a_color;

            uniform mat4 u_projTrans;

            out vec2 v_texCoords;
            out vec4 v_color;

            void main() {
                v_texCoords = a_texCoord0;
                v_color = a_color;
                gl_Position = u_projTrans * vec4(a_position, 0.0, 1.0);
            }
            """;

    /**
     * Fragment program of the sky.
     * <p>
     * How much of a texel is drawn is its own alpha times how bright it is. That one line is what the art of
     * the sky asks for: the sun and the moon are glowing bodies on a black ground and the layer of clouds is
     * white shapes on a transparent one, so a black texel of a body has to leave the sky standing and a white
     * one has to stand in it. The colour of a body is drawn at its own brightness and not added to the sky,
     * because adding a warm body to a bright blue sky saturates three channels at once - which is a white
     * square and not a sun.
     */
    private static final String FRAGMENT_SHADER = """
            #version 150
            in vec2 v_texCoords;
            in vec4 v_color;

            uniform sampler2D u_texture;

            out vec4 fragColor;

            void main() {
                vec4 texel = texture(u_texture, v_texCoords);
                float lit = max(max(texel.r, texel.g), texel.b);
                float share = lit * texel.a;
                if (share <= 0.02) {
                    discard;
                }
                fragColor = vec4(texel.rgb * v_color.rgb, share * v_color.a);
            }
            """;

    /** The batch of the three passes of the sky, owned by this renderer. */
    private final SpriteBatch batch;

    /**
     * Program of the pass, {@code null} when it did not compile.
     * <p>
     * A sky the driver refuses to compile is no sky at all rather than a broken frame: the pictures are left
     * out with it and the world keeps its colour of the hour, see the constructor.
     */
    private final ShaderProgram program;

    /** Picture of the sun, {@code null} when the asset pack does not carry it. */
    private final Texture sunPicture;

    /** Picture holding the phases of the moon, {@code null} when the asset pack does not carry it. */
    private final Texture moonPictures;

    /** Picture of the layer of clouds, {@code null} when the asset pack does not carry it. */
    private final Texture cloudPicture;

    /** The whole picture of the sun, {@code null} while there is none. */
    private final TextureRegion sun;

    /** The phases of the moon, cut once, {@code null} where the picture is missing. */
    private final TextureRegion[] moonPhases = new TextureRegion[MOON_PHASES];

    /** The whole picture of the clouds, {@code null} while there is none. */
    private final TextureRegion clouds;

    /** Matrix placing one quad, reused every frame instead of filling the heap with matrices. */
    private final Matrix4 model = new Matrix4();

    /** Placement of the layer of clouds, the flat plane of a tile lifted to {@link #CLOUD_Y}. */
    private final Matrix4 flat = new Matrix4();

    /** Colour of the pass being written, reused every frame. */
    private final Color colour = new Color();

    /** Where the sun stands right now, a point of the sky no player can reach. */
    private final Vector3 sunPosition = new Vector3();

    /** Where the moon stands right now. */
    private final Vector3 moonPosition = new Vector3();

    /** Direction of the body being written, the way from the eye to it. */
    private final Vector3 direction = new Vector3();

    /**
     * Loads the pictures of the sky.
     * <p>
     * A picture the asset pack does not carry leaves its part of the sky out instead of breaking the frame,
     * so a world with a sun and no clouds can still be played, see {@link #load(String)}.
     */
    public SkyRenderer() {
        this.program = program();
        this.batch = new SpriteBatch(SKY_SPRITES, program);
        this.sunPicture = program == null ? null : load(SUN_PICTURE);
        this.moonPictures = program == null ? null : load(MOON_PICTURES);
        this.cloudPicture = program == null ? null : load(CLOUD_PICTURE);
        this.sun = region(sunPicture);
        this.clouds = region(cloudPicture);
        for (int phase = 0; phase < MOON_PHASES; phase++) {
            moonPhases[phase] = moonPictures == null ? null
                    : new TextureRegion(moonPictures, phase % MOON_COLUMNS * MOON_CELL,
                            phase / MOON_COLUMNS * MOON_CELL, MOON_CELL, MOON_CELL);
        }
        // A tile of the clouds is the flat plane of the batch turned into the ground of the sky and lifted
        // to the height of the layer: the first axis of a tile runs along the world, the second one runs into
        // it, see #drawClouds.
        this.flat.translate(0.0f, CLOUD_Y, 0.0f).rotate(Vector3.X, -90.0f);
    }

    /**
     * Draws the sky of a world.
     *
     * @param camera camera the world is seen through
     * @param worldTime time of the day in ticks, see {@link DayCycle}
     */
    public void render(Camera camera, long worldTime) {
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        batch.setProjectionMatrix(camera.combined);

        float distance = camera.far * BODY_DISTANCE_SHARE;
        float height = DayCycle.sunHeight(worldTime);
        DayCycle.sunDirection(worldTime, direction);
        sunPosition.set(camera.position).mulAdd(direction, distance);
        DayCycle.moonDirection(worldTime, direction);
        moonPosition.set(camera.position).mulAdd(direction, distance);
        // The sun fades away as it reaches the horizon and the moon fades in on the other side, so a body
        // that stands below the ground is never drawn over the terrain. The moon keeps its own brightness:
        // it is what lights the night, see DayCycle.
        drawBody(camera, sun, sunPosition, DayCycle.SUN_SIZE,
                MathUtils.clamp(height * BODY_FADE, 0.0f, 1.0f),
                DayCycle.SUN_RED, DayCycle.SUN_GREEN, DayCycle.SUN_BLUE);
        drawBody(camera, moonPhases[moonPhase(worldTime)], moonPosition, DayCycle.MOON_SIZE,
                MathUtils.clamp(-height * BODY_FADE, 0.0f, 1.0f),
                DayCycle.MOON_RED, DayCycle.MOON_GREEN, DayCycle.MOON_BLUE);
        drawClouds(camera, worldTime);

        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    }

    /**
     * Draws one body of the sky: a quad that turns its face to the eye and stands where the clock puts it.
     * <p>
     * The body is drawn through its own brightness, see {@link #FRAGMENT_SHADER}: the ground of its picture is
     * black and leaves the sky standing, which is what makes the body a body and not a square of a picture.
     * The two axes of the quad are the axes of the camera, so the body keeps its face wherever a player looks.
     *
     * @param camera camera the sky is seen through
     * @param picture art of the body, {@code null} draws nothing
     * @param position where the body stands, a point of the sphere of the sky
     * @param size side of the quad, the dark ground of the picture included
     * @param fade how solid the body is drawn, {@code 0} draws nothing
     * @param red red share of the tint of its art
     * @param green green share of the tint of its art
     * @param blue blue share of the tint of its art
     */
    private void drawBody(Camera camera, TextureRegion picture, Vector3 position, float size, float fade,
            float red, float green, float blue) {
        if (picture == null || fade <= 0.0f) {
            return;
        }
        model.set(camera.view).inv().setTranslation(position);
        batch.setTransformMatrix(model);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.begin();
        batch.setColor(red, green, blue, fade);
        batch.draw(picture, -size * 0.5f, -size * 0.5f, size, size);
        batch.end();
    }

    /**
     * Draws the layer of clouds over the camera.
     * <p>
     * The layer is the picture of the clouds laid on the ground of the sky, one copy of it every
     * {@link #CLOUD_TILE} blocks, and the copies around the camera are drawn. <b>The layer stands in the world
     * and not in the view</b>: which copy of it a player walks under is a question of the world they are in
     * and of the drift of the day, so a player who turns around finds the same clouds behind them and one who
     * walks on walks under them.
     *
     * @param camera camera the sky is seen through
     * @param worldTime time of the day in ticks, see {@link DayCycle}
     */
    private void drawClouds(Camera camera, long worldTime) {
        if (clouds == null) {
            return;
        }
        float drift = cloudDrift(worldTime);
        float tileX = MathUtils.floor((camera.position.x - drift) / CLOUD_TILE) * (float) CLOUD_TILE
                + drift;
        float tileZ = MathUtils.floor(camera.position.z / CLOUD_TILE) * (float) CLOUD_TILE;
        DayCycle.cloudTint(worldTime, colour);
        batch.setTransformMatrix(flat);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.begin();
        batch.setColor(colour);
        for (int aroundX = -CLOUD_RADIUS; aroundX <= CLOUD_RADIUS; aroundX++) {
            for (int aroundZ = -CLOUD_RADIUS; aroundZ <= CLOUD_RADIUS; aroundZ++) {
                float x = tileX + aroundX * (float) CLOUD_TILE;
                float z = tileZ + aroundZ * (float) CLOUD_TILE;
                // The second axis of a tile of the batch runs backwards in the world, so a tile covering the
                // blocks between z and z + CLOUD_TILE is laid at the negative of them, see the flat matrix.
                batch.draw(clouds, x, -z - CLOUD_TILE, CLOUD_TILE, CLOUD_TILE);
            }
        }
        batch.end();
    }

    /**
     * Phase the moon stands in, one phase to a day.
     * <p>
     * The picture of the phases is a sheet of {@link #MOON_PHASES} of them and the day a world is in decides
     * which one is drawn, so the moon of a world walks through a whole moon every eight days the way a moon
     * does.
     *
     * @param worldTime time of the world in ticks, see {@link DayCycle}
     * @return the phase, {@code 0} for the first of the sheet and up to {@code MOON_PHASES - 1}
     */
    public static int moonPhase(long worldTime) {
        return (int) Math.floorMod(worldTime / DayCycle.DAY_TICKS, (long) MOON_PHASES);
    }

    /**
     * How far the layer of clouds has drifted at a moment, wrapped into one tile.
     * <p>
     * The drift is a value of the day and never of the player: the field of clouds moves over the world while
     * nobody walks anywhere. It is wrapped into the size of one tile, because only the place of the fields
     * matters and not how many of them have passed: without that, a world played for hours would have drifted
     * thousands of blocks away and the arithmetic of a float would start to step in whole blocks.
     *
     * @param worldTime time of the world in ticks, see {@link DayCycle}
     * @return the drift, between {@code 0} and {@code CLOUD_TILE}
     */
    public static float cloudDrift(long worldTime) {
        float drift = worldTime * CLOUD_DRIFT;
        return drift - MathUtils.floor(drift / CLOUD_TILE) * (float) CLOUD_TILE;
    }

    /**
     * Builds the program of the sky.
     *
     * @return the program, or {@code null} when the driver refused it
     */
    private static ShaderProgram program() {
        ShaderProgram shader = new ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (!shader.isCompiled()) {
            LOGGER.error("The sky cannot be drawn, its program was refused: {}", shader.getLog());
            shader.dispose();
            return null;
        }
        return shader;
    }

    /**
     * Loads one picture of the sky.
     *
     * @param path path of the picture relative to the asset root
     * @return the picture, or {@code null} when the asset pack does not carry it
     */
    private static Texture load(String path) {
        if (!Gdx.files.internal(path).exists()) {
            LOGGER.warn("The picture {} is missing, that part of the sky is not drawn", path);
            return null;
        }
        Texture texture = new Texture(Gdx.files.internal(path));
        // Pixel art: one texel of a picture is there to be seen, so nothing is interpolated between two.
        texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        return texture;
    }

    /**
     * Wraps a picture into a region.
     *
     * @param texture picture, may be {@code null}
     * @return the region, or {@code null} when there is no picture
     */
    private static TextureRegion region(Texture texture) {
        return texture == null ? null : new TextureRegion(texture);
    }

    @Override
    public void dispose() {
        batch.dispose();
        // The program was handed to the batch, which does not own it, so it is released here.
        if (program != null) {
            program.dispose();
        }
        // The pictures of the sky are read by this renderer and are not handed out by the cache of the game,
        // so they belong to it.
        if (sunPicture != null) {
            sunPicture.dispose();
        }
        if (moonPictures != null) {
            moonPictures.dispose();
        }
        if (cloudPicture != null) {
            cloudPicture.dispose();
        }
    }

    @Override
    public String toString() {
        return "SkyRenderer(" + (CLOUD_RADIUS * 2 + 1) + " by " + (CLOUD_RADIUS * 2 + 1)
                + " tiles of clouds)";
    }
}
