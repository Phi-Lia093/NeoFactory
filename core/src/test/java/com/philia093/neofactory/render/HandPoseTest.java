package com.philia093.neofactory.render;

import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.support.TestRegistries;
import com.philia093.neofactory.util.Constants;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void theArmStandsInThePictureOfTheEye() {
        // The cone the eye of the game sees: the field of view across the vertical axis and a window of
        // sixteen to nine. The hand is placed for that cone, so the far end of the arm has to be in it -
        // a hand that hangs below the picture is drawn and never seen, which is what this case is for.
        float halfHeight = (float) Math.tan(Math.toRadians(Constants.VIEW_FIELD_OF_VIEW) * 0.5);
        float halfWidth = halfHeight * 16.0f / 9.0f;
        HandPose pose = HandPose.rest();
        MeshData arm = HandMeshes.arm(0);

        int onScreen = 0;
        for (int corner = 0; corner < arm.vertexCount(); corner++) {
            float[] vertex = vertexOf(arm, corner);
            // The renderer places the mesh with the inverse of the view times the pose, which for a hand
            // at rest is the same as adding the place of the pose to the corner, and the hand hangs in
            // front of the eye along the negative Z axis.
            float x = vertex[0] + pose.x();
            float y = vertex[1] + pose.y();
            float depth = -(vertex[2] + pose.z());
            if (depth > 0.0f && Math.abs(x) <= depth * halfWidth && Math.abs(y) <= depth * halfHeight) {
                onScreen++;
            }
        }
        assertTrue(onScreen >= 4, "the far end of the arm has to stand in the picture of the eye, "
                + onScreen + " of " + arm.vertexCount() + " corners are in it: " + pose);
    }

    @Test
    void aHeldToolIsACardThatFacesTheEye() {
        MeshData card = HandMeshes.flatItem(4, 1.0f, 0.5f, 0.25f);

        assertEquals(4, card.vertexCount(), "a card is one quad");
        assertEquals(6, card.indexCount(), "two triangles");
        for (int corner = 0; corner < card.vertexCount(); corner++) {
            float[] vertex = vertexOf(card, corner);
            assertTrue(Math.abs(vertex[0]) <= HandMeshes.LENGTH * 0.5f, "the card is as wide as the arm");
            assertTrue(Math.abs(vertex[1]) <= HandMeshes.LENGTH * 0.5f, "and as tall");
            assertEquals(-HandMeshes.LENGTH, vertex[2], 1.0e-6f, "it stands at the end of the arm");
            assertEquals(4.0f, vertex[5], 1.0e-6f, "and shows the picture of the item");
            assertEquals(1.0f, vertex[6], 1.0e-6f, "the whole card carries the tint of the item");
            assertEquals(0.5f, vertex[7], 1.0e-6f);
            assertEquals(0.25f, vertex[8], 1.0e-6f);
        }
        // The two triangles of the quad, which is what makes the card a surface and not a line.
        assertEquals(card.vertexCount() * 3 / 2, card.indexCount());
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

    @Test
    void everyItemTheHandCanHoldHasAPicture() {
        TestRegistries.ensure();
        Path assets = Path.of("..", "assets");
        List<String> missing = new ArrayList<>();
        for (Item item : ItemRegistry.all()) {
            if (item.block() != null || item.texture().isEmpty()) {
                // A block is held as the cube of that block, so it needs no card of its own.
                continue;
            }
            if (!Files.isRegularFile(assets.resolve(BlockPictures.path(BlockPictures.itemPicture(item))))
                    && !Files.isRegularFile(assets.resolve(BlockPictures.path(item.texture())))) {
                missing.add(item.name());
            }
        }
        assertTrue(missing.isEmpty(), "no hand can show these items, their picture is nowhere: " + missing);
    }

    @Test
    void theHandIsARegionOfTheSkinAndNotAFileOfItsOwn() {
        Path assets = Path.of("..", "assets");
        assertTrue(Files.isRegularFile(assets.resolve(BlockPictures.SKIN)),
                "the skin of a body is missing: " + BlockPictures.SKIN);
        assertFalse(Files.exists(assets.resolve(BlockPictures.path(BlockPictures.HAND))),
                "the hand has no file of its own - it is a region of the skin - so a check that only "
                        + "looks for a file of its own name drops the arm out of the array");
    }

    /** The ten floats of one corner of a mesh. */
    private static float[] vertexOf(MeshData mesh, int corner) {
        float[] floats = new float[MeshData.FLOATS_PER_VERTEX];
        System.arraycopy(mesh.vertexFloats(), corner * MeshData.FLOATS_PER_VERTEX, floats, 0,
                MeshData.FLOATS_PER_VERTEX);
        return floats;
    }
}
