package com.philia093.neofactory.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.philia093.neofactory.util.Constants;

import java.util.Locale;

/**
 * The clock of a world: what hour it is, how bright the sky is and where the sun stands.
 * <p>
 * A day lasts {@link #DAY_TICKS} ticks, which is twenty minutes of play at the rate of
 * {@link TickClock#TICKS_PER_SECOND}. The day starts with the sunrise: tick {@code 0} is the moment the sun
 * touches the horizon in the east, {@link #NOON} is the brightest hour, {@link #SUNSET} is the moment it
 * touches the horizon in the west and {@link #MIDNIGHT} is the middle of the night. A world counts its time
 * in ticks and never wraps it, so the hour of a save game is one number and the day of a world is
 * {@code worldTime / DAY_TICKS}: a world that was left in the evening is opened in the evening.
 * <p>
 * Everything here is a pure function of that number, which is what keeps the work of a frame small - the
 * brightness the shader scales the light of the sky with, the colour a frame is cleared with, the
 * directions the sun and the moon are drawn in and the clock the game reports are all read from the very
 * same tick - and what lets a test drive them without a window, see {@code DayCycleTest} and
 * {@code SkyPreviewTest}.
 * <p>
 * A day runs like this:
 * <pre>
 *     0  dawn       the sun touches the horizon in the east, the sky turns warm
 *  1000  morning    where a new world starts
 *  6000  noon       the sun overhead, the brightest hour
 * 12000  dusk       the sun touches the horizon in the west, the sky turns warm again
 * 13000  night      the sun is gone, the moon is left
 * 18000  midnight   the middle of the night
 * 23000  dawn       the sky starts to warm up again
 * </pre>
 */
public final class DayCycle {

    /** Ticks one whole day of a world lasts, twenty minutes of play. */
    public static final int DAY_TICKS = 24000;

    /** Tick the sun comes up at, the start of the day. */
    public static final int SUNRISE = 0;

    /** Tick the sun stands highest, the brightest hour of the day. */
    public static final int NOON = DAY_TICKS / 4;

    /** Tick the sun goes down at, the way into the night. */
    public static final int SUNSET = DAY_TICKS / 2;

    /** Tick the sun stands lowest, the middle of the night. */
    public static final int MIDNIGHT = DAY_TICKS * 3 / 4;

    /** Tick a new world starts at: the morning of a fresh day. */
    public static final int NEW_WORLD_TIME = 1000;

    /** Brightness of the light of the sky in the darkest night. */
    public static final float NIGHT_BRIGHTNESS = 0.08f;

    /** Brightness of the light of the sky at noon. */
    public static final float NOON_BRIGHTNESS = 1.0f;

    /** Red share of the colour of the night sky, a deep blue that is almost black. */
    public static final float NIGHT_RED = 0.02f;

    /** Green share of the colour of the night sky. */
    public static final float NIGHT_GREEN = 0.03f;

    /** Blue share of the colour of the night sky. */
    public static final float NIGHT_BLUE = 0.09f;

    /** Red share of the colour the sky takes while the sun stands at the horizon. */
    public static final float TWILIGHT_RED = 0.94f;

    /** Green share of the colour of the twilight. */
    public static final float TWILIGHT_GREEN = 0.52f;

    /** Blue share of the colour of the twilight. */
    public static final float TWILIGHT_BLUE = 0.26f;

    /** Ticks the twilight lasts on either side of a sunrise and of a sunset. */
    public static final int TWILIGHT_TICKS = 1000;

    /** Share of the twilight colour that reaches the sky while the sun stands at the horizon. */
    public static final float TWILIGHT_SHARE = 0.7f;

    /** Side of the square the sun is drawn as. */
    public static final float SUN_SIZE = 52.0f;

    /** Side of the square the moon is drawn as, a little smaller than the sun. */
    public static final float MOON_SIZE = 40.0f;

    /** Red share of the colour of the sun, warm and low. */
    public static final float SUN_RED = 1.0f;

    /** Green share of the colour of the sun. */
    public static final float SUN_GREEN = 0.94f;

    /** Blue share of the colour of the sun. */
    public static final float SUN_BLUE = 0.72f;

    /** Red share of the colour of the moon, a pale grey. */
    public static final float MOON_RED = 0.92f;

    /** Green share of the colour of the moon. */
    public static final float MOON_GREEN = 0.94f;

