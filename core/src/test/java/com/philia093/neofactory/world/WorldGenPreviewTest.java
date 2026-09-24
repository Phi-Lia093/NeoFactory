package com.philia093.neofactory.world;

import com.philia093.neofactory.block.Block;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.fluid.FluidFlow;
import com.philia093.neofactory.support.TestRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Draws a piece of the generated world and writes it to a picture.
 * <p>
 * The test walks a square of blocks and paints three things: the ground of the layer below - the
 * bed of a river, the sand of a desert, the grass of a plain - the water and lava that stand on it,
 * and the ring of sand, gravel and stone that keeps them in. The result lies in
 * {@code core/build/reports/worldgen-preview.png} and is meant to be looked at: a river has to wind,
 * a lake has to be a ragged shape and not a circle, and the ring has to close around both of them.
 * <p>
 * A picture tells what a number cannot, which is why the shape of the terrain is checked this way
 * as well as through the measurements of {@link WorldGenTest}.
 */
class WorldGenPreviewTest {

    /** Seed of the world the picture is drawn from. */
    private static final int SEED = 20260921;

    /** Side of the square the picture shows, in blocks. */
    private static final int SIZE = 320;

    /** Pixels one block is drawn with. */
    private static final int ZOOM = 2;

    /** Picture the preview is written to. */
    private static final Path PREVIEW = Path.of("build", "reports", "worldgen-preview.png");

    @BeforeAll
    static void registerGameData() {
        TestRegistries.ensure();
    }

    @Test
    void writesThePictureOfAGeneratedPatch() throws IOException {
        World world = new World(SEED);
        WorldGen generator = new WorldGen(SEED);
        int centerX = world.spawnX() - SIZE / 2;
        int centerY = world.spawnZ() - SIZE / 2;
        int radius = SIZE / 16 + 2;
        int centerChunkX = Chunk.chunkOf(centerX);
        int centerChunkY = Chunk.chunkOf(centerY);
        for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; chunkX++) {
            for (int chunkY = centerChunkY - radius; chunkY <= centerChunkY + radius; chunkY++) {
                world.loadChunk(chunkX, chunkY);
            }
        }

        BufferedImage image = new BufferedImage(SIZE * ZOOM, SIZE * ZOOM,
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int blockX = centerX + x;
                int blockY = centerY + y;
                int color = colorOf(world.getBlock(blockX, generator.groundY(blockX, blockY), blockY));
                Block above = world.getBlock(blockX, generator.surfaceY(blockX, blockY), blockY);
                if (!above.isAir()) {
                    // The object layer lies on top of the ground, the way the renderer draws it.
                    color = blend(color, colorOf(above), above == Blocks.WATER);
                }
                fill(image, x, y, color);
            }
        }

        Files.createDirectories(PREVIEW.getParent());
        ImageIO.write(image, "png", PREVIEW.toFile());
        assertTrue(Files.exists(PREVIEW), "the picture of the terrain was written: " + PREVIEW);
    }

    /** Paints one block of the picture. */
    private static void fill(BufferedImage image, int x, int y, int color) {
        for (int pixelY = 0; pixelY < ZOOM; pixelY++) {
            for (int pixelX = 0; pixelX < ZOOM; pixelX++) {
                image.setRGB(x * ZOOM + pixelX, y * ZOOM + pixelY, color);
            }
        }
    }

    /**
     * Mixes what stands on a cell into the colour of the ground.
     * <p>
     * Water is drawn the way the world draws it: a colour over the ground that lets it shine
     * through. Lava is opaque and simply covers it.
     *
     * @param ground colour of the ground of the cell
     * @param above colour of what stands in the layer of the player
     * @param water {@code true} when the cell carries water instead of something solid
     * @return the colour of the block of the picture
     */
    private static int blend(int ground, int above, boolean water) {
        if (!water) {
            return above;
        }
        int red = (int) (((above >> 16) & 0xFF) * 0.7f + ((ground >> 16) & 0xFF) * 0.3f);
        int green = (int) (((above >> 8) & 0xFF) * 0.7f + ((ground >> 8) & 0xFF) * 0.3f);
        int blue = (int) ((above & 0xFF) * 0.7f + (ground & 0xFF) * 0.3f);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    /** Colour a block is painted in, the palette the map of the terrain is read with. */
    private static int colorOf(Block block) {
        if (block == Blocks.GRASS) {
            return 0xFF63A83C;
        }
        if (block == Blocks.DIRT) {
            return 0xFF7A5A3A;
        }
        if (block == Blocks.STONE) {
            return 0xFF8C8C8C;
        }
        if (block == Blocks.SAND) {
            return 0xFFE6DC9C;
        }
        if (block == Blocks.SANDSTONE) {
            return 0xFFD5C88F;
        }
        if (block == Blocks.GRAVEL) {
            return 0xFF9C9C98;
        }
        if (block == Blocks.CLAY) {
            return 0xFF9FA8B8;
        }
        if (block == Blocks.SNOW) {
            return 0xFFF2F7FA;
        }
        if (block == Blocks.WATER) {
            return 0xFF2F5FE8;
        }
        if (block == Blocks.LAVA) {
            return 0xFFFF6A1E;
        }
        if (block == Blocks.COAL_ORE) {
            return 0xFF3A3A3A;
        }
        if (block == Blocks.IRON_ORE) {
            return 0xFFC9A183;
        }
        if (block.isAir()) {
            return 0xFF202020;
        }
        return 0xFF707070;
    }
}
