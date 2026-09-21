package com.philia093.neofactory.fluid;

import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the grey scale sheets of the fluids against the numbers the fluids claim.
 * <p>
 * The picture of a fluid holds brightness only, the colour is applied while it is drawn, so
 * three things have to hold for every fluid of the table: the sheet carries no colour, it is
 * cut into the amount of cells the fluid says and it keeps the brightness of the art pack it
 * was made from. A sheet that lost its contrast would turn a fluid into a flat plate.
 */
class FluidTextureTest {

    /** Assets of the project, the tests run inside the core module. */
    private static final Path ASSETS = Path.of("..", "assets");

    /** Value a channel reaches when a picture is more than one flat colour. */
    private static final int BRIGHT = 0x80;

    @BeforeAll
    static void register() {
        TestRegistries.ensure();
    }

    @Test
    void everyFluidSharesTheOneSheetOfTheGame() {
        for (Fluid fluid : Fluids.all()) {
            assertEquals(Fluids.SHEET, fluid.stillTexture(),
                    fluid + " is painted from the one sheet of the game");
            assertEquals(Fluids.FRAMES, fluid.frames(),
                    "and it holds the frames that sheet holds");
        }
    }

    @Test
    void theSheetOfEveryFluidIsGreyScale() {
        for (Fluid fluid : Fluids.all()) {
            BufferedImage sheet = read(fluid);
            for (int y = 0; y < sheet.getHeight(); y++) {
                for (int x = 0; x < sheet.getWidth(); x++) {
                    int argb = sheet.getRGB(x, y);
                    int red = (argb >> 16) & 0xFF;
                    int green = (argb >> 8) & 0xFF;
                    int blue = argb & 0xFF;
                    assertEquals(red, green,
                            fluid + " carries colour at " + x + ", " + y);
                    assertEquals(green, blue,
                            fluid + " carries colour at " + x + ", " + y);
                }
            }
        }
    }

    @Test
    void theSheetOfEveryFluidHoldsTheFramesItClaims() {
        for (Fluid fluid : Fluids.all()) {
            BufferedImage sheet = read(fluid);

            assertEquals(Constants.ITEM_ICON_SIZE, sheet.getWidth(),
                    fluid + " is one cell wide");
            assertEquals(0, sheet.getHeight() % Constants.ITEM_ICON_SIZE,
                    fluid + " is cut into whole cells");
            assertEquals(fluid.frames(), sheet.getHeight() / Constants.ITEM_ICON_SIZE,
                    fluid + " holds the frames it claims");
        }
    }

    @Test
    void theSheetOfEveryFluidKeepsTheBrightnessOfTheArtPack() {
        for (Fluid fluid : Fluids.all()) {
            BufferedImage sheet = read(fluid);
            int brightest = 0;
            int darkest = 0xFF;
            for (int y = 0; y < sheet.getHeight(); y++) {
                for (int x = 0; x < sheet.getWidth(); x++) {
                    int value = (sheet.getRGB(x, y) >> 16) & 0xFF;
                    brightest = Math.max(brightest, value);
                    darkest = Math.min(darkest, value);
                }
            }

            assertTrue(brightest > BRIGHT,
                    fluid + " keeps the bright pixels of the art pack, was " + brightest);
            assertTrue(darkest < brightest,
                    fluid + " is more than one flat colour");
        }
    }

    /** Reads the sheet a fluid is painted from. */
    private static BufferedImage read(Fluid fluid) {
        Path file = ASSETS.resolve(BlockTextureCache.resolvePath(fluid.stillTexture()));
        BufferedImage sheet;
        try {
            sheet = ImageIO.read(file.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + file, e);
        }
        assertNotNull(sheet, "Missing picture of " + fluid + ": " + file);
        return sheet;
    }
}
