package com.philia093.neofactory.support;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the pictures of the project, without opening a window.
 * <p>
 * The audit answers two questions that are otherwise only found by playing:
 * <ul>
 *     <li>does every block and every item have the file its texture name points
 *         at, and does an animation frame fit into its sheet?</li>
 *     <li>which pictures does nothing reference at all?</li>
 * </ul>
 * The second answer is written to {@code core/build/reports/texture-audit.txt},
 * which is where the list of spare art is read from when the resources are tidied
 * up. The checks themselves are the regression net: a renamed picture or a lost
 * file fails the build instead of showing up as a missing tile while playing.
 * <p>
 * The art of a top-down game only needed the view from above, which is why the block folder of the
 * flat engine held 271 pictures of the pack and not 356: the side, the bottom and the front of
 * every block had been deleted, and this class failed the build when one of them came back. A world
 * of cubes shows six faces, so they were fetched home again by
 * {@code build/verify/extract_3d_assets.ps1} and the rule turned around: every face of a block that
 * is more than one picture has to be there, see {@link #everyFaceOfAMultiFaceBlockExists()}.
 */
class TextureAuditTest {

    /** Art root of the project, the tests run inside the core module. */
    private static final Path ASSETS = Path.of("..", "assets");

    /** Report listing what the game uses and what it does not. */
    private static final Path REPORT = Path.of("build", "reports", "texture-audit.txt");

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void everyBlockAndItemTextureExists() {
        List<String> missing = new ArrayList<>();
        for (Block block : BlockRegistry.all()) {
            if (block.texture().isEmpty()) {
                continue;
            }
            if (!Files.isRegularFile(ASSETS.resolve(BlockTextureCache.resolvePath(block.texture())))) {
                missing.add("block " + block.name() + " points at blocks/" + block.texture() + ".png");
            }
        }
        for (Item item : ItemRegistry.all()) {
            if (!item.hasTexture()) {
                continue;
            }
            if (!Files.isRegularFile(ASSETS.resolve(BlockTextureCache.resolvePath(item.texture())))) {
                missing.add("item " + item.name() + " points at " + item.texture() + ".png");
            }
            // A shape of a material may carry a second layer, see Item#overlayTexture: the picture
            // of the overlay is a file of its own and is watched like the shape it covers.
            if (item.hasOverlay() && !Files.isRegularFile(ASSETS.resolve(
                    BlockTextureCache.resolvePath(item.overlayTexture())))) {
                missing.add("item " + item.name() + " points at its overlay "
                        + item.overlayTexture() + ".png");
            }
        }
        assertTrue(missing.isEmpty(), "textures are missing:\n" + String.join("\n", missing));
    }

    @Test
    void everyIconFrameFitsIntoItsSheet() throws IOException {
        List<String> wrong = new ArrayList<>();
        for (Item item : ItemRegistry.all()) {
            if (!item.hasTexture()) {
                continue;
            }
            Path file = ASSETS.resolve(BlockTextureCache.resolvePath(item.texture()));
            if (!Files.isRegularFile(file)) {
                continue;
            }
            BufferedImage image = ImageIO.read(file.toFile());
            if (image == null) {
                wrong.add(item.name() + ": the picture cannot be read");
                continue;
            }
            int frames = Math.max(1, image.getHeight() / Constants.ITEM_ICON_SIZE);
            if (item.iconFrame() < 0 || item.iconFrame() >= frames) {
                wrong.add(item.name() + ": frame " + item.iconFrame() + " of " + frames);
            }
        }
        assertTrue(wrong.isEmpty(), "icons use a frame their sheet does not hold:\n"
                + String.join("\n", wrong));
    }

    @Test
    void theSaveFolderLivesOutsideTheAssets() {
        // The game writes its save games into its working directory. When that is
        // the asset folder again, every save game ends up inside the shipped jar.
        assertFalse(Files.exists(ASSETS.resolve("saves")),
                "save games must not be written into the assets, see the run task");
    }

    /**
     * Every face of a block that is more than one picture is really there.
     * <p>
     * The list lives in {@link MultiFaceTextures} and holds the 78 pictures the flat engine deleted,
     * plus the metadata of the seven sheets whose animation is described outside of them. The check
     * is the net under that art: a face that was renamed, lost while the pack was replaced or
     * mistyped in the script that fetched it fails the build here, instead of showing up as a hole
     * in the world that a player finds before anybody else does.
     */
    @Test
    void everyFaceOfAMultiFaceBlockExists() {
        List<String> missing = new ArrayList<>();
        for (String face : MultiFaceTextures.FACES) {
            if (!Files.isRegularFile(ASSETS.resolve("blocks").resolve(face + ".png"))) {
                missing.add("blocks/" + face + ".png");
            }
        }
        for (String sheet : MultiFaceTextures.ANIMATED_SHEETS) {
            if (!Files.isRegularFile(ASSETS.resolve("blocks").resolve(sheet + ".png.mcmeta"))) {
                missing.add("blocks/" + sheet + ".png.mcmeta");
            }
        }
        assertTrue(missing.isEmpty(), "the faces of a world of cubes are missing, run"
                + " build/verify/extract_3d_assets.ps1:\n" + String.join("\n", missing));
    }

    @Test
    void theAuditReportListsWhatTheGameUses() throws IOException {
        Set<String> usedBlocks = new TreeSet<>();
        for (Block block : BlockRegistry.all()) {
            if (!block.texture().isEmpty()) {
                usedBlocks.add(block.texture());
            }
        }
        Set<String> usedItems = new TreeSet<>();
        for (Item item : ItemRegistry.all()) {
            if (item.hasTexture() && item.texture().startsWith(Item.ITEM_FOLDER)) {
                usedItems.add(item.texture().substring(Item.ITEM_FOLDER.length()));
            }
            if (item.hasOverlay() && item.overlayTexture().startsWith(Item.ITEM_FOLDER)) {
                usedItems.add(item.overlayTexture().substring(Item.ITEM_FOLDER.length()));
            }
        }
        // The faces of the blocks that are more than one picture are kept on purpose: the blocks
        // name them one by one as the world grows its third axis, see MultiFaceTextures. Listing
        // them as art nothing references would hide the art that is really spare.
        Set<String> usedBlocksAndKeptFaces = new TreeSet<>(usedBlocks);
        usedBlocksAndKeptFaces.addAll(MultiFaceTextures.FACES);

        StringBuilder report = new StringBuilder();
        report.append("# Texture audit\n\n");
        report.append("Referenced blocks: ").append(usedBlocks.size()).append('\n');
        report.append("Referenced items: ").append(usedItems.size()).append('\n');
        report.append("Faces kept for a world of cubes: ")
                .append(MultiFaceTextures.FACES.size()).append("\n\n");

        List<String> spareBlocks = spare(ASSETS.resolve("blocks"), usedBlocksAndKeptFaces);
        List<String> spareItems = spare(ASSETS.resolve("items"), usedItems);
        report.append("## Blocks nothing references (").append(spareBlocks.size()).append(")\n");
        appendAll(report, spareBlocks);
        report.append("\n## Items nothing references (").append(spareItems.size()).append(")\n");
        appendAll(report, spareItems);
        report.append("\n## Other folders\n");
        for (String folder : new String[] {"gui", "map", "font"}) {
            report.append(folder).append(": ").append(count(ASSETS.resolve(folder)))
                    .append(" files\n");
        }

        Files.createDirectories(REPORT.getParent());
        Files.writeString(REPORT, report.toString(), StandardCharsets.UTF_8);
        System.out.println("texture audit written to " + REPORT.toAbsolutePath()
                + ": " + spareBlocks.size() + " spare blocks, " + spareItems.size()
                + " spare items");

        // The report is only useful while the game really uses pictures, so this
        // fails when the audit itself stopped finding anything.
        assertFalse(usedBlocks.isEmpty() && usedItems.isEmpty(), "no texture is referenced");
    }

    /** Pictures of a folder that the given set of names does not use. */
    private static List<String> spare(Path folder, Set<String> used) throws IOException {
        List<String> spare = new ArrayList<>();
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (name.endsWith(".png") && !used.contains(name.substring(0, name.length() - 4))) {
                    spare.add(name);
                }
            }
        }
        spare.sort(null);
        return spare;
    }

    /** Amount of files below a folder. */
    private static long count(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) {
            return 0L;
        }
        try (Stream<Path> files = Files.walk(folder)) {
            return files.filter(Files::isRegularFile).count();
        }
    }

    private static void appendAll(StringBuilder report, List<String> names) {
        for (String name : names) {
            report.append(name).append('\n');
        }
    }
}

