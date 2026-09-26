package com.philia093.neofactory.pipe;

import com.badlogic.gdx.graphics.Color;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.model.BlockModel;
import com.philia093.neofactory.block.model.ModelBox;
import com.philia093.neofactory.block.model.ModelRegistry;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Draws the pipes of the game into pictures, so their shape can be looked at without starting the game.
 * <p>
 * The number of a pipe state is the mask of the sides it is joined on, so the shape of a line of pipes is
 * data and nothing else, see {@link Pipes}: this test walks that data and paints it. Two sheets are written
 * under {@code core/build/reports}:
 * <p>
 * {@code pipe-preview.png} holds one {@link PipeSize size} per row and one way of being joined per column -
 * alone, a straight run, a corner, a cross and every side at once - so the thickness of a tube and the
 * bundle of the quadruple and the nonuple can be judged by looking at the file.
 * <p>
 * {@code pipe-junction.png} holds two pipes that meet at a wall, one pair per column: two of the same size,
 * which shows a seam a player must not see at all, and pairs of different sizes, which shows the plate of
 * the wider tube where the two meet. A pipe carries no collar any more, see
 * {@code tools/gen_pipe_models.ps1}: what a joined side shows is the end face of its own arm and nothing
 * else.
 */
class PipePreviewTest {

    /** Picture the preview is written to. */
    private static final Path PREVIEW = Path.of("build", "reports", "pipe-preview.png");

    /** Picture of the junctions, written next to {@link #PREVIEW}. */
    private static final Path JUNCTIONS = Path.of("build", "reports", "pipe-junction.png");

    /** Side of one drawing in pixels. */
    private static final int CELL = 96;

    /** Backdrop of the picture, a dark grey that lets the metal stand out. */
    private static final java.awt.Color BACKDROP = new java.awt.Color(0x20, 0x20, 0x20);

    /** How far the two horizontal axes spread on screen, as a share of one drawing. */
    private static final float SPREAD = 0.40f;

    /** How much of a step along both horizontal axes becomes height, as a share of one drawing. */
    private static final float RISE = 0.22f;

    /** How much one unit of height becomes, as a share of one drawing. */
    private static final float HEIGHT = 0.42f;

    /** The ways a pipe is joined that the sheet shows, one column each. */
    private static final List<Integer> MASKS = List.of(0, Pipes.STRAIGHT_MASK,
            Pipes.mask(BlockFace.NORTH, BlockFace.EAST),
            Pipes.ALL_MASK ^ Pipes.bit(BlockFace.BOTTOM), Pipes.ALL_MASK);

    /**
     * The pairs of the junction sheet, one column each.
     * <p>
     * A pair is the pipe of the west cell and the pipe of the east cell, both joined along the axis of the
     * two, which is the mask of a straight run: the two tubes therefore meet at the wall between the cells,
     * and what is drawn there is what a player sees.
     */
    private static final List<List<PipeSize>> JUNCTION_PAIRS = List.of(
            List.of(PipeSize.MEDIUM, PipeSize.MEDIUM),
            List.of(PipeSize.TINY, PipeSize.MEDIUM),
            List.of(PipeSize.TINY, PipeSize.HUGE),
            List.of(PipeSize.NONUPLE, PipeSize.MEDIUM));

    /** One pipe of a drawing: the model to draw and how far along the block X axis its cell lies. */
    private record Part(String model, float shift) {
    }

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    /** One face of a drawing: where its corners land on screen, how deep it lies and how bright it is. */
    private record Face(int[] xs, int[] ys, float depth, float light) {
    }

    @Test
    void theSheetOfThePipesIsWritten() throws IOException {
        List<PipeSize> sizes = List.of(PipeSize.values());
        int width = CELL * MASKS.size();
        int height = CELL * sizes.size();
        BufferedImage sheet = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = sheet.createGraphics();
        graphics.setColor(BACKDROP);
        graphics.fillRect(0, 0, width, height);

        for (int row = 0; row < sizes.size(); row++) {
            for (int column = 0; column < MASKS.size(); column++) {
                String model = Pipes.modelName(PipeTexture.METAL, sizes.get(row), MASKS.get(column));
                draw(graphics, List.of(new Part(model, 0.0f)), PipeMaterials.COPPER.color(),
                        column * CELL, row * CELL);
            }
        }
        graphics.dispose();

        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(sheet, "png", PREVIEW.toFile());
        assertTrue(Files.size(PREVIEW) > 0, "the sheet is empty");
        assertFalse(sheet.getRGB(CELL / 2, CELL / 2) == BACKDROP.getRGB(),
                "the first drawing of the sheet holds nothing");
    }

