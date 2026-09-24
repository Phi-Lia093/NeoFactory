package com.philia093.neofactory.entity;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.philia093.neofactory.item.PlayerInventory;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.save.SaveTags;

/**
 * The controllable player.
 * <p>
 * The world is seen from above, so the player is a square box that slides over
 * the ground instead of a figure that falls. Both world axes are horizontal and
 * the player may walk in any direction, the camera follows its position.
 * <p>
 * The position is stored in world units, one block covering
 * {@link Constants#BLOCK_SIZE} of them, which is the same unit the renderer uses
 * to place a tile. Block coordinates are derived on demand through
 * {@link #blockX()} and {@link #blockY()}.
 * <p>
 * The facing direction is driven by the mouse and never by the keyboard, the
 * movement keys leave it untouched.
 * <p>
 * The player is an {@link Entity} like everything else in the world: it is stored
 * with the position every entity has plus a {@code Data} group holding the facing,
 * the selected slot and the inventory, see {@link #writeData(NbtCompound)}. That is
 * what makes a world remember where the player stood without a special case in the
 * save code.
 */
public class Player extends Entity {

    /**
     * Distance the player box stops short of a solid cell.
     * <p>
     * Without it a box that touches a cell border exactly would already count as
     * overlapping and the player would stop a whole hitbox away from every wall.
     */
    private static final float SKIN_WIDTH = 1.0e-3f;

    /**
     * Longest step the height of a body may take at once, in blocks.
     * <p>
     * The ground of the world is a single block thick, so a step longer than that would step right over
     * it: a fall is walked in pieces of this length, see {@code moveVertically}.
     */
    private static final float LONGEST_STEP = 0.5f;

    /** Length below which the movement vector counts as zero. */
    private static final float MIN_INPUT_LENGTH = 1.0e-4f;

    /** Facing direction the flat view starts with, seen from above, and the view it belongs to. */
    private final Vector2 facing = new Vector2(0.0f, 1.0f);

    /** Yaw of the view in degrees, see {@link #yaw()}. */
    private float yaw;

    /** Pitch of the view in degrees, see {@link #pitch()}. */
    private float pitch;

    /** {@code true} while the feet stand on a block, see {@link #jump()}. */
    private boolean onGround;

    /** Walking direction requested by the keyboard, normalized or zero. */
    private final Vector2 moveInput = new Vector2();

    /** Items the player carries, see {@link PlayerInventory}. */
    private final PlayerInventory inventory = new PlayerInventory();

    /**
     * Factor the walking speed is multiplied with.
     * <p>
     * The camera zoom is written into this field by the screen, which keeps the
     * player feeling equally fast no matter how far the camera is zoomed out,
     * without the entity interface having to know about a camera.
     */
    private float speedScale = 1.0f;

    /**
     * Creates a player standing in a column of the flat view.
     * <p>
     * The game is still drawn from above, so a column has a ground layer and the cell above it and a
     * body stands in the upper one, see {@link Chunk#flatY(int)}. This is the constructor the screens
     * of that view use; it goes away with the view.
     *
     * @param x world X coordinate of the player center
     * @param z world Z coordinate of the player center
     */
    public Player(float x, float z) {
        this(x, Chunk.flatY(Chunk.LAYER_OBJECT) * Constants.BLOCK_SIZE, z);
    }

    /**
     * Creates a player standing at a position.
     *
     * @param x world X coordinate of the player center
     * @param y world Y coordinate of the feet of the player
     * @param z world Z coordinate of the player center
     */
    public Player(float x, float y, float z) {
        super(EntityTypes.PLAYER);
        position.set(x, y, z);
    }

    /**
     * Creates a player standing in the middle of a walkable cell.
     * <p>
     * The spawn cell is searched for, so the player never starts inside a tree or
     * on top of a hole, and its feet are put on the surface of that column.
     *
     * @param world world the player lives in
     * @param x preferred block X coordinate of the spawn
     * @param z preferred block Z coordinate of the spawn
     * @return a player centered in a walkable cell, standing on its ground
     */
    public static Player spawnOnGround(World world, int x, int z) {
        int[] cell = world.findSpawnPosition(x, z);
        return new Player((cell[0] + 0.5f) * Constants.BLOCK_SIZE,
                world.surfaceY(cell[0], cell[1]) * Constants.BLOCK_SIZE,
                (cell[1] + 0.5f) * Constants.BLOCK_SIZE);
    }

