package com.philia093.neofactory.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.philia093.neofactory.NeoFactoryGame;
import com.philia093.neofactory.gui.GuiViewport;
import com.philia093.neofactory.util.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base class for every screen of the game.
 * <p>
 * It owns the UI stage shared by all screens and forwards the resize event to
 * it. Subclasses only implement the rendering and the input handling they need,
 * which keeps the screen classes small and consistent.
 */
public abstract class NeoFactoryScreen extends ScreenAdapter {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Game instance giving access to the asset manager and the screen manager. */
    protected final NeoFactoryGame game;

    /** Stage holding the user interface actors of this screen. */
    protected final Stage stage;

    /**
     * Viewport of the user interface, shared by the stage and by everything the
     * screens draw themselves.
     * <p>
     * It blows the interface up by a whole number, see {@link GuiViewport}. Text and
     * slots stay readable on a large window, and because drawing and hit testing both
     * run through this viewport a click lands on the slot the player sees, also in
     * fullscreen or after the window was resized.
     */
    protected final GuiViewport uiViewport;

    /** Window width to restore when leaving fullscreen mode, {@code -1} while unknown. */
    private int windowedWidth = -1;

    /** Window height to restore when leaving fullscreen mode, {@code -1} while unknown. */
    private int windowedHeight = -1;

    /**
     * Creates a screen.
     *
     * @param game game instance owning this screen
     */
    protected NeoFactoryScreen(NeoFactoryGame game) {
        this.game = game;
        this.uiViewport = new GuiViewport();
        this.stage = new Stage(uiViewport);
    }

    /**
     * Makes this screen receive the UI input events.
     * <p>
     * Screens that read raw keyboard and mouse state may install a multiplexer
     * instead, so that both the stage and their own handler receive events.
     */
    protected void activateUiInput() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void resize(int width, int height) {
        // The camera is kept as is, only the viewport is updated. Centring it on the
        // new size is what keeps the interface in the middle of the window after a
        // resize or after switching to fullscreen.
        uiViewport.update(width, height, true);
    }

    @Override
    public void show() {
        activateUiInput();
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void pause() {
        // Nothing to do yet, kept for future save on pause support.
    }

    @Override
    public void resume() {
        // Nothing to do yet.
    }

    /** Clears the frame with the sky color. */
    protected void clearScreen() {
        Gdx.gl.glClearColor(Constants.SKY_RED, Constants.SKY_GREEN, Constants.SKY_BLUE, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    /** {@code true} while the window covers the whole screen. */
    public boolean isFullscreen() {
        return Gdx.graphics.isFullscreen();
    }

    /**
     * Switches between windowed and fullscreen mode.
     * <p>
     * The windowed size before switching is remembered, so leaving fullscreen
     * restores the size the player had chosen. libGDX fires a resize event after
     * the mode changed, which is why both viewports are updated automatically.
     */
    public void toggleFullscreen() {
        Graphics graphics = Gdx.graphics;
        if (graphics.isFullscreen()) {
            int width = windowedWidth > 0 ? windowedWidth : graphics.getWidth();
            int height = windowedHeight > 0 ? windowedHeight : graphics.getHeight();
            graphics.setWindowedMode(width, height);
        } else {
            windowedWidth = graphics.getWidth();
            windowedHeight = graphics.getHeight();
            graphics.setFullscreenMode(graphics.getDisplayMode());
        }
        LOGGER.info("Window mode changed, fullscreen is now {}", graphics.isFullscreen());
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}