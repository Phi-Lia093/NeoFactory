package com.philia093.neofactory.block;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the six faces of a cube and the way they lie in space.
 * <p>
 * A face is not a label: the mesher places the four corners of the picture it carries by it, and a
 * graphics card only keeps a triangle when its corners run counter clockwise as seen from outside.
 * A face whose corners run the other way would disappear the moment the game turns the culling on,
 * so the winding is checked here - in pure arithmetic, without a window.
 */
class BlockFaceTest {

    @Test
    void theSixFacesPointAwayFromTheBlockAlongOneAxisEach() {
        assertEquals(6, BlockFace.ALL.length);
        for (BlockFace face : BlockFace.ALL) {
            int steps = Math.abs(face.x()) + Math.abs(face.y()) + Math.abs(face.z());
            assertEquals(1, steps, face + " points along more than one axis");
            assertEquals(face.axis(), face.x() != 0 ? 0 : face.y() != 0 ? 1 : 2, face.toString());
            assertEquals(face.axis() == 1, face.isVertical(), face.toString());
        }
    }

    @Test
    void everyFaceHasTheFaceThatLooksBackOppositeOfIt() {
        for (BlockFace face : BlockFace.ALL) {
            BlockFace other = face.opposite();
            assertEquals(face, other.opposite(), face + " does not look back at itself");
            assertEquals(-face.x(), other.x(), face.toString());
            assertEquals(-face.y(), other.y(), face.toString());
            assertEquals(-face.z(), other.z(), face.toString());
            assertFalse(face == other, face + " is its own opposite");
        }
    }

    @Test
    void aFaceCatchesTheLightTheOriginalGameGivesIt() {
        assertEquals(1.0f, BlockFace.TOP.shade());
        assertEquals(0.8f, BlockFace.NORTH.shade());
        assertEquals(0.8f, BlockFace.SOUTH.shade());
        assertEquals(0.6f, BlockFace.WEST.shade());
        assertEquals(0.6f, BlockFace.EAST.shade());
        assertEquals(0.5f, BlockFace.BOTTOM.shade());
    }

    @Test
    void theCornersOfAFaceRunCounterClockwiseSeenFromOutside() {
        for (BlockFace face : BlockFace.ALL) {
            // The two edges that leave the first corner of the face span it, so the cross product
            // of the first and the last edge is the normal the winding claims.
            int[] first = {face.cornerX(1) - face.cornerX(0), face.cornerY(1) - face.cornerY(0),
                    face.cornerZ(1) - face.cornerZ(0)};
            int[] last = {face.cornerX(3) - face.cornerX(0), face.cornerY(3) - face.cornerY(0),
                    face.cornerZ(3) - face.cornerZ(0)};
            assertEquals(face.x(), crossX(first, last),
                    face + " winds the wrong way around its X axis");
            assertEquals(face.y(), crossY(first, last),
                    face + " winds the wrong way around its Y axis");
            assertEquals(face.z(), crossZ(first, last),
                    face + " winds the wrong way around its Z axis");
        }
    }

    @Test
    void everyCornerOfAFaceIsACornerOfTheUnitCube() {
        for (BlockFace face : BlockFace.ALL) {
            for (int corner = 0; corner < 4; corner++) {
                for (int coordinate : new int[] {face.cornerX(corner), face.cornerY(corner),
                        face.cornerZ(corner)}) {
                    assertTrue(coordinate == 0 || coordinate == 1,
                            face + " corner " + corner + " is not a corner of the cube: "
                                    + coordinate);
                }
            }
        }
    }

    @Test
    void aCornerThatIsNotOneOfTheFourIsRefused() {
        for (BlockFace face : BlockFace.ALL) {
            assertThrows(IndexOutOfBoundsException.class, () -> face.cornerX(-1), face.toString());
            assertThrows(IndexOutOfBoundsException.class, () -> face.cornerY(4), face.toString());
        }
    }

    @Test
    void aFaceIsFoundByItsName() {
        for (BlockFace face : BlockFace.ALL) {
            assertEquals(face, BlockFace.byName(face.toString()));
            assertEquals(face, BlockFace.byName(face.toString().toUpperCase(Locale.ROOT)));
            assertEquals(face, BlockFace.byName(" " + face + " "));
        }
        assertNull(BlockFace.byName("front"));
        assertNull(BlockFace.byName(null));
    }

    @Test
    void theFourSidesAreTheFacesThatAreNotUpOrDown() {
        assertEquals(4, BlockFace.SIDES.length);
        for (BlockFace face : BlockFace.SIDES) {
            assertFalse(face.isVertical(), face + " is not a side");
        }
    }

    private static int crossX(int[] a, int[] b) {
        return a[1] * b[2] - a[2] * b[1];
    }

    private static int crossY(int[] a, int[] b) {
        return a[2] * b[0] - a[0] * b[2];
    }

    private static int crossZ(int[] a, int[] b) {
        return a[0] * b[1] - a[1] * b[0];
    }
}
