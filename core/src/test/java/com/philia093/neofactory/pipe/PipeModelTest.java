package com.philia093.neofactory.pipe;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.model.BlockModel;
import com.philia093.neofactory.block.model.ModelBox;
import com.philia093.neofactory.block.model.ModelRegistry;
import com.philia093.neofactory.block.state.BlockStateTable;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the models and the blockstates the generator wrote against the table of the pipes.
 * <p>
 * The files under {@code assets} are written by {@code tools/gen_pipe_models.ps1} and not by hand, so this
 * test is what keeps the script and the game in step: it walks all sixty four states of all twenty eight
 * pipes, looks up the model and the quarter turn the game would draw, and compares them with what
 * {@link Pipes} says about the mask - and it opens every model the states name and checks that the boxes
 * really stand where that mask says they stand.
 */
class PipeModelTest {

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void everyStateNamesTheModelOfItsMaskAndTheTurnThatShowsIt() {
        for (Pipes.Pipe pipe : Pipes.all()) {
            BlockStateTable states = pipe.block().states();
            assertEquals(64, states.stateCount(), pipe + " has to know every way to be joined");
            for (int state = 0; state <= Pipes.ALL_MASK; state++) {
                BlockStateTable.Variant variant = states.variant(state);
                assertEquals(pipe.modelName(state), variant.model(), pipe + " state " + state);
                assertEquals(90 * Pipes.turnsToDraw(state), variant.rotateY(), pipe + " state " + state);
                assertNotNull(ModelRegistry.byName(variant.model()),
                        pipe + " state " + state + " names a model no file holds");
                Map<String, String> values = states.decode(state);
                for (BlockFace face : Pipes.DIRECTIONS) {
                    String wanted = Pipes.isConnected(state, face) ? "true" : "false";
                    assertEquals(wanted, values.get(face.name().toLowerCase(Locale.ROOT)),
                            pipe + " state " + state + " and its " + face);
                }
            }
        }
    }

    @Test
    void everyFaceOfEveryModelNamesAPictureThatIsThere() {
        for (PipeTexture texture : PipeTexture.values()) {
            for (PipeSize size : PipeSize.values()) {
                for (int mask = 0; mask <= Pipes.ALL_MASK; mask++) {
                    String name = Pipes.modelName(texture, size, mask);
                    BlockModel model = ModelRegistry.byName(name);
                    assertNotNull(model, "the model " + name + " is missing");
                    for (String picture : model.pictures()) {
                        Path file = TestRegistries.ASSETS.resolve(BlockTextureCache.resolvePath(picture));
                        assertTrue(Files.isRegularFile(file),
                                name + " asks for " + picture + ", which is not there");
                    }
                }
            }
        }
    }

    @Test
    void everyModelNamesTheArtOfItsOwnFamilyAndSize() {
        for (PipeTexture texture : PipeTexture.values()) {
            for (PipeSize size : PipeSize.values()) {
                for (int state = 0; state <= Pipes.ALL_MASK; state++) {
                    BlockModel model = ModelRegistry.byName(Pipes.modelName(texture, size, state));
                    String where = Pipes.modelName(texture, size, state);
                    Set<String> allowed = Set.of(texture.side(), texture.end(size));
                    if (!model.isEmpty()) {
                        assertFalse(model.pictures().isEmpty(), where + " names no picture");
                    }
                    for (String picture : model.pictures()) {
                        assertTrue(allowed.contains(picture),
                                where + " draws " + picture + ", which is not the art of its family");
                    }
                }
            }
        }
    }

