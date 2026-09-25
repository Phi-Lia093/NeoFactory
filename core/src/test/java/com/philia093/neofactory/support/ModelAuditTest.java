package com.philia093.neofactory.support;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.block.model.BlockModel;
import com.philia093.neofactory.block.model.ModelBox;
import com.philia093.neofactory.block.model.ModelRegistry;
import com.philia093.neofactory.block.state.BlockStateTable;
import com.philia093.neofactory.block.state.BlockStateTable.Variant;
import com.philia093.neofactory.render.BlockTextureCache;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the shapes of the blocks against the art, without opening a window.
 * <p>
 * A block is drawn from a model file, and every face of every box of that model names a picture. The
 * audit answers the two questions that would otherwise only be found by playing:
 * <ul>
 *     <li>does every block that is drawn have a model, and does every face of it name a picture that
 *         is really there?</li>
 *     <li>does every state a blockstate file names a model the game holds?</li>
 * </ul>
 * The list itself is written to {@code core/build/reports/model-audit.txt}, which is where a face
 * that has no file is read from when the art is tidied up.
 */
class ModelAuditTest {

    /** Report listing the shape of every block and the pictures it asks for. */
    private static final Path REPORT = Path.of("build", "reports", "model-audit.txt");

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void everyDrawnBlockHasAModelAndEveryFaceOfItAPicture() {
        List<String> missing = new ArrayList<>();
        StringBuilder report = new StringBuilder("# Models of the blocks\n\n");
        for (Block block : BlockRegistry.all()) {
            if (!block.isDrawable()) {
                continue;
            }
            BlockModel model = ModelRegistry.of(block);
            if (model.isEmpty()) {
                missing.add("block " + block.name() + " has no model and no picture");
                continue;
            }
            report.append(block.name()).append(": ").append(model.boxes().size())
                    .append(" boxes, ");
            for (ModelBox box : model.boxes()) {
                for (BlockFace face : BlockFace.ALL) {
                    if (!box.hasFace(face)) {
                        continue;
                    }
                    report.append(face).append('=').append(box.face(face).picture());
                    if (box.face(face).hasOverlay()) {
                        report.append('+').append(box.face(face).overlay());
                    }
                    report.append(' ');
                }
            }
            report.append('\n');
            for (String picture : model.pictures()) {
                Path file = TestRegistries.ASSETS.resolve(BlockTextureCache.resolvePath(picture));
                if (!Files.isRegularFile(file)) {
                    missing.add("block " + block.name() + " asks for the picture " + picture
                            + ", which is not there");
                }
            }
        }
        write(report);
        assertTrue(missing.isEmpty(), String.join("\n", missing));
        assertFalse(report.toString().isBlank(), "no block was audited");
    }

    @Test
    void everyStateNamesAModelTheGameHolds() {
        List<String> missing = new ArrayList<>();
        for (Block block : BlockRegistry.all()) {
            BlockStateTable table = block.states();
            if (table == BlockStateTable.NONE) {
                continue;
            }
            for (int state = 0; state < table.stateCount(); state++) {
                Variant variant = table.variant(state);
                if (variant.model() != null && ModelRegistry.byName(variant.model()) == null) {
                    missing.add("state " + state + " of " + block.name() + " names the model "
                            + variant.model() + ", which no file holds: values "
                            + table.decode(state));
                }
            }
        }
        assertTrue(missing.isEmpty(), String.join("\n", missing));
        assertTrue(BlockRegistry.byName("furnace").states().stateCount() == 4,
                "the furnace looks in four directions");
    }

    @Test
    void everyBlockThatIsNotDrawnTheSameWayHasAStateFile() {
        // A block whose model names more than one picture is a block whose appearance the game has
        // to look up, so a missing file shows up as a block drawn with one picture on six faces.
        List<String> missing = new ArrayList<>();
        for (Block block : BlockRegistry.all()) {
            if (!block.isDrawable() || block.states() != BlockStateTable.NONE) {
                continue;
            }
            if (ModelRegistry.byName(block.name()) == null) {
                missing.add("block " + block.name() + " has neither a model file nor a state file");
            }
        }
        assertTrue(missing.isEmpty(), String.join("\n", missing));
    }

    /** Writes the report, so the shape of a block can be read next to what a player sees. */
    private static void write(StringBuilder report) {
        try {
            Files.createDirectories(REPORT.getParent());
            Files.writeString(REPORT, report.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write " + REPORT, e);
        }
    }
}
