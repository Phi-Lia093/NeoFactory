package com.philia093.neofactory.chat;

import java.util.ArrayList;
import java.util.List;

/**
 * The line the player types into, opened with {@code /} or {@code T}.
 * <p>
 * The prompt only holds text, a cursor position and the lines that were submitted
 * before. It does not know about the world and it draws nothing, so a game loop and
 * a unit test drive it the very same way, see
 * {@link com.philia093.neofactory.gui.ChatOverlay} for the drawing part.
 * <p>
 * The most recent submissions are remembered, which the arrow up and arrow down
 * keys walk through - a line a player typed before can be repeated or corrected
 * without typing it again.
 */
public final class ChatPrompt {

    /** Amount of characters a line may hold. */
    public static final int MAX_LENGTH = 256;

    /** Amount of submitted lines that stay in the history. */
    public static final int HISTORY_LIMIT = 50;

    private final StringBuilder text = new StringBuilder();
    private final List<String> history = new ArrayList<>();

    /** Text of the line before the player started to walk through the history. */
    private String draft = "";

    /** Index into {@link #history}, {@code -1} while the player types a fresh line. */
    private int historyIndex = -1;

    /** Position of the cursor inside {@link #text}, from {@code 0} to its length. */
    private int cursor;

    private boolean open;

    /**
     * {@code true} while the character of the key that opened the line is thrown away.
     * <p>
     * The {@code T} key opens the chat and produces the letter {@code t} in the same
     * press, which must not land in the field. The {@code /} key is the other way
     * round: there the slash is what the player wants to see, so it is kept.
     */
    private boolean discardNextCharacter;

    /**
     * Opens the line.
     *
     * @param discardFirstCharacter {@code true} to drop the character of the opening
     *                              key, see {@link #discardNextCharacter}
     */
    public void open(boolean discardFirstCharacter) {
        text.setLength(0);
        cursor = 0;
        historyIndex = -1;
        draft = "";
        discardNextCharacter = discardFirstCharacter;
        open = true;
    }

    /** Closes the line and forgets what was typed into it. */
    public void cancel() {
        text.setLength(0);
        cursor = 0;
        historyIndex = -1;
        draft = "";
        discardNextCharacter = false;
        open = false;
    }

    /** {@code true} while the player types. */
    public boolean isOpen() {
        return open;
    }

    /** Text the player typed so far. */
    public String text() {
        return text.toString();
    }

    /** Position of the cursor inside {@link #text()}, counted in characters. */
    public int cursor() {
        return cursor;
    }

    /**
     * Adds a character at the cursor.
     *
     * @param character character the keyboard produced, control characters are ignored
     */
    public void type(char character) {
        if (discardNextCharacter) {
            discardNextCharacter = false;
            return;
        }
        if (character < ' ' || text.length() >= MAX_LENGTH) {
            return;
        }
        text.insert(cursor, character);
        cursor++;
    }

    /** Removes the character left of the cursor. */
    public void backspace() {
        if (cursor > 0) {
            text.deleteCharAt(cursor - 1);
            cursor--;
        }
    }

    /** Removes the character right of the cursor. */
    public void delete() {
        if (cursor < text.length()) {
            text.deleteCharAt(cursor);
        }
    }

    /** Moves the cursor one character to the left. */
    public void moveLeft() {
        if (cursor > 0) {
            cursor--;
        }
    }

    /** Moves the cursor one character to the right. */
    public void moveRight() {
        if (cursor < text.length()) {
            cursor++;
        }
    }

    /** Moves the cursor in front of the line. */
    public void moveToStart() {
        cursor = 0;
    }

    /** Moves the cursor behind the line. */
    public void moveToEnd() {
        cursor = text.length();
    }

    /**
     * Shows the line that was submitted before.
     * <p>
     * The first step keeps what is currently typed as the draft, so walking back
     * down returns to it instead of losing it.
     */
    public void showPrevious() {
        if (history.isEmpty()) {
            return;
        }
        if (historyIndex < 0) {
            draft = text();
            historyIndex = history.size();
        }
        if (historyIndex > 0) {
            historyIndex--;
        }
        setText(history.get(historyIndex));
    }

    /** Shows the next, more recent line of the history. */
    public void showNext() {
        if (historyIndex < 0) {
            return;
        }
        historyIndex++;
        if (historyIndex >= history.size()) {
            historyIndex = -1;
            setText(draft);
            return;
        }
        setText(history.get(historyIndex));
    }

    /**
     * Takes the typed line, closes the prompt and remembers it.
     * <p>
     * An empty line is not remembered, it simply closes the prompt.
     *
     * @return the line that was submitted, empty when nothing was typed
     */
    public String submit() {
        String submitted = text();
        cancel();
        if (!submitted.isBlank()) {
            history.add(submitted);
            while (history.size() > HISTORY_LIMIT) {
                history.remove(0);
            }
        }
        return submitted;
    }

    /** Amount of lines the history holds. */
    public int historySize() {
        return history.size();
    }

    @Override
    public String toString() {
        return "ChatPrompt(open=" + open + ", '" + text + "', cursor " + cursor + ")";
    }

    /** Replaces the text and puts the cursor behind it. */
    private void setText(String value) {
        text.setLength(0);
        text.append(value);
        cursor = text.length();
    }
}
