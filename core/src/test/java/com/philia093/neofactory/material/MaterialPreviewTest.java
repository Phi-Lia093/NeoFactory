package com.philia093.neofactory.material;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.support.TestIcons;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paints every shape of every material and writes the sheet to a file.
 * <p>
 * One grey scale shape serves every material of the game, and the colour of the material is
 * multiplied over it while it is drawn, see {@link MaterialForm}. That is cheap to write down and
 * impossible to review by reading, so this class does what the screen would do: it reads the
 * picture of a shape, multiplies the colour of a material over it, lays the overlay of the shape
 * on top and writes the whole grid to {@code core/build/reports/material-preview.png} - one row per
 * material, one column per shape.
 * <p>
 * The last column stays empty on purpose: the cell is the shape a material owns once it can be
 * molten, and the twenty shapes are laid out in full so that the sheet never moves when that
 * happens. The checks are the regression net under the picture: a shape whose file is missing, one
 * that paints nothing and two materials that end up looking the same all fail the build.
 */
class MaterialPreviewTest {

    /** Assets of the project, the tests run inside the core module. */
    private static final Path ASSETS = Path.of("..", "assets");

    /** Picture the grid is written to. */
    private static final Path PREVIEW = Path.of("build", "reports", "material-preview.png");

    /** Side length of one icon inside the grid. */
    private static final int CELL = Constants.ITEM_ICON_SIZE;

    /** Zoom the icons are drawn with, so the corners of a shape are visible. */
    private static final int ZOOM = 3;

    /** Space between two icons and around the grid. */
    private static final int GAP = 2;

    /** Colour the sheet is filled with, a dark grey that shows the frame of a light shape. */
    private static final int BACKDROP = 0xFF202020;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void everyShapeIsPaintedInTheColourOfEveryMaterial() throws IOException {
        List<Material> materials = Materials.all();
        List<MaterialForm> shapes = List.of(MaterialForm.values());

        int cell = CELL * ZOOM;
        BufferedImage sheet = new BufferedImage(GAP + shapes.size() * (cell + GAP),
                GAP + materials.size() * (cell + GAP), BufferedImage.TYPE_INT_ARGB);
        fill(sheet, BACKDROP);

        int painted = 0;
        for (int row = 0; row < materials.size(); row++) {
            Material material = materials.get(row);
            for (int column = 0; column < shapes.size(); column++) {
                MaterialForm form = shapes.get(column);
                int[] pixels = paintedIcon(material, form);
                if (pixels == null) {
                    continue;
                }
                draw(sheet, pixels, GAP + column * (cell + GAP), GAP + row * (cell + GAP), ZOOM);
                painted++;
            }
        }

        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(sheet, "PNG", PREVIEW.toFile());
        System.out.println("material preview written to " + PREVIEW.toAbsolutePath()
                + ": " + painted + " icons of " + materials.size() + " materials");

        assertEquals(expectedIcons(materials), painted, "every shape of every material is painted");
        assertTrue(painted > 150, "the game has a lot of material items by now");
        assertTrue(Files.size(PREVIEW) > 0, "the picture has to hold something");
    }

    @Test
    void aShapePaintsSomethingAndTwoMaterialsNeverLookTheSame() {
        List<Material> materials = Materials.all();
        for (MaterialForm form : MaterialKind.METAL.defaultForms()) {
            long first = 0L;
            String firstName = null;
            for (Material material : materials) {
                if (!material.has(form)) {
                    // A material that is no metal does not come in every shape of one: wood is a board and
                    // nothing else, see Materials#declare.
                    continue;
                }
                int[] pixels = paintedIcon(material, form);
                assertNotNull(pixels, material.name() + " has no " + form);
                assertTrue(opaque(pixels) > 8,
                        "the " + form + " of " + material.name() + " paints almost nothing");
                if (firstName == null) {
                    first = fingerprint(pixels);
                    firstName = material.name();
                } else {
                    assertNotEquals(first, fingerprint(pixels),
                            "the " + form + " of " + firstName + " and " + material.name()
                                    + " look the same");
                }
            }
        }
    }

    @Test
    void theCellOfTheShapesStaysEmptyUntilAMaterialCanBeMolten() {
        for (Material material : Materials.all()) {
            assertNull(material.cell(),
                    material.name() + " owns a cell although no fluid of it exists yet");
        }
    }

    /** Amount of icons the grid has to hold, one per shape a material really comes in. */
    private static int expectedIcons(List<Material> materials) {
        int total = 0;
        for (Material material : materials) {
            total += material.forms().size();
        }
        return total;
    }

