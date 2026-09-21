package com.philia093.neofactory.chat.command;

import com.philia093.neofactory.chat.ChatLog;
import com.philia093.neofactory.entity.Player;

/**
 * Everything a command may work with.
 * <p>
 * The context is the bridge between the chat and the running game: the screen that
 * owns the world implements it, while a test provides its own implementation and
 * therefore never needs a window. A command only ever writes to
 * {@link #log()}, so nothing it does can escape into the interface.
 */
public interface CommandContext {

    /** Player the commands work on. */
    Player player();

    /** Chat the answers of a command are written into. */
    ChatLog log();

    /** Seed the current world was generated from. */
    int seed();
}
