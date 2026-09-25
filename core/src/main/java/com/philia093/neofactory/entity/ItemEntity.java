package com.philia093.neofactory.entity;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.util.Aabb;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.util.nbt.NbtCompound;
import com.philia093.neofactory.world.Chunk;
import com.philia093.neofactory.world.World;
import com.philia093.neofactory.world.save.SaveTags;

/**
 * Items lying on the ground.
 * <p>
 * Breaking a block hands its items to {@code WorldDrops}, which spawns an entity like this
 * one instead of moving the items straight into the inventory. The entity is thrown a
 * little when it appears, slides a short distance and comes to rest, then waits
 * {@link #DEFAULT_PICKUP_DELAY} seconds before it may be taken: a drop that is picked up
 * in the same frame it appeared would never be seen.
 * <p>
 * Inside {@link #MAGNET_RANGE_BLOCKS} of the player the item is drawn towards them, which is
 * what makes walking past a pile collect it. A full inventory attracts nothing, so an item
 * that cannot be taken stops where it is instead of piling up on the player. Further away
 * nothing happens, so a drop the player does not want stays where it is.
 * <p>
 * What does not fit stays where it is: the stack shrinks to the remaining amount instead
 * of being lost, which is what the inventory only implementation of {@code ItemDrops}
 * could not do.
 */
public class ItemEntity extends Entity {

    /** Distance in blocks within which the player picks the item up. */
    private static final float PICKUP_RANGE_BLOCKS = 0.8f;

    /** Distance in blocks within which an item is drawn towards the player. */
    private static final float MAGNET_RANGE_BLOCKS = 3.0f;

    /** Speed in blocks per second an item flies to the player with inside the magnet. */
    private static final float MAGNET_SPEED_BLOCKS = 7.0f;

    /** Share of its speed an item keeps per second while it slides over the ground. */
    private static final float SLIDE_PER_SECOND = 0.02f;

    /**
     * Seconds an item on the ground waits before it can be picked up.
     * <p>
     * The delay is what keeps a block the player just broke from flying straight back into
     * the inventory: without it the drop of a block that was mined from above would never
     * be seen, and a stack that was thrown away would be gone before it left the hand.
     */
    private static final float DEFAULT_PICKUP_DELAY = 0.8f;

    /** Seconds an item stays in the world before it is removed. */
    private static final float DESPAWN_SECONDS = 300.0f;

    /**
     * Longest piece a movement of an item is walked in, in blocks.
     * <p>
     * A frame that took long enough - the first ones of a world, a stall - would move an item further than the
     * terrain is thick in one go. A step that long goes right through the ground, because the box of the item
     * never overlaps a block on its way: nothing stops it and the item ends up under the world. Every movement
     * is therefore walked in pieces of at most this length, the way a player walks a fall in pieces, see
     * {@code Player#moveVertically}.
     */
    private static final float LONGEST_STEP = 0.5f;

    /** Reused vector pointing from the item to the player. */
    private final Vector3 towards = new Vector3();

    /** Box of this item, written while it is moved through the world. */
    private final Aabb box = new Aabb();

    /** Box of one cell, written while the world is asked for the shapes of its blocks. */
    private final Aabb shapeBox = new Aabb();

    /** Items this entity stands for, empty only for a broken save game. */
    private ItemStack stack = ItemStack.EMPTY;

    /** Seconds left before the item may be picked up. */
    private float pickupDelay = DEFAULT_PICKUP_DELAY;

    /** Seconds this item exists, counted since it was dropped. */
    private float age;

    /**
     * Creates an item on the ground.
     *
     * @param x world X coordinate of the item
     * @param y world Y coordinate of the item, its height
     * @param z world Z coordinate of the item
     * @param stack items this entity stands for, copied by the constructor
     */
    public ItemEntity(float x, float y, float z, ItemStack stack) {
        super(EntityTypes.ITEM);
        position.set(x, y, z);
        if (stack != null && !stack.isEmpty()) {
            this.stack = stack.copy();
        }
    }

    /** Items this entity stands for, never {@code null}. */
    public ItemStack stack() {
        return stack;
    }

    /** Seconds this item exists. */
    public float age() {
        return age;
    }

    /** Seconds left before the item may be picked up. */
    public float pickupDelay() {
        return pickupDelay;
    }

    @Override
    public float hitboxHalfExtent() {
        return Constants.ITEM_SIZE * 0.5f;
    }

