package com.philia093.neofactory.chat;

import com.badlogic.gdx.Input;
import com.philia093.neofactory.chat.command.CommandContext;
import com.philia093.neofactory.chat.command.CommandRegistry;

import java.util.List;
import java.util.Objects;

/**
 * The chat of the game: one input line for messages and for commands.
 * <p>
 * The line is opened with {@code /} or with {@code T} and looks the same in both
 * cases. What the player typed decides what happens when they press enter: a line
 * that starts with a slash is a command and is handed to the
 * {@link CommandRegistry}, every other line is a message and is simply added to the
 * {@link ChatLog}. That is the whole difference between chatting and commanding.
 * <p>
 * While the line is open the chat owns the keyboard, which the caller has to honour
 * by swallowing every key this class reports as handled, see {@link #keyDown(int)}.
 * The world keeps running behind the line, so a drop next to the player is still
 * picked up while a message is typed.
 */
public final class ChatController {

    /** Key that opens the line and writes the command slash into it. */
    public static final int KEY_COMMAND = Input.Keys.SLASH;

    /** Key that opens the line for a message. */
    public static final int KEY_CHAT = Input.Keys.T;

    /** Name the messages of the player are signed with. */
    public static final String PLAYER_NAME = "Player";

    private final ChatPrompt prompt = new ChatPrompt();
    private final CommandRegistry commands;
    private final CommandContext context;

    /**
     * Creates the chat.
     *
     * @param commands commands a line behind a slash is looked up in
     * @param context world, player and chat the commands work with
     */
    public ChatController(CommandRegistry commands, CommandContext context) {
        this.commands = Objects.requireNonNull(commands, "commands");
        this.context = Objects.requireNonNull(context, "context");
    }

    /**
     * {@code true} when a key opens the line, which is {@code /} or {@code T}.
     *
     * @param keyCode key code, see {@link Input.Keys}
     * @return {@code true} for the two keys that open the chat
     */
    public static boolean isOpenKey(int keyCode) {
        return keyCode == KEY_COMMAND || keyCode == KEY_CHAT;
    }

    /**
     * {@code true} when the opening key also produces a character that has to be
     * thrown away.
     * <p>
     * Pressing {@code T} types the letter {@code t} in the same breath, which must
     * not land in the empty line. The slash key is the opposite case: the player
     * wants to see it, because it is what turns the line into a command.
     *
     * @param keyCode key that opened the line
     * @return {@code true} for {@code T}
     */
    public static boolean discardsOpeningCharacter(int keyCode) {
        return keyCode == KEY_CHAT;
    }

    /** {@code true} while the player types. */
    public boolean isOpen() {
        return prompt.isOpen();
    }

    /**
     * Chat the messages and the answers of the commands are collected in.
     * <p>
     * The log belongs to the context, not to the chat: a command writes its answer
     * into the very same log the player reads, and the game owns that log, see
     * {@link CommandContext#log()}.
     *
     * @return the log of the chat
     */
    public ChatLog log() {
        return context.log();
    }

    /** Text the player typed so far. */
    public String text() {
        return prompt.text();
    }

    /**
     * Lines to draw, oldest first.
     *
     * @return the messages of the last seconds, or the recent conversation while the
     *         line is open
     */
    public List<ChatMessage> visibleLines() {
        return log().visibleLines(prompt.isOpen());
    }

    /**
     * Opens the line.
     *
     * @param keyCode key that opened it, see {@link #isOpenKey(int)}
     */
    public void open(int keyCode) {
        prompt.open(discardsOpeningCharacter(keyCode));
    }

    /** Closes the line and forgets what was typed into it. */
    public void close() {
        prompt.cancel();
    }

    /**
     * Ages the messages, which is what makes them leave the screen.
     *
     * @param delta time since the last frame in seconds
     */
    public void update(float delta) {
        log().update(delta);
    }

    /**
     * Handles a key press.
     * <p>
     * Every key belongs to the open line, also the ones the game would use otherwise:
     * walking, dropping, opening the inventory or switching to fullscreen behind a
     * half typed message would be a surprise. The caller therefore swallows every key
     * this method reports as handled.
     *
     * @param keyCode key code, see {@link Input.Keys}
     * @return {@code true} when the key was used by the line
     */
    public boolean keyDown(int keyCode) {
        if (!prompt.isOpen()) {
            return false;
        }
        if (keyCode == Input.Keys.ESCAPE) {
            prompt.cancel();
        } else if (keyCode == Input.Keys.ENTER || keyCode == Input.Keys.NUMPAD_ENTER) {
            submit();
        } else if (keyCode == Input.Keys.BACKSPACE) {
            prompt.backspace();
        } else if (keyCode == Input.Keys.DEL) {
            prompt.delete();
        } else if (keyCode == Input.Keys.LEFT) {
            prompt.moveLeft();
        } else if (keyCode == Input.Keys.RIGHT) {
            prompt.moveRight();
        } else if (keyCode == Input.Keys.HOME) {
            prompt.moveToStart();
        } else if (keyCode == Input.Keys.END) {
            prompt.moveToEnd();
        } else if (keyCode == Input.Keys.UP) {
            prompt.showPrevious();
        } else if (keyCode == Input.Keys.DOWN) {
            prompt.showNext();
        }
        return true;
    }

    /**
     * Handles a typed character.
     *
     * @param character character the keyboard produced
     * @return {@code true} when the line took the character
     */
    public boolean keyTyped(char character) {
        if (!prompt.isOpen()) {
            return false;
        }
        prompt.type(character);
        return true;
    }

    /**
     * Takes the typed line and closes the chat.
     * <p>
     * A line behind a slash becomes a command, anything else becomes a message signed
     * with the name of the player. An empty line only closes the chat.
     *
     * @return the line that was submitted, empty when nothing was typed
     */
    public String submit() {
        String line = prompt.submit();
        if (line.isBlank()) {
            return line;
        }
        if (line.startsWith("/")) {
            commands.run(line, context);
        } else {
            log().addPlayer("<" + PLAYER_NAME + "> " + line);
        }
        return line;
    }

    @Override
    public String toString() {
        return "ChatController(" + (isOpen() ? "typing '" + text() + "'" : "closed")
                + ", " + log() + ")";
    }
}
