package com.philia093.neofactory.render;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the arm of a first-person view without a window.
 * <p>
 * A hand is drawn like a block of the world - the same shader, the same vertex layout, the same mesher
 * rules - so everything but the upload can be read back here: the place the pose puts the hand in, the
 * shape it is meshed from and the region of the skin it shows. The last case is the one that guards the
 * art: the arm is cut out of the body picture at the spot the classic skin puts it, so a skin that is
 * packed differently fails here instead of showing a piece of a face as an arm.
 */
class HandPoseTest {

    /** Corner count of a box: six faces of four corners. */
    private static final int CORNERS = 24;

    /** Index count of a box: six faces of two triangles of three corners. */
    private static final int INDICES = 36;

    @Test
    void theHandRestsInTheLowerRightOfTheView() {
        HandPose pose = HandPose.rest();

        assertEquals(HandPose.REST_X, pose.x(), 1.0e-6f, "the hand hangs to the right of the middle");
        assertEquals(HandPose.REST_Y, pose.y(), 1.0e-6f, "and below it");
        assertEquals(HandPose.REST_Z, pose.z(), 1.0e-6f, "in front of the eye");
        assertTrue(pose.z() < 0.0f, "in front of the eye is the negative Z axis");
        assertEquals(0.0f, pose.pitch(), 1.0e-6f, "a hand at rest is not turned");
    }

    @Test
    void aSwingLiftsTheHandAndThrowsItForward() {
        HandPose middle = HandPose.of(0.5f, 0.0f);

        assertTrue(middle.y() > HandPose.REST_Y, "a swing lifts the arm: " + middle);
        assertTrue(middle.z() < HandPose.REST_Z, "and throws it into the picture: " + middle);
        assertTrue(middle.pitch() < 0.0f, "which turns it down: " + middle);
        assertTrue(middle.isSwinging(), "a hand in a swing is not at rest");

        // Both ends of the stroke are the rest of the hand again, which is what makes one hit one stroke.
        assertEquals(HandPose.REST_Y, HandPose.of(0.0f, 0.0f).y(), 1.0e-6f);
        assertEquals(HandPose.REST_Y, HandPose.of(1.0f, 0.0f).y(), 1.0e-6f);
    }

    @Test
    void walkingSwaysTheHandFromSideToSide() {
        HandPose first = HandPose.of(0.0f, 0.25f);
        HandPose second = HandPose.of(0.0f, 0.75f);
        HandPose half = HandPose.of(0.0f, 0.5f);

        assertTrue(first.x() > HandPose.REST_X, "the first step sways the hand outwards: " + first);
        assertTrue(second.x() < HandPose.REST_X, "and the second one back inwards: " + second);
        assertTrue(half.y() < HandPose.REST_Y,
                "half a step lowers the hand, which is the bob of a walk: " + half);
    }

    @Test
    void theArmIsABoxThatReachesIntoTheView() {
        MeshData mesh = HandMeshes.arm(7);

        assertEquals(CORNERS, mesh.vertexCount(), "an arm is a box of six faces");
        assertEquals(INDICES, mesh.indexCount(), "two triangles per face");
        for (int corner = 0; corner < mesh.vertexCount(); corner++) {
            float[] vertex = vertexOf(mesh, corner);
            assertTrue(Math.abs(vertex[0]) <= HandMeshes.WIDTH, "the arm stays inside its width");
            assertTrue(Math.abs(vertex[1]) <= HandMeshes.THICKNESS, "and inside its thickness");
            assertTrue(vertex[2] <= 0.0f && vertex[2] >= -HandMeshes.LENGTH,
                    "and reaches into the view: " + vertex[2]);
            assertEquals(7.0f, vertex[5], 1.0e-6f, "every corner asks for the layer of the hand");
            assertTrue(vertex[3] >= 0.0f && vertex[3] <= 1.0f, "the picture spot stays inside a layer");
            assertTrue(vertex[4] >= 0.0f && vertex[4] <= 1.0f, "on both axes");
        }
    }

    @Test
    void theSidesOfTheArmShowTheStripOfTheSkin() {
        MeshData mesh = HandMeshes.arm(0);

        // The four sides of the arm show the strip of four by twelve pixels of the skin and its two ends
        // the square of four by four, so the strip covers four faces of four corners and the square two.
        int strip = 0;
        int square = 0;
        for (int corner = 0; corner < mesh.vertexCount(); corner++) {
            float[] vertex = vertexOf(mesh, corner);
            if (vertex[4] > 4.0f / 16.0f) {
                strip++;
            } else {
                square++;
            }
        }
        assertEquals(16, strip, "the four sides of the arm are twelve pixels long");
        assertEquals(8, square, "and its two ends are the square of four by four");
    }

    @Test
    void theSkinHoldsAnArmWhereTheHandLooksForIt() throws Exception {
        Path skin = Path.of("..", "assets").resolve(BlockPictures.SKIN);
        assertTrue(Files.isRegularFile(skin), "the skin of a body is missing: " + skin);

        try (InputStream file = Files.newInputStream(skin)) {
            BufferedImage picture = ImageIO.read(file);
            assertNotNull(picture, "the skin of a body has to be a readable picture");

            int[] region = BlockPictures.ARM_REGION;
            assertTrue(region[0] + region[2] <= picture.getWidth()
                            && region[1] + region[3] <= picture.getHeight(),
                    "the arm has to fit into the skin: " + region[0] + ", " + region[1]);

            int painted = 0;
            for (int y = region[1]; y < region[1] + region[3]; y++) {
                for (int x = region[0]; x < region[0] + region[2]; x++) {
                    if ((picture.getRGB(x, y) >>> 24) > 0) {
                        painted++;
                    }
                }
            }
            assertNotEquals(0, painted, "the region of the arm holds no pixel of the skin at all");
        }
    }

    /** The ten floats of one corner of a mesh. */
    private static float[] vertexOf(MeshData mesh, int corner) {
        float[] floats = new float[MeshData.FLOATS_PER_VERTEX];
        System.arraycopy(mesh.vertexFloats(), corner * MeshData.FLOATS_PER_VERTEX, floats, 0,
                MeshData.FLOATS_PER_VERTEX);
        return floats;
    }
}
