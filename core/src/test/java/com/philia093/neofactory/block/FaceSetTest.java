package com.philia093.neofactory.block;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the pictures of a block and the way a face picks one.
 * <p>
 * The point of a {@link FaceSet} is that a face falls back instead of being repeated: a block of one
 * picture is one line, a block whose top differs is two, and only the face that really differs names
 * its own. A fallback that resolves the wrong way would give a log its rings on the bark, which is
 * why the order is checked here rather than by looking at the world.
 */
class FaceSetTest {

    @Test
    void aSetOfOnePictureShowsItOnEveryFace() {
        FaceSet set = FaceSet.of("stone");

        assertTrue(set.isUniform());
        assertEquals("stone", set.uniformTexture());
        for (BlockFace face : BlockFace.ALL) {
            assertEquals("stone", set.face(face), face.toString());
            assertFalse(set.hasOverlay(face), face.toString());
        }
        assertEquals(List.of("stone"), set.names());
        assertFalse(set.isEmpty());
    }

    @Test
    void aFaceWithoutAPictureOfItsOwnTakesTheOneOfTheBlock() {
        FaceSet set = FaceSet.builder().all("grass_side").top("grass_top").bottom("dirt").build();

        assertEquals("grass_top", set.face(BlockFace.TOP));
        assertEquals("dirt", set.face(BlockFace.BOTTOM));
        for (BlockFace side : BlockFace.SIDES) {
            assertEquals("grass_side", set.face(side), side.toString());
        }
        assertFalse(set.isUniform());
        assertEquals(Block.NO_TEXTURE, set.uniformTexture());
        assertEquals(List.of("dirt", "grass_side", "grass_top"), set.names());
    }

    @Test
    void theSidesOfABlockMayBeNamedWhileItsTopDiffers() {
        FaceSet set = FaceSet.builder().all("log_oak_top").side("log_oak").build();

        assertEquals("log_oak_top", set.face(BlockFace.TOP));
        assertEquals("log_oak_top", set.face(BlockFace.BOTTOM));
        for (BlockFace side : BlockFace.SIDES) {
            assertEquals("log_oak", set.face(side), side.toString());
        }
    }

    @Test
    void aFaceThatNamesItsOwnPictureWinsOverItsSide() {
        FaceSet set = FaceSet.builder().all("furnace_side").top("furnace_top")
                .front("furnace_front_off").build();

        assertEquals("furnace_top", set.face(BlockFace.TOP));
        assertEquals("furnace_front_off", set.face(BlockFace.SOUTH));
        assertEquals("furnace_side", set.face(BlockFace.NORTH));
        assertEquals("furnace_side", set.face(BlockFace.EAST));
        assertEquals("furnace_side", set.face(BlockFace.BOTTOM));
    }

    @Test
    void aFaceMayCarryASecondLayerInItsOwnColours() {
        FaceSet set = FaceSet.builder().all("grass_side").top("grass_top")
                .sideOverlay("grass_side_overlay").build();

        for (BlockFace side : BlockFace.SIDES) {
            assertTrue(set.hasOverlay(side), side.toString());
            assertEquals("grass_side_overlay", set.overlay(side));
        }
        assertFalse(set.hasOverlay(BlockFace.TOP));
        assertFalse(set.hasOverlay(BlockFace.BOTTOM));
        assertEquals(Block.NO_TEXTURE, set.overlay(BlockFace.TOP));
        assertEquals(List.of("grass_side", "grass_side_overlay", "grass_top"), set.names());
    }

    @Test
    void anOverlayOfOneFaceStaysOnThatFace() {
        FaceSet set = FaceSet.builder().all("stonebrick")
                .overlay(BlockFace.TOP, "stonebrick_carved").build();

        assertTrue(set.hasOverlay(BlockFace.TOP));
        assertFalse(set.hasOverlay(BlockFace.NORTH));
        assertEquals(Block.NO_TEXTURE, set.overlay(BlockFace.SOUTH));
        assertEquals(List.of("stonebrick", "stonebrick_carved"), set.names());
    }

    @Test
    void aSetWithoutAPictureIsEmpty() {
        assertTrue(FaceSet.NONE.isEmpty());
        assertFalse(FaceSet.NONE.isUniform());
        assertEquals(Block.NO_TEXTURE, FaceSet.NONE.uniformTexture());
        assertTrue(FaceSet.NONE.names().isEmpty());
        assertFalse(FaceSet.of("stone").isEmpty());
    }

    @Test
    void aPictureNameThatIsNotABareNameIsRefusedWhereItIsWritten() {
        assertThrows(IllegalArgumentException.class, () -> FaceSet.of(""));
        assertThrows(IllegalArgumentException.class, () -> FaceSet.of("   "));
        assertThrows(IllegalArgumentException.class, () -> FaceSet.of("stone.png"));
        assertThrows(IllegalArgumentException.class, () -> FaceSet.of("blocks/stone"));
        assertThrows(IllegalArgumentException.class, () -> FaceSet.builder().top(""));
        assertThrows(IllegalArgumentException.class,
                () -> FaceSet.builder().overlay(BlockFace.TOP, "a.b"));
        assertThrows(NullPointerException.class, () -> FaceSet.builder().all(null));
        assertThrows(NullPointerException.class, () -> FaceSet.of("stone").face(null));
    }

    @Test
    void aNameIsTrimmedWhereItIsWritten() {
        assertEquals("stone", FaceSet.of("  stone  ").face(BlockFace.TOP));
    }

    @Test
    void aSetDescribesItself() {
        FaceSet set = FaceSet.builder().all("log_oak_top").side("log_oak").build();
        String text = set.toString();

        assertTrue(text.contains("top=log_oak_top"), text);
        assertTrue(text.contains("north=log_oak"), text);
        assertEquals("FaceSet()", FaceSet.NONE.toString());
    }
}