    /**
     * Writes the sheet of the junctions: two pipes that meet at the wall between their cells.
     * <p>
     * Two pipes of one size put the very same plate against each other, so the wall between them is a seam
     * the eye must not find - an interrupted line is what a collar used to show. Two pipes of different
     * sizes cover one plate with the other, so the plate of the wider tube is what stands at the wall, which
     * is the collar of a reducer and the only place the art of a pipe is seen from its end.
     *
     * @throws IOException when the picture cannot be written
     */
    @Test
    void theSheetOfTheJunctionsIsWritten() throws IOException {
        int width = CELL * 2 * JUNCTION_PAIRS.size();
        int height = CELL;
        BufferedImage sheet = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = sheet.createGraphics();
        graphics.setColor(BACKDROP);
        graphics.fillRect(0, 0, width, height);

        for (int column = 0; column < JUNCTION_PAIRS.size(); column++) {
            List<PipeSize> pair = JUNCTION_PAIRS.get(column);
            // The two cells are drawn a half block apart around the middle of the column, so the wall
            // between them falls where the two pictures meet.
            List<Part> parts = List.of(
                    new Part(Pipes.modelName(PipeTexture.METAL, pair.get(0), Pipes.STRAIGHT_MASK), -0.5f),
                    new Part(Pipes.modelName(PipeTexture.METAL, pair.get(1), Pipes.STRAIGHT_MASK), 0.5f));
            draw(graphics, parts, PipeMaterials.COPPER.color(), column * 2 * CELL + CELL / 2, 0);
        }
        graphics.dispose();

        Files.createDirectories(JUNCTIONS.getParent());
        ImageIO.write(sheet, "png", JUNCTIONS.toFile());
        assertTrue(Files.size(JUNCTIONS) > 0, "the sheet of the junctions is empty");
        assertFalse(sheet.getRGB(CELL, CELL / 2) == BACKDROP.getRGB(),
                "the junction of the first pair of the sheet holds nothing");
    }

    /**
     * Draws one drawing into one column of a sheet, seen from the corner above a player.
     *
     * @param graphics pen the picture is drawn with
     * @param parts pipes of the drawing, drawn into the one list of faces that is sorted by depth
     * @param tint colour the grey art of the metal is painted with
     * @param originX left edge of the column in the picture
     * @param originY upper edge of the column in the picture
     */
    private static void draw(Graphics2D graphics, List<Part> parts, Color tint, int originX, int originY) {
        List<Face> faces = new ArrayList<>();
        for (Part part : parts) {
            BlockModel shape = ModelRegistry.byName(part.model());
            assertNotNull(shape, "the model " + part.model() + " is missing");
            for (ModelBox box : shape.boxes()) {
                for (BlockFace face : BlockFace.ALL) {
                    if (!box.hasFace(face)) {
                        continue;
                    }
                    int[] xs = new int[4];
                    int[] ys = new int[4];
                    float depth = 0.0f;
                    for (int corner = 0; corner < 4; corner++) {
                        float x = corner(box, 0, face, corner) + part.shift();
                        float y = corner(box, 1, face, corner);
                        float z = corner(box, 2, face, corner);
                        xs[corner] = originX + Math.round(CELL * 0.5f + (x - z) * CELL * SPREAD);
                        ys[corner] = originY
                                + Math.round(CELL * 0.5f + ((x + z) * RISE - y * HEIGHT) * CELL);
                        depth += x + y + z;
                    }
                    faces.add(new Face(xs, ys, depth, face.shade()));
                }
            }
        }
        // Painter's algorithm: the corner of the block furthest from the eye first, so a face of a tube
        // covers the one behind it and never the other way round.
        faces.sort(Comparator.comparingDouble(Face::depth));
        for (Face face : faces) {
            java.awt.Color colour = shade(tint, face.light());
            graphics.setColor(colour);
            graphics.fillPolygon(face.xs(), face.ys(), 4);
            graphics.setColor(colour.darker());
            graphics.drawPolygon(face.xs(), face.ys(), 4);
        }
    }

    /** The colour of a face: the tint of the material darkened by the light of its direction. */
    private static java.awt.Color shade(Color tint, float light) {
        return new java.awt.Color(clamp(tint.r * light), clamp(tint.g * light),
                clamp(tint.b * light));
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    /** The coordinate of one corner of a face along an axis, in the units of a block. */
    private static float corner(ModelBox box, int axis, BlockFace face, int index) {
        int side;
        float from;
        float to;
        if (axis == 0) {
            side = face.cornerX(index);
            from = box.fromX();
            to = box.toX();
        } else if (axis == 1) {
            side = face.cornerY(index);
            from = box.fromY();
            to = box.toY();
        } else {
            side = face.cornerZ(index);
            from = box.fromZ();
            to = box.toZ();
        }
        return (side == 1 ? to : from) / ModelBox.UNITS;
    }
}
