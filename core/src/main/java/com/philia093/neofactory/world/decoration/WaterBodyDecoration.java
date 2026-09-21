package com.philia093.neofactory.world.decoration;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.fluid.FluidState;
import com.philia093.neofactory.world.Biome;
import com.philia093.neofactory.world.Noise;
import com.philia093.neofactory.world.World;

import java.util.Random;

/**
 * The water of a river and of a lake, standing on the bed the generator laid out for it.
 * <p>
 * Every cell of the water is a source and every cell of the ring around it is solid, which is
 * what keeps a lake a lake instead of a spill that is on its way out. See
 * {@link #isWater(TerrainSampler, int, int)} for how a body is recognized and
 * {@link #isRim(TerrainSampler, int, int)} for why the ring has no gap.
 * <p>
 * The bed itself is not written here: it belongs to the ground layer and comes from
 * {@code WorldGen#floorAt(int, int)}, which lays gravel, clay and sand under every cell of a
 * river and of a lake.
 */
public class WaterBodyDecoration extends Decoration {

    /** Share of the ring that is gravel, the rest of it is sand. */
    private static final float RIM_GRAVEL_SHARE = 0.3f;

    private final int seed;

    /**
     * Creates the decorator.
     *
     * @param seed world seed, hashed into the material of the ring
     */
    public WaterBodyDecoration(int seed) {
        super("water_body");
        this.seed = seed;
    }

    @Override
    public boolean shouldPlaceAt(TerrainSampler terrain, int x, int y) {
        return isWater(terrain, x, y) || isRim(terrain, x, y);
    }

    @Override
    public void place(World world, TerrainSampler terrain, int x, int y, Random random) {
        if (isWater(terrain, x, y)) {
            // A source: the water of a lake is not a spill that runs out, and the ring around it
            // is what holds it, see FluidFlow. Written only into an empty cell, so that a
            // decoration that came before - or a block the player put there - keeps the cell.
            world.placeObjectIfAir(x, y, Blocks.WATER, FluidState.SOURCE.pack());
            return;
        }
        world.placeObjectIfAir(x, y, rimBlock(x, y));
    }

    /**
     * {@code true} when the cell lies under the open water of a river or a lake.
     * <p>
     * The generator decides this through the biome: a river and a lake are biomes of their own,
     * picked from the contour line of a noise field before the field of the ordinary biomes is
     * asked, see {@code WorldGen#biomeAt(int, int)}. A decorator only sees the terrain through
     * {@link TerrainSampler}, so this is exactly the one question it needs to ask - and the
     * answer is a pure function of the seed and the coordinates, which keeps on demand chunk
     * generation reproducible.
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

    /**
     * {@code true} when the cell touches the water of a body and has to be filled up.
     * <p>
     * <b>Why the ring is needed at all:</b> a fluid of the layer of the player stands in every
     * empty cell around it and floods soft blocks such as tall grass, see
     * {@link com.philia093.neofactory.fluid.FluidFlow}. Water without a rim would therefore run
     * out of its lake until nothing of it was left - or until it had drowned the landscape. The
     * cells right around the water are filled with sand and gravel instead, and a solid block
     * holds a fluid back.
     * <p>
     * <b>Why no gap can appear:</b> the neighbours of a cell are asked, not a second noise
     * threshold. A threshold would hand out a ring of one cell in most places and of no cells
     * where the noise happens to be steep, and a single missing cell is a hole a lake runs
     * through. Asking the four neighbours makes the ring complete by construction - a diagonal
     * pair of water cells still has both of its corner cells rimmed, because each of them
     * touches one of the two.
     *
     * @param terrain read only view on the generated terrain
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return {@code true} when the cell has to be filled to hold a body of water
     */
    private static boolean isRim(TerrainSampler terrain, int x, int y) {
        if (isWater(terrain, x, y) || terrain.isLavaPoolAt(x, y)) {
            // A cell of a pool belongs to the burn: its own decorator owns it, and a bank of sand
            // there would have covered the lava over.
            return false;
        }
        return isWater(terrain, x + 1, y) || isWater(terrain, x - 1, y)
                || isWater(terrain, x, y + 1) || isWater(terrain, x, y - 1);
    }

    /**
     * Material of the ring at a cell.
     *
     * @param x block coordinate along the first horizontal axis
     * @param y block coordinate along the second horizontal axis
     * @return gravel or sand, the two materials a bank of a river is made of
     */
    private Block rimBlock(int x, int y) {
        return Noise.hash(x, y, seed) < RIM_GRAVEL_SHARE ? Blocks.GRAVEL : Blocks.SAND;
    }
}