    @Test
    void aSinglePipeHasAnArmWhereItIsJoined() {
        for (PipeTexture texture : PipeTexture.values()) {
            for (PipeSize size : PipeSize.values()) {
                if (size.isBundle()) {
                    continue;
                }
                for (int state = 0; state <= Pipes.ALL_MASK; state++) {
                    int mask = Pipes.canonical(state);
                    BlockModel model = ModelRegistry.byName(Pipes.modelName(texture, size, state));
                    String where = Pipes.modelName(texture, size, state) + " for the state " + state;
                    for (BlockFace face : Pipes.DIRECTIONS) {
                        assertEquals(Pipes.isConnected(mask, face), hasArm(model, size, face),
                                where + " has no arm towards " + face);
                        if (Pipes.isConnected(mask, face)) {
                            assertTrue(hasPlate(model, size, face, texture),
                                    where + " ends the arm towards " + face
                                            + " without the plate of its size");
                        }
                    }
                    int connections = Integer.bitCount(mask);
                    // The core carries a face where the pipe is not joined and every arm carries five: the
                    // four along the tube and the plate at the border. A pipe that is joined on every side
                    // keeps its core with all six of its faces, because it is the joint of the six arms and
                    // a joint that is left out is a hole, see Boxes in tools/gen_pipe_models.ps1.
                    assertEquals((connections == 6 ? 6 : 6 - connections) + 5 * connections,
                            model.faceCount(), where + " draws the wrong number of faces");
                    List<ModelBox> cores = cores(model, size);
                    assertEquals(1, cores.size(), where + " has to hold its core");
                    assertEquals(connections == 6 ? 6 : 6 - connections, cores.get(0).faceCount(),
                            where + " caps its core where it is not joined");
                    if (connections == 6) {
                        // Every side is joined, so the core is the joint the six arms meet in: its faces are
                        // inside the tube, and without them the arms would enclose an open middle that the
                        // cell is looked through.
                        for (BlockFace face : Pipes.DIRECTIONS) {
                            assertEquals(texture.side(), cores.get(0).face(face).picture(),
                                    where + " shows its core towards " + face);
                        }
                    }
                }
            }
        }
    }

    @Test
    void aSinglePipeIsItsTubeAndNothingElse() {
        for (PipeTexture texture : PipeTexture.values()) {
            for (PipeSize size : PipeSize.values()) {
                if (size.isBundle()) {
                    continue;
                }
                for (int state = 0; state <= Pipes.ALL_MASK; state++) {
                    BlockModel model = ModelRegistry.byName(Pipes.modelName(texture, size, state));
                    String where = Pipes.modelName(texture, size, state) + " for the state " + state;
                    for (ModelBox box : model.boxes()) {
                        assertTrue(isTheTubeOnly(box, size),
                                where + " holds a box that is wider than the tube of its size, which is the "
                                        + "collar a line of pipes used to show");
                    }
                }
            }
        }
    }

    @Test
    void aBundleIsOneBoxThatShowsEverySide() {
        for (PipeTexture texture : PipeTexture.values()) {
            for (PipeSize size : PipeSize.values()) {
                if (!size.isBundle()) {
                    continue;
                }
                for (int state = 0; state <= Pipes.ALL_MASK; state++) {
                    int mask = Pipes.canonical(state);
                    BlockModel model = ModelRegistry.byName(Pipes.modelName(texture, size, state));
                    String where = Pipes.modelName(texture, size, state) + " for the state " + state;
                    assertEquals(1, model.boxes().size(), where + " is one box");
                    ModelBox box = model.boxes().get(0);
                    assertTrue(box.isWholeCube(), where + " has to fill its cell");
                    // Every side carries the plate of the bundle, joined or not: a bundle is a block of tubes
                    // and is read by its cross section from wherever a player looks at it, which is also what
                    // the item of a bundle shows in a slot. A side that is left out would be a hole through a
                    // block that fills its cell.
                    assertEquals(6, model.faceCount(), where + " shows all six of its sides");
                    for (BlockFace face : Pipes.DIRECTIONS) {
                        assertTrue(box.hasFace(face), where + " has no side towards " + face);
                        assertEquals(texture.end(size), box.face(face).picture(),
                                where + " has to show the plate of the bundle towards " + face);
                    }
                }
            }
        }
    }

    @Test
    void theModelOfAPipeBlockIsAStraightLengthOfIt() {
        int straight = Pipes.canonical(Pipes.STRAIGHT_MASK);
        for (Pipes.Pipe pipe : Pipes.all()) {
            BlockModel model = ModelRegistry.of(pipe.block());
            assertFalse(model.isEmpty(), pipe + " has no model of its own and no picture");
            for (String picture : model.pictures()) {
                assertTrue(picture.startsWith(pipe.material().texture().folder()),
                        pipe + " draws " + picture + ", which is not the art of its family");
            }
            if (pipe.size().isBundle()) {
                // A bundle fills its cell with one box, so its picture shows the whole cell.
                assertEquals(1, model.boxes().size(), pipe + " is one box");
                assertTrue(model.boxes().get(0).isWholeCube(), pipe + " fills its cell");
                continue;
            }
            for (BlockFace face : Pipes.DIRECTIONS) {
                assertEquals(Pipes.isConnected(straight, face), hasArm(model, pipe.size(), face),
                        pipe + " has no arm towards " + face);
            }
        }
    }

