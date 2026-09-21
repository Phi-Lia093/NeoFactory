package com.philia093.neofactory.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.philia093.neofactory.NeoFactoryGame;
import com.philia093.neofactory.gui.panel.PanelTextures;
import com.philia093.neofactory.gui.widget.ScrollListWidget;
import com.philia093.neofactory.gui.widget.TextFieldWidget;
import com.philia093.neofactory.gui.widget.Widget;
import com.philia093.neofactory.gui.widget.WidgetStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Base class for every screen that is built from {@link Widget widgets}.
 * <p>
 * The class owns what every menu needs and no menu should repeat: the list of its
 * widgets, the sprite batch they are drawn with, the mouse position in interface
 * pixels and the input adapter that offers the mouse and the keyboard to the
 * widgets.
 * <p>
 * Coordinates come from {@link com.philia093.neofactory.gui.GuiViewport}, the same
 * viewport everything is drawn with, so a click lands on the widget the player sees
 * in every window size, in fullscreen and at every interface scale.
 * <p>
 * Keyboard input goes to the focused {@link TextFieldWidget}, if there is one: a
 * press on a field focuses it, a press anywhere else drops the focus. A screen that
 * needs a key of its own overrides {@link #onKeyDown(int)}.
 */
public abstract class WidgetScreen extends NeoFactoryScreen {

    private final SpriteBatch batch = new SpriteBatch();
    private final List<Widget> widgets = new ArrayList<>();
    private final Vector2 mouse = new Vector2();

    /** Style of the widgets, created once per screen from the shared pictures. */
    private final WidgetStyle style;

    /** Field that takes the keyboard, {@code null} while none is focused. */
    private TextFieldWidget focusedField;

    /** Size one tile of the menu background is drawn with, in interface pixels. */
    private static final float MENU_TILE = PanelTextures.DIRT_SIZE;

    /** Colour the tiles of the menu background are darkened with. */
    private static final Color MENU_SHADE = new Color(0.25f, 0.25f, 0.25f, 1.0f);

    /** Tile the menu background is made of, {@code null} when the picture is missing. */
    private final TextureRegion menuBackground;

    /** Adapter offering the mouse and the keyboard to the widgets. */
    private final InputAdapter widgetInput = new InputAdapter() {

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            updateMouse(screenX, screenY);
            // The last widget added is drawn on top, so it is asked first.
            for (int index = widgets.size() - 1; index >= 0; index--) {
                Widget widget = widgets.get(index);
                if (widget.touchDown(mouse.x, mouse.y, button)) {
                    focusOn(widget);
                    onWidgetPressed(widget);
                    return true;
                }
            }
            focusOn(null);
            return false;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            updateMouse(screenX, screenY);
            boolean consumed = false;
            for (int index = widgets.size() - 1; index >= 0; index--) {
                if (widgets.get(index).touchUp(mouse.x, mouse.y, button)) {
                    consumed = true;
                }
            }
            return consumed;
        }

        @Override
        public boolean keyDown(int keyCode) {
            if (onKeyDown(keyCode)) {
                return true;
            }
            return focusedField != null && focusedField.handleKeyDown(keyCode);
        }

        @Override
        public boolean keyTyped(char character) {
            return focusedField != null && focusedField.handleKeyTyped(character);
        }

        @Override
        public boolean scrolled(float amountX, float amountY) {
            // The wheel belongs to the list the mouse is over.
            for (int index = widgets.size() - 1; index >= 0; index--) {
                Widget widget = widgets.get(index);
                if (widget instanceof ScrollListWidget && widget.contains(mouse.x, mouse.y)) {
                    ((ScrollListWidget) widget).scroll(-amountY);
                    return true;
                }
            }
            return false;
        }
    };

    /**
     * Creates a screen.
     *
     * @param game game instance owning this screen
     */
    protected WidgetScreen(NeoFactoryGame game) {
        super(game);
        this.style = new WidgetStyle(game.textures());
        this.menuBackground = new PanelTextures(game.textures()).dirt();
    }

    /** Style providing the button and text field pictures. */
    protected WidgetStyle widgetStyle() {
        return style;
    }

    /**
     * Draws the dark tiled background the menus of the original game use.
     * <p>
     * The picture is a single 16 by 16 dirt tile that is repeated until the interface
     * is filled and drawn grey, which is what makes a menu readable without hiding
     * that there is a game behind it.
     */
    protected void drawMenuBackground() {
        TextureRegion tile = menuBackground;
        if (tile == null) {
            return;
        }
        float width = uiViewport.guiWidth();
        float height = uiViewport.guiHeight();
        batch.setColor(MENU_SHADE);
        for (float x = 0.0f; x < width; x += MENU_TILE) {
            for (float y = 0.0f; y < height; y += MENU_TILE) {
                batch.draw(tile, x, y, MENU_TILE, MENU_TILE);
            }
        }
        batch.setColor(Color.WHITE);
    }

    /**
     * Mouse position in interface pixels, updated every frame.
     *
     * @return the live position, do not keep a reference to it
     */
    protected Vector2 mouse() {
        return mouse;
    }

    /**
     * Sprite batch the widgets and the background are drawn with.
     * <p>
     * A screen that overrides {@code drawContent} draws into this batch: it is
     * already started and switched to the projection of the interface.
     */
    protected SpriteBatch batch() {
        return batch;
    }

    /**
     * Adds a widget to the screen.
     *
     * @param widget widget to add, drawn and asked in the order it was added
     * @param <T> type of the widget
     * @return the added widget, so a screen can keep it in a field
     */
    protected <T extends Widget> T addWidget(T widget) {
        widgets.add(Objects.requireNonNull(widget, "widget"));
        return widget;
    }

    /**
     * Places the widgets of the screen.
     * <p>
     * Called after the screen is shown and whenever the window changed its size,
     * because both change the size of the interface in virtual pixels.
     */
    protected abstract void layoutWidgets();

    /**
     * Draws whatever lies behind the widgets.
     *
     * @param delta time since the last frame in seconds
     * @param mouseX X coordinate of the mouse inside the interface
     * @param mouseY Y coordinate of the mouse inside the interface, from the bottom
     */
    protected void drawContent(float delta, float mouseX, float mouseY) {
        // Most screens draw nothing but their widgets.
    }

    /**
     * Handles a key the screen itself is interested in.
     *
     * @param keyCode key code, see {@link Input.Keys}
     * @return {@code true} when the key was consumed
     */
    protected boolean onKeyDown(int keyCode) {
        return false;
    }

    /**
     * Called after a widget consumed a mouse press.
     * <p>
     * A screen uses this to react to a change the widget made, for example to enable
     * the buttons that work on the row a list just selected.
     *
     * @param widget widget that was pressed
     */
    protected void onWidgetPressed(Widget widget) {
        // Most screens do not react to a press.
    }

    @Override
    public void show() {
        // The widgets are asked before the stage, which no menu of this game uses so far.
        Gdx.input.setInputProcessor(new InputMultiplexer(widgetInput, stage));
        layoutWidgets();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        layoutWidgets();
    }

    @Override
    public void render(float delta) {
        updateMouse(Gdx.input.getX(), Gdx.input.getY());
        for (Widget widget : widgets) {
            widget.update(delta);
        }

        clearScreen();
        uiViewport.apply();
        batch.setProjectionMatrix(uiViewport.getCamera().combined);
        batch.begin();
        drawContent(delta, mouse.x, mouse.y);
        for (Widget widget : widgets) {
            widget.render(batch, mouse.x, mouse.y);
        }
        batch.end();
        batch.setColor(Color.WHITE);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void dispose() {
        batch.dispose();
        super.dispose();
    }

    /**
     * Offers the focus to a widget.
     *
     * @param widget widget that was pressed, {@code null} to drop the focus
     */
    private void focusOn(Widget widget) {
        TextFieldWidget field = widget instanceof TextFieldWidget ? (TextFieldWidget) widget : null;
        if (focusedField == field) {
            return;
        }
        if (focusedField != null) {
            focusedField.setFocused(false);
        }
        focusedField = field;
        if (focusedField != null) {
            focusedField.setFocused(true);
        }
    }

    /**
     * Converts a point of the window into the virtual pixels of the interface.
     *
     * @param screenX X coordinate of the window
     * @param screenY Y coordinate of the window, measured from the top
     */
    private void updateMouse(int screenX, int screenY) {
        uiViewport.unproject(screenX, screenY, mouse);
    }
}
