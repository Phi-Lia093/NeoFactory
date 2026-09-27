package com.philia093.neofactory.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.philia093.neofactory.world.DayCycle;

/**
 * Draws the sky of a world: the sun, the moon and the layer of clouds over the terrain.
 * <p>
 * Nothing here needs a picture of its own. A body of the sky is a square of colour that stands where the
 * clock of the world says it stands - the sun in the east in the morning and in the west in the evening,
 * the moon always opposite it - and the layer of clouds is a field of flat squares at one height that
 * drifts with the day, so a player watches the hours pass without a single mesh of the world being built
 * again, see {@link DayCycle}.
 * <p>
 * The sky is drawn <b>before</b> the world and writes no depth of its own: a block drawn afterwards covers
 * whatever stood behind it, so the sun goes down behind a mountain without anything here knowing about the
 * terrain. A body fades out as it reaches the horizon, which is what keeps the sun of the night - the one
 * below the ground - out of the picture, and what is left of the sky at night is the moon and the pale grey
 * of a cloud.
 * <p>
 * The state the pass draws with is set up here and not inherited - blending on, no depth test and no culling,
 * the depth test handed back when it is done - so it runs before the passes of the world set up their own
 * state and never between them, see {@code GameScreen#renderCubes}.
 */
public final class SkyRenderer implements Disposable {

    /** Share of the view distance a body of the sky is drawn at, far enough to stand behind anything. */
    private static final float BODY_DISTANCE_SHARE = 0.9f;

    /** How quickly a body fades away as it reaches the horizon. */
    private static final float BODY_FADE = 6.0f;

    /** Height the layer of clouds is drawn at, above the terrain a player walks on. */
    public static final int CLOUD_Y = 180;

    /** Side of one cell of the layer of clouds, in blocks. */
    public static final float CLOUD_CELL = 24.0f;

    /** Cells of the layer of clouds drawn around the camera, on either side of it. */
    public static final int CLOUD_RADIUS = 6;

    /** How far the layer of clouds drifts per tick, in blocks. */
    public static final float CLOUD_DRIFT = 0.012f;

    /** How solid a cloud is drawn. */
    public static final float CLOUD_ALPHA = 0.55f;

    /**
     * The triangles of the sky.
     * <p>
     * A sky is written as it is drawn and kept nowhere: a body of it is one square of two triangles and the
     * layer of clouds is a few hundred of them, so the frames of this pass cost what they draw and no buffer
     * of the game is touched. Every vertex carries its own colour, which is how the sun fades away as it
     * reaches the horizon, see {@link #drawBody}.
     */
    private final ImmediateModeRenderer20 triangles = new ImmediateModeRenderer20(3, true, false, 0);

    /** Colour of the pass being written, reused every frame. */
    private final Color colour = new Color();

    /** Where the sun stands right now, a point of the sky no player can reach. */
    private final Vector3 sunPosition = new Vector3();

    /** Where the moon stands right now. */
    private final Vector3 moonPosition = new Vector3();

    /** Direction of the body being written, the way from the eye to it. */
    private final Vector3 direction = new Vector3();

    /** First axis of the square a body is written as, reused every frame. */
    private final Vector3 axisRight = new Vector3();