    /**
     * Direction the player looks in, on the plane of X and Z, derived from {@link #yaw()}.
     * <p>
     * The flat view turns the player towards the cursor, so it needs the direction as a vector; a body
     * that stands in the world carries a yaw and a pitch instead, see {@link #turn(float, float)}. Do
     * not write into this vector: it belongs to the player.
     */
    public Vector2 facing() {
        return facing;
    }

    /**
     * Yaw of the view in degrees, the compass the player faces.
     * <p>
     * Zero looks south, along {@code +Z}, and the view turns clockwise seen from above as the number
     * grows: ninety looks west, along {@code -X}, exactly the way the original game counts it. The
     * direction of the view is {@code (-sin(yaw), 0, cos(yaw))}.
     */
    public float yaw() {
        return yaw;
    }

    /** Pitch of the view in degrees, positive looks up, {@code -PITCH_LIMIT} to {@code +PITCH_LIMIT}. */
    public float pitch() {
        return pitch;
    }

    /**
     * Turns the view.
     *
     * @param deltaYaw degrees to turn by, positive turns to the right
     * @param deltaPitch degrees to look up by, positive looks up
     */
    public void turn(float deltaYaw, float deltaPitch) {
        setView(yaw + deltaYaw, pitch + deltaPitch);
    }

    /**
     * Points the view somewhere.
     *
     * @param yaw yaw in degrees, see {@link #yaw()}
     * @param pitch pitch in degrees, kept inside {@link Constants#PITCH_LIMIT}
     */
    public void setView(float yaw, float pitch) {
        this.yaw = wrapDegrees(yaw);
        this.pitch = MathUtils.clamp(pitch, -Constants.PITCH_LIMIT, Constants.PITCH_LIMIT);
        updateFacing();
    }

    /**
     * Direction the view looks in, in space.
     * <p>
     * This is what a camera is placed with and what a line of sight is cast along, see
     * {@link com.philia093.neofactory.world.interaction.BlockRay}. The pixel size of the block is
     * applied by the caller, which is why the vector is a direction and not a distance.
     *
     * @param out vector to write, returned for chaining
     * @return the direction, normalized
     */
    public Vector3 lookDirection(Vector3 out) {
        float cosPitch = MathUtils.cosDeg(pitch);
        out.set(-MathUtils.sinDeg(yaw) * cosPitch, MathUtils.sinDeg(pitch),
                MathUtils.cosDeg(yaw) * cosPitch);
        return out.nor();
    }

    /** Turns the compass into the range {@code -180} to {@code 180}. */
    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0f;
        if (wrapped > 180.0f) {
            wrapped -= 360.0f;
        } else if (wrapped < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }

    /** Keeps the vector the flat view reads in step with the yaw of the view. */
    private void updateFacing() {
        facing.set(-MathUtils.sinDeg(yaw), MathUtils.cosDeg(yaw));
    }

    /**
     * Sets the factor the walking speed is multiplied with.
     *
     * @param speedScale factor, greater than zero
     */
    public void setSpeedScale(float speedScale) {
        this.speedScale = speedScale > 0.0f ? speedScale : 1.0f;
    }

    @Override
    public float hitboxHalfExtent() {
        return Constants.PLAYER_HITBOX * 0.5f;
    }

    @Override
    protected void writeData(NbtCompound data) {
        data.putFloat(SaveTags.ROTATION_X, yaw);
        data.putFloat(SaveTags.ROTATION_Y, pitch);
        data.putInt(SaveTags.SELECTED_SLOT, inventory.selectedSlot());
        data.put(SaveTags.writeInventory(inventory));
    }

    @Override
    protected void readData(NbtCompound data) {
        // A world written before the player carried a compass holds the two components of its facing
        // direction in these tags; read as a yaw they name a direction instead of an angle, which is
        // wrong but harmless, and the format refuses a world of another version anyway.
        setView(data.getFloat(SaveTags.ROTATION_X, 0.0f), data.getFloat(SaveTags.ROTATION_Y, 0.0f));
        SaveTags.readInventory(inventory, data.getList(SaveTags.INVENTORY));
        inventory.setSelectedSlot(data.getInt(SaveTags.SELECTED_SLOT, 0));
    }

    /**
     * Inventory of the player.
     * <p>
     * The first nine slots form the hotbar, the remaining ones are the storage the
     * inventory screen shows.
     */
    public PlayerInventory inventory() {
        return inventory;
    }

    /** Block X coordinate the player currently stands in. */
    public int blockX() {
        return MathUtils.floor(position.x / Constants.BLOCK_SIZE);
    }

