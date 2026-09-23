package com.philia093.neofactory.world.interaction;

import com.philia093.neofactory.block.BlockFace;
import com.philia093.neofactory.world.BlockAccess;

/**
 * Walks a ray through the cells of a world and reports the first one it meets.
 * <p>
 * The flat engine never needed this: it knew which cell the mouse pointed at and read that cell out
 * of the layer the player had picked, so a line of sight was only ever walked in a straight line
 * with a fixed step. A body that stands in the world aims with its eyes instead, and the cell it
 * looks at is the one a ray from those eyes enters first - together with the face it enters through,
 * because that face is what a block is built against.
 * <p>
 * The walk is the one every voxel engine uses, and it costs one step per cell the ray crosses
 * instead of one step per fraction of a cell, see Amanatides and Woo, <i>A Fast Voxel Traversal
 * Algorithm for Ray Tracing</i> (1987): the cell the ray starts in is known, and so is the distance
 * along the ray to the next boundary on every axis. The smallest of the three is the boundary the
 * ray reaches first, the walk steps across it, and the distance of the boundary beyond it is one
 * cell further away. A ray therefore visits the cells it really passes through, in order, and never
 * steps over a thin wall between two of them.
 * <p>
 * <b>Units.</b> A coordinate is a block, so a direction of {@code (1, 0, 0)} walks east and a
 * distance of {@code 4.5} is the reach of a player. A direction does not have to be normalized, but
 * the distance a hit reports is measured the way the direction is: pass a unit vector, which is what
 * a body looking somewhere produces, and the distance is in blocks.
 * <p>
 * <b>The walk is a query, not an action.</b> The caller decides what stops the ray through
 * {@link Stop}, so the same walk serves a player aiming at a block, a machine looking for what it
 * faces, or a fluid searching for the ground it runs onto.
 */
public final class BlockRay {

    /** A cell a ray met, with the face it entered through and the distance it walked. */
    public record Hit(int x, int y, int z, BlockFace face, float distance) {
    }

    /** Decides whether a cell stops a ray. */
    @FunctionalInterface
    public interface Stop {

        /**
         * Tests a cell the ray entered.
         *
         * @param world world the ray walks through
         * @param x block X coordinate
         * @param y block Y coordinate, the height
         * @param z block Z coordinate
         * @return {@code true} when the walk ends here
         */
        boolean test(BlockAccess world, int x, int y, int z);
    }

    private BlockRay() {
        // Utility class: never instantiated.
    }

    /**
     * Walks a ray and returns the first cell the stop test accepts.
     *
     * @param world world to walk through
     * @param startX world X coordinate the ray starts at
     * @param startY world Y coordinate the ray starts at
     * @param startZ world Z coordinate the ray starts at
     * @param dirX X component of the direction
     * @param dirY Y component of the direction
     * @param dirZ Z component of the direction
     * @param maxDistance how far the ray may reach, in blocks
     * @param stop test deciding which cell ends the walk
     * @return the cell that stopped the ray, or {@code null} when it reached nothing
     */
    public static Hit cast(BlockAccess world, float startX, float startY, float startZ,
            float dirX, float dirY, float dirZ, float maxDistance, Stop stop) {
        int x = floor(startX);
        int y = floor(startY);
        int z = floor(startZ);

        int stepX = sign(dirX);
        int stepY = sign(dirY);
        int stepZ = sign(dirZ);
        if (stepX == 0 && stepY == 0 && stepZ == 0) {
            return null;
        }

        // Distance along the ray from one boundary of a cell to the next, infinite on an axis the
        // ray does not move along.
        float deltaX = stepX == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dirX);
        float deltaY = stepY == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dirY);
        float deltaZ = stepZ == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dirZ);

        // Distance along the ray to the first boundary on every axis.
        float nextX = boundaryDistance(startX, x, dirX, deltaX);
        float nextY = boundaryDistance(startY, y, dirY, deltaY);
        float nextZ = boundaryDistance(startZ, z, dirZ, deltaZ);

        // The cell the ray starts in carries no face: the ray did not enter it through anything. That
        // is the cell the body stands in, so a caller that wants to skip it starts the ray in front
        // of itself, see BlockTargeting.
        BlockFace face = null;
        float walked = 0.0f;
        while (walked <= maxDistance) {
            if (stop.test(world, x, y, z)) {
                return new Hit(x, y, z, face, walked);
            }
            if (nextX <= nextY && nextX <= nextZ) {
                x += stepX;
                walked = nextX;
                nextX += deltaX;
                face = stepX > 0 ? BlockFace.WEST : BlockFace.EAST;
            } else if (nextY <= nextZ) {
                y += stepY;
                walked = nextY;
                nextY += deltaY;
                face = stepY > 0 ? BlockFace.BOTTOM : BlockFace.TOP;
            } else {
                z += stepZ;
                walked = nextZ;
                nextZ += deltaZ;
                face = stepZ > 0 ? BlockFace.NORTH : BlockFace.SOUTH;
            }
        }
        return null;
    }

    /**
     * Distance along the ray to the first boundary of a cell on one axis.
     *
     * @param start coordinate of the ray on that axis
     * @param cell cell the ray starts in, on that axis
     * @param direction component of the direction on that axis, may be zero
     * @param delta distance between two boundaries of a cell on that axis
     * @return the distance, infinite when the ray does not move along that axis
     */
    private static float boundaryDistance(float start, int cell, float direction, float delta) {
        if (delta == Float.POSITIVE_INFINITY) {
            return Float.POSITIVE_INFINITY;
        }
        float boundary = direction > 0.0f ? cell + 1 : cell;
        return (boundary - start) / direction;
    }

    /** Sign of a component of a direction: {@code -1}, {@code 0} or {@code 1}. */
    private static int sign(float value) {
        if (value > 0.0f) {
            return 1;
        }
        return value < 0.0f ? -1 : 0;
    }

    /** Cell a coordinate lies in, rounded down, the way the world addresses cells. */
    private static int floor(float coordinate) {
        return (int) Math.floor(coordinate);
    }
}