    /** Blue share of the colour of the moon. */
    public static final float MOON_BLUE = 1.0f;

    private DayCycle() {
        // Utility class: never instantiated.
    }

    /**
     * The tick of the day a moment falls on.
     *
     * @param worldTime time of the world in ticks, may lie in the future
     * @return the tick of the day, between {@code 0} and {@code DAY_TICKS - 1}
     */
    public static long ofDay(long worldTime) {
        return Math.floorMod(worldTime, (long) DAY_TICKS);
    }

    /**
     * How high the sun stands, {@code 1} overhead and {@code -1} at its lowest.
     * <p>
     * The sun walks the whole circle of the sky in one day: it stands at the horizon at
     * {@link #SUNRISE}, overhead at {@link #NOON}, at the horizon again at {@link #SUNSET} and at its
     * lowest point at {@link #MIDNIGHT}. The value is the sine of that walk, so it is negative whenever the
     * sun is below the horizon.
     *
     * @param worldTime time of the world in ticks
     * @return height of the sun, between {@code -1} and {@code 1}
     */
    public static float sunHeight(long worldTime) {
        double phase = (double) ofDay(worldTime) / DAY_TICKS;
        return (float) Math.sin(2.0 * Math.PI * phase);
    }

    /** {@code true} while the sun stands above the horizon. */
    public static boolean sunIsUp(long worldTime) {
        return sunHeight(worldTime) > 0.0f;
    }

    /**
     * How bright the light of the sky is drawn.
     * <p>
     * Noon is {@link #NOON_BRIGHTNESS} and the night is {@link #NIGHT_BRIGHTNESS}, which is what makes a
     * cave dark at noon and a torch worth carrying. The height of the sun is taken to its square root on
     * the way, because the eye reads a sky whose sun stands low as far brighter than the height alone
     * says: a morning is dim, it is not almost night. The value scales the light of the sky in the shader,
     * so it is read once per frame and never written into a mesh, see
     * {@code BlockShader#skyBrightness(float)}.
     *
     * @param worldTime time of the world in ticks
     * @return brightness of the light of the sky, between {@link #NIGHT_BRIGHTNESS} and
     *         {@link #NOON_BRIGHTNESS}
     */
    public static float brightness(long worldTime) {
        float height = Math.max(0.0f, sunHeight(worldTime));
        return NIGHT_BRIGHTNESS + (NOON_BRIGHTNESS - NIGHT_BRIGHTNESS) * (float) Math.sqrt(height);
    }

    /**
     * Where the sun stands in the sky, as a turn in degrees.
     * <p>
     * It is the angle a painter would draw the sun at: {@code 0} at the sunrise in the east, {@code 90}
     * overhead, {@code 180} at the sunset in the west, and the rest of the circle is the way the sun takes
     * below the horizon during the night.
     *
     * @param worldTime time of the world in ticks
     * @return the turn of the sun, between {@code 0} and {@code 360}
     */
    public static float sunAngle(long worldTime) {
        return 360.0f * ofDay(worldTime) / DAY_TICKS;
    }

    /**
     * The direction the sun is seen in.
     *
     * @param worldTime time of the world in ticks
     * @param out vector to write into, its length becomes {@code 1}
     */
    public static void sunDirection(long worldTime, Vector3 out) {
        double angle = Math.toRadians(sunAngle(worldTime));
        // The sun rises in the east and sets in the west, so the first horizontal axis carries the walk
        // and the second one is the height. Nothing of it leans into the third axis: a world of this game
        // has one day and no seasons.
        out.set((float) Math.cos(angle), (float) Math.sin(angle), 0.0f);
        out.nor();
    }

    /**
     * The direction the moon is seen in, the one the sun is not.
     *
     * @param worldTime time of the world in ticks
     * @param out vector to write into, its length becomes {@code 1}
     */
    public static void moonDirection(long worldTime, Vector3 out) {
        sunDirection(worldTime, out);
        out.scl(-1.0f);
    }