    /** Block Y coordinate of the feet of the player, its height in the world. */
    public int blockY() {
        return MathUtils.floor(position.y / Constants.BLOCK_SIZE);
    }

    /** Block Z coordinate the player currently stands in. */
    public int blockZ() {
        return MathUtils.floor(position.z / Constants.BLOCK_SIZE);
    }

    /**
     * Sets the walking direction requested by the keyboard.
     * <p>
     * The vector is normalized, therefore moving diagonally is not faster than
     * moving along a single axis. Passing {@code (0, 0)} stops the player.
     *
     * @param x direction along the X axis, {@code -1}, {@code 0} or {@code 1}
     * @param y direction along the Y axis, {@code -1}, {@code 0} or {@code 1}
     */
    public void setMoveInput(float x, float y) {
        moveInput.set(x, y);
        if (moveInput.len2() > MIN_INPUT_LENGTH) {
            moveInput.nor();
        }
    }

    /**
     * Turns the player towards a world position, normally the mouse cursor.
     *
     * @param worldX world X coordinate to look at
     * @param worldY world Y coordinate to look at
     */
    public void lookAt(float worldX, float worldZ) {
        float dx = worldX - position.x;
        float dz = worldZ - position.z;
        if (Math.abs(dx) < MIN_INPUT_LENGTH && Math.abs(dz) < MIN_INPUT_LENGTH) {
            return;
        }
        // The flat view asks for a direction, the view of a body in the world is a yaw: the two agree
        // because the direction of a yaw is (-sin(yaw), 0, cos(yaw)), see #yaw().
        setView(MathUtils.atan2(-dx, dz) * MathUtils.radiansToDegrees, pitch);
    }

    /**
     * Stops the player right away.
     * <p>
     * Called while a screen covers the world, for example the inventory, so the
     * player does not keep walking while the keyboard drives the interface.
     */
    public void halt() {
        setMoveInput(0.0f, 0.0f);
        velocity.setZero();
    }

    /** {@code true} when the player stands in water or another liquid. */
    public boolean isInLiquid(World world) {
        return world.getBlock(blockX(), blockY(), blockZ()).isLiquid();
    }

    /**
     * Advances the player by one frame.
     * <p>
     * The movement is scaled by {@code delta}, which makes the speed independent
     * of the frame rate, and by the speed scale, which keeps the player feeling
     * equally fast no matter how far the camera is zoomed out.
     *
     * @param world world used to resolve collisions
     * @param delta time since the last frame in seconds
     */
    @Override
    public void update(World world, float delta) {
        float speed = Constants.PLAYER_SPEED * Constants.BLOCK_SIZE * speedScale;
        // The keyboard asks for a walk in the frame of the view: forward is where the player looks and
        // right is the right hand of that view. The direction of a yaw is (-sin, 0, cos) and its right
        // hand is the cross product of that direction with the up axis, (direction x up) =
        // (-cos, 0, -sin) - the order matters, because up x direction is the left hand.
        float sin = MathUtils.sinDeg(yaw);
        float cos = MathUtils.cosDeg(yaw);
        float forward = moveInput.y;
        float sideways = moveInput.x;
        velocity.x = (-sideways * cos - forward * sin) * speed;
        velocity.z = (forward * cos - sideways * sin) * speed;

        // The world pulls the body towards the ground and a jump pushes it away from it, see #jump().
        velocity.y -= Constants.GRAVITY * delta;
        if (moveInput.isZero()) {
            velocity.x = 0.0f;
            velocity.z = 0.0f;
        } else {
            moveWithCollision(world, velocity.x * delta, velocity.z * delta);
        }
        moveVertically(world, velocity.y * delta);
    }

    /**
     * Pushes the body off the ground.
     * <p>
     * A jump is only possible while the body stands on something: a player who jumps in the air is
     * asking for a second jump, which the world does not have.
     */
    public void jump() {
        if (!onGround) {
            return;
        }
        velocity.y = Constants.JUMP_SPEED;
        onGround = false;
    }

    /** {@code true} while the body stands on a block, see {@link #jump()}. */
    public boolean isOnGround() {
        return onGround;
    }

