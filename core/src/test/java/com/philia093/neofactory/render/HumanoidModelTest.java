package com.philia093.neofactory.render;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.render.model.Bone;
import com.philia093.neofactory.render.model.HumanoidModel;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the body the player is drawn with, without opening a window.
 * <p>
 * Two things are worth checking here and nowhere else. The first is the cut of the skin: a face of a
 * body is a rectangle of {@code entity/steve.png}, and a rectangle that is off by a few pixels shows
 * up as a shirt on a head or a face on a shoulder, which is hard to find while playing. The second is
 * the figure: the boxes have to form a body of two blocks, with the head above the shoulders, the arms
 * beside them and the legs below, and the arm a view from inside the body shows has to be the very box
 * the skin draws for an arm.
 */
class HumanoidModelTest {

    @Test
    void aFaceOfTheHeadIsCutWhereTheSkinHoldsIt() {
        // The head of the classic skin starts at 0, 0 with a box of eight on every side, so its top is
        // the square from 8, 0 to 16, 8 and its face the one from 8, 8 to 16, 16.
        assertRect("head", BlockFace.TOP, 8, 0, 8, 8);
        assertRect("head", BlockFace.BOTTOM, 16, 0, 8, 8);
        assertRect("head", BlockFace.EAST, 0, 8, 8, 8);
        assertRect("head", BlockFace.NORTH, 8, 8, 8, 8);
        assertRect("head", BlockFace.WEST, 16, 8, 8, 8);
        assertRect("head", BlockFace.SOUTH, 24, 8, 8, 8);
    }

    @Test
    void aFaceOfAnArmIsCutWhereTheSkinHoldsIt() {
        // The arm starts at 40, 16 with a box of four across, twelve tall and four deep, which is the
        // very region the hand of a view was cut from before, see SkinRegions.
        assertRect("arm_right", BlockFace.TOP, 44, 16, 4, 4);
        assertRect("arm_right", BlockFace.BOTTOM, 48, 16, 4, 4);
        assertRect("arm_right", BlockFace.NORTH, 44, 20, 4, 12);
        assertRect("arm_right", BlockFace.SOUTH, 52, 20, 4, 12);
    }

    @Test
    void everyFaceOfEveryBoneHasItsOwnPicture() {
        Set<String> names = new HashSet<>(SkinRegions.names());

        assertEquals(SkinRegions.bones().size() * BlockFace.ALL.length, SkinRegions.names().size(),
                "a face of every bone is one picture of the array");
        assertEquals(names.size(), SkinRegions.names().size(), "and no name is used twice");
        assertTrue(SkinRegions.isSkinFace(SkinRegions.name("leg_left", BlockFace.TOP)),
                "a face of a body is recognised by its name");
    }

    /** Checks the rectangle one face of a bone is cut from. */
    private static void assertRect(String bone, BlockFace face, int x, int y, int width,
            int height) {
        int[] rect = SkinRegions.rect(bone, face);
        assertEquals(x, rect[0], bone + " " + face + " starts at the wrong column");
        assertEquals(y, rect[1], bone + " " + face + " starts at the wrong row");
        assertEquals(width, rect[2], bone + " " + face + " is the wrong width");
        assertEquals(height, rect[3], bone + " " + face + " is the wrong height");
    }

    @Test
    void theBoxesFormABodyOfTwoBlocks() {
        HumanoidModel model = HumanoidModel.of();

        assertEquals(6, model.bones().size(), "a head, a body, two arms and two legs");
        assertEquals(HumanoidModel.HEIGHT, 2.0f, 1.0e-6f, "a figure of cubes is two blocks tall");
        assertEquals(HumanoidModel.SCALE, 0.9375f, 1.0e-6f,
                "and is drawn at the height a player really fills");

        Bone head = model.bone(HumanoidModel.HEAD);
        Bone body = model.bone(HumanoidModel.BODY);
        Bone leg = model.bone(HumanoidModel.LEG_RIGHT);
        assertEquals(1.5f, head.fromY(), 1.0e-6f, "the head stands on the shoulders");
        assertEquals(2.0f, head.toY(), 1.0e-6f, "and reaches the top of the figure");
        assertEquals(head.fromY(), body.toY(), 1.0e-6f, "the body ends where the head begins");
        assertEquals(0.0f, leg.fromY(), 1.0e-6f, "the legs stand on the ground");
        assertEquals(body.fromY(), leg.toY(), 1.0e-6f, "and end where the body begins");
    }

    @Test
    void theArmOfAViewIsTheArmOfTheSkin() {
        HumanoidModel model = HumanoidModel.of();
        Bone arm = model.bone(HumanoidModel.ARM_RIGHT);

        assertEquals(0.25f, arm.toX() - arm.fromX(), 1.0e-6f,
                "an arm of a classic skin is four pixels across");
        assertEquals(0.75f, arm.toY() - arm.fromY(), 1.0e-6f, "and twelve pixels long");
        assertTrue(arm.fromX() < 0.0f, "the right arm hangs on the west side of the body");
        assertEquals(1.375f, arm.pivotY(), 1.0e-6f, "and swings from the shoulder");
        for (BlockFace face : BlockFace.ALL) {
            assertNotNull(arm.faces().stream().filter(one -> one.face() == face).findFirst()
                    .orElse(null), "the arm draws its face " + face);
        }
    }

    @Test
    void aBoneIsMeshedOnceFacePerPicture() {
        HumanoidModel model = HumanoidModel.of();

        MeshData mesh = model.bone(HumanoidModel.ARM_RIGHT).build(picture -> 1);

        assertEquals(6 * 4, mesh.vertexCount(), "a box shows four corners per face");
        float[] vertices = mesh.vertexFloats();
        for (int corner = 0; corner < mesh.vertexCount(); corner++) {
            int base = corner * MeshData.FLOATS_PER_VERTEX;
            assertEquals(1.0f, vertices[base + 5], 1.0e-6f, "every face asks for the layer it is in");
            assertTrue(vertices[base + 3] >= 0.0f && vertices[base + 3] <= 1.0f,
                    "and reads a window inside that picture");
            assertTrue(vertices[base + 4] >= 0.0f && vertices[base + 4] <= 1.0f);
        }
    }
}
