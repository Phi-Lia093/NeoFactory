package com.philia093.neofactory.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.philia093.neofactory.NeoFactoryGame;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new NeoFactoryGame(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle(NeoFactoryGame.WINDOW_NAME);
        configuration.useVsync(true);
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        configuration.setWindowedMode(1280, 800);
        // The window may be resized and maximized freely, it only refuses to shrink
        // below a size that still shows a useful part of the world.
        configuration.setResizable(true);
        configuration.setWindowSizeLimits(640, 480, -1, -1);
//        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        // The world of cubes draws the faces of thousands of blocks from one texture per block
        // picture, which is what a texture array is for: every picture becomes one layer of a
        // single texture, so a draw call never has to be split by picture and no picture bleeds
        // into its neighbour the way it does in a packed atlas, see the render package.
        //
        // A texture array is a feature of OpenGL 3.0, which is why the game asks for a 3.2 context
        // instead of the 2.0 one libGDX creates by default. That request is also what macOS needs
        // at all, because it only offers core profiles of 3.2 and above; on a driver that cannot
        // answer it the game falls back to the flat renderer, see WorldRenderer.
        configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2);
        return configuration;
    }
}
