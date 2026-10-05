package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.chemistry.SkeletalLayout;
import com.philia093.neofactory.chemistry.SkeletalStructure;
import com.philia093.neofactory.support.GuiPreview;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Draws a shelf of molecules as skeletal formulas and writes the result to a picture.
 * <p>
 * The picture lies in {@code core/build/reports/skeletal-preview.png} and is meant to be looked at: a ring,
 * a fused pair and a chain stand next to each other, each one laid out by {@link SkeletalLayout} and painted
 * by {@link SkeletalRenderer} with the very code the game would paint it with, the names of the heteroatoms
 * written in the bitmap of the game. It is how a skeletal drawing is reviewed without starting the game, and
 * the check under it only insists that something was painted at all - the arithmetic of the drawing belongs
 * to {@code SkeletalLayoutTest} and {@code SkeletalRendererTest} and not to the eye.
 */
class SkeletalPreviewTest {

    /** Picture the preview is written to. */
    private static final Path PREVIEW = Path.of("build", "reports", "skeletal-preview.png");

    /** Side of one tile of the picture, the square a drawing is scaled into. */
    private static final int TILE = 72;

    /** Space between two tiles. */
    private static final int GAP = 6;

    /** Amount of tiles a row of the picture holds. */
    private static final int COLUMNS = 4;

    /** Height of the line the name of a molecule is written on, under its tile. */
    private static final int CAPTION = 10;

    /** Colour the bonds are painted in. */
    private static final Color INK = new Color(0f, 0f, 0f, 1f);

    /** A molecule of the shelf: the name it is written under and the string it is read from. */
    private static final String[][] MOLECULES = {
        {"benzene", "c1ccccc1"},
        {"toluene", "Cc1ccccc1"},
        {"pyridine", "n1ccccc1"},
        {"naphthalene", "c1ccc2ccccc2c1"},
        {"cyclohexane", "C1CCCCC1"},
        {"phenol", "Oc1ccccc1"},
        {"ethanol", "CCO"},
        {"acetic acid", "CC(=O)O"},
        {"ethyl acetate", "CC(=O)OCC"},
        {"glycine", "NCC(=O)O"},
        {"ethene", "C=C"},
        {"ethyne", "C#C"}
    };

    @Test
    void theMoleculesAreDrawnAsSkeletalFormulas() throws IOException {
        BufferedImage font = GuiPreview.read(GuiPreview.FONT);
        int rows = (MOLECULES.length + COLUMNS - 1) / COLUMNS;
        int width = GuiPreview.MARGIN * 2 + COLUMNS * TILE + (COLUMNS - 1) * GAP;
        int height = GuiPreview.MARGIN * 2 + rows * (TILE + CAPTION) + (rows - 1) * GAP;
        BufferedImage picture = GuiPreview.newPicture(width, height);

        int drawn = 0;
        for (int index = 0; index < MOLECULES.length; index++) {
            int column = index % COLUMNS;
            int row = index / COLUMNS;
            int tileX = GuiPreview.MARGIN + column * (TILE + GAP);
            int tileY = GuiPreview.MARGIN + row * (TILE + CAPTION + GAP);
            drawn += drawMolecule(picture, font, MOLECULES[index][0], MOLECULES[index][1], tileX, tileY);
            GuiPreview.drawText(picture, font, MOLECULES[index][0], tileX, tileY + TILE);
        }

        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(picture, "png", PREVIEW.toFile());
        assertTrue(Files.isRegularFile(PREVIEW), "the preview was not written");
        assertTrue(drawn > 0, "nothing was painted at all");
    }

    /** Paints one molecule and its name into a tile, and answers with how much of it was painted. */
    private static int drawMolecule(BufferedImage picture, BufferedImage font, String name, String smiles,
            int x, int y) {
        SkeletalStructure structure = SkeletalLayout.layout(smiles);
        int[] pixels = SkeletalRenderer.draw(structure, TILE, INK, INK);
        int painted = 0;
        for (int row = 0; row < TILE; row++) {
            for (int column = 0; column < TILE; column++) {
                int rgba = pixels[row * TILE + column];
                if ((rgba & 0xFF) == 0) {
                    continue;
                }
                picture.setRGB(x + column, y + row, toArgb(rgba));
                painted++;
            }
        }
        for (SkeletalRenderer.Label label : SkeletalRenderer.labels(structure, TILE)) {
            int textX = x + label.centerX() - GuiPreview.textWidth(font, label.text()) / 2;
            int textY = y + label.centerY() - 4;
            GuiPreview.drawText(picture, font, label.text(), textX, textY);
            painted++;
        }
        return painted;
    }

    /** Turns a pixel packed the way a {@code Pixmap} holds it into a pixel a picture holds. */
    private static int toArgb(int rgba) {
        int alpha = rgba & 0xFF;
        int red = (rgba >>> 24) & 0xFF;
        int green = (rgba >>> 16) & 0xFF;
        int blue = (rgba >>> 8) & 0xFF;
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
