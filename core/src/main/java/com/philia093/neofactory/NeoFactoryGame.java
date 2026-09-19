package com.philia093.neofactory;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.philia093.neofactory.block.BlockRegistry;
import com.philia093.neofactory.block.Blocks;
import com.philia093.neofactory.render.BlockTextureCache;
import com.philia093.neofactory.screen.ScreenManager;
import com.philia093.neofactory.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point of the game.
 * <p>
 * The game owns the resources shared by every screen: the {@link AssetManager},
 * the texture cache and the {@link ScreenManager}. Screens never dispose those
 * resources themselves, the game releases them once the application shuts down.
 */
public class NeoFactoryGame extends Game {

    /** Window name, kept here because the launcher reads it before startup. */
    public static final String WINDOW_NAME = Constants.WINDOW_NAME;

    private static final Logger LOGGER = LogManager.getLogger();

    private AssetManager assets;
    private BlockTextureCache textures;
    private ScreenManager screenManager;

    @Override
    public void create() {
        LOGGER.info("Starting {}", WINDOW_NAME);

        assets = new AssetManager();
        textures = new BlockTextureCache(assets);

        // Every block must exist before a world is created.
        Blocks.registerAll();
        LOGGER.info("Registered {} block types, next free id is {}",
                BlockRegistry.count(), Blocks.NEXT_FREE_ID);

        screenManager = new ScreenManager(this);
        screenManager.show(ScreenManager.ScreenType.GAME);
    }

    /** Shared asset manager, screens load their pictures through it. */
    public AssetManager assets() {
        return assets;
    }

    /** Texture cache providing the block and map regions. */
    public BlockTextureCache textures() {
        return textures;
    }

    /** Manager creating and switching the screens of the game. */
    public ScreenManager screens() {
        return screenManager;
    }

    /**
     * Applies the pixel art look to a texture.
     * <p>
     * Nearest neighbour filtering keeps the 16 by 16 block textures crisp and
     * disables the blur that bilinear filtering would introduce.
     *
     * @param texture texture to configure
     */
    public static void applyPixelArtSettings(Texture texture) {
        texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (screenManager != null) {
            screenManager.dispose();
            screenManager = null;
        }
        if (assets != null) {
            assets.dispose();
            assets = null;
        }
        LOGGER.info("Shutdown complete");
    }
}