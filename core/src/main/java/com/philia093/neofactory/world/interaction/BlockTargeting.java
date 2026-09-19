package com.philia093.neofactory.world.interaction;

import com.badlogic.gdx.math.MathUtils;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.World;

/**
 * Picks the cell a break or a build action would touch.
 * <p>
 * The mouse decides, as long as the cell under it is close enough to the player:
 * that is the cell the player points at, and reach is what stops the player from
 * editing the whole landscape at once.
 * <p>
 * Aiming further away than {@link Constants#PLAYER_REACH} would leave the player
 * without any target at all, so the line of sight takes over: the ray starts in
 * front of the player, walks towards the cursor and the first cell of the chosen
 * layer that holds something becomes the target. A ray that meets nothing ends at
 * the reach of the player, which keeps a cell available to build into.
 * <p>
 * The player faces the mouse, see {@link Player#lookAt(float, float)}, so the ray
 * always points where the player is looking.
 */
public final class BlockTargeting {

    /** Length of a single step along the line of sight, in blocks. */
    private static final float STEP = 0.25f;

    /** Tolerance that keeps the last step from dropping out of the loop. */
    private static final float EPSILON = 1.0e-3f;

    private BlockTargeting() {
        // Utility class: never instantiated.
    }

    /**
     * Finds the cell an action applies to.
     *
     * @param world world holding the blocks
     * @param player player deciding where the reach ends
     * @param mouseWorldX world X coordinate of the mouse
     * @param mouseWorldY world Y coordinate of the mouse
     * @param layer layer the action applies to, see {@link Chunk#LAYER_FLOOR} and
     *              {@link Chunk#LAYER_OBJECT}
     * @return the targeted cell, never {@code null}
     */
    public static BlockTarget select(World world, Player player, float mouseWorldX,
            float mouseWorldY, int layer) {
        int cellX = MathUtils.floor(mouseWorldX / Constants.TILE_SIZE);
        int cellY = MathUtils.floor(mouseWorldY / Constants.TILE_SIZE);
        if (withinReach(player, cellX, cellY)) {
            return BlockTarget.of(cellX, cellY, layer);
        }
        return alongSight(world, player, layer);
    }

    /**
     * {@code true} when the middle of a cell is close enough to the player.
     *
     * @param player player the distance is measured from
     * @param cellX block X coordinate of the cell
     * @param cellY block Y coordinate of the cell
     */
    public static boolean withinReach(Player player, int cellX, int cellY) {
        float centerX = (cellX + 0.5f) * Constants.TILE_SIZE;
        float centerY = (cellY + 0.5f) * Constants.TILE_SIZE;
        float reach = Constants.PLAYER_REACH * Constants.TILE_SIZE;
        float dx = centerX - player.position().x;
        float dy = centerY - player.position().y;
        return dx * dx + dy * dy <= reach * reach;
    }

    /**
     * Walks along the line of sight and returns the first cell that holds
     * something.
     * <p>
     * The cell the player stands in is never reported: the ground below it is always
     * filled, so the walk would otherwise stop on the player itself instead of the
     * block the player is looking at.
     *
     * @param world world holding the blocks
     * @param player player the ray starts at and the reach belongs to
     * @param layer layer the ray looks at, only this layer can stop it
     * @return the cell the ray met, or the cell at the end of the reach
     */
    private static BlockTarget alongSight(World world, Player player, int layer) {
        float tileSize = Constants.TILE_SIZE;
        float stepLength = STEP * tileSize;
        float maxDistance = Constants.PLAYER_REACH * tileSize;
        float directionX = player.facing().x;
        float directionY = player.facing().y;

        int ownX = MathUtils.floor(player.position().x / tileSize);
        int ownY = MathUtils.floor(player.position().y / tileSize);

        int previousX = Integer.MIN_VALUE;
        int previousY = Integer.MIN_VALUE;
        int lastX = previousX;
        int lastY = previousY;

        for (float distance = stepLength; distance <= maxDistance + EPSILON; distance += stepLength) {
            int cellX = MathUtils.floor((player.position().x + directionX * distance) / tileSize);
            int cellY = MathUtils.floor((player.position().y + directionY * distance) / tileSize);
            if ((cellX == previousX && cellY == previousY) || (cellX == ownX && cellY == ownY)) {
                // Several steps stay inside one cell, and the own cell is skipped.
                continue;
            }
            previousX = cellX;
            previousY = cellY;
            lastX = cellX;
            lastY = cellY;
            if (!world.getBlock(cellX, cellY, layer).isAir()) {
                return BlockTarget.fromRay(cellX, cellY, layer);
            }
        }

        if (lastX == Integer.MIN_VALUE) {
            // Only reachable with a reach shorter than one step, kept for safety.
            lastX = ownX;
            lastY = ownY;
        }
        return BlockTarget.fromRay(lastX, lastY, layer);
    }
}
