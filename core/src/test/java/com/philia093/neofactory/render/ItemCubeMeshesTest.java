package com.philia093.neofactory.render;

import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the little body a dropped item that is no block is drawn as.
 * <p>
 * Such an item has no cube of a block to show, so it is drawn as one upright board of its own picture, see
 * {@link ItemCubeMeshes#board(int)}. How a board <i>looks</i> cannot be checked here, because a mesh needs a
 * graphics card; the numbers it is built from can: the room it takes inside a block, the one pixel of
 * thickness it is given so that it cannot turn into nothing, the way its picture lies on it and the winding
 * of every one of its faces, which is what decides whether the world draws that face or culls it.
 */
class ItemCubeMeshesTest {

    /** Layer the boards are built with, a layer the cases here only read back. */
    private static final int LAYER = 7;

    /** Offset of the texture coordinate across the picture inside a corner. */
    private static final int U = 3;

    /** Offset of the texture coordinate up the picture inside a corner. */
    private static final int V = 4;

    /** Offset of the picture layer inside a corner. */
    private static final int PICTURE = 5;

    /** Thickness of the board in the room of a block: one pixel of a picture of sixteen pixels. */
    private static final float THICKNESS = 1.0f / Constants.ITEM_ICON_SIZE;

    /** Distance the board reaches to either side of the middle of the block. */
    private static final float HALF = THICKNESS * 0.5f;

    /** Middle of the first pixel of the picture across or up, which is the pixel a rim shows. */
    private static final float FIRST_PIXEL = 0.5f / Constants.ITEM_ICON_SIZE;

    /** Middle of the last pixel of the picture, the one the opposite rim shows. */
    private static final float LAST_PIXEL = 1.0f - FIRST_PIXEL;

    @Test
    void theBoardOfAnItemStandsInTheRoomOfOneBlock() {
        MeshData board = ItemCubeMeshes.board(LAYER);
        float[] vertex = board.vertexFloats();

        assertEquals(24, board.vertexCount(), "six faces of four corners");
        assertEquals(36, board.indexCount(), "and two triangles per face");
        for (int corner = 0; corner < board.vertexCount(); corner++) {
            int base = at(corner);
            for (int axis = 0; axis < 3; axis++) {
                float coordinate = vertex[base + axis];
                assertTrue(coordinate >= 0.0f && coordinate <= 1.0f,
                        "corner " + corner + " reaches out of the block it stands in: " + coordinate);
            }
            assertEquals(LAYER, vertex[base + PICTURE], 1.0e-6f,
                    "corner " + corner + " shows the picture the board was built with");
            assertTrue(vertex[base + U] >= 0.0f && vertex[base + U] <= 1.0f
                            && vertex[base + V] >= 0.0f && vertex[base + V] <= 1.0f,
                    "corner " + corner + " reaches out of the picture of the item");
        }
    }

    @Test
    void theBoardIsOnePixelOfThePictureThick() {
        MeshData board = ItemCubeMeshes.board(LAYER);
        float[] vertex = board.vertexFloats();
        int near = 0;
        int far = 0;

        for (int corner = 0; corner < board.vertexCount(); corner++) {
            float depth = vertex[at(corner) + 2];
            if (depth < 0.5f) {
                near++;
                assertEquals(0.5f - HALF, depth, 1.0e-6f,
                        "the board reaches half of its thickness behind the middle of the block");
            } else {
                far++;
                assertEquals(0.5f + HALF, depth, 1.0e-6f, "and half of it in front of it");
            }
        }

        assertEquals(12, near, "a large face and two rim corners of every rim lie on one side");
        assertEquals(12, far, "and the same amount on the other one");
        assertEquals(THICKNESS, 2.0f * HALF, 1.0e-6f, "the whole board is one pixel of the picture thick");
        // The picture covers a square of the size of the item, so one pixel of it is a sixteenth of that size
        // in the world as well: the board of a sword is a square of {@code ItemCubeMeshes.SIZE} across and a
        // sixteenth of it thick.
        assertEquals(ItemCubeMeshes.SIZE / Constants.ITEM_ICON_SIZE, THICKNESS * ItemCubeMeshes.SIZE,
                1.0e-6f, "a sixteenth of the size the picture is drawn in");
    }

    @Test
    void thePictureLiesUprightOnBothLargeFaces() {
        MeshData board = ItemCubeMeshes.board(LAYER);
        float[] vertex = board.vertexFloats();
        int corners = 0;

        for (int corner = 0; corner < board.vertexCount(); corner++) {
            int base = at(corner);
            float u = vertex[base + U];
            float v = vertex[base + V];
            if (u != 0.0f && u != 1.0f || v != 0.0f && v != 1.0f) {
                continue; // A rim, which shows one row or one column of the picture all along.
            }
            corners++;
            float height = vertex[base + 1];
            assertTrue(height == 0.0f || height == 1.0f,
                    "a large face has a bottom edge and a top edge and nothing between them");
            // A picture is read downwards from its first row, so that first row - V = 0 - lies along the top
            // edge of the board: that is what keeps a tool standing on its handle instead of hanging from its
            // head, and it holds for the face in front and for the one behind it.
            assertEquals(height == 0.0f ? 1.0f : 0.0f, v, 1.0e-6f,
                    "the corner at height " + height + " shows the wrong row of the picture");
        }

        assertEquals(8, corners, "both large faces of the board were looked at");
    }

    @Test
    void theBackOfTheBoardShowsThePictureMirrored() {
        MeshData board = ItemCubeMeshes.board(LAYER);
        float[] vertex = board.vertexFloats();
        int front = 0;
        int back = 0;

        for (int corner = 0; corner < board.vertexCount(); corner++) {
            int base = at(corner);
            float u = vertex[base + U];
            float v = vertex[base + V];
            if (u != 0.0f && u != 1.0f || v != 0.0f && v != 1.0f) {
                continue; // A rim, which is no face of the item.
            }
            boolean lowX = vertex[base] == 0.0f;
            boolean inFront = vertex[base + 2] > 0.5f;
            if (inFront) {
                front++;
            } else {
                back++;
            }
            // Both faces run with the X axis, so the face behind shows the picture the way it comes through
            // the board: mirrored. That is what makes the turn of an item a whole turn - the blade of a sword
            // points to one side at the start, away at every quarter and to the other side halfway through -
            // instead of a board that keeps looking the same after half of its turn, which reads as a quarter
            // of a turn that jumps back.
            assertEquals(lowX, u == 0.0f,
                    "the picture runs the wrong way on the " + (inFront ? "front" : "back") + " face");
        }

        assertEquals(4, front, "the face the item is read from was looked at");
        assertEquals(4, back, "and the face behind it");
    }

    @Test
    void theRimsShowTheEdgeOfThePicture() {
        MeshData board = ItemCubeMeshes.board(LAYER);
        float[] vertex = board.vertexFloats();
        int west = 0;
        int east = 0;
        int top = 0;
        int bottom = 0;

        for (int corner = 0; corner < board.vertexCount(); corner++) {
            int base = at(corner);
            if (vertex[base + U] == FIRST_PIXEL) {
                west++;
            }
            if (vertex[base + U] == LAST_PIXEL) {
                east++;
            }
            if (vertex[base + V] == FIRST_PIXEL) {
                top++;
            }
            if (vertex[base + V] == LAST_PIXEL) {
                bottom++;
            }
        }

        // A rim is one pixel of the picture deep, so it shows the very column or row of the picture that
        // meets it - the edge of a picture that was cut out and stood up - instead of a squeezed picture.
        assertEquals(4, west, "the western rim shows the first column of the picture");
        assertEquals(4, east, "the eastern one the last column");
        assertEquals(4, top, "the rim along the top shows the first row");
        assertEquals(4, bottom, "and the one along the bottom the last row");
    }

    @Test
    void theBoardIsAClosedBodyOfFacesTurnedOutwards() {
        MeshData board = ItemCubeMeshes.board(LAYER);
        int large = 0;
        int rim = 0;

        assertEquals(12, board.indexCount() / 3, "six faces of two triangles");
        for (int triangle = 0; triangle < board.indexCount() / 3; triangle++) {
            float[] normal = normalOf(board, triangle);
            float size = (float) Math.sqrt(normal[0] * normal[0] + normal[1] * normal[1]
                    + normal[2] * normal[2]);
            float[] middle = middleOf(board, triangle);
            // The middle of a triangle of the board lies away from the middle of the block, and the normal it
            // is wound around has to point the same way: a face that is wound towards the inside is a face the
            // world culls, which is a board a player looks right through.
            float out = normal[0] * (middle[0] - 0.5f) + normal[1] * (middle[1] - 0.5f)
                    + normal[2] * (middle[2] - 0.5f);
            assertTrue(out > 0.0f, "triangle " + triangle + " is wound towards the inside of the board");
            if (size > 0.5f) {
                large++;
                assertEquals(1.0f, size, 1.0e-5f, "half of a large face of the board");
            } else {
                rim++;
                assertEquals(THICKNESS, size, 1.0e-5f, "half of a rim, which is one pixel deep");
            }
        }

        assertEquals(4, large, "the two large faces carry the picture");
        assertEquals(8, rim, "and the four rims close the board around them");
    }

    /**
     * The normal of one triangle of a mesh, as long as twice the area of that triangle.
     * <p>
     * The normal is read off the order the three corners are walked in, which is the winding a graphics card
     * keeps: the cross product of the first two edges of a triangle points out of the side that is drawn and
     * into the side that is culled.
     *
     * @param mesh mesh to read
     * @param triangle triangle to read, counted from the first index
     * @return three floats, the normal of that triangle
     */
    private static float[] normalOf(MeshData mesh, int triangle) {
        short[] indices = mesh.indexShorts();
        float[] vertex = mesh.vertexFloats();
        int a = at(indices[triangle * 3]);
        int b = at(indices[triangle * 3 + 1]);
        int c = at(indices[triangle * 3 + 2]);
        float abx = vertex[b] - vertex[a];
        float aby = vertex[b + 1] - vertex[a + 1];
        float abz = vertex[b + 2] - vertex[a + 2];
        float acx = vertex[c] - vertex[a];
        float acy = vertex[c + 1] - vertex[a + 1];
        float acz = vertex[c + 2] - vertex[a + 2];
        return new float[] {
            aby * acz - abz * acy,
            abz * acx - abx * acz,
            abx * acy - aby * acx,
        };
    }

    /**
     * The middle of one triangle of a mesh.
     *
     * @param mesh mesh to read
     * @param triangle triangle to read, counted from the first index
     * @return three floats, the X, Y and Z of the middle of that triangle
     */
    private static float[] middleOf(MeshData mesh, int triangle) {
        short[] indices = mesh.indexShorts();
        float[] vertex = mesh.vertexFloats();
        float[] middle = new float[3];
        for (int corner = 0; corner < 3; corner++) {
            int base = at(indices[triangle * 3 + corner]);
            for (int axis = 0; axis < 3; axis++) {
                middle[axis] += vertex[base + axis] / 3.0f;
            }
        }
        return middle;
    }

    /** Index of the first float of one corner inside {@link MeshData#vertexFloats()}. */
    private static int at(int corner) {
        return corner * MeshData.FLOATS_PER_VERTEX;
    }
}
