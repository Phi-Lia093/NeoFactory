package com.philia093.neofactory.entity;

import com.badlogic.gdx.math.Vector2;
import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.item.ItemStack;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.util.nbt.NbtCompound;
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

    /** Reused vector pointing from the item to the player. */
    private final Vector2 towards = new Vector2();

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
     * @param y world Y coordinate of the item
     * @param stack items this entity stands for, copied by the constructor
     */
    public ItemEntity(float x, float y, ItemStack stack) {
        super(EntityTypes.ITEM);
        position.set(x, y);
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
        return Constants.ITEM_ICON_SIZE * 0.25f;
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

        slide(delta);

        Player player = world.entities().player();
        if (player == null || pickupDelay > 0.0f) {
            return;
        }
        if (!player.inventory().hasRoomFor(stack)) {
            // A full inventory attracts nothing: an item that was already flying towards the
            // player stops where it is and waits until a place is free again.
            return;
        }
        float magnetRange = MAGNET_RANGE_BLOCKS * Constants.TILE_SIZE;
        if (player.position().dst2(position) <= magnetRange * magnetRange) {
            pull(player, delta);
        }
        float range = PICKUP_RANGE_BLOCKS * Constants.TILE_SIZE;
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
     * Moves the item by its velocity and lets it come to rest.
     * <p>
     * The speed a drop is thrown with comes from whoever spawned it, see
     * {@code WorldDrops}. It fades away within a fraction of a second, which is what makes
     * the item stop after the short slide the original game shows.
     *
     * @param delta time since the last frame in seconds
     */
    private void slide(float delta) {
        if (velocity.isZero()) {
            return;
        }
        position.add(velocity.x * delta, velocity.y * delta);
        velocity.scl((float) Math.pow(SLIDE_PER_SECOND, delta));
        if (velocity.len2() < 1.0f) {
            velocity.setZero();
        }
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
        float step = Math.min(distance, MAGNET_SPEED_BLOCKS * Constants.TILE_SIZE * delta);
        position.add(towards.scl(step / distance));
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
        return "ItemEntity(" + stack + "@" + position.x + "," + position.y + ")";
    }
}
