package com.philia093.neofactory.render;

import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.DayCycle;
import com.badlogic.gdx.graphics.Color;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paints a whole day of the sky of a world into a picture, with the very art the game draws it with.
 * <p>
 * The picture is a time lapse: one column of it is one moment of the day, its ground is the colour a frame is
 * cleared with at that moment, see {@link DayCycle#skyColor(long, com.badlogic.gdx.graphics.Color)}, and over
 * it stand the sun and the moon of {@code environment/sun.png} and {@code environment/moon_phases.png} and
 * the layer of {@code environment/clouds.png}, blended the way the pass blends them - a body by adding it to
 * the sky and a cloud through its transparency. A look at the picture therefore says whether the art is read
 * the way the pass reads it: a sun that is a warm body and no white square, a moon that stands in the sky of
 * the night, and clouds that are grey at night and white at noon.
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

    /** Rows the sun is painted with. */
    private static final int SUN_ROWS = 40;

    /** Rows the moon is painted with. */
    private static final int MOON_ROWS = 32;

    /** Rows the band of an arc of the day is painted with. */
    private static final int ARC_ROWS = 8;

    /** Row the layer of clouds is painted at. */
    private static final int CLOUD_ROW = 30;

    /** Height of the band of clouds. */
    private static final int CLOUD_BAND = 24;

    @Test
    void aWholeDayOfTheSkyIsWrittenToAPicture() throws IOException {
        BufferedImage sky = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        BufferedImage sun = read(SkyRenderer.SUN_PICTURE);
        BufferedImage phases = read(SkyRenderer.MOON_PICTURES);
        BufferedImage clouds = read(SkyRenderer.CLOUD_PICTURE);
        Color colour = new Color();

        for (int column = 0; column < WIDTH; column++) {
            long worldTime = (long) column * DayCycle.DAY_TICKS / WIDTH;
            DayCycle.skyColor(worldTime, colour);
            fillColumn(sky, column, 0, HEIGHT, toArgb(colour.r, colour.g, colour.b, 1.0f));
            paintArcs(sky, column, worldTime);
            paintClouds(sky, column, worldTime, clouds);
        }
        // The art of the pack is pasted where a body stands highest, after the arcs, so the picture shows both
        // the walk of the day and the very pictures the pass draws it with.
        pasteAdded(sky, sun, 0, 0, sun.getWidth(), columnOf(DayCycle.NOON), rowOf(1.0f), SUN_ROWS,
                new Color(DayCycle.SUN_RED, DayCycle.SUN_GREEN, DayCycle.SUN_BLUE, 1.0f), 1.0f);
        int phase = SkyRenderer.moonPhase(DayCycle.MIDNIGHT);
        pasteAdded(sky, phases, phase % SkyRenderer.MOON_COLUMNS * SkyRenderer.MOON_CELL,
                phase / SkyRenderer.MOON_COLUMNS * SkyRenderer.MOON_CELL, SkyRenderer.MOON_CELL,
                columnOf(DayCycle.MIDNIGHT), rowOf(1.0f), MOON_ROWS,
                new Color(DayCycle.MOON_RED, DayCycle.MOON_GREEN, DayCycle.MOON_BLUE, 1.0f), 1.0f);
        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(sky, "png", PREVIEW.toFile());
        System.out.println("sky preview written to " + PREVIEW.toAbsolutePath() + ": sunrise at the left, "
                + DayCycle.NOON + " ticks in the middle, midnight at " + DayCycle.MIDNIGHT + " ticks");

        assertTrue(Files.isRegularFile(PREVIEW), "the preview exists");
        assertTheDayWalksFromTheMorningIntoTheNight(sky);
        assertTheBodiesOfTheSkyAreTheArtOfThePack(sky);
        assertTheCloudsAreBrightAtNoonAndDarkAtNight(sky, clouds);
    }

    /** The ground of the picture is the colour of the hour, so the day walks from the dawn into the night. */
    private static void assertTheDayWalksFromTheMorningIntoTheNight(BufferedImage sky) {
        int noon = sky.getRGB(columnOf(DayCycle.NOON), 2);
        int morning = sky.getRGB(columnOf(DayCycle.NEW_WORLD_TIME), 2);
        int midnight = sky.getRGB(columnOf(DayCycle.MIDNIGHT), 2);

        assertTrue(blue(noon) > red(noon), "the sky of the noon is blue");
        assertTrue(brightness(noon) > brightness(morning), "the noon is brighter than the morning");
        assertTrue(brightness(morning) > brightness(midnight), "the morning is brighter than the night");
        assertTrue(brightness(midnight) < 60, "the sky of the night is almost black");
    }

    /** The sun is a warm body in the sky of the day and the moon a pale one in the sky of the night. */
    private static void assertTheBodiesOfTheSkyAreTheArtOfThePack(BufferedImage sky) {
        int sun = sky.getRGB(columnOf(DayCycle.NOON), rowOf(1.0f));
        int sunArc = sky.getRGB(columnOf(DayCycle.NEW_WORLD_TIME),
                rowOf(DayCycle.sunHeight(DayCycle.NEW_WORLD_TIME)));
        int moonArc = sky.getRGB(columnOf(DayCycle.MIDNIGHT + 2000L), rowOf(1.0f));
        int moon = brightestOf(sky, columnOf(DayCycle.MIDNIGHT), rowOf(1.0f), MOON_ROWS);

        assertTrue(red(sun) > 200 && blue(sun) < red(sun),
                "the sun of the pack is a warm body and not a white square, it was "
                        + Integer.toHexString(sun));
        assertTrue(blue(sunArc) < red(sunArc), "the arc of the day is warm as well");
        assertTrue(blue(moonArc) >= red(moonArc), "the arc of the night is the pale one of the moon");
        assertTrue(moon > 100, "the art of the moon stands in the sky of the night, its brightest texel was "
                + moon);
    }

    /** Brightest pixel of a square of the picture, which is how a body is asked whether it is drawn at all. */
    private static int brightestOf(BufferedImage sky, int column, int row, int size) {
        int brightest = 0;
        for (int y = row - size / 2; y < row + size / 2; y++) {
            for (int x = column - size / 2; x < column + size / 2; x++) {
                if (x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) {
                    continue;
                }
                brightest = Math.max(brightest, brightness(sky.getRGB(x, y)));
            }
        }
        return brightest;
    }

    /** Clouds are bright at noon and almost gone at night. */
    private static void assertTheCloudsAreBrightAtNoonAndDarkAtNight(BufferedImage sky,
            BufferedImage clouds) {
        int noon = brightestCloudOf(sky, clouds, DayCycle.NOON);
        int midnight = brightestCloudOf(sky, clouds, DayCycle.MIDNIGHT);

        assertTrue(noon > 0 && midnight > 0, "the picture carries clouds at all");
        assertTrue(noon > midnight, "the cloud of the noon is brighter than the one of the night, they were "
                + noon + " and " + midnight);
    }

    /** Paints the arc of one column: the band a body of that hour would stand in. */
    private static void paintArcs(BufferedImage sky, int column, long worldTime) {
        float height = DayCycle.sunHeight(worldTime);
        float fade = Math.min(1.0f, Math.abs(height) * 6.0f);
        if (fade <= 0.0f) {
            return;
        }
        boolean sun = height > 0.0f;
        int row = rowOf(Math.abs(height));
        fillColumn(sky, column, row - ARC_ROWS / 2, ARC_ROWS,
                mix(sky.getRGB(column, row), 0xFFFFFFFF, fade,
                        sun ? DayCycle.SUN_RED : DayCycle.MOON_RED,
                        sun ? DayCycle.SUN_GREEN : DayCycle.MOON_GREEN,
                        sun ? DayCycle.SUN_BLUE : DayCycle.MOON_BLUE));
    }

    /** Paints the clouds of one column out of the picture of the layer. */
    private static void paintClouds(BufferedImage sky, int column, long worldTime, BufferedImage clouds) {
        Color tint = DayCycle.cloudTint(worldTime, new Color());
        for (int row = 0; row < CLOUD_BAND; row++) {
            int texel = clouds.getRGB(column * clouds.getWidth() / WIDTH,
                    row * clouds.getHeight() / CLOUD_BAND);
            float alpha = (texel >>> 24) / 255.0f;
            if (alpha <= 0.0f) {
                continue;
            }
            int under = sky.getRGB(column, CLOUD_ROW + row);
            sky.setRGB(column, CLOUD_ROW + row, mix(under, texel, alpha, tint.r, tint.g, tint.b));
        }
    }

    /**
     * Draws one body of the sky over the picture, the way the pass draws it.
     * <p>
     * A texel of a body is drawn through <b>its own brightness times its alpha</b>: the ground of the art is
     * black and leaves the sky standing, and the body itself keeps its colour - a yellow sun on a blue sky is
     * a yellow sun, while adding it to the sky would turn it white, see {@code SkyRenderer}.
     */
    private static void pasteAdded(BufferedImage sky, BufferedImage sprite, int cellX, int cellY, int cell,
            int centreColumn, int centreRow, int size, Color tint, float fade) {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int texel = sprite.getRGB(cellX + x * cell / size, cellY + y * cell / size);
                int targetColumn = centreColumn + x - size / 2;
                int targetRow = centreRow + y - size / 2;
                if (targetColumn < 0 || targetColumn >= WIDTH || targetRow < 0 || targetRow >= HEIGHT) {
                    continue;
                }
                int under = sky.getRGB(targetColumn, targetRow);
                sky.setRGB(targetColumn, targetRow,
                        mix(under, texel, lit(texel) * ((texel >>> 24) / 255.0f) * fade,
                                tint.r, tint.g, tint.b));
            }
        }
    }

    /** How bright a texel of a picture of the sky is, the amount of it that is drawn. */
    private static float lit(int argb) {
        return Math.max(Math.max(red(argb), green(argb)), blue(argb)) / 255.0f;
    }

    /** Lays a texel over the colour that stands there, the way the pass blends a cloud. */
    private static int mix(int under, int texel, float alpha, float red, float green, float blue) {
        int share = Math.round(alpha * 255.0f);
        return 0xFF000000
                | (red(under) * (255 - share) + Math.round(red(texel) * red) * share) / 255 << 16
                | (green(under) * (255 - share) + Math.round(green(texel) * green) * share) / 255 << 8
                | (blue(under) * (255 - share) + Math.round(blue(texel) * blue) * share) / 255;
    }

    /** Brightest clouded pixel of the band near a moment, {@code 0} when no cloud stands there at all. */
    private static int brightestCloudOf(BufferedImage sky, BufferedImage clouds, long worldTime) {
        int centre = columnOf(worldTime);
        int brightest = 0;
        for (int column = centre - 30; column <= centre + 30; column++) {
            if (column < 0 || column >= WIDTH) {
                continue;
            }
            for (int row = 0; row < CLOUD_BAND; row++) {
                if ((clouds.getRGB(column * clouds.getWidth() / WIDTH,
                        row * clouds.getHeight() / CLOUD_BAND) >>> 24) > 128) {
                    brightest = Math.max(brightest, brightness(sky.getRGB(column, CLOUD_ROW + row)));
                }
            }
        }
        return brightest;
    }

    /** Column of the picture a moment of the day falls on. */
    private static int columnOf(long worldTime) {
        return (int) (DayCycle.ofDay(worldTime) * WIDTH / DayCycle.DAY_TICKS);
    }

    /** Row of the picture a height of the sky falls on. */
    private static int rowOf(float height) {
        return Math.round(HORIZON - height * SKY_ROWS);
    }

    /** Fills a run of rows of one column with a colour, ignoring what leaves the picture. */
    private static void fillColumn(BufferedImage picture, int column, int row, int rows, int argb) {
        for (int at = row; at < row + rows; at++) {
            if (at < 0 || at >= HEIGHT) {
                continue;
            }
            picture.setRGB(column, at, argb);
        }
    }

    /** Reads a picture of the asset pack. */
    private static BufferedImage read(String path) throws IOException {
        Path file = TestRegistries.ASSETS.resolve(path);
        assertTrue(Files.isRegularFile(file), "the asset pack carries " + path);
        return ImageIO.read(file.toFile());
    }

    /** ARGB of three shares of a colour with an alpha of its own. */
    private static int toArgb(float red, float green, float blue, float alpha) {
        return Math.round(alpha * 255.0f) << 24 | Math.round(red * 255.0f) << 16
                | Math.round(green * 255.0f) << 8 | Math.round(blue * 255.0f);
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

    /** Sum of the three channels of a pixel. */
    private static int brightness(int argb) {
        return red(argb) + green(argb) + blue(argb);
    }
}
