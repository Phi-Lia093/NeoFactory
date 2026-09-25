package com.philia093.neofactory.util;

/**
 * An axis aligned box, the shape everything in a world of cubes is measured against.
 * <p>
 * A block of the flat engine was a square tile and a player was a square box on the same plane,
 * which is why the collision code of the old world worked with one size and a small fudge factor
 * to keep a box that touches a border from counting as an overlap. A world of cubes needs a box
 * with a height, and the fudge factor is no longer needed: a box that shares a face with a block
 * touches it, and touching is not overlapping, see {@link #intersects(Aabb)}.
 * <p>
 * The box is used wherever the world is looked at in space:
 * <ul>
 *     <li>the body of the player and of a dropped item, tested against the blocks around it</li>
 *     <li>the shape of a block, because a slab, a stair or a fence fills its cell only in part</li>
 *     <li>the selection frame around the block an action would touch</li>
 *     <li>the box of a chunk section, culled against the view before its mesh is drawn</li>
 * </ul>
 * <p>
 * <b>The box is mutable on purpose.</b> A frame walks many cells through the collision code, and a
 * box that allocates on the way would fill the heap with garbage every frame; the same reason the
 * vectors of libGDX are written into instead of replaced. A box that a {@link
 * com.philia093.neofactory.block.Block} hands out belongs to that block and is shared - reading it
 * is free, writing into it is a bug, exactly like the colour {@code Block#tint()} hands out.
 * <p>
 * The axes follow the world: X points east, Y points up and Z points south, so {@link #sizeY()} is
 * the height of a box.
 */
public final class Aabb {

    private float minX;
    private float minY;
    private float minZ;
    private float maxX;
    private float maxY;
    private float maxZ;

    /** Creates a box that covers nothing, at the origin of the world. */
    public Aabb() {
        // A fresh box covers nothing until it is set, see set.
    }

