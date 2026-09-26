package com.philia093.neofactory.render;

import com.badlogic.gdx.utils.ObjectMap;
import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.world.Section;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the pictures of the world cover every face the world may draw.
 * <p>
 * The texture array is stacked once while the game starts, and the mesher draws a face only when it finds
 * the layer of its picture: a picture the array does not hold is a face that is not drawn at all. A block
 * whose cell stands there and draws nothing is not only a hole in the world - it is also the icon of its
 * item, which the interface bakes by drawing the block itself, so a block that draws nothing is baked
 * empty and asked for again on every frame.
 * <p>
 * A block is drawn from the model its <i>state</i> names, and that model is not always the one named after
 * the block: the lower and the upper half of a slab are models of their own, and an anvil names one of its
 * plate beside the body it is. Both cases are checked here - that every state of every block draws
 * something with the pictures of the world, and that the pictures only a state file reaches are among them.
 */
class BlockPicturesTest {

    /** Art root of the project, the tests run inside the core module. */
    private static final Path ASSETS = TestRegistries.ASSETS;

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void everyStateOfEveryBlockIsDrawnWithThePicturesOfTheWorld() {
        List<String> world = worldPictures();
        List<String> empty = new ArrayList<>();
        for (Block block : BlockRegistry.all()) {
            for (int state = 0; state < block.states().stateCount(); state++) {
                if (!isDrawnByTheWorld(block, state)) {
                    continue;
                }
                if (cornersOf(block, state, world) == 0) {
                    empty.add("state " + state + " of " + block.name() + " draws no face at all: "
                            + block.states().decode(state));
                }
            }
        }
        assertTrue(empty.isEmpty(), String.join("\n", empty));
    }

    @Test
    void thePicturesAStateFileReachesAreInTheWorld() {
        List<String> world = worldPictures();

        assertTrue(world.contains("stone_slab_top"), "the plate of the upper half of a slab");
        assertTrue(world.contains("stone_slab_side"), "and the rim of its upper half");
        assertTrue(world.contains("anvil/anvil_top"), "the plate of an anvil");
        assertTrue(world.contains("anvil/anvil_base"), "and the body of its base");
    }

    /**
     * Every picture of the art pack is stored with the alpha channel the array is uploaded from.
     * <p>
     * A layer is written with four bytes per pixel, and libGDX reads a picture into as many pixels as its
     * file carries: a picture stored as plain RGB, or as a palette, is three bytes - or one - where the
     * upload reads four, so every pixel of it slips and the layer shows a shifted, coloured mesh instead of
     * the picture. That is exactly how the plates of the pipes were stored: a bundle of their pipes was a
     * fine coloured grid on screen instead of the plate with four or nine tubes on it. The array converts a
     * picture that arrives in another format as well, see {@code BlockPictures#toLayerFormat}, and this check
     * is what keeps the art itself honest.
     */
    @Test
    void everyPictureOfTheWorldCarriesAnAlphaChannel() {
        int checked = 0;
        List<String> plain = new ArrayList<>();
        for (String folder : List.of("blocks", "items")) {
            Path root = ASSETS.resolve(folder);
            try (var files = Files.walk(root)) {
                for (Path file : files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".png")).toList()) {
                    checked++;
                    if (colourTypeOf(file) != RGBA) {
                        plain.add(file + " carries no alpha channel, see BlockPictures#build");
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to walk " + root, e);
            }
        }

        assertTrue(checked > 100, "the art of the world was not found at all: " + checked + " pictures");
        assertTrue(plain.isEmpty(), String.join("\n", plain));
    }

    /** Colour type of a PNG file that carries an alpha channel, see the format of a PNG. */
    private static final int RGBA = 6;

