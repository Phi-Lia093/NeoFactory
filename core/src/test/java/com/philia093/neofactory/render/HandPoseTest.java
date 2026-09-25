package com.philia093.neofactory.render;

import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemRegistry;
import com.philia093.neofactory.support.TestRegistries;
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
 * Checks the hand of a first-person view without a window.
 * <p>
 * A hand is drawn like a block of the world - the same shader, the same vertex layout, the same mesher -
 * so everything but the upload can be read back here: the place the pose puts the hand in, and the region
 * of the skin a body is cut from. The cases guard the art as well as the place: a hand out of the picture
 * is drawn and never seen, and the skin of a body has no file of its own for the arm - a check that only
 * looks for one drops the arm out of the array, which is what happened once.
 */
class HandPoseTest {

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
    void theHandIsARegionOfTheSkinAndNotAFileOfItsOwn() {
        Path assets = Path.of("..", "assets");
        assertTrue(Files.isRegularFile(assets.resolve(BlockPictures.SKIN)),
                "the skin of a body is missing: " + BlockPictures.SKIN);
        assertFalse(Files.exists(assets.resolve(BlockPictures.path(BlockPictures.HAND))),
                "the hand has no file of its own - it is a region of the skin - so a check that only "
                        + "looks for a file of its own name drops the arm out of the array");
    }

    @Test
    void everyItemHasAPictureOfItsOwn() {
        TestRegistries.ensure();
        Path assets = Path.of("..", "assets");
        List<String> missing = new ArrayList<>();
        for (Item item : ItemRegistry.all()) {
            if (item.block() != null || item.texture().isEmpty()) {
                // A block is drawn as the cube of that block, so it needs no picture of an item.
                continue;
            }
            if (!Files.isRegularFile(assets.resolve(BlockPictures.path(BlockPictures.itemPicture(item))))
                    && !Files.isRegularFile(assets.resolve(BlockPictures.path(item.texture())))) {
                missing.add(item.name());
            }
        }
        assertTrue(missing.isEmpty(),
                "these items are nowhere to be seen, their picture is nowhere: " + missing);
    }
}
