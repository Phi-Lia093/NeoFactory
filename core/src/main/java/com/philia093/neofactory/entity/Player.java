package com.philia093.neofactory.entity;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
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
 * {@link Constants#TILE_SIZE} of them, which is the same unit the renderer uses
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

    /** Length below which the movement vector counts as zero. */
    private static final float MIN_INPUT_LENGTH = 1.0e-4f;

    /** Normalized direction the player is looking at, set from the mouse. */
    private final Vector2 facing = new Vector2(1.0f, 0.0f);

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

    /** Live facing direction, always normalized. */
    public Vector2 facing() {
        return facing;
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
        data.putFloat(SaveTags.ROTATION_X, facing.x);
        data.putFloat(SaveTags.ROTATION_Y, facing.y);
        data.putInt(SaveTags.SELECTED_SLOT, inventory.selectedSlot());
        data.put(SaveTags.writeInventory(inventory));
    }

    @Override
    protected void readData(NbtCompound data) {
        facing.set(data.getFloat(SaveTags.ROTATION_X, 1.0f),
                data.getFloat(SaveTags.ROTATION_Y, 0.0f));
        if (facing.isZero()) {
            facing.set(1.0f, 0.0f);
        }
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
        return MathUtils.floor(position.x / Constants.TILE_SIZE);
    }

    /** Block Y coordinate of the feet of the player, its height in the world. */
    public int blockY() {
        return MathUtils.floor(position.y / Constants.TILE_SIZE);
    }

    /** Block Z coordinate the player currently stands in. */
    public int blockZ() {
        return MathUtils.floor(position.z / Constants.TILE_SIZE);
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
    public void lookAt(float worldX, float worldY) {
        float dx = worldX - position.x;
        float dy = worldY - position.y;
        if (Math.abs(dx) < MIN_INPUT_LENGTH && Math.abs(dy) < MIN_INPUT_LENGTH) {
            return;
        }
        facing.set(dx, dy).nor();
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
        velocity.set(moveInput.x * speed, 0.0f, moveInput.y * speed);
        if (moveInput.isZero()) {
            velocity.setZero();
            return;
        }
        moveWithCollision(world, velocity.x * delta, velocity.z * delta);
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
        float half = Constants.PLAYER_HITBOX * 0.5f;
        float height = Constants.PLAYER_HEIGHT;
        int minX = MathUtils.floor(centerX - half);
        int maxX = MathUtils.floor(centerX + half - SKIN_WIDTH);
        int minZ = MathUtils.floor(centerZ - half);
        int maxZ = MathUtils.floor(centerZ + half - SKIN_WIDTH);
        int minY = MathUtils.floor(position.y);
        int maxY = MathUtils.floor(position.y + height - SKIN_WIDTH);

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