    /**
     * Size of a PNG, read from its header, as width and height.
     * <p>
     * The width and the height stand in the first block of the file, behind the eight bytes of the
     * signature and the four of the name and the length of that block, each of them four bytes long and
     * written the way round a picture reader expects them.
     *
     * @param file picture to read
     * @return the width and the height in pixels
     * @throws IOException when the file cannot be read
     */
    private static int[] sizeOf(Path file) throws IOException {
        byte[] header = new byte[24];
        try (InputStream stream = Files.newInputStream(file)) {
            if (stream.read(header) != header.length) {
                throw new IOException(file + " is no picture, it is too short for a header");
            }
        }
        return new int[] {fourBytes(header, 16), fourBytes(header, 20)};
    }

    /** One four byte number of a header, the way round a picture reader writes it. */
    private static int fourBytes(byte[] header, int at) {
        return (header[at] & 0xFF) << 24 | (header[at + 1] & 0xFF) << 16 | (header[at + 2] & 0xFF) << 8
                | (header[at + 3] & 0xFF);
    }

    /**
     * Colour type of a PNG, read from its header.
     * <p>
     * The byte behind the signature and the length of the first block names how the pixels are stored:
     * {@code 2} is red, green and blue, {@code 3} a palette, {@code 6} red, green, blue and alpha.
     *
     * @param file picture to read
     * @return the colour type
     * @throws IOException when the file cannot be read
     */
    private static int colourTypeOf(Path file) throws IOException {
        byte[] header = new byte[26];
        try (InputStream stream = Files.newInputStream(file)) {
            if (stream.read(header) != header.length) {
                throw new IOException(file + " is no picture, it is too short for a header");
            }
        }
        return header[25] & 0xFF;
    }

    /**
     * A drop of an item that is no block is drawn as two crossed cards of its picture, see
     * {@code ItemCubeMeshes}: a picture the array of the world does not hold is an item that lies on the
     * ground as nothing at all, which is a drop a player walks past without ever seeing it. The pictures of
     * a body are cut out of a file instead of living in one, so they are skipped: only the items are asked,
     * and only the ones that place no block, because a block item is drawn as the cube of its block.
     */
    @Test
    void everyItemThatIsNoBlockHasAPictureInTheWorld() {
        List<String> world = worldPictures();
        List<String> missing = new ArrayList<>();
        int checked = 0;
        for (Item item : ItemRegistry.all()) {
            if (item.block() != null || item.texture().isEmpty()) {
                continue;
            }
            checked++;
            if (!world.contains(BlockPictures.itemPicture(item)) && !world.contains(item.texture())) {
                missing.add(item.name());
            }
        }
        assertTrue(missing.isEmpty(),
                "a drop of these items would lie on the ground as nothing at all: " + missing);
        assertTrue(checked > 0, "no item that is no block was checked at all");
    }

    /**
     * Names the texture array holds, decided by the files on disk instead of by a graphics card.
     *
     * @return the names, in the order the layers are numbered
     */
    private static List<String> worldPictures() {
        return BlockPictures.pictureNames(new ObjectMap<>(), new ObjectMap<>(), name -> {
            if (BlockPictures.HAND.equals(name) || SkinRegions.isSkinFace(name)) {
                // A piece cut out of the skin of a body and no file of its own: no cube shows a body, the
                // hand of a view is drawn by the renderer of the figure.
                return false;
            }
            return Files.isRegularFile(ASSETS.resolve(BlockPictures.path(name)));
        }, BlockPicturesTest::framesInTheFileOf);
    }