    /**
     * {@code true} when a model holds the arm of a tube towards a direction.
     * <p>
     * An arm is the box of the tube that runs from the core of the pipe to the border of the block, so it
     * has the width of the tube on the two sides across from the direction and reaches the border along it.
     */
    private static boolean hasArm(BlockModel model, PipeSize size, BlockFace face) {
        for (ModelBox box : model.boxes()) {
            if (acrossIsTheTube(box, face, size.tubeFrom(), size.tubeTo())
                    && reachesTheBorder(box, face, size)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code true} when the arm towards a direction carries the plate of its size at the border.
     * <p>
     * The plate is the end face of the arm, which is the picture of the size and not the flank of the tube.
     * Two pipes of the same size put two of those plates against each other and the seam between them
     * disappears; two pipes of different sizes cover one plate with the other, so the plate of the wider
     * tube is what a player sees where the sizes meet. A pipe whose arm ends in the open shows the plate as
     * well, which is the ending of the tube.
     */
    private static boolean hasPlate(BlockModel model, PipeSize size, BlockFace face, PipeTexture texture) {
        String plate = texture.end(size);
        for (ModelBox box : model.boxes()) {
            if (!acrossIsTheTube(box, face, size.tubeFrom(), size.tubeTo())
                    || !reachesTheBorder(box, face, size) || !box.hasFace(face)) {
                continue;
            }
            if (plate.equals(box.face(face).picture())) {
                return true;
            }
        }
        return false;
    }

    /** {@code true} when a box runs from the core of a pipe to the very border of its cell. */
    private static boolean reachesTheBorder(ModelBox box, BlockFace face, PipeSize size) {
        if (face.x() > 0) {
            return box.fromX() == size.tubeTo() && box.toX() == ModelBox.UNITS;
        }
        if (face.x() < 0) {
            return box.fromX() == 0.0f && box.toX() == size.tubeFrom();
        }
        if (face.y() > 0) {
            return box.fromY() == size.tubeTo() && box.toY() == ModelBox.UNITS;
        }
        if (face.y() < 0) {
            return box.fromY() == 0.0f && box.toY() == size.tubeFrom();
        }
        if (face.z() > 0) {
            return box.fromZ() == size.tubeTo() && box.toZ() == ModelBox.UNITS;
        }
        return box.fromZ() == 0.0f && box.toZ() == size.tubeFrom();
    }

    /**
     * {@code true} when every box of a single pipe is its tube and nothing wider.
     * <p>
     * A pipe of the game used to carry a collar at every border it joined - a flat plate a little wider than
     * the tube, which made a line of pipes read as segments. It is gone: what a joined side shows is the end
     * face of the arm, and the arm is the tube. This is what makes the seam of two pipes of one size
     * invisible, so every box has to keep exactly the width of the tube on two of its three axes.
     */
    private static boolean isTheTubeOnly(ModelBox box, PipeSize size) {
        int across = 0;
        if (box.fromX() == size.tubeFrom() && box.toX() == size.tubeTo()) {
            across++;
        }
        if (box.fromY() == size.tubeFrom() && box.toY() == size.tubeTo()) {
            across++;
        }
        if (box.fromZ() == size.tubeFrom() && box.toZ() == size.tubeTo()) {
            across++;
        }
        return across >= 2;
    }

    /** {@code true} when a box has the width of a tube on the two sides across from a direction. */
    private static boolean acrossIsTheTube(ModelBox box, BlockFace face, float tubeFrom, float tubeTo) {
        boolean acrossX = face.x() == 0 && box.fromX() == tubeFrom && box.toX() == tubeTo;
        boolean acrossY = face.y() == 0 && box.fromY() == tubeFrom && box.toY() == tubeTo;
        boolean acrossZ = face.z() == 0 && box.fromZ() == tubeFrom && box.toZ() == tubeTo;
        if (face.x() != 0) {
            return acrossY && acrossZ;
        }
        if (face.y() != 0) {
            return acrossX && acrossZ;
        }
        return acrossX && acrossY;
    }

    /** The boxes of a model that fill the tube of a size and nothing else, the core of a pipe. */
    private static List<ModelBox> cores(BlockModel model, PipeSize size) {
        List<ModelBox> found = new ArrayList<>();
        for (ModelBox box : model.boxes()) {
            if (box.fromX() == size.tubeFrom() && box.toX() == size.tubeTo()
                    && box.fromY() == size.tubeFrom() && box.toY() == size.tubeTo()
                    && box.fromZ() == size.tubeFrom() && box.toZ() == size.tubeTo()) {
                found.add(box);
            }
        }
        return found;
    }

}