    @Override
    public void update(World world, float delta) {
        age += delta;
        if (age >= DESPAWN_SECONDS) {
            discard();
            return;
        }
        if (pickupDelay > 0.0f) {
            pickupDelay = Math.max(0.0f, pickupDelay - delta);
        }
        if (stack.isEmpty()) {
            // Nothing to hand out, only a broken save game gets here.
            discard();
            return;
        }

        move(world, delta);

        Player player = world.entities().player();
        if (player == null || pickupDelay > 0.0f) {
            return;
        }
        if (!player.inventory().hasRoomFor(stack)) {
            // A full inventory attracts nothing: an item that was already flying towards the
            // player stops where it is and waits until a place is free again.
            return;
        }
        float magnetRange = MAGNET_RANGE_BLOCKS * Constants.BLOCK_SIZE;
        if (player.position().dst2(position) <= magnetRange * magnetRange) {
            pull(player, delta);
        }
        float range = PICKUP_RANGE_BLOCKS * Constants.BLOCK_SIZE;
        if (player.position().dst2(position) > range * range) {
            return;
        }

        int remaining = player.inventory().add(stack);
        if (remaining <= 0) {
            discard();
            return;
        }
        // A full inventory keeps the rest on the ground instead of throwing it away.
        stack = ItemStack.of(stack.item(), remaining);
    }

    /**
     * Moves the item by its velocity through the world, which changes both of them.
     * <p>
     * An item is a body like a player is one: the world pulls it down, a block stops it and it comes to rest
     * on top of the shape it lands on - the half of a slab is where a stone stops and not the top of the cell.
     * The two horizontal axes are resolved apart, so an item that is thrown against a wall slides along it
     * instead of sticking to it, and a block that is built into the cell an item lies in pushes the item on
     * top of itself, see {@link #squeezeOut(World)}.
     *
     * @param world world the item lies in
     * @param delta time since the last frame in seconds
     */
    private void move(World world, float delta) {
        velocity.y -= Constants.GRAVITY * delta;
        walk(world, velocity.x * delta, velocity.y * delta, velocity.z * delta);
        friction(delta);
        squeezeOut(world);
    }

    /**
     * Walks one movement of an item in pieces, so a long step cannot pass through the ground.
     *
     * @param world world the item lies in
     * @param stepX movement along the X axis
     * @param stepY movement along the Y axis, the height
     * @param stepZ movement along the Z axis
     */
    private void walk(World world, float stepX, float stepY, float stepZ) {
        float longest = Math.max(Math.abs(stepX), Math.max(Math.abs(stepY), Math.abs(stepZ)));
        int pieces = Math.max(1, (int) Math.ceil(longest / LONGEST_STEP));
        for (int piece = 0; piece < pieces; piece++) {
            step(world, stepX / pieces, stepY / pieces, stepZ / pieces);
        }
    }

    /**
     * Moves the item one piece of its movement through the world, which changes both of them.
     * <p>
     * The two horizontal axes are resolved apart, so an item that is thrown against a wall slides along it
     * instead of sticking to it, and the height is resolved last: the item comes to rest on top of the shape
     * that stopped it, which is the half of a slab and not always the top of the cell. A block that is built
     * into the cell an item lies in pushes the item on top of itself, see {@link #squeezeOut(World)}.
     *
     * @param world world the item lies in
     * @param stepX movement of this piece along the X axis
     * @param stepY movement of this piece along the Y axis, the height
     * @param stepZ movement of this piece along the Z axis
     */
    private void step(World world, float stepX, float stepY, float stepZ) {
        if (stepX != 0.0f) {
            if (hits(world, stepX, 0.0f, 0.0f)) {
                velocity.x = 0.0f;
            } else {
                position.x += stepX;
            }
        }
        if (stepZ != 0.0f) {
            if (hits(world, 0.0f, 0.0f, stepZ)) {
                velocity.z = 0.0f;
            } else {
                position.z += stepZ;
            }
        }
        if (stepY == 0.0f) {
            return;
        }
        if (hits(world, 0.0f, stepY, 0.0f)) {
            if (stepY > 0.0f) {
                // A ceiling carries nothing: an item that was thrown up against one stops moving up and is
                // left where it is, and the world pulls it down again on the next frame.
                velocity.y = 0.0f;
            } else {
                // The item comes to rest on top of the shape that stopped it, and a fall does not bounce.
                position.y = world.landingHeight(box, MathUtils.floor(position.y + stepY), position.y,
                        shapeBox);
                velocity.y = 0.0f;
            }
        } else {
            position.y += stepY;
        }
        if (position.y < Constants.MIN_Y) {
            // The bottom of the world is a floor, see BlockAccess#landingHeight.
            position.y = Constants.MIN_Y;
            velocity.y = 0.0f;
        }
    }

