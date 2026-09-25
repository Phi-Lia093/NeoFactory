package com.philia093.neofactory.item;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.philia093.neofactory.entity.ItemEntity;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.util.Constants;
import com.philia093.neofactory.world.World;

import java.util.Objects;

/**
 * Leaves dropped items lying in the world.
 * <p>
 * This is the implementation the game uses: a broken block hands its items over, an
 * {@link ItemEntity} appears where the block was and the player picks it up by walking
 * over it. Compared with {@link InventoryDrops}, which pushed everything into the
 * inventory right away, this is what makes digging, carrying and a full inventory
 * visible at all.
 * <p>
 * Every drop of a broken block is thrown a little in a random direction, which is what
 * keeps a row of broken blocks from piling all its items up in one point. A stack a
 * player throws leaves the hand along the line of sight instead, see
 * {@link #throwFrom(Player, ItemStack)}.
 */
public class WorldDrops implements ItemDrops {

    /** Slowest speed in blocks per second a dropped stack flies away with. */
    private static final float MIN_SPEED_BLOCKS = 1.0f;

    /** Fastest speed in blocks per second a dropped stack flies away with. */
    private static final float MAX_SPEED_BLOCKS = 3.5f;

    /** Distance in blocks in front of the eyes a thrown stack leaves the hand. */
    private static final float THROW_OFFSET_BLOCKS = 0.4f;

    /** Height in blocks below the eyes a thrown stack leaves the hand, about the height of the hand. */
    private static final float THROW_BELOW_EYES_BLOCKS = 0.3f;

    /** Speed in blocks per second a thrown stack flies away with. */
    private static final float THROW_SPEED_BLOCKS = 10.0f;

    /** Speed in blocks per second a throw carries upwards on top of the line of sight. */
    private static final float THROW_ARC_BLOCKS = 1.0f;

    /** World the items are spawned in. */
    private final World world;

    /**
     * Creates a sink that fills a world with items.
     *
     * @param world world receiving the items
     */
    public WorldDrops(World world) {
        this.world = Objects.requireNonNull(world, "world");
    }

    @Override
    public void drop(ItemStack stack, float worldX, float worldZ) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        int blockX = MathUtils.floor(worldX);
        int blockZ = MathUtils.floor(worldZ);
        ItemEntity entity = new ItemEntity(worldX,
                world.surfaceY(blockX, blockZ) * Constants.BLOCK_SIZE, worldZ, stack);
        float angle = MathUtils.random(0.0f, MathUtils.PI2);
        float speed = MathUtils.random(MIN_SPEED_BLOCKS, MAX_SPEED_BLOCKS) * Constants.BLOCK_SIZE;
        entity.velocity().set(MathUtils.cos(angle) * speed, 0.0f, MathUtils.sin(angle) * speed);
        world.entities().spawn(entity);
    }

    /**
     * Throws a stack out of the hand of a player, along the line of sight.
     * <p>
     * The stack leaves the hand in front of the eyes with the speed of a throw, so what a player drops flies
     * where they look and lands a few blocks away. The line of sight is the only direction a player names with
     * the mouse, and a stack that was merely put down under the feet could not be thrown anywhere. What
     * happens after the throw is up to the world: gravity pulls the stack down, a block stops it and it comes
     * to rest on the shape it lands on, see {@link ItemEntity}.
     *
     * @param player player that throws the stack
     * @param stack items to throw, nothing happens for an empty stack
     */
    public void throwFrom(Player player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Vector3 direction = player.lookDirection(new Vector3()).nor();
        ItemEntity entity = new ItemEntity(
                player.position().x + direction.x * THROW_OFFSET_BLOCKS,
                player.position().y + Constants.PLAYER_EYE_HEIGHT - THROW_BELOW_EYES_BLOCKS,
                player.position().z + direction.z * THROW_OFFSET_BLOCKS,
                stack);
        float speed = THROW_SPEED_BLOCKS * Constants.BLOCK_SIZE;
        entity.velocity().set(direction.x * speed,
                direction.y * speed + THROW_ARC_BLOCKS * Constants.BLOCK_SIZE,
                direction.z * speed);
        world.entities().spawn(entity);
    }

    @Override
    public String toString() {
        return "WorldDrops(" + world + ")";
    }
}