    /** Second axis of the square a body is written as, reused every frame. */
    private final Vector3 axisUp = new Vector3();

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
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);

        float distance = camera.far * BODY_DISTANCE_SHARE;
        DayCycle.sunDirection(worldTime, direction);
        sunPosition.set(camera.position).mulAdd(direction, distance);
        DayCycle.moonDirection(worldTime, direction);
        moonPosition.set(camera.position).mulAdd(direction, distance);
        // The sun fades away as it reaches the horizon and the moon fades in on the other side, so a body
        // that stands below the ground is never drawn over the terrain.
        float height = DayCycle.sunHeight(worldTime);
        float sunFade = MathUtils.clamp(height * BODY_FADE, 0.0f, 1.0f);
        float moonFade = MathUtils.clamp(-height * BODY_FADE, 0.0f, 1.0f);

        triangles.begin(camera.combined, GL20.GL_TRIANGLES);
        drawBody(camera, sunPosition, DayCycle.SUN_SIZE, sunFade,
                DayCycle.SUN_RED, DayCycle.SUN_GREEN, DayCycle.SUN_BLUE);
        drawBody(camera, moonPosition, DayCycle.MOON_SIZE, moonFade,
                DayCycle.MOON_RED, DayCycle.MOON_GREEN, DayCycle.MOON_BLUE);
        drawClouds(camera, worldTime);
        triangles.end();

        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    }

    /**
     * Draws one body of the sky as a square that always faces the eye.
     * <p>
     * The two axes of the square are taken from the view itself - across the line of sight and up it - so
     * the body keeps its shape wherever a player looks.
     *
     * @param camera camera the sky is seen through
     * @param position where the body stands, a point of the sphere of the sky
     * @param size side of the square
     * @param fade how solid the body is drawn, {@code 0} draws nothing
     * @param red red share of its colour
     * @param green green share of its colour
     * @param blue blue share of its colour
     */
    private void drawBody(Camera camera, Vector3 position, float size, float fade,
            float red, float green, float blue) {
        if (fade <= 0.0f) {
            return;
        }
        colour.set(red, green, blue, fade);
        triangles.color(colour);
        direction.set(position).sub(camera.position).nor();
        axisRight.set(direction).crs(camera.up).nor().scl(size * 0.5f);
        axisUp.set(axisRight).crs(direction).nor().scl(size * 0.5f);
        float x = position.x;
        float y = position.y;
        float z = position.z;
        float x1 = x - axisRight.x - axisUp.x;
        float y1 = y - axisRight.y - axisUp.y;
        float z1 = z - axisRight.z - axisUp.z;
        float x2 = x + axisRight.x - axisUp.x;
        float y2 = y + axisRight.y - axisUp.y;
        float z2 = z + axisRight.z - axisUp.z;
        float x3 = x + axisRight.x + axisUp.x;
        float y3 = y + axisRight.y + axisUp.y;
        float z3 = z + axisRight.z + axisUp.z;
        float x4 = x - axisRight.x + axisUp.x;
        float y4 = y - axisRight.y + axisUp.y;
        float z4 = z - axisRight.z + axisUp.z;
        triangle(x1, y1, z1, x2, y2, z2, x3, y3, z3);
        triangle(x1, y1, z1, x3, y3, z3, x4, y4, z4);
    }

    /**
     * Writes one triangle of the sky.
     *
     * @param x1 X coordinate of the first corner
     * @param y1 Y coordinate of the first corner
     * @param z1 Z coordinate of the first corner
     * @param x2 X coordinate of the second corner
     * @param y2 Y coordinate of the second corner
     * @param z2 Z coordinate of the second corner
     * @param x3 X coordinate of the third corner
     * @param y3 Y coordinate of the third corner
     * @param z3 Z coordinate of the third corner
     */
    private void triangle(float x1, float y1, float z1, float x2, float y2, float z2,
            float x3, float y3, float z3) {
        triangles.vertex(x1, y1, z1);
        triangles.vertex(x2, y2, z2);
        triangles.vertex(x3, y3, z3);
    }

    /**
     * Draws the layer of clouds over the camera.
     * <p>
     * The field of clouds is anchored to the world and not to the player: the drift of the layer is a value
     * of the day, so a player who walks on sees the same cloud from the other side and a player who stands
     * still watches it travel. Only the cells around the camera are drawn, which is what keeps a frame at
     * the clouds it can see instead of a layer of a whole world.
     *
     * @param camera camera the sky is seen through
     * @param worldTime time of the day in ticks, see {@link DayCycle}
     */
    private void drawClouds(Camera camera, long worldTime) {
        float drift = worldTime * CLOUD_DRIFT;
        float brightness = DayCycle.cloudBrightness(worldTime);
        colour.set(brightness, brightness, brightness, CLOUD_ALPHA * brightness);
        triangles.color(colour);
        int cellX = MathUtils.floor((camera.position.x - drift) / CLOUD_CELL);
        int cellZ = MathUtils.floor(camera.position.z / CLOUD_CELL);
        for (int aroundX = -CLOUD_RADIUS; aroundX <= CLOUD_RADIUS; aroundX++) {
            for (int aroundZ = -CLOUD_RADIUS; aroundZ <= CLOUD_RADIUS; aroundZ++) {
                if (!cloudAt(cellX + aroundX, cellZ + aroundZ)) {
                    continue;
                }
                float x = (cellX + aroundX) * CLOUD_CELL + drift;
                float z = (cellZ + aroundZ) * CLOUD_CELL;
                float x2 = x + CLOUD_CELL;
                float z2 = z + CLOUD_CELL;
                triangle(x, CLOUD_Y, z, x, CLOUD_Y, z2, x2, CLOUD_Y, z2);
                triangle(x, CLOUD_Y, z, x2, CLOUD_Y, z2, x2, CLOUD_Y, z);
            }
        }
    }

    /**
     * Whether the layer of clouds carries a cloud in one cell of it.
     * <p>
     * The pattern is a hash of the cell of the layer and not a random number, so the same cells carry clouds
     * in every frame and in every session: the gaps of the layer are the shape of the clouds, and a player
     * who walks back finds them where they were.
     *
     * @param cellX cell of the layer along the first horizontal axis
     * @param cellZ cell of the layer along the second horizontal axis
     * @return {@code true} when that cell has a cloud
     */
    public static boolean cloudAt(int cellX, int cellZ) {
        int hash = cellX * 374761393 + cellZ * 668265263;
        hash = (hash ^ (hash >>> 13)) * 1274126177;
        return ((hash ^ (hash >>> 16)) & 3) != 0;
    }

    @Override
    public void dispose() {
        triangles.dispose();
    }

    @Override
    public String toString() {
        return "SkyRenderer(" + (CLOUD_RADIUS * 2 + 1) + " by " + (CLOUD_RADIUS * 2 + 1) + " cloud cells)";
    }
}
