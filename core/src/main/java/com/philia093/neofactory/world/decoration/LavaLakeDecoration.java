package com.philia093.neofactory.world.decoration;

import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.fluid.FluidState;
import com.philia093.neofactory.world.Biome;
import com.philia093.neofactory.world.World;

import java.util.Random;

/**
 * A pool of lava, a decoration rather than a biome.
 * <p>
 * <b>Where the lava lies is a field of the terrain</b>, see
 * {@code WorldGen#isLavaPoolAt(int, int)}: the ground under a pool has to know that it is scorched,
 * or everything that plants on the ground would be planted in the burn - which is exactly what
 * happened while the field lived here and a tree grew out of the lava. What this decorator owns is
 * the look: the lava itself and the ring of stone around it.
 * <p>
 * A pool keeps away from a river and a lake: a body of water and a burn share no cell, and the two
 * decorators would otherwise write over each other. The biome tells them apart.
 * <p>
 * <b>Why a pool is rimmed:</b> lava of the layer of the player stands in every empty cell around
 * it, so it would run out of its pool without a wall. A ring of stone holds it in - and keeps a
 * player out of it, which is the one thing lava is good at.
 */
public class LavaLakeDecoration extends Decoration {

    /** Creates the decorator. */
    public LavaLakeDecoration() {
        super("lava_lake");
    }

    @Override
    public boolean shouldPlaceAt(TerrainSampler terrain, int x, int y) {
        return isPool(terrain, x, y) || isRim(terrain, x, y);
    }

    @Override
    public void place(World world, TerrainSampler terrain, int x, int y, Random random) {
        if (isPool(terrain, x, y)) {
            // A source, held in place by the ring of stone around it. Written only into an empty
            // cell, so that a body of water or a block of the player keeps the cell it owns.
            world.placeObjectIfAir(x, y, Blocks.LAVA, FluidState.SOURCE.pack());
            return;
        }
        world.placeObjectIfAir(x, y, Blocks.STONE);
    }

    /**
     * {@code true} when the cell lies under the lava of a pool.
     * <p>
     * A pool never covers a river or a lake: the field behind a pool does not care where it is, so
     * without this the glow of a burn would be poured over the water of a lake.
     *
     * @param terrain read only view on the generated terrain
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return {@code true} when lava belongs into that cell
     */
    private static boolean isPool(TerrainSampler terrain, int x, int y) {
        return terrain.isLavaPoolAt(x, y) && !isWater(terrain, x, y);
    }

    /**
     * {@code true} when the cell touches a pool and has to be walled off.
     * <p>
     * A cell of a body of water is left alone even when it touches a pool: it belongs to the water,
     * whose own decorator fills it, and a wall of stone there would have taken the water away.
     * <p>
     * The four neighbours are asked rather than a second noise threshold, see
     * {@link WaterBodyDecoration#isRim(TerrainSampler, int, int)} for why a threshold would leave a
     * gap for the lava to run through.
     *
     * @param terrain read only view on the generated terrain
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return {@code true} when the cell has to be walled off
     */
    private static boolean isRim(TerrainSampler terrain, int x, int y) {
        if (isPool(terrain, x, y) || isWater(terrain, x, y)) {
            return false;
        }
        return isPool(terrain, x + 1, y) || isPool(terrain, x - 1, y)
                || isPool(terrain, x, y + 1) || isPool(terrain, x, y - 1);
    }

    /**
     * {@code true} when the cell lies under a river or a lake.
     *
     * @param terrain read only view on the generated terrain
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return {@code true} when water belongs into that cell
     */
    private static boolean isWater(TerrainSampler terrain, int x, int y) {
        Biome biome = terrain.biomeAt(x, y);
        return biome == Biome.RIVER || biome == Biome.LAKE;
    }
}