    /**
     * Moves the body up or down and lets it land.
     * <p>
     * A fall ends where the feet meet the top of a block, and the body is put back on that top instead
     * of into it: a body that is drawn into a block can never be moved out of it again, because every
     * step it takes still overlaps the block.
     * <p>
     * A long step is walked in pieces of at most {@link #LONGEST_STEP} blocks, because the ground is one
     * block thick: a body that falls further than that in one go steps from above the ground to below it,
     * finds nothing at the place it asks about and keeps falling through a floor that was right under its
     * feet. That is how a player who entered a world on the slow first frames fell out of it.
     *
     * @param world world used for collision tests
     * @param stepY requested movement along the height
     */
    private void moveVertically(World world, float stepY) {
        float remaining = stepY;
        while (Math.abs(remaining) > LONGEST_STEP) {
            if (!stepVertically(world, Math.copySign(LONGEST_STEP, stepY))) {
                return;
            }
            remaining -= Math.copySign(LONGEST_STEP, stepY);
        }
        stepVertically(world, remaining);
    }

    /**
     * One piece of a vertical movement.
     *
     * @param world world used for collision tests
     * @param stepY movement of this piece, never longer than {@link #LONGEST_STEP} blocks
     * @return {@code true} when the body moved, {@code false} when it landed or was stopped
     */
    private boolean stepVertically(World world, float stepY) {
        float candidateY = position.y + stepY;
        if (!collides(world, position.x, candidateY, position.z)) {
            onGround = false;
            position.y = candidateY;
            return true;
        }
        velocity.y = 0.0f;
        if (stepY > 0.0f) {
            return false;
        }
        // The feet come to rest on the block that stopped the fall.
        position.y = MathUtils.floor(candidateY) + 1.0f;
        while (collides(world, position.x, position.y, position.z)
                && position.y <= Constants.MAX_Y) {
            position.y += 1.0f;
        }
        onGround = true;
        return false;
    }

    /**
     * Moves the player and stops the movement at solid blocks.
     * <p>
     * The two horizontal axes are resolved separately so that walking into a wall still
     * allows sliding along it. The height of the body is left alone: the flat view knows no
     * gravity, so a body that stands on its ground keeps standing on it.
     *
     * @param world world used for collision tests
     * @param stepX requested movement along the X axis
     * @param stepZ requested movement along the Z axis
     */
    private void moveWithCollision(World world, float stepX, float stepZ) {
        if (stepX != 0.0f) {
            float candidateX = position.x + stepX;
            if (collides(world, candidateX, position.z)) {
                velocity.x = 0.0f;
            } else {
                position.x = candidateX;
            }
        }
        if (stepZ != 0.0f) {
            float candidateZ = position.z + stepZ;
            if (collides(world, position.x, candidateZ)) {
                velocity.z = 0.0f;
            } else {
                position.z = candidateZ;
            }
        }
    }

    /**
     * Tests the box of the player against the solid cells of the world.
     * <p>
     * The body of the flat view was a square that slid over the ground; a world of cubes gives it a
     * height, so the box is {@link Constants#PLAYER_HITBOX} wide and
     * {@link Constants#PLAYER_HEIGHT} tall, and every cell it covers decides whether the body may
     * stand there. Its feet are at the position of the entity.
     *
     * @param world world to test against
     * @param centerX candidate world X coordinate of the player center
     * @param centerZ candidate world Z coordinate of the player center
     * @return {@code true} when the box overlaps at least one solid cell
     */
    public boolean collides(World world, float centerX, float centerZ) {
        return collides(world, centerX, position.y, centerZ);
    }

    /**
     * Tests the box of the player against the solid cells of the world at a height of its own.
     *
     * @param world world to test against
     * @param centerX candidate world X coordinate of the player center
     * @param centerY candidate world Y coordinate of the feet of the player
     * @param centerZ candidate world Z coordinate of the player center
     * @return {@code true} when the box overlaps at least one solid cell
     */
    public boolean collides(World world, float centerX, float centerY, float centerZ) {
        // The world has a bottom, and it is a floor: a body that reaches it stands on it instead of falling
        // out of the world and asking about a cell that is not there.
        if (centerY < Constants.MIN_Y) {
            return true;
        }
        float half = Constants.PLAYER_HITBOX * 0.5f;
        float height = Constants.PLAYER_HEIGHT;
        int minX = MathUtils.floor(centerX - half);
        int maxX = MathUtils.floor(centerX + half - SKIN_WIDTH);
        int minZ = MathUtils.floor(centerZ - half);
        int maxZ = MathUtils.floor(centerZ + half - SKIN_WIDTH);
        int minY = MathUtils.floor(centerY);
        int maxY = Math.min(Constants.MAX_Y, MathUtils.floor(centerY + height - SKIN_WIDTH));

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (world.isSolid(x, y, z)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Player(" + position.x + ", " + position.y + ")";
    }
}