    /**
     * Creates a box from its two corners.
     *
     * @param minX smallest X
     * @param minY smallest Y
     * @param minZ smallest Z
     * @param maxX largest X
     * @param maxY largest Y
     * @param maxZ largest Z
     * @return the box, never {@code null}
     */
    public static Aabb of(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return new Aabb().set(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Creates the box of one whole block.
     * <p>
     * One block covers one world unit, see {@link Constants#BLOCK_SIZE}, so the box of the block at
     * {@code (x, y, z)} runs from its coordinate to the next one.
     *
     * @param x block X coordinate
     * @param y block Y coordinate
     * @param z block Z coordinate
     * @return the box of that cell
     */
    public static Aabb block(int x, int y, int z) {
        return new Aabb().setBlock(x, y, z);
    }

    /**
     * Creates a box that covers one block, from the origin.
     *
     * @return the box from {@code (0, 0, 0)} to {@code (1, 1, 1)}
     */
    public static Aabb unit() {
        return of(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Sets both corners of this box.
     *
     * @param minX smallest X
     * @param minY smallest Y
     * @param minZ smallest Z
     * @param maxX largest X
     * @param maxY largest Y
     * @param maxZ largest Z
     * @return this box, for chaining
     */
    public Aabb set(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
        return this;
    }

    /**
     * Makes this box the one of a whole block.
     *
     * @param x block X coordinate
     * @param y block Y coordinate
     * @param z block Z coordinate
     * @return this box, for chaining
     */
    public Aabb setBlock(int x, int y, int z) {
        float size = Constants.BLOCK_SIZE;
        return set(x * size, y * size, z * size, (x + 1) * size, (y + 1) * size, (z + 1) * size);
    }

    /**
     * Copies another box into this one.
     *
     * @param other box to read
     * @return this box, for chaining
     */
    public Aabb set(Aabb other) {
        return set(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ);
    }

    /** Smallest X of this box. */
    public float minX() {
        return minX;
    }

    /**
     * Empties this box.
     * <p>
     * An empty box covers nothing, see {@link #isEmpty()}, and shares no space with anything: it is what
     * a cell answers with that a body walks through.
     *
     * @return this box, for chaining
     */
    public Aabb clear() {
        return set(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    /** Smallest Y of this box. */
    public float minY() {
        return minY;
    }

    /** Smallest Z of this box. */
    public float minZ() {
        return minZ;
    }

    /** Largest X of this box. */
    public float maxX() {
        return maxX;
    }

    /** Largest Y of this box. */
    public float maxY() {
        return maxY;
    }

    /** Largest Z of this box. */
    public float maxZ() {
        return maxZ;
    }

    /** Width of this box along the X axis. */
    public float sizeX() {
        return maxX - minX;
    }

    /** Height of this box along the Y axis. */
    public float sizeY() {
        return maxY - minY;
    }

    /** Depth of this box along the Z axis. */
    public float sizeZ() {
        return maxZ - minZ;
    }

    /** X coordinate of the middle of this box. */
    public float centerX() {
        return (minX + maxX) * 0.5f;
    }

    /** Y coordinate of the middle of this box. */
    public float centerY() {
        return (minY + maxY) * 0.5f;
    }

    /** Z coordinate of the middle of this box. */
    public float centerZ() {
        return (minZ + maxZ) * 0.5f;
    }

    /** {@code true} when this box covers no space at all. */
    public boolean isEmpty() {
        return sizeX() <= 0.0f || sizeY() <= 0.0f || sizeZ() <= 0.0f;
    }

    /**
     * Moves this box through the world.
     *
     * @param dx distance along the X axis
     * @param dy distance along the Y axis
     * @param dz distance along the Z axis
     * @return this box, for chaining
     */
    public Aabb offset(float dx, float dy, float dz) {
        minX += dx;
        minY += dy;
        minZ += dz;
        maxX += dx;
        maxY += dy;
        maxZ += dz;
        return this;
    }

    /**
     * Makes this box larger or smaller on every side.
     * <p>
     * Growing is how the reach of a player is tested against a box and how a mesh is kept from
     * being culled while it still reaches into the view. Shrinking past the middle turns the box
     * inside out, which {@link #isEmpty()} reports instead of hiding.
     *
     * @param amount distance added to every side, negative to shrink
     * @return this box, for chaining
     */
    public Aabb grow(float amount) {
        minX -= amount;
        minY -= amount;
        minZ -= amount;
        maxX += amount;
        maxY += amount;
        maxZ += amount;
        return this;
    }

    /**
     * {@code true} when this box and another one share a piece of space.
     * <p>
     * Two boxes that only touch - a player standing exactly on the floor of a block, two cells side
     * by side - do <b>not</b> overlap. That is what keeps a body that rests on the ground from being
     * pushed out of it, and it is the rule the fudge factor of the flat engine was standing in for.
     *
     * @param other box to test against
     * @return {@code true} when they share more than a face
     */
    public boolean intersects(Aabb other) {
        return maxX > other.minX && minX < other.maxX
                && maxY > other.minY && minY < other.maxY
                && maxZ > other.minZ && minZ < other.maxZ;
    }

    /**
     * {@code true} when a point lies inside this box.
     * <p>
     * A point on the near side counts as inside, a point on the far side does not, which is the
     * same half open rule {@link #intersects(Aabb)} follows.
     *
     * @param x X coordinate of the point
     * @param y Y coordinate of the point
     * @param z Z coordinate of the point
     * @return {@code true} when the point is inside
     */
    public boolean contains(float x, float y, float z) {
        return x >= minX && x < maxX
                && y >= minY && y < maxY
                && z >= minZ && z < maxZ;
    }

    /** A copy of this box, for a caller that needs one it may write into. */
    public Aabb copy() {
        return of(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public String toString() {
        return "Aabb(" + minX + ", " + minY + ", " + minZ + ") to (" + maxX + ", " + maxY + ", "
                + maxZ + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Aabb)) {
            return false;
        }
        Aabb other = (Aabb) o;
        return Float.compare(minX, other.minX) == 0 && Float.compare(minY, other.minY) == 0
                && Float.compare(minZ, other.minZ) == 0 && Float.compare(maxX, other.maxX) == 0
                && Float.compare(maxY, other.maxY) == 0 && Float.compare(maxZ, other.maxZ) == 0;
    }

    @Override
    public int hashCode() {
        int result = Float.floatToIntBits(minX);
        result = 31 * result + Float.floatToIntBits(minY);
        result = 31 * result + Float.floatToIntBits(minZ);
        result = 31 * result + Float.floatToIntBits(maxX);
        result = 31 * result + Float.floatToIntBits(maxY);
        result = 31 * result + Float.floatToIntBits(maxZ);
        return result;
    }
}
