package com.philia093.neofactory.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.philia093.neofactory.NeoFactoryGame;
import com.philia093.neofactory.gui.MenuLayout;
import com.philia093.neofactory.gui.widget.ButtonWidget;
import com.philia093.neofactory.gui.widget.LabelWidget;
import com.philia093.neofactory.render.BlockTextureCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main menu of the game.
 * <p>
 * The screen is built from widgets like every other menu: the title, a subtitle,
 * the buttons {@code Singleplayer} and {@code Exit} and the version in the corner.
 * <p>
 * Behind the menu the panorama of the art pack scrolls past. Its pictures are
 * {@code gui/title/background/panorama_0} to {@code panorama_5}, drawn side by side
 * and repeated until the interface is filled; the strip slides slowly, which is the
 * trick the original game uses as well. A dark layer on top of it keeps the text
 * readable.
 */
public class TitleScreen extends WidgetScreen {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Text of the button that leads to the world list. */
    private static final String SINGLEPLAYER = "Singleplayer";

    /** Text of the button that closes the game. */
    private static final String EXIT = "Exit";

    /** Subtitle below the name of the game. */
    private static final String SUBTITLE = "Java Edition";

    /** Version line shown in the lower left corner. */
    private static final String VERSION = "NeoFactory 1.0";

    /** Amount of panorama pictures in the art pack. */
    private static final int PANORAMA_COUNT = 6;

    /** Name of the panorama pictures, without the frame number and extension. */
    private static final String PANORAMA_FORMAT = BlockTextureCache.GUI_FOLDER
            + "title/background/panorama_%d";

    /** Interface pixels the panorama slides per second. */
    private static final float PANORAMA_SPEED = 4.0f;

    /** Colour laid over the panorama so the text stays readable. */
    private static final Color SHADE = new Color(0.0f, 0.0f, 0.0f, 0.45f);

    /** Colour of the subtitle under the title. */
    private static final Color SUBTITLE_COLOR = new Color(1.0f, 1.0f, 0.55f, 1.0f);

    /** Colour of the version line. */
    private static final Color VERSION_COLOR = new Color(0.7f, 0.7f, 0.7f, 1.0f);

    /** Size of the title text. */
    private static final float TITLE_SCALE = 3.0f;

    private final TextureRegion[] panorama = new TextureRegion[PANORAMA_COUNT];
    private final LabelWidget title;
    private final LabelWidget subtitle;
    private final ButtonWidget singleplayer;
    private final ButtonWidget exit;
    private final LabelWidget version;

    /** Interface pixels the panorama is scrolled by. */
    private float panoramaOffset;

    /**
     * Creates the title screen.
     *
     * @param game game instance owning this screen
     */
    public TitleScreen(NeoFactoryGame game) {
        super(game);
        for (int frame = 0; frame < panorama.length; frame++) {
            panorama[frame] = game.textures().region(String.format(PANORAMA_FORMAT, frame));
        }

        this.title = addWidget(new LabelWidget(game.font(), NeoFactoryGame.WINDOW_NAME, true));
        this.title.setScale(TITLE_SCALE);
        this.subtitle = addWidget(new LabelWidget(game.font(), SUBTITLE, true));
        this.subtitle.color().set(SUBTITLE_COLOR);
        this.singleplayer = addWidget(new ButtonWidget(game.font(), widgetStyle(), SINGLEPLAYER,
                this::openWorldList));
        this.exit = addWidget(new ButtonWidget(game.font(), widgetStyle(), EXIT, this::exitGame));
        this.version = addWidget(new LabelWidget(game.font(), VERSION, true));
        this.version.color().set(VERSION_COLOR);
    }

    /** Shows the list of stored worlds. */
    private void openWorldList() {
        LOGGER.info("Singleplayer requested from the title screen");
        game.screens().show(ScreenManager.ScreenType.WORLD_SELECT);
    }

    /** Closes the game. */
    private void exitGame() {
        LOGGER.info("Exit requested from the title screen");
        Gdx.app.exit();
    }

    @Override
    protected void layoutWidgets() {
        float width = uiViewport.guiWidth();
        float height = uiViewport.guiHeight();
        float smallHeight = game.font().glyphHeight();
        float buttonX = MenuLayout.centeredX(width, MenuLayout.BUTTON_WIDTH);

        title.layout(buttonX, height * 0.68f, MenuLayout.BUTTON_WIDTH, title.textHeight());
        subtitle.layout(buttonX, height * 0.68f - smallHeight - MenuLayout.LABEL_GAP,
                MenuLayout.BUTTON_WIDTH, smallHeight);

        float buttonsCenter = height * 0.34f;
        singleplayer.layout(buttonX, MenuLayout.stackY(buttonsCenter, 0, 2),
                MenuLayout.BUTTON_WIDTH, MenuLayout.BUTTON_HEIGHT);
        exit.layout(buttonX, MenuLayout.stackY(buttonsCenter, 1, 2),
                MenuLayout.BUTTON_WIDTH, MenuLayout.BUTTON_HEIGHT);

        version.layout(4.0f, 4.0f, MenuLayout.BUTTON_WIDTH, smallHeight);
    }

    @Override
    protected void drawContent(float delta, float mouseX, float mouseY) {
        drawPanorama(delta);
    }

    /**
     * Draws the sliding panorama and the dark layer over it.
     *
     * @param delta time since the last frame in seconds
     */
    private void drawPanorama(float delta) {
        float width = uiViewport.guiWidth();
        float height = uiViewport.guiHeight();
        if (panorama[0] == null) {
            // Without the pictures the plain sky colour stays and the menu still works.
            return;
        }

        // One picture is drawn as a square covering the height of the interface.
        float pictureWidth = height;
        panoramaOffset += PANORAMA_SPEED * delta;
        if (panoramaOffset >= pictureWidth) {
            panoramaOffset -= pictureWidth;
        }

        for (int index = 0; index < panorama.length; index++) {
            TextureRegion picture = panorama[index];
            if (picture == null) {
                continue;
            }
            for (float x = index * pictureWidth - panoramaOffset; x < width;
                    x += pictureWidth * panorama.length) {
                batch().draw(picture, x, 0.0f, pictureWidth, height);
            }
        }

        TextureRegion pixel = widgetStyle().whitePixel();
        if (pixel != null) {
            batch().setColor(SHADE);
            batch().draw(pixel, 0.0f, 0.0f, width, height);
            batch().setColor(Color.WHITE);
        }
    }
}
