package com.philia093.neofactory.support;

import com.philia093.neofactory.item.Item;
import com.philia093.neofactory.render.BlockIconFactory;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.util.Constants;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Reads the icon of an item the way the game paints it, so a preview shows what the player
 * sees without starting the game.
 * <p>
 * A block is folded into a cube at the size the game folds it, see
 * {@link BlockIconFactory#FOLDED_SIZE}, and shrunk back to the cell that shows it. What the
 * game leaves to the graphics card - a fine texture drawn into a small cell - is averaged
 * here instead, so a preview carries the soft edges of a cube as well and does not show the
 * coarse staircase the icon used to have.
 */
public final class TestIcons {

    /** Assets of the project, the tests run inside the core module. */
    private static final Path ASSETS = Path.of("..", "assets");

    private TestIcons() {
        // Utility class: never instantiated.
    }

    /**
     * Pixels of the icon of an item, row by row, packed as RGBA8888.
     *
     * @param item item whose picture is read
     * @return the pixels of its icon, {@value Constants#ITEM_ICON_SIZE} pixels wide and high
     */
    public static int[] icon(Item item) {
        int cell = Constants.ITEM_ICON_SIZE;
        int[] tile = tile(item, cell);
        if (!isCube(item)) {
            return tile;
        }
        int step = BlockIconFactory.FOLD_SCALE;
        int size = BlockIconFactory.FOLDED_SIZE;
        int[] folded = BlockIconFactory.isometric(
                (x, y) -> tile[(y / step) * cell + (x / step)], size);
        return BlockIconFactory.downscale(folded, size, step);
    }

    /** {@code true} when an item is a block that fills its cell, the ones the game folds. */
    private static boolean isCube(Item item) {
        return item.isBlockItem() && item.block() != null && !item.block().isTransparent();
    }

    /** Reads one frame of the picture of an item, row by row, packed as RGBA8888. */
    private static int[] tile(Item item, int size) {
        Path file = ASSETS.resolve(BlockTextureCache.resolvePath(item.texture()));
        BufferedImage picture;
        try {
            picture = ImageIO.read(file.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read " + file, e);
        }
        int[] tile = new int[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int argb = picture.getRGB(x, y + item.iconFrame() * size);
                tile[y * size + x] = (argb << 8) | (argb >>> 24);
            }
        }
        return tile;
    }
}