    /**
     * Paints the icon of one shape of one material, the very way the screen paints it.
     *
     * @param material material that owns the shape
     * @param form shape to paint
     * @return the pixels of the icon, row by row, packed as RGBA8888, or {@code null} when the
     *         material does not come in that shape
     */
    private static int[] paintedIcon(Material material, MaterialForm form) {
        Item item = material.form(form);
        if (item == null) {
            return null;
        }
        int[] shape = TestIcons.icon(item);
        int[] painted = new int[shape.length];
        for (int i = 0; i < shape.length; i++) {
            painted[i] = tint(shape[i], material.color());
        }
        if (item.hasOverlay()) {
            int[] overlay = readIcon(item.overlayTexture());
            if (overlay != null) {
                for (int i = 0; i < painted.length; i++) {
                    painted[i] = over(painted[i], overlay[i]);
                }
            }
        }
        return painted;
    }

    /**
     * Reads one frame of a picture into pixels.
     *
     * @param texture texture name or path, without extension
     * @return the pixels, row by row, packed as RGBA8888, or {@code null} when the file is missing
     */
    private static int[] readIcon(String texture) {
        String path = BlockTextureCache.resolvePath(texture);
        if (path == null) {
            return null;
        }
        Path file = ASSETS.resolve(path);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        BufferedImage picture;
        try {
            picture = ImageIO.read(file.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + file, e);
        }
        int[] pixels = new int[CELL * CELL];
        for (int y = 0; y < CELL; y++) {
            for (int x = 0; x < CELL; x++) {
                // A picture packs as ARGB, an icon of the game as RGBA8888.
                int argb = picture.getRGB(x, y);
                pixels[y * CELL + x] = (argb << 8) | (argb >>> 24);
            }
        }
        return pixels;
    }

    /**
     * Multiplies a colour over a pixel, which is what the graphics card does while drawing.
     * <p>
     * An icon of the game packs its channels as RGBA8888 - red in the highest byte, opacity in the
     * lowest - which is the format {@code CellIconFactory} writes and every reader of an icon
     * expects, see {@code Constants#ITEM_ICON_SIZE}.
     */
    private static int tint(int argb, Color color) {
        int r = (argb >>> 24) & 0xFF;
        int g = (argb >>> 16) & 0xFF;
        int b = (argb >>> 8) & 0xFF;
        int a = argb & 0xFF;
        // The tint multiplies the colour and leaves the opacity alone, exactly like the batch of
        // the game, see GuiItemRenderer.
        return pack(Math.round(r * color.r), Math.round(g * color.g), Math.round(b * color.b), a);
    }

    /** Lays a pixel of the overlay over a pixel of the shape, keeping the shape where it is. */
    private static int over(int base, int top) {
        return (top & 0xFF) == 0 ? base : top;
    }

    /** Amount of pixels of an icon that are not fully transparent. */
    private static int opaque(int[] pixels) {
        int count = 0;
        for (int pixel : pixels) {
            if ((pixel & 0xFF) > 0) {
                count++;
            }
        }
        return count;
    }

    /** A value that tells two icons apart, one number for the pixels of a whole icon. */
    private static long fingerprint(int[] pixels) {
        long hash = 1L;
        for (int pixel : pixels) {
            hash = hash * 31L + pixel;
        }
        return hash;
    }

    /** Fills a picture with one colour. */
    private static void fill(BufferedImage picture, int argb) {
        for (int y = 0; y < picture.getHeight(); y++) {
            for (int x = 0; x < picture.getWidth(); x++) {
                picture.setRGB(x, y, argb);
            }
        }
    }

    /** Draws an icon into a picture, every one of its pixels as a square of the zoom. */
    private static void draw(BufferedImage picture, int[] pixels, int x, int y, int zoom) {
        for (int row = 0; row < CELL; row++) {
            for (int column = 0; column < CELL; column++) {
                int argb = pixels[row * CELL + column];
                if ((argb & 0xFF) == 0) {
                    // An empty pixel of the icon keeps the backdrop of the sheet.
                    continue;
                }
                int colour = ((argb & 0xFF) << 24) | (argb >>> 8);
                for (int dy = 0; dy < zoom; dy++) {
                    for (int dx = 0; dx < zoom; dx++) {
                        picture.setRGB(x + column * zoom + dx, y + row * zoom + dy, colour);
                    }
                }
            }
        }
    }

    /**
     * Packs four channels into a pixel, the format an icon of the game is stored in.
     *
     * @param r red share
     * @param g green share
     * @param b blue share
     * @param a opacity, which sits in the lowest byte, see {@link #tint(int, Color)}
     * @return the pixel, packed as RGBA8888
     */
    private static int pack(int r, int g, int b, int a) {
        return (channel(r) << 24) | (channel(g) << 16) | (channel(b) << 8) | channel(a);
    }

    /** Keeps a channel inside the range of a byte. */
    private static int channel(int value) {
        return Math.min(Math.max(value, 0), 0xFF);
    }
}
