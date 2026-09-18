package com.philia093.neofactory;

import com.badlogic.gdx.Game;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class NeoFactoryGame extends Game {
    public GameScreen gameScreen;
    public static final String WINDOW_NAME = "NeoFactory";

    private static final Logger LOGGER = LogManager.getLogger();
    @Override
    public void create() {
        LOGGER.info("starting");
        gameScreen = new GameScreen(this);
        setScreen(gameScreen);
    }
}
