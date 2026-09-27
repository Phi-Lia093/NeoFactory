package com.philia093.neofactory.render;

import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.DayCycle;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the art the sky is drawn with and the two numbers that place it.
 * <p>
 * The sky has no drawing of its own: the sun, the phases of the moon and the layer of clouds are the pictures
 * of the asset pack, and a pass that draws them has to know three things about them - that the ground of a
 * body is black, which is what adding it to the sky needs, that the ground of the layer is transparent, which
 * is what makes the gaps between the clouds, and how many blocks one pixel of it covers. All three are read
 * out of the files themselves here, so a pack that ships other art fails in this test instead of in a frame of
 * the game.
 */
class SkyRendererTest {

    /** Side of the picture of the sun, in pixels. */
    private static final int SUN_PIXELS = 32;

    /** Brightness a pixel of a body needs to count as a drawn one, out of the three channels. */
    private static final int LIT = 90;

    @Test
    void thePicturesOfTheSkyAreTheOnesTheRendererAssumes() throws IOException {
        BufferedImage sun = read(SkyRenderer.SUN_PICTURE);
        BufferedImage moon = read(SkyRenderer.MOON_PICTURES);

        assertEquals(SUN_PIXELS, sun.getWidth(), "the sun is one square of this pack");
        assertEquals(SUN_PIXELS, sun.getHeight());
        assertEquals(SkyRenderer.MOON_CELL * SkyRenderer.MOON_COLUMNS, moon.getWidth(),
                "the phases of the moon stand in rows");
        assertEquals(SkyRenderer.MOON_CELL * (SkyRenderer.MOON_PHASES / SkyRenderer.MOON_COLUMNS),
                moon.getHeight(), "and the sheet carries a row for every group of them");
    }

    @Test
    void theSunIsAGlowingBodyOnABlackGround() throws IOException {
        BufferedImage sun = read(SkyRenderer.SUN_PICTURE);
        int centre = sun.getRGB(sun.getWidth() / 2, sun.getHeight() / 2);
        int corner = sun.getRGB(0, 0);

        assertTrue(red(centre) > 200 && blue(centre) < red(centre),
                "the sun itself is a warm square and not a white one");
        assertTrue(brightness(corner) < 40, "its ground is black, which is what adding it to the sky needs");
    }

    @Test
    void everyPhaseOfTheMoonIsDrawnAndThePhasesDiffer() throws IOException {
        BufferedImage sheet = read(SkyRenderer.MOON_PICTURES);
        int[] lit = new int[SkyRenderer.MOON_PHASES];

        for (int phase = 0; phase < SkyRenderer.MOON_PHASES; phase++) {
            lit[phase] = litPixels(sheet, phase);
            assertTrue(lit[phase] > 0, "the phase " + phase + " of the moon is drawn at all");
        }
        long distinct = Arrays.stream(lit).distinct().count();
        assertTrue(distinct >= 3, "the sheet carries different moons, it showed " + distinct + " of them");
    }

    @Test
    void theMoonWalksThroughItsPhasesOneToADay() {
        assertEquals(0, SkyRenderer.moonPhase(0L));
        assertEquals(SkyRenderer.MOON_PHASES - 1, SkyRenderer.moonPhase(
                (SkyRenderer.MOON_PHASES - 1L) * DayCycle.DAY_TICKS + 100L));
        assertEquals(0, SkyRenderer.moonPhase(SkyRenderer.MOON_PHASES * (long) DayCycle.DAY_TICKS),
                "the ninth day of a world carries the first phase again");
    }

    /** Reads a picture of the asset pack. */
    private static BufferedImage read(String path) throws IOException {
        Path file = TestRegistries.ASSETS.resolve(path);
        assertTrue(Files.isRegularFile(file), "the asset pack carries " + path);
        return ImageIO.read(file.toFile());
    }

    /** Pixels of one phase of the sheet that carry a moon. */
    private static int litPixels(BufferedImage sheet, int phase) {
        int cellX = phase % SkyRenderer.MOON_COLUMNS * SkyRenderer.MOON_CELL;
        int cellY = phase / SkyRenderer.MOON_COLUMNS * SkyRenderer.MOON_CELL;
        int lit = 0;
        for (int y = 0; y < SkyRenderer.MOON_CELL; y++) {
            for (int x = 0; x < SkyRenderer.MOON_CELL; x++) {
                if (brightness(sheet.getRGB(cellX + x, cellY + y)) > LIT) {
                    lit++;
                }
            }
        }
        return lit;
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
