package com.philia093.neofactory.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the cracks drawn over a block that is being broken.
 * <p>
 * How the cracks look cannot be checked here, because a mesh needs a graphics card; the numbers they are
 * built from can: the room they take around a cell, the stage a break has reached, the winding of every face
 * and the way a face reads its picture. That is what keeps the cracks from being invisible, from hanging
 * inside the block they cover or from lying on their side.
 */
class BreakOverlayTest {

    /** Layer the overlay is built with, a layer the cases here only read back. */
    private static final int LAYER = 3;

    /** Distance the overlay stands outside the cell, the value {@link BreakOverlay} uses. */
    private static final float OUTSIDE = 0.002f;

    /** Offset of the picture layer inside a corner of a mesh. */
    private static final int PICTURE = 5;

    @Test
    void theStageGrowsWithTheBreak() {
        assertEquals(-1, BreakOverlay.stageOf(0.0f), "nothing is broken at the start");
        assertEquals(-1, BreakOverlay.stageOf(-0.5f), "and a break cannot be behind its start");
        assertEquals(0, BreakOverlay.stageOf(0.01f), "the first chip shows as soon as the break runs");
        assertEquals(BreakOverlay.STAGES - 1, BreakOverlay.stageOf(1.0f), "the last one closes the break");

        int previous = -1;
        for (float progress = 0.0f; progress <= 1.0f; progress += 0.001f) {
            int stage = BreakOverlay.stageOf(progress);
            assertTrue(stage >= -1 && stage < BreakOverlay.STAGES,
                    "the stage of " + progress + " is inside the art: " + stage);
            assertTrue(stage >= previous, "the stage grows with the break, it fell at " + progress);
            previous = stage;
        }
    }

    @Test
    void theOverlayStandsAroundTheCellItCovers() {
        MeshData overlay = BreakOverlay.overlay(LAYER);

        assertEquals(24, overlay.vertexCount(), "six faces of four corners");
        assertEquals(36, overlay.indexCount(), "and two triangles per face");
        for (int corner = 0; corner < overlay.vertexCount(); corner++) {
            for (int axis = 0; axis < 3; axis++) {
                float coordinate = overlay.vertexFloats()[at(corner) + axis];
                assertTrue(coordinate == -OUTSIDE || coordinate == 1.0f + OUTSIDE,
                        "a corner of the overlay stands a hair outside the cell, not at " + coordinate);
            }
            assertEquals(LAYER, overlay.vertexFloats()[at(corner) + PICTURE], 1.0e-6f,
                    "corner " + corner + " shows the picture of the stage that was asked for");
        }
    }

    @Test
    void everyFaceOfTheOverlayIsTurnedOutwards() {
        MeshData overlay = BreakOverlay.overlay(LAYER);

        for (int triangle = 0; triangle < overlay.indexCount() / 3; triangle++) {
            float[] normal = normalOf(overlay, triangle);
            float[] middle = middleOf(overlay, triangle);
            float out = normal[0] * (middle[0] - 0.5f) + normal[1] * (middle[1] - 0.5f)
                    + normal[2] * (middle[2] - 0.5f);
            assertTrue(out > 0.0f, "triangle " + triangle + " is wound towards the inside of the cell, so a "
                    + "player would look right through the cracks");
        }
    }

    @Test
    void theCracksReadTheWayABlockReadsItsPictures() {
        MeshData overlay = BreakOverlay.overlay(LAYER);
        short[] indices = overlay.indexShorts();
        float[] vertex = overlay.vertexFloats();
        int corners = 0;

        for (int triangle = 0; triangle < overlay.indexCount() / 3; triangle++) {
            float[] normal = normalOf(overlay, triangle);
            for (int corner = 0; corner < 3; corner++) {
                int base = at(indices[triangle * 3 + corner]);
                float x = cell(vertex[base]);
                float y = cell(vertex[base + 1]);
                float z = cell(vertex[base + 2]);
                float u = vertex[base + 3];
                float v = vertex[base + 4];
                if (normal[0] != 0.0f) {
                    // A face along X: the east one counts the picture against the Z axis, which is the way a
                    // reader at the east side of the cell reads it, and the west one counts with it.
                    assertTrue(matched(normal[0] > 0.0f ? 1.0f - z : z, u),
                            "the picture of a face along X runs the wrong way across it");
                    assertTrue(matched(1.0f - y, v), "and the wrong way down it");
                } else if (normal[2] != 0.0f) {
                    assertTrue(matched(normal[2] > 0.0f ? x : 1.0f - x, u),
                            "the picture of a face along Z runs the wrong way across it");
                    assertTrue(matched(1.0f - y, v), "and the wrong way down it");
                } else {
                    // A face that looks up or down lies as the picture lies, see FaceLayout.
                    assertTrue(matched(x, u), "a face that looks up or down lies on its side");
                    assertTrue(matched(1.0f - z, v), "and its rows run the wrong way");
                }
                corners++;
            }
        }

        assertEquals(36, corners, "every corner of the overlay was looked at");
    }

    /** {@code true} when a coordinate of a picture is the one a corner of the cell asks for. */
    private static boolean matched(float expected, float actual) {
        return Math.abs(expected - actual) < 1.0e-6f;
    }

    /** Position of a corner inside the cell, undoing the hair the overlay stands outside it. */
    private static float cell(float coordinate) {
        return coordinate <= 0.0f ? 0.0f : 1.0f;
    }

    /**
     * The normal of one triangle of a mesh, read off the order its corners are walked in.
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