    /**
     * Frames the file of a picture holds, read out of the picture itself.
     * <p>
     * The game reads them the very same way while it stacks the array, see
     * {@link BlockPictures#frameCountOf(int, int)}: a picture of one tile stands still and a strip of
     * tiles is a picture that moves.
     *
     * @param name name of the picture, relative to {@code blocks/} without extension
     * @return the frames, at least one
     */
    private static int framesInTheFileOf(String name) {
        try {
            int[] size = sizeOf(ASSETS.resolve(BlockPictures.path(name)));
            return BlockPictures.frameCountOf(size[0], size[1]);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + name, e);
        }
    }

    /**
     * A strip of frames is counted as the frames it holds and takes a layer each.
     * <p>
     * The top of a machine that works is a strip of four tiles, the first frame at the top of the file,
     * and everything else of the machine is a single picture: the engine plays the run of the strip with
     * the tick of the world while the casing around it stays where it is, see
     * {@code BlockShader#ANIMATION} and {@code MeshData#FRAMES}.
     */
    @Test
    void aStripOfFramesIsCountedFromItsFile() {
        assertEquals(4, framesInTheFileOf("grinder/grinder_top_active"),
                "the gear on the top of a running grinder is the strip of the art pack");
        assertEquals(4, framesInTheFileOf("compressor/compressor_top_active"),
                "and the top of a compressor is written as the quarter turns of its picture");
        assertEquals(4, framesInTheFileOf("steel_extractor/steel_extractor_top_active"),
                "which the machine of steel carries as well");
        assertEquals(1, framesInTheFileOf("grinder/grinder_top"),
                "the top of a machine that stands still is one picture");
        assertEquals(1, framesInTheFileOf("grinder/grinder_front_active"),
                "and the mouth of a running machine does not turn either");
        assertEquals(1, framesInTheFileOf("bronze_casing/bronze_casing_side"),
                "the casing of a machine is a single picture like any other block");

        assertTrue(worldPictures().contains("compressor/compressor_top_active"),
                "a strip is a picture of the world like every other one");
    }

    /**
     * {@code true} when every picture a state is drawn from is art this project holds.
     * <p>
     * Not every block of the game carries a picture of the art pack: the water of a lake and the lava of a
     * volcano travel with the fluids and are drawn by their own pass, so a state whose model names a
     * picture that is not there is not a block the world leaves out. Everything else has to be drawn -
     * a picture that is there and is not in the array is a face the world silently does not draw.
     *
     * @param block block to ask
     * @param state state of that block
     * @return {@code true} when the state is expected to be seen in the world
     */
    private static boolean isDrawnByTheWorld(Block block, int state) {
        Set<String> pictures = block.shown(state).model().pictures();
        if (pictures.isEmpty()) {
            return false;
        }
        for (String picture : pictures) {
            if (!Files.isRegularFile(ASSETS.resolve(BlockPictures.path(picture)))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Amount of corners a state of a block draws when only the pictures of the world may be used.
     *
     * @param block block to draw
     * @param state state of that block
     * @param world pictures the array holds
     * @return the amount of corners of the faces that were drawn, {@code 0} for a block that stays invisible
     */
    private static int cornersOf(Block block, int state, List<String> world) {
        Section section = new Section(0);
        section.setRawId(0, 0, 0, block.id());
        section.setState(0, 0, 0, state);
        SectionMesher.Blocks blocks = (x, y, z) -> x == 0 && y == 0 && z == 0 ? block : Blocks.AIR;
        List<MeshData> meshes = SectionMesher.build(section, 0, 0, 0, blocks,
                name -> world.contains(name) ? 0 : -1);
        int corners = 0;
        for (MeshData mesh : meshes) {
            corners += mesh.vertexCount();
        }
        return corners;
    }

    @Test
    void theWorldHoldsEveryStageOfABreak() {
        List<String> world = worldPictures();

        for (int stage = 0; stage < BlockPictures.DESTROY_STAGES; stage++) {
            assertTrue(world.contains(BlockPictures.destroyStagePicture(stage)),
                    "the cracks of a breaking block are drawn from the pictures of the world, stage "
                            + stage + " is missing");
        }
    }

    /** Keeps the test from passing on an empty list of pictures. */
    @Test
    void theWorldHoldsThePicturesOfEveryBlock() {
        assertFalse(worldPictures().isEmpty(), "no picture of the game was collected");
    }
}
