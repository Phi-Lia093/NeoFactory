package com.philia093.neofactory.world.interaction;

import com.badlogic.gdx.math.MathUtils;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.World;

/**
 * Picks the cell a break or a build action would touch.
 * <p>
 * The mouse decides, as long as the cell under it is close enough to the player: that is the cell the
 * player points at, and reach is what stops the player from editing the whole landscape at once.
 * <p>
 * Aiming further away than {@link Constants#PLAYER_REACH} would leave the player without any target
 * at all, so the line of sight takes over: a ray starts in front of the player, walks towards the
 * cursor and the first cell of the chosen layer that holds something becomes the target, together
 * with the face the ray entered it through - which is what a block is built against, see
 * {@link BlockTarget#face()}. A ray that meets nothing ends at the reach of the player, which keeps a
 * cell available to build into.
 * <p>
 * <b>The layer is still named.</b> The game is drawn from above, so an action happens in one of the
 * two layers of the flat view, see {@link Chunk#flatY(int)}, and the ray walks in the plane of the
 * layer the player picked. Once the player stands in the world the layer is gone and the ray simply
 * follows the eyes, which is why the height is the only thing this class still takes from that view.
 * <p>
 * The player faces the mouse, see {@link Player#lookAt(float, float)}, so the ray always points where
 * the player is looking.
 */
public final class BlockTargeting {

    /**
     * Distance in blocks the ray starts in front of the player.
     * <p>
     * A ray that starts exactly at the body would meet the cell the body stands in, whose ground is
     * always filled, and would stop there instead of at the block the player is looking at.
     */
    private static final float START_AHEAD = 0.05f;

    private BlockTargeting() {
        // Utility class: never instantiated.
    }

    /**
     * Finds the cell an action applies to.
     *
     * @param world world holding the blocks
     * @param player player deciding where the reach ends
     * @param mouseWorldX world X coordinate of the mouse
     * @param mouseWorldY world Z coordinate of the mouse
     * @param layer layer the action applies to, see {@link Chunk#LAYER_FLOOR} and
     *              {@link Chunk#LAYER_OBJECT}
     * @return the targeted cell, never {@code null}
     */
    public static BlockTarget select(World world, Player player, float mouseWorldX,
            float mouseWorldY, int layer) {
        int cellX = MathUtils.floor(mouseWorldX / Constants.TILE_SIZE);
        int cellZ = MathUtils.floor(mouseWorldY / Constants.TILE_SIZE);
        int cellY = Chunk.flatY(layer);
        if (withinReach(player, cellX, cellZ)) {
            return BlockTarget.of(cellX, cellY, cellZ);
        }
        return alongSight(world, player, cellY);
    }

    /**
     * {@code true} when the middle of a cell is close enough to the player.
     *
     * @param player player the distance is measured from
     * @param cellX block X coordinate of the cell
     * @param cellZ block Z coordinate of the cell
     * @return {@code true} when the player can reach that cell
     */
    public static boolean withinReach(Player player, int cellX, int cellZ) {
        float centerX = (cellX + 0.5f) * Constants.TILE_SIZE;
        float centerZ = (cellZ + 0.5f) * Constants.TILE_SIZE;
        float reach = Constants.PLAYER_REACH * Constants.TILE_SIZE;
        float dx = centerX - player.position().x;
        float dz = centerZ - player.position().z;
        return dx * dx + dz * dz <= reach * reach;
    }

    /**
     * Walks the line of sight and returns the first cell that holds something.
     * <p>
     * The ray starts in front of the player, so the cell the player stands in is never reported: the
     * ground below it is always filled, and the walk would otherwise stop on the player itself
     * instead of the block the player is looking at. Its height is the layer the action applies to,
     * and it walks in the plane of that layer, which is what a view from above can show.
     *
     * @param world world holding the blocks
     * @param player player the ray starts at and the reach belongs to
     * @param cellY height the ray walks at, see {@link Chunk#flatY(int)}
     * @return the cell the ray met, or the cell at the end of the reach
     */
    private static BlockTarget alongSight(World world, Player player, int cellY) {
        float tile = Constants.TILE_SIZE;
        float startX = player.position().x / tile;
        float startZ = player.position().z / tile;
        float directionX = player.facing().x;
        float directionZ = player.facing().y;

        BlockRay.Hit hit = BlockRay.cast(world,
                startX + directionX * START_AHEAD, cellY, startZ + directionZ * START_AHEAD,
                directionX, 0.0f, directionZ, Constants.PLAYER_REACH,
                (access, x, y, z) -> !access.getBlock(x, y, z).isAir());
        if (hit != null) {
            return BlockTarget.of(hit);
        }

        // A ray that meets nothing ends at the reach of the player, which keeps a cell available to
        // build into. It carries no face, because it entered nothing.
        int endX = MathUtils.floor(startX + directionX * Constants.PLAYER_REACH);
        int endZ = MathUtils.floor(startZ + directionZ * Constants.PLAYER_REACH);
        return BlockTarget.fromRay(endX, cellY, endZ, null);
    }
}