    /**
     * Slows an item that lies on the ground down.
     * <p>
     * The speed a drop is thrown with comes from whoever spawned it, see {@code WorldDrops}, and it fades
     * away within a fraction of a second, which is what makes the item stop after the short slide the
     * original game shows. An item in the air keeps its speed: only what touches the ground is held back.
     *
     * @param delta time since the last frame in seconds
     */
    private void friction(float delta) {
        if (velocity.y != 0.0f) {
            return;
        }
        float keep = (float) Math.pow(SLIDE_PER_SECOND, delta);
        velocity.x *= keep;
        velocity.z *= keep;
        if (velocity.x * velocity.x + velocity.z * velocity.z < 1.0f) {
            velocity.x = 0.0f;
            velocity.z = 0.0f;
        }
    }

    /**
     * {@code true} when the box of this item would reach into a block at a moved place.
     *
     * @param world world the item lies in
     * @param dx distance along the X axis the item would move
     * @param dy distance along the Y axis the item would move
     * @param dz distance along the Z axis the item would move
     * @return {@code true} when the item cannot be there
     */
    private boolean hits(World world, float dx, float dy, float dz) {
        boxOf(position.x + dx, position.y + dy, position.z + dz);
        return world.overlaps(box, shapeBox);
    }

    /**
     * Writes the box of this item at a place.
     * <p>
     * The position of an item is the bottom of its box, the way the position of a player is the height of
     * their feet, so an item that comes to rest stands on the shape it landed on.
     *
     * @param x world X coordinate of the item
     * @param y world Y coordinate of the bottom of the item
     * @param z world Z coordinate of the item
     */
    private void boxOf(float x, float y, float z) {
        float half = Constants.ITEM_SIZE * 0.5f;
        box.set(x - half, y, z - half, x + half, y + Constants.ITEM_SIZE, z + half);
    }

    /**
     * Pushes the item on top of a block it is inside of.
     * <p>
     * A block that is built into the cell an item lies in would have the item inside of it, and a body that is
     * inside a block can never be moved out of it again, because every step of it still overlaps the block.
     * The item is therefore put on top of the shape it is stuck in, which is what a block laid over an item
     * does in the original game as well.
     *
     * @param world world the item lies in
     */
    private void squeezeOut(World world) {
        boxOf(position.x, position.y, position.z);
        if (!world.overlaps(box, shapeBox)) {
            return;
        }
        position.y = world.landingHeight(box, MathUtils.floor(position.y), Float.POSITIVE_INFINITY,
                shapeBox);
        velocity.y = 0.0f;
    }

    /**
     * Draws the item towards the player.
     *
     * @param player player that attracts this item
     * @param delta time since the last frame in seconds
     */
    private void pull(Player player, float delta) {
        towards.set(player.position()).sub(position);
        float distance = towards.len();
        if (distance <= 0.001f) {
            return;
        }
        float step = Math.min(distance, MAGNET_SPEED_BLOCKS * Constants.BLOCK_SIZE * delta);
        position.x += towards.x * step / distance;
        position.z += towards.z * step / distance;
    }

    @Override
    protected void writeData(NbtCompound data) {
        data.putString(SaveTags.ITEM_ID, stack.isEmpty() ? "" : stack.item().name());
        data.putInt(SaveTags.COUNT, stack.count());
        data.putFloat(SaveTags.ENTITY_AGE, age);
        data.putFloat(SaveTags.PICKUP_DELAY, pickupDelay);
    }

    @Override
    protected void readData(NbtCompound data) {
        Item item = SaveTags.itemByName(data.getString(SaveTags.ITEM_ID, ""));
        int count = data.getInt(SaveTags.COUNT, 0);
        stack = item == null || count <= 0 ? ItemStack.EMPTY : ItemStack.of(item, count);
        age = data.getFloat(SaveTags.ENTITY_AGE, 0.0f);
        pickupDelay = data.getFloat(SaveTags.PICKUP_DELAY, DEFAULT_PICKUP_DELAY);
    }

    @Override
    public String toString() {
        return "ItemEntity(" + stack + "@" + position.x + "," + position.y + "," + position.z + ")";
    }
}
