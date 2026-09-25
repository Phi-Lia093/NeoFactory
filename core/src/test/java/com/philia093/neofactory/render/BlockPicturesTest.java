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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        return BlockPictures.pictureNames(new ObjectMap<>(), name -> {
            if (BlockPictures.HAND.equals(name) || SkinRegions.isSkinFace(name)) {
                // A piece cut out of the skin of a body and no file of its own: no cube shows a body, the
                // hand of a view is drawn by the renderer of the figure.
                return false;
            }
            return Files.isRegularFile(ASSETS.resolve(BlockPictures.path(name)));
        });
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
