package com.philia093.neofactory.render;

import com.badlogic.gdx.math.Vector3;
import com.philia093.neofactory.block.BlockFace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the corner a block item is looked at from when its icon is baked.
 * <p>
 * A slot shows a small cube of the block, and the corner the eye stands in decides which of its faces a
 * player sees. <b>A block draws the picture it wants to be known by on its north side</b> - the mouth of a
 * furnace, the front of a machine, the door of a boiler - so the eye has to stand north of the cube: with it
 * south every machine of an age, built of the same casing, looked like every other one in the inventory,
 * which is the bug this test keeps from coming back, see {@code BlockIconRenderer#DIRECTION}.
 * <p>
 * The camera a drawing is made through needs the native library of libGDX, so what is checked here is the
 * corner itself - the arithmetic the camera is placed with, see {@code BlockIconRenderer#eyeDirection()}.
 */
class BlockIconCameraTest {

    @Test
    void theIconOfABlockIsLookedAtFromTheCornerOfItsFront() {
        Vector3 eye = BlockIconRenderer.eyeDirection();

        assertTrue(eye.y > 0, "the eye stands above the cube, so its top reads as a top face");
        assertEquals(BlockFace.NORTH.z(), (int) Math.signum(eye.z),
                "the eye stands north of the cube, where the face a block draws its picture on is turned");
        assertEquals(BlockFace.EAST.x(), (int) Math.signum(eye.x),
                "and beside it, so one flank of the cube is seen as well");
        assertTrue(eye.len() > 0.0f, "and it is a direction and not the middle of the cube");
    }
}