    /**
     * How much of the twilight colour the sky carries.
     * <p>
     * The sky is warm while the sun stands near the horizon and fades into the plain colour of the day
     * within {@link #TWILIGHT_TICKS}, which is what keeps a morning and an evening recognisable from a
     * single frame. Below the horizon there is no twilight at all until the sun comes close to it again.
     *
     * @param worldTime time of the world in ticks
     * @return share of the twilight, {@code 0} for none and {@code 1} while the sun stands at the horizon
     */
    public static float twilight(long worldTime) {
        long day = ofDay(worldTime);
        long distance = Math.min(day - SUNRISE, SUNSET - day);
        if (distance < 0) {
            // The sun is below the horizon: the warmth of the sunset is over and the one of the next
            // sunrise has not begun.
            return 0.0f;
        }
        return Math.max(0.0f, 1.0f - (float) distance / TWILIGHT_TICKS);
    }

    /**
     * The colour of the sky of a moment, which is also the colour the distance fades into.
     * <p>
     * The sky walks from the deep blue of the night to the colour of the day and takes the warm orange of
     * a sunrise and of a sunset on the way, see {@link #twilight(long)}.
     *
     * @param worldTime time of the world in ticks
     * @param out colour to write into
     * @return the colour, for chaining
     */
    public static Color skyColor(long worldTime, Color out) {
        float dayShare = (brightness(worldTime) - NIGHT_BRIGHTNESS)
                / (NOON_BRIGHTNESS - NIGHT_BRIGHTNESS);
        float red = MathUtils.lerp(NIGHT_RED, Constants.SKY_RED, dayShare);
        float green = MathUtils.lerp(NIGHT_GREEN, Constants.SKY_GREEN, dayShare);
        float blue = MathUtils.lerp(NIGHT_BLUE, Constants.SKY_BLUE, dayShare);
        float twilight = twilight(worldTime) * TWILIGHT_SHARE;
        out.set(MathUtils.lerp(red, TWILIGHT_RED, twilight),
                MathUtils.lerp(green, TWILIGHT_GREEN, twilight),
                MathUtils.lerp(blue, TWILIGHT_BLUE, twilight), 1.0f);
        return out;
    }

    /**
     * How bright a cloud is drawn, which is the day with a floor.
     * <p>
     * A cloud is lit by the sun and by nothing else, so it fades with it - but never below the pale grey
     * a moonlit cloud keeps, or the sky of a night would be empty.
     *
     * @param worldTime time of the world in ticks
     * @return brightness of a cloud, between {@code 0.35} and {@code 1}
     */
    public static float cloudBrightness(long worldTime) {
        return Math.max(0.35f, brightness(worldTime));
    }

    /**
     * The name of the hour of a moment, as the game reports it.
     *
     * @param worldTime time of the world in ticks
     * @return {@code "dawn"}, {@code "morning"}, {@code "noon"}, {@code "afternoon"}, {@code "dusk"} or
     *         {@code "night"}
     */
    public static String timeName(long worldTime) {
        long day = ofDay(worldTime);
        if (day < TWILIGHT_TICKS || day >= DAY_TICKS - TWILIGHT_TICKS) {
            return "dawn";
        }
        if (day < NOON - TWILIGHT_TICKS) {
            return "morning";
        }
        if (day <= NOON + TWILIGHT_TICKS) {
            return "noon";
        }
        if (day < SUNSET - TWILIGHT_TICKS) {
            return "afternoon";
        }
        if (day < SUNSET + TWILIGHT_TICKS) {
            return "dusk";
        }
        return "night";
    }

    /**
     * The clock of a moment, read the way a player reads a clock.
     * <p>
     * Tick zero is six in the morning, so the clock of a world runs from {@code 06:00} through
     * {@code 12:00} at noon to {@code 18:00} at the sunset and {@code 00:00} at midnight, after which the
     * next day brings {@code 06:00} again.
     *
     * @param worldTime time of the world in ticks
     * @return the clock as {@code HH:MM}
     */
    public static String format(long worldTime) {
        long day = ofDay(worldTime);
        long hour = (day / 1000L + 6L) % 24L;
        long minute = day % 1000L * 60L / 1000L;
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    /**
     * The hour of a moment as one phrase, the way the chat reports it.
     *
     * @param worldTime time of the world in ticks
     * @return something like {@code "12:00 (6000 ticks, noon)"}
     */
    public static String describe(long worldTime) {
        return format(worldTime) + " (" + ofDay(worldTime) + " ticks, " + timeName(worldTime) + ")";
    }

    @Override
    public String toString() {
        return "DayCycle(" + DAY_TICKS + " ticks to the day)";
    }
}
