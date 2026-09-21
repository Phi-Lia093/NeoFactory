package com.philia093.neofactory.item;

import com.badlogic.gdx.math.MathUtils;
import com.philia093.neofactory.entity.ItemEntity;
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
 * Every drop is thrown a little in a random direction, which is what keeps a row of
 * broken blocks from piling all its items up in one point.
 */
public class WorldDrops implements ItemDrops {

    /** Slowest speed in blocks per second a dropped stack flies away with. */
    private static final float MIN_SPEED_BLOCKS = 1.0f;

    /** Fastest speed in blocks per second a dropped stack flies away with. */
    private static final float MAX_SPEED_BLOCKS = 3.5f;

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
    public void drop(ItemStack stack, float worldX, float worldY) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        ItemEntity entity = new ItemEntity(worldX, worldY, stack);
        float angle = MathUtils.random(0.0f, MathUtils.PI2);
        float speed = MathUtils.random(MIN_SPEED_BLOCKS, MAX_SPEED_BLOCKS) * Constants.TILE_SIZE;
        entity.velocity().set(MathUtils.cos(angle), MathUtils.sin(angle)).scl(speed);
        world.entities().spawn(entity);
    }

    @Override
    public String toString() {
        return "WorldDrops(" + world + ")";
    }
}
