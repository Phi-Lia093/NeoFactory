package com.philia093.neofactory.world.interaction;

import com.badlogic.gdx.math.Vector3;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.World;

/**
 * Picks the cell a break or a build action would touch.
 * <p>
 * The eyes of the player decide: a ray leaves them along the direction of the view, and the first cell it
 * enters that holds a block is the target, together with the face the ray entered through - which is what
 * a block is built against, see {@link BlockTarget#face()}.
 * <p>
 * Aiming further than {@link Constants#PLAYER_REACH} leaves the player without a target at all: the ray
 * stops at the reach, so a cell far away is neither broken nor built on, and the cell the eyes meet is
 * always close enough to be touched.
 * <p>
 * <b>The line of sight is the only aim of the game.</b> The view from above is gone and nothing here
 * remembers it: a mouse position used to pick a cell and a named layer used to say which height the
 * action worked at, and neither is left. A body that stands in the world names a cell with its eyes, and
 * the height of the view is part of that aim - standing on a mountain and looking down reaches a cell the
 * flat view could never name.
 */
public final class BlockTargeting {

    /**
     * Distance in blocks the ray starts in front of the eyes.
     * <p>
     * A ray that starts exactly at the eyes would meet the cell they stand in, which holds the body, and
     * would stop there instead of at the block the player looks at.
     */
    private static final float START_AHEAD = 0.05f;

    private BlockTargeting() {
        // Utility class: never instantiated.
    }

    /**
     * Finds the cell the eyes of the player meet, the way a body that stands in the world aims.
     * <p>
     * The ray leaves the eyes of the player along the direction of the view - yaw and pitch, see
     * {@link Player#lookDirection(Vector3)} - and the first cell that holds a block is what the player
     * looks at, together with the face the ray entered it through, which is what a block is built
     * against. The height of the view is therefore part of the aim: standing on a mountain and looking
     * down reaches a cell the flat view could never name.
     * <p>
     * The ray starts a little in front of the eyes, because the cell the player stands in holds the
     * ground below their feet and would otherwise be what the ray meets first.
     *
     * @param world world holding the blocks
     * @param player player the ray starts at and the reach belongs to
     * @param direction direction of the view, normalized
     * @return the cell the eyes meet, or {@code null} when nothing is within reach
     */
    public static BlockTarget selectInSight(World world, Player player, Vector3 direction) {
        float eyeY = player.position().y + Constants.PLAYER_EYE_HEIGHT;
        BlockRay.Hit hit = BlockRay.cast(world,
                player.position().x + direction.x * START_AHEAD, eyeY + direction.y * START_AHEAD,
                player.position().z + direction.z * START_AHEAD,
                direction.x, direction.y, direction.z, Constants.PLAYER_REACH,
                (access, x, y, z) -> !access.getBlock(x, y, z).isAir());
        return hit == null ? null : BlockTarget.of(hit);
    }
}
