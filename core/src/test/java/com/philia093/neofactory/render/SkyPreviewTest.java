package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.world.DayCycle;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paints a whole day of the sky of a world into a picture, the way the game draws it.
 * <p>
 * The picture is a time lapse: one column of it is one moment of the day and the colour of that column is the
 * very colour a frame is cleared with at that moment, see {@link DayCycle#skyColor(long, Color)}. Over it lie
 * the arc of the sun and the arc of the moon - drawn where {@link DayCycle#sunHeight(long)} puts them and
 * fading out the way {@link SkyRenderer} fades them - and the pattern of the layer of clouds of
 * {@link SkyRenderer#cloudAt(int, int)}. A look at the picture therefore says what the sky of an hour looks
 * like and whether the day really walks from the morning through the noon into the night.
 * <p>
 * The result lies in {@code core/build/reports/sky-preview.png} and is meant to be looked at.
 */
class SkyPreviewTest {

    /** Picture the preview is written to. */
    private static final Path PREVIEW = Path.of("build", "reports", "sky-preview.png");

    /** Width of the picture, one column per twenty five ticks of the day. */
    private static final int WIDTH = 960;

    /** Height of the picture. */
    private static final int HEIGHT = 320;

    /** Row the horizon of the sky is drawn at. */
    private static final int HORIZON = HEIGHT * 3 / 4;

    /** Rows the arc of the sky spans, the height {@code 1} of {@link DayCycle#sunHeight(long)}. */
    private static final float SKY_ROWS = 110.0f;

    /** Rows the sun spans, so a body reads as a body and not as a line. */
    private static final int SUN_ROWS = 22;

    /** Rows the moon spans. */
    private static final int MOON_ROWS = 16;

    /** Row the layer of clouds is drawn at. */
    private static final int CLOUD_ROW = 40;

    /** Height of the band of clouds. */
    private static final int CLOUD_BAND = 18;

    /** Cells of the layer of clouds the picture shows across its width. */
    private static final int CLOUD_CELLS = 60;

    @Test
    void aWholeDayOfTheSkyIsWrittenToAPicture() throws IOException {
        BufferedImage picture = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Color sky = new Color();
        for (int column = 0; column < WIDTH; column++) {
            long worldTime = (long) column * DayCycle.DAY_TICKS / WIDTH;
            DayCycle.skyColor(worldTime, sky);
            fillColumn(picture, column, 0, HEIGHT, toArgb(sky, 1.0f));
            drawBody(picture, column, worldTime, false);
            drawBody(picture, column, worldTime, true);
            drawClouds(picture, column, worldTime);
        }
        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(picture, "png", PREVIEW.toFile());
        System.out.println("sky preview written to " + PREVIEW.toAbsolutePath() + ": sunrise at the left, "
                + DayCycle.NOON + " ticks in the middle, midnight at " + DayCycle.MIDNIGHT + " ticks");

        assertTrue(Files.isRegularFile(PREVIEW), "the preview exists");
        assertTheDayWalksFromTheMorningIntoTheNight(picture);
        assertTheSunAndTheMoonStandWhereTheClockPutsThem(picture);
        assertTheLayerOfCloudsHasGaps(picture);
    }

    /** The sky of the noon is bright and blue, and the sky of the night is almost black. */
    private static void assertTheDayWalksFromTheMorningIntoTheNight(BufferedImage picture) {
        int noon = picture.getRGB(columnOf(DayCycle.NOON), 2);
        int morning = picture.getRGB(columnOf(DayCycle.NEW_WORLD_TIME), 2);
        int midnight = picture.getRGB(columnOf(DayCycle.MIDNIGHT), 2);

        assertTrue(blue(noon) > red(noon), "the sky of the noon is blue");
        assertTrue(brightness(noon) > brightness(morning), "the noon is brighter than the morning");
        assertTrue(brightness(morning) > brightness(midnight), "the morning is brighter than the night");
        assertTrue(brightness(midnight) < 60, "the sky of the night is almost black");
    }

    /** The sun stands overhead at noon and the moon overhead at midnight. */
    private static void assertTheSunAndTheMoonStandWhereTheClockPutsThem(BufferedImage picture) {
        int sun = picture.getRGB(columnOf(DayCycle.NOON), rowOf(1.0f));
        int moon = picture.getRGB(columnOf(DayCycle.MIDNIGHT), rowOf(1.0f));

        assertTrue(red(sun) > 200 && blue(sun) < 220, "the sun of the noon is a warm square overhead");
        assertTrue(red(moon) > 150 && blue(moon) > red(moon), "the moon of the midnight is a pale one");
    }

    /** The layer of clouds covers part of the sky and leaves the gaps the game draws it with. */
    private static void assertTheLayerOfCloudsHasGaps(BufferedImage picture) {
        int clouded = 0;
        int clear = 0;
        for (int column = 0; column < WIDTH; column++) {
            if (picture.getRGB(column, CLOUD_ROW) != picture.getRGB(column, 2)) {
                clouded++;
            } else {
                clear++;
            }
        }
        assertTrue(clouded > 0, "the sky carries clouds");
        assertTrue(clear > 0, "and it has gaps between them");
    }

    /**
     * Draws the sun of one column, or the moon when the sun is down.
     * <p>
     * A body below the horizon is left out, which is the fade the renderer draws a body with as it reaches
     * the horizon.
     */
    private static void drawBody(BufferedImage picture, int column, long worldTime, boolean moon) {
        float height = DayCycle.sunHeight(worldTime);
        if (moon) {
            // The moon is the body the sun is not, so it stands high while the sun is down.
            height = -height;
        }
        if (height <= 0.0f) {
            return;
        }
        Color colour = new Color(moon ? DayCycle.MOON_RED : DayCycle.SUN_RED,
                moon ? DayCycle.MOON_GREEN : DayCycle.SUN_GREEN,
                moon ? DayCycle.MOON_BLUE : DayCycle.SUN_BLUE, 1.0f);
        float fade = Math.min(1.0f, height * 6.0f);
        int rows = moon ? MOON_ROWS : SUN_ROWS;
        int row = rowOf(height);
        fillColumn(picture, column, row - rows / 2, rows, toArgb(colour, fade));
    }

    /** Draws the cloud of one column of the layer over the camera. */
    private static void drawClouds(BufferedImage picture, int column, long worldTime) {
        if (!SkyRenderer.cloudAt(column * CLOUD_CELLS / WIDTH, 0)) {
            return;
        }
        float brightness = DayCycle.cloudBrightness(worldTime);
        Color colour = new Color(brightness, brightness, brightness, 1.0f);
        fillColumn(picture, column, CLOUD_ROW, CLOUD_BAND, toArgb(colour, SkyRenderer.CLOUD_ALPHA));
    }

    /** Column of the picture a moment of the day falls on. */
    private static int columnOf(long worldTime) {
        return (int) (DayCycle.ofDay(worldTime) * WIDTH / DayCycle.DAY_TICKS);
    }

    /** Row of the picture a height of the sky falls on. */
    private static int rowOf(float height) {
        return Math.round(HORIZON - height * SKY_ROWS);
    }

    /** Lays a run of rows of one column over what stands there, ignoring what leaves the picture. */
    private static void fillColumn(BufferedImage picture, int column, int row, int rows, int argb) {
        for (int at = row; at < row + rows; at++) {
            if (at < 0 || at >= HEIGHT) {
                continue;
            }
            picture.setRGB(column, at, blend(picture.getRGB(column, at), argb));
        }
    }

    /** Lays a colour with an alpha over the colour that already stands there. */
    private static int blend(int under, int over) {
        int alpha = over >>> 24;
        if (alpha == 0) {
            return under;
        }
        int red = (red(under) * (255 - alpha) + red(over) * alpha) / 255;
        int green = (green(under) * (255 - alpha) + green(over) * alpha) / 255;
        int blue = (blue(under) * (255 - alpha) + blue(over) * alpha) / 255;
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    /** ARGB of a colour of the game with an alpha of its own. */
    private static int toArgb(Color colour, float alpha) {
        int a = Math.round(Math.min(1.0f, Math.max(0.0f, alpha)) * 255.0f);
        return a << 24 | Math.round(colour.r * 255.0f) << 16
                | Math.round(colour.g * 255.0f) << 8 | Math.round(colour.b * 255.0f);
    }

    /** Red channel of a pixel. */
    private static int red(int argb) {
        return argb >> 16 & 0xFF;
    }

    /** Green channel of a pixel. */
    private static int green(int argb) {
        return argb >> 8 & 0xFF;
    }

    /** Blue channel of a pixel. */
    private static int blue(int argb) {
        return argb & 0xFF;
    }

    /** Sum of the channels of a pixel, what "brighter" means here. */
    private static int brightness(int argb) {
        return red(argb) + green(argb) + blue(argb);
    }
}
