package com.philia093.neofactory.support;

import com.philia093.neofactory.chat.ChatLog;
import com.philia093.neofactory.chat.ChatMessage;
import com.philia093.neofactory.chat.command.CommandContext;
import com.philia093.neofactory.entity.Player;
import com.philia093.neofactory.world.GameMode;

import java.util.List;

/**
 * A command context that needs no game.
 * <p>
 * A command works on a player and writes its answers into the chat, so a test needs
 * exactly those two things. The player is a plain one with an empty inventory and
 * the chat collects the lines, which makes an answer checkable without a window,
 * see {@link #lastLine()} and {@link #gameMode()}.
 */
public final class TestCommandContext implements CommandContext {

    /** Player every command of the test works on. */
    public final Player player = new Player(0.0f, 0.0f);

    /** Chat the answers of the commands are collected in. */
    public final ChatLog log = new ChatLog();

    /** Seed the world of this context was generated from. */
    private final int seed;

    /** Mode the world of this context is played in. */
    private GameMode gameMode = GameMode.SURVIVAL;

    /** Creates a context with the seed {@code 1234}. */
    public TestCommandContext() {
        this(1234);
    }

    /**
     * Creates a context.
     *
     * @param seed seed {@code /seed} should answer with
     */
    public TestCommandContext(int seed) {
        this.seed = seed;
    }

    @Override
    public Player player() {
        return player;
    }

    @Override
    public ChatLog log() {
        return log;
    }

    @Override
    public int seed() {
        return seed;
    }

    @Override
    public GameMode gameMode() {
        return gameMode;
    }

    @Override
    public void setGameMode(GameMode mode) {
        gameMode = mode == null ? GameMode.SURVIVAL : mode;
    }

    /** The answer of the command that ran last. */
    public ChatMessage lastLine() {
        List<ChatMessage> lines = log.visibleLines(true);
        return lines.get(lines.size() - 1);
    }
}
