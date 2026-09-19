package com.philia093.neofactory.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.philia093.neofactory.entity.Player;

/**
 * Translates raw mouse and keyboard state into player actions.
 * <p>
 * The keyboard only produces a movement direction. The mouse is unprojected
 * through the camera, which turns the cursor position into a world position and
 * therefore sets the facing direction of the player.
 * <p>
 * The class extends {@link InputAdapter} because two actions cannot be polled
 * safely: a key that is still held down would toggle fullscreen on every frame
 * and the mouse wheel produces no lasting state at all. Both are therefore
 * collected from events and handed to the game loop through
 * {@link #consumeZoomSteps()} and {@link #consumeFullscreenToggle()}.
 */
public class InputHandler extends InputAdapter {

    /** Movement key for walking towards positive X. */
    private static final int KEY_RIGHT = Input.Keys.D;

    /** Movement key for walking towards negative X. */
    private static final int KEY_LEFT = Input.Keys.A;

    /** Movement key for walking towards positive Y. */
    private static final int KEY_UP = Input.Keys.W;

    /** Movement key for walking towards negative Y. */
    private static final int KEY_DOWN = Input.Keys.S;

    /** Key that requests a graceful exit of the game. */
    private static final int KEY_EXIT = Input.Keys.ESCAPE;

    /** Key that switches between windowed and fullscreen mode. */
    private static final int KEY_FULLSCREEN = Input.Keys.F11;

    /** Reused vector holding the unprojected mouse position. */
    private final Vector3 cursor = new Vector3();

    /** Mouse wheel notches collected since the last frame, positive means zoom in. */
    private float zoomSteps;

    /** Set when the fullscreen key was pressed, cleared by the consumer. */
    private boolean fullscreenToggle;

    /** Set when the exit key was pressed, cleared by the consumer. */
    private boolean exitRequested;

    /**
     * Reads the current input state and applies it to the player.
     *
     * @param player player receiving the movement and looking direction
     * @param camera camera used to unproject the mouse position
     */
    public void update(Player player, OrthographicCamera camera) {
        player.setMoveInput(readAxis(KEY_LEFT, KEY_RIGHT), readAxis(KEY_DOWN, KEY_UP));
        player.lookAt(mouseWorldX(camera), mouseWorldY(camera));
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        // Browsers and touchpads report fractional values, only the direction
        // matters because the zoom is applied in fixed multiplicative steps.
        if (amountY > 0.0f) {
            zoomSteps += 1.0f;
        } else if (amountY < 0.0f) {
            zoomSteps -= 1.0f;
        }
        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == KEY_FULLSCREEN) {
            fullscreenToggle = true;
            return true;
        }
        if (keycode == KEY_EXIT) {
            exitRequested = true;
            return true;
        }
        return false;
    }

    /**
     * Returns the wheel notches collected since the last call and resets them.
     *
     * @return positive when the wheel was rolled forwards, negative otherwise
     */
    public float consumeZoomSteps() {
        float steps = zoomSteps;
        zoomSteps = 0.0f;
        return steps;
    }

    /**
     * Returns and clears the fullscreen request, if the key was pressed.
     *
     * @return {@code true} when the window mode should be switched
     */
    public boolean consumeFullscreenToggle() {
        boolean requested = fullscreenToggle;
        fullscreenToggle = false;
        return requested;
    }

    /**
     * Returns and clears the exit request, if the key was pressed.
     *
     * @return {@code true} when the game should close
     */
    public boolean consumeExitRequest() {
        boolean requested = exitRequested;
        exitRequested = false;
        return requested;
    }

    /**
     * Unprojects the mouse position and returns its world X coordinate.
     *
     * @param camera camera describing the visible world area
     * @return world X coordinate of the mouse cursor
     */
    public float mouseWorldX(OrthographicCamera camera) {
        updateCursor(camera);
        return cursor.x;
    }

    /**
     * Unprojects the mouse position and returns its world Y coordinate.
     *
     * @param camera camera describing the visible world area
     * @return world Y coordinate of the mouse cursor
     */
    public float mouseWorldY(OrthographicCamera camera) {
        updateCursor(camera);
        return cursor.y;
    }

    /**
     * Reads a single movement axis.
     *
     * @param negative key moving towards negative coordinates
     * @param positive key moving towards positive coordinates
     * @return {@code -1}, {@code 0} or {@code 1}
     */
    private float readAxis(int negative, int positive) {
        float axis = 0.0f;
        if (Gdx.input.isKeyPressed(negative)) {
            axis -= 1.0f;
        }
        if (Gdx.input.isKeyPressed(positive)) {
            axis += 1.0f;
        }
        return axis;
    }

    /**
     * Unprojects the current mouse position into world space.
     * <p>
     * The camera uses a viewport that maps screen pixels to world units, so the
     * unprojection has to use the screen size of that viewport.
     *
     * @param camera camera describing the visible world area
     */
    private void updateCursor(OrthographicCamera camera) {
        cursor.set(Gdx.input.getX(), Gdx.input.getY(), 0.0f);
        camera.unproject(cursor);
    }
}
