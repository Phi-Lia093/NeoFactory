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
 * The art of a top-down game only needs the view from above, which is why the
 * block folder may not hold side or bottom faces, see
 * {@link #theBlockFolderStoresTopFacesOnly()}.
 */
class TextureAuditTest {

    /** Art root of the project, the tests run inside the core module. */
    private static final Path ASSETS = Path.of("..", "assets");

    /** Report listing what the game uses and what it does not. */
    private static final Path REPORT = Path.of("build", "reports", "texture-audit.txt");

    /**
     * Faces of the art pack whose file name carries no suffix.
     * <p>
     * The pack names the side of an oak log {@code log_oak} and its top
     * {@code log_oak_top}, so a rule that only looks at suffixes would keep the side
     * faces of the logs, of the sandstone and of a few other blocks.
     */
    private static final Set<String> SIDE_FACES_WITHOUT_SUFFIX = Set.of("log_acacia",
            "log_big_oak", "log_birch", "log_jungle", "log_oak", "log_spruce",
            "hopper_outside", "quartz_block_lines", "quartz_block_chiseled",
            "sandstone_normal", "sandstone_carved", "sandstone_smooth");

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
     * {@code true} when a picture name uses a side, a bottom or another face that a
     * view from above never shows.
     *
     * @param name file name without extension
     * @return {@code true} for a face the game does not need
     */
    private static boolean isOtherFace(String name) {
        return name.matches(".*(_side|_bottom|_front|_end|_upper|_lower|_inner"
                + "|_inside|_base|_face).*")
                || name.endsWith("_back")
                || name.endsWith("_stem")
                || SIDE_FACES_WITHOUT_SUFFIX.contains(name);
    }

    @Test
    void theBlockFolderStoresTopFacesOnly() throws IOException {
        List<String> otherFaces = new ArrayList<>();
        try (Stream<Path> files = Files.list(ASSETS.resolve("blocks"))) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (name.endsWith(".png") && isOtherFace(name.substring(0, name.length() - 4))) {
                    otherFaces.add(name);
                }
            }
        }
        assertTrue(otherFaces.isEmpty(), "the block folder holds faces a top down view"
                + " never shows, delete them or extend this rule:\n"
                + String.join("\n", otherFaces));
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
        }

        StringBuilder report = new StringBuilder();
        report.append("# Texture audit\n\n");
        report.append("Referenced blocks: ").append(usedBlocks.size()).append('\n');
        report.append("Referenced items: ").append(usedItems.size()).append("\n\n");

        List<String> spareBlocks = spare(ASSETS.resolve("blocks"), usedBlocks);
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

